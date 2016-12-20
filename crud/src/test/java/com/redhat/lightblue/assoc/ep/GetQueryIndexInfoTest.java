package com.redhat.lightblue.assoc.ep;

import java.util.List;

import org.junit.Test;
import org.junit.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import com.redhat.lightblue.query.*;

import com.redhat.lightblue.util.Path;
import com.redhat.lightblue.util.JsonUtils;

import com.redhat.lightblue.assoc.AnalyzeQuery;
import com.redhat.lightblue.assoc.QueryFieldInfo;

import com.redhat.lightblue.metadata.*;
import com.redhat.lightblue.metadata.parser.Extensions;
import com.redhat.lightblue.metadata.parser.JSONMetadataParser;
import com.redhat.lightblue.metadata.types.DefaultTypes;
import com.redhat.lightblue.metadata.test.DatabaseMetadata;
import com.redhat.lightblue.util.test.AbstractJsonSchemaTest;
import com.redhat.lightblue.TestDataStoreParser;

public class GetQueryIndexInfoTest extends AbstractJsonSchemaTest {

    private class TestMetadata extends DatabaseMetadata {
        public EntityMetadata getEntityMetadata(String entityName, String version) {
            return getMd(entityName);
        }
    }
    
    private EntityMetadata getMd(String fname) {
        try {
            JsonNode node = loadJsonNode("composite/" + fname + ".json");
            Extensions<JsonNode> extensions = new Extensions<>();
            extensions.addDefaultExtensions();
            extensions.registerDataStoreParser("mongo", new TestDataStoreParser<JsonNode>());
            TypeResolver resolver = new DefaultTypes();
            JSONMetadataParser parser = new JSONMetadataParser(extensions, resolver, JsonNodeFactory.instance);
            EntityMetadata md = parser.parseEntityMetadata(node);
            PredefinedFields.ensurePredefinedFields(md);
            return md;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CompositeMetadata getCmd(String fname, Projection p) {
        EntityMetadata md = getMd(fname);
        return CompositeMetadata.buildCompositeMetadata(md, new GMD(p, null));
    }

    private class GMD extends AbstractGetMetadata {
        public GMD(Projection p, QueryExpression q) {
            super(p, q);
        }
        
        @Override
        protected EntityMetadata retrieveMetadata(Path injectionField,
                                                  String entityName,
                                                  String version) {
            try {
                return getMd(entityName);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private QueryExpression query(String s) throws Exception {
        return QueryExpression.fromJson(JsonUtils.json(s.replace('\'', '\"')));
    }

    private Projection projection(String s) throws Exception {
        return Projection.fromJson(JsonUtils.json(s.replace('\'', '\"')));
    }

    @Test
    public void testBasicQuery() throws Exception {
        GMD gmd = new GMD(projection("{'field':'obj1.c','include':1}"), null);
        CompositeMetadata md = CompositeMetadata.buildCompositeMetadata(getMd("A"), gmd);
        
        AnalyzeQuery pq = new AnalyzeQuery(md, null);
        QueryExpression q = query("{ 'field':'field1','op':'=','rvalue':'x'}");
        pq.iterate(q);
        List<QueryFieldInfo> list = pq.getFieldInfo();
        IndexInfo info=new GetQueryIndexInfo(list).iterate(q);
        System.out.println(info);
        Assert.assertEquals(1,info.size());
        Assert.assertTrue(info.contains(new Path("field1")));
    }

    @Test
    public void testAndQuery() throws Exception {
        GMD gmd = new GMD(projection("{'field':'obj1.c','include':1}"), null);
        CompositeMetadata md = CompositeMetadata.buildCompositeMetadata(getMd("A"), gmd);
        
        AnalyzeQuery pq = new AnalyzeQuery(md, null);
        QueryExpression q = query("{'$and':[{ 'field':'field1','op':'=','rvalue':'x'},{'field':'b_ref','op':'=','rvalue':'y'}]}");
        pq.iterate(q);
        List<QueryFieldInfo> list = pq.getFieldInfo();
        IndexInfo info=new GetQueryIndexInfo(list).iterate(q);
        System.out.println(info);
        Assert.assertEquals(2,info.size());
        Assert.assertTrue(info.contains(new Path("field1")));
        Assert.assertTrue(info.contains(new Path("b_ref")));
    }

    @Test
    public void testOrQuery() throws Exception {
        GMD gmd = new GMD(projection("{'field':'obj1.c','include':1}"), null);
        CompositeMetadata md = CompositeMetadata.buildCompositeMetadata(getMd("A"), gmd);
        
        AnalyzeQuery pq = new AnalyzeQuery(md, null);
        QueryExpression q = query("{'$or':[{ 'field':'field1','op':'=','rvalue':'x'},{'field':'b_ref','op':'=','rvalue':'y'}]}");
        pq.iterate(q);
        List<QueryFieldInfo> list = pq.getFieldInfo();
        IndexInfo info=new GetQueryIndexInfo(list).iterate(q);
        System.out.println(info);
        Assert.assertEquals(0,info.size());
    }

    @Test
    public void testElemMatchQuery() throws Exception {
        GMD gmd = new GMD(projection("{'field':'obj1.c','include':1}"), null);
        CompositeMetadata md = CompositeMetadata.buildCompositeMetadata(getMd("A"), gmd);        
        AnalyzeQuery pq = new AnalyzeQuery(md, null);
        QueryExpression q = query("{'array':'level1.arr1','elemMatch':{'$and':[{'field':'b_ref','op':'=','rvalue':'x'},{'field':'field','op':'=','rvalue':'y'}]}}");
        pq.iterate(q);
        List<QueryFieldInfo> list = pq.getFieldInfo();
        IndexInfo info=new GetQueryIndexInfo(list).iterate(q);
        System.out.println(info);
        Assert.assertEquals(2,info.size());
        Assert.assertTrue(info.contains(new Path("level1.arr1.*.b_ref")));
        Assert.assertTrue(info.contains(new Path("level1.arr1.*.field")));
   }
}
