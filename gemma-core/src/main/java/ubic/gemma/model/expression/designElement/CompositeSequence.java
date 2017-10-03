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
package ubic.gemma.model.expression.designElement;

import ubic.gemma.model.common.Describable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * A "Probe set" (Affymetrix) or a "Probe" (other types of arrays). The sequence referred to is a "target sequence"
 * (Affymetrix), oligo (oligo arrays) or cDNA clone/EST (cDNA arrays)
 */
public class CompositeSequence extends ubic.gemma.model.common.Describable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -3859507822452159349L;
    private BioSequence biologicalCharacteristic;
    private ArrayDesign arrayDesign;

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    public CompositeSequence() {
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        Describable other = ( Describable ) obj;
        if ( getId() == null ) {
            if ( other.getId() != null )
                return false;
        } else if ( !getId().equals( other.getId() ) )
            return false;
        if ( getName() == null ) {
            if ( other.getName() != null )
                return false;
        } else if ( !getName().equals( other.getName() ) )
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( getId() == null ) ? 0 : getId().hashCode() );
        result = prime * result + ( ( getName() == null ) ? 0 : getName().hashCode() );
        return result;
    }

    public ArrayDesign getArrayDesign() {
        return this.arrayDesign;
    }

    public void setArrayDesign( ArrayDesign arrayDesign ) {
        this.arrayDesign = arrayDesign;
    }

    /**
     * @return The sequence for this composite sequence.
     */
    public BioSequence getBiologicalCharacteristic() {
        return this.biologicalCharacteristic;
    }

    public void setBiologicalCharacteristic( BioSequence biologicalCharacteristic ) {
        this.biologicalCharacteristic = biologicalCharacteristic;
    }

    public static final class Factory {

        public static CompositeSequence newInstance() {
            return new CompositeSequence();
        }

    }

}