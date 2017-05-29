/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author paul
 */
public interface AuditEventService {

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    List<AuditEvent> getEvents( Auditable auditable );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    AuditEvent getLastEvent( Auditable auditable, Class<? extends AuditEventType> type );

    /**
     * Fast method to retrieve auditEventTypes of multiple classes.
     *
     * @return map of AuditEventType to a Map of Auditable to the AuditEvent matching that type.
     * Note: cannot secure this very easily since map key is a Class.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> getLastEvents(
            Collection<? extends Auditable> auditables, Collection<Class<? extends AuditEventType>> types );

    /**
     * Returns a collection of Auditables created since the date given.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    java.util.Collection<Auditable> getNewSinceDate( java.util.Date date );

    /**
     * Returns a collection of Auditable objects that were updated since the date entered.
     * Note that this security setting works even though auditables aren't necessarily securable; non-securable
     * auditables will be returned. See AclEntryAfterInvocationCollectionFilteringProvider and
     * applicationContext-security.xml
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<Auditable> getUpdatedSinceDate( java.util.Date date );

    boolean hasEvent( Auditable a, Class<? extends AuditEventType> type );

    void retainHavingEvent( Collection<? extends Auditable> a, Class<? extends AuditEventType> type );

    void retainLackingEvent( Collection<? extends Auditable> a, Class<? extends AuditEventType> type );

}
