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
package ubic.gemma.web.controller.javaspaces.gigaspaces;

import java.util.concurrent.FutureTask;

import net.jini.core.lease.Lease;
import net.jini.space.JavaSpace;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.javaspaces.gigaspaces.ExpressionExperimentTaskImpl;
import ubic.gemma.util.javaspaces.GemmaSpacesProgressEntry;
import ubic.gemma.util.javaspaces.JavaSpacesJobObserver;
import ubic.gemma.util.javaspaces.gigaspaces.GemmaSpacesEnum;
import ubic.gemma.util.javaspaces.gigaspaces.GigaSpacesUtil;
import ubic.gemma.util.progress.TaskRunningService;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BackgroundProcessingFormController;
import ubic.gemma.web.util.MessageUtil;

import com.j_spaces.core.client.NotifyModifiers;

/**
 * Controllers requiring the capability to submit jobs to a compute server should extend this controller.
 * 
 * @author keshav
 * @version $Id$
 */
public abstract class AbstractGigaSpacesFormController extends BackgroundProcessingFormController {

    private GigaSpacesUtil gigaSpacesUtil = null;

    protected ApplicationContext updatedContext = null;

    /**
     * Runs the job in a {@link JavaSpace}.
     * 
     * @param jobId
     * @param securityContext
     * @param request
     * @param command
     * @param messenger
     * @return BackgroundControllerJob<ModelAndView>
     */
    abstract protected BackgroundControllerJob<ModelAndView> getSpaceRunner( String jobId,
            SecurityContext securityContex, Object command, MessageUtil messenger );

    /**
     * Controllers extending this class must implement this method. The implementation should call
     * injectGigaspacesUtil(GigaSpacesUtil gigaSpacesUtil) to "inject" a spring loaded GigaSpacesUtil into this abstract
     * class.
     * 
     * @param gigaSpacesUtil
     */
    abstract protected void setGigaSpacesUtil( GigaSpacesUtil gigaSpacesUtil );

    /**
     * @return ApplicationContext
     */
    public ApplicationContext addGigaspacesToApplicationContext() {
        if ( gigaSpacesUtil == null ) gigaSpacesUtil = new GigaSpacesUtil();

        return gigaSpacesUtil.addGigaspacesToApplicationContext( GemmaSpacesEnum.DEFAULT_SPACE.getSpaceUrl() );
    }

    /**
     * Starts the job on a compute server resource if the space is running and the task can be serviced. If runInWebapp
     * is true, the task will be run in the webapp virtual machine. If false the task will only be run if the space is
     * started and workers that can service the task exist.
     * 
     * @param command
     * @param spaceUrl
     * @param taskName
     * @param runInWebapp
     * @return {@link ModelAndView}
     */
    protected synchronized ModelAndView startJob( Object command, String spaceUrl, String taskName, boolean runInWebapp ) {
        String taskId = run( command, spaceUrl, taskName, runInWebapp );

        ModelAndView mnv = new ModelAndView( new RedirectView( "/Gemma/processProgress.html?taskid=" + taskId ) );
        mnv.addObject( JOB_ATTRIBUTE, taskId );
        return mnv;
    }

    /**
     * For use in AJAX-driven runs.
     * 
     * @param command
     * @param spaceUrl
     * @param taskName
     * @param runInWebapp
     * @return
     */
    protected String run( Object command, String spaceUrl, String taskName, boolean runInWebapp ) {
        /*
         * all new threads need this to acccess protected resources (like services)
         */
        SecurityContext context = SecurityContextHolder.getContext();

        String taskId = null;

        updatedContext = addGigaspacesToApplicationContext();
        BackgroundControllerJob<ModelAndView> job = null;
        if ( updatedContext.containsBean( "gigaspacesTemplate" ) ) {

            taskId = ( String ) ( ( ExpressionExperimentTaskImpl ) updatedContext.getBean( "taskBean" ) ).getTaskId();

            if ( !gigaSpacesUtil.canServiceTask( taskName, spaceUrl ) ) {
                // TODO Add sending of email to user.
                // User user = SecurityUtil.getUserFromUserDetails( ( UserDetails ) SecurityContextHolder.getContext()
                // .getAuthentication().getPrincipal() );
                // this.sendEmail( user, "Cannot service task " + taskName + " on the compute server at this time.",
                // "http://www.bioinformatics.ubc.ca/Gemma/" );

                throw new RuntimeException( "No workers are registered to service task "
                        + taskName.getClass().getSimpleName() + " on the compute server at this time." );
            }
            /* register this "spaces client" to receive notifications */
            JavaSpacesJobObserver javaSpacesJobObserver = new JavaSpacesJobObserver( taskId );

            GigaSpacesTemplate template = ( GigaSpacesTemplate ) updatedContext.getBean( "gigaspacesTemplate" );

            template.addNotifyDelegatorListener( javaSpacesJobObserver, new GemmaSpacesProgressEntry(), null, true,
                    Lease.FOREVER, NotifyModifiers.NOTIFY_ALL );

            job = getSpaceRunner( taskId, context, command, this.getMessageUtil() );
        } else if ( !updatedContext.containsBean( "gigaspacesTemplate" ) && !runInWebapp ) {
            throw new RuntimeException(
                    "This task must be run on the compute server, but the space is not running. Please try again later" );
        }

        else {
            taskId = TaskRunningService.generateTaskId();
            job = getRunner( taskId, context, command, this.getMessageUtil() );
        }

        assert taskId != null;

        taskRunningService.submitTask( taskId, new FutureTask<ModelAndView>( job ) );
        return taskId;
    }

    /**
     * @param gigaSpacesUtil
     */
    protected void injectGigaspacesUtil( GigaSpacesUtil gigaspacesUtil ) {
        this.gigaSpacesUtil = gigaspacesUtil;
    }
}
