// license-header java merge-point
//
// Attention: Generated code! Do not modify by hand!
// Generated by: Gemma Enumeration.vsl in andromda-java-cartridge.
// $Id$
package ubic.gemma.model.expression.arrayDesign;

/**
 * 
 */
public class TechnologyType implements java.io.Serializable, Comparable<TechnologyType> {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 994098639935513674L;

    /**
     * <p>
     * Indicates this array design uses two channels and expression measurements are ratios.
     * </p>
     */
    public static final TechnologyType TWOCOLOR = new TechnologyType( "TWOCOLOR" );

    /**
     * <p>
     * Indicates this array design can be used in either a one- or two- channel mode.
     * </p>
     */
    public static final TechnologyType DUALMODE = new TechnologyType( "DUALMODE" );

    /**
     * <p>
     * Indicates this array design uses one channel and measurements are non-ratiometric.
     * </p>
     */
    public static final TechnologyType ONECOLOR = new TechnologyType( "ONECOLOR" );

    /**
     * <p>
     * Indicates that this "array design" is not really an array, such as a map directly to genes, as we use for RNA-seq
     * based data.
     * </p>
     */
    public static final TechnologyType NONE = new TechnologyType( "NONE" );

    /**
     * Creates an instance of TechnologyType from <code>value</code>.
     * 
     * @param value the value to create the TechnologyType from.
     */
    public static TechnologyType fromString( String value ) {
        final TechnologyType typeValue = values.get( value );
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
    public static java.util.List<String> literals() {
        return literals;
    }

    /**
     * Returns an unmodifiable list containing the names of the literals that are known by this enumeration.
     * 
     * @return A List containing the actual names of the literals defined by this enumeration, this list can not be
     *         modified.
     */
    public static java.util.List<String> names() {
        return names;
    }

    /**
     * Returns an unmodifiable list containing the actual enumeration instance values.
     * 
     * @return A List containing the actual enumeration instance values.
     */
    public static java.util.List<TechnologyType> values() {
        return valueList;
    }

    private String value;

    private static final java.util.Map<String, TechnologyType> values = new java.util.LinkedHashMap<String, TechnologyType>(
            4, 1 );

    private static java.util.List<String> literals = new java.util.ArrayList<String>( 4 );

    private static java.util.List<String> names = new java.util.ArrayList<String>( 4 );

    private static java.util.List<TechnologyType> valueList = new java.util.ArrayList<TechnologyType>( 4 );

    /**
     * Initializes the values.
     */
    static {
        values.put( TWOCOLOR.value, TWOCOLOR );
        valueList.add( TWOCOLOR );
        literals.add( TWOCOLOR.value );
        names.add( "TWOCOLOR" );
        values.put( DUALMODE.value, DUALMODE );
        valueList.add( DUALMODE );
        literals.add( DUALMODE.value );
        names.add( "DUALMODE" );
        values.put( ONECOLOR.value, ONECOLOR );
        valueList.add( ONECOLOR );
        literals.add( ONECOLOR.value );
        names.add( "ONECOLOR" );
        values.put( NONE.value, NONE );
        valueList.add( NONE );
        literals.add( NONE.value );
        names.add( "NONE" );
        valueList = java.util.Collections.unmodifiableList( valueList );
        literals = java.util.Collections.unmodifiableList( literals );
        names = java.util.Collections.unmodifiableList( names );
    }

    /**
     * The default constructor allowing super classes to access it.
     */
    protected TechnologyType() {
    }

    private TechnologyType( String value ) {
        this.value = value;
    }

    /**
     * @see Comparable#compareTo(Object)
     */
    @Override
    public int compareTo( TechnologyType that ) {
        return ( this == that ) ? 0 : this.getValue().compareTo( ( that ).getValue() );
    }

    /**
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals( Object object ) {
        return ( this == object )
                || ( object instanceof TechnologyType && ( ( TechnologyType ) object ).getValue().equals(
                        this.getValue() ) );
    }

    /**
     * Gets the underlying value of this type safe enumeration.
     * 
     * @return the underlying value.
     */
    public String getValue() {
        return this.value;
    }

    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.getValue().hashCode();
    }

    /**
     * @see Object#toString()
     */
    @Override
    public String toString() {
        return String.valueOf( value );
    }

    /**
     * This method allows the deserialization of an instance of this enumeration type to return the actual instance that
     * will be the singleton for the JVM in which the current thread is running.
     * <p>
     * Doing this will allow users to safely use the equality operator <code>==</code> for enumerations because a
     * regular deserialized object is always a newly constructed instance and will therefore never be an existing
     * reference; it is this <code>readResolve()</code> method which will intercept the deserialization process in order
     * to return the proper singleton reference.
     * <p>
     * This method is documented here: <a
     * href="http://java.sun.com/j2se/1.3/docs/guide/serialization/spec/input.doc6.html">Java Object Serialization
     * Specification</a>
     */
    private Object readResolve() {
        return TechnologyType.fromString( this.value );
    }
}