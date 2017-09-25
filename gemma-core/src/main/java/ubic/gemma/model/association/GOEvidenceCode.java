// license-header java merge-point
//
// Attention: Generated code! Do not modify by hand!
// Generated by: Gemma Enumeration.vsl in andromda-java-cartridge.
// $Id$
package ubic.gemma.model.association;

import java.util.*;

/**
 * This enumeration was originally based on GO, but is used for all entities that have evidenciary aspects; Thus it has
 * been expanded to include: Terms from RGD&#160;(rat genome database)
 * <ul>
 * <li>IED = Inferred from experimental data
 * <li>IAGP = Inferred from association of genotype and phenotype
 * <li>IPM = Inferred from phenotype manipulation
 * <li>QTM = Quantitative Trait Measurement
 * </ul>
 * And our own custom code IIA which means Inferred from Imported Annotation to distinguish IEAs that we ourselves have
 * computed
 */
@SuppressWarnings("WeakerAccess") // All constants should have the same access level
public class GOEvidenceCode implements java.io.Serializable, Comparable<GOEvidenceCode> {
    public static final GOEvidenceCode IC = new GOEvidenceCode( "IC" );
    public static final GOEvidenceCode IDA = new GOEvidenceCode( "IDA" );
    public static final GOEvidenceCode IEA = new GOEvidenceCode( "IEA" );
    public static final GOEvidenceCode IEP = new GOEvidenceCode( "IEP" );
    public static final GOEvidenceCode IGI = new GOEvidenceCode( "IGI" );
    public static final GOEvidenceCode IMP = new GOEvidenceCode( "IMP" );
    public static final GOEvidenceCode IPI = new GOEvidenceCode( "IPI" );
    public static final GOEvidenceCode ISS = new GOEvidenceCode( "ISS" );
    public static final GOEvidenceCode NAS = new GOEvidenceCode( "NAS" );
    public static final GOEvidenceCode ND = new GOEvidenceCode( "ND" );
    public static final GOEvidenceCode RCA = new GOEvidenceCode( "RCA" );
    public static final GOEvidenceCode TAS = new GOEvidenceCode( "TAS" );
    public static final GOEvidenceCode NR = new GOEvidenceCode( "NR" );
    public static final GOEvidenceCode EXP = new GOEvidenceCode( "EXP" );
    public static final GOEvidenceCode ISA = new GOEvidenceCode( "ISA" );
    public static final GOEvidenceCode ISM = new GOEvidenceCode( "ISM" );
    /**
     * Inferred from Genomic Context; This evidence code can be used whenever information about the genomic context of a
     * gene product forms part of the evidence for a particular annotation. Genomic context includes, but is not limited
     * to, such things as identity of the genes neighboring the gene product in question (i.e. synteny), operon
     * structure, and phylogenetic or other whole genome analysis. "We recommend making an entry in the with/from column
     * when using this evidence code. In cases where operon structure or synteny are the compelling evidence, include
     * identifier(s) for the neighboring genes in the with/from column. In casees where metabolic reconstruction is the
     * compelling evidence, and there is an identifier for the pathway or system, that should be entered in the
     * with/from column. When multiple entries are placed in the with/from field, they are separated by pipes."
     */
    public static final GOEvidenceCode IGC = new GOEvidenceCode( "IGC" );

    public static final GOEvidenceCode ISO = new GOEvidenceCode( "ISO" );
    /**
     * Added by Gemma: Inferred from Imported Annotation. To be distinguished from IEA or IC, represents annotations
     * that were present in imported data, and which have unknown evidence in the original source (though generally put
     * there manually).
     */
    public static final GOEvidenceCode IIA = new GOEvidenceCode( "IIA" );
    /**
     * A type of phylogenetic evidence whereby an aspect of a descendent is inferred through the characterization of an
     * aspect of a ancestral gene.
     */
    public static final GOEvidenceCode IBA = new GOEvidenceCode( "IBA" );
    /**
     * A type of phylogenetic evidence whereby an aspect of an ancestral gene is inferred through the characterization
     * of an aspect of a descendant gene.
     */
    public static final GOEvidenceCode IBD = new GOEvidenceCode( "IBD" );
    /**
     * A type of phylogenetic evidence characterized by the loss of key sequence residues. Annotating with this evidence
     * codes implies a NOT annotation. This evidence code is also referred to as IMR (inferred from Missing Residues).
     */
    public static final GOEvidenceCode IKR = new GOEvidenceCode( "IKR" );
    /**
     * Inferred from Rapid Divergence. A type of phylogenetic evidence characterized by rapid divergence from ancestral
     * sequence. Annotating with this evidence codes implies a NOT annotation.
     */
    public static final GOEvidenceCode IRD = new GOEvidenceCode( "IRD" );
    /**
     * Inferred from Missing Residues. Represents a NOT association. IMR is a synonym of IKR.
     */
    public static final GOEvidenceCode IMR = new GOEvidenceCode( "IMR" );
    /**
     * Inferred from experimental data (RGD code)
     */
    public static final GOEvidenceCode IED = new GOEvidenceCode( "IED" );
    /**
     * Inferred from association of genotype and phenotype (RGD code)
     */
    public static final GOEvidenceCode IAGP = new GOEvidenceCode( "IAGP" );
    /**
     * Inferred from phenotype manipulation (RGD code)
     */
    public static final GOEvidenceCode IPM = new GOEvidenceCode( "IPM" );
    /**
     * Quantitative Trait Measurement (RGD code)
     */
    public static final GOEvidenceCode QTM = new GOEvidenceCode( "QTM" );
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 1672679992320181566L;
    private static final Map<String, GOEvidenceCode> values = new LinkedHashMap<>( 28, 1 );
    private static List<String> literals = new ArrayList<>( 28 );
    private static List<String> names = new ArrayList<>( 28 );
    private static List<GOEvidenceCode> valueList = new ArrayList<>( 28 );

    static {
        values.put( IC.value, IC );
        valueList.add( IC );
        literals.add( IC.value );
        names.add( "IC" );
        values.put( IDA.value, IDA );
        valueList.add( IDA );
        literals.add( IDA.value );
        names.add( "IDA" );
        values.put( IEA.value, IEA );
        valueList.add( IEA );
        literals.add( IEA.value );
        names.add( "IEA" );
        values.put( IEP.value, IEP );
        valueList.add( IEP );
        literals.add( IEP.value );
        names.add( "IEP" );
        values.put( IGI.value, IGI );
        valueList.add( IGI );
        literals.add( IGI.value );
        names.add( "IGI" );
        values.put( IMP.value, IMP );
        valueList.add( IMP );
        literals.add( IMP.value );
        names.add( "IMP" );
        values.put( IPI.value, IPI );
        valueList.add( IPI );
        literals.add( IPI.value );
        names.add( "IPI" );
        values.put( ISS.value, ISS );
        valueList.add( ISS );
        literals.add( ISS.value );
        names.add( "ISS" );
        values.put( NAS.value, NAS );
        valueList.add( NAS );
        literals.add( NAS.value );
        names.add( "NAS" );
        values.put( ND.value, ND );
        valueList.add( ND );
        literals.add( ND.value );
        names.add( "ND" );
        values.put( RCA.value, RCA );
        valueList.add( RCA );
        literals.add( RCA.value );
        names.add( "RCA" );
        values.put( TAS.value, TAS );
        valueList.add( TAS );
        literals.add( TAS.value );
        names.add( "TAS" );
        values.put( NR.value, NR );
        valueList.add( NR );
        literals.add( NR.value );
        names.add( "NR" );
        values.put( EXP.value, EXP );
        valueList.add( EXP );
        literals.add( EXP.value );
        names.add( "EXP" );
        values.put( ISA.value, ISA );
        valueList.add( ISA );
        literals.add( ISA.value );
        names.add( "ISA" );
        values.put( ISM.value, ISM );
        valueList.add( ISM );
        literals.add( ISM.value );
        names.add( "ISM" );
        values.put( IGC.value, IGC );
        valueList.add( IGC );
        literals.add( IGC.value );
        names.add( "IGC" );
        values.put( ISO.value, ISO );
        valueList.add( ISO );
        literals.add( ISO.value );
        names.add( "ISO" );
        values.put( IIA.value, IIA );
        valueList.add( IIA );
        literals.add( IIA.value );
        names.add( "IIA" );
        values.put( IBA.value, IBA );
        valueList.add( IBA );
        literals.add( IBA.value );
        names.add( "IBA" );
        values.put( IBD.value, IBD );
        valueList.add( IBD );
        literals.add( IBD.value );
        names.add( "IBD" );
        values.put( IKR.value, IKR );
        valueList.add( IKR );
        literals.add( IKR.value );
        names.add( "IKR" );
        values.put( IRD.value, IRD );
        valueList.add( IRD );
        literals.add( IRD.value );
        names.add( "IRD" );
        values.put( IMR.value, IMR );
        valueList.add( IMR );
        literals.add( IMR.value );
        names.add( "IMR" );
        values.put( IED.value, IED );
        valueList.add( IED );
        literals.add( IED.value );
        names.add( "IED" );
        values.put( IAGP.value, IAGP );
        valueList.add( IAGP );
        literals.add( IAGP.value );
        names.add( "IAGP" );
        values.put( IPM.value, IPM );
        valueList.add( IPM );
        literals.add( IPM.value );
        names.add( "IPM" );
        values.put( QTM.value, QTM );
        valueList.add( QTM );
        literals.add( QTM.value );
        names.add( "QTM" );
        valueList = Collections.unmodifiableList( valueList );
        literals = Collections.unmodifiableList( literals );
        names = Collections.unmodifiableList( names );
    }

    private String value;

    /**
     * The default constructor allowing super classes to access it.
     */
    protected GOEvidenceCode() {
    }

    private GOEvidenceCode( String value ) {
        this.value = value;
    }

    /**
     * Creates an instance of GOEvidenceCode from <code>value</code>.
     *
     * @param value the value to create the GOEvidenceCode from.
     */
    public static GOEvidenceCode fromString( String value ) {
        final GOEvidenceCode typeValue = values.get( value );
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
    public static List<GOEvidenceCode> values() {
        return valueList;
    }

    /**
     * @see Comparable#compareTo(Object)
     */
    @Override
    public int compareTo( GOEvidenceCode that ) {
        return ( this == that ) ? 0 : this.getValue().compareTo( ( that ).getValue() );
    }

    /**
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals( Object object ) {
        return ( this == object ) || ( object instanceof GOEvidenceCode && ( ( GOEvidenceCode ) object ).getValue()
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
     * will be the singleton for the JVM in which the current thread is running. Doing this will allow users to safely
     * use the equality operator <code>==</code> for enumerations because a regular deserialized object is always a
     * newly constructed instance and will therefore never be an existing reference; it is this
     * <code>readResolve()</code> method which will intercept the deserialization process in order to return the proper
     * singleton reference. This method is documented here: <a
     * href="http://java.sun.com/j2se/1.3/docs/guide/serialization/spec/input.doc6.html">Java Object Serialization
     * Specification</a>
     */
    private Object readResolve() {
        return GOEvidenceCode.fromString( this.value );
    }
}