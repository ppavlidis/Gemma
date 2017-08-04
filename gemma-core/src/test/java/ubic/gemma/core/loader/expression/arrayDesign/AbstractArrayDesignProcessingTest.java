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
package ubic.gemma.core.loader.expression.arrayDesign;

import java.io.FileNotFoundException;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.core.testing.BaseSpringContextTest;

/**
 * Base test for tests that need persistent array design with sequences.
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class AbstractArrayDesignProcessingTest extends BaseSpringContextTest {

    ArrayDesign ad;

    @Autowired
    ArrayDesignService arrayDesignService;

    @Autowired
    private GeneService geneService;

    final static String ACCESSION = "GPL140";

    public ArrayDesign getAd() {
        return ad;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.testing.BaseSpringContextTest#onSetUp()
     */
    @Before
    public void setupArrayDesign() throws Exception {
        ad = arrayDesignService.findByShortName( ACCESSION );

        if ( ad == null ) {

            // first load small twoc-color
            GeoService geoService = this.getBean( GeoService.class );
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );

            try {
                @SuppressWarnings("unchecked")
                final Collection<ArrayDesign> ads = ( Collection<ArrayDesign> ) geoService.fetchAndLoad( ACCESSION,
                        true, true, false, false, true, true );

                ad = ads.iterator().next();

            } catch ( Exception e ) {
                if ( e.getCause() instanceof FileNotFoundException ) {
                    log.warn( "problem with initializing array design for test: " + e.getCause().getMessage() );
                    return;
                }
                throw e;
            }
        }

        ad = arrayDesignService.thaw( ad );

    }

    @After
    public void tearDown() {
        Collection<Gene> genes = geneService.loadAll();
        for ( Gene gene : genes ) {
            try {
                geneService.remove( gene );
            } catch ( Exception e ) {

            }
        }

        if ( ad != null ) try {
            arrayDesignService.remove( ad );
        } catch ( Exception e ) {

        }
    }

}