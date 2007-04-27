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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Observer;

import ubic.gemma.model.common.auditAndSecurity.JobInfo;
import ubic.gemma.util.progress.ProgressData;
import ubic.gemma.util.progress.ProgressJob;

import com.j_spaces.core.client.MetaDataEntry;

/**
 * @author keshav
 * @version $Id$
 */
public class GigaspacesProgressJobImpl extends MetaDataEntry implements ProgressJob {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public ProgressData pData;
    protected JobInfo jInfo; // this obj is persisted to DB
    protected int currentPhase;
    protected String trackingId; // session id
    protected String forwardingURL;

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        // return "TrackingId: " + trackingId + " JobID: " + jInfo.getId() + " TaskID: " + jInfo.getTaskId();
        return pData.toString();
    }

    public GigaspacesProgressJobImpl() {

    }

    /**
     * The factory create method in ProgressManager is the advised way to create a ProgressJob
     * 
     * @param ownerId
     * @param description
     */
    public GigaspacesProgressJobImpl( JobInfo info, String description ) {
        this.pData = new ProgressData( 0, description, false );
        this.jInfo = info;
        currentPhase = 0;
    }

    /**
     * @return Returns the pData.
     */
    public ProgressData getProgressData() {
        return pData;
    }

    /**
     * @param data The pData to set.
     */
    public void setProgressData( ProgressData data ) {
        pData = data;
    }

    /**
     * @return Returns the runningStatus.
     */
    public boolean isRunningStatus() {
        return jInfo.getRunningStatus();
    }

    /**
     * @param runningStatus The runningStatus to set.
     */
    public void setRunningStatus( boolean runningStatus ) {
        jInfo.setRunningStatus( runningStatus );
        if ( !jInfo.getRunningStatus() ) this.pData.setDone( false );
    }

    public String getUser() {
        if ( jInfo.getUser() == null ) return null;

        return jInfo.getUser().getUserName();
    }

    /**
     * Updates the percent completion of the job by 1 percent
     */
    public void nudgeProgress() {
        pData.setPercent( pData.getPercent() + 1 );
        // setChanged();
        // notifyObservers( pData );
    }

    /**
     * Updates the progress job by a complete progressData. Used if more than the percent needs to be updates. Updating
     * the entire datapack causes the underlying dao to update its database entry for desciption only
     * 
     * @param pd
     */
    public void updateProgress( ProgressData pd ) {
        setProgressData( pd );
        setDescription( pd.getDescription() );
        updateDescriptionHistory( pd.getDescription() );
        // setChanged();
        // notifyObservers( pData );
    }

    /**
     * Upates the current progress of the job to the desired percent. doesn't change anything else.
     * 
     * @param newPercent
     */
    public void updateProgress( int newPercent ) {
        pData.setPercent( newPercent );
        // setChanged();
        // notifyObservers( pData );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.progress.ProgressJob#updateProgress(java.lang.String)
     */
    public void updateProgress( String newDescription ) {
        pData.setDescription( newDescription );
        setDescription( newDescription );
        updateDescriptionHistory( newDescription );
        // setChanged();
        // notifyObservers( pData );
    }

    /**
     * returns the id of the current job
     */
    public Long getId() {
        return jInfo.getId();
    }

    public void done() {

        Calendar cal = new GregorianCalendar();
        jInfo.setEndTime( cal.getTime() );

    }

    public int getPhase() {
        return currentPhase;

    }

    public void setPhase( int phase ) {
        if ( phase < 0 ) return;

        if ( phase > jInfo.getPhases() ) jInfo.setPhases( phase );

        currentPhase = phase;
    }

    public void setDescription( String description ) {
        this.pData.setDescription( description );
        this.jInfo.setDescription( description );
    }

    public String getDescription() {
        return this.pData.getDescription();
    }

    public JobInfo getJobInfo() {
        return this.jInfo;
    }

    /**
     * @return the anonymousId
     */
    public String getTrackingId() {
        return trackingId;
    }

    /**
     * @param anonymousId the anonymousId to set
     */
    public void setTrackingId( String trackingId ) {
        this.trackingId = trackingId;
    }

    /**
     * @return the forwardingURL
     */
    public String getForwardingURL() {
        return forwardingURL;
    }

    /**
     * @param forwardingURL the forwardingURL to set
     */
    public void setForwardingURL( String forwardingURL ) {
        this.forwardingURL = forwardingURL;
    }

    private void updateDescriptionHistory( String message ) {
        if ( this.jInfo.getMessages() == null )
            this.jInfo.setMessages( message );
        else
            this.jInfo.setMessages( this.jInfo.getMessages() + '\n' + message );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.progress.ProgressJob#addObserver(java.util.Observer)
     */
    public void addObserver( Observer O ) {
        // TODO Auto-generated method stub
    }

    // /**
    // * Implemented to programmatically allow for indexing of attributes. This indexing speeds up read and take
    // * operations.
    // *
    // * @return String[]
    // */
    // public static String[] __getSpaceIndexedFields() {
    // TODO must implement equals and hashCode in ProgressData before adding this back in.
    // String[] indexedFields = { "pData" };
    // return indexedFields;
    // }

}
