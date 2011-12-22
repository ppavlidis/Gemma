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
package ubic.gemma.association.phenotype;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.DiseaseOntologyService;
import ubic.basecode.ontology.providers.HumanPhenotypeOntologyService;
import ubic.basecode.ontology.providers.MammalianPhenotypeOntologyService;
import ubic.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.model.association.phenotype.ExternalDatabaseEvidence;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.service.PhenotypeAssociationService;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceService;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicService;
import ubic.gemma.model.common.description.DatabaseEntryDao;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.common.description.VocabCharacteristicImpl;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.gene.phenotype.valueObject.BibliographicPhenotypesValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.TreeCharacteristicValueObject;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;

/** High Level Service used to add Candidate Gene Management System capabilities */
@Component
public class PhenotypeAssociationManagerServiceImpl implements PhenotypeAssociationManagerService, InitializingBean {

    @Autowired
    private PhenotypeAssociationService associationService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private PhenotypeAssoManagerServiceHelper phenotypeAssoManagerServiceHelper;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private CharacteristicService characteristicService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private BibliographicReferenceService bibliographicReferenceService;
    
    @Autowired
    private DatabaseEntryDao databaseEntryDao;

    private DiseaseOntologyService diseaseOntologyService = null;
    private MammalianPhenotypeOntologyService mammalianPhenotypeOntologyService = null;
    private HumanPhenotypeOntologyService humanPhenotypeOntologyService = null;
    private PubMedXMLFetcher pubMedXmlFetcher = null;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.diseaseOntologyService = this.ontologyService.getDiseaseOntologyService();
        this.mammalianPhenotypeOntologyService = this.ontologyService.getMammalianPhenotypeOntologyService();
        this.humanPhenotypeOntologyService = this.ontologyService.getHumanPhenotypeOntologyService();
        this.pubMedXmlFetcher = new PubMedXMLFetcher();
    }

    /**
     * Links an Evidence to a Gene
     * 
     * @param geneNCBI The Gene NCBI we want to add the evidence
     * @param evidence The evidence
     * @return The Gene updated with the new evidence and phenotypes
     */
    @Override
    public GeneEvidenceValueObject create( String geneNCBI, EvidenceValueObject evidence ) {

        if ( evidence.getPhenotypes().size() < 1 ) {
            throw new IllegalArgumentException();
        }

        Gene gene = this.geneService.findByNCBIId( new Integer( geneNCBI ) );

        Collection<EvidenceValueObject> evidenceValueObjects = EvidenceValueObject.convert2ValueObjects( gene
                .getPhenotypeAssociations() );

        // verify that the evidence is not a duplicate
        for ( EvidenceValueObject evidenceFound : evidenceValueObjects ) {
            if ( evidenceFound.equals( evidence ) ) {
                // the evidence already exists, no need to create it again
                return null;
            }
        }

        PhenotypeAssociation phenotypeAssociation = this.phenotypeAssoManagerServiceHelper
                .valueObject2Entity( evidence );
        phenotypeAssociation.setGene( gene );
        phenotypeAssociation = this.associationService.create( phenotypeAssociation );
        gene.getPhenotypeAssociations().add( phenotypeAssociation );

        // if the trees are present in the cache, change the tree with the corresponding phenotypes
        if ( this.cacheManager.cacheExists( PhenotypeAssociationConstants.PHENOTYPES_COUNT_CACHE ) ) {
            buildTree( evidence.getPhenotypes() );
        }

        return new GeneEvidenceValueObject( gene );
    }

    /**
     * Return all evidence for a specific gene NCBI
     * 
     * @param geneNCBI The Evidence id
     * @return The Gene we are interested in
     */
    @Override
    public Collection<EvidenceValueObject> findEvidenceByGeneNCBI( String geneNCBI ) {

        Gene gene = this.geneService.findByNCBIId( new Integer( geneNCBI ) );

        if ( gene == null ) {
            return new HashSet<EvidenceValueObject>();
        }
        return EvidenceValueObject.convert2ValueObjects( gene.getPhenotypeAssociations() );
    }

    /**
     * Return all evidence for a specific gene id
     * 
     * @param geneId The Evidence id
     * @return The Gene we are interested in
     */
    @Override
    public Collection<EvidenceValueObject> findEvidenceByGeneId( Long geneId ) {

        Gene gene = this.geneService.load( ( geneId.longValue() ) );

        if ( gene == null ) {
            return new HashSet<EvidenceValueObject>();
        }
        return EvidenceValueObject.convert2ValueObjects( gene.getPhenotypeAssociations() );
    }

    /**
     * Given an set of phenotypes returns the genes that have all those phenotypes or children phenotypes
     * 
     * @param phenotypesValuesUri the roots phenotype of the query
     * @return A collection of the genes found
     */
    @Override
    public Collection<GeneEvidenceValueObject> findCandidateGenes( Set<String> phenotypesValuesUri ) {

        if ( phenotypesValuesUri == null || phenotypesValuesUri.size() == 0 ) {
            throw new IllegalArgumentException();
        }

        // map query phenotypes given to the set of possible children phenotypes in the database + query phenotype
        HashMap<String, Set<String>> phenotypesWithChildren = findChildrenForEachPhenotypes( phenotypesValuesUri );

        Set<String> possibleChildrenPhenotypes = new HashSet<String>();

        for ( String key : phenotypesWithChildren.keySet() ) {
            possibleChildrenPhenotypes.addAll( phenotypesWithChildren.get( key ) );
        }

        String firstPhenotypesValuesUri = "";

        for ( String phenotypeValueUri : phenotypesValuesUri ) {
            firstPhenotypesValuesUri = phenotypeValueUri;
            break;
        }

        // find all Genes containing the first phenotypeValueUri
        Collection<Gene> genes = this.associationService.findPhenotypeAssociations( phenotypesWithChildren
                .get( firstPhenotypesValuesUri ) );
        phenotypesWithChildren.remove( firstPhenotypesValuesUri );

        Collection<GeneEvidenceValueObject> genesWithFirstPhenotype = GeneEvidenceValueObject
                .convert2GeneEvidenceValueObjects( genes );

        Collection<GeneEvidenceValueObject> genesVO = null;

        // only 1 phenotypeValueUri in the query, so no need to filter values received
        if ( phenotypesValuesUri.size() == 1 ) {
            genesVO = genesWithFirstPhenotype;
        }
        // we received a set of Gene with the first phenotype, we need to filter this set and keep only genes that have
        // all root phenotypes or their children
        else {
            genesVO = filterGenesWithPhenotypes( genesWithFirstPhenotype, phenotypesWithChildren );
        }

        // put some flags for the Interface indicating witch phenotypes are root for the given query or children
        flagEvidence( genesVO, phenotypesValuesUri, possibleChildrenPhenotypes );

        return genesVO;
    }

    /**
     * This method is a temporary solution, we will be using findAllPhenotypesByTree() directly in the future Get all
     * phenotypes linked to genes and count how many genes are link to each phenotype
     * 
     * @return A collection of the phenotypes with the gene occurence
     */
    @Override
    public Collection<CharacteristicValueObject> loadAllPhenotypes() {

        Collection<CharacteristicValueObject> characteristcsVO = new TreeSet<CharacteristicValueObject>();

        // load the tree
        Collection<TreeCharacteristicValueObject> treeCharacteristicValueObject = findAllPhenotypesByTree();

        // undo the tree in a simple structure
        for ( TreeCharacteristicValueObject t : treeCharacteristicValueObject ) {
            addChildren( characteristcsVO, t );
        }

        return characteristcsVO;
    }

    /**
     * Removes an evidence
     * 
     * @param id The Evidence database id
     */
    @Override
    public void remove( Long id ) {
        PhenotypeAssociation loaded = this.associationService.load( id );

        EvidenceValueObject evidenceVo = EvidenceValueObject.convert2ValueObjects( loaded );

        // if the trees are present in the cache, change the tree with the corresponding phenotypes
        if ( this.cacheManager.cacheExists( PhenotypeAssociationConstants.PHENOTYPES_COUNT_CACHE ) ) {
            buildTree( evidenceVo.getPhenotypes() );
        }

        if ( loaded != null ) {
            
            // We should also delete the databaseEntry for ExternalDatabaseEvidence
            if(loaded instanceof ExternalDatabaseEvidence){
                this.databaseEntryDao.remove(((ExternalDatabaseEvidence) loaded).getEvidenceSource().getId());
            }
            
            this.associationService.remove( loaded );
        }
    }

    /**
     * Load an evidence
     * 
     * @param id The Evidence database id
     */
    @Override
    public EvidenceValueObject load( Long id ) {
        return EvidenceValueObject.convert2ValueObjects( this.associationService.load( id ) );
    }

    /**
     * Modify an existing evidence
     * 
     * @param evidenceValueObject the evidence with modified fields
     */
    @Override
    // TODO to test and to be modified
    public void update( EvidenceValueObject evidenceValueObject ) {

        // new phenotypes found on the evidence (the difference will be what is added or removed)
        Set<String> newPhenotypesValuesUri = new HashSet<String>();

        for ( CharacteristicValueObject cha : evidenceValueObject.getPhenotypes() ) {
            newPhenotypesValuesUri.add( cha.getValueUri() );
        }

        // an evidenceValueObject always has at least 1 phenotype
        if ( newPhenotypesValuesUri.size() == 0 ) {
            throw new IllegalArgumentException();
        }

        // replace specific values for this type of evidence
        PhenotypeAssociation phenotypeAssociation = this.phenotypeAssoManagerServiceHelper
                .populateTypePheAsso( evidenceValueObject );

        // the final characteristics to update the evidence with
        Collection<Characteristic> characteristicsUpdated = new HashSet<Characteristic>();

        if ( evidenceValueObject.getDatabaseId() != null ) {

            // for each phenotypes determine if there is new or delete ones
            for ( Characteristic cha : phenotypeAssociation.getPhenotypes() ) {

                if ( cha instanceof VocabCharacteristicImpl ) {
                    String valueUri = ( ( VocabCharacteristicImpl ) cha ).getValueUri();

                    // this phenotype been deleted
                    if ( !newPhenotypesValuesUri.contains( valueUri ) ) {
                        // delete phenotype from the database
                        this.characteristicService.delete( cha.getId() );
                    }
                    // this phenotype is already on the evidence
                    else {
                        characteristicsUpdated.add( cha );
                        newPhenotypesValuesUri.remove( valueUri );
                    }
                }
            }

            // all phenotypes left in newPhenotypesValuesUri represent new phenotypes that were not there before
            for ( String valueUri : newPhenotypesValuesUri ) {
                Characteristic cha = valueUri2Characteristic( valueUri );
                characteristicsUpdated.add( cha );
            }

            // set the correct new phenotypes
            phenotypeAssociation.getPhenotypes().clear();
            phenotypeAssociation.getPhenotypes().addAll( characteristicsUpdated );

            // replace simple values common to all evidences
            this.phenotypeAssoManagerServiceHelper.populatePheAssoWithoutPhenotypes( phenotypeAssociation,
                    evidenceValueObject );

            // update changes to database
            this.associationService.update( phenotypeAssociation );
        }
    }

    /**
     * Giving a phenotype searchQuery, returns a selection choice to the user
     * 
     * @param termUsed is what the user typed
     * @param geneId the id of the gene chosen
     * @return Collection<CharacteristicValueObject> list of choices returned
     */
    @Override
    public Collection<CharacteristicValueObject> searchOntologyForPhenotypes( String searchQuery, Long geneId ) {

        ArrayList<CharacteristicValueObject> orderedPhenotypesFromOntology = new ArrayList<CharacteristicValueObject>();

        boolean geneProvided = true;

        if ( geneId == null ) {
            geneProvided = false;
        }

        // prepare the searchQuery to correctly query the Ontology
        String newSearchQuery = prepareOntologyQuery( searchQuery );

        // search the Ontology with the new search query
        Set<CharacteristicValueObject> allPhenotypesFoundInOntology = findPhenotypesInOntology( newSearchQuery );

        // All phenotypes present on the gene (if the gene was given)
        Set<CharacteristicValueObject> phenotypesOnCurrentGene = null;

        if ( geneProvided ) {
            phenotypesOnCurrentGene = findUniquePhenotpyesForGeneId( geneId );
        }

        // all phenotypes currently in the database
        Set<String> allPhenotypesInDatabase = this.associationService.loadAllPhenotypesUri();

        // rules to order the Ontology results found
        Collection<CharacteristicValueObject> phenotypesWithExactMatch = new ArrayList<CharacteristicValueObject>();
        Collection<CharacteristicValueObject> phenotypesAlreadyPresentOnGene = new ArrayList<CharacteristicValueObject>();
        Collection<CharacteristicValueObject> phenotypesStartWithQueryAndInDatabase = new ArrayList<CharacteristicValueObject>();
        Collection<CharacteristicValueObject> phenotypesStartWithQuery = new ArrayList<CharacteristicValueObject>();
        Collection<CharacteristicValueObject> phenotypesSubstringAndInDatabase = new ArrayList<CharacteristicValueObject>();
        Collection<CharacteristicValueObject> phenotypesSubstring = new ArrayList<CharacteristicValueObject>();
        Collection<CharacteristicValueObject> phenotypesNoRuleFound = new ArrayList<CharacteristicValueObject>();

        /*
         * for each CharacteristicVO found from the Ontology, filter them and add them to a specific list if they
         * satisfied the condition
         */
        for ( CharacteristicValueObject cha : allPhenotypesFoundInOntology ) {

            // set flag for UI, flag if the phenotype is on the Gene or if in the database
            if ( phenotypesOnCurrentGene != null && phenotypesOnCurrentGene.contains( cha ) ) {
                cha.setAlreadyPresentOnGene( true );
            } else if ( allPhenotypesInDatabase.contains( cha.getValueUri() ) ) {
                cha.setAlreadyPresentInDatabase( true );
            }

            // order the results by specific rules

            // Case 1, exact match
            if ( cha.getValue().equalsIgnoreCase( searchQuery ) ) {
                phenotypesWithExactMatch.add( cha );
            }
            // Case 2, phenotype already present on Gene
            else if ( phenotypesOnCurrentGene != null && phenotypesOnCurrentGene.contains( cha ) ) {
                phenotypesAlreadyPresentOnGene.add( cha );
            }
            // Case 3, starts with a substring of the word
            else if ( cha.getValue().toLowerCase().startsWith( searchQuery.toLowerCase() ) ) {
                if ( allPhenotypesInDatabase.contains( cha.getValueUri() ) ) {
                    phenotypesStartWithQueryAndInDatabase.add( cha );
                } else {
                    phenotypesStartWithQuery.add( cha );
                }
            }
            // Case 4, contains a substring of the word
            else if ( cha.getValue().toLowerCase().indexOf( searchQuery.toLowerCase() ) != -1 ) {
                if ( allPhenotypesInDatabase.contains( cha.getValueUri() ) ) {
                    phenotypesSubstringAndInDatabase.add( cha );
                } else {
                    phenotypesSubstring.add( cha );
                }
            } else {
                phenotypesNoRuleFound.add( cha );
            }
        }

        // place them in the correct order to display
        orderedPhenotypesFromOntology.addAll( phenotypesWithExactMatch );
        orderedPhenotypesFromOntology.addAll( phenotypesAlreadyPresentOnGene );
        orderedPhenotypesFromOntology.addAll( phenotypesStartWithQueryAndInDatabase );
        orderedPhenotypesFromOntology.addAll( phenotypesSubstringAndInDatabase );
        orderedPhenotypesFromOntology.addAll( phenotypesStartWithQuery );
        orderedPhenotypesFromOntology.addAll( phenotypesSubstring );
        orderedPhenotypesFromOntology.addAll( phenotypesNoRuleFound );

        // limit the size of the returned phenotypes to 100 terms
        if ( orderedPhenotypesFromOntology.size() > 100 ) {
            return orderedPhenotypesFromOntology.subList( 0, 100 );
        }

        return orderedPhenotypesFromOntology;
    }

    /**
     * Using all the phenotypes in the database, builds a tree structure using the Ontology, uses cache for fast access
     * 
     * @return Collection<TreeCharacteristicValueObject> list of all phenotypes in gemma represented as trees
     */
    @Override
    public Collection<TreeCharacteristicValueObject> findAllPhenotypesByTree() {

        // init the cache if it is not
        if ( !this.cacheManager.cacheExists( PhenotypeAssociationConstants.PHENOTYPES_COUNT_CACHE ) ) {
            this.cacheManager.addCache( new Cache( PhenotypeAssociationConstants.PHENOTYPES_COUNT_CACHE, 1500, false,
                    false, 72 * 3600, 72 * 3600 ) );
        }
        // the phenotypes in the database correspond to many trees not linked to each other, each of those tree are keep
        // in the cache separately, when a change is detected we only need to reconstruct the specific tree the
        // phenotype belong to ( the occurence count for a phenotype depends his children occurence)

        Collection<TreeCharacteristicValueObject> treesPhenotypes = buildTree( null );

        Cache phenoCountCache = this.cacheManager.getCache( PhenotypeAssociationConstants.PHENOTYPES_COUNT_CACHE );

        Collection<TreeCharacteristicValueObject> finalTree = new TreeSet<TreeCharacteristicValueObject>();

        for ( TreeCharacteristicValueObject tc : treesPhenotypes ) {

            // the branch is not in the cache ( it was removed or expired)
            if ( phenoCountCache.get( tc.getValueUri() ) == null ) {
                // count occurence for each phenotype in the branch
                countGeneOccurence( tc );
                phenoCountCache.put( new Element( tc.getValueUri(), tc ) );
                finalTree.add( tc );
            } else {
                tc = ( TreeCharacteristicValueObject ) phenoCountCache.get( tc.getValueUri() ).getObjectValue();
                finalTree.add( tc );
            }
        }

        return finalTree;
    }

    /**
     * Does a Gene search (by name or symbol) for a query and return only Genes with evidence
     * 
     * @param query
     * @param taxonId, can be null to not constrain by taxon
     * @return Collection<GeneEvidenceValueObject> list of Genes
     */
    @Override
    public Collection<GeneEvidenceValueObject> findGenesWithEvidence( String query, Long taxonId ) {

        if ( query == null || query.length() == 0 ) {
            throw new IllegalArgumentException();
        }

        // make sure it does an inexact search
        String newQuery = query + "%";

        Taxon taxon = null;
        if ( taxonId != null ) {
            taxon = this.taxonService.load( taxonId );
        }
        SearchSettings settings = SearchSettings.geneSearch( newQuery, taxon );
        List<SearchResult> geneSearchResults = this.searchService.search( settings ).get( Gene.class );

        Collection<Gene> genes = new HashSet<Gene>();
        if ( geneSearchResults == null || geneSearchResults.isEmpty() ) {
            return new HashSet<GeneEvidenceValueObject>();
        }

        for ( SearchResult sr : geneSearchResults ) {
            genes.add( ( Gene ) sr.getResultObject() );
        }

        Collection<GeneEvidenceValueObject> geneEvidenceValueObjects = GeneEvidenceValueObject
                .convert2GeneEvidenceValueObjects( genes );

        Collection<GeneEvidenceValueObject> geneValueObjectsFilter = new ArrayList<GeneEvidenceValueObject>();

        for ( GeneEvidenceValueObject gene : geneEvidenceValueObjects ) {
            if ( gene.getEvidence() != null && gene.getEvidence().size() != 0 ) {
                geneValueObjectsFilter.add( gene );
            }
        }

        return geneValueObjectsFilter;
    }

    /**
     * Find all phenotypes associated to a pubmedID
     * 
     * @param pubMedId
     * @return BibliographicReferenceValueObject
     */
    @Override
    public BibliographicReferenceValueObject findBibliographicReference( String pubMedId ) {

        Collection<ExpressionExperiment> experiments = null;

        // check if already in the database
        BibliographicReference bibliographicReference = this.bibliographicReferenceService.findByExternalId( pubMedId );

        if ( bibliographicReference == null ) {
            // find the Bibliographic on PubMed
            bibliographicReference = this.pubMedXmlFetcher.retrieveByHTTP( Integer.parseInt( pubMedId ) );

            // the pudmedId doesn't exists in PudMed
            if ( bibliographicReference == null ) {
                return null;
            }
        } else {
            experiments = this.bibliographicReferenceService.getRelatedExperiments( bibliographicReference );
        }

        BibliographicReferenceValueObject bibliographicReferenceVO = new BibliographicReferenceValueObject(
                bibliographicReference );

        Collection<PhenotypeAssociation> phenotypeAssociations = this.associationService
                .findPhenotypesForBibliographicReference( pubMedId );

        Collection<BibliographicPhenotypesValueObject> bibliographicPhenotypesValueObjects = BibliographicPhenotypesValueObject
                .phenotypeAssociations2BibliographicPhenotypesValueObjects( phenotypeAssociations );

        bibliographicReferenceVO.setBibliographicPhenotypes( bibliographicPhenotypesValueObjects );

        if ( experiments != null && experiments.size() > 0 ) {
            bibliographicReferenceVO
                    .setExperiments( ExpressionExperimentValueObject.convert2ValueObjects( experiments ) );
        }

        return bibliographicReferenceVO;
    }

    /** counts gene on a TreeCharacteristicValueObject */
    private void countGeneOccurence( TreeCharacteristicValueObject tc ) {

        tc.setOccurence( this.associationService.countGenesWithPhenotype( tc.getAllChildrenUri() ) );

        // count for each node of the tree
        for ( TreeCharacteristicValueObject tree : tc.getChildren() ) {
            countGeneOccurence( tree );
        }
    }

    /** For a valueUri return the Characteristic (represents a phenotype) */
    private Characteristic valueUri2Characteristic( String valueUri ) {

        OntologyTerm o = findPhenotypeInOntology( valueUri );

        VocabCharacteristic myPhenotype = VocabCharacteristic.Factory.newInstance();

        myPhenotype.setValueUri( o.getUri() );
        myPhenotype.setValue( o.getLabel() );
        myPhenotype.setCategory( PhenotypeAssociationConstants.PHENOTYPE );
        myPhenotype.setCategoryUri( PhenotypeAssociationConstants.PHENOTYPE_CATEGORY_URI );

        return myPhenotype;
    }

    /** For a valueUri return the OntologyTerm found */
    private OntologyTerm findPhenotypeInOntology( String valueUri ) {

        OntologyTerm ontologyTerm = this.diseaseOntologyService.getTerm( valueUri );

        if ( ontologyTerm == null ) {
            ontologyTerm = this.mammalianPhenotypeOntologyService.getTerm( valueUri );
        }
        if ( ontologyTerm == null ) {
            ontologyTerm = this.humanPhenotypeOntologyService.getTerm( valueUri );
        }
        return ontologyTerm;
    }

    /** Given a geneId finds all phenotypes for that gene */
    private Set<CharacteristicValueObject> findUniquePhenotpyesForGeneId( Long geneId ) {

        Set<CharacteristicValueObject> phenotypes = new TreeSet<CharacteristicValueObject>();

        Collection<EvidenceValueObject> evidence = findEvidenceByGeneId( geneId );

        for ( EvidenceValueObject evidenceVO : evidence ) {
            phenotypes.addAll( evidenceVO.getPhenotypes() );
        }
        return phenotypes;
    }

    /** This method is a temporary solution, we will be using findAllPhenotypesByTree() directly in the future */
    private void addChildren( Collection<CharacteristicValueObject> characteristcsVO, TreeCharacteristicValueObject t ) {

        CharacteristicValueObject cha = new CharacteristicValueObject( t.getValue().toLowerCase(), t.getCategory(),
                t.getValueUri(), t.getCategoryUri() );

        cha.setOccurence( t.getOccurence() );

        characteristcsVO.add( cha );

        for ( TreeCharacteristicValueObject tree : t.getChildren() ) {
            addChildren( characteristcsVO, tree );
        }
    }

    /** built the tree, the valueUriUpdate is used when a new phenotype is created or deleted to reset cache */
    private Collection<TreeCharacteristicValueObject> buildTree( Collection<CharacteristicValueObject> phenotypesUpdate ) {
        // represents each phenotype and childs found in the Ontology, TreeSet used to order trees
        TreeSet<TreeCharacteristicValueObject> treesPhenotypes = new TreeSet<TreeCharacteristicValueObject>();

        // all phenotypes in Gemma
        Set<CharacteristicValueObject> allPhenotypes = this.associationService.loadAllPhenotypes();

        // keep track of all phenotypes found in the trees, used to find quickly the position to add subtrees
        HashMap<String, TreeCharacteristicValueObject> phenotypeFoundInTree = new HashMap<String, TreeCharacteristicValueObject>();

        // for each phenotype in Gemma construct its subtree of children if necessary
        for ( CharacteristicValueObject c : allPhenotypes ) {

            // dont create the tree if it is already present in an other
            if ( phenotypeFoundInTree.get( c.getValueUri() ) != null ) {
                // flag the node as phenotype found in database
                phenotypeFoundInTree.get( c.getValueUri() ).setDbPhenotype( true );

            } else {

                // find the ontology term using the valueURI
                OntologyTerm ontologyTerm = findPhenotypeInOntology( c.getValueUri() );

                if ( ontologyTerm != null ) {

                    // transform an OntologyTerm and his children to a TreeCharacteristicValueObject
                    TreeCharacteristicValueObject treeCharacteristicValueObject = this.phenotypeAssoManagerServiceHelper
                            .ontology2TreeCharacteristicValueObjects( ontologyTerm, phenotypeFoundInTree,
                                    treesPhenotypes );

                    // set flag that this node represents a phenotype in the database
                    treeCharacteristicValueObject.setDbPhenotype( true );

                    // add tree to the phenotypes found in ontology
                    phenotypeFoundInTree.put( ontologyTerm.getUri(), treeCharacteristicValueObject );

                    treesPhenotypes.add( treeCharacteristicValueObject );
                }
            }
        }

        // remove all nodes in the trees found in the Ontology but not in the database
        for ( TreeCharacteristicValueObject tc : treesPhenotypes ) {
            tc.removeUnusedPhenotypes( tc.getValueUri() );
        }

        // used to update the tree in cache when there is a new phenotype or deleted phenotype
        if ( phenotypesUpdate != null && phenotypesUpdate.size() > 0 ) {
            if ( this.cacheManager.cacheExists( PhenotypeAssociationConstants.PHENOTYPES_COUNT_CACHE ) ) {

                Cache phenoCountCache = this.cacheManager
                        .getCache( PhenotypeAssociationConstants.PHENOTYPES_COUNT_CACHE );

                for ( CharacteristicValueObject phenotypeUpdate : phenotypesUpdate ) {

                    // find the new added or deleted term
                    TreeCharacteristicValueObject treeCharacteristicValueObject = phenotypeFoundInTree
                            .get( phenotypeUpdate.getValueUri() );
                    if ( treeCharacteristicValueObject != null ) {
                        // determine the root of the tree this phenotype is in
                        String rootParent = treeCharacteristicValueObject.getRootOfTree();

                        // remove the root from the cache has the gene counts changed
                        phenoCountCache.remove( rootParent );
                    }
                }
            }
        }

        return treesPhenotypes;
    }

    /** map query phenotypes given to the set of possible children phenotypes in the database */
    private HashMap<String, Set<String>> findChildrenForEachPhenotypes( Set<String> phenotypesValuesUri ) {

        // root corresponds to one value found in phenotypesValuesUri
        // root ---> root+children phenotypes
        HashMap<String, Set<String>> parentPheno = new HashMap<String, Set<String>>();

        Set<String> phenotypesUriInDatabase = this.associationService.loadAllPhenotypesUri();

        // determine all children terms for each other phenotypes
        for ( String phenoRoot : phenotypesValuesUri ) {

            OntologyTerm ontologyTermFound = findPhenotypeInOntology( phenoRoot );
            Collection<OntologyTerm> ontologyChildrenFound = ontologyTermFound.getChildren( false );

            Set<String> parentChildren = new HashSet<String>();
            parentChildren.add( phenoRoot );

            for ( OntologyTerm ot : ontologyChildrenFound ) {

                if ( phenotypesUriInDatabase.contains( ot.getUri() ) ) {
                    parentChildren.add( ot.getUri() );
                }
            }
            parentPheno.put( phenoRoot, parentChildren );
        }
        return parentPheno;
    }

    /** Filter a set of genes if who have the root phenotype or a children of a root phenotype */
    private Collection<GeneEvidenceValueObject> filterGenesWithPhenotypes(
            Collection<GeneEvidenceValueObject> geneEvidenceValueObjects,
            HashMap<String, Set<String>> phenotypesWithChildren ) {

        Collection<GeneEvidenceValueObject> genesVO = new HashSet<GeneEvidenceValueObject>();

        for ( GeneEvidenceValueObject geneVO : geneEvidenceValueObjects ) {

            // all phenotypeUri for a gene
            Set<String> allPhenotypesOnGene = geneVO.findAllPhenotpyesOnGene();

            // if the Gene has all the phenotypes
            boolean keepGene = true;

            for ( String phe : phenotypesWithChildren.keySet() ) {

                // at least 1 value must be found
                Set<String> possiblePheno = phenotypesWithChildren.get( phe );

                boolean foundSpecificPheno = false;

                for ( String pheno : possiblePheno ) {

                    if ( allPhenotypesOnGene.contains( pheno ) ) {
                        foundSpecificPheno = true;
                    }
                }

                if ( foundSpecificPheno == false ) {
                    // dont keep gene since a root phenotype + children was not found for all evidence of that gene
                    keepGene = false;
                    break;
                }
            }
            if ( keepGene ) {
                genesVO.add( geneVO );
            }
        }

        return genesVO;
    }

    /** add flag to Evidence and CharacteristicvalueObjects */
    private void flagEvidence( Collection<GeneEvidenceValueObject> genesVO, Set<String> phenotypesValuesUri,
            Set<String> possibleChildrenPhenotypes ) {

        // flag relevant evidence, root phenotypes and children phenotypes
        for ( GeneEvidenceValueObject geneVO : genesVO ) {

            for ( EvidenceValueObject evidenceVO : geneVO.getEvidence() ) {

                boolean relevantEvidence = false;

                for ( CharacteristicValueObject chaVO : evidenceVO.getPhenotypes() ) {

                    // if the phenotype is a root
                    if ( phenotypesValuesUri.contains( chaVO.getValueUri() ) ) {
                        relevantEvidence = true;
                        chaVO.setRoot( true );
                    }
                    // if the phenotype is a children of the root
                    else if ( possibleChildrenPhenotypes.contains( chaVO.getValueUri() ) ) {
                        chaVO.setChild( true );
                        relevantEvidence = true;
                    }
                }
                if ( relevantEvidence ) {
                    evidenceVO.setRelevance( new Double( 1.0 ) );
                }
            }
        }
    }

    /** change a seachQuery to make it seach in the Ontology using * and AND */
    private String prepareOntologyQuery( String searchQuery ) {
        String[] tokens = searchQuery.split( " " );
        String newSearchQuery = "";

        for ( int i = 0; i < tokens.length; i++ ) {

            newSearchQuery = newSearchQuery + tokens[i] + "* ";

            // last one
            if ( i != tokens.length - 1 ) {
                newSearchQuery = newSearchQuery + "AND ";
            }
        }
        return newSearchQuery;
    }

    /** search the disease,hp and mp ontology for a searchQuery and return an ordered set of CharacteristicVO */
    private Set<CharacteristicValueObject> findPhenotypesInOntology( String searchQuery ) {
        Set<CharacteristicValueObject> allPhenotypesFoundInOntology = new TreeSet<CharacteristicValueObject>();

        // search disease ontology
        allPhenotypesFoundInOntology.addAll( this.phenotypeAssoManagerServiceHelper.ontology2CharacteristicValueObject(
                this.diseaseOntologyService.findTerm( searchQuery ), PhenotypeAssociationConstants.DISEASE ) );

        // search mp ontology
        allPhenotypesFoundInOntology.addAll( this.phenotypeAssoManagerServiceHelper.ontology2CharacteristicValueObject(
                this.mammalianPhenotypeOntologyService.findTerm( searchQuery ),
                PhenotypeAssociationConstants.MAMMALIAN_PHENOTYPE ) );

        // search hp ontology
        allPhenotypesFoundInOntology.addAll( this.phenotypeAssoManagerServiceHelper.ontology2CharacteristicValueObject(
                this.humanPhenotypeOntologyService.findTerm( searchQuery ),
                PhenotypeAssociationConstants.HUMAN_PHENOTYPE ) );

        return allPhenotypesFoundInOntology;
    }

}
