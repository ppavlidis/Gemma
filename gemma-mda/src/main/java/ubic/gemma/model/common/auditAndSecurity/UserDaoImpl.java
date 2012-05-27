/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.common.auditAndSecurity;

import java.util.Collection;
import java.util.List;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateAccessor;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;

import ubic.gemma.util.BusinessKey;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.User
 */
@Repository
public class UserDaoImpl extends ubic.gemma.model.common.auditAndSecurity.UserDaoBase {

    @Autowired
    public UserDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#addAuthority(ubic.gemma.model.common.auditAndSecurity.User,
     * java.lang.String)
     */
    @Override
    public void addAuthority( User user, String roleName ) {
        throw new UnsupportedOperationException( "User group-based authority instead" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.common.auditAndSecurity.UserDao#changePassword(ubic.gemma.model.common.auditAndSecurity.User,
     * java.lang.String)
     */
    @Override
    public void changePassword( User user, String password ) {
        user.setPassword( password );
        this.getHibernateTemplate().update( user );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.UserDaoBase#find(ubic.gemma.model.common.auditAndSecurity.user)
     */
    @Override
    public User find( User user ) {
        BusinessKey.checkKey( user );
        return this.findByUserName( user.getUserName() );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByUserName(int, java.lang.String)
     */
    @Override
    public User findByUserName( final String userName ) {

        // we make this method safer to call in a transaction, as it really is a read-only method that should be
        // accessing information that is already committed.
        HibernateTemplate t = new HibernateTemplate( this.getSessionFactory() );
        t.setAlwaysUseNewSession( true );
        t.setFlushMode( HibernateAccessor.FLUSH_NEVER );
        List<?> r = t.findByNamedParam( "from UserImpl u where u.userName=:userName", "userName", userName );
        if ( r.isEmpty() ) {
            return null;
        } else if ( r.size() > 1 ) {
            throw new IllegalStateException( "Multiple users with name=" + userName );
        }
        return ( User ) r.get( 0 );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.common.auditAndSecurity.UserDao#loadGroupAuthorities(ubic.gemma.model.common.auditAndSecurity
     * .User)
     */
    @Override
    public Collection<GroupAuthority> loadGroupAuthorities( User u ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select gr.authorities from UserGroupImpl gr inner join gr.groupMembers m where m = :user ", "user", u );
    }

    @Override
    public Collection<UserGroup> loadGroups( User user ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select gr from UserGroupImpl gr inner join gr.groupMembers m where m = :user ", "user", user );
    }

}