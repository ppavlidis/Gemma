/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.web.controller.expression.experiment;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.web.controller.BaseMultiActionController;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="experimentalFactorController" name="/experimentalFactor/*"
 * @spring.property name = "experimentalFactorService" ref="experimentalFactorService"
 * @spring.property name="methodNameResolver" ref="experimentalFactorActions"
 */
public class ExperimentalFactorController extends BaseMultiActionController {

    private ExperimentalFactorService experimentalFactorService = null;

    private final String messagePrefix = "Experimenal factor with id ";

    /**
     * @param experimentalFactorService
     */
    public void setExperimentalFactorService( ExperimentalFactorService experimentalFactorService ) {
        this.experimentalFactorService = experimentalFactorService;
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return
     */
    @SuppressWarnings("unused")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {

        Long id = Long.parseLong( request.getParameter( "id" ) );

        if ( id == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( "Must provide an Experimental Factor name" );
        }

        ExperimentalFactor experimentalFactor = experimentalFactorService.findById( id );
        if ( experimentalFactor == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }

        this.addMessage( request, "object.found", new Object[] { messagePrefix, id } );
        request.setAttribute( "name", id );
        return new ModelAndView( "experimentalFactor.detail" ).addObject( "experimentalFactor", experimentalFactor );
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings("unused")
    public ModelAndView showAll( HttpServletRequest request, HttpServletResponse response ) {
        return new ModelAndView( "experimentalFactors" ).addObject( "experimentalFactors", experimentalFactorService
                .loadAll() );
    }

    /**
     * TODO add delete to the model
     * 
     * @param request
     * @param response
     * @return
     */
    // @SuppressWarnings("unused")
    // public ModelAndView delete(HttpServletRequest request,
    // HttpServletResponse response) {
    // String name = request.getParameter("name");
    //
    // if (name == null) {
    // // should be a validation error.
    // throw new EntityNotFoundException("Must provide a name");
    // }
    //
    // ExperimentalFactor experimentalFactor = experimentalFactorService
    // .findByName(name);
    // if (experimentalFactor == null) {
    // throw new EntityNotFoundException(experimentalFactor
    // + " not found");
    // }
    //
    // return doDelete(request, experimentalFactor);
    // }
    /**
     * TODO add doDelete to the model
     * 
     * @param request
     * @param experimentalFactor
     * @return
     */
    // private ModelAndView doDelete(HttpServletRequest request,
    // ExperimentalFactor experimentalFactor) {
    // experimentalFactorService.delete(experimentalFactor);
    // log.info("Expression Experiment with name: "
    // + experimentalFactor.getName() + " deleted");
    // addMessage(request, "experimentalFactor.deleted",
    // new Object[] { experimentalFactor.getName() });
    // return new ModelAndView("experimentalFactors",
    // "experimentalFactor", experimentalFactor);
    // }
}
