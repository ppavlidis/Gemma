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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.common.description;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author Luke
 * @author Paul
 * @see ubic.gemma.model.common.description.Characteristic
 * @version $Id$
 */
public class CharacteristicDaoImpl extends ubic.gemma.model.common.description.CharacteristicDaoBase {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.CharacteristicDaoBase#handleFindByParentClass(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map handleFindByParentClass( Class parentClass ) throws Exception {
        final String queryString = "select parent, char from " + parentClass.getSimpleName() + " as parent "
                + "inner join parent.characteristics as char";

        Map charToParent = new HashMap<Characteristic, Object>();
        for ( Object o : getHibernateTemplate().find( queryString ) ) {
            Object[] row = ( Object[] ) o;
            charToParent.put( ( Characteristic ) row[1], row[0] );
        }
        return charToParent;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.CharacteristicDaoBase#handleFindByUri(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<Characteristic> handleFindByUri( Collection uris ) throws Exception {
        int batchSize = 1000; // to avoid HQL parser barfing
        Collection<String> batch = new HashSet<String>();
        Collection<Characteristic> results = new HashSet<Characteristic>();
        final String queryString = "from VocabCharacteristicImpl where valueUri in (:uris)";

        for ( String uri : ( Collection<String> ) uris ) {
            batch.add( uri );
            if ( batch.size() >= batchSize ) {
                results.addAll( getHibernateTemplate().findByNamedParam( queryString, "uris", batch ) );
                batch.clear();
            }
        }
        if ( batch.size() > 0 ) {
            results.addAll( getHibernateTemplate().findByNamedParam( queryString, "uris", batch ) );
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.CharacteristicDaoBase#handleFindByUri(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<Characteristic> handleFindByUri( String searchString ) throws Exception {
        final String queryString = "select char from VocabCharacteristicImpl as char where  char.valueUri = :search";
        return getHibernateTemplate().findByNamedParam( queryString, "search", searchString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.CharacteristicDaoBase#handleFindByvalue(java.lang.String)
     */
    @Override
    protected Collection handleFindByValue( String search ) throws Exception {
        final String queryString = "select distinct char from CharacteristicImpl as char where char.value like :search";
        return getHibernateTemplate().findByNamedParam( queryString, "search", search );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.CharacteristicDaoBase#handleFindParents(java.lang.Class,
     *      java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map handleGetParents( Class parentClass, Collection characteristics ) throws Exception {
        if ( characteristics.isEmpty() ) return new HashMap();

        final String queryString = "select parent, char from " + parentClass.getSimpleName() + " as parent "
                + "inner join parent.characteristics as char " + "where char in (:chars)";

        Map charToParent = new HashMap<Characteristic, Object>();
        for ( Object o : getHibernateTemplate().findByNamedParam( queryString, "chars", characteristics ) ) {
            Object[] row = ( Object[] ) o;
            charToParent.put( ( Characteristic ) row[1], row[0] );
        }
        return charToParent;
    }

}