/*

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
package ubic.gemma.core.ontology.providers;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.larq.IndexLARQ;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.ontology.OntologyLoader;
import ubic.basecode.ontology.model.AnnotationProperty;
import ubic.basecode.ontology.model.OntologyClassRestriction;
import ubic.basecode.ontology.model.OntologyResource;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.search.OntologyIndexer;
import ubic.basecode.ontology.search.OntologySearch;
import ubic.basecode.ontology.search.SearchIndex;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.association.Gene2GOAssociationService;
import ubic.gemma.persistence.util.Settings;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Holds a complete copy of the GeneOntology. This gets loaded on startup.
 *
 * @author pavlidis
 * @version $Id$
 */
@Component
public class GeneOntologyServiceImpl implements GeneOntologyService {

    private static final String ALL_ROOT = BASE_GO_URI + "ALL";
    /*
     * FIXME don't hardcode this.
     */
    private final static String GO_URL = "http://purl.obolibrary.org/obo/go.owl";
    private final static boolean LOAD_BY_DEFAULT = true;
    private static final String LOAD_GENE_ONTOLOGY_OPTION = "load.geneOntology";
    private static final String PART_OF_URI = "http://purl.obolibrary.org/obo/BFO_0000050";
    private static final AtomicBoolean ready = new AtomicBoolean( false );
    private static final AtomicBoolean running = new AtomicBoolean( false );
    private static boolean enabled = true;
    private static Log log = LogFactory.getLog( GeneOntologyServiceImpl.class.getName() );
    // cache
    private static Map<String, GOAspect> term2Aspect = new HashMap<>();
    // cache
    private static Map<String, OntologyTerm> uri2Term = new HashMap<>();
    /**
     * Cache of go term -> child terms
     */
    private Map<String, Collection<OntologyTerm>> childrenCache = Collections
            .synchronizedMap( new HashMap<String, Collection<OntologyTerm>>() );

    private Gene2GOAssociationService gene2GOAssociationService;
    private GeneService geneService;
    /**
     * Cache of gene -> go terms.
     */
    private Map<Gene, Collection<OntologyTerm>> goTerms = new HashMap<Gene, Collection<OntologyTerm>>();
    private Collection<SearchIndex> indices = new HashSet<>();
    private OntModel model;
    /**
     * Cache of go term -> parent terms
     */
    private Map<String, Collection<OntologyTerm>> parentsCache = Collections
            .synchronizedMap( new HashMap<String, Collection<OntologyTerm>>() );

    /**
     * @return Usual formatted GO id, e.g., GO:0039392
     */
    public static String asRegularGoId( Characteristic term ) {
        String uri = term.getValue();
        return asRegularGoId( uri );
    }

    /**
     * @param term
     * @return Usual formatted GO id, e.g., GO:0039392
     */
    public static String asRegularGoId( OntologyTerm term ) {
        if ( term == null )
            return null;
        String uri = term.getUri();
        return asRegularGoId( uri );
    }

    public static String asRegularGoId( String uri ) {
        return uri.replaceAll( ".*?/", "" ).replace( "_", ":" );
    }

    /**
     * @param goId e.g. GO:0001312
     * @return null if not found
     */
    public static OntologyTerm getTermForId( String goId ) {
        if ( uri2Term == null )
            return null;

        if ( !uri2Term.containsKey( toUri( goId ) ) ) {
            log.warn( "GOID " + goId + " not recognized?" );
        }

        return uri2Term.get( toUri( goId ) );
    }

    /**
     * @return null if not found
     */
    public static OntologyTerm getTermForURI( String uri ) {
        if ( uri2Term == null || !uri2Term.containsKey( uri ) )
            return null;
        return uri2Term.get( uri );
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Turn an id like GO:0038128 into a URI.
     */
    private static String toUri( String goId ) {
        String uriTerm = goId.replace( ":", "_" );
        return BASE_GO_URI + uriTerm;
    }

    @Autowired
    public void setGene2GOAssociationService( Gene2GOAssociationService gene2GOAssociationService ) {
        this.gene2GOAssociationService = gene2GOAssociationService;
    }

    @Autowired
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    @Override
    public void afterPropertiesSet() {
        this.init( false );
    }

    @Override
    public Map<Long, Collection<OntologyTerm>> calculateGoTermOverlap( Gene queryGene, Collection<Long> geneIds ) {

        Map<Long, Collection<OntologyTerm>> overlap = new HashMap<Long, Collection<OntologyTerm>>();
        if ( queryGene == null )
            return null;
        if ( geneIds.size() == 0 )
            return overlap;

        Collection<OntologyTerm> queryGeneTerms = getGOTerms( queryGene );

        overlap.put( queryGene.getId(), queryGeneTerms ); // include the query gene in the list. Clearly 100% overlap
        // with itself!

        Collection<Gene> genes = this.geneService.load( geneIds );

        putOverlapGenes( overlap, queryGeneTerms, genes );

        return overlap;
    }

    @Override
    public Collection<OntologyTerm> calculateGoTermOverlap( Gene queryGene1, Gene queryGene2 ) {

        if ( queryGene1 == null || queryGene2 == null )
            return null;

        Collection<OntologyTerm> queryGeneTerms1 = getGOTerms( queryGene1 );
        Collection<OntologyTerm> queryGeneTerms2 = getGOTerms( queryGene2 );

        return computeOverlap( queryGeneTerms1, queryGeneTerms2 );
    }

    @Override
    public Map<Long, Collection<OntologyTerm>> calculateGoTermOverlap( Long queryGene, Collection<Long> geneIds ) {
        Map<Long, Collection<OntologyTerm>> overlap = new HashMap<Long, Collection<OntologyTerm>>();
        if ( queryGene == null )
            return null;
        if ( geneIds.size() == 0 )
            return overlap;

        Collection<OntologyTerm> queryGeneTerms = getGOTerms( queryGene );

        overlap.put( queryGene, queryGeneTerms ); // include the query gene in the list. Clearly 100% overlap
        // with itself!

        Collection<Gene> genes = this.geneService.load( geneIds );

        putOverlapGenes( overlap, queryGeneTerms, genes );

        return overlap;
    }

    @Override
    public Collection<OntologyTerm> computeOverlap( Collection<OntologyTerm> masterOntos,
            Collection<OntologyTerm> comparisonOntos ) {
        Collection<OntologyTerm> overlapTerms = new HashSet<OntologyTerm>( masterOntos );
        overlapTerms.retainAll( comparisonOntos );

        return overlapTerms;
    }

    @Override
    public Collection<OntologyTerm> findTerm( String queryString ) {

        if ( !isReady() )
            return new HashSet<OntologyTerm>();

        if ( log.isDebugEnabled() )
            log.debug( "Searching Gene Ontology for '" + queryString + "'" );

        // make sure we are all-inclusive
        queryString = queryString.trim();
        queryString = queryString.replaceAll( "\\s+", " AND " );

        StopWatch timer = new StopWatch();
        timer.start();
        Collection<OntologyResource> rawMatches = new HashSet<>();
        for ( SearchIndex index : this.indices ) {
            rawMatches.addAll( OntologySearch.matchIndividuals( model, index, queryString ) );
        }
        if ( timer.getTime() > 100 ) {
            log.info( "Find " + rawMatches.size() + " raw go terms from " + queryString + ": " + timer.getTime()
                    + " ms" );
        }
        timer.reset();
        timer.start();

        /*
         * Required to make sure the descriptions are filled in.
         */
        Collection<OntologyTerm> matches = new HashSet<>();
        for ( OntologyResource r : rawMatches ) {
            if ( StringUtils.isBlank( r.getUri() ) )
                continue;
            OntologyTerm termForURI = GeneOntologyServiceImpl.getTermForURI( r.getUri() );
            if ( termForURI == null ) {
                log.warn( "No term for : " + r );
                continue;
            }
            matches.add( termForURI );
        }

        if ( timer.getTime() > 100 ) {
            log.info( "Convert " + rawMatches.size() + " raw go terms to terms: " + timer.getTime() + " ms" );
        }

        return matches;
    }

    @Override
    public Collection<OntologyTerm> getAllChildren( OntologyTerm entry ) {
        return getAllChildren( entry, false );
    }

    @Override
    public Collection<OntologyTerm> getAllChildren( OntologyTerm entry, boolean includePartOf ) {
        return getDescendants( entry, includePartOf );
    }

    @Override
    public Collection<String> getAllGOTermIds() {
        Collection<String> goTermIds = uri2Term.keySet();
        return goTermIds;
    }

    @Override
    public Collection<OntologyTerm> getAllParents( Collection<OntologyTerm> entries ) {
        return getAllParents( entries, false );
    }

    @Override
    public Collection<OntologyTerm> getAllParents( Collection<OntologyTerm> entries, boolean includePartOf ) {
        if ( entries == null )
            return null;
        Collection<OntologyTerm> result = new HashSet<OntologyTerm>();
        for ( OntologyTerm entry : entries ) {
            result.addAll( getAncestors( entry, includePartOf ) );
        }
        return result;
    }

    @Override
    public Collection<OntologyTerm> getAllParents( OntologyTerm entry ) {
        return getAllParents( entry, true );
    }

    @Override
    public Collection<OntologyTerm> getAllParents( OntologyTerm entry, boolean includePartOf ) {
        if ( entry == null )
            return new HashSet<OntologyTerm>();
        return getAncestors( entry, includePartOf );
    }

    @Override
    public Collection<OntologyTerm> getChildren( OntologyTerm entry ) {
        return getChildren( entry, false );

    }

    @Override
    public Collection<OntologyTerm> getChildren( OntologyTerm entry, boolean includePartOf ) {
        if ( entry == null )
            return null;
        if ( log.isDebugEnabled() )
            log.debug( "Getting children of " + entry );
        Collection<OntologyTerm> terms = entry.getChildren( true );

        if ( includePartOf )
            terms.addAll( getPartsOf( entry ) );

        return terms;
    }

    @Override
    public Collection<Gene> getGenes( String goId, Taxon taxon ) {
        OntologyTerm t = getTermForId( goId );
        if ( t == null )
            return null;
        Collection<OntologyTerm> terms = getAllChildren( t );
        Collection<Gene> results = new HashSet<Gene>( this.gene2GOAssociationService.findByGOTerm( goId, taxon ) );

        for ( OntologyTerm term : terms ) {
            results.addAll( this.gene2GOAssociationService.findByGOTerm( asRegularGoId( term ), taxon ) );
        }
        return results;
    }

    @Override
    public Collection<OntologyTerm> getGOTerms( Gene gene ) {
        return getGOTerms( gene, true, null );
    }

    @Override
    public Collection<OntologyTerm> getGOTerms( Gene gene, boolean includePartOf ) {
        return getGOTerms( gene, includePartOf, null );
    }

    @Override
    public Collection<OntologyTerm> getGOTerms( Gene gene, boolean includePartOf, GOAspect goAspect ) {
        Collection<OntologyTerm> cachedTerms = goTerms.get( gene );
        if ( log.isTraceEnabled() && cachedTerms != null ) {
            logIds( "found cached GO terms for " + gene.getOfficialSymbol(), goTerms.get( gene ) );
        }

        if ( cachedTerms == null ) {
            Collection<OntologyTerm> allGOTermSet = new HashSet<OntologyTerm>();

            Collection<VocabCharacteristic> annotations = gene2GOAssociationService.findByGene( gene );
            for ( VocabCharacteristic c : annotations ) {
                if ( !uri2Term.containsKey( c.getValueUri() ) ) {
                    log.warn( "Term " + c.getValueUri() + " not found in term list cant add to results" );
                    continue;
                }
                allGOTermSet.add( uri2Term.get( c.getValueUri() ) );
            }

            allGOTermSet.addAll( getAllParents( allGOTermSet, includePartOf ) );

            cachedTerms = Collections.unmodifiableCollection( allGOTermSet );
            if ( log.isTraceEnabled() )
                logIds( "caching GO terms for " + gene.getOfficialSymbol(), allGOTermSet );
            goTerms.put( gene, cachedTerms );
        }

        if ( goAspect != null ) {

            Collection<OntologyTerm> finalTerms = new HashSet<OntologyTerm>();

            for ( OntologyTerm ontologyTerm : cachedTerms ) {
                if ( getTermAspect( ontologyTerm ).equals( goAspect ) ) {
                    finalTerms.add( ontologyTerm );
                }
            }

            return finalTerms;
        }

        return cachedTerms;
    }

    @Override
    public Collection<OntologyTerm> getGOTerms( Long geneId ) {
        return getGOTerms( geneId, true, null );
    }

    /**
     * FIXME it might be better to avoid the fetch.
     */
    public Collection<OntologyTerm> getGOTerms( Long gene, boolean includePartOf, GOAspect goAspect ) {
        return this.getGOTerms( geneService.load( gene ), includePartOf, goAspect );
    }

    @Override
    public Collection<OntologyTerm> getParents( OntologyTerm entry ) {
        return getParents( entry, false );
    }

    @Override
    public Collection<OntologyTerm> getParents( OntologyTerm entry, boolean includePartOf ) {
        Collection<OntologyTerm> parents = entry.getParents( true );
        Collection<OntologyTerm> results = new HashSet<OntologyTerm>();
        for ( OntologyTerm term : parents ) {
            // The isRoot() returns true for the MolecularFunction, BiologicalProcess, CellularComponent
            if ( term.isRoot() )
                continue;
            if ( term.getUri().equalsIgnoreCase( ALL_ROOT ) )
                continue;

            if ( term instanceof OntologyClassRestriction ) {
                // log.info( "Skipping " + term );
                // OntologyProperty restrictionOn = ( ( OntologyClassRestriction ) term ).getRestrictionOn();
                // if ( restrictionOn.getLabel().equals( "part_of" ) ) {
                // OntologyTerm restrictedTo = ( ( OntologyClassRestriction ) term ).getRestrictedTo();
                // results.add( restrictedTo );
                // }
                // don't keep it. - for example, "ends_during" RO_0002093
            } else {
                // log.info( "Adding " + term );
                results.add( term );
            }
        }

        if ( includePartOf )
            results.addAll( getIsPartOf( entry ) );

        return results;
    }

    @Override
    public Collection<OntologyTerm> listTerms() {
        return uri2Term.values();
    }

    @Override
    public GOAspect getTermAspect( String goId ) {
        OntologyTerm term = getTermForId( goId );
        if ( term == null )
            return null;
        return getTermAspect( term );
    }

    @Override
    public GOAspect getTermAspect( VocabCharacteristic goId ) {
        String string = asRegularGoId( goId );
        return getTermAspect( string );
    }

    @Override
    public String getTermDefinition( String goId ) {
        OntologyTerm t = getTermForId( goId );
        assert t != null;
        Collection<AnnotationProperty> annotations = t.getAnnotations();
        for ( AnnotationProperty annot : annotations ) {
            log.info( annot.getProperty() );
            if ( annot.getProperty().equals( "hasDefinition" ) ) {
                return annot.getContents();
            }
        }
        return null;
    }

    @Override
    public String getTermName( String goId ) {

        OntologyTerm t = getTermForId( goId );
        if ( t == null )
            return "[Not available]"; // not ready yet?
        return t.getTerm();
    }

    @Override
    public Boolean isAChildOf( OntologyTerm parent, OntologyTerm potentialChild ) {
        return isAParentOf( potentialChild, parent );
    }

    @Override
    public Boolean isAParentOf( OntologyTerm child, OntologyTerm potentialParent ) {
        if ( potentialParent.isRoot() )
            return true; // well....
        Collection<OntologyTerm> parents = getAllParents( child );
        return parents.contains( potentialParent );
    }

    @Override
    public Boolean isAValidGOId( String goId ) {
        if ( !this.isReady() ) {
            throw new UnsupportedOperationException( "Gene ontology isn't ready so cannot check validity of IDs" );
        }
        if ( uri2Term.containsKey( toUri( goId ) ) )
            return true;
        return uri2Term.containsKey( toUri( goId.replaceFirst( "_", ":" ) ) );
    }

    @Override
    public boolean isBiologicalProcess( OntologyTerm term ) {

        GOAspect nameSpace = getTermAspect( term );
        if ( nameSpace == null ) {
            log.warn( "No namespace for " + term + ", assuming not Biological Process" );
            return false;
        }

        return nameSpace.equals( GOAspect.BIOLOGICAL_PROCESS );
    }

    @Override
    public boolean isReady() {
        return ready.get();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void loadTermsInNameSpace( InputStream is ) {
        this.model = OntologyLoader.loadMemoryModel( is, null, OntModelSpec.OWL_MEM );
        Collection<OntologyResource> terms = OntologyLoader.initialize( null, model );
        this.indices.add( OntologyIndexer.indexOntology( "GeneOntology", model ) );
        uri2Term.clear();
        addTerms( terms );
        ready.set( true );
    }

    @Override
    public void shutDown() {
        if ( this.isReady() ) {
            try {
                this.goTerms.clear();
                this.childrenCache.clear();
                this.parentsCache.clear();
                term2Aspect.clear();
                for ( IndexLARQ l : indices ) {
                    l.close();
                }

                this.model.close();
                this.model = null;
            } catch ( Exception e ) {
                throw new RuntimeException( e );
            } finally {
                ready.set( false );
                running.set( false );
            }

        }

    }

    @Override
    public synchronized void init( boolean force ) {

        if ( running.get() ) {
            log.warn( "Gene Ontology initialization is already running" );
            return;
        }

        boolean loadOntology = Settings.getBoolean( LOAD_GENE_ONTOLOGY_OPTION, LOAD_BY_DEFAULT );

        if ( !force && !loadOntology ) {
            log.info( "Loading Gene Ontology is disabled (force=" + force + ", " + LOAD_GENE_ONTOLOGY_OPTION + "="
                    + loadOntology + ")" );
            enabled = false;
            return;
        }

        initializeGeneOntology();
    }

    @Override
    public synchronized boolean isGeneOntologyLoaded() {

        return ready.get();
    }

    /**
     *
     */
    protected synchronized void forceLoadOntology() {
        initializeGeneOntology();
    }

    /**
     * @param url
     * @throws IOException
     */
    protected void loadTermsInNameSpace( String url ) {
        this.model = OntologyLoader.loadMemoryModel( url, OntModelSpec.OWL_MEM );
        Collection<OntologyResource> terms = OntologyLoader.initialize( url, model );
        this.indices.add( OntologyIndexer.indexOntology( url.replaceFirst( ".*/", "" ).replace( ".owl", "" ), model ) );
        addTerms( terms );
    }

    private void putOverlapGenes( Map<Long, Collection<OntologyTerm>> overlap, Collection<OntologyTerm> queryGeneTerms,
            Collection<Gene> genes ) {
        for ( Object obj : genes ) {
            Gene gene = ( Gene ) obj;
            if ( queryGeneTerms.isEmpty() ) {
                overlap.put( gene.getId(), new HashSet<OntologyTerm>() );
                continue;
            }

            Collection<OntologyTerm> comparisonOntos = getGOTerms( gene );

            if ( comparisonOntos == null || comparisonOntos.isEmpty() ) {
                overlap.put( gene.getId(), new HashSet<OntologyTerm>() );
                continue;
            }

            overlap.put( gene.getId(), computeOverlap( queryGeneTerms, comparisonOntos ) );
        }
    }

    private void addTerms( Collection<OntologyResource> newTerms ) {

        for ( OntologyResource term : newTerms ) {
            if ( term.getUri() == null )
                continue;
            if ( term instanceof OntologyTerm ) {
                OntologyTerm ontTerm = ( OntologyTerm ) term;
                uri2Term.put( term.getUri(), ontTerm );
                for ( String alternativeID : ontTerm.getAlternativeIds() ) {
                    log.debug( toUri( alternativeID ) );
                    uri2Term.put( toUri( alternativeID ), ontTerm );
                }
            }
        }
    }

    /**
     * Return terms to which the given term has a part_of relation (it is "part_of" them).
     */
    private Collection<OntologyTerm> getIsPartOf( OntologyTerm entry ) {
        Collection<OntologyTerm> r = new HashSet<OntologyTerm>();
        String u = entry.getUri();
        String queryString = "SELECT ?x WHERE {  <" + u + ">  <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?v . "
                + "?v <http://www.w3.org/2002/07/owl#onProperty>  <" + PART_OF_URI + "> . "
                + "?v <http://www.w3.org/2002/07/owl#someValuesFrom> ?x . }";
        Query q = QueryFactory.create( queryString );
        QueryExecution qexec = QueryExecutionFactory.create( q, ( Model ) entry.getModel() );
        try {
            ResultSet results = qexec.execSelect();
            while ( results.hasNext() ) {
                QuerySolution soln = results.nextSolution();
                Resource x = soln.getResource( "x" );
                if ( x.isAnon() )
                    continue; // some reasoners will return these.
                String uri = x.getURI();
                if ( log.isDebugEnabled() )
                    log.debug( entry + " is part of " + uri2Term.get( uri ) );
                r.add( uri2Term.get( uri ) );
            }
        } finally {
            qexec.close();
        }
        return r;
    }

    /**
     * Return terms which have "part_of" relation with the given term (they are "part_of" the given term).
     */
    private Collection<OntologyTerm> getPartsOf( OntologyTerm entry ) {
        Collection<OntologyTerm> r = new HashSet<OntologyTerm>();
        String u = entry.getUri();
        String queryString = "SELECT ?x WHERE {" + "?x <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?v . "
                + "?v <http://www.w3.org/2002/07/owl#onProperty> <" + PART_OF_URI + ">  . "
                + "?v <http://www.w3.org/2002/07/owl#someValuesFrom> <" + u + "> . }";
        Query q = QueryFactory.create( queryString );
        QueryExecution qexec = QueryExecutionFactory.create( q, ( Model ) entry.getModel() );
        try {
            ResultSet results = qexec.execSelect();
            while ( results.hasNext() ) {
                QuerySolution soln = results.nextSolution();
                Resource x = soln.getResource( "x" );
                String uri = x.getURI();
                if ( x.isAnon() )
                    continue; // some reasoners will return these.
                if ( log.isDebugEnabled() )
                    log.debug( uri2Term.get( uri ) + " is part of " + entry );
                r.add( uri2Term.get( uri ) );
            }
        } finally {
            qexec.close();
        }
        return r;
    }

    private GOAspect getTermAspect( OntologyTerm term ) {
        assert term != null;
        String goid = term.getTerm();

        if ( term2Aspect.containsKey( goid ) ) {
            return term2Aspect.get( goid );
        }

        String nameSpace = null;
        for ( AnnotationProperty annot : term.getAnnotations() ) {
            /*
             * Why they changed this, I can't say. It used to be hasOBONamespace but now comes through as
             * has_obo_namespace.
             */
            if ( annot.getProperty().equals( "hasOBONamespace" ) || annot.getProperty()
                    .equals( "has_obo_namespace" ) ) {
                nameSpace = annot.getContents();
                break;
            }
        }

        if ( nameSpace == null ) {
            log.warn( "aspect could not be determined for: " + term );
            return null;
        }

        GOAspect aspect = GOAspect.valueOf( nameSpace.toUpperCase() );
        term2Aspect.put( goid, aspect );

        return aspect;
    }

    private synchronized Collection<OntologyTerm> getAncestors( OntologyTerm entry, boolean includePartOf ) {

        if ( entry == null ) {
            return new HashSet<OntologyTerm>();
        }

        Collection<OntologyTerm> ancestors = parentsCache.get( entry.getUri() );
        if ( ancestors == null ) {
            ancestors = new HashSet<OntologyTerm>();

            Collection<OntologyTerm> parents = getParents( entry, includePartOf );
            if ( parents != null ) {
                for ( OntologyTerm parent : parents ) {
                    ancestors.add( parent );
                    ancestors.addAll( getAncestors( parent, includePartOf ) );
                }
            }

            ancestors = Collections.unmodifiableCollection( ancestors );
            parentsCache.put( entry.getUri(), ancestors );
        }
        return new HashSet<OntologyTerm>( ancestors );
    }

    /**
     * @return Given an ontology term recursivly determines all the children and adds them to a cache (same as
     * getAllParents but the recusive code is a little cleaner and doesn't use and accumulator)
     */
    private synchronized Collection<OntologyTerm> getDescendants( OntologyTerm entry, boolean includePartOf ) {

        Collection<OntologyTerm> descendants = childrenCache.get( entry.getUri() );
        if ( descendants == null ) {
            descendants = new HashSet<OntologyTerm>();

            Collection<OntologyTerm> children = getChildren( entry, includePartOf );
            if ( children != null ) {
                for ( OntologyTerm child : children ) {
                    descendants.add( child );
                    descendants.addAll( getDescendants( child, includePartOf ) );
                }
            }

            descendants = Collections.unmodifiableCollection( descendants );
            childrenCache.put( entry.getUri(), descendants );
        }
        return new HashSet<OntologyTerm>( descendants );

    }

    private synchronized void initializeGeneOntology() {
        if ( running.get() )
            return;

        Thread loadThread = new Thread( new Runnable() {
            @Override
            public void run() {
                running.set( true );
                uri2Term = new HashMap<String, OntologyTerm>();
                log.info( "Loading Gene Ontology..." );
                StopWatch loadTime = new StopWatch();
                loadTime.start();
                //
                try {
                    loadTermsInNameSpace( GO_URL );

                    log.info( "Gene Ontology loaded, total of " + uri2Term.size() + " items in "
                            + loadTime.getTime() / 1000 + "s" );
                    ready.set( true );
                    running.set( false );

                    log.info( "Done loading GO" );
                    loadTime.stop();
                } catch ( Throwable e ) {
                    if ( log != null )
                        log.error( e, e );
                    ready.set( false );
                    running.set( false );
                }
            }

        } );

        loadThread.start();

    }

    private void logIds( String prefix, Collection<OntologyTerm> terms ) {
        StringBuffer buf = new StringBuffer( prefix );
        buf.append( ": [ " );
        Iterator<OntologyTerm> i = terms.iterator();
        while ( i.hasNext() ) {
            buf.append( GeneOntologyServiceImpl.asRegularGoId( i.next() ) );
            if ( i.hasNext() )
                buf.append( ", " );
        }
        buf.append( " ]" );
        log.trace( buf.toString() );
    }

    public enum GOAspect {
        BIOLOGICAL_PROCESS, CELLULAR_COMPONENT, MOLECULAR_FUNCTION
    }

}