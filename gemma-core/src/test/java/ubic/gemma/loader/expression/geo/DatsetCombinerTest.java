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

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import junit.framework.TestCase;
import ubic.gemma.loader.expression.geo.model.GeoDataset;
import ubic.gemma.loader.expression.geo.model.GeoPlatform;
import ubic.gemma.loader.expression.geo.model.GeoSample;
import ubic.gemma.loader.expression.geo.model.GeoSubset;

/**
 * @author pavlidis
 * @version $Id$
 */
public class DatsetCombinerTest extends TestCase {

    private static Log log = LogFactory.getLog( DatsetCombinerTest.class.getName() );
    Collection<GeoDataset> gds;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'ubic.gemma.loader.expression.geo.DatsetCombiner.findGDSGrouping(String)'
     */
    public void testFindGDSGrouping() throws Exception {
        try {
            Collection result = DatasetCombiner.findGDSforGSE( "GSE674" );
            assertTrue( result.contains( "GDS472" ) && result.contains( "GDS473" ) );
        } catch ( RuntimeException e ) {
            if ( e.getCause() instanceof java.net.UnknownHostException ) {
                log.warn( "Test skipped due to unknown host exception" );
                return;
            }
            throw e;
        }
    }

    // todo: add test of findGSECorrespondence( GeoSeries series ) when there is no dataset.

    /*
     * Test method for 'ubic.gemma.loader.expression.geo.DatsetCombiner.findGSECorrespondence(Collection<GeoDataset>)'
     */
    public void testFindGSECorrespondence() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/twoDatasets/GDS472.soft.gz" ) );
        assert is != null;
        parser.parse( is );
        is.close();
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/twoDatasets/GSE674_family.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/twoDatasets/GDS473.soft.gz" ) );
        parser.parse( is );
        is.close();

        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );

        gds = parseResult.getDatasets().values();

        fillInDatasetPlatformAndOrganism();

        assertEquals( 2, gds.size() );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gds );

        // log.info( result );
        assertEquals( 15, result.size() );

        // these are just all the sample names.
        String[] keys = new String[] { "GSM10354", "GSM10355", "GSM10356", "GSM10359", "GSM10360", "GSM10361",
                "GSM10362", "GSM10363", "GSM10364", "GSM10365", "GSM10366", "GSM10367", "GSM10368", "GSM10369",
                "GSM10370", "GSM10374", "GSM10375", "GSM10376", "GSM10377", "GSM10378", "GSM10379", "GSM10380",
                "GSM10381", "GSM10382", "GSM10383", "GSM10384", "GSM10385", "GSM10386", "GSM10387", "GSM10388" };

        for ( int i = 0; i < keys.length; i++ ) {
            String string = keys[i];
            assertEquals( "Wrong result for " + keys[i] + ", expected 2", 2, result.getCorrespondingSamples( string )
                    .size() );
        }
        assertTrue( result.getCorrespondingSamples( "GSM10354" ).contains( "GSM10374" ) );
        assertTrue( result.getCorrespondingSamples( "GSM10374" ).contains( "GSM10354" ) );
    }

    private void fillInDatasetPlatformAndOrganism() {
        for ( GeoDataset geods : gds ) {
            GeoPlatform platform = geods.getPlatform();
            platform.getOrganisms().add( geods.getOrganism() );
            for ( GeoSubset subset : geods.getSubsets() ) {
                for ( GeoSample sample : subset.getSamples() ) {
                    sample.getPlatforms().add( platform );
                }
            }
        }
    }

    /*
     * Test method for 'ubic.gemma.loader.expression.geo.DatsetCombiner.findGSECorrespondence(Collection<GeoDataset>)'
     * This is a really hard case because the sample names are very similar. It has 8 samples, each on three arrays.
     */
    public void testFindGSECorrespondenceThreeDatasets() throws Exception {

        // GSE479 GDS242-244 fits the bill (MG-U74A,B and C (v2)

        GeoFamilyParser parser = new GeoFamilyParser();

        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/threeDatasets/GDS242.soft.gz" ) );
        parser.parse( is );
        assert is != null;
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/threeDatasets/GSE479_family.soft.gz" ) );
        parser.parse( is );
        assert is != null;
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/threeDatasets/GDS243.soft.gz" ) );
        parser.parse( is );
        assert is != null;
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/threeDatasets/GDS244.soft.gz" ) );
        parser.parse( is );
        assert is != null;
        is.close();

        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );

        String[] keys = new String[] { "GSM4045", "GSM4047", "GSM4049", "GSM4051", "GSM4053", "GSM4055", "GSM4057",
                "GSM4059", "GSM4061", "GSM4063", "GSM4065", "GSM4067", "GSM4069", "GSM4071", "GSM4073", "GSM4075",
                "GSM4077", "GSM4081", "GSM4079", "GSM4083", "GSM4085", "GSM4087", "GSM4089", "GSM4091" };

        assert keys.length == 24;
        gds = parseResult.getDatasets().values();

        // Where did these come from?
        // "GSM4076","GSM4052","GSM4070","GSM4046","GSM4082", "GSM4088","GSM4058","GSM4064","GSM4048",
        // "GSM4078","GSM4072","GSM4054","GSM4090", "GSM4060","GSM4084","GSM4066", "GSM4086","GSM4062",
        // "GSM4056","GSM4080", "GSM4068","GSM4044","GSM4050","GSM4074",

        /**
         * <pre>
         *                       GSM4045     PGA-MFD-CtrPD1-1aAv2-s2a
         *                       GSM4047     PGA-MFD-CtrPD1-1aBv2-s2
         *                       GSM4049     PGA-MFD-CtrPD1-1aCv2-s2
         *                       GSM4051     PGA-MFD-CtrPD1-2aAv2-s2b
         *                       GSM4053     PGA-MFD-CtrPD1-2aBv2-s2
         *                       GSM4055     PGA-MFD-CtrPD1-2aCv2-s2
         *                       GSM4057     PGA-MFD-CtrPD5-1aAv2-s2
         *                       GSM4059     PGA-MFD-CtrPD5-1aBv2-s2
         *                       GSM4061     PGA-MFD-CtrPD5-1aCv2-s2
         *                       GSM4063     PGA-MFD-CtrPD5-2aAv2-s2
         *                       GSM4065     PGA-MFD-CtrPD5-2aBv2-s2
         *                       GSM4067     PGA-MFD-CtrPD5-2aCv2-s2
         *                       GSM4069     PGA-MFD-MutantPD1-1aAv2-s2b
         *                       GSM4071     PGA-MFD-MutantPD1-1aBv2-s2
         *                       GSM4073     PGA-MFD-MutantPD1-1aCv2-s2
         *                       GSM4075     PGA-MFD-MutantPD1-2aAv2-s2a
         *                       GSM4077     PGA-MFD-MutantPD1-2aBv2-s2
         *                       GSM4079     PGA-MFD-MutantPD1-2aCv2-s2
         *                       GSM4081     PGA-MFD-MutantPD5-1aAv2-s2
         *                       GSM4083     PGA-MFD-MutantPD5-1aBv2-s2
         *                       GSM4085     PGA-MFD-MutantPD5-1aCv2-s2
         *                       GSM4087     PGA-MFD-MutantPD5-2aAv2-s2
         *                       GSM4089     PGA-MFD-MutantPD5-2aBv2-s2
         *                       GSM4091     PGA-MFD-MutantPD5-2aCv2-s2
         * </pre>
         */

        assertEquals( 3, gds.size() );

        fillInDatasetPlatformAndOrganism();
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gds );
        log.info( result );
        assertEquals( 8, result.size() );

        for ( int i = 0; i < keys.length; i++ ) {
            String string = keys[i];
            assertNotNull( "Got null for " + string, result.getCorrespondingSamples( string ) );
            assertTrue( "Wrong result for " + keys[i] + ", expected 3",
                    result.getCorrespondingSamples( string ).size() == 3 );
        }
        assertTrue( result.getCorrespondingSamples( "GSM4051" ).contains( "GSM4053" ) );
        assertTrue( result.getCorrespondingSamples( "GSM4083" ).contains( "GSM4085" ) );

    }

    /**
     * Fairly hard case; twelve samples, 3 array design each sample run on each array design
     * 
     * @throws Exception
     */
    public void testFindGSE611() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();

        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE611Short/GDS428.soft.gz" ) );
        parser.parse( is );
        assert is != null;
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE611Short/GSE611_family.soft.gz" ) );
        parser.parse( is );
        assert is != null;
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE611Short/GDS429.soft.gz" ) );
        parser.parse( is );
        assert is != null;
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE611Short/GDS430.soft.gz" ) );
        parser.parse( is );
        assert is != null;
        is.close();
        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );
        gds = parseResult.getDatasets().values();
        assertEquals( 3, gds.size() );
        fillInDatasetPlatformAndOrganism();
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gds );

        Iterator<Set<String>> it = result.iterator();
        int numBioMaterials = 0;
        while ( it.hasNext() ) {
            Collection c = it.next();
            assertEquals( 3, c.size() );
            numBioMaterials++;
        }
        assertEquals( 4, numBioMaterials );

        log.info( result );
    }

    /**
     * Really hard case.
     * 
     * @throws Exception
     */
    public void testFindGSE1133Human() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();

        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse1133Short/GDS594.soft.gz" ) );
        parser.parse( is );
        assert is != null;
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse1133Short/GSE1133_family.soft.gz" ) );
        parser.parse( is );
        assert is != null;
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse1133Short/GDS596.soft.gz" ) );
        parser.parse( is );
        assert is != null;
        is.close();

        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );
        gds = parseResult.getDatasets().values();
        assertEquals( 2, gds.size() );
        fillInDatasetPlatformAndOrganism();

        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gds );

        log.info( result );

        Iterator<Set<String>> it = result.iterator();
        int numBioMaterials = 0;
        while ( it.hasNext() ) {
            Collection c = it.next();
            assertTrue( c.size() == 1 || c.size() == 2 );
            numBioMaterials++;
        }
        assertEquals( 158, numBioMaterials );

    }

}
