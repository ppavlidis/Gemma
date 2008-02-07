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
package ubic.gemma.web.controller.genome.gene;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.genome.taxon.SupportedTaxa;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;
import ubic.gemma.web.controller.BaseMultiActionController;

/**
 * @author luke
 * @spring.bean id="genePickerController"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="taxonService" ref="taxonService"
 * @spring.property name="searchService" ref="searchService"
 */
public class GenePickerController extends BaseMultiActionController {
    private static Log log = LogFactory.getLog( GenePickerController.class.getName() );

    private GeneService geneService = null;
    private TaxonService taxonService = null;
    private SearchService searchService = null;
    
    private static Comparator<Taxon> TAXON_COMPARATOR = new Comparator<Taxon>() {
        public int compare( Taxon o1, Taxon o2 ) {
            return ( o1 ).getScientificName().compareTo( ( o2 ).getScientificName() );
        }
    };

    @SuppressWarnings("unchecked")
    public Collection<Taxon> getTaxa() {
        SortedSet<Taxon> taxa = new TreeSet<Taxon>( TAXON_COMPARATOR );
        for ( Taxon taxon : ( Collection<Taxon> ) taxonService.loadAll() ) {
            if ( SupportedTaxa.contains( taxon ) )
                taxa.add( taxon );
        }
        return taxa;
    }

    @SuppressWarnings("unchecked")
    public Collection<Gene> getGenes( Collection<Long> geneIds ) {
        return geneService.loadMultiple( geneIds );
    }

    public Collection<Gene> searchGenes( String query, Long taxonId ) {
        Taxon taxon = taxonService.load( taxonId );
        SearchSettings settings = SearchSettings.GeneSearch( query, taxon );
        List<SearchResult> geneSearchResults = searchService.search( settings ).get( Gene.class );
        
        Collection<Gene> genes = new HashSet<Gene>();
        for ( SearchResult sr : geneSearchResults ) {
            genes.add( ( Gene ) sr.getResultObject() );
        }
        return genes;
    }

    /**
     * @param geneService The geneService to set.
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * @param taxonService The taxonService to set.
     */
    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

    /**
     * @param searchService The searchService to set.
     */
    public void setSearchService( SearchService searchService ) {
        this.searchService = searchService;
    }

}