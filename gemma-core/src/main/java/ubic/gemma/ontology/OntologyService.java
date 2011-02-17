/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.ontology;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.basecode.ontology.OntologyLoader;
import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyResource;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.AbstractOntologyService;
import ubic.basecode.ontology.providers.BirnLexOntologyService;
import ubic.basecode.ontology.providers.CellTypeOntologyService;
import ubic.basecode.ontology.providers.ChebiOntologyService;
import ubic.basecode.ontology.providers.DiseaseOntologyService;
import ubic.basecode.ontology.providers.FMAOntologyService;
import ubic.basecode.ontology.providers.HumanDevelopmentOntologyService;
import ubic.basecode.ontology.providers.MammalianPhenotypeOntologyService;
import ubic.basecode.ontology.providers.MouseDevelopmentOntologyService;
import ubic.basecode.ontology.providers.NIFSTDOntologyService;
import ubic.basecode.ontology.providers.ObiService;
import ubic.basecode.ontology.search.OntologySearch;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicService;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.ontology.providers.MgedOntologyService;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;

import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * Has a static method for finding out which ontologies are loaded into the system and a general purpose find method
 * that delegates to the many ontology services. NOTE: Logging messages from this service are important for tracking
 * changes to annotations.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Service
public class OntologyService implements InitializingBean {

    // Private class for sorting Characteristics
    static class TermComparator implements Comparator<Characteristic>, Serializable {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        String comparator;

        public TermComparator( String comparator ) {
            super();
            this.comparator = comparator;
        }

        public int compare( Characteristic o1, Characteristic o2 ) {
            String term1 = o1.getValue();
            String term2 = o2.getValue();

            if ( term1.equals( term2 ) ) return 0;

            if ( term1.equals( comparator ) ) return 1;

            if ( term2.equals( comparator ) ) return -1;

            if ( term1.startsWith( comparator ) ) {
                if ( term2.startsWith( comparator ) ) return 0;
                return 1;
            } else if ( term2.startsWith( comparator ) ) {
                return -1;
            }

            return 0;

        }
    }

    private static Log log = LogFactory.getLog( OntologyService.class.getName() );

    private static final String USED = " -USED- ";

    /**
     * List the ontologies that are available in the jena database.
     * 
     * @return
     */
    public static Collection<ubic.gemma.ontology.Ontology> listAvailableOntologies() {

        Collection<ubic.gemma.ontology.Ontology> ontologies = new HashSet<ubic.gemma.ontology.Ontology>();

        ModelMaker maker = OntologyLoader.getRDBMaker();

        try {
            ExtendedIterator<String> iterator = maker.listModels();
            while ( iterator.hasNext() ) {
                String name = iterator.next();
                ExternalDatabase database = OntologyUtils.ontologyAsExternalDatabase( name );
                ubic.gemma.ontology.Ontology o = new ubic.gemma.ontology.Ontology( database );
                ontologies.add( o );
            }
            return ontologies;
        } finally {
            maker.close();
        }

    }

    @Autowired
    private BioMaterialService bioMaterialService;

    private BirnLexOntologyService birnLexOntologyService;

    private CellTypeOntologyService cellTypeOntologyService;
    
    private NIFSTDOntologyService nifstdOntologyService;

    @Autowired
    private CharacteristicService characteristicService;

    private ChebiOntologyService chebiOntologyService;

    private DiseaseOntologyService diseaseOntologyService;

    @Autowired
    private ExpressionExperimentService eeService;

    private FMAOntologyService fmaOntologyService;

    private HumanDevelopmentOntologyService humanDevelopmentOntologyService;

    private MammalianPhenotypeOntologyService mammalianPhenotypeOntologyService;

    @Autowired
    private MgedOntologyService mgedOntologyService;

    private MouseDevelopmentOntologyService mouseDevelopmentOntologyService;

    private Collection<AbstractOntologyService> ontologyServices = new HashSet<AbstractOntologyService>();

    @Autowired
    private SearchService searchService;

    private ObiService obiService;

    public void afterPropertiesSet() throws Exception {

        this.birnLexOntologyService = new BirnLexOntologyService();
        this.nifstdOntologyService = new NIFSTDOntologyService();
        this.chebiOntologyService = new ChebiOntologyService();
        this.fmaOntologyService = new FMAOntologyService();
        this.diseaseOntologyService = new DiseaseOntologyService();
        this.humanDevelopmentOntologyService = new HumanDevelopmentOntologyService();
        this.cellTypeOntologyService = new CellTypeOntologyService();
        this.mouseDevelopmentOntologyService = new MouseDevelopmentOntologyService();
        this.mammalianPhenotypeOntologyService = new MammalianPhenotypeOntologyService();
        this.obiService = new ObiService();

        this.ontologyServices.add( this.birnLexOntologyService );
        this.ontologyServices.add( this.nifstdOntologyService );
        this.ontologyServices.add( this.chebiOntologyService );
        this.ontologyServices.add( this.fmaOntologyService );
        this.ontologyServices.add( this.diseaseOntologyService );
        this.ontologyServices.add( this.mgedOntologyService );
        this.ontologyServices.add( this.mouseDevelopmentOntologyService );
        this.ontologyServices.add( this.humanDevelopmentOntologyService );
        this.ontologyServices.add( this.cellTypeOntologyService );
        this.ontologyServices.add( this.mammalianPhenotypeOntologyService );
        this.ontologyServices.add( this.obiService );

        for ( AbstractOntologyService serv : this.ontologyServices ) {
            serv.startInitializationThread( false );
        }

    }

    /**
     * Given a search string will first look through the characterisc database for any entries that have a match. If a
     * ontologyTermURI is given it will add all the individuals from that URI that match the search term criteria to the
     * returned list also. Then will search the loaded ontologies for OntologyResources (Terms and Individuals) that
     * match the search term exactly
     * 
     * @param givenQueryString
     * @param categoryUri
     * @param taxon Only used if we're going to search for genes or taxon is otherwise relevant.
     * @return
     */
    public Collection<Characteristic> findExactTerm( String givenQueryString, String categoryUri, Taxon taxon ) {

        if ( StringUtils.isBlank( givenQueryString ) ) return null;

        StopWatch watch = new StopWatch();
        watch.start();

        String queryString = OntologySearch.stripInvalidCharacters( givenQueryString );
        if ( StringUtils.isBlank( queryString ) ) {
            log.warn( "The query was not valid (ended up being empty): " + givenQueryString );
            return new HashSet<Characteristic>();
        }

        if ( log.isDebugEnabled() ) {
            log.debug( "starting findExactTerm for " + queryString + ". Timing information begins from here" );
        }

        Collection<OntologyResource> results;
        Collection<Characteristic> searchResults = new HashSet<Characteristic>();

        // Add the matching individuals
        Collection<Characteristic> individualResults = new HashSet<Characteristic>();
        if ( StringUtils.isNotBlank( categoryUri ) && !categoryUri.equals( "{}" ) ) {
            results = new HashSet<OntologyResource>( mgedOntologyService.getTermIndividuals( categoryUri.trim() ) );
            if ( results.size() > 0 ) individualResults.addAll( filter( results, queryString ) );
        }
        if ( log.isDebugEnabled() )
            log.debug( "found " + individualResults.size() + " individuals from ontology term " + categoryUri + " in "
                    + watch.getTime() + " ms" );

        Collection<String> foundValues = new HashSet<String>();

        Collection<Characteristic> previouslyUsedInSystem = new HashSet<Characteristic>();

        // this should be very fast.
        Collection<Characteristic> foundChars = characteristicService.findByValue( queryString );

        /*
         * remove duplicates, don't want to redefine == operator for Characteristics for this use consider if the value
         * = then its a duplicate.
         */
        if ( foundChars != null ) {
            for ( Characteristic characteristic : foundChars ) {
                if ( !foundValues.contains( foundValueKey( characteristic ) ) ) {
                    /*
                     * Want to flag in the web interface that these are already used by Gemma Didn't want to make a
                     * characteristic value object just to hold a boolean flag for used....
                     */
                    characteristic.setDescription( USED + characteristic.getDescription() );
                    previouslyUsedInSystem.add( characteristic );
                    foundValues.add( foundValueKey( characteristic ) );
                }
            }
        }
        if ( log.isDebugEnabled() || watch.getTime() > 100 )
            log.info( "found " + previouslyUsedInSystem.size() + " matching characteristics used in the database"
                    + " in " + watch.getTime() + " ms" );

        searchForGenes( queryString, categoryUri, taxon, searchResults );

        for ( AbstractOntologyService serv : this.ontologyServices ) {
            results = serv.findResources( queryString );
            if ( log.isDebugEnabled() ) log.debug( "found " + results.size() + " in " + watch.getTime() + " ms" );
            searchResults.addAll( filter( results, queryString ));
        }

        // Sort the individual results.
        Collection<Characteristic> sortedResults = sort( individualResults, previouslyUsedInSystem, searchResults,
                queryString, foundValues );

        if ( watch.getTime() > 1000 ) {
            log.info( "Ontology term query for: " + givenQueryString + ": " + watch.getTime() + "ms" );
        }

        return sortedResults;

    }

    /**
     * @param search
     * @return
     */
    public Collection<OntologyIndividual> findIndividuals( String givenSearch ) {

        String query = OntologySearch.stripInvalidCharacters( givenSearch );
        Collection<OntologyIndividual> results = new HashSet<OntologyIndividual>();

        for ( AbstractOntologyService ontology : ontologyServices ) {
            Collection<OntologyIndividual> found = ontology.findIndividuals( query );
            if ( found != null ) results.addAll( found );
        }

        return results;
    }

    /**
     * Given a search string will look through the Mged, birnlex, obo Disease Ontology and FMA Ontology for terms that
     * match the search term. this a lucene backed search, is inexact and for general terms can return alot of results.
     * 
     * @param search
     * @return a collection of VocabCharacteristics that are backed by the corresponding found OntologyTerm
     */
    public Collection<VocabCharacteristic> findTermAsCharacteristic( String search ) {

        String query = OntologySearch.stripInvalidCharacters( search );
        Collection<VocabCharacteristic> results = new HashSet<VocabCharacteristic>();

        if ( StringUtils.isBlank( query ) ) {
            return results;
        }

        for ( AbstractOntologyService ontology : ontologyServices ) {
            Collection<OntologyTerm> found = ontology.findTerm( query );
            if ( found != null ) results.addAll( convert( new HashSet<OntologyResource>( found ) ) );
        }

        return results;
    }

    /**
     * Given a search string will look through the loaded ontologies for terms that match the search term. this a lucene
     * backed search, is inexact and for general terms can return a lot of results.
     * 
     * @param search
     * @return returns a collection of ontologyTerm's
     */
    public Collection<OntologyTerm> findTerms( String search ) {

        String query = OntologySearch.stripInvalidCharacters( search );

        Collection<OntologyTerm> results = new HashSet<OntologyTerm>();

        if ( StringUtils.isBlank( query ) ) {
            return results;
        }

        for ( AbstractOntologyService ontology : ontologyServices ) {
            if ( ontology.isOntologyLoaded() ) {
                Collection<OntologyTerm> found = ontology.findTerm( query );
                if ( found != null ) results.addAll( found );
            }
        }

        return results;
    }

    /**
     * @return the birnLexOntologyService
     */
    public BirnLexOntologyService getBirnLexOntologyService() {
        return birnLexOntologyService;
    }
    
    public NIFSTDOntologyService getNifstfOntologyService() {
        return nifstdOntologyService;        
    }

    /**
     * @return the chebiOntologyService
     */
    public ChebiOntologyService getChebiOntologyService() {
        return chebiOntologyService;
    }

    /**
     * @return the diseaseOntologyService
     */
    public DiseaseOntologyService getDiseaseOntologyService() {
        return diseaseOntologyService;
    }

    /**
     * @return the fmaOntologyService
     */
    public FMAOntologyService getFmaOntologyService() {
        return fmaOntologyService;
    }

    /**
     * @return the mgedOntologyService
     */
    public MgedOntologyService getMgedOntologyService() {
        return mgedOntologyService;
    }

    /**
     * @return the OntologyResource for the specified URI
     */
    public OntologyResource getResource( String uri ) {
        for ( AbstractOntologyService ontology : ontologyServices ) {
            OntologyResource resource = ontology.getResource( uri );
            if ( resource != null ) return resource;
        }
        return null;
    }

    /**
     * @return the OntologyTerm for the specified URI.
     */
    public OntologyTerm getTerm( String uri ) {
        for ( AbstractOntologyService ontology : ontologyServices ) {
            OntologyTerm term = ontology.getTerm( uri );
            if ( term != null ) return term;
        }
        return null;
    }

    /**
     * Will persist the give vocab characteristic to each biomaterial id supplied in the list.
     * 
     * @param vc
     * @param bmIdList
     */
    public void removeBioMaterialStatement( Collection<Long> characterIds, Collection<Long> bmIdList ) {

        log.debug( "Vocab Characteristic: " + characterIds );
        log.debug( "biomaterial ID List: " + bmIdList );

        Collection<BioMaterial> bms = bioMaterialService.loadMultiple( bmIdList );

        for ( BioMaterial bm : bms ) {

            Collection<Characteristic> current = bm.getCharacteristics();
            if ( current == null ) continue;

            Collection<Characteristic> found = new HashSet<Characteristic>();

            for ( Characteristic characteristic : current ) {
                if ( characterIds.contains( characteristic.getId() ) ) found.add( characteristic );

            }
            if ( found.size() == 0 ) continue;

            current.removeAll( found );

            for ( Characteristic characteristic : found ) {
                log.info( "Removing characteristic from " + bm + " : " + characteristic );
            }

            bm.setCharacteristics( current );
            bioMaterialService.update( bm );

        }

        for ( Long id : characterIds ) {
            characteristicService.delete( id );
        }
    }

    /**
     * @param characterId characteristic id
     * @param bm
     */
    public void removeBioMaterialStatement( Long characterId, BioMaterial bm ) {
        Characteristic vc = characteristicService.load( characterId );
        if ( vc == null )
            throw new IllegalArgumentException( "No characteristic with id=" + characterId + " was foundF" );
        bm.getCharacteristics().remove( vc );
        characteristicService.delete( characterId );
    }

    /**
     * Will persist the give vocab characteristic to the given biomaterial
     * 
     * @param vc
     * @param bm
     */
    public void saveBioMaterialStatement( Characteristic vc, BioMaterial bm ) {

        log.debug( "Vocab Characteristic: " + vc );

        vc.setEvidenceCode( GOEvidenceCode.IC ); // manually added characteristic
        Set<Characteristic> chars = new HashSet<Characteristic>();
        chars.add( vc );

        Collection<Characteristic> current = bm.getCharacteristics();
        if ( current == null )
            current = new HashSet<Characteristic>( chars );
        else
            current.addAll( chars );

        for ( Characteristic characteristic : chars ) {
            log.info( "Adding characteristic to " + bm + " : " + characteristic );
        }

        bm.setCharacteristics( current );
        bioMaterialService.update( bm );

    }

    /**
     * Will persist the give vocab characteristic to each biomaterial id supplied in the list.
     * 
     * @param vc
     * @param bioMaterialIdList
     */
    public void saveBioMaterialStatement( Characteristic vc, Collection<Long> bioMaterialIdList ) {

        log.debug( "Vocab Characteristic: " + vc );
        log.debug( "Biomaterial ID List: " + bioMaterialIdList );

        Collection<BioMaterial> biomaterials = bioMaterialService.loadMultiple( bioMaterialIdList );

        for ( BioMaterial bioM : biomaterials ) {
            saveBioMaterialStatement( vc, bioM );
        }

    }

    /**
     * Will persist the give vocab characteristic to the expression experiment.
     * 
     * @param vc . If the evidence code is null, it will be filled in with IC. A category and value must be provided.
     * @param ee
     */
    public void saveExpressionExperimentStatement( Characteristic vc, ExpressionExperiment ee ) {
        if ( vc == null ) {
            throw new IllegalArgumentException( "Null characteristic" );
        }
        if ( StringUtils.isBlank( vc.getCategory() ) ) {
            throw new IllegalArgumentException( "Must provide a category" );
        }

        if ( StringUtils.isBlank( vc.getValue() ) ) {
            throw new IllegalArgumentException( "Must provide a value" );
        }

        if ( vc.getEvidenceCode() == null ) {
            vc.setEvidenceCode( GOEvidenceCode.IC ); // assume: manually added
            // characteristic
        }

        Set<Characteristic> chars = new HashSet<Characteristic>();
        chars.add( vc );
        Collection<Characteristic> current = ee.getCharacteristics();
        if ( current == null ) {
            current = new HashSet<Characteristic>( chars );
        } else {
            current.addAll( chars );
        }

        log.info( "Adding characteristic to " + ee + " : " + vc );

        ee.setCharacteristics( current );
        eeService.update( ee );

    }

    /**
     * @param vc
     * @param ee
     */
    public void saveExpressionExperimentStatements( Collection<Characteristic> vc, ExpressionExperiment ee ) {
        for ( Characteristic characteristic : vc ) {
            saveExpressionExperimentStatement( characteristic, ee );
        }
    }

    /**
     * @param bioMaterialService the bioMaterialService to set
     */
    public void setBioMaterialService( BioMaterialService bioMaterialService ) {
        this.bioMaterialService = bioMaterialService;
    }

    /**
     * @param characteristicService the characteristicService to set
     */
    public void setCharacteristicService( CharacteristicService characteristicService ) {
        this.characteristicService = characteristicService;
    }

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.eeService = expressionExperimentService;
    }

    /**
     * This is provided for testing purposes. Normally this is set internally.
     * 
     * @param fmaOntologyService the fmaOntologyService to set
     */
    public void setFmaOntologyService( FMAOntologyService fmaOntologyService ) {
        if ( this.ontologyServices.contains( this.fmaOntologyService ) ) {
            this.ontologyServices.remove( this.fmaOntologyService );
        }
        this.fmaOntologyService = fmaOntologyService;
        this.ontologyServices.add( this.fmaOntologyService );
    }

    /**
     * @param searchService the searchService to set
     */
    public void setSearchService( SearchService searchService ) {
        this.searchService = searchService;
    }

    /**
     * Given a collection of ontology terms converts them to a collection of VocabCharacteristics
     * 
     * @param terms
     * @param filterTerm
     * @return
     */
    private Collection<VocabCharacteristic> convert( final Collection<OntologyResource> resources ) {

        Collection<VocabCharacteristic> converted = new HashSet<VocabCharacteristic>();

        if ( ( resources == null ) || ( resources.isEmpty() ) ) return converted;

        for ( OntologyResource res : resources ) {
            VocabCharacteristic vc = VocabCharacteristic.Factory.newInstance();

            // If there is no URI we don't want to send it back (ie useless)
            if ( ( res.getUri() == null ) || StringUtils.isEmpty( res.getUri() ) ) continue;

            if ( res instanceof OntologyTerm ) {
                OntologyTerm term = ( OntologyTerm ) res;
                vc.setValue( term.getTerm() );
                vc.setValueUri( term.getUri() );
                vc.setDescription( term.getComment() );
            }
            if ( res instanceof OntologyIndividual ) {
                OntologyIndividual indi = ( OntologyIndividual ) res;
                vc.setValue( indi.getLabel() );
                vc.setValueUri( indi.getUri() );
                vc.setDescription( "Individual" );
            }

            converted.add( vc );
        }

        return converted;
    }

    /**
     * Given a collection of ontology terms will filter out all the terms that don't have the filter term in their
     * label.
     * 
     * @param terms
     * @param filterTerm
     * @return
     */
    private Collection<VocabCharacteristic> filter( final Collection<OntologyResource> terms, final String filter ) {

        Collection<VocabCharacteristic> filtered = new HashSet<VocabCharacteristic>();

        if ( ( terms == null ) || ( terms.isEmpty() ) ) return filtered;

        String caseInsensitiveFilter = filter.toLowerCase().trim();

        for ( OntologyResource res : terms ) {
            if ( StringUtils.isNotEmpty( res.getLabel() )
                    && res.getLabel().toLowerCase().indexOf( caseInsensitiveFilter ) > -1) {
                VocabCharacteristic vc = VocabCharacteristic.Factory.newInstance();
                if ( res instanceof OntologyTerm ) {
                    OntologyTerm term = ( OntologyTerm ) res;
                    vc.setValue( term.getTerm() );
                    vc.setValueUri( term.getUri() );
                    vc.setDescription( term.getComment() );
                } else if ( res instanceof OntologyIndividual ) {
                    OntologyIndividual indi = ( OntologyIndividual ) res;
                    vc.setValue( indi.getLabel() );
                    vc.setValueUri( indi.getUri() );
                    vc.setDescription( "Individual" );
                }

                filtered.add( vc );
            }
        }
        log.debug( "returning " + filtered.size() + " terms after filter" );

        return filtered;
    }
    
    /**
     * @param c
     * @return
     */
    private String foundValueKey( Characteristic c ) {
        if ( c instanceof VocabCharacteristic ) {
            return ( ( VocabCharacteristic ) c ).getValueUri();
        }
        return c.getValue();
    }

    /**
     * Allow us to store gene information as a characteristic associated with our entities. This doesn't work so well
     * for non-ncbi genes.
     * 
     * @param g
     * @return
     */
    private Characteristic gene2Characteristic( Gene g ) {
        VocabCharacteristic vc = VocabCharacteristic.Factory.newInstance();
        vc.setCategory( "gene" );
        vc.setCategoryUri( "http://purl.org/commons/hcls/gene" );
        vc.setValue( g.getOfficialSymbol() + " [" + g.getTaxon().getCommonName() + "]" + " " + g.getOfficialName() );
        vc.setDescription( g.toString() );
        if ( g.getNcbiId() != null ) {
            vc.setValueUri( "http://purl.org/commons/record/ncbi_gene/" + g.getNcbiId() );
        }
        return vc;
    }

    /**
     * @param queryString
     * @param categoryUri
     * @param taxon okay if null
     * @param searchResults
     */
    private void searchForGenes( String queryString, String categoryUri, Taxon taxon,
            Collection<Characteristic> searchResults ) {
        if ( categoryUri == null ) return;

        if ( categoryUri.equals( "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#GeneticModification" )
                || categoryUri
                        .equals( "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#IndividualGeneticCharacteristics" )
                || categoryUri.equals( "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#Genotype" ) ) {

            /*
             * Kick into a special search for genes. The user will have to deal with choosing one from the right taxon.
             */
            SearchSettings ss = new SearchSettings();
            ss.setQuery( queryString );
            ss.noSearches();
            ss.setTaxon( taxon );
            ss.setSearchGenes( true );
            Map<Class<?>, List<SearchResult>> geneResults = this.searchService.search( ss, true );

            if ( geneResults.containsKey( Gene.class ) ) {
                for ( SearchResult sr : geneResults.get( Gene.class ) ) {
                    Gene g = ( Gene ) sr.getResultObject();
                    log.debug( "Search for " + queryString + " returned: " + g );
                    searchResults.add( gene2Characteristic( g ) );
                }
            }
        }
    }

    /**
     * @param individualResults
     * @param alreadyUsedResults
     * @param searchResults
     * @param searchTerm
     * @param foundValues
     * @return
     */
    private Collection<Characteristic> sort( Collection<Characteristic> individualResults,
            Collection<Characteristic> alreadyUsedResults, Collection<Characteristic> searchResults, String searchTerm,
            Collection<String> foundValues ) {

        /*
         * Organize the list into 3 parts. Want to get the exact match showing up on top But close matching
         * individualResults and alreadyUsedResults should get priority over jena's search results. Each result's order
         * should be preserved.
         */

        List<Characteristic> sortedResultsExact = new ArrayList<Characteristic>();
        List<Characteristic> sortedResultsStartsWith = new ArrayList<Characteristic>();
        List<Characteristic> sortedResultsBottom = new ArrayList<Characteristic>();

        for ( Characteristic characteristic : alreadyUsedResults ) {
            if ( characteristic.getValue().equalsIgnoreCase( searchTerm ) ) {
                sortedResultsExact.add( characteristic );
            } else if ( characteristic.getValue().startsWith( searchTerm ) ) {
                sortedResultsStartsWith.add( characteristic );
            } else {
                sortedResultsBottom.add( characteristic );
            }
        }

        for ( Characteristic characteristic : individualResults ) {
            String key = foundValueKey( characteristic );
            if ( foundValues.contains( key ) ) continue;

            foundValues.add( key );

            if ( characteristic.getValue().equalsIgnoreCase( searchTerm ) ) {
                sortedResultsExact.add( characteristic );
            } else if ( characteristic.getValue().startsWith( searchTerm ) ) {
                sortedResultsStartsWith.add( characteristic );
            } else {
                sortedResultsBottom.add( characteristic );
            }
        }

        for ( Characteristic characteristic : searchResults ) {
            String key = foundValueKey( characteristic );
            if ( foundValues.contains( key ) ) continue;
            foundValues.add( key );
            if ( characteristic.getValue().equalsIgnoreCase( searchTerm ) ) {
                sortedResultsExact.add( characteristic );
            } else if ( characteristic.getValue().startsWith( searchTerm ) ) {
                sortedResultsStartsWith.add( characteristic );
            } else {
                sortedResultsBottom.add( characteristic );
            }
        }

        Collections.sort( sortedResultsExact, new CharacteristicComparator() );
        Collections.sort( sortedResultsStartsWith, new CharacteristicComparator() );
        Collections.sort( sortedResultsBottom, new CharacteristicComparator() );

        List<Characteristic> sortedTerms = new ArrayList<Characteristic>( foundValues.size() );
        sortedTerms.addAll( sortedResultsExact );
        sortedTerms.addAll( sortedResultsStartsWith );
        sortedTerms.addAll( sortedResultsBottom );

        return sortedTerms;
    }

    private class CharacteristicComparator implements Comparator<Characteristic> {

        /*
         * (non-Javadoc)
         * 
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare( Characteristic o1, Characteristic o2 ) {

            if ( o1.getValue().length() == o2.getValue().length() ) {
                if ( o1.getDescription().startsWith( USED ) ) {
                    if ( o2.getDescription().startsWith( USED ) ) {
                        return compareByUri( o1, o2 );
                    }
                    // o1 is used, o2 is not
                    return -1;

                } else if ( o2.getDescription().startsWith( USED ) ) {
                    // o2 is used and o1 is not.
                    return -1;
                } else {
                    // neither is used.
                    return compareByUri( o1, o2 );
                }
            }
            return o1.getValue().length() < o2.getValue().length() ? -1 : 1;
        }

        private int compareByUri( Characteristic o1, Characteristic o2 ) {
            // both are used. Break tie based on whether it has a URI
            if ( o1 instanceof VocabCharacteristic && ( ( VocabCharacteristic ) o1 ).getValueUri() != null ) {
                if ( !( o2 instanceof VocabCharacteristic ) || ( ( VocabCharacteristic ) o2 ).getValueUri() == null ) {
                    return -1;
                }
                // both have URIs
                return ( ( VocabCharacteristic ) o1 ).getValueUri().compareTo(
                        ( ( VocabCharacteristic ) o2 ).getValueUri() );
            } else if ( o2 instanceof VocabCharacteristic && ( ( VocabCharacteristic ) o2 ).getValueUri() != null ) {
                // we know o1 does not have a uri.
                return 1;
            }
            // both not having uris
            return o1.getValue().toLowerCase().compareTo( o2.getValue().toLowerCase() );
        }
    }

}