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
package ubic.gemma.web.controller.expression.bioAssay;

import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.web.controller.BaseMultiActionController;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="bioAssayController"
 * @spring.property name = "bioAssayService" ref="bioAssayService"
 * @spring.property name="methodNameResolver" ref="bioAssayActions"
 */
public class BioAssayController extends BaseMultiActionController {

    private static Log log = LogFactory.getLog( BioAssayController.class.getName() );

    private BioAssayService bioAssayService = null;

    private final String messagePrefix = "BioAssay with id ";
    private final String identifierNotFound = "Must provide a valid BioAssay identifier";

    /**
     * @param bioAssayService
     */
    public void setBioAssayService( BioAssayService bioAssayService ) {
        this.bioAssayService = bioAssayService;
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {

        log.debug( request.getParameter( "id" ) );

        Long id = Long.parseLong( request.getParameter( "id" ) );

        if ( id == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( identifierNotFound );
        }

        BioAssay bioAssay = bioAssayService.findById( id );
        if ( bioAssay == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }
        

        this.addMessage( request, "object.found", new Object[] { messagePrefix, id } );
        request.setAttribute( "id", id );
        return new ModelAndView( "bioAssay.detail" ).addObject( "bioAssay", bioAssay );
    }

    /**
     * @param request
     * @param response
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView showAll( HttpServletRequest request, HttpServletResponse response ) {
        String sId = request.getParameter( "id" );
        Collection<BioAssay> bioAssays = new ArrayList<BioAssay>();
        if ( sId == null ) {
            bioAssays = bioAssayService.loadAll();
        }
        else {
            String[] idList = StringUtils.split( sId, ',' );
            for (int i = 0; i < idList.length; i++) {
                Long id = Long.parseLong( idList[i] );
                BioAssay bioAssay = bioAssayService.findById( id );
                if ( bioAssay == null ) {
                    throw new EntityNotFoundException( id + " not found" );
                }
                bioAssays.add( bioAssay );
            }
        }
        return new ModelAndView( "bioAssays" ).addObject( "bioAssays", bioAssays );
    }

    /**
     * TODO add delete to the model
     * 
     * @param request
     * @param response
     * @return ModelAndView
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
    // BioAssay bioAssay = bioAssayService
    // .findByName(name);
    // if (bioAssay == null) {
    // throw new EntityNotFoundException(bioAssay
    // + " not found");
    // }
    //
    // return doDelete(request, bioAssay);
    // }
    /**
     * TODO add doDelete to the model
     * 
     * @param request
     * @param bioAssay
     * @return ModelAndView
     */
    // private ModelAndView doDelete(HttpServletRequest request,
    // BioAssay bioAssay) {
    // bioAssayService.delete(bioAssay);
    // log.info("Expression Experiment with name: "
    // + bioAssay.getName() + " deleted");
    // addMessage(request, "bioAssay.deleted",
    // new Object[] { bioAssay.getName() });
    // return new ModelAndView("bioAssays",
    // "bioAssay", bioAssay);
    // }
}
