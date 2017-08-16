/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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
package ubic.gemma.core.ontology.providers;

import org.springframework.beans.factory.InitializingBean;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneOntologyTermValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.core.ontology.providers.GeneOntologyServiceImpl.GOAspect;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

/**
 * @author paul
 */
public interface GeneOntologyService extends InitializingBean {

    String BASE_GO_URI = "http://purl.obolibrary.org/obo/";

    /**
     * <p>
     * Given a query Gene, and a collection of gene ids calculates the go term overlap for each pair of queryGene and
     * gene in the given collection. Returns a Map<Gene,Collection<OntologyEntries>>. The key is the gene (from the
     * [queryGene,gene] pair) and the values are a collection of the overlapping ontology entries.
     * </p>
     *
     * @return map of gene ids to collections of ontologyTerms. This will always be populated but collection values
     * will be empty when there is no overlap.
     */
    Map<Long, Collection<OntologyTerm>> calculateGoTermOverlap( Gene queryGene, Collection<Long> geneIds );

    /**
     * @return Collection<OntologyEntries>
     */
    Collection<OntologyTerm> calculateGoTermOverlap( Gene queryGene1, Gene queryGene2 );

    Map<Long, Collection<OntologyTerm>> calculateGoTermOverlap( Long queryGene, Collection<Long> geneIds );

    Collection<OntologyTerm> computeOverlap( Collection<OntologyTerm> masterOntos,
            Collection<OntologyTerm> comparisonOntos );

    /**
     * Search by inexact string
     */
    Collection<OntologyTerm> findTerm( String queryString );

    /**
     * @return children, NOT including part-of relations.
     */
    Collection<OntologyTerm> getAllChildren( OntologyTerm entry );

    Collection<OntologyTerm> getAllChildren( OntologyTerm entry, boolean includePartOf );

    /**
     * @return a collection of all existing GO term ids (GO_XXXXXXX) -- including the roots of the ontologies
     * ('biological process' etc.)
     */
    Collection<String> getAllGOTermIds();

    /**
     * @param entries NOTE terms that are in this collection are NOT explicitly included; however, some of them may be
     *                included incidentally if they are parents of other terms in the collection.
     */
    Collection<OntologyTerm> getAllParents( Collection<OntologyTerm> entries );

    Collection<OntologyTerm> getAllParents( Collection<OntologyTerm> entries, boolean includePartOf );

    /**
     * Return all the parents of GO OntologyEntry, up to the root, as well as terms that this has a restriction
     * relationship with (part_of). NOTE: the term itself is NOT included; nor is the root.
     *
     * @return parents (excluding the root)
     */
    Collection<OntologyTerm> getAllParents( OntologyTerm entry );

    Collection<OntologyTerm> getAllParents( OntologyTerm entry, boolean includePartOf );

    /**
     * Returns the immediate children of the given entry
     *
     * @return children of entry, or null if there are no children (or if entry is null)
     */

    Collection<OntologyTerm> getChildren( OntologyTerm entry );

    Collection<OntologyTerm> getChildren( OntologyTerm entry, boolean includePartOf );

    /**
     * @return Collection of all genes in the given taxon that are annotated with the given id, including its child
     * terms in the hierarchy.
     */
    Collection<Gene> getGenes( String goId, Taxon taxon );

    /**
     * @param gene Take a gene and return a set of all GO terms including the parents of each GO term
     */
    Collection<OntologyTerm> getGOTerms( Gene gene );

    /**
     * Get all GO terms for a gene, including parents of terms via is-a relationships; and optionally also parents via
     * part-of relationships.
     */
    Collection<OntologyTerm> getGOTerms( Gene gene, boolean includePartOf );

    /**
     * Get all GO terms for a gene, including parents of terms via is-a relationships; and optionally also parents via
     * part-of relationships.
     *
     * @param goAspect limit only to the given aspect (pass null to use all)
     */
    Collection<OntologyTerm> getGOTerms( Gene gene, boolean includePartOf, GOAspect goAspect );

    Collection<OntologyTerm> getGOTerms( Long geneId );

    /**
     * Return the immediate parent(s) of the given entry. The root node is never returned.
     *
     * @return collection, because entries can have multiple parents. (only allroot is excluded)
     */
    Collection<OntologyTerm> getParents( OntologyTerm entry );

    /**
     * @return the immediate parents of the given ontology term. includePartOf determins if part of relationships are
     * included in the returned information
     */
    Collection<OntologyTerm> getParents( OntologyTerm entry, boolean includePartOf );

    GOAspect getTermAspect( String goId );

    GOAspect getTermAspect( VocabCharacteristic goId );

    /**
     * Return a definition for a GO Id.
     *
     * @param goId e.g. GO:0094491
     * @return Definition or null if there is no definition.
     */
    String getTermDefinition( String goId );

    /**
     * Return human-readable term ("protein kinase") for a GO Id.
     */
    String getTermName( String goId );

    void init( boolean force );

    /**
     * Determines if one ontology entry is a child (direct or otherwise) of a given parent term.
     */
    Boolean isAChildOf( OntologyTerm parent, OntologyTerm potentialChild );

    /**
     * Determines if one ontology entry is a parent (direct or otherwise) of a given child term.
     *
     * @return True if potentialParent is in the parent graph of the child; false otherwise.
     */
    Boolean isAParentOf( OntologyTerm child, OntologyTerm potentialParent );

    /**
     * @param goId e.g. GO:0000244 or as GO_0000244
     */
    Boolean isAValidGOId( String goId );

    boolean isBiologicalProcess( OntologyTerm term );

    /**
     * Used for determining if the Gene Ontology has finished loading into memory yet Although calls like getParents,
     * getChildren will still work (its much faster once the gene ontologies have been preloaded into memory.
     */
    boolean isGeneOntologyLoaded();

    boolean isReady();

    boolean isRunning();

    Collection<OntologyTerm> listTerms();

    /**
     * Returns GO Terms VOs for the given Gene.
     * @param gene the Gene to retrieve GO Terms for and convert them to VOs.
     * @return Gene Ontology VOs representing all GO Terms associated with the given gene.
     */
    Collection<GeneOntologyTermValueObject> getValueObjects(Gene gene);

    /**
     * Converts the given collection of Ontology Terms to Gene Ontology Value Objects.
     * @param terms the terms to be converted.
     * @return collection of value objects representing the given terms.
     */
    Collection<GeneOntologyTermValueObject> getValueObjects(Collection<OntologyTerm> terms);

    /**
     * Converts the given  Ontology Term to a Gene Ontology Value Object.
     * @param term the term to be converted.
     * @return value object representing the given term.
     */
    GeneOntologyTermValueObject getValueObject(OntologyTerm term);

    /**
     * Primarily here for testing.
     */
    void loadTermsInNameSpace( InputStream is );

    /**
     * Primarily here for testing, to recover memory.
     */
    void shutDown();

}