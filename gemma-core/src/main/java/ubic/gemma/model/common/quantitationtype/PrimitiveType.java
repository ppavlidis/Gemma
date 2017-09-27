/*
 * The gemma-core project
 *
 * Copyright (c) 2017 University of British Columbia
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
package ubic.gemma.model.common.quantitationtype;

import java.io.Serializable;
import java.util.*;

public class PrimitiveType implements Serializable, Comparable<PrimitiveType> {
    public static final PrimitiveType DOUBLE = new PrimitiveType( "DOUBLE" );
    public static final PrimitiveType INT = new PrimitiveType( "INT" );
    public static final PrimitiveType LONG = new PrimitiveType( "LONG" );
    public static final PrimitiveType CHAR = new PrimitiveType( "CHAR" );
    public static final PrimitiveType BOOLEAN = new PrimitiveType( "BOOLEAN" );
    public static final PrimitiveType STRING = new PrimitiveType( "STRING" );
    public static final PrimitiveType INTARRAY = new PrimitiveType( "INTARRAY" );
    public static final PrimitiveType DOUBLEARRAY = new PrimitiveType( "DOUBLEARRAY" );
    public static final PrimitiveType CHARARRAY = new PrimitiveType( "CHARARRAY" );
    public static final PrimitiveType BOOLEANARRAY = new PrimitiveType( "BOOLEANARRAY" );
    public static final PrimitiveType STRINGARRAY = new PrimitiveType( "STRINGARRAY" );
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 8068644810546069278L;
    private static final Map<String, PrimitiveType> values = new LinkedHashMap<>( 11, 1 );

    private static List<String> literals = new ArrayList<>( 11 );
    private static List<String> names = new ArrayList<>( 11 );
    private static List<PrimitiveType> valueList = new ArrayList<>( 11 );

    static {
        values.put( DOUBLE.value, DOUBLE );
        valueList.add( DOUBLE );
        literals.add( DOUBLE.value );
        names.add( "DOUBLE" );
        values.put( INT.value, INT );
        valueList.add( INT );
        literals.add( INT.value );
        names.add( "INT" );
        values.put( LONG.value, LONG );
        valueList.add( LONG );
        literals.add( LONG.value );
        names.add( "LONG" );
        values.put( CHAR.value, CHAR );
        valueList.add( CHAR );
        literals.add( CHAR.value );
        names.add( "CHAR" );
        values.put( BOOLEAN.value, BOOLEAN );
        valueList.add( BOOLEAN );
        literals.add( BOOLEAN.value );
        names.add( "BOOLEAN" );
        values.put( STRING.value, STRING );
        valueList.add( STRING );
        literals.add( STRING.value );
        names.add( "STRING" );
        values.put( INTARRAY.value, INTARRAY );
        valueList.add( INTARRAY );
        literals.add( INTARRAY.value );
        names.add( "INTARRAY" );
        values.put( DOUBLEARRAY.value, DOUBLEARRAY );
        valueList.add( DOUBLEARRAY );
        literals.add( DOUBLEARRAY.value );
        names.add( "DOUBLEARRAY" );
        values.put( CHARARRAY.value, CHARARRAY );
        valueList.add( CHARARRAY );
        literals.add( CHARARRAY.value );
        names.add( "CHARARRAY" );
        values.put( BOOLEANARRAY.value, BOOLEANARRAY );
        valueList.add( BOOLEANARRAY );
        literals.add( BOOLEANARRAY.value );
        names.add( "BOOLEANARRAY" );
        values.put( STRINGARRAY.value, STRINGARRAY );
        valueList.add( STRINGARRAY );
        literals.add( STRINGARRAY.value );
        names.add( "STRINGARRAY" );
        valueList = Collections.unmodifiableList( valueList );
        literals = Collections.unmodifiableList( literals );
        names = Collections.unmodifiableList( names );
    }

    private String value;

    /**
     * The default constructor allowing super classes to access it.
     */
    protected PrimitiveType() {
    }

    private PrimitiveType( String value ) {
        this.value = value;
    }

    /**
     * Creates an instance of PrimitiveType from <code>value</code>.
     *
     * @param value the value to create the PrimitiveType from.
     * @return primitive type
     */
    public static PrimitiveType fromString( String value ) {
        final PrimitiveType typeValue = values.get( value );
        if ( typeValue == null ) {
            /*
             * Customization to permit database values to change before code does. Previously this would throw an
             * exception.
             */
            // throw new IllegalArgumentException("invalid value '" + value + "', possible values are: " + literals);
            return null;
        }
        return typeValue;
    }

    /**
     * Returns an unmodifiable list containing the literals that are known by this enumeration.
     *
     * @return A List containing the actual literals defined by this enumeration, this list can not be modified.
     */
    public static List<String> literals() {
        return literals;
    }

    /**
     * Returns an unmodifiable list containing the names of the literals that are known by this enumeration.
     *
     * @return A List containing the actual names of the literals defined by this enumeration, this list can not be
     * modified.
     */
    public static List<String> names() {
        return names;
    }

    /**
     * Returns an unmodifiable list containing the actual enumeration instance values.
     *
     * @return A List containing the actual enumeration instance values.
     */
    public static List<PrimitiveType> values() {
        return valueList;
    }

    @Override
    public int compareTo( PrimitiveType that ) {
        return ( this == that ) ? 0 : this.getValue().compareTo( ( that ).getValue() );
    }

    @Override
    public boolean equals( Object object ) {
        return ( this == object ) || ( object instanceof PrimitiveType && ( ( PrimitiveType ) object ).getValue()
                .equals( this.getValue() ) );
    }

    /**
     * Gets the underlying value of this type safe enumeration.
     *
     * @return the underlying value.
     */
    public String getValue() {
        return this.value;
    }

    @Override
    public int hashCode() {
        return this.getValue().hashCode();
    }

    @Override
    public String toString() {
        return String.valueOf( value );
    }

    /**
     * This method allows the deserialization of an instance of this enumeration type to return the actual instance that
     * will be the singleton for the JVM in which the current thread is running.
     * Doing this will allow users to safely use the equality operator <code>==</code> for enumerations because a
     * regular deserialized object is always a newly constructed instance and will therefore never be an existing
     * reference; it is this <code>readResolve()</code> method which will intercept the deserialization process in order
     * to return the proper singleton reference.
     * This method is documented here: <a
     * href="http://java.sun.com/j2se/1.3/docs/guide/serialization/spec/input.doc6.html">Java Object Serialization
     * Specification</a>
     *
     * @return object
     */
    private Object readResolve() {
        return PrimitiveType.fromString( this.value );
    }
}