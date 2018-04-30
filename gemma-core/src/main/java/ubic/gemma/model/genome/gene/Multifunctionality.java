/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.genome.gene;

public class Multifunctionality implements java.io.Serializable {

    private static final long serialVersionUID = 1842256912459855071L;
    private Double score;
    private Double rank;
    private Integer numGoTerms;
    private Long id;

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    @SuppressWarnings("WeakerAccess") // Required by spring
    public Multifunctionality() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    /**
     * @return The number of GO terms the gene has, after propagation, but excluding the roots
     */
    public Integer getNumGoTerms() {
        return this.numGoTerms;
    }

    public void setNumGoTerms( Integer numGoTerms ) {
        this.numGoTerms = numGoTerms;
    }

    /**
     * @return The relative rank of the gene among other genes in the taxon, based on the multifunctionality score (not the
     * number of GO terms, though that would generally give a similar result). A rank of 1 means the
     * "most multifunctional", while 0 is assigned to the least multifunctional gene.
     */
    public Double getRank() {
        return this.rank;
    }

    public void setRank( Double rank ) {
        this.rank = rank;
    }

    /**
     * @return The multifunctionality of the gene, as scored using the "optimal ranking" method of Gillis and Pavlidis (2011).
     * It is a value from 0 to 1, where 1 is the highest multifunctionality. Note that this score is not very useful by
     * itself as it really only makes sense as a relative measure among genes. Thus the rank should be used for display.
     */
    public Double getScore() {
        return this.score;
    }

    public void setScore( Double score ) {
        this.score = score;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof Multifunctionality ) ) {
            return false;
        }
        final Multifunctionality that = ( Multifunctionality ) object;
        return this.id != null && that.getId() != null && this.id.equals( that.getId() );
    }

    @Override
    public String toString() {
        return String.format( "terms=%d score=%.2f rank=%.3f", this.numGoTerms, this.score, this.rank );
    }

    public static final class Factory {
        public static Multifunctionality newInstance() {
            return new Multifunctionality();
        }
    }

}