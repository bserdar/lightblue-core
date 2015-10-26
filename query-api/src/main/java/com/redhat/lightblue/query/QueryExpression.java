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
package com.redhat.lightblue.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.lightblue.util.Error;
import com.redhat.lightblue.util.JsonObject;
import com.redhat.lightblue.util.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Base class for all query expressions
 */
public abstract class QueryExpression extends JsonObject {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryExpression.class);

    private static final class BindableClausesItr extends QueryIterator {
        private List<QueryInContext> list;
        private List<QueryExpression> stack=new ArrayList<>(8);

        public BindableClausesItr(List<QueryInContext> l) {
            this.list = l;
        }

        @Override
        protected QueryExpression itrFieldComparisonExpression(FieldComparisonExpression q, Path ctx) {
            list.add(new QueryInContext(ctx, q, stack.toArray(new QueryExpression[stack.size()])));
            return q;
        }
        @Override
        protected QueryExpression itrNaryFieldRelationalExpression(NaryFieldRelationalExpression q, Path ctx) {
            list.add(new QueryInContext(ctx,q,stack.toArray(new QueryExpression[stack.size()])));
            return q;
        }

        @Override
        protected QueryExpression itrArrayMatchExpression(ArrayMatchExpression q,Path ctx) {
            stack.add(q);
            QueryExpression x=super.itrArrayMatchExpression(q,ctx);
            stack.remove(stack.size()-1);
            return x;
        }
    }

    private static final class GetQueryFieldsItr extends QueryIterator {
        private List<FieldInfo> fields;

        public GetQueryFieldsItr(List<FieldInfo> fields) {
            this.fields = fields;
        }

        @Override
        protected QueryExpression itrArrayContainsExpression(ArrayContainsExpression q, Path ctx) {
            fields.add(new FieldInfo(new Path(ctx, q.getArray()), ctx, q));
            return q;
        }

        @Override
        protected QueryExpression itrArrayMatchExpression(ArrayMatchExpression q, Path ctx) {
            // Array match expression does not have the array itself as a field.
            // All the fields references in the nested expression are the fields used by this expression
            return super.itrArrayMatchExpression(q,ctx);
        }

        @Override
        protected QueryExpression itrFieldComparisonExpression(FieldComparisonExpression q, Path ctx) {
            fields.add(new FieldInfo(new Path(ctx, q.getField()), ctx, q));
            fields.add(new FieldInfo(new Path(ctx, q.getRfield()), ctx, q));
            return q;
        }

        @Override
        protected QueryExpression itrNaryValueRelationalExpression(NaryValueRelationalExpression q, Path ctx) {
            fields.add(new FieldInfo(new Path(ctx, q.getField()), ctx, q));
            return q;
        }

        @Override
        protected QueryExpression itrNaryFieldRelationalExpression(NaryFieldRelationalExpression q, Path ctx) {
            fields.add(new FieldInfo(new Path(ctx, q.getField()), ctx, q));
            fields.add(new FieldInfo(new Path(ctx, q.getRfield()), ctx, q));
            return q;
        }

        @Override
        protected QueryExpression itrRegexMatchExpression(RegexMatchExpression q, Path ctx) {
            fields.add(new FieldInfo(new Path(ctx, q.getField()), ctx, q));
            return q;
        }

        @Override
        protected QueryExpression itrValueComparisonExpression(ValueComparisonExpression q, Path ctx) {
            fields.add(new FieldInfo(new Path(ctx, q.getField()), ctx, q));
            return q;
        }
    }

    /**
     * Returns field information about the query
     */
    public List<FieldInfo> getQueryFields() {
        List<FieldInfo> list = new ArrayList<FieldInfo>(16);
        getQueryFields(list);
        return list;
    }

    /**
     * Returns field information about the query
     *
     * @param fields The call adds the field information to this list
     */
    public void getQueryFields(List<FieldInfo> fields) {
        getQueryFields(fields, Path.EMPTY);
    }

    /**
     * The implementation should populate the list with the field information
     */
    public void getQueryFields(List<FieldInfo> fields, Path ctx) {
        new GetQueryFieldsItr(fields).iterate(this, ctx);
    }

    /**
     * Returns the query expressions that can be bound to a value
     *
     * These clauses are bindable:
     * <ul>
     * <li>FieldComparisonExpression</li>
     * <li>NaryFieldRelationalExpression</li>
     * </ul>
     */
    public List<QueryInContext> getBindableClauses() {
        List<QueryInContext> list = new ArrayList<>();
        getBindableClauses(list, Path.EMPTY);
        return list;
    }

    /**
     * Adds the query expressions that can be bound to a value to the given list
     */
    public void getBindableClauses(List<QueryInContext> list, Path ctx) {
        new BindableClausesItr(list).iterate(this, ctx);
    }

    public QueryExpression bind(List<FieldBinding> bindingResult,
                                Set<Path> bindRequest) {
        return bind(Path.EMPTY, bindingResult, bindRequest);
    }

    /**
     * Binds all the bindable fields in the bindRequest, populates the
     * bindingResult with binding information, and return a new QueryExpression
     * with bound values.
     *
     * @param ctx Context
     * @param bindingResult The results of the bindings will be added to this
     * list
     * @param bindRequest Full paths to the fields to be bound. If there are
     * array elements, '*' must be used
     *
     * @return A new instance of the query object with boun values. If there are
     * no bindable values, the same query object will be returned.
     */
    public QueryExpression bind(Path ctx,
                                List<FieldBinding> bindingResult,
                                Set<Path> bindRequest) {
        return new Bind(bindingResult, bindRequest).iterate(this, ctx);
    }

    /**
     * Parses a query expression from the given json node
     */
    public static QueryExpression fromJson(JsonNode node) {
        if (node instanceof ObjectNode) {
            ObjectNode onode = (ObjectNode) node;
            // If there is only one field, then that field must be a
            // logical operator
            String firstField = onode.fieldNames().next();
            if (UnaryLogicalOperator.fromString(firstField) != null) {
                return UnaryLogicalExpression.fromJson(onode);
            } else if (NaryLogicalOperator.fromString(firstField) != null) {
                return NaryLogicalExpression.fromJson(onode);
            } else {
                return ComparisonExpression.fromJson(onode);
            }
        } else {
            throw Error.get(QueryConstants.ERR_INVALID_QUERY, node.toString());
        }
    }

    /**
     * Returns if the field is required to evaluate this query
     *
     * @param field The field
     */
    public boolean isRequired(Path field) {
        LOGGER.debug("Checking if {} is queried", field);
        return isRequired(field, Path.EMPTY);
    }

    /**
     * Returns if the field is required to evaluate this query
     *
     * @param field The field
     * @param ctx The nested context
     */
    public boolean isRequired(Path field, Path ctx) {
        if (this instanceof ValueComparisonExpression) {
            return isFieldQueried(field, (ValueComparisonExpression) this, ctx);
        } else if (this instanceof FieldComparisonExpression) {
            return isFieldQueried(field, (FieldComparisonExpression) this, ctx);
        } else if (this instanceof RegexMatchExpression) {
            return isFieldQueried(field, (RegexMatchExpression) this, ctx);
        } else if (this instanceof NaryRelationalExpression) {
            return isFieldQueried(field, (NaryRelationalExpression) this, ctx);
        } else if (this instanceof UnaryLogicalExpression) {
            return isFieldQueried(field, (UnaryLogicalExpression) this, ctx);
        } else if (this instanceof NaryLogicalExpression) {
            return isFieldQueried(field, (NaryLogicalExpression) this, ctx);
        } else if (this instanceof ArrayContainsExpression) {
            return isFieldQueried(field, (ArrayContainsExpression) this, ctx);
        } else if (this instanceof ArrayMatchExpression) {
            return isFieldQueried(field, (ArrayMatchExpression) this, ctx);
        }
        return false;
    }

    private static boolean isFieldQueried(Path field, Path qField, Path context) {
        LOGGER.debug("Checking if field {} is included in qfield={} with context={}",field,qField,context);
        Path absField = new Path(context, qField);
        if (field.matchingPrefix(absField)) {
            LOGGER.debug("Field {} is queried", absField);
            return true;
        } else {
            LOGGER.debug("Field {} is not queried", absField);
            return false;
        }
    }

    private static boolean isFieldQueried(Path field, ValueComparisonExpression q, Path context) {
        LOGGER.debug("Checking if field {} is queried by value comparison {}", field, q);
        return isFieldQueried(field, q.getField(), context);
    }

    private static boolean isFieldQueried(Path field, FieldComparisonExpression q, Path context) {
        LOGGER.debug("Checking if field {} is queried by field comparison {}", field, q);
        return isFieldQueried(field, q.getField(), context)
                || isFieldQueried(field, q.getRfield(), context);
    }

    private static boolean isFieldQueried(Path field, RegexMatchExpression q, Path context) {
        LOGGER.debug("Checking if field {} is queried by regex {}", field, q);
        return isFieldQueried(field, q.getField(), context);
    }

    private static boolean isFieldQueried(Path field, NaryRelationalExpression q, Path context) {
        LOGGER.debug("Checking if field {} is queried by expr {}", field, q);
        return isFieldQueried(field, q.getField(), context);
    }

    private static boolean isFieldQueried(Path field, UnaryLogicalExpression q, Path context) {
        return q.getQuery().isRequired(field, context);
    }

    private static boolean isFieldQueried(Path field, NaryLogicalExpression q, Path context) {
        for (QueryExpression x : q.getQueries()) {
            if (x.isRequired(field, context)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFieldQueried(Path field, ArrayContainsExpression q, Path context) {
        LOGGER.debug("Checking if field {} is queried by array expression {}", field, q);
        return isFieldQueried(field, q.getArray(), context);
    }

    private static boolean isFieldQueried(Path field, ArrayMatchExpression q, Path context) {
        LOGGER.debug("Checking if field {} is queried by array expression {}", field, q);
        if (isFieldQueried(field, q.getArray(), context)) {
            return true;
        } else {
            return q.getElemMatch().isRequired(field, new Path(new Path(context, q.getArray()), Path.ANYPATH));
        }
    }
}
