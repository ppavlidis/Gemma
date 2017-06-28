/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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

package ubic.gemma.core.search;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.ontology.model.OntologyResource;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.core.genome.gene.GOGroupValueObject;
import ubic.gemma.core.genome.gene.GeneSetValueObjectHelper;
import ubic.gemma.core.genome.gene.service.GeneSetService;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.ontology.providers.GeneOntologyServiceImpl;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetMember;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.persistence.service.association.Gene2GOAssociationService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author paul
 */
@Component
public class GeneSetSearchImpl implements GeneSetSearch {

    /**
     * Also defined in GeneSearchServiceImpl.
     */
    private static final int MAX_GO_GROUP_SIZE = 200;
    private static final Log log = LogFactory.getLog( GeneSetSearchImpl.class );

    @Autowired
    private Gene2GOAssociationService gene2GoService;
    @Autowired
    private GeneOntologyService geneOntologyService;
    @Autowired
    private GeneSetService geneSetService;
    @Autowired
    private GeneSetValueObjectHelper geneSetValueObjectHelper;
    @Autowired
    private PhenotypeAssociationManagerService phenotypeAssociationManagerService;
    @Autowired
    private TaxonService taxonService;

    @Override
    public Collection<GeneSet> findByGene( Gene gene ) {
        return geneSetService.findByGene( gene );
    }

    @Override
    public GeneSet findByGoId( String goId, Taxon taxon ) {
        OntologyTerm goTerm = GeneOntologyServiceImpl.getTermForId( StringUtils.strip( goId ) );

        if ( goTerm == null ) {
            return null;
        }
        // if taxon is null, this returns a geneset with genes from different taxons
        return goTermToGeneSet( goTerm, taxon );
    }

    @Override
    public Collection<GeneSet> findByGoTermName( String goTermName, Taxon taxon ) {
        return findByGoTermName( goTermName, taxon, null, null );
    }

    @Override
    public Collection<GeneSet> findByGoTermName( String goTermName, Taxon taxon, Integer maxGoTermsProcessed,
            Integer maxGeneSetSize ) {
        Collection<? extends OntologyResource> matches = this.geneOntologyService
                .findTerm( StringUtils.strip( goTermName ) );

        Collection<GeneSet> results = new HashSet<GeneSet>();

        for ( OntologyResource t : matches ) {
            assert t instanceof OntologyTerm;

            if ( taxon == null ) {
                Collection<GeneSet> sets = goTermToGeneSets( ( OntologyTerm ) t, maxGeneSetSize );
                results.addAll( sets );

                // should we count each species as one go
                if ( maxGoTermsProcessed != null && results.size() > maxGoTermsProcessed ) {
                    // return results;
                }
            } else {

                GeneSet converted = goTermToGeneSet( t, taxon, maxGeneSetSize );
                // converted will be null if its size is more than maxGeneSetSize
                if ( converted != null ) {
                    results.add( converted );

                }
            }

            if ( maxGoTermsProcessed != null && results.size() > maxGoTermsProcessed ) {
                return results;
            }
        }

        return results;

    }

    @Override
    public Collection<GeneSet> findByName( String name ) {
        return geneSetService.findByName( StringUtils.strip( name ) );
    }

    @Override
    public Collection<GeneSet> findByName( String name, Taxon taxon ) {
        return geneSetService.findByName( StringUtils.strip( name ), taxon );
    }

    @Override
    public Collection<GeneSetValueObject> findByPhenotypeName( String phenotypeQuery, Taxon taxon ) {

        StopWatch timer = new StopWatch();
        timer.start();
        Collection<CharacteristicValueObject> phenotypes = phenotypeAssociationManagerService
                .searchOntologyForPhenotypes( StringUtils.strip( phenotypeQuery ), null );

        Collection<GeneSetValueObject> results = new HashSet<GeneSetValueObject>();

        if ( phenotypes.isEmpty() ) {
            return results;
        }

        if ( timer.getTime() > 200 ) {
            log.info( "Find phenotypes: " + timer.getTime() + "ms" );
        }

        log.debug( " Converting CharacteristicValueObjects collection(size:" + phenotypes.size()
                + ") into GeneSets for  phenotype query " + phenotypeQuery );
        Map<String, CharacteristicValueObject> uris = new HashMap<String, CharacteristicValueObject>();
        for ( CharacteristicValueObject cvo : phenotypes ) {
            uris.put( cvo.getValueUri(), cvo );
        }

        Map<String, Collection<? extends GeneValueObject>> genes = phenotypeAssociationManagerService
                .findCandidateGenesForEach( uris.keySet(), taxon );

        if ( timer.getTime() > 500 ) {
            log.info( "Find phenotype genes done at " + timer.getTime() + "ms" );
        }

        for ( String uri : genes.keySet() ) {

            Collection<? extends GeneValueObject> gvos = genes.get( uri );

            if ( gvos.isEmpty() )
                continue;

            Collection<Long> geneIds = EntityUtils.getIds( gvos );

            GeneSetValueObject transientGeneSet = new GeneSetValueObject();

            transientGeneSet.setName( uri2phenoID( uris.get( uri ) ) );
            transientGeneSet.setDescription( uris.get( uri ).getValue() );
            transientGeneSet.setGeneIds( geneIds );

            transientGeneSet.setTaxonId( gvos.iterator().next().getTaxonId() );
            transientGeneSet.setTaxonName( gvos.iterator().next().getTaxonCommonName() );

            results.add( transientGeneSet );

        }

        if ( timer.getTime() > 1000 ) {
            log.info(
                    "Loaded " + phenotypes.size() + " phenotype gene sets for query " + phenotypeQuery + " in " + timer
                            .getTime() + "ms" );
        }
        return results;

    }

    @Override
    public Collection<GeneSet> findGeneSetsByName( String query, Long taxonId ) {

        if ( StringUtils.isBlank( query ) ) {
            return new HashSet<GeneSet>();
        }
        Collection<GeneSet> foundGeneSets;
        Taxon tax;
        tax = taxonService.load( taxonId );

        if ( tax == null ) {
            // throw new IllegalArgumentException( "Can't locate taxon with id=" + taxonId );
            foundGeneSets = findByName( query );
        } else {
            foundGeneSets = findByName( query, tax );
        }

        foundGeneSets.clear(); // for testing general search

        /*
         * SEARCH GENE ONTOLOGY
         */

        if ( query.toUpperCase().startsWith( "GO" ) ) {
            if ( tax == null ) {
                Collection<GeneSet> goSets = findByGoId( query );
                foundGeneSets.addAll( goSets );
            } else {
                GeneSet goSet = findByGoId( query, tax );
                if ( goSet != null )
                    foundGeneSets.add( goSet );
            }
        } else {
            foundGeneSets.addAll( findByGoTermName( query, tax ) );
        }

        return foundGeneSets;
    }

    @Override
    public GOGroupValueObject findGeneSetValueObjectByGoId( String goId, Long taxonId ) {

        // shouldn't need to set the taxon here, should be taken care of when creating the value object
        Taxon taxon;

        if ( taxonId != null ) {
            taxon = taxonService.load( taxonId );
            if ( taxon == null ) {
                log.warn( "No such taxon with id=" + taxonId );
            } else {
                GeneSet result = findByGoId( goId, taxonService.load( taxonId ) );
                if ( result == null ) {
                    log.warn( "No matching gene set found for: " + goId );
                    return null;
                }
                GOGroupValueObject ggvo = geneSetValueObjectHelper.convertToGOValueObject( result, goId, goId );

                ggvo.setTaxonId( taxon.getId() );
                ggvo.setTaxonName( taxon.getCommonName() );

                return ggvo;
            }
        }
        return null;
    }

    private Collection<GeneSet> findByGoId( String query ) {
        OntologyTerm goTerm = GeneOntologyServiceImpl.getTermForId( StringUtils.strip( query ) );

        if ( goTerm == null ) {
            return new HashSet<GeneSet>();
        }
        // if taxon is null, this returns genesets for all taxa
        return goTermToGeneSets( goTerm, MAX_GO_GROUP_SIZE );
    }

    private GeneSet goTermToGeneSet( OntologyResource term, Taxon taxon ) {
        return goTermToGeneSet( term, taxon, null );
    }

    /**
     * Convert a GO term to a 'GeneSet', including genes from all child terms. Divide up by taxon.
     */
    private GeneSet goTermToGeneSet( OntologyResource term, Taxon taxon, Integer maxGeneSetSize ) {
        assert taxon != null;
        if ( term == null )
            return null;
        if ( term.getUri() == null )
            return null;

        Collection<OntologyResource> allMatches = new HashSet<OntologyResource>();
        allMatches.add( term );
        assert term instanceof OntologyTerm;
        allMatches.addAll( this.geneOntologyService.getAllChildren( ( OntologyTerm ) term ) );
        log.info( term );
        /*
         * Gather up uris
         */
        Collection<String> termsToFetch = new HashSet<String>();
        for ( OntologyResource t : allMatches ) {
            String goId = uri2goid( t );
            termsToFetch.add( goId );
        }

        Collection<Gene> genes = this.gene2GoService.findByGOTerms( termsToFetch, taxon );

        if ( genes.isEmpty() || ( maxGeneSetSize != null && genes.size() > maxGeneSetSize ) ) {
            return null;
        }

        GeneSet transientGeneSet = GeneSet.Factory.newInstance();
        transientGeneSet.setName( uri2goid( term ) );

        if ( term.getLabel() == null ) {
            log.warn( " Label for term " + term.getUri() + " was null" );
        }
        if ( term.getLabel() != null && term.getLabel().toUpperCase().startsWith( "GO_" ) ) {
            // hm, this is an individual or a 'resource', not a 'class', but it's a real GO term. How to get the text.
        }

        transientGeneSet.setDescription( term.getLabel() );

        for ( Gene gene : genes ) {
            GeneSetMember gmember = GeneSetMember.Factory.newInstance();
            gmember.setGene( gene );
            transientGeneSet.getMembers().add( gmember );
        }
        return transientGeneSet;
    }

    private Collection<GeneSet> goTermToGeneSets( OntologyTerm term, Integer maxGeneSetSize ) {
        if ( term == null )
            return null;
        if ( term.getUri() == null )
            return null;

        Collection<OntologyResource> allMatches = new HashSet<OntologyResource>();
        allMatches.add( term );
        allMatches.addAll( this.geneOntologyService.getAllChildren( term ) );
        log.info( term );
        /*
         * Gather up uris
         */
        Collection<String> termsToFetch = new HashSet<String>();
        for ( OntologyResource t : allMatches ) {
            String goId = uri2goid( t );
            termsToFetch.add( goId );
        }

        Map<Taxon, Collection<Gene>> genesByTaxon = this.gene2GoService.findByGOTermsPerTaxon( termsToFetch );

        Collection<GeneSet> results = new HashSet<GeneSet>();
        for ( Taxon t : genesByTaxon.keySet() ) {
            Collection<Gene> genes = genesByTaxon.get( t );

            if ( genes.isEmpty() || ( maxGeneSetSize != null && genes.size() > maxGeneSetSize ) ) {
                continue;
            }

            GeneSet transientGeneSet = GeneSet.Factory.newInstance();
            transientGeneSet.setName( uri2goid( term ) );
            transientGeneSet.setDescription( term.getLabel() );

            for ( Gene gene : genes ) {
                GeneSetMember gmember = GeneSetMember.Factory.newInstance();
                gmember.setGene( gene );
                transientGeneSet.getMembers().add( gmember );
            }
            results.add( transientGeneSet );
        }
        return results;
    }

    private String uri2goid( OntologyResource t ) {
        return t.getUri().replaceFirst( ".*/", "" );
    }

    private String uri2phenoID( CharacteristicValueObject t ) {
        return t.getValueUri().replaceFirst( ".*/", "" );
    }

}
