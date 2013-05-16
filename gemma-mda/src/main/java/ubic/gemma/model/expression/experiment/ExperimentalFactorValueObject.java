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
package ubic.gemma.model.expression.experiment;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;

/**
 * @author luke
 * @author keshav
 * @version $Id$ This is the
 *          "experimentalFActor" value object
 */
public class ExperimentalFactorValueObject implements java.io.Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -2615804031123874251L;

    private String category;
    private String categoryUri;

    private String description;

    private String factorValues;

    private long id;
    private String name;
    private int numValues = 0;
    private String type = "categorical"; // continuous or categorical.
    private Collection<FactorValueValueObject> values;
    private boolean isBatch = false; // if it is the batch fator

    public boolean getIsBatch() {
        return isBatch;
    }

    public void setIsBatch( boolean batch ) {
        this.isBatch = batch;
    }

    public ExperimentalFactorValueObject() {
    }

    public ExperimentalFactorValueObject( ExperimentalFactor factor ) {
        this.setId( factor.getId() );
        this.setName( factor.getName() );
        this.setDescription( factor.getDescription() );
        this.isBatch = ExperimentalDesignUtils.isBatch( factor );

        if ( factor.getCategory() != null ) this.setCategory( factor.getCategory().getCategory() );

        this.setCategoryUri( getCategoryUri( factor.getCategory() ) );

        /*
         * Note: this code copied from the ExperimentalDesignController.
         */
        Collection<FactorValueValueObject> vals = new HashSet<FactorValueValueObject>();

        if ( factor.getType() != null ) {
            this.type = factor.getType().equals( FactorType.CATEGORICAL ) ? "categorical" : "continuous";
        } else {
            // Backwards compatibility: for old entries created prior to introduction of 'type' field in
            // ExperimentalFactor entity.
            // We have to take a guess.
            if ( factor.getFactorValues().isEmpty() ) {
                this.type = "categorical";
            } else {
                // Just use first factor value to make our guess.
                if ( factor.getFactorValues().iterator().next().getMeasurement() != null ) {
                    this.type = "continuous";
                } else {
                    this.type = "categorical";
                }
            }
        }

        if ( factor.getFactorValues() == null || factor.getFactorValues().isEmpty() ) {
            return;
        }

        this.numValues = factor.getFactorValues().size();

        for ( FactorValue value : factor.getFactorValues() ) {

            // if ( value.getMeasurement() != null ) {
            // if ( this.type.equals( "categorical" ) ) {
            // throw new IllegalStateException(
            // "Violation of factor type requirement: factors with measurement must be be attached to factors of type 'continuous': "
            // + factor );
            // }
            //
            // this.type = "continuous";
            // } else {
            // if ( this.type.equals( "continuous" ) ) {
            // throw new IllegalStateException(
            // "Violation of factor type requirement: factors without measurement must be be attached to factors of type 'categorical': "
            // + factor );
            // }
            // this.type = "categorical";
            // }

            Characteristic c = value.getExperimentalFactor().getCategory();
            if ( c == null ) {
                c = Characteristic.Factory.newInstance();
                if ( value.getExperimentalFactor().getCategory() != null )
                    c.setValue( value.getExperimentalFactor().getCategory().getCategory() );
            }
            vals.add( new FactorValueValueObject( value, c ) );
        }

        this.setValues( vals );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        ExperimentalFactorValueObject other = ( ExperimentalFactorValueObject ) obj;
        if ( id != other.id ) return false;
        return true;
    }

    public String getCategory() {
        return category;
    }

    public String getCategoryUri() {
        return categoryUri;
    }

    public String getDescription() {
        return description;
    }

    public String getFactorValues() {
        return factorValues;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * @return the numValues
     */
    public int getNumValues() {
        return this.numValues;
    }

    public String getType() {
        return type;
    }

    public Collection<FactorValueValueObject> getValues() {
        return values;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( int ) ( id ^ ( id >>> 32 ) );
        return result;
    }

    public void setCategory( String category ) {
        this.category = category;
    }

    public void setCategoryUri( String categoryUri ) {
        this.categoryUri = categoryUri;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    /**
     * Set a string which describes (in summary) the factor values
     * 
     * @param factorValues
     */
    public void setFactorValues( String factorValues ) {
        this.factorValues = factorValues;
    }

    public void setId( long id ) {
        this.id = id;
    }

    public void setName( String name ) {
        this.name = name;
    }

    /**
     * @param numValues the numValues to set
     */
    public void setNumValues( int numValues ) {
        this.numValues = numValues;
    }

    public void setType( String type ) {
        this.type = type;
    }

    public void setValues( Collection<FactorValueValueObject> values ) {
        this.values = values;
    }

    private String getCategoryUri( Characteristic c ) {
        if ( c instanceof VocabCharacteristic ) {
            VocabCharacteristic vc = ( VocabCharacteristic ) c;
            return vc.getCategoryUri();
        }
        return null;

    }

}
