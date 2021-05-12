/*
 * The gemma project
 * 
 * Copyright (c) 2014 University of British Columbia
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

package ubic.gemma.model.common.auditAndSecurity.eventType;

/**
 * Represents a change in permissions
 *
 * @author paul
 */
public class PermissionChangeEvent extends AuditEventType {

    private static final long serialVersionUID = -7205154783209555418L;
    
    public PermissionChangeEvent() {
        
    }
    
    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static final class Factory {

        public static ubic.gemma.model.common.auditAndSecurity.eventType.PermissionChangeEvent newInstance() {
            return new ubic.gemma.model.common.auditAndSecurity.eventType.PermissionChangeEvent();
        }

    }


}
