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
package ubic.gemma.web.controller;

import java.util.concurrent.FutureTask;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.security.authentication.ManualAuthenticationService;
import ubic.gemma.util.progress.TaskRunningService;
import ubic.gemma.web.util.MessageUtil;

/**
 * Extends this when the controller needs to run a long task (show a progress bar). To use it, implement getRunner and
 * call startJob in your onSubmit method.
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class BackgroundProcessingFormController extends BaseFormController {

    @Autowired
    protected TaskRunningService taskRunningService;

    @Autowired
    private ManualAuthenticationService manualAuthenticationService;

    /**
     * This method can be exposed via AJAX to allow asynchronous calls
     * 
     * @param command The command object containing parameters.
     * @return the task id.
     */
    public String run( Object command ) {

        String taskId = TaskRunningService.generateTaskId();

        BackgroundControllerJob<ModelAndView> job = getRunner( taskId, command, this.getMessageUtil() );

        assert taskId != null;

        taskRunningService.submitTask( taskId, new FutureTask<ModelAndView>( job ) );
        return taskId;
    }

    /**
     * @param taskRunningService the taskRunningService to set
     */
    public void setTaskRunningService( TaskRunningService taskRunningService ) {
        this.taskRunningService = taskRunningService;
    }

    /**
     * You have to implement this in your subclass.
     * 
     * @param jobId a unique job identifier that is used to retrieve results and status information about the job.
     * @param command from form
     * @return
     */
    protected abstract BackgroundControllerJob<ModelAndView> getRunner( String jobId, Object command,
            MessageUtil messenger );

    /**
     * @param command
     * @param request
     * @returns a model and view
     */
    protected synchronized ModelAndView startJob( Object command ) {
        String taskId = run( command );

        ModelAndView mnv = new ModelAndView( new RedirectView( "/Gemma/processProgress.html?taskid=" + taskId ) );
        mnv.addObject( "taskId", taskId );
        return mnv;
    }

    protected void provideAuthentication() {
        if ( SecurityContextHolder.getContext().getAuthentication() == null ) {
            manualAuthenticationService.authenticateAnonymously();
        }
    }
}
