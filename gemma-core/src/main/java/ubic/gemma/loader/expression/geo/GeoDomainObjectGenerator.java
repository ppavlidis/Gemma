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
package ubic.gemma.loader.expression.geo;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.expression.geo.fetcher.DatasetFetcher;
import ubic.gemma.loader.expression.geo.fetcher.PlatformFetcher;
import ubic.gemma.loader.expression.geo.fetcher.RawDataFetcher;
import ubic.gemma.loader.expression.geo.fetcher.SeriesFetcher;
import ubic.gemma.loader.expression.geo.model.GeoDataset;
import ubic.gemma.loader.expression.geo.model.GeoPlatform;
import ubic.gemma.loader.expression.geo.model.GeoSeries;
import ubic.gemma.loader.util.fetcher.Fetcher;
import ubic.gemma.loader.util.sdo.SourceDomainObjectGenerator;
import ubic.gemma.model.common.description.LocalFile;

/**
 * Handle fetching and parsing GEO files.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoDomainObjectGenerator implements SourceDomainObjectGenerator {

    protected static Log log = LogFactory.getLog( GeoDomainObjectGenerator.class.getName() );

    protected Fetcher datasetFetcher;
    protected Fetcher seriesFetcher;
    protected Fetcher platformFetcher;

    protected GeoFamilyParser parser;

    private boolean processPlatformsOnly;

    /**
     * 
     *
     */
    public GeoDomainObjectGenerator() {
        this.intialize();
    }

    /**
     * Initialize fetchers, clear out any data that was already generated by this Generator.
     */
    public void intialize() {
        parser = new GeoFamilyParser();
        datasetFetcher = new DatasetFetcher();
        seriesFetcher = new SeriesFetcher();
        platformFetcher = new PlatformFetcher();
    }

    /**
     * Process a data set and add it to the series
     * 
     * @param series
     * @param dataSetAccession
     */
    public void processDataSet( GeoSeries series, String dataSetAccession ) {
        log.info( "Processing " + dataSetAccession );
        GeoDataset gds = processDataSet( dataSetAccession );
        series.addDataSet( gds );
    }

    /**
     * Process a data set from an accession values
     * 
     * @param dataSetAccession
     * @return A GeoDataset object
     */
    private GeoDataset processDataSet( String dataSetAccession ) {
        if ( !dataSetAccession.startsWith( "GDS" ) ) {
            throw new IllegalArgumentException( "Invalid GEO dataset accession " + dataSetAccession );
        }
        String dataSetPath = fetchDataSetToLocalFile( dataSetAccession );
        GeoDataset gds = null;
        try {
            gds = processDataSet( dataSetAccession, dataSetPath );

        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        return gds;
    }

    /**
     * @param geoAccession, either a GPL, GDS or GSE value.
     * @return If processPlatformsOnly is true, a collection of GeoPlatforms. Otherwise a Collection of series (just
     *         one). If the accession is a GPL then processPlatformsOnly is set to true and any sample data is ignored.
     */
    public Collection<?> generate( String geoAccession ) {
        log.info( "Generating objects for " + geoAccession + " using " + this.getClass().getSimpleName() );
        Collection<Object> result = new HashSet<Object>();
        if ( geoAccession.startsWith( "GPL" ) ) {
            GeoPlatform platform = processPlatform( geoAccession );
            result.add( platform );
        } else if ( geoAccession.startsWith( "GDS" ) ) {
            // common starting point.
            String seriesAccession = DatasetCombiner.findGSEforGDS( geoAccession );
            if ( processPlatformsOnly ) {
                return processSeriesPlatforms( seriesAccession ); // FIXME, this is ugly.
            }
            log.info( geoAccession + " corresponds to " + seriesAccession );
            GeoSeries series = processSeries( seriesAccession );
            result.add( series );
        } else if ( geoAccession.startsWith( "GSE" ) ) {
            if ( processPlatformsOnly ) {
                return processSeriesPlatforms( geoAccession ); // FIXME, this is ugly.
            }
            GeoSeries series = processSeries( geoAccession );
            result.add( series );
            return result;
        } else {
            throw new IllegalArgumentException( "Cannot handle acccession: " + geoAccession
                    + ", must be a GDS, GSE or GPL" );
        }
        return result;

    }

    /**
     * @param datasetsToProcess Collection of GDS accession ids to process.
     * @return
     */
    private Collection<Object> processDatasets( Collection<String> datasetsToProcess ) {
        Collection<Object> result = new HashSet<Object>();
        for ( String accession : datasetsToProcess ) {
            result.add( processDataSet( accession ) );
        }
        return result;
    }

    /**
     * @param geoAccession
     * @return
     */
    private GeoPlatform processPlatform( String geoAccession ) {
        assert platformFetcher != null;
        Collection<LocalFile> platforms = platformFetcher.fetch( geoAccession );
        if ( platforms == null ) {
            throw new RuntimeException( "No series file found for " + geoAccession );
        }
        LocalFile platformFile = ( platforms.iterator() ).next();
        String platformPath;

        platformPath = platformFile.getLocalURL().getPath();

        parser.setProcessPlatformsOnly( true );
        try {
            parser.parse( platformPath );
        } catch ( IOException e1 ) {
            throw new RuntimeException( e1 );
        }

        return ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatformMap().get( geoAccession );

    }

    /**
     * Download and parse a GEO series.
     * 
     * @param seriesAccession
     */
    private GeoSeries processSeries( String seriesAccession ) {

        Collection<String> datasetsToProcess = DatasetCombiner.findGDSforGSE( seriesAccession );

        if ( datasetsToProcess == null || datasetsToProcess.size() == 0 ) {
            log.warn( "No data set found for " + seriesAccession );
        }

        Collection<LocalFile> fullSeries = seriesFetcher.fetch( seriesAccession );
        if ( fullSeries == null ) {
            throw new RuntimeException( "No series file found for " + seriesAccession );
        }
        LocalFile seriesFile = ( fullSeries.iterator() ).next();
        String seriesPath = seriesFile.getLocalURL().getPath();

        parser.setProcessPlatformsOnly( this.processPlatformsOnly );
        try {
            parser.parse( seriesPath );
        } catch ( IOException e1 ) {
            throw new RuntimeException( e1 );
        }

        // Only allow one series...
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get(
                seriesAccession );

        // FIXME put this back...or something.
        // Raw data files have been added to series object as a path (during parsing).
        // processRawData( series )

        for ( String dataSetAccession : datasetsToProcess ) {
            log.info( "Processing " + dataSetAccession );
            processDataSet( series, dataSetAccession );
        }

        GeoSampleCorrespondence correspondence = DatasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        return series;
    }

    /**
     * Download and parse a GEO platform using a series accession.
     * 
     * @param seriesAccession
     */
    private Collection<?> processSeriesPlatforms( String seriesAccession ) {
        Collection<LocalFile> fullSeries = seriesFetcher.fetch( seriesAccession );
        if ( fullSeries == null ) {
            throw new RuntimeException( "No series file found for " + seriesAccession );
        }
        LocalFile seriesFile = ( fullSeries.iterator() ).next();
        String seriesPath;

        seriesPath = seriesFile.getLocalURL().getPath();

        parser.setProcessPlatformsOnly( this.processPlatformsOnly );
        try {
            parser.parse( seriesPath );
        } catch ( IOException e1 ) {
            throw new RuntimeException( e1 );
        }

        return ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatformMap().values();

    }

    /**
     * Parse a GEO GDS file, return the extracted GeoDataset.
     * 
     * @param geoDataSetAccession
     * @param dataSetPath
     * @return GeoDataset
     * @throws IOException
     */
    private GeoDataset processDataSet( String geoDataSetAccession, String dataSetPath ) throws IOException {
        parser.parse( dataSetPath );

        // first result is where we start.
        GeoParseResult results = ( GeoParseResult ) parser.getResults().iterator().next();

        Map<String, GeoDataset> datasetMap = results.getDatasets();
        if ( !datasetMap.containsKey( geoDataSetAccession ) ) {
            throw new IllegalStateException( "Failed to get parse of " + geoDataSetAccession );
        }

        GeoDataset gds = datasetMap.get( geoDataSetAccession );
        return gds;
    }

    /**
     * @param geoDataSetAccession
     * @return
     */
    private String fetchDataSetToLocalFile( String geoDataSetAccession ) {
        Collection<LocalFile> result = datasetFetcher.fetch( geoDataSetAccession );

        if ( result == null ) return null;

        if ( result.size() != 1 ) {
            throw new IllegalStateException( "Got " + result.size() + " files for " + geoDataSetAccession
                    + ", expected only one." );
        }

        LocalFile dataSetFile = ( result.iterator() ).next();
        String dataSetPath;

        dataSetPath = dataSetFile.getLocalURL().getPath();

        return dataSetPath;
    }

    /**
     * Fetch any raw data files
     * 
     * @param series
     */
    private void processRawData( GeoSeries series ) {
        RawDataFetcher rawFetcher = new RawDataFetcher();
        Collection<LocalFile> rawFiles = rawFetcher.fetch( series.getGeoAccession() );
        if ( rawFiles != null ) {
            // FIXME maybe do something more. These are usually (always?) CEL files so they can be parsed and
            // assembled or left alone.
            log.info( "Downloaded raw data files" );
        }
    }

    /**
     * @param datasetFetcher The datasetFetcher to set.
     */
    public void setDatasetFetcher( Fetcher df ) {
        this.datasetFetcher = df;
    }

    /**
     * @param seriesFetcher The seriesFetcher to set.
     */
    public void setSeriesFetcher( Fetcher seriesFetcher ) {
        this.seriesFetcher = seriesFetcher;
    }

    /**
     * @param b
     */
    public void setProcessPlatformsOnly( boolean b ) {
        this.processPlatformsOnly = b;
    }

    /**
     * @param platformFetcher The platformFetcher to set.
     */
    public void setPlatformFetcher( Fetcher platformFetcher ) {
        this.platformFetcher = platformFetcher;
    }

}

class NoDatasetForSeriesException extends RuntimeException {

    /**
     * 
     */
    public NoDatasetForSeriesException() {
        super();
    }

    /**
     * @param message
     * @param cause
     */
    public NoDatasetForSeriesException( String message, Throwable cause ) {
        super( message, cause );
    }

    /**
     * @param message
     */
    public NoDatasetForSeriesException( String message ) {
        super( message );
    }

    /**
     * @param cause
     */
    public NoDatasetForSeriesException( Throwable cause ) {
        super( cause );
    }

}
