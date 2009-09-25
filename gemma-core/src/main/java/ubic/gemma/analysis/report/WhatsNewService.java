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
package ubic.gemma.analysis.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import ubic.basecode.util.FileTools;
import ubic.gemma.datastructure.AuditableObject;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEventService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.security.SecurityService;
import ubic.gemma.util.ConfigUtils;

/**
 * Service to collect data on object that are new in the system.
 * 
 * @spring.bean id="whatsNewService"
 * @spring.property name="auditEventService" ref="auditEventService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 * @spring.property name="cacheManager" ref="cacheManager"
 * @spring.property name="securityService" ref="securityService"
 * @author pavlidis
 * @version $Id$
 */
public class WhatsNewService implements InitializingBean {

    private static Log log = LogFactory.getLog( WhatsNewService.class.getName() );

    ArrayDesignService arrayDesignService = null;
    AuditEventService auditEventService;
    ExpressionExperimentService expressionExperimentService = null;
    SecurityService securityService = null;
    private CacheManager cacheManager = null;

    private String HOME_DIR = ConfigUtils.getString( "gemma.appdata.home" );

    private String WHATS_NEW_CACHE = "WhatsNew";
    private String WHATS_NEW_DIR = "WhatsNew";
    private String WHATS_NEW_FILE = "WhatsNew";
    private Cache whatsNewCache;

    public void afterPropertiesSet() throws Exception {
        try {

            if ( cacheManager.cacheExists( WHATS_NEW_CACHE ) ) {
                return;
            }

            // last two values are timetolive and timetoidle.
            whatsNewCache = new Cache( WHATS_NEW_CACHE, 1500, false, false, 12 * 3600, 12 * 3600 );

            cacheManager.addCache( whatsNewCache );
            whatsNewCache = cacheManager.getCache( WHATS_NEW_CACHE );

        } catch ( CacheException e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * save the report from last week. This will be the report that will be used by the WhatsNew box.
     * 
     * @param date
     */
    public void generateWeeklyReport() {
        Calendar c = Calendar.getInstance();
        Date date = c.getTime();
        date = DateUtils.addDays( date, -7 );
        saveReport( date );
    }

    /**
     * @param date
     * @return representing the updated or new objects.
     */
    @SuppressWarnings("unchecked")
    public WhatsNew getReport( Date date ) {
        WhatsNew wn = new WhatsNew( date );
        wn.setUpdatedObjects( auditEventService.getUpdatedSinceDate( date ) );
        log.info( wn.getUpdatedObjects().size() + " updated objects since " + date );
        wn.setNewObjects( auditEventService.getNewSinceDate( date ) );
        log.info( wn.getNewObjects().size() + " new objects since " + date );
        return wn;
    }

    /**
     * Retrieve the latest WhatsNew report.
     * 
     * @return WhatsNew the latest WhatsNew report cache.
     */
    public WhatsNew retrieveReport() {
        WhatsNew wn = new WhatsNew();
        try {
            File newObjects = new File( HOME_DIR + File.separatorChar + WHATS_NEW_DIR + File.separatorChar
                    + WHATS_NEW_FILE + ".new" );
            File updatedObjects = new File( HOME_DIR + File.separatorChar + WHATS_NEW_DIR + File.separatorChar
                    + WHATS_NEW_FILE + ".updated" );
            if ( !newObjects.exists() && !updatedObjects.exists() ) {
                return null;
            }

            // load up all new objects
            if ( newObjects.exists() ) {
                Collection<AuditableObject> aos = loadAuditableObjects( newObjects );

                for ( AuditableObject object : aos ) {
                    Auditable auditable = fetch( wn, object );

                    if ( auditable == null ) continue;

                    wn.addNewObjects( auditable );
                    updateDate( wn, object );
                }

            }

            // load up all updated objects
            if ( updatedObjects.exists() ) {
                Collection<AuditableObject> aos = loadAuditableObjects( updatedObjects );
                for ( AuditableObject object : aos ) {
                    Auditable auditable = fetch( wn, object );

                    if ( auditable == null ) continue;

                    wn.addUpdatedObjects( auditable );
                    updateDate( wn, object );
                }
            }
        } catch ( Throwable e ) {
            log.error( e, e );
            return null;
        }
        return wn;
    }

    /**
     * save the report from the date specified. This will be the report that will be used by the WhatsNew box.
     * 
     * @param date
     */
    public void saveReport( Date date ) {
        WhatsNew wn = getReport( date );
        initDirectories( true );
        saveFile( wn );
    }

    /**
     * @param arrayDesignService the arrayDesignService to set
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    public void setAuditEventService( AuditEventService auditEventService ) {
        this.auditEventService = auditEventService;
    }

    /**
     * @param cacheManager the cacheManager to set
     */
    public void setCacheManager( CacheManager cacheManager ) {
        this.cacheManager = cacheManager;
    }

    /**
     * @param expressionExperimentService the expressionExperimentService to set
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param securityService the securityService to set
     */
    public void setSecurityService( SecurityService securityService ) {
        this.securityService = securityService;
    }

    /**
     * @param wn
     * @param object
     * @return
     */
    private Auditable fetch( WhatsNew wn, AuditableObject object ) {
        Auditable auditable = null;
        Element element = this.whatsNewCache.get( object );
        if ( object.type.equalsIgnoreCase( "ArrayDesign" ) ) {
            if ( element != null ) {
                auditable = ( Auditable ) element.getValue();
            } else {
                auditable = arrayDesignService.load( object.getId() );
                whatsNewCache.put( new Element( object, auditable ) );
            }

        } else if ( object.type.equalsIgnoreCase( "ExpressionExperiment" ) ) {
            if ( element != null ) {
                auditable = ( Auditable ) element.getValue();
            } else {
                // this is slower than loading them all at once but the cache saves even more time.
                auditable = expressionExperimentService.load( object.getId() );
                boolean isPrivate = securityService.isPrivate( auditable );

                /*
                 * Don't list private experiments is being new.
                 */
                if ( isPrivate ) return null;

                whatsNewCache.put( new Element( object, auditable ) );
            }
        }
        return auditable;
    }

    /**
     * @param deleteFiles
     */
    private void initDirectories( boolean deleteFiles ) {
        // check to see if the home directory exists. If it doesn't, create it.
        // check to see if the reports directory exists. If it doesn't, create it.
        FileTools.createDir( HOME_DIR );
        FileTools.createDir( HOME_DIR + File.separatorChar + WHATS_NEW_DIR );
        File f = new File( HOME_DIR + File.separatorChar + WHATS_NEW_DIR );
        Collection<File> files = new ArrayList<File>();
        File[] fileArray = f.listFiles();
        for ( File file : fileArray ) {
            files.add( file );
        }
        // clear out all files
        if ( deleteFiles ) {
            FileTools.deleteFiles( files );
        }
    }

    /**
     * @param newObjects
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    private Collection<AuditableObject> loadAuditableObjects( File newObjects ) throws FileNotFoundException,
            IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream( newObjects );
        ObjectInputStream ois = new ObjectInputStream( fis );
        Collection<AuditableObject> aos = ( Collection<AuditableObject> ) ois.readObject();
        ois.close();
        fis.close();
        return aos;
    }

    /**
     * @param wn
     * @return
     */
    private boolean saveFile( WhatsNew wn ) {
        try {
            // remove file first
            File newOutput = new File( HOME_DIR + File.separatorChar + WHATS_NEW_DIR + File.separatorChar
                    + WHATS_NEW_FILE + ".new" );
            File updatedOutput = new File( HOME_DIR + File.separatorChar + WHATS_NEW_DIR + File.separatorChar
                    + WHATS_NEW_FILE + ".updated" );
            if ( newOutput.exists() ) {
                newOutput.delete();
            }
            if ( updatedOutput.exists() ) {
                updatedOutput.delete();
            }
            Calendar c = Calendar.getInstance();
            Date date = c.getTime();

            Collection<ArrayDesign> ads = wn.getNewArrayDesigns();
            Collection<ExpressionExperiment> ees = wn.getNewExpressionExperiments();
            // save the IDs for new Auditables
            Collection<AuditableObject> newObjects = new ArrayList<AuditableObject>();
            for ( ArrayDesign ad : ads ) {
                AuditableObject ao = new AuditableObject();
                ao.date = date;
                ao.type = "ArrayDesign";
                ao.id = ad.getId();
                newObjects.add( ao );
            }
            for ( ExpressionExperiment ee : ees ) {
                AuditableObject ao = new AuditableObject();
                ao.date = date;
                ao.type = "ExpressionExperiment";
                ao.id = ee.getId();
                newObjects.add( ao );
            }

            // save the ids for updated Auditables
            ads = wn.getUpdatedArrayDesigns();
            ees = wn.getUpdatedExpressionExperiments();
            // save the IDs for new Auditables
            Collection<AuditableObject> updatedObjects = new ArrayList<AuditableObject>();
            for ( ArrayDesign ad : ads ) {
                AuditableObject ao = new AuditableObject();
                ao.date = date;
                ao.type = "ArrayDesign";
                ao.id = ad.getId();
                updatedObjects.add( ao );
            }
            for ( ExpressionExperiment ee : ees ) {
                AuditableObject ao = new AuditableObject();
                ao.date = date;
                ao.type = "ExpressionExperiment";
                ao.id = ee.getId();
                updatedObjects.add( ao );
            }
            FileOutputStream fos = new FileOutputStream( newOutput );
            ObjectOutputStream oos = new ObjectOutputStream( fos );
            oos.writeObject( newObjects );
            oos.flush();
            oos.close();

            fos = new FileOutputStream( updatedOutput );
            oos = new ObjectOutputStream( fos );
            oos.writeObject( updatedObjects );
            oos.flush();
            oos.close();
        } catch ( Throwable e ) {
            return false;
        }

        return true;
    }

    /**
     * Sets the date to the earliest update date of any object that has been retrieved so far.
     * 
     * @param wn
     * @param object
     */
    private void updateDate( WhatsNew wn, AuditableObject object ) {
        if ( object.getDate() != null && ( wn.getDate() == null || wn.getDate().after( object.getDate() ) ) ) {
            wn.setDate( object.getDate() );
        }
    }

}
