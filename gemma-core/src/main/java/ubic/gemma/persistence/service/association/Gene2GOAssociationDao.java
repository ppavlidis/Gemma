/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.persistence.service.association;

import java.util.Collection;
import java.util.Map;

import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.BaseDao;

/**
 * @see Gene2GOAssociation
 */
public interface Gene2GOAssociationDao extends BaseDao<Gene2GOAssociation> {

    /**
     * 
     */
    public Gene2GOAssociation find( Gene2GOAssociation gene2GOAssociation );

    /**
     * Returns the Gene2GoAssociations associated with the given Gene
     */
    public Collection<Gene2GOAssociation> findAssociationByGene( Gene gene );

    /**
     * 
     */
    public Collection<VocabCharacteristic> findByGene( Gene gene );

    public Map<Gene, Collection<VocabCharacteristic>> findByGenes( Collection<Gene> needToFind );

    public Collection<Gene> findByGoTerm( java.lang.String goId );

    /**
     * <p>
     * Return all genes for the given taxon that have the given GO id associated.
     * </p>
     */
    public Collection<Gene> findByGoTerm( java.lang.String goId, Taxon taxon );

    /**
     * <p>
     * Given a collection of GO Objects returns a collection of genes that have any of the given goterms
     * </p>
     */
    public Collection<Gene> findByGOTerm( Collection<String> goTerms, Taxon taxon );

    /**
     * @param termsToFetch
     * @return
     */
    public Map<Taxon, Collection<Gene>> findByGoTermsPerTaxon( Collection<String> termsToFetch );

    /**
     * 
     */
    public Gene2GOAssociation findOrCreate( Gene2GOAssociation gene2GOAssociation );

    /**
     * @param ids
     * @return
     */
    public Collection<Gene> getGenes( Collection<String> ids );

    public Collection<Gene> getGenes( Collection<String> ids, Taxon taxon );

    /**
     * @param uris
     * @return
     */
    public Map<String, Collection<Gene>> getSets( Collection<String> uris );

    /**
     * Delete all {@link Gene2GOAssociation}s
     */
    public void removeAll();

}