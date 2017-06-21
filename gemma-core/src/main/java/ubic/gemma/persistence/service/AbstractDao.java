/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2011 University of British Columbia
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
package ubic.gemma.persistence.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.genome.taxon.TaxonServiceImpl;
import ubic.gemma.persistence.util.EntityUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * AbstractDao can find the generic type at runtime and simplify the code implementation of the BaseDao interface
 *
 * @author Anton, Nicolas
 */
public abstract class AbstractDao<T extends Identifiable> extends HibernateDaoSupport implements BaseDao<T> {

    protected static final Log log = LogFactory.getLog( TaxonServiceImpl.class );

    private Class<T> elementClass;

    /* ********************************
     * Constructors
     * ********************************/

    protected AbstractDao( Class<T> elementClass, SessionFactory sessionFactory ) {
        super();
        super.setSessionFactory( sessionFactory );
        this.elementClass = elementClass;
    }

    /* ********************************
     * Public methods
     * ********************************/

    @Override
    public Collection<T> create( Collection<T> entities ) {
        int i = 0;
        for ( T t : entities ) {
            this.create( t );
            if ( ++i % 100 == 0 )
                this.getSessionFactory().getCurrentSession().flush();
        }
        return entities;
    }

    @Override
    public T create( T entity ) {
        Serializable id = this.getSessionFactory().getCurrentSession().save( entity );
        assert EntityUtils.getId( entity ) != null;
        assert id.equals( EntityUtils.getId( entity ) );
        return entity;
    }

    @Override
    public Collection<T> load( Collection<Long> ids ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( "from " + elementClass.getSimpleName() + " e where e.id in (:ids)" )
                .setParameterList( "ids", ids ).list();
    }

    @Override
    public T load( Long id ) {
        // Don't use 'load' because if the object doesn't exist you can get an invalid proxy.
        //noinspection unchecked
        return id == null ? null : ( T ) this.getSessionFactory().getCurrentSession().get( elementClass, id );
    }

    @Override
    public Collection<T> loadAll() {
        return this.getHibernateTemplate().loadAll( elementClass );
    }

    @Override
    public Integer countAll() {
        return this.loadAll().size();
    }

    @Override
    public void remove( Collection<T> entities ) {
        for ( T e : entities ) {
            this.remove( e );
        }
    }

    @Override
    public void remove( Long id ) {
        this.remove( this.load( id ) );
    }

    @Override
    public void remove( T entity ) {
        this.getSession().delete( entity );
    }

    @Override
    public void update( Collection<T> entities ) {
        for ( T entity : entities ) {
            this.update( entity );
        }
    }

    @Override
    public void update( T entity ) {
        this.getSessionFactory().getCurrentSession().update( entity );
    }

    @Override
    public T findOrCreate( T entity ) {
        T found = find( entity );
        return found == null ? create( entity ) : found;
    }

    public T find( T entity ) {
        return this.load( entity.getId() );
    }

    public void thaw( T entity ) {
    }

    /* ********************************
     * Protected methods
     * ********************************/

    /**
     * Does a like-match case insensitive search on given property and its value.
     *
     * @param propertyName  the name of property to be matched.
     * @param propertyValue the value to look for.
     * @return an entity whose property first like-matched the given value.
     */
    protected T findOneByStringProperty( String propertyName, String propertyValue ) {
        Criteria criteria = this.getSessionFactory().getCurrentSession().createCriteria( this.elementClass );
        criteria.add( Restrictions.ilike( propertyName, propertyValue ) );
        criteria.setMaxResults( 1 );
        //noinspection unchecked
        return ( T ) criteria.uniqueResult();
    }

    /**
     * Does a like-match case insensitive search on given property and its value.
     *
     * @param propertyName  the name of property to be matched.
     * @param propertyValue the value to look for.
     * @return a list of entities whose properties like-matched the given value.
     */
    protected List<T> findByStringProperty( String propertyName, String propertyValue ) {
        Criteria criteria = this.getSessionFactory().getCurrentSession().createCriteria( this.elementClass );
        criteria.add( Restrictions.ilike( propertyName, propertyValue ) );
        //noinspection unchecked
        return criteria.list();
    }

    /**
     * Lists all entities whose given property matches the given value.
     *
     * @param propertyName  the name of property to be matched.
     * @param propertyValue the value to look for.
     * @return a list of entities whose properties matched the given value.
     */
    protected T findOneByProperty( String propertyName, Object propertyValue ) {
        Criteria criteria = this.getSessionFactory().getCurrentSession().createCriteria( this.elementClass );
        criteria.add( Restrictions.eq( propertyName, propertyValue ) );
        criteria.setMaxResults( 1 );
        //noinspection unchecked
        return ( T ) criteria.uniqueResult();
    }

    /**
     * Does a search on given property and its value.
     *
     * @param propertyName  the name of property to be matched.
     * @param propertyValue the value to look for.
     * @return an entity whose property first matched the given value.
     */
    protected List<T> findByProperty( String propertyName, Object propertyValue ) {
        Criteria criteria = this.getSessionFactory().getCurrentSession().createCriteria( this.elementClass );
        criteria.add( Restrictions.eq( propertyName, propertyValue ) );
        //noinspection unchecked
        return criteria.list();
    }

}
