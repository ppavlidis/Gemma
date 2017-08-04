/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.persistence.service.expression.bioAssayData;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.stereotype.Component;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.persistence.util.CacheUtils;
import ubic.gemma.persistence.util.Settings;

import java.io.Serializable;
import java.util.Collection;

/**
 * Configures the cache for data vectors.
 * Implementation note: This uses ehCache. I have decided to make one cache per expression experiment. The reason for
 * this is that having complex keys for cached Elements based on expression experiment AND gene makes it difficult to
 * invalidate the cache when an expression experiment's data changes. The drawback is that there are potentially
 * hundreds of caches; I don't know if there are any performance considerations there.
 *
 * @author paul
 */
@Component
public class ProcessedDataVectorCacheImpl implements InitializingBean, ProcessedDataVectorCache {

    private static final String VECTOR_CACHE_NAME = "ProcessedExpressionDataVectorCache";
    private static final boolean VECTOR_CACHE_DEFAULT_ETERNAL = true;
    private static final boolean VECTOR_CACHE_DEFAULT_OVERFLOW_TO_DISK = true;
    private static final int VECTOR_CACHE_DEFAULT_MAX_ELEMENTS = 100000;
    private static final int VECTOR_CACHE_DEFAULT_TIME_TO_IDLE = 10000;
    private static final int VECTOR_CACHE_DEFAULT_TIME_TO_LIVE = 10000;

    private Cache cache;

    @Autowired
    private EhCacheManagerFactoryBean cacheManagerFactory;

    @Override
    public void addToCache( Long eeid, Long g, Collection<DoubleVectorValueObject> collection ) {
        cache.put( new Element( new CacheKey( eeid, g ), collection ) );
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        CacheManager cacheManager = this.cacheManagerFactory.getObject();
        int maxElements = Settings.getInt( "gemma.cache.vectors.maxelements", VECTOR_CACHE_DEFAULT_MAX_ELEMENTS );
        int timeToLive = Settings.getInt( "gemma.cache.vectors.timetolive", VECTOR_CACHE_DEFAULT_TIME_TO_LIVE );
        int timeToIdle = Settings.getInt( "gemma.cache.vectors.timetoidle", VECTOR_CACHE_DEFAULT_TIME_TO_IDLE );
        boolean overFlowToDisk = Settings
                .getBoolean( "gemma.cache.vectors.usedisk", VECTOR_CACHE_DEFAULT_OVERFLOW_TO_DISK );
        boolean terracottaEnabled = Settings.getBoolean( "gemma.cache.clustered", true );
        boolean eternal =
                Settings.getBoolean( "gemma.cache.vectors.eternal", VECTOR_CACHE_DEFAULT_ETERNAL ) && timeToLive == 0;
        boolean diskPersistent = Settings.getBoolean( "gemma.cache.diskpersistent", true ) && !terracottaEnabled;

        this.cache = CacheUtils
                .createOrLoadCache( cacheManager, VECTOR_CACHE_NAME, terracottaEnabled, maxElements, overFlowToDisk,
                        eternal, timeToIdle, timeToLive, diskPersistent );

    }

    @Override
    public void clearCache() {
        cache.removeAll();
    }

    @Override
    public void clearCache( Long eeid ) {
        for ( Object o : cache.getKeys() ) {
            CacheKey k = ( CacheKey ) o;
            if ( k.getEeid().equals( eeid ) ) {
                cache.remove( k );
            }
        }
    }

    @Override
    public Collection<DoubleVectorValueObject> get( BioAssaySet ee, Long g ) {
        Element element = cache.get( new CacheKey( ee.getId(), g ) );
        if ( element == null )
            return null;
        @SuppressWarnings("unchecked") Collection<DoubleVectorValueObject> result = ( Collection<DoubleVectorValueObject> ) element
                .getObjectValue();

        /*
         * See 2878 - we don't want to keep these values cached, so the vectors can be re-used.
         */
        for ( DoubleVectorValueObject dvvo : result ) {
            dvvo.setPvalue( null );
        }
        return result;
    }

    @Override
    public int size() {
        return this.cache.getSize();
    }
}

class CacheKey implements Serializable {

    private static final long serialVersionUID = -7873367550383853137L;
    private final Long eeid;
    private final Long geneId;

    CacheKey( Long eeid, Long geneId ) {
        this.eeid = eeid;
        this.geneId = geneId;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        CacheKey other = ( CacheKey ) obj;
        if ( eeid == null ) {
            if ( other.eeid != null )
                return false;
        } else if ( !eeid.equals( other.eeid ) )
            return false;
        if ( geneId == null ) {
            if ( other.geneId != null )
                return false;
        } else if ( !geneId.equals( other.geneId ) )
            return false;
        return true;
    }

    public Long getEeid() {
        return eeid;
    }

    public Long getGeneId() {
        return geneId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( eeid == null ? 0 : eeid.hashCode() );
        result = prime * result + ( geneId == null ? 0 : geneId.hashCode() );
        return result;
    }

}