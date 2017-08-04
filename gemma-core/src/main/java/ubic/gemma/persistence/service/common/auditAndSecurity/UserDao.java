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

import ubic.gemma.model.common.auditAndSecurity.GroupAuthority;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;
import ubic.gemma.persistence.service.BaseDao;

import java.util.Collection;

/**
 * @author Gemma
 * @see ubic.gemma.model.common.auditAndSecurity.User
 */
public interface UserDao extends BaseDao<User> {

    void addAuthority( User user, String roleName );

    /**
     * @param password - encrypted
     */
    void changePassword( User user, String password );

    User findByEmail( String email );

    User findByUserName( String userName );

    Collection<GroupAuthority> loadGroupAuthorities( User u );

    Collection<UserGroup> loadGroups( User user );
}