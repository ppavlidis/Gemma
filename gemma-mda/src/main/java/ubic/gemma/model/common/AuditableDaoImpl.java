/*
 * The Gemma project.
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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.common;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.Auditable
 */
public class AuditableDaoImpl extends ubic.gemma.model.common.AuditableDaoBase {
    private Log log = LogFactory.getLog( this.getClass() );
    
    /**
     * This is basically a thaw method.
     */
    @Override
    public Collection handleGetAuditEvents( final Auditable auditable ) {
        if ( auditable == null ) throw new IllegalArgumentException( "Auditable cannot be null" );
        return ( Collection ) getHibernateTemplate().execute(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        return auditable.getAuditTrail().getEvents();
                    }
                } );

    }
    
    protected ubic.gemma.model.common.auditAndSecurity.AuditEvent handleGetLastAuditEvent( Long auditableId, AuditEventType type ) throws java.lang.Exception {
        
        log.info("is this method ever called?");
        return null;
        
    }
    
    @Override
    protected ubic.gemma.model.common.auditAndSecurity.AuditEvent handleGetLastAuditEvent( final Auditable auditable, AuditEventType type ) throws java.lang.Exception {
        return handleGetLastAuditEvent( auditable.getAuditTrail(), type );
    }
    
    protected ubic.gemma.model.common.auditAndSecurity.AuditEvent handleGetLastAuditEvent( final AuditTrail auditTrail, AuditEventType type ) throws java.lang.Exception {

        //for the = operator to work in hibernate the class name can't be passed in as a parameter :type setParameter("type", type.getClass.getCanoicalName)
        //wouldn't work.  Although technically this is now vunerable to an sql injection attack, it seems moot as an attacker would have to have access to the JVM to inject
        //a malformed AuditEventType class name and if they had access to the JVM then sql injection is the least of our worries. 
        
        final String queryString = "select distinct event " +
            "from ubic.gemma.model.common.auditAndSecurity.AuditTrail trail " +
                "inner join trail.events event " +
            "where trail = :trail " +
                "and event.eventType.class = " + type.getClass().getCanonicalName() + " " +
            "order by event.date desc ";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "trail", auditTrail );
            queryObject.setMaxResults( 1 );
            
            Collection results = queryObject.list();

            if (results == null || results.isEmpty())
                return null;
            
            return ( AuditEvent ) results.iterator().next();
            
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        
    }
}