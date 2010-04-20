/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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

package ubic.gemma.security.authorization.acl;

import javax.sql.DataSource;

import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.Sid;

/**
 * Subclass to support some additional functionality we need that JdbcMutableAclService does not implement.
 * 
 * @author paul
 * @version $Id$
 */
public class AclService extends JdbcMutableAclService {

    /**
     * @param dataSource
     * @param lookupStrategy
     * @param aclCache
     */
    public AclService( DataSource dataSource, LookupStrategy lookupStrategy, AclCache aclCache ) {
        super( dataSource, lookupStrategy, aclCache );
    }

    /**
     * Remove a sid and all associated ACEs.
     * 
     * @param sid
     */
    public void deleteSid( Sid sid ) {

        Long sidId = super.createOrRetrieveSidPrimaryKey( sid, false );

        String deleteAces = "delete e from acl_entry e inner join acl_sid s on s.id=e.sid where s.sid = ?";
        jdbcTemplate.update( deleteAces, new Object[] { sidId } );

        String deleteSid = "delete from acl_sid where id = ?";
        jdbcTemplate.update( deleteSid, new Object[] { sidId } );
        

    }

}
