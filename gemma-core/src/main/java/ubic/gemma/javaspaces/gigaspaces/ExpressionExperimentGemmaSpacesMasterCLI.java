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
package ubic.gemma.javaspaces.gigaspaces;

import java.rmi.RemoteException;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import net.jini.core.lease.Lease;
import net.jini.space.JavaSpace;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.apps.LoadExpressionDataCli;
import ubic.gemma.util.javaspaces.GemmaSpacesProgressEntry;
import ubic.gemma.util.javaspaces.gigaspaces.GemmaSpacesEnum;
import ubic.gemma.util.javaspaces.gigaspaces.GigaSpacesUtil;

import com.j_spaces.core.client.EntryArrivedRemoteEvent;
import com.j_spaces.core.client.ExternalEntry;
import com.j_spaces.core.client.NotifyModifiers;

/**
 * This command line interface (CLI) serves as a handy tool/test to submit a task (@see ExpressionExperimentTask) to a
 * {@link JavaSpace}. The CLI implements {@link RemoteEventListener} to be able to receive notifications from the
 * server side.
 * 
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentGemmaSpacesMasterCLI extends LoadExpressionDataCli implements RemoteEventListener {

    private static Log log = LogFactory.getLog( ExpressionExperimentGemmaSpacesMasterCLI.class );

    private GigaSpacesUtil gigaspacesUtil = null;

    private GigaSpacesTemplate template = null;

    private ExpressionExperimentTask proxy = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.apps.LoadExpressionDataCli#buildOptions()
     */
    @Override
    protected void buildOptions() {
        super.buildOptions();

    }

    /**
     * Starts the command line interface.
     * 
     * @param args
     */
    public static void main( String[] args ) {
        log.info( "Running GigaSpaces Master ... \n" );
        ExpressionExperimentGemmaSpacesMasterCLI p = new ExpressionExperimentGemmaSpacesMasterCLI();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.apps.LoadExpressionDataCli#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( this.getClass().getName(), args );
        try {

            if ( accessions == null && accessionFile == null ) {
                return new IllegalArgumentException(
                        "You must specific either a file or accessions on the command line" );
            }
            init();
            start();
        } catch ( Exception e ) {
            log.error( "Transformation error..." + e.getMessage() );
            e.printStackTrace();
        }

        return err;
    }

    /**
     * Initialization of spring beans.
     * 
     * @throws Exception
     */
    protected void init() throws Exception {

        gigaspacesUtil = ( GigaSpacesUtil ) this.getBean( "gigaSpacesUtil" );
        ApplicationContext updatedContext = gigaspacesUtil
                .addGigaspacesToApplicationContext( GemmaSpacesEnum.DEFAULT_SPACE.getSpaceUrl() );

        if ( !updatedContext.containsBean( "gigaspacesTemplate" ) )
            throw new RuntimeException( "Gigaspaces beans could not be loaded. Cannot start master." );

        template = ( GigaSpacesTemplate ) updatedContext.getBean( "gigaspacesTemplate" );

        proxy = ( ExpressionExperimentTask ) updatedContext.getBean( "proxy" );
    }

    /**
     * Submits the task to the space and retrieves the result.
     */
    protected void start() {

        log.debug( "Got accession(s) from command line " + accessions );

        GigaSpacesResult res = null;
        if ( accessions != null ) {
            String[] accsToRun = StringUtils.split( accessions, ',' );

            for ( String accession : accsToRun ) {

                log.info( "processing accession " + accession );
                StopWatch stopwatch = new StopWatch();
                stopwatch.start();

                accession = StringUtils.strip( accession );

                if ( StringUtils.isBlank( accession ) ) {
                    continue;
                }

                if ( platformOnly ) {
                    // TODO add back in
                    throw new IllegalArgumentException( "\'Platform Only\' (y) unsupported at this time." );
                    // Collection designs = geoService.fetchAndLoad( accession, true, true );
                    // for ( Object object : designs ) {
                    // assert object instanceof ArrayDesign;
                    // successObjects.add( ( ( Describable ) object ).getName()
                    // + " ("
                    // + ( ( ArrayDesign ) object ).getExternalReferences().iterator().next()
                    // .getAccession() + ")" );
                    // }
                } else {

                    /* configure this client to be receive notifications */
                    try {

                        template.addNotifyDelegatorListener( this, new GemmaSpacesProgressEntry(), null, true,
                                Lease.FOREVER, NotifyModifiers.NOTIFY_ALL );

                    } catch ( Exception e ) {
                        throw new RuntimeException( e );
                    }

                    if ( !gigaspacesUtil.canServiceTask( ExpressionExperimentTask.class.getName(),
                            GemmaSpacesEnum.DEFAULT_SPACE.getSpaceUrl() ) ) continue;

                    res = proxy.execute( accession, platformOnly, doMatching, aggressive );

                    stopwatch.stop();
                    long wt = stopwatch.getTime();
                    log.info( "Job with id " + res.getTaskID() + " completed in " + wt
                            + " ms.  Number of expression experiments persisted: " + res.getAnswer() + "." );
                }
            }

            /*
             * Terminate the VM after you get the result. This is needed else the VM will wait for the timeout millis
             * that is set in the spring context.
             */
            System.exit( 0 );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.apps.LoadExpressionDataCli#processOptions()
     */
    @Override
    protected void processOptions() {
        super.processOptions();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.jini.core.event.RemoteEventListener#notify(net.jini.core.event.RemoteEvent)
     */
    public void notify( RemoteEvent remoteEvent ) throws UnknownEventException, RemoteException {
        log.debug( "notified ..." );

        try {
            EntryArrivedRemoteEvent arrivedRemoteEvent = ( EntryArrivedRemoteEvent ) remoteEvent;

            log.debug( "event: " + arrivedRemoteEvent );
            ExternalEntry entry = ( ExternalEntry ) arrivedRemoteEvent.getEntry( true );
            log.debug( "entry: " + entry );
            log.debug( "id: " + arrivedRemoteEvent.getID() );
            log.debug( "sequence number: " + arrivedRemoteEvent.getSequenceNumber() );
            log.debug( "notify type: " + arrivedRemoteEvent.getNotifyType() );

            String message = ( String ) entry.getFieldValue( "message" );
            log.info( "message: " + message );

        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

    }
}
