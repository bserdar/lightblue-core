/*
 Copyright 2013 Red Hat, Inc. and/or its affiliates.

 This file is part of lightblue.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.redhat.lightblue.assoc.ep;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import java.util.stream.Collectors;

import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import com.redhat.lightblue.query.QueryExpression;
import com.redhat.lightblue.query.NaryLogicalOperator;

import com.redhat.lightblue.metadata.CompositeMetadata;

import com.redhat.lightblue.assoc.BindQuery;
import com.redhat.lightblue.assoc.QueryFieldInfo;

import com.redhat.lightblue.eval.QueryEvaluator;

import com.redhat.lightblue.util.Path;
import com.redhat.lightblue.util.JsonDoc;

/**
 * There are two sides to an Assemble step: Assemble gets results from the
 * source, and for each of those documents, it runs the associated queries on
 * the destinations, gets the results, and inserts those documents to the
 * document it got from the source side.
 */
public class Assemble extends Step<ResultDocument> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Assemble.class);

    private final ExecutionBlock[] destinationBlocks;
    private final Source<ResultDocument> source;
    private Map<ExecutionBlock, Assemble> destinations;

    public Assemble(ExecutionBlock block,
                    Source<ResultDocument> source,
                    ExecutionBlock[] destinationBlocks) {
        super(block);
        this.source = source;
        this.destinationBlocks = destinationBlocks;
    }

    public List<ResultDocument> getResultList(QueryExpression q, ExecutionContext ctx) {
        LOGGER.debug("getResultList q={} block={}", q, block);
        Retrieve r = block.getStep(Retrieve.class);
        if (r != null) {
            r.setQuery(q);
            StepResult<ResultDocument> results = block.getResultStep().getResults(ctx);
            return results.stream().collect(Collectors.toList());
        } else {
            throw new IllegalStateException("Cannot find a Retrieve step in block");
        }
    }

    @Override
    public void initialize() {
        destinations = new HashMap<ExecutionBlock, Assemble>();
        for (ExecutionBlock x : destinationBlocks) {
            Assemble a = x.getStep(Assemble.class);
            if (a != null) {
                destinations.put(x, a);
            } else {
                throw new IllegalArgumentException("No assemble step in " + x);
            }
        }
    }
    
    @Override
    public StepResult<ResultDocument> getResults(ExecutionContext ctx) {
        LOGGER.debug("getResults, source:{}, destinations={}", source, destinations);
        // Get the results from the source
        StepResult<ResultDocument> sourceResults = source.getStep().getResults(ctx);
        List<ResultDocument> results = sourceResults.stream().collect(Collectors.toList());
        if (ctx.hasErrors()) {
            return StepResult.EMPTY;
        }

        List<Future> assemblers = new ArrayList<>();
        for (Map.Entry<ExecutionBlock, Assemble> destination : destinations.entrySet()) {
            AssociationQuery aq = destination.getKey().getAssociationQueryForEdge(block);
            LOGGER.debug("Scheduling batch assembler with aq={} block={}", aq, destination.getKey());
            BatchAssembler batchAssembler = new BatchAssembler(256, aq, destination.getValue(), ctx);
            assemblers.add(ctx.getExecutor().submit(() -> {
                if (aq.getQuery() == null) {
                    if(aq.isAlwaysTrue()) {
                        results.stream().forEach(batchAssembler::addDoc);
                        batchAssembler.endDoc();
                    }
                } else {
                    results.stream().forEach(doc -> {
                        batchAssembler.addDoc(doc);
                        Map<ChildSlot, QueryExpression> queries = Searches.
                                writeChildQueriesFromParentDoc(aq, doc);
                        queries.values().stream().forEach(batchAssembler::addQuery);
                        batchAssembler.endDoc();
                    });
                }
                batchAssembler.commit();
            }));
        }
        try {
            for (Future x : assemblers) {
                x.get();
            }
        } catch (Exception ie) {
            throw new RuntimeException(ie);
        }
        if (ctx.hasErrors()) {
            return StepResult.EMPTY;
        }
        // Stream results
        return new ListStepResult(results);
    }

    private static class DocAndQ {
        private final ResultDocument doc;
        private final List<QueryExpression> queries = new ArrayList<>();

        public DocAndQ(ResultDocument doc) {
            this.doc = doc;
        }
    }

    private static class BatchAssembler {
        private List<DocAndQ> docs = new ArrayList<>();
        private List<QueryExpression> queries = new ArrayList<>();
        private final int batchSize;
        private final AssociationQuery aq;
        private final Assemble dest;
        private final ExecutionContext ctx;

        public BatchAssembler(int batchSize, AssociationQuery aq, Assemble dest, ExecutionContext ctx) {
            this.batchSize = batchSize;
            this.dest = dest;
            this.aq = aq;
            this.ctx = ctx;
        }

        public void addQuery(QueryExpression q) {
            docs.get(docs.size() - 1).queries.add(q);
            queries.add(q);
        }

        public void addDoc(ResultDocument doc) {
            docs.add(new DocAndQ(doc));
        }

        public void endDoc() {
            if (queries.size() >= batchSize) {
                commit();
            }
        }

        public void commit() {
            if (!docs.isEmpty()) {
                QueryExpression combinedQuery;
                if (!queries.isEmpty()) {
                    combinedQuery = Searches.combine(NaryLogicalOperator._or, queries);
                    LOGGER.debug("Combined retrieval query:{}", combinedQuery);
                } else {
                    combinedQuery = null;
                }
                List<ResultDocument> destResults = dest.getResultList(combinedQuery, ctx);
                DocIndex childIndex=null;
                if(aq.getQuery()!=null) {
                    GetQueryIndexInfo qii=new GetQueryIndexInfo(aq.getQueryFieldInfo());
                    IndexInfo indexInfo=qii.iterate(aq.getQuery());
                    LOGGER.debug("In-memory index info:{}",indexInfo);
                    if(indexInfo.size()>0) {
                        // index child docs
                        childIndex=new DocIndex(indexInfo,aq.getReference().getReferencedMetadata());
                        for(ResultDocument child:destResults)
                            childIndex.add(child.getDoc());
                    }
                }
                for (DocAndQ parentDocAndQ : docs) {
                    associateDocs(parentDocAndQ.doc, destResults, aq, childIndex);
                }
            }
            docs = new ArrayList<>();
            queries = new ArrayList<>();
        }
    }
    
    private JsonNode toJson(Step.ToJsonCb<Step> scb,Step.ToJsonCb<ExecutionBlock> bcb) {
        ObjectNode o = JsonNodeFactory.instance.objectNode();
        ObjectNode a = JsonNodeFactory.instance.objectNode();
        o.set("assemble", a);
        a.set("entity", JsonNodeFactory.instance.textNode(block.getMetadata().getName()));
        a.set("left", scb.toJson(source.getStep()));
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        a.set("right", array);
        for (ExecutionBlock b : destinationBlocks) {
            ObjectNode detail=JsonNodeFactory.instance.objectNode();
            AssociationQuery aq = b.getAssociationQueryForEdge(block);
            detail.set("associationQuery",aq.getQuery().toJson());
            detail.set("source",bcb.toJson(b));
            array.add(detail);
        }
        return o;
    }

    @Override
    public JsonNode toJson() {
        return toJson(Step::toJson,ExecutionBlock::toJson);
    }

    @Override
    public JsonNode explain(ExecutionContext ctx) {
        return toJson(s->{return s.explain(ctx);},
                      t->{return t.explain(ctx);});
    }

        /**
     * Associates child documents obtained from 'aq' to all the slots in the
     * parent document
     */
    public static void associateDocs(ResultDocument parentDoc,
                                     List<ResultDocument> childDocs,
                                     AssociationQuery aq,
                                     DocIndex childIndex) {
        if(!childDocs.isEmpty()) {
            CompositeMetadata childMetadata = childDocs.get(0).getBlock().getMetadata();
            List<ChildSlot> slots = parentDoc.getSlots().get(aq.getReference());        
            for (ChildSlot slot : slots) {
                BindQuery binders = parentDoc.getBindersForSlot(slot, aq);
                // No binders means all child docs will be added to the parent
                // aq.always==true means query is always true, so add everything to the parent
                if (binders.getBindings().isEmpty()||(aq.getAlways()!=null && aq.getAlways()) ) {
                    associateAllDocs(parentDoc,childDocs,slot.getSlotFieldName());
                } else if(aq.getAlways()==null||!aq.getAlways()) { // If query is not always false
                    if(childIndex==null)
                        associateDocs(childMetadata,parentDoc,slot.getSlotFieldName(),binders,childDocs,aq.getQuery());
                    else 
                        associateDocsWithIndex(childMetadata,parentDoc,slot.getSlotFieldName(),binders,childDocs,aq,childIndex);
                }
            }
        }
    }

    private static void associateAllDocs(ResultDocument parentDoc,List<ResultDocument> childDocs,Path fieldName) {
        ArrayNode destNode=ensureDestNodeExists(parentDoc,null,fieldName);
        for (ResultDocument d : childDocs) {
            destNode.add(d.getDoc().getRoot());
        }
    }

    private static ArrayNode ensureDestNodeExists(ResultDocument doc,ArrayNode destNode,Path fieldName) {
        if (destNode == null) {
            destNode = JsonNodeFactory.instance.arrayNode();
            doc.getDoc().modify(fieldName, destNode, true);
        }
        return destNode;
    }

    /**
     * Associate child documents with their parents. The association query is
     * for the association from the child to the parent, so caller must flip it
     * before sending it in if necessary. The caller also make sure parentDocs
     * is a unique stream.
     *
     * @param parentDoc The parent document
     * @param parentSlot The slot in parent docuemnt to which the results will
     * be attached
     * @param childDocs The child documents
     * @param aq The association query from parent to child. This may not be the
     * same association query between the blocks. If the child block is before
     * the parent block, a new aq must be constructed for the association from
     * the parent to the child
     */
    private static void associateDocs(CompositeMetadata childMetadata,
                                      ResultDocument parentDoc,
                                      Path destFieldName,
                                      BindQuery binders,
                                      List<ResultDocument> childDocs,
                                      QueryExpression query) {
        LOGGER.debug("Associating docs");
        QueryExpression boundQuery = binders.iterate(query);
        LOGGER.debug("Association query:{}", boundQuery);
        QueryEvaluator qeval = QueryEvaluator.getInstance(boundQuery, childMetadata);
        ArrayNode destNode=null;
        for (ResultDocument childDoc : childDocs) {
            if (qeval.evaluate(childDoc.getDoc()).getResult()) {
                destNode=ensureDestNodeExists(parentDoc,destNode,destFieldName);
                destNode.add(childDoc.getDoc().getRoot());
            }
        }
    }

    private static void associateDocsWithIndex(CompositeMetadata childMetadata,
                                               ResultDocument parentDoc,
                                               Path destFieldName,
                                               BindQuery binders,
                                               List<ResultDocument> childDocs,
                                               AssociationQuery aq,
                                               DocIndex childIndex) {
        LOGGER.debug("Associating docs using index");
        // We have an index. We use the index to reduce our result
        // set, and make our query smaller. Where this makes a
        // difference is when we're looking for values in arrays,
        // because an array search increases query evaluation time by
        // the number of elements in the array.
        
        // Set the below two up using full doclist and query
        // If we can optimize these two using the index, then we'll do that
        // otherwise, we'll fall back to the indexless association
        QueryExpression associationQuery=aq.getQuery();
        List<ResultDocument> resultDocs=childDocs;
        
        // The index uses a subset of the association query conjuncts
        // The remaining conjuncts will be used to compose a new
        // association query to run on a restricted set of documents
        List<AssociationQuery.AssociationQueryConjunct> conjuncts=aq.getConjuncts();
        // Split the query conjuncts into two
        //  queries[0]: conjuncts used by the index
        //  queries[1]: conjuncts not used by the index
        List<AssociationQuery.AssociationQueryConjunct>[] queries=splitQuery(childIndex,conjuncts);
        LOGGER.debug("Index uses: {}, does not use: {}",queries[0],queries[1]);
        if(!queries[0].isEmpty()) {
            // This means we can use the index
            List<QueryFieldInfo> qfi=new ArrayList<>(queries[0].size());
            List<QueryExpression> expressions=new ArrayList<>(queries[0].size());
            for(AssociationQuery.AssociationQueryConjunct c:queries[0]) {
                qfi.addAll(c.qfi);
                expressions.add(c.query);
            }
            QueryExpression indexedQuery=binders.iterate(Searches.and(expressions));
            LOGGER.debug("Indexed query:{}",indexedQuery);
            resultDocs=reorder(childDocs,childIndex.getResults(indexedQuery));
            if(!queries[1].isEmpty()) {
                // This means, the index does not use all of the query
                // Reduce the result set, and use a subset of the query
                associationQuery=Searches.and(queries[1].stream().map(t->t.query).collect(Collectors.toList()));
            } else {
                // Index fully uses the query, and result docs are all that there is
                associateAllDocs(parentDoc,resultDocs,destFieldName);
                return;
            }
        } 
        // Fallback: do it the slow way
        associateDocs(childMetadata,parentDoc,destFieldName,binders,resultDocs,associationQuery);
    }

    /**
     * Decides which ones of the conjuncts can be resolved by the index. Returns those in ret[0], and the others in ret[1]
     */
    private static List<AssociationQuery.AssociationQueryConjunct>[] splitQuery(DocIndex index,
                                                                                List<AssociationQuery.AssociationQueryConjunct> conjuncts) {
        List[] ret=new ArrayList[2];
        ret[0]=new ArrayList<>(conjuncts.size());
        ret[1]=new ArrayList<>(conjuncts.size());
        for(AssociationQuery.AssociationQueryConjunct c:conjuncts) {
            if(index.getClauseItem(c.query)!=null)
                ret[0].add(c);
            else
                ret[1].add(c);
        }
        return ret;
    }

    /**
     * Returns the documents in foundList in the order of originalList
     */
    private static List<ResultDocument> reorder(List<ResultDocument> originalList,Set<JsonDoc> foundList) {
        List<ResultDocument> ret=new ArrayList<>(foundList.size());
        for(ResultDocument d:originalList) {
            if(foundList.contains(d.getDoc()))
                ret.add(d);
        }
        return ret;
    }
}
