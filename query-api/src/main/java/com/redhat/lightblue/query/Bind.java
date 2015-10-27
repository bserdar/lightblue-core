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

import java.util.List;
import java.util.Set;
import java.util.ArrayList;

import com.redhat.lightblue.util.Path;
import com.redhat.lightblue.util.Error;

public class Bind extends QueryIterator {

    private static int BIND_LEFT=0x01;
    private static int BIND_RIGHT=0x02;
    
    protected List<FieldBinding> bindingResult;
    protected Set<Path> bindRequest;
    
    public Bind(List<FieldBinding> bindingResult,
                Set<Path> bindRequest) {
        this.bindingResult = bindingResult;
        this.bindRequest = bindRequest;
    }
    
    private QueryExpression checkError(QueryExpression q, Path field, Path ctx) {
        if (bindRequest.contains(new Path(ctx, field))) {
            throw Error.get(QueryConstants.ERR_INVALID_VALUE_BINDING, q.toString());
        }
        return q;
    }
    
    @Override
    protected QueryExpression itrArrayContainsExpression(ArrayContainsExpression q, Path ctx) {
        return checkError(q, q.getArray(), ctx);
    }
    
    @Override
    protected QueryExpression itrValueComparisonExpression(ValueComparisonExpression q, Path ctx) {
        return checkError(q, q.getField(), ctx);
    }
    
    @Override
    protected QueryExpression itrRegexMatchExpression(RegexMatchExpression q, Path ctx) {
        return checkError(q, q.getField(), ctx);
    }
    
    @Override
    protected QueryExpression itrNaryValueRelationalExpression(NaryValueRelationalExpression q, Path ctx) {
        return checkError(q, q.getField(), ctx);
    }

    /**
     * Rewrites:
     * <pre>
     *   { field: lfield, op: <op>, rfield }
     * </pre>
     * as
     * <pre>
     *   { field: lfield | rfield, op:<op>, values: [valueList] }
     * </pre>
     * where valueList is a bound value that will be set once the values are known
     */
    @Override
    protected QueryExpression itrNaryFieldRelationalExpression(NaryFieldRelationalExpression q, Path ctx) {
        return rewrite(q,ctx,bindRequest,bindingResult);
    }

    private static QueryExpression rewrite(NaryFieldRelationalExpression q, Path ctx, Set<Path> bindRequest,List<FieldBinding> bindingResult) {
        QueryExpression newq=q;
        int blr=getBindLeftRight(q,ctx,bindRequest);
        if (blr==(BIND_LEFT|BIND_RIGHT)) {
            throw Error.get(QueryConstants.ERR_INVALID_VALUE_BINDING, q.toString());
        }
        if (blr!=0) {
            // If we're here, only one of the fields is bound
            if ((blr&BIND_RIGHT)!=0) {    
                BoundValueList newValue = new BoundValueList();
                newq = new NaryValueRelationalExpression(q.getField(), q.getOp(), newValue);
                bindingResult.add(new ListBinding(new Path(ctx,q.getRfield()), newValue, q, newq));
            } else {
                BoundValue newValue=new BoundValue();
                List<Value> list=new ArrayList<>(1);
                list.add(newValue);
                newq = new ArrayContainsExpression(q.getRfield(), q.getOp()==NaryRelationalOperator._in?
                                                   ContainsOperator._all:ContainsOperator._none, 
                                                   list);
                bindingResult.add(new ValueBinding(new Path(ctx,q.getField()), newValue, q, newq));
            }                
        }
        return newq; 
    }
    
    /**
     * Rewrites
     * <pre>
     *   { field: lfield, op:<op>, rfield: rfield }
     * </pre>
     *  as
     * <pre>
     *  { field: lfield or rfield, op:<op>, rvalue: value }
     * </pre>
     * 
     */
    @Override
    protected QueryExpression itrFieldComparisonExpression(FieldComparisonExpression q, Path ctx) {
        return rewrite(q,ctx,bindRequest,bindingResult);
    }

    private static QueryExpression rewrite(FieldComparisonExpression q, Path ctx,Set<Path> bindRequest,List<FieldBinding> bindingResult) {
        QueryExpression newq=q;
        int blr=getBindLeftRight(q,ctx,bindRequest);
        if (blr==(BIND_LEFT|BIND_RIGHT)) {
            throw Error.get(QueryConstants.ERR_INVALID_VALUE_BINDING, q.toString());
        }
        if (blr!=0) {
            // If we're here, only one of the fields is bound
            BoundValue newValue = new BoundValue();
            if ((blr&BIND_RIGHT)!=0) {
                newq = new ValueComparisonExpression(q.getField(), q.getOp(), newValue);
                bindingResult.add(new ValueBinding(new Path(ctx,q.getRfield()), newValue, q, newq));
            } else {
                newq = new ValueComparisonExpression(q.getRfield(), q.getOp().invert(), newValue);
                bindingResult.add(new ValueBinding(new Path(ctx,q.getField()), newValue, q, newq));
            }
        }
        return newq;
    }

    private static int getBindLeftRight(FieldRelationalExpression expr, Path ctx,Set<Path> bindRequest) {
        Path l = new Path(ctx, expr.getField());
        Path r = new Path(ctx, expr.getRfield());
        boolean bindl = bindRequest.contains(l);
        boolean bindr = bindRequest.contains(r);
        return (bindl?BIND_LEFT:0) | (bindr?BIND_RIGHT:0);
    }

    /**
     * Binding an array match expression may require changes to the query structure.
     * <pre>
     *  { array: A, elemMatch: { field: X, rfield: Y} }
     *
     *  Bind A.*.X:
     *     A.*.X is under A, A.*.Y not under A: then { field:Y, rvalue: <bound value> } (type 1)
     *     A.*.X is under A, A.*.Y under A: then: invalid
     *     A.*.X is not under A, A.*.Y not under A: invalid
     *     A.*.X is not under A, A.*.Y under A: then { array: A, elemMatch: { field: Y, rvalue: <bound value> } } (type 2)
     * Bind A.*.Y:
     *     A.*.X is under A, A.*.Y not under A: then { array: A, elemMatch: { field:X, rvalue: <bound value> }
     *     A.*.X is under A, A.*.Y under A: then: invalid
     *     A.*.X is not under A, A.*.Y not under A: invalid
     *     A.*.X is not under A, A.*.Y under A: then : { field: X, rvalue: <bound value> } }
     *
     * If the expression is not a trivial expression:
     * { array: A, elemMatch: { $and: [ X, Y, ..] } }
     *   - If all bindings X, Y are of the same type, apply the conversion, otherwise invalid
     *  All bindings being of the same type means: all clauses are either not bound, or they are of type 1 or 2
     *   
     * </pre>
     *
     * TODO: This is not how it ought to be done. During query planning, we should prepare all bindable queries, and associate
     * them to nodes/edges.
     */
    @Override
    protected QueryExpression itrArrayMatchExpression(ArrayMatchExpression q, Path ctx) {
        checkError(q, q.getArray(), ctx);
        NestedBindIterator nbi=new NestedBindIterator(bindingResult,bindRequest);
        QueryExpression newq=nbi.iterate(q.getElemMatch(),new Path(new Path(ctx,q.getArray()),Path.ANYPATH));
        if(nbi.removeArray!=null&&nbi.removeArray) {
            return newq;
        } else {
            if(newq == q.getElemMatch())
                return q;
            else
                return new ArrayMatchExpression(q.getArray(),newq);
        }
    }


    private static class NestedBindIterator extends Bind {

        public Boolean removeArray=null;

        public NestedBindIterator(List<FieldBinding> bindingResult,
                                  Set<Path> bindRequest) {
            super(bindingResult,bindRequest);
        }
        
        @Override
        protected QueryExpression itrNaryFieldRelationalExpression(NaryFieldRelationalExpression q, Path ctx) {
            QueryExpression newq=q;
            int blr=getBindLeftRight(q,ctx,bindRequest);
            if (blr==(BIND_LEFT|BIND_RIGHT)) {
                throw Error.get(QueryConstants.ERR_INVALID_VALUE_BINDING, q.toString());
            } else if(blr==0) {
                return q;
            } else {
                int ctxLength=ctx.numSegments();
                // Only one field is bound
                Path l=new Path(ctx,q.getField()).normalize();
                boolean lUnderCtx=l.numSegments()>ctxLength && l.prefix(ctxLength).equals(ctx);
                Path r=new Path(ctx,q.getRfield()).normalize();
                boolean rUnderCtx=r.numSegments()>ctxLength && r.prefix(ctxLength).equals(ctx);
                if(!lUnderCtx&&!rUnderCtx ) {
                    throw Error.get(QueryConstants.ERR_INVALID_VALUE_BINDING,q.toString());
                }
                    
                if( (blr&BIND_LEFT)!=0 ) {
                    if(lUnderCtx) {
                        if(removeArray==null)
                            removeArray=true;
                        else if(!removeArray)
                            throw Error.get(QueryConstants.ERR_INVALID_VALUE_BINDING,q.toString());
                    } else { // !lUnderCtx
                        if(removeArray==null)
                            removeArray=false;
                        else if(removeArray)
                            throw Error.get(QueryConstants.ERR_INVALID_VALUE_BINDING,q.toString());
                        
                    }
                    BoundValue newValue=new BoundValue();
                    List<Value> list=new ArrayList<>(1);
                    list.add(newValue);
                    newq = new ArrayContainsExpression(q.getRfield(), q.getOp()==NaryRelationalOperator._in?
                                                       ContainsOperator._all:ContainsOperator._none, 
                                                       list);
                    bindingResult.add(new ValueBinding(l, newValue, q, newq));
                } else {
                    // Binding rfield
                    if(rUnderCtx) {
                        if(removeArray==null)
                            removeArray=true;
                        else if(!removeArray)
                            throw Error.get(QueryConstants.ERR_INVALID_VALUE_BINDING,q.toString());
                    } else {
                        if(removeArray==null)
                            removeArray=false;
                        else if(removeArray)
                            throw Error.get(QueryConstants.ERR_INVALID_VALUE_BINDING,q.toString());
                    }
                    BoundValueList newValue = new BoundValueList();
                    newq = new NaryValueRelationalExpression(q.getField(), q.getOp(), newValue);
                    bindingResult.add(new ListBinding(r, newValue, q, newq));
                }
            }
            return newq;            
        }
        
        @Override
        protected QueryExpression itrFieldComparisonExpression(FieldComparisonExpression q, Path ctx) {
            QueryExpression newq=q;
            int blr=getBindLeftRight(q,ctx,bindRequest);
            if (blr==(BIND_LEFT|BIND_RIGHT)) {
                throw Error.get(QueryConstants.ERR_INVALID_VALUE_BINDING, q.toString());
            } else if(blr==0) {
                return q;
            } else {
                int ctxLength=ctx.numSegments();
                // Only one field is bound
                Path l=new Path(ctx,q.getField()).normalize();
                boolean lUnderCtx=l.numSegments()>ctxLength && l.prefix(ctxLength).equals(ctx);
                Path r=new Path(ctx,q.getRfield()).normalize();
                boolean rUnderCtx=r.numSegments()>ctxLength && r.prefix(ctxLength).equals(ctx);
                if(!lUnderCtx&&!rUnderCtx ) {
                    throw Error.get(QueryConstants.ERR_INVALID_VALUE_BINDING,q.toString());
                }
                BoundValue newValue = new BoundValue();
                if( (blr&BIND_LEFT)!=0 ) {
                    if(lUnderCtx) {
                        if(removeArray==null)
                            removeArray=true;
                        else if(!removeArray)
                            throw Error.get(QueryConstants.ERR_INVALID_VALUE_BINDING,q.toString());
                    } else { // !lUnderCtx
                        if(removeArray==null)
                            removeArray=false;
                        else if(removeArray)
                            throw Error.get(QueryConstants.ERR_INVALID_VALUE_BINDING,q.toString());
                        
                    }
                    newq = new ValueComparisonExpression(q.getRfield(), q.getOp().invert(), newValue);
                    bindingResult.add(new ValueBinding(new Path(ctx,q.getField()), newValue, q, newq));
                } else {
                    // Binding rfield
                    if(rUnderCtx) {
                        if(removeArray==null)
                            removeArray=true;
                        else if(!removeArray)
                            throw Error.get(QueryConstants.ERR_INVALID_VALUE_BINDING,q.toString());
                    } else {
                        if(removeArray==null)
                            removeArray=false;
                        else if(removeArray)
                            throw Error.get(QueryConstants.ERR_INVALID_VALUE_BINDING,q.toString());
                   }
                    newq = new ValueComparisonExpression(q.getField(), q.getOp(), newValue);
                    bindingResult.add(new ValueBinding(new Path(ctx,q.getRfield()), newValue, q, newq));
                }
            }
            return newq;
        }
    }
    
}
