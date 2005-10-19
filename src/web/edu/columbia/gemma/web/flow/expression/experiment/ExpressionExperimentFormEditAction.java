/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.web.flow.expression.experiment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.DataBinder;
import org.springframework.webflow.Event;
import org.springframework.webflow.RequestContext;
import org.springframework.webflow.ScopeType;
import org.springframework.webflow.action.FormAction;

import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentImpl;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentService;
import edu.columbia.gemma.web.flow.AbstractFlowFormAction;

/**
 * Webflow. This webflow action bean is used to handle editing of expressionExperiment form data.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @spring.bean name="expressionExperiment.Edit.formAction"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentFormEditAction extends AbstractFlowFormAction {
    protected final transient Log log = LogFactory.getLog( getClass() );
    private ExpressionExperimentService expressionExperimentService;
    private ExpressionExperiment exprExp = null;

    /**
     * Programmatically set the domain object the class is refers to, and the scope.
     */
    public ExpressionExperimentFormEditAction() {
        setFormObjectName( "expressionExperiment" );
        setFormObjectClass( ExpressionExperimentImpl.class );
        setFormObjectScope( ScopeType.FLOW );
    }

    /**
     * flowScope - attributes in the flowScope are available for the duration of the flow requestScope - attributes in
     * the requestScope are available for the duration of the request sourceEvent - this is the event that originated
     * the request. SourceEvent contains parameters provided as input by the client.
     * 
     * @param context
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object createFormObject( RequestContext context ) {

        if ( log.isInfoEnabled() ) logScopes( context );
        // String name = ( String ) context.getFlowScope().getRequiredAttribute( "name", String.class );

        String name = ( String ) context.getSourceEvent().getAttribute( "name" );
        log.debug( name );

        context.getFlowScope().setAttribute( "name", name );

        exprExp = expressionExperimentService.findByName( name );

        if ( exprExp != null ) context.getRequestScope().setAttribute( "expressionExperiment", exprExp );

        return exprExp;
    }

    /**
     * This is the webflow equivalent of mvc's formBackingObject
     * 
     * @param context
     * @param binder
     */
    @Override
    @SuppressWarnings( { "boxing", "unused" })
    protected void initBinder( RequestContext context, DataBinder binder ) {

        this.setBindOnSetupForm( true );

        log.info( ( this.isBindOnSetupForm() ) );

    }

    /**
     * @param context
     * @return
     * @throws Exception
     */
    public Event save( RequestContext context ) throws Exception {

        if ( log.isInfoEnabled() ) logScopes( context );

        exprExp.setName( ( String ) context.getSourceEvent().getAttribute( "name" ) );
        exprExp.setSource( ( String ) context.getSourceEvent().getAttribute( "source" ) );
        // exprExp.setVolume( ( String ) context.getSourceEvent().getAttribute( "volume" ) );

        log.info( "updating expression experiment reference " + exprExp.getName() );

        expressionExperimentService.updateExpressionExperiment( exprExp );

        addMessage(context, "expressionExperiment.update");
        
        return success();
    }

    private void logScopes( RequestContext context ) {

        log.info( "originating event: " + context.getSourceEvent() );
        log.info( "flow scope: " + context.getFlowScope().getAttributeMap() );
        log.info( "request scope: " + context.getRequestScope().getAttributeMap() );
    }

    /**
     * @param expressionExperimentService The expressionExperimentService to set.
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

}