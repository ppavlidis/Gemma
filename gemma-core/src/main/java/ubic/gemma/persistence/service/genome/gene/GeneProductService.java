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
package ubic.gemma.persistence.service.genome.gene;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledService;

import java.util.Collection;

/**
 * @author kelsey
 */
public interface GeneProductService extends BaseVoEnabledService<GeneProduct, GeneProductValueObject> {

    GeneProduct find( GeneProduct gProduct );

    @Secured({ "GROUP_USER" })
    GeneProduct findOrCreate( GeneProduct geneProduct );

    /**
     * Returns all the genes that share the given gene product name
     */
    Collection<Gene> getGenesByName( String search );

    /**
     * Returns all the genes that share the given gene product ncbi id
     */
    Collection<Gene> getGenesByNcbiId( String search );

    @Secured({ "GROUP_ADMIN" })
    void remove( Collection<GeneProduct> toRemove );

    GeneProduct findByGi( String string );

    Collection<GeneProduct> findByName( String name, Taxon taxon );

}
