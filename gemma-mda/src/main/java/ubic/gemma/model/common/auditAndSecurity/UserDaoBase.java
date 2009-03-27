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
package ubic.gemma.model.common.auditAndSecurity;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.auditAndSecurity.User</code>.
 * </p>
 * 
 * @see ubic.gemma.model.common.auditAndSecurity.User
 */
public abstract class UserDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.common.auditAndSecurity.UserDao {

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#create(int, java.util.Collection)
     */

    public java.util.Collection<User> create( final int transform, final java.util.Collection<User> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "User.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<User> entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform, entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#create(int transform,
     *      ubic.gemma.model.common.auditAndSecurity.User)
     */
    public User create( final int transform, final ubic.gemma.model.common.auditAndSecurity.User user ) {
        if ( user == null ) {
            throw new IllegalArgumentException( "User.create - 'user' can not be null" );
        }
        this.getHibernateTemplate().save( user );
        return ( User ) this.transformEntity( transform, user );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#create(java.util.Collection)
     */
    public java.util.Collection<User> create( final java.util.Collection<User> entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#create(ubic.gemma.model.common.auditAndSecurity.User)
     */
    public User create( ubic.gemma.model.common.auditAndSecurity.User user ) {
        return this.create( TRANSFORM_NONE, user );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByEmail(int, java.lang.String)
     */
    public User findByEmail( final int transform, final java.lang.String email ) {
        return this.findByEmail( transform, "from UserImpl c where c.email = :email", email );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByEmail(int, java.lang.String, java.lang.String)
     */

    @SuppressWarnings( { "unchecked" })
    public User findByEmail( final int transform, final java.lang.String queryString, final java.lang.String email ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( email );
        argNames.add( "email" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.common.auditAndSecurity.Contact"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }
        result = transformEntity( transform, ( ubic.gemma.model.common.auditAndSecurity.User ) result );
        return ( User ) result;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByEmail(java.lang.String)
     */

    public ubic.gemma.model.common.auditAndSecurity.User findByEmail( java.lang.String email ) {
        return this.findByEmail( TRANSFORM_NONE, email );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByFirstAndLastName(int, java.lang.String,
     *      java.lang.String)
     */
    public java.util.Collection<User> findByFirstAndLastName( final int transform, final java.lang.String name,
            final java.lang.String secondName ) {
        return this
                .findByFirstAndLastName(
                        transform,
                        "from ubic.gemma.model.common.auditAndSecurity.UserImpl as user where user.name = :name and user.secondName = :secondName",
                        name, secondName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByFirstAndLastName(int, java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public java.util.Collection<User> findByFirstAndLastName( final int transform, final java.lang.String queryString,
            final java.lang.String name, final java.lang.String secondName ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( name );
        argNames.add( "name" );
        args.add( secondName );
        argNames.add( "secondName" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByFirstAndLastName(java.lang.String, java.lang.String)
     */

    public java.util.Collection<User> findByFirstAndLastName( java.lang.String name, java.lang.String secondName ) {
        return this.findByFirstAndLastName( TRANSFORM_NONE, name, secondName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByFirstAndLastName(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    public java.util.Collection<User> findByFirstAndLastName( final java.lang.String queryString,
            final java.lang.String name, final java.lang.String secondName ) {
        return this.findByFirstAndLastName( TRANSFORM_NONE, queryString, name, secondName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByFullName(int, java.lang.String, java.lang.String)
     */

    @SuppressWarnings( { "unchecked" })
    public java.util.Collection<User> findByFullName( final int transform, final java.lang.String name,
            final java.lang.String secondName ) {
        return this.findByFullName( transform,
                "from PersonImpl p where p.firstName=:firstName and p.lastName=:lastName and p.middleName=:middleName",
                name, secondName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByFullName(int, java.lang.String, java.lang.String,
     *      java.lang.String)
     */

    @SuppressWarnings( { "unchecked" })
    public java.util.Collection findByFullName( final int transform, final java.lang.String queryString,
            final java.lang.String name, final java.lang.String secondName ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( name );
        argNames.add( "name" );
        args.add( secondName );
        argNames.add( "secondName" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByFullName(java.lang.String, java.lang.String)
     */

    public java.util.Collection<User> findByFullName( java.lang.String name, java.lang.String secondName ) {
        return this.findByFullName( TRANSFORM_NONE, name, secondName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByFullName(java.lang.String, java.lang.String,
     *      java.lang.String)
     */

    @SuppressWarnings( { "unchecked" })
    public java.util.Collection findByFullName( final java.lang.String queryString, final java.lang.String name,
            final java.lang.String secondName ) {
        return this.findByFullName( TRANSFORM_NONE, queryString, name, secondName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByLastName(int, java.lang.String)
     */
    public java.util.Collection<User> findByLastName( final int transform, final java.lang.String lastName ) {
        return this.findByLastName( transform, "from  UserImpl as user where user.lastName = :lastName", lastName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByLastName(int, java.lang.String, java.lang.String)
     */

    @SuppressWarnings( { "unchecked" })
    public java.util.Collection<User> findByLastName( final int transform, final java.lang.String queryString,
            final java.lang.String lastName ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( lastName );
        argNames.add( "lastName" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByLastName(java.lang.String)
     */

    public java.util.Collection<User> findByLastName( java.lang.String lastName ) {
        return this.findByLastName( TRANSFORM_NONE, lastName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByLastName(java.lang.String, java.lang.String)
     */
    public java.util.Collection<User> findByLastName( final java.lang.String queryString,
            final java.lang.String lastName ) {
        return this.findByLastName( TRANSFORM_NONE, queryString, lastName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByUserName(int, java.lang.String)
     */
    public User findByUserName( final int transform, final java.lang.String userName ) {
        return this.findByUserName( transform, "from UserImpl u where u.userName=:userName", userName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByUserName(int, java.lang.String, java.lang.String)
     */
    @SuppressWarnings( { "unchecked" })
    public User findByUserName( final int transform, final java.lang.String queryString, final java.lang.String userName ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( userName );
        argNames.add( "userName" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.common.auditAndSecurity.User"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform, ( ubic.gemma.model.common.auditAndSecurity.User ) result );
        return ( User ) result;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByUserName(java.lang.String)
     */
    public ubic.gemma.model.common.auditAndSecurity.User findByUserName( java.lang.String userName ) {
        return this.findByUserName( TRANSFORM_NONE, userName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#findByUserName(java.lang.String, java.lang.String)
     */
    public ubic.gemma.model.common.auditAndSecurity.User findByUserName( final java.lang.String queryString,
            final java.lang.String userName ) {
        return this.findByUserName( TRANSFORM_NONE, queryString, userName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#load(int, java.lang.Long)
     */
    public User load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "User.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ubic.gemma.model.common.auditAndSecurity.UserImpl.class,
                id );
        return ( User ) transformEntity( transform, ( ubic.gemma.model.common.auditAndSecurity.User ) entity );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#load(java.lang.Long)
     */

    public User load( java.lang.Long id ) {
        return this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#loadAll()
     */
    public java.util.Collection<User> loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#loadAll(int)
     */

    @SuppressWarnings("unchecked")
    public java.util.Collection<User> loadAll( final int transform ) {
        final java.util.Collection<User> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.common.auditAndSecurity.UserImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "User.remove - 'id' can not be null" );
        }
        ubic.gemma.model.common.auditAndSecurity.User entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    public void remove( java.util.Collection<User> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "User.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#remove(ubic.gemma.model.common.auditAndSecurity.User)
     */
    public void remove( ubic.gemma.model.common.auditAndSecurity.User user ) {
        if ( user == null ) {
            throw new IllegalArgumentException( "User.remove - 'user' can not be null" );
        }
        this.getHibernateTemplate().delete( user );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final java.util.Collection<User> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "User.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<User> entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserDao#update(ubic.gemma.model.common.auditAndSecurity.User)
     */
    public void update( ubic.gemma.model.common.auditAndSecurity.User user ) {
        if ( user == null ) {
            throw new IllegalArgumentException( "User.update - 'user' can not be null" );
        }
        this.getHibernateTemplate().update( user );
    }

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.common.auditAndSecurity.User)} method. This method does not
     * instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in <code>ubic.gemma.model.common.auditAndSecurity.UserDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.common.auditAndSecurity.User)
     */
    @SuppressWarnings("unchecked")
    protected void transformEntities( final int transform, final java.util.Collection entities ) {
        switch ( transform ) {
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in
     * <code>ubic.gemma.model.common.auditAndSecurity.UserDao</code>, please note that the {@link #TRANSFORM_NONE}
     * constant denotes no transformation, so the entity itself will be returned. If the integer argument value is
     * unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.common.auditAndSecurity.UserDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform, final ubic.gemma.model.common.auditAndSecurity.User entity ) {
        Object target = null;
        if ( entity != null ) {
            switch ( transform ) {
                case TRANSFORM_NONE: // fall-through
                default:
                    target = entity;
            }
        }
        return target;
    }

}