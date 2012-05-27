/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.web.controller.common.auditAndSecurity;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.Constants;
import ubic.gemma.security.SecurityServiceImpl;
import ubic.gemma.web.controller.WebConstants;

/**
 * Performs actions required when we wish to indicate that the system is undergoing maintenance and many not behave
 * normally.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Controller
@RequestMapping("/admin/maintenanceMode.html")
public class MaintenanceModeController {

    @RequestMapping(method = RequestMethod.GET)
    public String getForm() {
        return "admin/maintenanceMode";
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(method = RequestMethod.POST)
    public ModelAndView setMode( String stop, String start, HttpServletRequest request ) throws Exception {
        
        Map<String, Object> config = ( Map<String, Object> ) request.getSession().getServletContext().getAttribute( Constants.CONFIG );

        //check that the user is admin!
        
        boolean isAdmin = SecurityServiceImpl.isUserAdmin();

        if ( !isAdmin ) {
            throw new AccessDeniedException( "Attempt by non-admin to alter system maintenance mode status!" );
        }

        if ( StringUtils.isNotBlank( start ) && StringUtils.isNotBlank( stop ) ) {
            throw new IllegalArgumentException( "Can't decide whether to stop or start" );
        }

        if ( StringUtils.isNotBlank( stop ) ) {
            config.put( "maintenanceMode", false );
        } else if ( StringUtils.isNotBlank( start ) ) {
            config.put( "maintenanceMode", true );
        }
        return new ModelAndView( new RedirectView( WebConstants.HOME_URL ) );

    }
}
