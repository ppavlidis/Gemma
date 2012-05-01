/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.loader.expression.geo.service;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.geo.model.GeoRecord;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Scans GEO for experiments that are not GEO.
 * 
 * @author paul
 * @version $Id$
 */
public class GeoGrabberCli extends AbstractSpringAwareCLI {

    @Override
    protected void buildOptions() {
        // TODO Auto-generated method stub
    }

    @Override
    protected Exception doWork( String[] args ) {
        super.processCommandLine( "foo", args );

        Set<String> seen = new HashSet<String>();
        GeoBrowserService gbs = ( GeoBrowserService ) this.getBean( "geoBrowserService" );
        ExpressionExperimentService ees = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );

        try {
            int start = 0;
            int chunksize = 100;
            while ( true ) {
                List<GeoRecord> recs = gbs.getRecentGeoRecords( start, chunksize );
                start += chunksize;

                if ( recs.isEmpty() ) {
                    log.info( "No records received for start=" + start );
                    break;
                }

                for ( GeoRecord geoRecord : recs ) {
                    if ( seen.contains( geoRecord.getGeoAccession() ) ) {
                        continue;
                    }

                    if ( ees.findByShortName( geoRecord.getGeoAccession() ) != null ) {
                        continue;
                    }

                    System.out.println( geoRecord.getGeoAccession() + "\t" + geoRecord.getOrganisms().iterator().next()
                            + "\t" + geoRecord.getNumSamples() + "\t" + geoRecord.getTitle() + "\t"
                            + StringUtils.join( geoRecord.getCorrespondingExperiments(), "," ) + "\t"
                            + geoRecord.getSeriesType() );
                    seen.add( geoRecord.getGeoAccession() );
                }
            }
        } catch ( IOException e ) {
            return e;
        } catch ( ParseException e ) {
            return e;
        }
        return null;
    }

    public static void main( String[] args ) {
        GeoGrabberCli d = new GeoGrabberCli();
        Exception e = d.doWork( args );
        if ( e != null ) {
            log.error( e, e );
        }
    }

}
