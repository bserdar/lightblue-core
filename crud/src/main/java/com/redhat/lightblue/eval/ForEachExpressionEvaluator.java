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
package com.redhat.lightblue.eval;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.redhat.lightblue.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.redhat.lightblue.crud.CrudConstants;
import com.redhat.lightblue.metadata.ArrayElement;
import com.redhat.lightblue.metadata.ArrayField;
import com.redhat.lightblue.metadata.FieldTreeNode;
import com.redhat.lightblue.query.AllMatchExpression;
import com.redhat.lightblue.query.ForEachExpression;
import com.redhat.lightblue.query.QueryExpression;
import com.redhat.lightblue.query.RemoveElementExpression;
import com.redhat.lightblue.query.UpdateExpression;

/**
 * Evaluates a loop over the elements of an array
 */
public class ForEachExpressionEvaluator extends Updater {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForEachExpressionEvaluator.class);

    private final JsonNodeFactory factory;
    private final Memento memento;
    private ProcessedInfo processedInfo = null;
    private int numAny = 0;
    private TreeSet<Path> updateFields;


    public ForEachExpressionEvaluator(JsonNodeFactory factory, FieldTreeNode context, ForEachExpression expr) {
        this.memento = new Memento(factory,context,expr);
        this.factory = factory;

        if(expr.getField().nAnys() == 0 ) {
            // Resolve the field, make sure it is an array
            this.processedInfo = generateProcessedInfo(factory, context, expr, null);
        } else {
            // if it have any, this process is postponed to the evaluation of the JSON document
            numAny = expr.getField().nAnys();
        }

    }

    private ProcessedInfo generateProcessedInfo(JsonNodeFactory factory, FieldTreeNode context, ForEachExpression expr, Path informedField) {
        Path field;
        ArrayField fieldMd;
        QueryEvaluator queryEvaluator;
        Updater updater;

        if(informedField == null) {
            field = expr.getField();
        } else {
            field = informedField;
        }
        FieldTreeNode md = context.resolve(field);
        if (md instanceof ArrayField) {
            fieldMd = (ArrayField) md;
        } else {
            throw new EvaluationError(CrudConstants.ERR_FIELD_NOT_ARRAY + field);
        }

        // Get a query evaluator
        QueryExpression query = expr.getQuery();
        if (query instanceof AllMatchExpression) {
            queryEvaluator = new AllEvaluator();
        } else {
            queryEvaluator = QueryEvaluator.getInstance(query, fieldMd.getElement());
        }

        // Get an updater to execute on each matching element
        UpdateExpression upd = expr.getUpdate();
        if (upd instanceof RemoveElementExpression) {
            updater = new RemoveEvaluator(fieldMd.getElement().getFullPath());
        } else {
            updater = Updater.getInstance(factory, fieldMd.getElement(), upd);
        }
        ProcessedInfo processedInfo1 = new ProcessedInfo(field, fieldMd, queryEvaluator, updater);
        return processedInfo1;
    }

    @Override
    public void getUpdateFields(Set<Path> fields) {
        if(numAny == 0) {
            this.processedInfo.updater.getUpdateFields(fields);
        } else {
            if(this.updateFields == null){
                this.updateFields = new TreeSet<>();
            }
            this.updateFields.addAll(fields);
        }
    }

    @Override
    public boolean update(JsonDoc doc, FieldTreeNode contextMd, Path contextPath) {
        boolean ret = false;

        if(numAny > 0) {
            KeyValueCursor<Path, JsonNode> cursor = doc.getAllNodes(memento.expr.getField());

            boolean b = cursor.hasNext();
            while(b){
                cursor.next();
                Path currentKey = cursor.getCurrentKey();
                JsonNode currentValue = cursor.getCurrentValue();

                ProcessedInfo processedInfo1 = generateProcessedInfo(factory, memento.context, memento.expr, currentKey);
                if(this.updateFields != null){
                    processedInfo1.updater.getUpdateFields(updateFields);
                }
                ret = updateUsingProcessedInfo(doc, contextPath, ret, processedInfo1);
                b = cursor.hasNext();
            }

            return ret;
        } else {
            ret = updateUsingProcessedInfo(doc, contextPath, ret, this.processedInfo);
            return ret;
        }
    }

    private boolean updateUsingProcessedInfo(JsonDoc doc, Path contextPath, boolean ret, ProcessedInfo processedInfo) {
        // Get a reference to the array field, and iterate all elements in the array
        ArrayNode arrayNode = (ArrayNode) doc.get(new Path(contextPath, processedInfo.field));
        LOGGER.debug("Array node {}={}", processedInfo.field, arrayNode);
        ArrayElement elementMd = processedInfo.fieldMd.getElement();
        if (arrayNode != null) {
            int index = 0;
            MutablePath itrPath = new MutablePath(contextPath);
            itrPath.push(processedInfo.field);
            MutablePath arrSizePath = itrPath.copy();
            arrSizePath.setLast(arrSizePath.getLast() + "#");
            arrSizePath.rewriteIndexes(contextPath);

            itrPath.push(index);
            // Copy the nodes to a separate list, so we iterate on the
            // new copy, and modify the original
            ArrayList<JsonNode> nodes = new ArrayList<>();
            for (Iterator<JsonNode> itr = arrayNode.elements(); itr.hasNext(); ) {
                nodes.add(itr.next());
            }
            for (JsonNode elementNode : nodes) {
                itrPath.setLast(index);
                Path elementPath = itrPath.immutableCopy();
                LOGGER.debug("itr:{}", elementPath);
                QueryEvaluationContext ctx = new QueryEvaluationContext(elementNode, elementPath);
                if (processedInfo.queryEvaluator.evaluate(ctx)) {
                    LOGGER.debug("query matches {}", elementPath);
                    LOGGER.debug("Calling updater {}", processedInfo.updater);
                    if (processedInfo.updater.update(doc, elementMd, elementPath)) {
                        LOGGER.debug("Updater {} returns {}", processedInfo.updater, true);
                        ret = true;
                        // Removal shifts nodes down
                        if (processedInfo.updater instanceof RemoveEvaluator) {
                            index--;
                        }
                    } else {
                        LOGGER.debug("Updater {} return false", processedInfo.updater);
                    }
                } else {
                    LOGGER.debug("query does not match {}", elementPath);
                }
                index++;
            }
            if (ret) {
                doc.modify(arrSizePath, factory.numberNode(arrayNode.size()), false);
            }
        }
        return ret;
    }

    private class Memento {
        private final JsonNodeFactory factory;
        private final FieldTreeNode context;
        private final ForEachExpression expr;

        public Memento(JsonNodeFactory factory, FieldTreeNode context, ForEachExpression expr) {
            this.factory =factory;
            this.context =context;
            this.expr =expr;
        }
    }

    private class ProcessedInfo {
        private Path field;
        private ArrayField fieldMd;
        private QueryEvaluator queryEvaluator;
        private Updater updater;

        public ProcessedInfo(Path field, ArrayField fieldMd, QueryEvaluator queryEvaluator, Updater updater) {
            this.field = field;
            this.fieldMd = fieldMd;
            this.queryEvaluator = queryEvaluator;
            this.updater = updater;
        }
    }


    /**
     * Inner class for $all
     */
    private static class AllEvaluator extends QueryEvaluator {
        @Override
        public boolean evaluate(QueryEvaluationContext ctx) {
            ctx.setResult(true);
            return true;
        }
    }

    private static class RemoveEvaluator extends Updater {
        private final Path absField;

        public RemoveEvaluator(Path absField) {
            this.absField = absField;
        }

        @Override
        public void getUpdateFields(Set<Path> fields) {
            fields.add(absField);
        }

        @Override
        public boolean update(JsonDoc doc, FieldTreeNode contextMd, Path contextPath) {
            return doc.modify(contextPath, null, false) != null;
        }
    }
}
