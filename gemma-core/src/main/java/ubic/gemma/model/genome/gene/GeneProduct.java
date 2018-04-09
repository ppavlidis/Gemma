package ubic.gemma.model.genome.gene;
/*
* The Gemma project.
* 
* Copyright (c) 2006-2018 University of British Columbia
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

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.ChromosomeFeature;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;

import java.util.Collection;

public abstract class GeneProduct extends ChromosomeFeature {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 6559927399916235369L;
    private GeneProductType type;
    private String ncbiGi;
    private Collection<DatabaseEntry> accessions = new java.util.HashSet<>();

    /**
     * Only used in transient instances in sequence analysis. The entity relation in the database is never used and will
     * be removed.
     */
    private Collection<PhysicalLocation> exons = new java.util.HashSet<>();

    private Gene gene;

    public Collection<DatabaseEntry> getAccessions() {
        return this.accessions;
    }

    public void setAccessions( Collection<DatabaseEntry> accessions ) {
        this.accessions = accessions;
    }

    /**
     * Only used for transient instances in sequence analysis, we do not store exon locations in the database.
     * 
     * @return
     */
    public Collection<PhysicalLocation> getExons() {
        return this.exons;
    }

    /**
     * Only used for transient instances, we do not store exon locations in the database.
     * 
     * @param exons
     */
    public void setExons( Collection<PhysicalLocation> exons ) {
        this.exons = exons;
    }

    public Gene getGene() {
        return this.gene;
    }

    public void setGene( Gene gene ) {
        this.gene = gene;
    }

    /**
     * @return GI for the gene product (if available)
     */
    public String getNcbiGi() {
        return this.ncbiGi;
    }

    public void setNcbiGi( String ncbiGi ) {
        this.ncbiGi = ncbiGi;
    }

    public GeneProductType getType() {
        return this.type;
    }

    public void setType( GeneProductType type ) {
        this.type = type;
    }

    public static final class Factory {
        public static GeneProduct newInstance() {
            return new GeneProductImpl();
        }

    }

}