/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.analysis.expression;

/**
 * @version $Id$
 * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService
 */
public class ExpressionExperimentSetServiceImpl extends
        ubic.gemma.model.analysis.expression.ExpressionExperimentSetServiceBase {

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#create(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    protected ubic.gemma.model.analysis.expression.ExpressionExperimentSet handleCreate(
            ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet )
            throws java.lang.Exception {
        return ( ExpressionExperimentSet ) this.getExpressionExperimentSetDao().create( expressionExperimentSet );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#delete(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    protected void handleDelete( ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet )
            throws java.lang.Exception {
        this.getExpressionExperimentSetDao().remove( expressionExperimentSet );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#update(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    protected void handleUpdate( ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet )
            throws java.lang.Exception {
        this.getExpressionExperimentSetDao().update( expressionExperimentSet );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#loadAll()
     */
    protected java.util.Collection handleLoadAll() throws java.lang.Exception {
        return this.getExpressionExperimentSetDao().loadAll();

    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#loadUserSets(ubic.gemma.model.common.auditAndSecurity.User)
     */
    protected java.util.Collection handleLoadUserSets( ubic.gemma.model.common.auditAndSecurity.User user )
            throws java.lang.Exception {
        // @todo implement protected java.util.Collection
        // handleLoadUserSets(ubic.gemma.model.common.auditAndSecurity.User user)
        throw new java.lang.UnsupportedOperationException(
                "ubic.gemma.model.analysis.expression.ExpressionExperimentSetService.handleLoadUserSets(ubic.gemma.model.common.auditAndSecurity.User user) Not implemented!" );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#load(java.lang.Long)
     */
    protected ubic.gemma.model.analysis.expression.ExpressionExperimentSet handleLoad( java.lang.Long id )
            throws java.lang.Exception {
        return ( ExpressionExperimentSet ) this.getExpressionExperimentSetDao().load( id );
    }

}