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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalDesignService;
import ubic.gemma.web.controller.BaseMultiActionController;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="experimentalDesignController"
 *              name="/experimentalDesign/*"
 * @spring.property name = "experimentalDesignService"
 *                  ref="experimentalDesignService"
 * @spring.property name="methodNameResolver" ref="experimentalDesignActions"
 */
public class ExperimentalDesignController extends BaseMultiActionController {

	private static Log log = LogFactory
			.getLog(ExperimentalDesignController.class.getName());

	private ExperimentalDesignService experimentalDesignService = null;
    
    private final String messagePrefix = "ExperimenalDesign with id ";
    private final String identifierNotFound = "Must provide a valid ExperimentalDesign identifier";

	/**
     * 
     * @param experimentalDesignService
	 */
	public void setExperimentalDesignService(
			ExperimentalDesignService experimentalDesignService) {
		this.experimentalDesignService = experimentalDesignService;
	}

	/**
	 * @param request
	 * @param response
	 * @param errors
	 * @return ModelAndView
	 */
	@SuppressWarnings("unused")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong(request.getParameter( "id" ));

        if ( id == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( identifierNotFound );
        }

        ExperimentalDesign experimentalDesign = experimentalDesignService.findById( id );
        if ( experimentalDesign == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }

        this.addMessage( request, "object.found", new Object[] { messagePrefix, id } );
        request.setAttribute( "id", id );
        return new ModelAndView( "experimentalDesign.detail" ).addObject( "experimentalDesign", experimentalDesign );
    }
	/**
	 * @param request
	 * @param response
	 * @return
	 */
	@SuppressWarnings("unused")
	public ModelAndView showAll(HttpServletRequest request,
			HttpServletResponse response) {
		return new ModelAndView("experimentalDesigns").addObject(
				"experimentalDesigns", experimentalDesignService.loadAll());
	}

	/**
     * TODO add delete to the model
	 * @param request
	 * @param response
	 * @return
	 */
//	@SuppressWarnings("unused")
//	public ModelAndView delete(HttpServletRequest request,
//			HttpServletResponse response) {
//		String name = request.getParameter("name");
//
//		if (name == null) {
//			// should be a validation error.
//			throw new EntityNotFoundException("Must provide a name");
//		}
//
//		ExperimentalDesign experimentalDesign = experimentalDesignService
//				.findByName(name);
//		if (experimentalDesign == null) {
//			throw new EntityNotFoundException(experimentalDesign
//					+ " not found");
//		}
//
//		return doDelete(request, experimentalDesign);
//	}

	/**
	 * TODO add doDelete to the model
	 * @param request
	 * @param experimentalDesign
	 * @return
	 */
//	private ModelAndView doDelete(HttpServletRequest request,
//			ExperimentalDesign experimentalDesign) {
//		experimentalDesignService.delete(experimentalDesign);
//		log.info("Expression Experiment with name: "
//				+ experimentalDesign.getName() + " deleted");
//		addMessage(request, "experimentalDesign.deleted",
//				new Object[] { experimentalDesign.getName() });
//		return new ModelAndView("experimentalDesigns",
//				"experimentalDesign", experimentalDesign);
//	}

}
