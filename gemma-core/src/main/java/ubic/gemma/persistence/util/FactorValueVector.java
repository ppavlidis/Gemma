/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.persistence.util;

import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

import java.util.*;

/**
 * @author luke
 */
public class FactorValueVector {

    private static final DescribableComparator factorComparator = DescribableComparator.getInstance();
    private static final FactorValueComparator factorValueComparator = FactorValueComparator.getInstance();
    /**
     * An ordered list of ExperimentalFactors represented in this vector. The order must be guaranteed across all
     * FactorValueVectors. i.e.: any two FactorValueVectors containing FactorValues for the same ExperimentalFactors
     * must maintain this list in the same order. [FIXME I don't see why this is necessary, as there can be only one
     * FactorValue per Factor per vector ? I fixed it so at least we don't get duplicates.]
     */
    private List<ExperimentalFactor> factors;
    /**
     * A map from ExperimentalFactor to an ordered list of FactorValues. The order must be guaranteed as above.
     */
    private Map<ExperimentalFactor, TreeSet<FactorValue>> valuesForFactor;
    private String key;

    /**
     * @param factors for the experiment
     * @param values  for one sample
     */
    public FactorValueVector( Collection<ExperimentalFactor> factors, Collection<FactorValue> values ) {

        valuesForFactor = new HashMap<ExperimentalFactor, TreeSet<FactorValue>>();

        for ( ExperimentalFactor f : factors ) {

            boolean found = false;
            for ( FactorValue v : values ) {
                if ( v.getExperimentalFactor().equals( f ) ) {
                    getValuesForFactor( f ).add( v );
                    found = true;
                    break;
                }
            }

            // Make sure we add a value for each factor; in this case we have a missing value.
            if ( !found ) {
                getValuesForFactor( f ).add( null );
            }

        }

        for ( FactorValue value : values ) {
            if ( value.getExperimentalFactor() != null && factors.contains( value.getExperimentalFactor() ) ) {
                getValuesForFactor( value.getExperimentalFactor() ).add( value );
            }
        }
        // for ( TreeSet<FactorValue> storedValues : valuesForFactor.values() ) {
        // Collections.sort( storedValues, factorValueComparator );
        // }
        this.factors = new ArrayList<ExperimentalFactor>( valuesForFactor.keySet() );
        Collections.sort( this.factors, factorComparator );

        key = buildKey();

    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj instanceof FactorValueVector )
            return key.equals( ( ( FactorValueVector ) obj ).key );

        return false;
    }

    public List<ExperimentalFactor> getFactors() {
        return factors;
    }

    public Collection<FactorValue> getValuesForFactor( ExperimentalFactor factor ) {
        if ( !valuesForFactor.containsKey( factor ) ) {
            valuesForFactor.put( factor, new TreeSet<FactorValue>( factorValueComparator ) );
        }
        return valuesForFactor.get( factor );
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return key;
    }

    private String buildKey() {
        StringBuffer buf = new StringBuffer();
        buf.append( "[" );
        for ( Iterator<ExperimentalFactor> i = factors.iterator(); i.hasNext(); ) {
            ExperimentalFactor factor = i.next();
            buf.append( factor.getCategory() );
            buf.append( factor.getName() + " (" + factor.getDescription() + ")" );
            buf.append( " => [ " );
            for ( Iterator<FactorValue> it = getValuesForFactor( factor ).iterator(); it.hasNext(); ) {
                buf.append( it.next() );
                if ( it.hasNext() )
                    buf.append( ", " );
            }
            buf.append( " ] " );
            if ( i.hasNext() )
                buf.append( "; " );
        }
        buf.append( "]" );
        return buf.toString();
    }
}
