/*
 * The Gemma project
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
package ubic.gemma.loader.expression.geo.service;

import java.util.Collection;

import ubic.gemma.loader.expression.geo.model.GeoSeries;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Non-interactive fetching, processing and persisting of GEO data.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="geoDatasetService"
 */
public class GeoDatasetService extends AbstractGeoService {

    /**
     * Given a GEO GSE or GDS:
     * <ol>
     * <li>Download and parse GDS files and GSE file needed</li>
     * <li>Convert the GDS and GSE into a ExpressionExperiment (or just the ArrayDesigns)
     * <li>Load the resulting ExpressionExperiment and/or ArrayDesigns into Gemma</li>
     * </ol>
     * 
     * @param geoDataSetAccession
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection fetchAndLoad( String geoAccession ) {
        this.geoConverter.clear();
        geoDomainObjectGenerator.intialize();
        geoDomainObjectGenerator.setProcessPlatformsOnly( this.loadPlatformOnly );

        if ( this.loadPlatformOnly ) {
            Collection<?> platforms = geoDomainObjectGenerator.generate( geoAccession );
            Collection<Object> arrayDesigns = geoConverter.convert( platforms );
            return persisterHelper.persist( arrayDesigns );
        }

        Collection<?> results = geoDomainObjectGenerator.generate( geoAccession );

        if ( results == null || results.size() == 0 ) {
            throw new RuntimeException( "Could not get domain objects for " + geoAccession );
        }

        Object obj = results.iterator().next();
        if ( !( obj instanceof GeoSeries ) ) {
            throw new RuntimeException( "Got a " + obj.getClass().getName() + " instead of a "
                    + GeoSeries.class.getName() + " (you may need to load platforms only)." );
        }

        GeoSeries series = ( GeoSeries ) obj;

        log.info( "Generated GEO domain objects for " + geoAccession );

        Collection<ExpressionExperiment> result = ( Collection<ExpressionExperiment> ) geoConverter.convert( series );

        log.info( "Converted " + series.getGeoAccession() );
        assert persisterHelper != null;
        Collection persistedResult = persisterHelper.persist( result );
        log.info( "Persisted " + series.getGeoAccession() );
        this.geoConverter.clear();
        return persistedResult;
    }

}
