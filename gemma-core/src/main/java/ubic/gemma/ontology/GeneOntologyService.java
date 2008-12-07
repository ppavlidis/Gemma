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
package ubic.gemma.ontology;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.ConfigUtils;

import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Holds a complete copy of the GeneOntology. This gets loaded on startup.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="geneOntologyService"
 * @spring.property name="gene2GOAssociationService" ref="gene2GOAssociationService"
 * @spring.property name="geneService" ref="geneService"
 */
public class GeneOntologyService {

    public static final String BASE_GO_URI = "http://purl.org/obo/owl/GO#";

    private static final String LOAD_GENE_ONTOLOGY_OPTION = "load.geneOntology";

    private final static String CC_URL = "http://www.berkeleybop.org/ontologies/obo-all/cellular_component/cellular_component.owl";

    private final static String BP_URL = "http://www.berkeleybop.org/ontologies/obo-all/biological_process/biological_process.owl";

    private final static String MF_URL = "http://www.berkeleybop.org/ontologies/obo-all/molecular_function/molecular_function.owl";

    private final static boolean LOAD_BY_DEFAULT = true;

    private static Log log = LogFactory.getLog( GeneOntologyService.class.getName() );

    // map of uris to terms
    private static Map<String, OntologyTerm> terms;

    // private DirectedGraph graph = null;

    private static final AtomicBoolean ready = new AtomicBoolean( false );
    private static final AtomicBoolean running = new AtomicBoolean( false );
    private static final String ALL_ROOT = BASE_GO_URI + "ALL";

    private static final String PART_OF_URI = "http://purl.org/obo/owl/OBO_REL#OBO_REL_part_of";

    private static boolean enabled = true;

    /**
     * @param term
     * @return Usual formatted GO id, e.g., GO:0039392
     */
    public static String asRegularGoId( Characteristic term ) {
        String uri = term.getValue();
        return uri.replaceAll( ".*?#", "" ).replace( "_", ":" );
    }

    /**
     * @param term
     * @return Usual formatted GO id, e.g., GO:0039392
     */
    public static String asRegularGoId( OntologyTerm term ) {
        String uri = term.getUri();
        return uri.replaceAll( ".*?#", "" ).replace( "_", ":" );
    }

    /**
     * @param goId
     * @return
     */
    public static String getTermAspect( String goId ) {
        OntologyTerm term = getTermForId( goId );
        return getTermAspect( term );
    }

    /**
     * @param goId
     * @return
     */
    public static String getTermAspect( VocabCharacteristic goId ) {
        String string = asRegularGoId( goId );
        return getTermAspect( string );
    }

    /**
     * @param goId e.g. GO:0001312
     * @return null if not found
     */
    public static OntologyTerm getTermForId( String goId ) {
        if ( terms == null ) return null;
        return terms.get( toUri( goId ) );
    }

    /*
     * @param goURI e.g. GO:0001312 @return null if not found
     */
    public static OntologyTerm getTermForURI( String uri ) {
        if ( terms == null ) return null;
        return terms.get( uri );
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * @param term
     * @return
     */
    private static String getTermAspect( OntologyTerm term ) {
        String nameSpace = null;
        for ( AnnotationProperty annot : term.getAnnotations() ) {
            if ( annot.getProperty().equals( "hasOBONamespace" ) ) {
                nameSpace = annot.getContents();
                break;
            }
        }
        return nameSpace;
    }

    /**
     * Turn an id like GO:0038128 into a URI.
     * 
     * @param goId
     * @return
     */
    private static String toUri( String goId ) {
        String uriTerm = goId.replace( ":", "_" );
        return BASE_GO_URI + uriTerm;
    }

    private Gene2GOAssociationService gene2GOAssociationService;

    private GeneService geneService;

    /**
     * Cache of go term -> child terms
     */
    private Map<String, Collection<OntologyTerm>> childrenCache = Collections
            .synchronizedMap( new HashMap<String, Collection<OntologyTerm>>() );

    /**
     * Cache of go term -> parent terms
     */
    private Map<String, Collection<OntologyTerm>> parentsCache = Collections
            .synchronizedMap( new HashMap<String, Collection<OntologyTerm>>() );

    /**
     * Cache of gene -> go terms.
     */
    private Map<Gene, Collection<OntologyTerm>> goTerms = new HashMap<Gene, Collection<OntologyTerm>>();

    /**
     * <p>
     * Given a query Gene, and a collection of gene ids calculates the go term overlap for each pair of queryGene and
     * gene in the given collection. Returns a Map<Gene,Collection<OntologyEntries>>. The key is the gene (from the
     * [queryGene,gene] pair) and the values are a collection of the overlapping ontology entries.
     * </p>
     * 
     * @param queryGene
     * @param geneIds
     * @returns map of gene ids to collections of ontologyTerms. This will always be populated but collection values
     *          will be empty when there is no overlap.
     */
    public Map<Long, Collection<OntologyTerm>> calculateGoTermOverlap( Gene queryGene, Collection<Long> geneIds ) {

        Map<Long, Collection<OntologyTerm>> overlap = new HashMap<Long, Collection<OntologyTerm>>();
        if ( queryGene == null ) return null;
        if ( geneIds.size() == 0 ) return overlap;

        Collection<OntologyTerm> queryGeneTerms = getGOTerms( queryGene );

        overlap.put( queryGene.getId(), queryGeneTerms ); // include the query gene in the list. Clearly 100% overlap
        // with itself!

        Collection<Gene> genes = this.geneService.loadMultiple( geneIds );

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

        return overlap;
    }

    /**
     * @return a collection of all existing GO term ids
     */
    public Collection<String> getAllGOTermIds() {

        Collection<String> goTermIds = terms.keySet();
        goTermIds.remove( BASE_GO_URI + "GO_0008150" );
        goTermIds.remove( BASE_GO_URI + "GO_0003674" );
        goTermIds.remove( BASE_GO_URI + "GO_0005575" );
        return goTermIds;
    }

    /**
     * @param queryGene1
     * @param queryGene2
     * @returns Collection<OntologyEntries>
     */
    public Collection<OntologyTerm> calculateGoTermOverlap( Gene queryGene1, Gene queryGene2 ) {

        if ( queryGene1 == null || queryGene2 == null ) return null;

        Collection<OntologyTerm> queryGeneTerms1 = getGOTerms( queryGene1 );
        Collection<OntologyTerm> queryGeneTerms2 = getGOTerms( queryGene2 );

        return computeOverlap( queryGeneTerms1, queryGeneTerms2 );
    }

    /**
     * @param masterOntos
     * @param comparisonOntos
     * @return
     */
    public Collection<OntologyTerm> computeOverlap( Collection<OntologyTerm> masterOntos,
            Collection<OntologyTerm> comparisonOntos ) {
        Collection<OntologyTerm> overlapTerms = new HashSet<OntologyTerm>( masterOntos );
        overlapTerms.retainAll( comparisonOntos );

        return overlapTerms;
    }

    /**
     * @param entry
     * @return children, NOT including part-of relations.
     */
    public Collection<OntologyTerm> getAllChildren( OntologyTerm entry ) {
        return getAllChildren( entry, false );
    }

    /**
     * @param entry
     * @param includePartOf
     * @return
     */
    public Collection<OntologyTerm> getAllChildren( OntologyTerm entry, boolean includePartOf ) {
        return getDescendants( entry, includePartOf );
    }

    /**
     * @param entries NOTE terms that are in this collection are NOT explicitly included; however, some of them may be
     *        included incidentally if they are parents of other terms in the collection.
     * @return
     */
    public Collection<OntologyTerm> getAllParents( Collection<OntologyTerm> entries ) {
        return getAllParents( entries, false );
    }

    /**
     * @param entries
     * @param includePartOf
     * @return
     */
    public Collection<OntologyTerm> getAllParents( Collection<OntologyTerm> entries, boolean includePartOf ) {
        if ( entries == null ) return null;
        Collection<OntologyTerm> result = new HashSet<OntologyTerm>();
        for ( OntologyTerm entry : entries ) {
            result.addAll( getAncestors( entry, includePartOf ) );
        }
        return result;
    }

    /**
     * Return all the parents of GO OntologyEntry, up to the root, as well as terms that this has a restriction
     * relationship with (part_of). NOTE: the term itself is NOT included; nor is the root.
     * 
     * @param entry
     * @return parents (excluding the root)
     */
    public Collection<OntologyTerm> getAllParents( OntologyTerm entry ) {
        return getAllParents( entry, true );
    }

    public Collection<OntologyTerm> getAllParents( OntologyTerm entry, boolean includePartOf ) {
        return getAncestors( entry, includePartOf );

    }

    /**
     * Returns the immediate children of the given entry
     * 
     * @param entry
     * @return children of entry, or null if there are no children (or if entry is null)
     */

    public Collection<OntologyTerm> getChildren( OntologyTerm entry ) {
        return getChildren( entry, false );

    }

    public Collection<OntologyTerm> getChildren( OntologyTerm entry, boolean includePartOf ) {
        if ( entry == null ) return null;
        if ( log.isDebugEnabled() ) log.debug( "Getting children of " + entry );
        Collection<OntologyTerm> terms = entry.getChildren( true );

        if ( includePartOf ) terms.addAll( getPartsOf( entry ) );

        return terms;
    }

    /**
     * @param goId
     * @param taxon
     * @return Collection of all genes in the given taxon that are annotated with the given id, including its child
     *         terms in the hierarchy.
     */
    @SuppressWarnings("unchecked")
    public Collection<Gene> getGenes( String goId, Taxon taxon ) {
        OntologyTerm t = getTermForId( goId );
        if ( t == null ) return null;
        Collection<OntologyTerm> terms = getAllChildren( t );
        Collection<Gene> results = new HashSet<Gene>( this.gene2GOAssociationService.findByGOTerm( goId, taxon ) );

        for ( OntologyTerm term : terms ) {
            results.addAll( this.gene2GOAssociationService.findByGOTerm( asRegularGoId( term ), taxon ) );
        }
        return results;
    }

    /**
     * @param Take a gene and return a set of all GO terms including the parents of each GO term
     * @param geneOntologyTerms
     */
    public Collection<OntologyTerm> getGOTerms( Gene gene ) {
        return getGOTerms( gene, true );
    }

    /**
     * Get all GO terms for a gene, including parents of terms via is-a relationships; and optinally also parents via
     * part-of relationships.
     * 
     * @param gene
     * @param includePartOf
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<OntologyTerm> getGOTerms( Gene gene, boolean includePartOf ) {
        Collection<OntologyTerm> cachedTerms = goTerms.get( gene );
        if ( log.isTraceEnabled() && cachedTerms != null ) {
            logIds( "found cached GO terms for " + gene.getOfficialSymbol(), goTerms.get( gene ) );
        }

        if ( cachedTerms == null ) {
            Collection<OntologyTerm> allGOTermSet = new HashSet<OntologyTerm>();

            Collection<VocabCharacteristic> annotations = gene2GOAssociationService.findByGene( gene );
            for ( VocabCharacteristic c : annotations ) {
                if ( !terms.containsKey( c.getValueUri() ) ) {
                    log.warn( "Term " + c.getValueUri() + " not found in term list cant add to results" );
                    continue;
                }
                allGOTermSet.add( terms.get( c.getValueUri() ) );
            }

            allGOTermSet.addAll( getAllParents( allGOTermSet, includePartOf ) );

            cachedTerms = Collections.unmodifiableCollection( allGOTermSet );
            if ( log.isTraceEnabled() ) logIds( "caching GO terms for " + gene.getOfficialSymbol(), allGOTermSet );
            goTerms.put( gene, cachedTerms );
        }

        return cachedTerms;
    }

    /**
     * Return the immediate parent(s) of the given entry. The root node is never returned.
     * 
     * @param entry
     * @return collection, because entries can have multiple parents. (only allroot is excluded)
     */ 
    public Collection<OntologyTerm> getParents( OntologyTerm entry ) {
        return getParents( entry, false );
    }

    /**
     * @param entry
     * @param includePartOf
     * @return the immediate parents of the given ontology term. includePartOf determins if part of relationships are
     *         included in the returned information
     */
    public Collection<OntologyTerm> getParents( OntologyTerm entry, boolean includePartOf ) {
        Collection<OntologyTerm> parents = entry.getParents( true );
        Collection<OntologyTerm> results = new HashSet<OntologyTerm>();
        for ( OntologyTerm term : parents ) {
            // The isRoot() returns true for the MolecularFunction, BiologicalProcess, CellularComponent
            // if ( term.isRoot() ) continue;
            if ( term.getUri().equalsIgnoreCase( ALL_ROOT ) ) continue;

            if ( term instanceof OntologyClassRestriction ) {
                // log.info( "Skipping " + term );
                // OntologyProperty restrictionOn = ( ( OntologyClassRestriction ) term ).getRestrictionOn();
                // if ( restrictionOn.getLabel().equals( "part_of" ) ) {
                // OntologyTerm restrictedTo = ( ( OntologyClassRestriction ) term ).getRestrictedTo();
                // results.add( restrictedTo );
                // }
            } else {
                // log.info( "Adding " + term );
                results.add( term );
            }
        }

        if ( includePartOf ) results.addAll( getIsPartOf( entry ) );

        return results;
    }

    /**
     * Return a definition for a GO Id.
     * 
     * @param goId e.g. GO:0094491
     * @return Definition or null if there is no definition.
     */
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

    /**
     * Return human-readable term ("protein kinase") for a GO Id.
     * 
     * @param goId
     * @return
     */
    public String getTermName( String goId ) {
        OntologyTerm t = getTermForId( goId );
        if ( t == null ) return "[Not available]"; // not ready yet?
        return t.getTerm();
    }

    /**
     * Determines if one ontology entry is a child (direct or otherwise) of a given parent term.
     * 
     * @param parent
     * @param potentialChild
     * @return
     */
    public Boolean isAChildOf( OntologyTerm parent, OntologyTerm potentialChild ) {
        return isAParentOf( potentialChild, parent );
    }

    /**
     * Determines if one ontology entry is a parent (direct or otherwise) of a given child term.
     * 
     * @param child
     * @param potentialParent
     * @return True if potentialParent is in the parent graph of the child; false otherwise.
     */
    public Boolean isAParentOf( OntologyTerm child, OntologyTerm potentialParent ) {
        if ( potentialParent.isRoot() ) return true; // well....
        Collection<OntologyTerm> parents = getAllParents( child );
        return parents.contains( potentialParent );
    }

    /**
     * @param term
     * @return
     */
    public boolean isBiologicalProcess( OntologyTerm term ) {

        String nameSpace = getTermAspect( term );
        if ( nameSpace == null ) {
            log.warn( "No namespace for " + term + ", assuming not Biological Process" );
            return false;
        }

        return nameSpace.equals( "biological_process" );
    }

    /**
     * Used for determining if the Gene Ontology has finished loading into memory yet Although calls like getParents,
     * getChildren will still work (its much faster once the gene ontologies have been preloaded into memory.
     * 
     * @returns boolean
     */
    public synchronized boolean isGeneOntologyLoaded() {

        return ready.get();
    }

    public boolean isReady() {
        return ready.get();
    }

    public boolean isRunning() {
        return running.get();
    }

    public Collection<OntologyTerm> listTerms() {
        return terms.values();
    }

    /**
     * @param gene2GOAssociationService the gene2GOAssociationService to set
     */
    public void setGene2GOAssociationService( Gene2GOAssociationService gene2GOAssociationService ) {
        this.gene2GOAssociationService = gene2GOAssociationService;
    }

    /**
     * @param geneService the geneService to set
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * 
     */
    protected synchronized void forceLoadOntology() {
        initilizeGoOntology();
    }

    /**
     * 
     */
    public synchronized void init( boolean force ) {

        if ( running.get() ) {
            log.warn( "Gene Ontology initialization is already running" );
            return;
        }

        boolean loadOntology = ConfigUtils.getBoolean( LOAD_GENE_ONTOLOGY_OPTION, LOAD_BY_DEFAULT );

        if ( !force && !loadOntology ) {
            log.info( "Loading Gene Ontology is disabled (force=" + force + ", " + LOAD_GENE_ONTOLOGY_OPTION + "="
                    + loadOntology + ")" );
            enabled = false;
            return;
        }

        initilizeGoOntology();
    }

    /**
     * Primarily here for testing.
     * 
     * @param is
     * @throws IOException
     */
    protected void loadTermsInNameSpace( InputStream is ) {
        Collection<OntologyResource> terms = OntologyLoader.initialize( null, OntologyLoader.loadMemoryModel( is, null,
                OntModelSpec.OWL_MEM ) );
        addTerms( terms );
    }

    /**
     * @param url
     * @throws IOException
     */
    protected void loadTermsInNameSpace( String url ) {
        Collection<OntologyResource> terms = OntologyLoader.initialize( url, OntologyLoader.loadMemoryModel( url,
                OntModelSpec.OWL_MEM ) );
        addTerms( terms );
    }

    private void addTerms( Collection<OntologyResource> newTerms ) {
        if ( terms == null ) terms = new HashMap<String, OntologyTerm>();
        for ( OntologyResource term : newTerms ) {
            if ( term.getUri() == null ) continue;
            if ( term instanceof OntologyTerm ) {
                OntologyTerm ontTerm = ( OntologyTerm ) term;
                terms.put( term.getUri(), ontTerm );
                for ( String alternativeID : ontTerm.getAlternativeIds() ) {
                    log.debug( toUri( alternativeID ) );
                    terms.put( toUri( alternativeID ), ontTerm );
                }
            }
        }
    }

    private synchronized Collection<OntologyTerm> getAncestors( OntologyTerm entry, boolean includePartOf ) {

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
     * @param entry
     * @return Given an ontology term recursivly determines all the children and adds them to a cache (same as
     *         getAllParents but the recusive code is a little cleaner and doesn't use and accumulator)
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

    /**
     * Return terms to which the given term has a part_of relation (it is "part_of" them).
     * 
     * @param entry
     * @return
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
                if ( x.isAnon() ) continue; // some reasoners will return these.
                String uri = x.getURI();
                if ( log.isDebugEnabled() ) log.debug( entry + " is part of " + terms.get( uri ) );
                r.add( terms.get( uri ) );
            }
        } finally {
            qexec.close();
        }
        return r;
    }

    /**
     * Return terms which have "part_of" relation with the given term (they are "part_of" the given term).
     * 
     * @param entry
     * @return
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
                if ( x.isAnon() ) continue; // some reasoners will return these.
                if ( log.isDebugEnabled() ) log.debug( terms.get( uri ) + " is part of " + entry );
                r.add( terms.get( uri ) );
            }
        } finally {
            qexec.close();
        }
        return r;
    }

    private synchronized void initilizeGoOntology() {

        Thread loadThread = new Thread( new Runnable() {
            public void run() {
                running.set( true );
                terms = new HashMap<String, OntologyTerm>();
                log.info( "Loading Gene Ontology..." );
                StopWatch loadTime = new StopWatch();
                loadTime.start();
                //
                try {
                    loadTermsInNameSpace( MF_URL );
                    log.info( "Gene Ontology Molecular Function loaded, total of " + terms.size() + " items in "
                            + loadTime.getTime() / 1000 + "s" );

                    loadTermsInNameSpace( BP_URL );
                    log.info( "Gene Ontology Biological Process loaded, total of " + terms.size() + " items in "
                            + loadTime.getTime() / 1000 + "s" );

                    loadTermsInNameSpace( CC_URL );
                    log.info( "Gene Ontology Cellular Component loaded, total of " + terms.size() + " items in "
                            + loadTime.getTime() / 1000 + "s" );

                    ready.set( true );
                    running.set( false );

                    log.info( "Done loading GO" );
                    loadTime.stop();
                } catch ( Throwable e ) {
                    log.error( e, e );
                    ready.set( false );
                    running.set( false );
                }
            }

        } );

        synchronized ( running ) {
            if ( running.get() ) return;
            loadThread.start();
        }

    }

    private void logIds( String prefix, Collection<OntologyTerm> terms ) {
        StringBuffer buf = new StringBuffer( prefix );
        buf.append( ": [ " );
        Iterator<OntologyTerm> i = terms.iterator();
        while ( i.hasNext() ) {
            buf.append( GeneOntologyService.asRegularGoId( i.next() ) );
            if ( i.hasNext() ) buf.append( ", " );
        }
        buf.append( " ]" );
        log.trace( buf.toString() );
    }

}