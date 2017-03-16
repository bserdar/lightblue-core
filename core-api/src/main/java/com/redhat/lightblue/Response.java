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
package com.redhat.lightblue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.lightblue.util.Error;
import com.redhat.lightblue.util.JsonObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Response information from mediator APIs
 */
public class Response extends JsonObject {

    private static final long serialVersionUID = 1L;

    private static final String PROPERTY_ENTITY = "entity";
    private static final String PROPERTY_VERSION = "entityVersion";
    private static final String PROPERTY_STATUS = "status";
    private static final String PROPERTY_MOD_COUNT = "modifiedCount";
    private static final String PROPERTY_MATCH_COUNT = "matchCount";
    private static final String PROPERTY_PROCESSED = "processed";
    private static final String PROPERTY_DATA_ERRORS = "dataErrors";
    private static final String PROPERTY_ERRORS = "errors";
    private static final String PROPERTY_HOSTNAME = "hostname";
    private static final String PROPERTY_RESULT_METADATA = "resultMetadata";

    private EntityVersion entity;
    private OperationStatus status;
    private long modifiedCount;
    private long matchCount;
    private JsonNode entityData;
    private List<ResultMetadata> resultMetadata;
    private String hostname;
    private final List<DataError> dataErrors = new ArrayList<>();
    private final List<Error> errors = new ArrayList<>();

    private final JsonNodeFactory jsonNodeFactory;

    public static final String HOSTNAME;

    static {
        String hostName = "unknown";
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            if (localHost != null) {
                hostName = localHost.getHostName();
            }
        } catch (UnknownHostException e) {

        }
        HOSTNAME = hostName;
    }

    /**
     * @deprecated use Response(JsonNodeFactory)
     */
    @Deprecated
    public Response() {
        jsonNodeFactory = JsonNodeFactory.withExactBigDecimals(true);
    }

    public Response(JsonNodeFactory jsonNodeFactory) {
        this.jsonNodeFactory = jsonNodeFactory;
        this.hostname = HOSTNAME;
    }

    public EntityVersion getEntity() {
        return entity;
    }

    public void setEntity(EntityVersion e) {
        entity=e;
    }

    public void setEntity(String entityName,String version) {
        this.entity=new EntityVersion(entityName,version);
    }

    /**
     * Status of the completed operation
     */
    public OperationStatus getStatus() {
        return status;
    }

    /**
     * Status of the completed operation
     */
    public void setStatus(OperationStatus s) {
        status = s;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * Number of documents inserted/updated/deleted
     */
    public long getModifiedCount() {
        return modifiedCount;
    }

    /**
     * Number of documents inserted/updated/deleted
     */
    public void setModifiedCount(long l) {
        modifiedCount = l;
    }

    /**
     * Number of documents that matched the search cResponseriteria
     */
    public long getMatchCount() {
        return matchCount;
    }

    /**
     * Number of documents that matched the search criteria
     */
    public void setMatchCount(long l) {
        matchCount = l;
    }

    /**
     * Returns the entity data resulting from the call.
     */
    public JsonNode getEntityData() {
        return entityData;
    }

    /**
     * Returns the entity data resulting from the call.
     */
    public void setEntityData(JsonNode node) {
        // if the node is not an array then wrap it in an array
        if (node != null && !node.isArray()) {
            ArrayNode arrayNode = new ArrayNode(jsonNodeFactory);
            arrayNode.add(node);
            entityData = arrayNode;
        } else {
            entityData = node;
        }
    }

    /**
     * Metadata list for documents in entityData. If there are more
     * than one documents, the entitydata and metadata indexes match.
     */
    public List<ResultMetadata> getResultMetadata() {
        return resultMetadata;
    }

    public void setResultMetadata(List<ResultMetadata> l) {
        resultMetadata=l;
    }

    /**
     * Errors related to each document
     */
    public List<DataError> getDataErrors() {
        return dataErrors;
    }

    /**
     * Errors related to the operation
     */
    public List<Error> getErrors() {
        return errors;
    }

    public static Response fromJson(JsonNode node) {
        Response ret=null;
        if(node instanceof ObjectNode) {
            ret=new Response();
            JsonNode x=node.get(PROPERTY_STATUS);
            if(x!=null)
                ret.status=OperationStatus.valueOf(x.asText());
            x=node.get(PROPERTY_MOD_COUNT);
            if(x!=null)
                ret.modifiedCount=x.asLong();
            x=node.get(PROPERTY_MATCH_COUNT);
            if(x!=null)
                ret.matchCount=x.asLong();
            x=node.get(PROPERTY_HOSTNAME);
            if(x!=null)
                ret.hostname=x.asText();
            ret.entityData=node.get(PROPERTY_PROCESSED);
            ArrayNode a=(ArrayNode)node.get(PROPERTY_DATA_ERRORS);
            if(a!=null) {
                for(Iterator<JsonNode> itr=a.elements();itr.hasNext();) {
                    x=itr.next();
                    if(x instanceof ObjectNode)
                        ret.dataErrors.add(DataError.fromJson((ObjectNode)x));
                }
            }
            a=(ArrayNode)node.get(PROPERTY_ERRORS);
            if(a!=null) {
                for(Iterator<JsonNode> itr=a.elements();itr.hasNext();) {
                    x=itr.next();
                    if(x instanceof ObjectNode)
                        ret.errors.add(Error.fromJson((ObjectNode)x));
                }
            }
            a=(ArrayNode)node.get(PROPERTY_RESULT_METADATA);
            if(a!=null) {
                ret.resultMetadata=new ArrayList<>();
                for(Iterator<JsonNode> itr=a.elements();itr.hasNext();) {
                    x=itr.next();
                    if(x instanceof ObjectNode) {
                        ret.resultMetadata.add(ResultMetadata.fromJson((ObjectNode)x));
                    }
                }
            }
        }
        return ret;
    }
    
    /**
     * Returns JSON representation of this
     */
    @Override
    public JsonNode toJson() {
        JsonNodeBuilder builder = new JsonNodeBuilder();
        if(entity!=null) {
            builder.add(PROPERTY_ENTITY,entity.getEntity());
            builder.add(PROPERTY_VERSION,entity.getVersion());
        }
        builder.add(PROPERTY_STATUS, status);
        builder.add(PROPERTY_MOD_COUNT, modifiedCount);
        builder.add(PROPERTY_MATCH_COUNT, matchCount);
        builder.add(PROPERTY_PROCESSED, entityData);
        builder.add(PROPERTY_HOSTNAME, HOSTNAME);
        builder.addJsonObjectsList(PROPERTY_DATA_ERRORS, dataErrors);
        builder.addErrorsList(PROPERTY_ERRORS, errors);
        if(resultMetadata!=null)
            builder.addJsonObjectsList(PROPERTY_RESULT_METADATA,resultMetadata);
        return builder.build();
    }

    // This class is not used
    @Deprecated
    public static class ResponseBuilder {

        private OperationStatus status;
        private long modifiedCount;
        private long matchCount;
        private JsonNode entityData;
        private String hostname;
        private List<DataError> dataErrors = new ArrayList<>();
        private List<Error> errors = new ArrayList<>();

        private final JsonNodeFactory jsonNodeFactory;

        public ResponseBuilder(JsonNodeFactory jsonNodeFactory) {
            this.jsonNodeFactory = jsonNodeFactory;
        }

        public ResponseBuilder(Response response) {
            status = response.getStatus();
            modifiedCount = response.getModifiedCount();
            matchCount = response.getMatchCount();
            hostname = response.getHostname();
            entityData = response.getEntityData();
            dataErrors = response.getDataErrors();
            errors = response.getErrors();
            jsonNodeFactory = response.jsonNodeFactory;
        }

        public ResponseBuilder withHostname(JsonNode node) {
            if (node != null) {
                hostname = node.asText();
            }
            return this;
        }

        public ResponseBuilder withStatus(JsonNode node) {
            if (node != null) {
                try {
                    status = OperationStatus.valueOf(node.asText().toUpperCase());
                } catch (IllegalArgumentException e) {
                    status = OperationStatus.ERROR;
                }
            }
            return this;
        }

        public ResponseBuilder withModifiedCount(JsonNode node) {
            if (node != null) {
                modifiedCount = node.asLong();
            }
            return this;
        }

        public ResponseBuilder withMatchCount(JsonNode node) {
            if (node != null) {
                matchCount = node.asLong();
            }
            return this;
        }


        public ResponseBuilder withEntityData(JsonNode node) {
            if (node != null) {
                entityData = node;
            }
            return this;
        }

        public ResponseBuilder withDataErrors(JsonNode node) {
            if (node instanceof ArrayNode) {
                for (Iterator<JsonNode> itr = ((ArrayNode) node).elements();
                        itr.hasNext();) {
                    dataErrors.add(DataError.fromJson((ObjectNode) itr.next()));
                }
            }
            return this;
        }

        public ResponseBuilder withErrors(JsonNode node) {
            if (node instanceof ArrayNode) {
                for (Iterator<JsonNode> itr = ((ArrayNode) node).elements();
                        itr.hasNext();) {
                    errors.add(Error.fromJson(itr.next()));
                }
            }
            return this;
        }

        public Response buildResponse() {
            Response response = new Response(jsonNodeFactory);

            response.setStatus(status);
            response.setModifiedCount(modifiedCount);
            response.setMatchCount(matchCount);
            response.setEntityData(entityData);
            response.getDataErrors().addAll(dataErrors);
            response.getErrors().addAll(errors);

            return response;
        }
    }
}
