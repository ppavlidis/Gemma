/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package edu.columbia.gemma.loader.expression.mage;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biomage.Array.ArrayManufacture;
import org.biomage.ArrayDesign.ArrayDesign;
import org.biomage.ArrayDesign.CompositeGroup;
import org.biomage.ArrayDesign.PhysicalArrayDesign;
import org.biomage.ArrayDesign.ReporterGroup;
import org.biomage.AuditAndSecurity.Contact;
import org.biomage.AuditAndSecurity.Organization;
import org.biomage.AuditAndSecurity.Person;
import org.biomage.BQS.BibliographicReference;
import org.biomage.BioAssay.BioAssay;
import org.biomage.BioAssay.BioAssayCreation;
import org.biomage.BioAssay.Channel;
import org.biomage.BioAssay.DerivedBioAssay;
import org.biomage.BioAssay.FeatureExtraction;
import org.biomage.BioAssay.MeasuredBioAssay;
import org.biomage.BioAssay.PhysicalBioAssay;
import org.biomage.BioAssayData.BioAssayData;
import org.biomage.BioAssayData.BioAssayDimension;
import org.biomage.BioAssayData.BioAssayMap;
import org.biomage.BioAssayData.BioDataCube;
import org.biomage.BioAssayData.BioDataTuples;
import org.biomage.BioAssayData.BioDataValues;
import org.biomage.BioAssayData.CompositeSequenceDimension;
import org.biomage.BioAssayData.DataExternal;
import org.biomage.BioAssayData.DerivedBioAssayData;
import org.biomage.BioAssayData.DesignElementDimension;
import org.biomage.BioAssayData.FeatureDimension;
import org.biomage.BioAssayData.MeasuredBioAssayData;
import org.biomage.BioAssayData.QuantitationTypeDimension;
import org.biomage.BioAssayData.ReporterDimension;
import org.biomage.BioAssayData.Transformation;
import org.biomage.BioMaterial.BioMaterialMeasurement;
import org.biomage.BioMaterial.BioSample;
import org.biomage.BioMaterial.BioSource;
import org.biomage.BioMaterial.CompoundMeasurement;
import org.biomage.BioMaterial.LabeledExtract;
import org.biomage.Common.Describable;
import org.biomage.Common.Extendable;
import org.biomage.Description.Database;
import org.biomage.Description.Description;
import org.biomage.Description.OntologyEntry;
import org.biomage.DesignElement.Feature;
import org.biomage.DesignElement.FeatureInformation;
import org.biomage.DesignElement.FeatureReporterMap;
import org.biomage.DesignElement.ReporterCompositeMap;
import org.biomage.DesignElement.ReporterPosition;
import org.biomage.Experiment.Experiment;
import org.biomage.Experiment.ExperimentDesign;
import org.biomage.Measurement.Unit;
import org.biomage.Measurement.Measurement.KindCV;
import org.biomage.Measurement.Measurement.Type;
import org.biomage.Protocol.Hardware;
import org.biomage.Protocol.Protocol;
import org.biomage.QuantitationType.ConfidenceIndicator;
import org.biomage.QuantitationType.DerivedSignal;
import org.biomage.QuantitationType.Failed;
import org.biomage.QuantitationType.MeasuredSignal;
import org.biomage.QuantitationType.PValue;
import org.biomage.QuantitationType.PresentAbsent;
import org.biomage.QuantitationType.QuantitationType;
import org.biomage.QuantitationType.Ratio;
import org.biomage.QuantitationType.SpecializedQuantitationType;
import org.dom4j.Document;
import org.dom4j.Element;

import edu.columbia.gemma.common.description.Characteristic;
import edu.columbia.gemma.common.description.DatabaseEntry;
import edu.columbia.gemma.common.description.DatabaseType;
import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.common.description.LocalFile;
import edu.columbia.gemma.common.measurement.Measurement;
import edu.columbia.gemma.common.measurement.MeasurementKind;
import edu.columbia.gemma.common.measurement.MeasurementType;
import edu.columbia.gemma.common.protocol.Parameter;
import edu.columbia.gemma.common.protocol.ProtocolApplication;
import edu.columbia.gemma.common.protocol.Software;
import edu.columbia.gemma.common.quantitationtype.GeneralType;
import edu.columbia.gemma.common.quantitationtype.PrimitiveType;
import edu.columbia.gemma.common.quantitationtype.ScaleType;
import edu.columbia.gemma.common.quantitationtype.StandardQuantitationType;
import edu.columbia.gemma.expression.biomaterial.BioMaterial;
import edu.columbia.gemma.expression.biomaterial.Compound;
import edu.columbia.gemma.expression.biomaterial.Treatment;
import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.designElement.DesignElement;
import edu.columbia.gemma.expression.designElement.Reporter;
import edu.columbia.gemma.expression.experiment.ExperimentalDesign;
import edu.columbia.gemma.expression.experiment.ExperimentalFactor;
import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.expression.experiment.FactorValue;
import edu.columbia.gemma.genome.biosequence.BioSequence;
import edu.columbia.gemma.genome.biosequence.PolymerType;
import edu.columbia.gemma.genome.biosequence.SequenceType;
import edu.columbia.gemma.loader.expression.geo.model.GeoSample;
import edu.columbia.gemma.loader.loaderutils.MgedOntologyHelper;
import edu.columbia.gemma.util.ReflectionUtil;

/**
 * <p>
 * Class to convert Mage domain objects to Gemma domain objects. In most cases, the user can simply call the "convert"
 * method on any MAGE domain object and get a fully-populated Gemma domain object. There is no need to use the methods
 * in this class directly when handling MAGE-ML files: use the {@link edu.columbia.gemma.loader.mage.MageMLParser.}
 * </p>
 * <h2>Implementation notes</h2>
 * <p>
 * Most MAGE objects have a corresponding method in this class called 'convertXXXXX', and many have a
 * 'convertXXXXAssociations' to handle the associations. Special cases (outlined below) have additional methods to map
 * MAGE associations to Gemma objects.
 * </p>
 * <h2>Zoo of packages that have references between them, but don't map directly to Gemma</h2>
 * <h3>DesignElement_package and ArrayDesign_package</h3>
 * <p>
 * DesignElement Contains the ReporterCompositeMap (and the FeatureReporterMap). This allows us to fill in the map in
 * the CompositeSequence.
 * </p>
 * <p>
 * ArrayDesign Contains the CompositeGropus, FeatureGroups and ReporterGroups. This leads us to a description of all the
 * DesignElement on the array, but not the reportercompositemap.
 * </p>
 * <p>
 * Thus both of these have references to CompositeSequences for example; the two packages can be in different files.
 * Therefore we need to fill in object that have the same Identifier with data from the other file.
 * </p>
 * <h3>BioAssay and BioAssayData</h3>
 * <p>
 * Gemma dispenses with the notion of a distinct BioAssayData object; BioAssays are directly associated to their
 * (external) data files, and in the database the actual data are stored as DataVectors associated with an
 * ExpressionExperiment. In contrast, Mage has a highly complex hierarchy of BioAssays and BioAssayData. In addition,
 * Gemma has only one type of BioAssay, which combines the features needed from the Physical, Measured and Derived
 * bioassays. (The Gemma BioAssay most closely resembles the DerivedBioAssay). Therefore we have to gather information
 * from the various Mage BioAssays and BioAssayData and put it all in one Gemma BioAssay object.
 * </p>
 * <h3>BioMaterial and subclasses</h3>
 * <p>
 * In Gemma we only have BioMaterial, not distinct BioSample, BioSource and LabeledExtract objects.
 * </p>
 * 
 * @see edu.columbia.gemma.loader.mage.MageMLParser
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="mageMLConverterHelper" singleton="false"
 */
@SuppressWarnings("unchecked")
public class MageMLConverterHelper {

    /**
     * Used to indicate that a MAGE list should be converted to a Gemma list (or collection)
     */
    public static final boolean CONVERT_ALL = false;

    /**
     * Used to indicate that when a MAGE list is encountered, we should only process the first element of the list.
     */
    public static final boolean CONVERT_FIRST_ONLY = true;

    public static final Log log = LogFactory.getLog( MageMLConverterHelper.class );

    /**
     * 
     */
    private static final String MGED_ONTOLOGY_URL = "http://mged.sourceforge.net/ontologies/MGEDontology.php";

    /**
     * 
     */
    private static final String MGED_DATABASE_IDENTIFIER = "MGED Ontology";

    /**
     * 
     */
    private static final String UNKNOWN_DATABASE_IDENTIFIER = "Unknown";

    /**
     * Different ways to refer to the MAGE Ontology
     */
    public Set<String> mgedOntologyAliases;

    /**
     * Stores the dimension information for the bioassays
     */
    private BioAssayDimensions bioAssayDimensions;

    /**
     * Holds the simplified MAGE-ML
     */
    private Document simplifiedXml;

    /**
     * 
     */
    private MgedOntologyHelper mgedOntologyHelper;

    /**
     * Places where, according to the current configuration, local MAGE bioDataCube external files are stored.
     */
    private Collection<String> localExternalDataPaths;

    private ExternalDatabase mgedOntology;

    /**
     * @param simplifiedXml
     */
    public void setSimplifiedXml( Document simplifiedXml ) {
        this.simplifiedXml = simplifiedXml;
    }

    /**
     * Constructor
     */
    public MageMLConverterHelper() {
        bioAssayDimensions = new BioAssayDimensions();

        initMGEDOntologyAliases();

        initLocalExternalDataPaths();

        initMGEDOntology();

    }

    /**
     * 
     */
    private void initMGEDOntology() {
        URL ontologyDaml = this.getClass().getResource( "resource/MGEDOntology.daml" );

        log.info( "Reading MGED Ontology" );
        File test = new File( ontologyDaml.getFile() );
        assert test.canRead() : "Could not read MGED Ontology DAML file";
        mgedOntologyHelper = new MgedOntologyHelper( ontologyDaml.getFile() );

    }

    /**
     * 
     */
    private void initLocalExternalDataPaths() {
        localExternalDataPaths = new HashSet<String>();
        try {
            // Eventually there may be more configuration locations.
            Configuration config = new PropertiesConfiguration( "Gemma.properties" );
            String path = ( String ) config.getProperty( "arrayExpress.local.datafile.basepath" );
            File p = new File( path );
            if ( !p.canRead() ) {
                log.error( "Cannot read from " + path );
            }
            localExternalDataPaths.add( path );
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( "Failed to load configuration", e );
        }
    }

    /**
     * 
     */
    private void initMGEDOntologyAliases() {
        mgedOntologyAliases = new HashSet<String>();
        mgedOntologyAliases.add( "MGED Ontology" );
        mgedOntologyAliases.add( "MO" );
        mgedOntologyAliases.add( "ebi.ac.uk:Database:MO" );
    }

    /**
     * A generic converter that figures out which specific conversion method to call based on the class of the object.
     * 
     * @param mageObj
     * @return
     */
    public Object convert( Object mageObj ) {
        log.debug( "Converting " + mageObj.getClass().getSimpleName() );
        return findAndInvokeConverter( mageObj );
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.description.OntologyEntry convertAction( OntologyEntry mageObj ) {
        return convertOntologyEntry( mageObj );
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.expression.arrayDesign.ArrayDesign convertArrayDesign( ArrayDesign mageObj ) {
        if ( mageObj == null ) return null;
        edu.columbia.gemma.expression.arrayDesign.ArrayDesign result = edu.columbia.gemma.expression.arrayDesign.ArrayDesign.Factory
                .newInstance();
        Integer numFeatures = mageObj.getNumberOfFeatures();
        if ( numFeatures != null ) result.setAdvertisedNumberOfDesignElements( numFeatures.intValue() );
        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertArrayDesignAssociations( ArrayDesign mageObj,
            edu.columbia.gemma.expression.arrayDesign.ArrayDesign gemmaObj, Method getter ) {

        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "CompositeGroups" ) ) {
            assert associatedObject instanceof List;
            specialConvertCompositeGroups( ( List ) associatedObject, gemmaObj );
        } else if ( associationName.equals( "DesignProviders" ) ) {
            assert associatedObject instanceof List;
        } else if ( associationName.equals( "FeatureGroups" ) ) {
            // assert associatedObject instanceof List;
            // specialConvertFeatureGroups( ( List ) associatedObject, gemmaObj );
            // no-op
        } else if ( associationName.equals( "ProtocolApplications" ) ) {
            assert associatedObject instanceof List;
        } else if ( associationName.equals( "ReporterGroups" ) ) {
            assert associatedObject instanceof List;
            specialConvertReporterGroups( ( List ) associatedObject, gemmaObj );
        } else {
            log.debug( "Unsupported or unknown association, or it belongs to the subclass: " + associationName );
        }
    }

    /**
     * A no-op, as we don't keep track of this.
     * 
     * @param mageObj
     * @return
     */
    public Object convertArrayManufacture( ArrayManufacture mageObj ) {
        if ( mageObj == null ) return null;
        return null; // no-op.
    }

    /**
     * @param actualGemmaAssociationName
     * @param gemmaAssociatedObj
     * @return
     */
    public String convertAssociationName( String actualGemmaAssociationName, Object gemmaAssociatedObj ) {
        String inferredGemmaAssociationName;
        if ( actualGemmaAssociationName != null ) {
            inferredGemmaAssociationName = actualGemmaAssociationName;
        } else {
            inferredGemmaAssociationName = ReflectionUtil.getBaseForImpl( gemmaAssociatedObj ).getSimpleName();
        }
        return inferredGemmaAssociationName;
    }

    /**
     * Generic method to find the associations a Mage object has and call the appropriate converter method. The
     * converters are named after the MAGE class, not the Gemma class.
     * 
     * @param mageObj
     * @param gemmaObj
     */
    public void convertAssociations( Object mageObj, Object gemmaObj ) {
        log.debug( "Converting associations of " + mageObj.getClass().getSimpleName() + " into associations for Gemma "
                + gemmaObj.getClass().getSimpleName() );
        convertAssociations( mageObj.getClass(), mageObj, gemmaObj );
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.description.BibliographicReference convertBibliographicReference(
            BibliographicReference mageObj ) {
        if ( mageObj == null ) return null;
        edu.columbia.gemma.common.description.BibliographicReference result = edu.columbia.gemma.common.description.BibliographicReference.Factory
                .newInstance();
        convertDescribable( mageObj, result );
        result.setEditor( mageObj.getEditor() );
        result.setAuthorList( mageObj.getAuthors() );
        result.setIssue( mageObj.getIssue() );
        result.setPages( mageObj.getPages() );
        result.setPublication( mageObj.getPublication() );
        result.setPublisher( mageObj.getPublisher() );
        result.setTitle( mageObj.getTitle() );
        result.setVolume( mageObj.getVolume() );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertBibliographicReferenceAssociations( BibliographicReference mageObj,
            edu.columbia.gemma.common.description.BibliographicReference gemmaObj, Method getter ) {

        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;

        if ( associationName.equals( "Accessions" ) ) {
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY, "PubAccession" );
        } else if ( associationName.equals( "Parameters" ) ) {
            // no-op, we don't support.
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.expression.bioAssay.BioAssay convertBioAssay( BioAssay mageObj ) {
        if ( mageObj == null ) return null;
        edu.columbia.gemma.expression.bioAssay.BioAssay result = edu.columbia.gemma.expression.bioAssay.BioAssay.Factory.
        newInstance();
        
        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertBioAssayAssociations( BioAssay mageObj,
            edu.columbia.gemma.expression.bioAssay.BioAssay gemmaObj, Method getter ) {

        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;

        if ( associationName.equals( "BioAssayFactorValues" ) ) {
            // Note that these are be the same factorvalues as referred to by the experimentalfactors.
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        } else if ( associationName.equals( "Channels" ) ) {
            ; // we don't support this.
        } else {
            log.info( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * Special case. We don't have a BioAssayData object, just BioAssays.
     * <p>
     * Mage BioAssayData comes in two flavors: BioDataCubes, and BioDataTuples. It appers that the former is much more
     * common. BioDataCubes, instead of having all three dimensions, tend to have only two: QuantitationType and
     * DesignElement. (Waiting to see an exception to this - certainly allowed by MAGE, but in practice...?)
     * <p>
     * Measured bioassaydata seems to be the 'raw' data, and what is of interest to us. Derived data might not be (?).
     * 
     * @param mageObj BioAssayData to be converted.
     * @return a LocalFile object representing the external data file. Unfortunately, the path stored here is often
     *         useless and must be filled in later.
     * @see specialConvertBioAssayBioAssayDataAssociations
     */
    public LocalFile convertBioAssayData( BioAssayData mageObj ) {

        BioDataValues data = mageObj.getBioDataValues();
        LocalFile result = LocalFile.Factory.newInstance();

        if ( data instanceof BioDataCube ) {
            DataExternal dataExternal = ( ( BioDataCube ) data ).getDataExternal();
            if ( dataExternal == null ) {
                log.warn( "BioDataCube with no external data" );
                return null;
            }
            URI localURI = findLocalMageExternalDataFile( dataExternal.getFilenameURI() );

            if ( localURI == null ) { // FIXME NEED TO GET ACTUAL FILE (or fill in later)
                result.setLocalURI( dataExternal.getFilenameURI() );
            } else {
                log.warn( "Local file not found, filling in " + dataExternal.getFilenameURI() );
                result.setLocalURI( localURI.toString() );
            }

            // this is not really the remote uri - just
            // whatever is in the mage document.
            result.setRemoteURI( dataExternal.getFilenameURI() );

        } else if ( data instanceof BioDataTuples ) {
            log.error( "Not ready to deal with BioDataTuples from Mage" );
            return null;
        } else {
            throw new IllegalArgumentException( "Unkonwn BioDataValue class" );
        }

        convertBioAssayDataAssociations( mageObj );
        return result;
    }

    /**
     * Given a URI, try to find the corresponding local file. The only part of the URI that is looked at is the file
     * name. We then look in known local directory paths that are used to store MAGE-ML derived files.
     * <p>
     * FIXME this is broken, need to do much smarter path search....just the idea for now.
     * 
     * @param seekURI
     * @return
     */
    private URI findLocalMageExternalDataFile( String seekURI ) {
        String fileName = seekURI.substring( seekURI.lastIndexOf( "/" ) + 1 );
        log.debug( "Seeking external data file " + fileName );
        for ( String path : this.localExternalDataPaths ) {
            File f = new File( path + "/" + fileName );
            log.debug( "Looking in " + f.getAbsolutePath() );
            if ( f.exists() ) {
                try {
                    return new URI( "file", "", path, "" );
                } catch ( URISyntaxException e ) {
                    log.error( e, e );
                }
            }
        }
        return null;
    }

    /**
     * In a typical MAGE file, we have the following associations:
     * <p>
     * MeasuredBioAssayData->BioDataCube->DataExternal
     * </p>
     * <p>
     * The dimensions (designelement, quantitationtype and bioassay) are not full-scale Gemma objects, but we need to
     * extract this information so we can decipher the data files. However, the FeatureDimension is often not available
     * in the MAGE file (for example, in the ArrayExpress MAGE files for experiments done on Affymetrix platforms). In
     * these cases we have to get the feature dimension from somewhere else.
     * <p>
     * In MAGE, there can be more than one QuantitationType dimension associated with a single BioAssay, because there
     * can be more than one BioAssayData (Measured, Derived, Physical).
     * <p>
     * There also seems to be no particular standard as to whether the external data files contain compositesequence or
     * reporter or feature data. Feature data makes the most sense. For Affy files that seems to be what you get (so
     * far).
     * <p>
     * Implementation note: Implementation note: We have to store things as Maps of Strings to the objects of interest,
     * rather than using the object itself as a key, because hashCode() for our entities looks just at the primary key
     * (the id), which is not filled in in many cases (when working with non-persistent objects).
     * 
     * @param mageObj
     */
    public void convertBioAssayDataAssociations( BioAssayData mageObj ) {
        QuantitationTypeDimension qtd = mageObj.getQuantitationTypeDimension();
        LinkedHashMap<String, edu.columbia.gemma.common.quantitationtype.QuantitationType> convertedQuantitationTypes = new LinkedHashMap<String, edu.columbia.gemma.common.quantitationtype.QuantitationType>();
        if ( qtd != null ) {
            List<QuantitationType> quantitationTypes = ( List<QuantitationType> ) qtd.getQuantitationTypes();
            for ( QuantitationType type : quantitationTypes ) {
                edu.columbia.gemma.common.quantitationtype.QuantitationType convertedType = convertQuantitationType( type );
                convertedQuantitationTypes.put( convertedType.getName(), convertedType );
            }
        }

        DesignElementDimension ded = mageObj.getDesignElementDimension();
        List<org.biomage.DesignElement.DesignElement> designElements = null;

        BioAssayDimension bioAssayDim = mageObj.getBioAssayDimension();
        if ( bioAssayDim == null ) return;

        List<BioAssay> bioAssays = ( List<BioAssay> ) bioAssayDim.getBioAssays();
        for ( BioAssay bioAssay : bioAssays ) {
            if ( ded instanceof FeatureDimension ) {
                log.debug( "Got a feature dimension: " + ded.getIdentifier() );
                designElements = ( List<org.biomage.DesignElement.DesignElement> ) ( ( FeatureDimension ) ded )
                        .getContainedFeatures();
            } else if ( ded instanceof CompositeSequenceDimension ) {
                log.debug( "Got a compositesequence dimension: " + ded.getIdentifier() );
                designElements = ( List<org.biomage.DesignElement.DesignElement> ) ( ( CompositeSequenceDimension ) ded )
                        .getCompositeSequences();
            } else if ( ded instanceof ReporterDimension ) {
                log.debug( "Got a reporter dimension: " + ded.getIdentifier() );
                designElements = ( List<org.biomage.DesignElement.DesignElement> ) ( ( ReporterDimension ) ded )
                        .getReporters();
            }

            LinkedHashMap<String, DesignElement> convertedDesignElements = new LinkedHashMap<String, DesignElement>();
            for ( org.biomage.DesignElement.DesignElement designElement : designElements ) {
                DesignElement de = convertDesignElement( designElement );
                if ( de == null ) continue;
                convertedDesignElements.put( de.getName(), de );
            }

            if ( convertedDesignElements.size() == 0 ) {
                // This happens with affymetrix raw data, which don't have explicit probe names. Not much to do about
                // it.
            }

            edu.columbia.gemma.expression.bioAssay.BioAssay convertedBioAssay = edu.columbia.gemma.expression.bioAssay.BioAssay.Factory
                    .newInstance();

            convertIdentifiable( bioAssay, convertedBioAssay );

            bioAssayDimensions.addDesignElementDimension( convertedBioAssay, convertedDesignElements );
            bioAssayDimensions.addQuantitationTypeDimension( convertedBioAssay, convertedQuantitationTypes );

        }

    }

    @SuppressWarnings("unused")
    public edu.columbia.gemma.expression.bioAssayData.BioAssayDimension convertBioAssayDimension( BioAssayDimension bad ) {

        edu.columbia.gemma.expression.bioAssayData.BioAssayDimension resultBioAssayDimension = 
            edu.columbia.gemma.expression.bioAssayData.BioAssayDimension.Factory.newInstance();

        convertIdentifiable( bad, resultBioAssayDimension );

        Collection<BioAssay> bioAssayList = bad.getBioAssays();

        for ( BioAssay sample : bioAssayList )
        {
            edu.columbia.gemma.expression.bioAssay.BioAssay resultBioAssay;
            
            if (sample instanceof MeasuredBioAssay)
                resultBioAssay = convertMeasuredBioAssay((MeasuredBioAssay)sample);
            
            else if (sample instanceof DerivedBioAssay)
                resultBioAssay = convertDerivedBioAssay((DerivedBioAssay)sample);
            
            else //physical
                resultBioAssay = convertBioAssay(sample);
            
            resultBioAssayDimension.getDimensionBioAssays().add(resultBioAssay);
        }
        
        return resultBioAssayDimension;

    }

    /**
     * Given a Gemma bioassay, return the associated DesignElementDimension.
     * 
     * @param bioAssay
     * @return A List of DesignElements representing the DesignElementDimension for the BioAssay. If there is no such
     *         bioAssay in the current data, returns null.
     */
    public List<DesignElement> getBioAssayDesignElementDimension(
            edu.columbia.gemma.expression.bioAssay.BioAssay bioAssay ) {
        if ( bioAssay == null ) throw new IllegalArgumentException();
        return bioAssayDimensions.getDesignElementDimension( bioAssay );
    }

    /**
     * @param bioAssay
     * @return A List of QuantitationTypes representing the QuantitationTypeDimension for the BioAssay. If there is no
     *         such bioAssay in the current data, returns null.
     */
    public List<edu.columbia.gemma.common.quantitationtype.QuantitationType> getBioAssayQuantitationTypeDimension(
            edu.columbia.gemma.expression.bioAssay.BioAssay bioAssay ) {
        if ( bioAssay == null ) throw new IllegalArgumentException();
        return bioAssayDimensions.getQuantitationTypeDimension( bioAssay );
    }

    /**
     * @param mageObj
     * @return
     * @see convertLabeledExtract
     * @see convertBioSource
     * @see convertBioMaterialAssociations
     */
    public edu.columbia.gemma.expression.biomaterial.BioMaterial convertBioMaterial(
            org.biomage.BioMaterial.BioMaterial mageObj ) {
        if ( mageObj == null ) return null;
        edu.columbia.gemma.expression.biomaterial.BioMaterial result = edu.columbia.gemma.expression.biomaterial.BioMaterial.Factory
                .newInstance();

        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     * @see convertBioMaterial
     */
    public void convertBioMaterialAssociations( org.biomage.BioMaterial.BioMaterial mageObj, BioMaterial gemmaObj,
            Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        assert mageObj != null;
        if ( associationName.equals( "Characteristics" ) ) {
            specialConvertBioMaterialBioCharacteristics( mageObj, gemmaObj );
        } else if ( associationName.equals( "MaterialType" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else if ( associationName.equals( "QualityControlStatistics" ) ) {
            assert associatedObject instanceof List;
            // we don't support
        } else if ( associationName.equals( "Treatments" ) ) {
            assert associatedObject instanceof List;
            // specialConvertBioMaterialTreatmentAssociations(
            // ( List<org.biomage.BioMaterial.Treatment> ) associatedObject, gemmaObj );
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        } else {
            log.debug( "Unsupported or unknown association, or from subclass: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param associatedObject
     */
    private void specialConvertBioMaterialBioCharacteristics( org.biomage.BioMaterial.BioMaterial mageObj,
            BioMaterial gemmaObj ) {

        assert simplifiedXml != null;
        List<Element> elmList = simplifiedXml.selectNodes( "/BioList/BioMaterial[@identifier='"
                + mageObj.getIdentifier() + "']/Characteristics/child::node()" );

        // just debugging information.
        if ( elmList.isEmpty() ) {
            List<Element> check = simplifiedXml.selectNodes( "/BioList/BioMaterial[@identifier='"
                    + mageObj.getIdentifier() + "']" );
            if ( check.isEmpty() ) {
                log.error( "Failed to find identifier " + mageObj.getIdentifier() + " in simplified DOM." );
            } else {
                log.debug( "Found identifier " + mageObj.getIdentifier()
                        + " in simplified DOM but no 'Characteristics'" );
            }
            return;
        }

        log.debug( "Found identifier " + mageObj.getIdentifier() + " in simplified DOM." );
        for ( Element elm : elmList ) {
            Characteristic bioCharacteristic = Characteristic.Factory.newInstance();
            bioCharacteristic.setCategory( elm.getName() );
            bioCharacteristic.setValue( elm.valueOf( "@value" ) );

            specialFillInCharacteristicOntologyEntries( bioCharacteristic, elm );

            List subList = elm.selectNodes( "child::node()" );
            Collection<Characteristic> bcConstituents = bioCharacteristic.getConstituents();
            if ( subList.size() > 0 ) {
                for ( Iterator subIter = subList.iterator(); subIter.hasNext(); ) {
                    Element elmSub = ( Element ) subIter.next();
                    Characteristic bcConstitutent = Characteristic.Factory.newInstance();
                    bcConstitutent.setCategory( elmSub.getName() );
                    bcConstitutent.setValue( elmSub.valueOf( "@value" ) );
                    log.debug( " CAT: " + bcConstitutent.getCategory() + " VAL: " + bcConstitutent.getValue() );
                    bcConstituents.add( bcConstitutent );
                }
                bioCharacteristic.setConstituents( bcConstituents );
            }
            gemmaObj.getCharacteristics().add( bioCharacteristic );
        }
    }

    /**
     * Fill in the associated OntologyEntry (controlled vocabulary terms) for a characteristic. If no accession or
     * database is given, we look in the MGED Ontology (MO) for a matching term.
     * <p>
     * Unfortunately, in MO the instances don't necessarily match up with the categories - annotators seem to use
     * whatever instance for whatever category. For example, technically a BioSampleType can only be "extract" or
     * "not_extract", but our tests include a file that has "fresh_sample" as the value for BioSampleType. Therefore we
     * have to check the instances of <em>other</em> classes as well.
     * 
     * @param characteristic
     * @param elm that holds the parsed accession and database identifiers.
     */
    private void specialFillInCharacteristicOntologyEntries( Characteristic characteristic, Element elm ) {
        assert mgedOntologyHelper != null;
        if ( characteristic == null ) {
            log.warn( "Null characteristic passed, ignoring" );
            return;
        }

        if ( characteristic.getCategory() == null ) throw new IllegalArgumentException( "Category cannot be null" );

        boolean isCategoryMo = false;
        boolean isValueMo = false;
        boolean hasCategoryAcc = false;
        boolean hasValueAcc = false;
        String categoryDb = elm.valueOf( "@CategoryDatabaseIdentifier" );
        String categoryAcc = elm.valueOf( "@CategoryDatabaseAccession" );
        String valueDb = elm.valueOf( "@ValueDatabaseIdentifier" );
        String valueAcc = elm.valueOf( "@ValueDatabaseAccession" );

        if ( categoryDb.length() > 0 ) {
            isCategoryMo = this.mgedOntologyAliases.contains( categoryDb );
            if ( isCategoryMo ) {
                categoryDb = MGED_DATABASE_IDENTIFIER;
            }
        } else if ( mgedOntologyHelper.classExists( characteristic.getCategory() ) ) {
            isCategoryMo = true;
            categoryDb = MGED_DATABASE_IDENTIFIER;
        } else {
            log.debug( "No category database for '" + characteristic.getCategory() + "'" );
        }

        if ( categoryAcc.length() > 0 ) {
            if ( isCategoryMo ) {
                categoryAcc = formMgedOntologyAccession( categoryAcc );
            }
        } else if ( isCategoryMo ) {
            categoryAcc = formMgedOntologyAccession( characteristic.getCategory() );
        } else {
            hasCategoryAcc = false;
            log.debug( "No category accession value for '" + characteristic.getCategory() + "'" );
        }

        if ( characteristic.getValue().length() > 0 ) {
            if ( valueDb.length() > 0 ) {
                isValueMo = this.mgedOntologyAliases.contains( valueDb );
                if ( isValueMo ) {
                    valueDb = MGED_DATABASE_IDENTIFIER;
                }
            } else if ( isCategoryMo
                    && mgedOntologyHelper.getInstanceNamesForClass( characteristic.getCategory() ) != null
                    && mgedOntologyHelper.getInstanceNamesForClass( characteristic.getCategory() ).contains(
                            characteristic.getValue() ) ) {
                isValueMo = true;
                valueDb = MGED_DATABASE_IDENTIFIER;

            } else if ( isCategoryMo ) {
                String instanceCategory = this.mgedOntologyHelper.getClassNameForInstance( characteristic.getValue() );
                if ( instanceCategory != null ) {
                    log.debug( "'" + characteristic.getValue() + "' is actually an instance of '" + instanceCategory
                            + "', not '" + characteristic.getCategory() + "', but we just go with the flow." );
                    isValueMo = true;
                    valueDb = MGED_DATABASE_IDENTIFIER;
                } else {
                    log.debug( "No value database available for '" + characteristic.getValue() + "'" );
                }

            } else {
                log.debug( "No value database available for '" + characteristic.getValue() + "'" );
            }

            if ( valueAcc.length() > 0 ) {
                if ( isValueMo ) {
                    valueAcc = formMgedOntologyAccession( valueAcc );
                }
            } else if ( isValueMo ) {
                valueAcc = formMgedOntologyAccession( characteristic.getValue() );
            } else {
                hasValueAcc = false;
            }
        }

        if ( hasCategoryAcc ) {
            ExternalDatabase categoryExternalDatabase = ExternalDatabase.Factory.newInstance();
            categoryExternalDatabase.setName( categoryDb );
            edu.columbia.gemma.common.description.OntologyEntry categoryOntologyEntry = edu.columbia.gemma.common.description.OntologyEntry.Factory
                    .newInstance();
            categoryOntologyEntry.setAccession( categoryAcc );
            categoryOntologyEntry.setExternalDatabase( categoryExternalDatabase );
            categoryOntologyEntry.setCategory( characteristic.getCategory() );
            categoryOntologyEntry.setValue( characteristic.getCategory() );
            characteristic.setCategoryTerm( categoryOntologyEntry );
        }

        if ( hasValueAcc ) {
            ExternalDatabase valueExternalDatabase = ExternalDatabase.Factory.newInstance();
            valueExternalDatabase.setName( valueDb );
            edu.columbia.gemma.common.description.OntologyEntry valueOntologyEntry = edu.columbia.gemma.common.description.OntologyEntry.Factory
                    .newInstance();
            valueOntologyEntry.setAccession( valueAcc );
            valueOntologyEntry.setExternalDatabase( valueExternalDatabase );
            valueOntologyEntry.setCategory( characteristic.getValue() );
            valueOntologyEntry.setValue( characteristic.getValue() );
            characteristic.setValueTerm( valueOntologyEntry );
        }

        log.debug( "Category: '" + characteristic.getCategory() + "'   Value: '" + characteristic.getValue()
                + "'   CatDb: '" + categoryDb + "'  ValDb: '" + valueDb + "'   CatAcc: '" + categoryAcc
                + "'   ValAcc: " + valueAcc );
    }

    /**
     * @param name Possibly mal-formed accession
     * @return property formed accession identifier for the term.
     */
    private String formMgedOntologyAccession( String name ) {
        if ( name.startsWith( MGED_ONTOLOGY_URL ) ) {
            return name;
        } else if ( name.startsWith( "#" ) ) {
            return MGED_ONTOLOGY_URL + name;
        } else {
            return MGED_ONTOLOGY_URL + "#" + name;
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public BioMaterial convertBioSample( BioSample mageObj ) {
        if ( mageObj == null ) return null;

        BioMaterial result = BioMaterial.Factory.newInstance();

        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     * @see convertBioMaterialAssociations
     */
    public void convertBioSampleAssociations( BioSample mageObj, BioMaterial gemmaObj, Method getter ) {
        if ( mageObj == null ) return;
        convertBioMaterialAssociations( mageObj, gemmaObj, getter );
        Object associatedObject = intializeConversion( mageObj, getter );
        if ( associatedObject == null ) return;
        String associationName = getterToPropertyName( getter );
        if ( associationName.equals( "Type" ) ) {
            // we don't support.
        } else {
            log.debug( "Unknown or unsupported type, or is for superclass " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return edu.columbia.gemma.sequence.biosequence.BioSequence
     */
    public edu.columbia.gemma.genome.biosequence.BioSequence convertBioSequence(
            org.biomage.BioSequence.BioSequence mageObj ) {
        if ( mageObj == null ) return null;

        edu.columbia.gemma.genome.biosequence.BioSequence result = edu.columbia.gemma.genome.biosequence.BioSequence.Factory
                .newInstance();

        result.setSequence( mageObj.getSequence() );
        if ( mageObj.getLength() != null ) result.setLength( mageObj.getLength().longValue() );
        if ( mageObj.getIsApproximateLength() != null )
            result.setIsApproximateLength( mageObj.getIsApproximateLength().booleanValue() );
        if ( mageObj.getIsCircular() != null ) result.setIsCircular( mageObj.getIsCircular().booleanValue() );

        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * Implementation note: This is passed a 'get' method for an association to one or more MAGE domain objects. In some
     * cases, the subsequent call can be computed using reflection; but in other cases it will have to be done by hand,
     * as when there is not a direct mapping of Gemma objects to MAGE objects and vice versa.
     * 
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertBioSequenceAssociations( org.biomage.BioSequence.BioSequence mageObj,
            edu.columbia.gemma.genome.biosequence.BioSequence gemmaObj, Method getter ) {

        Object associatedObject = intializeConversion( mageObj, getter );
        if ( associatedObject == null ) return;
        String associationName = getterToPropertyName( getter );

        if ( associationName.equals( "PolymerType" ) ) { // Ontology Entry - enumerated type.
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else if ( associationName.equals( "SequenceDatabases" ) ) { // list of DatabaseEntries, we use one
            assert ( associatedObject instanceof List );
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY, "SequenceDatabaseEntry" );
        } else if ( associationName.equals( "Type" ) ) { // ontology entry, we map to a enumerated type.
            assert associatedObject instanceof OntologyEntry;
            specialConvertSequenceType( ( OntologyEntry ) associatedObject, gemmaObj );
        } else if ( associationName.equals( "Species" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter, "Taxon" );
        } else if ( associationName.equals( "SeqFeatures" ) ) {
            ; // list of Sequence features, we ignore
        } else if ( associationName.equals( "OntologyEntries" ) ) {
            ; // list of generic ontology entries, we ignore.
        } else {
            log.debug( "Unknown or unsupported type " + associationName );
        }

    }

    /**
     * @param mageObj
     * @return BioMaterial
     * @see convertBioMaterial
     */
    public edu.columbia.gemma.expression.biomaterial.BioMaterial convertBioSource( BioSource mageObj ) {
        if ( mageObj == null ) return null;

        edu.columbia.gemma.expression.biomaterial.BioMaterial result = convertBioMaterial( mageObj );

        // mageObj.getSourceContact();
        return result;
    }

    /**
     * The only extra association a BioSource has is a contact; we ignore this, so this call is passed up to
     * convertBioMaterialAssociations
     * 
     * @param mageObj
     * @param gemmaObj
     * @param getter
     * @see convertBioMaterialAssociations
     */
    public void convertBioSourceAssociations( BioSource mageObj, BioMaterial gemmaObj, Method getter ) {
        convertBioMaterialAssociations( mageObj, gemmaObj, getter );
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.description.OntologyEntry convertCategory( OntologyEntry mageObj ) {
        return convertOntologyEntry( mageObj );
    }

    /**
     * A no-op, since we don't explicitly support channels at the moment.
     * 
     * @param mageObj
     * @return
     */
    public Object convertChannel( Channel mageObj ) {
        if ( mageObj == null ) return null;
        return null; // No-op
    }

    /**
     * NO-op, we deal with it elsewhere specially.
     * 
     * @param mageObj
     * @return
     */
    public Object convertCompositeGroup( CompositeGroup mageObj ) {
        if ( mageObj == null ) return null;
        return null;
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.expression.designElement.CompositeSequence convertCompositeSequence(
            org.biomage.DesignElement.CompositeSequence mageObj ) {

        if ( mageObj == null ) return null;

        CompositeSequence result = CompositeSequence.Factory.newInstance();

        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertCompositeSequenceAssociations( org.biomage.DesignElement.CompositeSequence mageObj,
            edu.columbia.gemma.expression.designElement.CompositeSequence gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );

        if ( associatedObject == null ) return;

        if ( associationName.equals( "BiologicalCharacteristics" ) ) {
            if ( ( ( List ) associatedObject ).size() > 1 )
                log.warn( "*** More than one BiologicalCharacteristic for a MAGE CompositeSequence!" );
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY, "BiologicalCharacteristic" );
        } else if ( associationName.equals( "CompositeCompositeMaps" ) ) {
            ; // we don't support.
        } else if ( associationName.equals( "ReporterCompositeMaps" ) ) {
            // special case. This is complicated, because the mage model has compositeSequence ->
            // reportercompositemap(s) -> reporterposition(s) -> reporter(1)
            gemmaObj.setReporters( specialConvertReporterCompositeMaps( ( List ) associatedObject ) );
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public Compound convertCompound( org.biomage.BioMaterial.Compound mageObj ) {
        if ( mageObj == null ) return null;
        Compound result = Compound.Factory.newInstance();
        result.setIsSolvent( mageObj.getIsSolvent() == null ? Boolean.FALSE : mageObj.getIsSolvent() );
        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertCompoundAssociations( org.biomage.BioMaterial.Compound mageObj, Compound gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );

        if ( associatedObject == null ) return;

        if ( associationName.equals( "CompoundIndices" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY );
        } else if ( associationName.equals( "ExternalLIMS" ) ) {
            // noop
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @param gemmaObj
     */
    public edu.columbia.gemma.expression.biomaterial.CompoundMeasurement convertCompoundMeasurement(
            CompoundMeasurement mageObj ) {
        edu.columbia.gemma.expression.biomaterial.CompoundMeasurement result = edu.columbia.gemma.expression.biomaterial.CompoundMeasurement.Factory
                .newInstance();
        convertAssociations( result, mageObj );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertCompoundMeasurementAssociations( CompoundMeasurement mageObj,
            edu.columbia.gemma.expression.biomaterial.CompoundMeasurement gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );

        if ( associatedObject == null ) return;

        if ( associationName.equals( "Compound" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else if ( associationName.equals( "Measurement" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.auditAndSecurity.Contact convertContact( Contact mageObj ) {
        if ( mageObj == null ) return null;
        return null;
    }

    /**
     * @param mageObj
     * @return
     */
    public ExternalDatabase convertDatabase( Database mageObj ) {
        if ( mageObj == null ) {
            return null;
        }

        if ( mgedOntologyAliases.contains( mageObj.getName() ) ) {
            return this.getMAGEOntologyDatabaseObject();
        }

        ExternalDatabase result = ExternalDatabase.Factory.newInstance();
        result.setWebUri( mageObj.getURI() );
        // we don't use version.
        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertDatabaseAssociations( Database mageObj,
            edu.columbia.gemma.common.description.ExternalDatabase gemmaObj, Method getter ) {

        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );

        if ( associatedObject == null ) return;

        if ( associationName.equals( "Contacts" ) )
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY, "Contact" );
        else
            log.debug( "Unsupported or unknown association: " + associationName );
    }

    /**
     * @param mageObj
     * @return
     */
    public DatabaseEntry convertDatabaseEntry( org.biomage.Description.DatabaseEntry mageObj ) {
        if ( mageObj == null ) return null;
        DatabaseEntry result = DatabaseEntry.Factory.newInstance();

        result.setAccession( mageObj.getAccession() );
        result.setAccessionVersion( mageObj.getAccessionVersion() );
        result.setUri( mageObj.getURI() );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertDatabaseEntryAssociations( org.biomage.Description.DatabaseEntry mageObj,
            DatabaseEntry gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "Database" ) )
            simpleFillIn( associatedObject, gemmaObj, getter );
        else if ( associationName.equals( "Type" ) )
            ; // we ain't got that.
        else
            log.debug( "Unsupported or unknown association: " + associationName );
    }

    /**
     * Values here are based on the MGED Ontology allowable values.
     * 
     * @param mageObj
     * @return
     */
    public PrimitiveType convertDataType( OntologyEntry mageObj ) {
        if ( mageObj == null ) return null;

        String val = mageObj.getValue();

        if ( val.equals( "boolean" ) ) {
            return PrimitiveType.BOOLEAN;
        } else if ( val.equalsIgnoreCase( "char" ) ) {
            return PrimitiveType.CHAR;
        } else if ( val.equalsIgnoreCase( "character" ) ) {
            return PrimitiveType.CHAR;
        } else if ( val.equalsIgnoreCase( "float" ) ) {
            return PrimitiveType.DOUBLE;
        } else if ( val.equalsIgnoreCase( "double" ) ) {
            return PrimitiveType.DOUBLE;
        } else if ( val.equalsIgnoreCase( "int" ) ) {
            return PrimitiveType.INT;
        } else if ( val.equalsIgnoreCase( "integer" ) ) {
            return PrimitiveType.INT;
        } else if ( val.equalsIgnoreCase( "long" ) ) {
            return PrimitiveType.LONG;
        } else if ( val.equalsIgnoreCase( "string" ) ) {
            return PrimitiveType.STRING;
        } else if ( val.equalsIgnoreCase( "string_datatype" ) ) {
            return PrimitiveType.STRING;
        } else if ( val.equalsIgnoreCase( "list_of_floats" ) ) {
            return PrimitiveType.DOUBLEARRAY;
        } else if ( val.equalsIgnoreCase( "list_of_integers" ) ) {
            return PrimitiveType.INTARRAY;
        } else if ( val.equalsIgnoreCase( "list_of_strings" ) ) {
            return PrimitiveType.STRINGARRAY;
        } else if ( val.equalsIgnoreCase( "positive_float" ) ) {
            return PrimitiveType.DOUBLE;
        } else if ( val.equalsIgnoreCase( "negative_float" ) ) {
            return PrimitiveType.DOUBLE;
        } else if ( val.equalsIgnoreCase( "positive_integer" ) ) {
            return PrimitiveType.INT;
        } else if ( val.equalsIgnoreCase( "negative_integer" ) ) {
            return PrimitiveType.INT;
        } else if ( val.equalsIgnoreCase( "list_of_booleans" ) ) {
            return PrimitiveType.BOOLEANARRAY;
        } else if ( val.equalsIgnoreCase( "nonnegative_float" ) ) {
            return PrimitiveType.DOUBLE;
        } else if ( val.equalsIgnoreCase( "nonnegative_integer" ) ) {
            return PrimitiveType.INT;
        } else if ( val.equalsIgnoreCase( "nonnegative_float" ) ) {
            return PrimitiveType.DOUBLE;
        } else if ( val.equalsIgnoreCase( "nonpositive_integer" ) ) {
            return PrimitiveType.INT;
        } else if ( val.equalsIgnoreCase( "nonnegative_float" ) ) {
            return PrimitiveType.DOUBLE;
        } else {
            log.error( "Unrecognized DataType " + val );
            return null;
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public Measurement convertDefaultValue( org.biomage.Measurement.Measurement mageObj ) {
        return convertMeasurement( mageObj );
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.expression.bioAssay.BioAssay convertDerivedBioAssay( DerivedBioAssay mageObj ) {
        if ( mageObj == null ) return null;
        edu.columbia.gemma.expression.bioAssay.BioAssay result = convertBioAssay( mageObj );

        // Attempt to get the array design for this bioassay. ( so far this hasn't worked - the information isn't linked
        // to the derived bioassay!)
        List map = mageObj.getDerivedBioAssayMap();
        if ( map.size() > 0 ) {
            BioAssayMap dbap = ( BioAssayMap ) map.get( 0 );

            List bioAssays = dbap.getSourceBioAssays();
            // if ( bioAssays.size() == 0 ) log.debug( "DerivedBioAssayMap, but no sourcebioAssays" );
            if ( bioAssays.size() > 1 ) log.warn( "More than one sourcebioAssay for a MeasuredBioAssay!" );
            for ( Iterator iter = bioAssays.iterator(); iter.hasNext(); ) {
                BioAssay bioAssay = ( BioAssay ) iter.next();
                if ( bioAssay instanceof MeasuredBioAssay ) {

                    specialConvertBioAssayCreation( ( ( MeasuredBioAssay ) bioAssay ).getFeatureExtraction()
                            .getPhysicalBioAssaySource(), result );
                } else {
                    log.error( "What kind of bioassay is associated?: " + bioAssay.getClass().getName() );
                }
            }
        }
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertDerivedBioAssayAssociations( DerivedBioAssay mageObj,
            edu.columbia.gemma.expression.bioAssay.BioAssay gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;

        if ( associationName.equals( "DerivedBioAssayMap" ) ) {
            // if ( ( ( List ) associatedObject ).size() > 0 ) log.warn( "Missing out on DerivedBioAssayMap" );
        } else if ( associationName.equals( "DerivedBioAssayData" ) ) {
            specialConvertBioAssayBioAssayDataAssociations( ( List ) associatedObject, gemmaObj );
        } else if ( associationName.equals( "Type" ) ) {
            // simpleFillIn( associatedObject, gemmaObj, getter, "Type" );
        } else if ( associationName.equals( "Channels" ) || associationName.equals( "BioAssayFactorValues" ) ) {
            ; // nothing.
        } else {
            log.warn( "Unsupported or unknown association, or belongs to superclass: " + associationName );
        }
    }

    /**
     * An Affymetrix CHP file is considered DerivedBioAssayData.
     * 
     * @param mageObj
     * @return
     * @see convertBioAssayData
     */
    public LocalFile convertDerivedBioAssayData( DerivedBioAssayData mageObj ) {
        return convertBioAssayData( mageObj );
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.quantitationtype.QuantitationType convertDerivedSignal( DerivedSignal mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertDerivedSignalAssociations( DerivedSignal mageObj,
            edu.columbia.gemma.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        convertQuantitationTypeAssociations( mageObj, gemmaObj, getter );
    }

    /**
     * Convert a MAGE Describable to a Gemma domain object. We only allow a single description, so we take the first
     * one. The association to Security and Audit are not filled in here.
     * 
     * @param mageObj
     * @param gemmaObj
     */
    public void convertDescribable( Describable mageObj, edu.columbia.gemma.common.Describable gemmaObj ) {

        if ( mageObj == null ) return;
        if ( gemmaObj == null ) throw new IllegalArgumentException( "Must pass in a valid object" );

        // This is a bit cheesy, we just concatenate the descriptions together.
        StringBuilder descBuf = new StringBuilder();
        List<Description> descriptions = mageObj.getDescriptions();
        for ( Description description : descriptions ) {
            descBuf.append( description.getText() );
            descBuf.append( " " );
            List<OntologyEntry> annotations = description.getAnnotations();
            for ( OntologyEntry element : annotations ) {
                edu.columbia.gemma.common.description.OntologyEntry ontologyEntry = convertOntologyEntry( element );
                log.debug( "Got association for describable: " + ontologyEntry.getValue() );
                // gemmaObj.addAnnotation( ontologyEntry );
            }

        }
        gemmaObj.setDescription( descBuf.toString() );

        convertExtendable( mageObj, gemmaObj );
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.quantitationtype.QuantitationType convertError(
            org.biomage.QuantitationType.Error mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertErrorAssociations( org.biomage.QuantitationType.Error mageObj,
            edu.columbia.gemma.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        convertQuantitationTypeAssociations( mageObj, gemmaObj, getter );
    }

    /**
     * @param mageObj
     * @return
     */
    public ExpressionExperiment convertExperiment( Experiment mageObj ) {
        if ( mageObj == null ) return null;
        ExpressionExperiment result = ExpressionExperiment.Factory.newInstance();
        result.setSource( "Imported from MAGE-ML" );

        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @return
     */
    public ExperimentalFactor convertExperimentalFactor( org.biomage.Experiment.ExperimentalFactor mageObj ) {
        if ( mageObj == null ) return null;
        ExperimentalFactor result = ExperimentalFactor.Factory.newInstance();
        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertExperimentalFactorAssociations( org.biomage.Experiment.ExperimentalFactor mageObj,
            ExperimentalFactor gemmaObj, Method getter ) {

        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;

        if ( associationName.equals( "Category" ) )
            simpleFillIn( associatedObject, gemmaObj, getter, "Category" );
        else if ( associationName.equals( "Annotations" ) )
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        else if ( associationName.equals( "FactorValues" ) )
            // Note that these should be the same factorvalues as referred to by the bioassays.
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        else
            log.warn( "Unsupported or unknown association: " + associationName );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertExperimentAssociations( Experiment mageObj, ExpressionExperiment gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );

        if ( associatedObject == null ) return;

        if ( associationName.equals( "AnalysisResults" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        } else if ( associationName.equals( "BioAssays" ) ) {
            assert associatedObject instanceof List;
            if ( ( ( List ) associatedObject ).size() > 0 ) log.debug( "Converting Experiment-->BioAssays" );
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        } else if ( associationName.equals( "Providers" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY, "Provider" );
        } else if ( associationName.equals( "BioAssayData" ) ) {
            // we get this directly through the bioassay->bioassay data association.
            // assert associatedObject instanceof List;
            // if ( gemmaObj.getBioAssays() == null ) {
            // // need bioAssays first!
            // log.error( "Need bioassays first before can convert bioassaydata!" );
            // }
            // specialConvertExperimentBioAssayDataAssociations( ( List ) associatedObject, gemmaObj );
        } else if ( associationName.equals( "ExperimentDesigns" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "ExperimentalDesigns" );
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public ExperimentalDesign convertExperimentDesign( ExperimentDesign mageObj ) {
        if ( mageObj == null ) return null;
        ExperimentalDesign result = ExperimentalDesign.Factory.newInstance();
        convertDescribable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     */
    public void convertExperimentDesignAssociations( ExperimentDesign mageObj, ExperimentalDesign gemmaObj,
            Method getter ) {

        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );

        if ( associatedObject == null ) return;

        if ( associationName.equals( "ExperimentalFactors" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        } else if ( associationName.equals( "NormalizationDescription" ) ) {
            // not supported as an association
        } else if ( associationName.equals( "QualityControlDescription" ) ) {
            // not supported as an association
        } else if ( associationName.equals( "ReplicateDescription" ) ) {
            // not supported as an association
        } else if ( associationName.equals( "TopLevelBioAssays" ) ) {
            assert associatedObject instanceof List;
            // we don't have this in our model --- check
        } else if ( associationName.equals( "Types" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        } else {
            log.warn( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * This is a no-op.
     * 
     * @param mageObj
     * @param gemmaObj
     */
    public void convertExtendable( Extendable mageObj, edu.columbia.gemma.common.Describable gemmaObj ) {
        if ( mageObj == null || gemmaObj == null ) return;
        ; // nothing to do, we aren't using this.
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertExternalDatabaseAssociations( Database mageObj, ExternalDatabase gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );

        if ( associatedObject == null ) return;

        if ( associationName.equals( "Contacts" ) )
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY, "Contact" );
        else
            log.debug( "Unsupported or unknown association: " + associationName );

    }

    /**
     * @param mageObj
     * @return
     */
    public FactorValue convertFactorValue( org.biomage.Experiment.FactorValue mageObj ) {
        if ( mageObj == null ) return null;

        FactorValue result = FactorValue.Factory.newInstance();
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertFactorValueAssociations( org.biomage.Experiment.FactorValue mageObj, FactorValue gemmaObj,
            Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associationName.equals( "ExperimentalFactor" ) ) {
            // we let the ExperimentalFactor manage this association.
        } else if ( associationName.equals( "Measurement" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter, "Measurement" );
        } else if ( associationName.equals( "Value" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter, "OntologyEntry" );
        } else {
            log.warn( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * Unlike in MAGE, feature-reporter map is not an entity. (The mage name is also confusing: it is an assocation
     * between a reporter and the features that make it up). Therefore, this is a no-op.
     * 
     * @param mageObj
     * @return
     */
    public Object convertFeatureReporterMap( FeatureReporterMap mageObj ) {
        if ( mageObj == null ) return null;
        return null;
    }

    // /**
    // * @param list
    // * @param gemmaObj
    // */
    // private void specialConvertExperimentBioAssayDataAssociations( List<BioAssayData> bioAssayData,
    // ExpressionExperiment gemmaObj ) {
    // for ( BioAssayData data : bioAssayData ) {
    // LocalFile file = convertBioAssayData( data );
    // // need to attachi this to
    // }
    // }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.protocol.Hardware convertHardware( org.biomage.Protocol.Hardware mageObj ) {
        if ( mageObj == null ) return null;
        edu.columbia.gemma.common.protocol.Hardware result = edu.columbia.gemma.common.protocol.Hardware.Factory
                .newInstance();
        result.setModel( mageObj.getModel() );
        result.setMake( mageObj.getMake() );
        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );

        return result;
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.protocol.HardwareApplication convertHardwareApplication(
            org.biomage.Protocol.HardwareApplication mageObj ) {
        edu.columbia.gemma.common.protocol.HardwareApplication result = edu.columbia.gemma.common.protocol.HardwareApplication.Factory
                .newInstance();

        result.setSerialNumber( mageObj.getSerialNumber() );
        convertDescribable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertHardwareApplicationAssociations( org.biomage.Protocol.HardwareApplication mageObj,
            edu.columbia.gemma.common.protocol.HardwareApplication gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "Hardware" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }

    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertHardwareAssociations( Hardware mageObj, edu.columbia.gemma.common.protocol.Hardware gemmaObj,
            Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "Hardware" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else if ( associationName.equals( "HardwareManufacturers" ) ) {
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        } else if ( associationName.equals( "Softwares" ) ) {
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        } else if ( associationName.equals( "Type" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * Copy attributes from a MAGE identifiable to a Gemma identifiable.
     * 
     * @param mageObj
     */
    public void convertIdentifiable( org.biomage.Common.Identifiable mageObj,
            edu.columbia.gemma.common.Describable gemmaObj ) {

        if ( mageObj == null ) return;
        if ( gemmaObj == null ) throw new IllegalArgumentException( "Must pass in a valid object" );

        // we do this here because Mage names go with Identifiable, not
        // describable.
        gemmaObj.setName( mageObj.getName() );

        /* Use the identifier as a name if there isn't a name. */
        if ( gemmaObj.getName() == null ) {
            gemmaObj.setName( mageObj.getIdentifier() );
        }

        convertDescribable( mageObj, gemmaObj );
    }

    /**
     * @param kindCV
     * @return
     */
    public MeasurementKind convertKindCV( KindCV kindCV ) {
        if ( kindCV == null ) return null;

        if ( kindCV.getValue() == kindCV.concentration ) {
            return MeasurementKind.CONCENTRATION;
        } else if ( kindCV.getValue() == kindCV.distance ) {
            return MeasurementKind.DISTANCE;
        } else if ( kindCV.getValue() == kindCV.mass ) {
            return MeasurementKind.MASS;
        } else if ( kindCV.getValue() == kindCV.quantity ) {
            return MeasurementKind.QUANTITY;
        } else if ( kindCV.getValue() == kindCV.temperature ) {
            return MeasurementKind.TEMPERATURE;
        } else if ( kindCV.getValue() == kindCV.time ) {
            return MeasurementKind.TIME;
        } else if ( kindCV.getValue() == kindCV.volume ) {
            return MeasurementKind.VOLUME;
        } else if ( kindCV.getValue() == kindCV.other ) {
            return MeasurementKind.OTHER;
        }

        log.error( "Unknown measurement kind: " + kindCV.getName() );
        return null;
    }

    /**
     * @param mageObj
     * @return
     */
    public BioMaterial convertLabeledExtract( org.biomage.BioMaterial.LabeledExtract mageObj ) {
        if ( mageObj == null ) return null;
        return convertBioMaterial( mageObj );
    }

    /**
     * A labeled extract has no associations that we keep track of, so this delegates to convertBioMaterialAssociations.
     * 
     * @param mageObj
     * @param gemmaObj
     * @param getter
     * @see convertBioMaterialAssociations
     */
    public void convertLabeledExtractAssociations( LabeledExtract mageObj, BioMaterial gemmaObj, Method getter ) {
        convertBioMaterialAssociations( mageObj, gemmaObj, getter );
    }

    /**
     * @param mageObj
     * @return an OntologyEntry correspondingi the the MaterialType.
     * @see convertOntologyEntry
     */
    public edu.columbia.gemma.common.description.OntologyEntry convertMaterialType( OntologyEntry mageObj ) {
        return convertOntologyEntry( mageObj );
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.expression.bioAssay.BioAssay convertMeasuredBioAssay( MeasuredBioAssay mageObj ) {
        if ( mageObj == null ) return null;
        edu.columbia.gemma.expression.bioAssay.BioAssay result = convertBioAssay( mageObj );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertMeasuredBioAssayAssociations( MeasuredBioAssay mageObj,
            edu.columbia.gemma.expression.bioAssay.BioAssay gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;

        if ( associationName.equals( "FeatureExtraction" ) ) {
            specialConvertFeatureExtraction( ( FeatureExtraction ) associatedObject, gemmaObj );
        } else if ( associationName.equals( "MeasuredBioAssayData" ) ) {
            specialConvertBioAssayBioAssayDataAssociations( ( List ) associatedObject, gemmaObj );
        } else {
            log.debug( "Unsupported or unknown association, or belongs to superclass: " + associationName );
        }       
    }

    /**
     * The Affymetrix CEL files are usually considered MeasuredBioAssayData.
     * 
     * @param mageObj
     * @return
     * @see convertBioAssayData
     */
    public LocalFile convertMeasuredBioAssayData( MeasuredBioAssayData mageObj ) {
        return convertBioAssayData( mageObj );
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.quantitationtype.QuantitationType convertMeasuredSignal( MeasuredSignal mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertMeasuredSignalAssociations( MeasuredSignal mageObj,
            edu.columbia.gemma.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        convertQuantitationTypeAssociations( mageObj, gemmaObj, getter );
    }

    /**
     * @param mageObj
     * @return
     */
    public Measurement convertMeasurement( org.biomage.Measurement.Measurement mageObj ) {
        if ( mageObj == null ) return null;
        Measurement result = Measurement.Factory.newInstance();

        result.setOtherKind( mageObj.getOtherKind() );
        result.setKindCV( convertKindCV( mageObj.getKindCV() ) );

        if ( mageObj.getValue() != null ) result.setValue( mageObj.getValue().toString() );

        result.setType( convertMeasurementType( mageObj.getType() ) );

        result.setRepresentation( PrimitiveType.STRING ); // FIXME This is somewhat silly as the QuantitationType has
        // the primitive type.
        return result;
    }

    /**
     * @param type
     * @return
     */
    public MeasurementType convertMeasurementType( Type type ) {
        if ( type == null ) return null;

        if ( type.getValue() == type.absolute ) {
            return MeasurementType.ABSOLUTE;
        } else if ( type.getValue() == type.change ) {
            return MeasurementType.CHANGE;
        }

        log.error( "Unknown measurement type: " + type.getName() );
        return null;
    }

    /**
     * OntologyEntry is a subclass of DatabaseEntry in Gemma, but not in MAGE. Instead, a MAGE object has an
     * OntologyReference to a databaseEntry.
     * <p>
     * In Gemma, where possible we convert MGED ontology objects into the corresponding Gemma object.
     * 
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.description.OntologyEntry convertOntologyEntry( OntologyEntry mageObj ) {
        if ( mageObj == null ) return null;

        edu.columbia.gemma.common.description.OntologyEntry result = edu.columbia.gemma.common.description.OntologyEntry.Factory
                .newInstance();
        result.setAccession( mageObj.getOntologyReference() == null ? null : mageObj.getOntologyReference()
                .getAccession() );
        result.setAccession( StringUtils.replace( result.getAccession(), MGED_ONTOLOGY_URL, "" ) );

        result.setCategory( mageObj.getCategory() );

        result.setDescription( mageObj.getDescription() );
        result.setValue( mageObj.getValue() );
        convertAssociations( mageObj, result );
        if ( result.getExternalDatabase() == null ) { // Maybe its in MO (often the case)
            if ( mgedOntologyHelper.classExists( StringUtils.capitalize( mageObj.getCategory() ) ) ) {
                result.setExternalDatabase( this.getMAGEOntologyDatabaseObject() );
                log.debug( "Automatically identified MO as database for " + result );
            } else {
                log.warn( "Using 'unknown' for source of OntologyEntry " + result + " (Converted from MAGE "
                        + mageObj.getCategory() + " " + mageObj.getValue() + ")" );
                result.setExternalDatabase( this.getUnknownDatabaseObject() );
            }
        }
        return result;
    }

    /**
     * @return
     */
    private ExternalDatabase getUnknownDatabaseObject() {
        ExternalDatabase unknownDatabase = ExternalDatabase.Factory.newInstance();
        unknownDatabase.setName( UNKNOWN_DATABASE_IDENTIFIER );
        return unknownDatabase;
    }

    /**
     * Convenience method to access a ready-made ExternalDatabase representing the MGED Ontology.
     * 
     * @return
     */
    private ExternalDatabase getMAGEOntologyDatabaseObject() {
        if ( this.mgedOntology != null ) {
            return mgedOntology;
        }
        this.mgedOntology = ExternalDatabase.Factory.newInstance();
        mgedOntology.setName( MGED_DATABASE_IDENTIFIER );
        mgedOntology.setType( DatabaseType.ONTOLOGY );
        mgedOntology.setWebUri( MGED_ONTOLOGY_URL );
        return mgedOntology;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertOntologyEntryAssociations( OntologyEntry mageObj,
            edu.columbia.gemma.common.description.OntologyEntry gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "Associations" ) ) {
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL );
            // specialConvertOntologyEntryAssociations( ( List ) associatedObject, gemmaObj );
        } else if ( associationName.equals( "OntologyReference" ) ) {
            assert associatedObject instanceof org.biomage.Description.DatabaseEntry;
            specialConvertOntologyEntryDatabaseEntry( ( org.biomage.Description.DatabaseEntry ) associatedObject,
                    gemmaObj );
        } else {
            log.warn( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return DatabaseEntry
     * @see convertDatabaseEntry
     */
    public DatabaseEntry convertOntologyReference( org.biomage.Description.DatabaseEntry mageObj ) {
        return this.convertDatabaseEntry( mageObj );
    }

    /**
     * Not supported.
     * 
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.auditAndSecurity.Organization convertOrganization( Organization mageObj ) {
        if ( mageObj == null ) return null;
        return null;
    }

    /**
     * @param mageObj
     * @return
     */
    public Parameter convertParameter( org.biomage.Protocol.Parameter mageObj ) {
        Parameter result = Parameter.Factory.newInstance();
        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertParameterAssociations( org.biomage.Protocol.Parameter mageObj, Parameter gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "DataType" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else if ( associationName.equals( "DefaultValue" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * Not supported.
     * 
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.auditAndSecurity.Person convertPerson( Person mageObj ) {
        if ( mageObj == null ) return null;
        return null;
    }

    /**
     * @param mageObj
     * @return
     * @see convertArrayDesign
     */
    public Object convertPhysicalArrayDesign( org.biomage.ArrayDesign.PhysicalArrayDesign mageObj ) {
        if ( mageObj == null ) return null;
        edu.columbia.gemma.expression.arrayDesign.ArrayDesign result = convertArrayDesign( mageObj );
        convertAssociations( mageObj, result );
        return result;

    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertPhysicalArrayDesignAssociations( PhysicalArrayDesign mageObj,
            edu.columbia.gemma.expression.arrayDesign.ArrayDesign gemmaObj, Method getter ) {
        convertArrayDesignAssociations( mageObj, gemmaObj, getter );
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "SurfaceType" ) ) {
            ; // we don't support this
        } else if ( associationName.equals( "ZoneGroups" ) ) {
            assert associatedObject instanceof List;
            // we don't support this.
        } else if ( associationName.equals( "ReporterGroups" ) || associationName.equals( "FeatureGroups" )
                || associationName.equals( "DesignProviders" ) || associationName.equals( "CompositeGroups" )
                || associationName.equals( "ProtocolApplications" ) ) {
            ; // nothing, superclass.
        } else {
            log.warn( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * We don't use this. A No-op. PhysicalBioAssays are converted via the
     * MeasuredBioAssay->FeatureExtraction->PhyscialBioAssay associations.
     * 
     * @param mageObj
     * @return
     * @see convertMeasuredBioAssay
     */
    public edu.columbia.gemma.expression.bioAssay.BioAssay convertPhysicalBioAssay( PhysicalBioAssay mageObj ) {
        return null;
    }

    /**
     * This is currently probably not used.
     * 
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertPhysicalBioAssayAssociations( PhysicalBioAssay mageObj,
            edu.columbia.gemma.expression.bioAssay.BioAssay gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );

        if ( associatedObject == null ) return;

        if ( associationName.equals( "BioAssayCreation" ) ) {
            specialConvertBioAssayCreation( mageObj, gemmaObj );

        } else if ( associationName.equals( "BioAssayTreatments" ) ) {
            assert associatedObject instanceof List;
            // this is not supported in our data model currently.
        } else if ( associationName.equals( "PhysicalBioAssayData" ) ) {
            assert associatedObject instanceof List;
            // these are Image objects - not supported
        } else {
            log.debug( "Unsupported or unknown association, or belongs to superclass: " + associationName );
        }
    }

    /**
     * @param associatedObject
     * @return
     */
    public PolymerType convertPolymerType( OntologyEntry mageObj ) {
        if ( mageObj.getValue().equalsIgnoreCase( "DNA" ) ) {
            return PolymerType.DNA;
        } else if ( mageObj.getValue().equalsIgnoreCase( "protein" ) ) {
            return PolymerType.PROTEIN;
        } else if ( mageObj.getValue().equalsIgnoreCase( "RNA" ) ) {
            return PolymerType.RNA;
        }
        log.error( "Unsupported polymer type:" + mageObj.getValue() );
        return null;
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.quantitationtype.QuantitationType convertPresentAbsent( PresentAbsent mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     * @see convertQuantitationTypeAssociations
     */
    public void convertPresentAbsentAssociations( org.biomage.QuantitationType.PresentAbsent mageObj,
            edu.columbia.gemma.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        convertQuantitationTypeAssociations( mageObj, gemmaObj, getter );
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.protocol.Protocol convertProtocol( Protocol mageObj ) {
        if ( mageObj == null ) return null;
        edu.columbia.gemma.common.protocol.Protocol result = edu.columbia.gemma.common.protocol.Protocol.Factory
                .newInstance();

        result.setURI( mageObj.getURI() );
        convertIdentifiable( mageObj, result );

        // we just use the name and description to hold the text and title.
        result.setDescription( mageObj.getText() );
        result.setName( mageObj.getTitle() );

        if ( result.getName() == null ) {
            result.setName( mageObj.getIdentifier() );
        }

        convertAssociations( mageObj, result );

        return result;
    }

    public ProtocolApplication convertProtocolApplication( org.biomage.Protocol.ProtocolApplication mageObj ) {
        ProtocolApplication result = ProtocolApplication.Factory.newInstance();

        if ( mageObj.getActivityDate() != null ) {
            String dateString = mageObj.getActivityDate();
            Date date = convertDateString( dateString );
            result.setActivityDate( date );
        }
        convertDescribable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param dateString
     * @return
     */
    public Date convertDateString( String dateString ) {
        Date result = null;
        try {
            result = ( new SimpleDateFormat() ).parse( dateString );

        } catch ( ParseException e ) {
            if ( dateString.equals( "n\\a" ) || dateString.equals( "n/a" ) ) {
                return null;
            }
            log.debug( "Trying alternative date formats" );
            DateFormat formatter = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
            try {
                result = formatter.parse( dateString );
            } catch ( ParseException e1 ) {
                log.error( "Could not parse date from  '" + dateString + "'" );
            }

            log.error( "Could not parse date from  '" + dateString + "'" );
            return null;
        }
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertProtocolApplicationAssociations( org.biomage.Protocol.ProtocolApplication mageObj,
            ProtocolApplication gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "SoftwareApplications" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        } else if ( associationName.equals( "HardwareApplications" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        } else if ( associationName.equals( "Protocol" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else if ( associationName.equals( "Performers" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertProtocolAssociations( Protocol mageObj, edu.columbia.gemma.common.protocol.Protocol gemmaObj,
            Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;

        if ( associationName.equals( "Hardwares" ) ) {
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        } else if ( associationName.equals( "Softwares" ) ) {
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL, "SoftwareUsed" );
        } else if ( associationName.equals( "Type" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else if ( associationName.equals( "ParameterTypes" ) ) {
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     * @see convertQuantitationType
     */
    public edu.columbia.gemma.common.quantitationtype.QuantitationType convertPValue( PValue mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertPValueAssociations( PValue mageObj,
            edu.columbia.gemma.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        convertQuantitationTypeAssociations( mageObj, gemmaObj, getter );
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.quantitationtype.QuantitationType convertQuantitationType( QuantitationType mageObj ) {

        edu.columbia.gemma.common.quantitationtype.QuantitationType result = edu.columbia.gemma.common.quantitationtype.QuantitationType.Factory
                .newInstance();

        // note that PrimitiveType and Scale are set via associations.
        if ( mageObj instanceof SpecializedQuantitationType ) {
            result.setGeneralType( GeneralType.UNKNOWN );
            result.setType( StandardQuantitationType.OTHER );
        } else if ( mageObj instanceof MeasuredSignal ) {
            result.setGeneralType( GeneralType.QUANTITATIVE );
            result.setType( StandardQuantitationType.MEASUREDSIGNAL );
        } else if ( mageObj instanceof DerivedSignal ) {
            result.setGeneralType( GeneralType.QUANTITATIVE );
            result.setType( StandardQuantitationType.DERIVEDSIGNAL );
        } else if ( mageObj instanceof Ratio ) {
            result.setGeneralType( GeneralType.QUANTITATIVE );
            result.setType( StandardQuantitationType.RATIO );
        } else if ( mageObj instanceof Failed ) {
            result.setGeneralType( GeneralType.CATEGORICAL );
            result.setType( StandardQuantitationType.FAILED );
        } else if ( mageObj instanceof PresentAbsent ) {
            result.setGeneralType( GeneralType.CATEGORICAL );
            result.setType( StandardQuantitationType.PRESENTABSENT );
        } else if ( mageObj instanceof ConfidenceIndicator ) {
            result.setGeneralType( GeneralType.QUANTITATIVE );
            result.setType( StandardQuantitationType.CONFIDENCEINDICATOR );
        } else {
            result.setGeneralType( GeneralType.UNKNOWN );
            result.setType( StandardQuantitationType.OTHER );
        }

        result.setIsBackground( mageObj.getIsBackground().booleanValue() );
        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );

        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertQuantitationTypeAssociations( QuantitationType mageObj,
            edu.columbia.gemma.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "Channel" ) ) {
            // we aren't support this
        } else if ( associationName.equals( "ConfidenceIndicators" ) ) {
            // this is bidirectionally navigable in MAGE - we don't support that.
        } else if ( associationName.equals( "DataType" ) ) {
            gemmaObj.setRepresentation( convertDataType( mageObj.getDataType() ) );
        } else if ( associationName.equals( "Scale" ) ) {
            gemmaObj.setScale( convertScale( mageObj.getScale() ) );
        } else if ( associationName.equals( "QuantitationTypeMaps" ) ) {
            ; // special case - transformations.
        } else if ( associationName.equals( "TargetQuantitationType" ) ) { // from ConfidenceIndicator.
            // this is an association to another QuantitationType: the confidence in it. I think we skip for now.
        } else {
            log.warn( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     * @see convertQuantitationType
     */
    public edu.columbia.gemma.common.quantitationtype.QuantitationType convertRatio( Ratio mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertRatioAssociations( Ratio mageObj,
            edu.columbia.gemma.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        convertQuantitationTypeAssociations( mageObj, gemmaObj, getter );
    }

    /**
     * @param designElement
     * @return
     */
    public DesignElement convertDesignElement( org.biomage.DesignElement.DesignElement designElement ) {
        if ( designElement instanceof org.biomage.DesignElement.Reporter ) {
            return convertReporter( ( org.biomage.DesignElement.Reporter ) designElement );
        } else if ( designElement instanceof org.biomage.DesignElement.CompositeSequence ) {
            return convertCompositeSequence( ( org.biomage.DesignElement.CompositeSequence ) designElement );
        } else if ( designElement instanceof Feature ) {
            return convertFeature( ( Feature ) designElement );
        } else {
            throw new IllegalArgumentException( "Can't convert a " + designElement.getClass().getName() );
        }
    }

    /**
     * @param feature
     * @return
     */
    @SuppressWarnings("unused")
    public DesignElement convertFeature( Feature feature ) {
        // I think we just have to ignore this.
        return null;
    }

    /**
     * @param mageObj
     * @return edu.columbia.gemma.expression.designElement.Reporter
     * @see convertReporterAssociations
     */
    public edu.columbia.gemma.expression.designElement.Reporter convertReporter(
            org.biomage.DesignElement.Reporter mageObj ) {
        if ( mageObj == null ) return null;
        Reporter result = Reporter.Factory.newInstance();
        specialGetReporterFeatureLocations( mageObj, result );
        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertReporterAssociations( org.biomage.DesignElement.Reporter mageObj, Reporter gemmaObj,
            Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "FailTypes" ) ) {
            // we don't support
        } else if ( associationName.equals( "FeatureReporterMaps" ) ) {
            // we don't support
        } else if ( associationName.equals( "ImmobilizedCharacteristics" ) ) {
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_FIRST_ONLY, "ImmobilizedCharacteristic" );
        } else if ( associationName.equals( "WarningType" ) ) {
            specialConvertFeatureReporterMaps( ( List ) associatedObject, gemmaObj );
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * Unlike in MAGE, reporter-composite map is not an entity. (The mage name is also confusing: it is an assocation
     * betwee a composite sequence and the reporters that make it up). Therefore, this is a no-op, we deal with it
     * specially.
     * 
     * @param mageObj
     * @return
     */
    public Object convertReporterCompositeMap( ReporterCompositeMap mageObj ) {
        if ( mageObj == null ) return null;
        return null;
    }

    /**
     * No-op, we deal with specially.
     * 
     * @param mageObj
     * @return
     */
    public Object convertReporterGroup( ReporterGroup mageObj ) {
        if ( mageObj == null ) return null;
        return null;
    }

    /**
     * @param mageObj
     * @return
     */
    public ScaleType convertScale( OntologyEntry mageObj ) {
        if ( mageObj == null ) return null;

        String val = mageObj.getValue();
        if ( val.equalsIgnoreCase( "foldchange" ) ) {
            return ScaleType.FOLDCHANGE;
        } else if ( val.equalsIgnoreCase( "fold_change" ) ) {
            return ScaleType.FOLDCHANGE;
        } else if ( val.equalsIgnoreCase( "linear_scale" ) ) {
            return ScaleType.LINEAR;
        } else if ( val.equalsIgnoreCase( "linear" ) ) {
            return ScaleType.LINEAR;
        } else if ( val.equalsIgnoreCase( "ln" ) ) {
            return ScaleType.LN;
        } else if ( val.equalsIgnoreCase( "log" ) ) {
            return ScaleType.LOGBASEUNKNOWN;
        } else if ( val.equalsIgnoreCase( "percent" ) ) {
            return ScaleType.PERCENT;
        } else if ( val.equalsIgnoreCase( "fraction" ) ) {
            return ScaleType.FRACTION;
        } else if ( val.equalsIgnoreCase( "log10" ) ) {
            return ScaleType.LOG10;
        } else if ( val.equalsIgnoreCase( "log_base_10" ) ) {
            return ScaleType.LOG10;
        } else if ( val.equalsIgnoreCase( "log2" ) ) {
            return ScaleType.LOG2;
        } else if ( val.equalsIgnoreCase( "log_base_2" ) ) {
            return ScaleType.LOG2;
        } else if ( val.equalsIgnoreCase( "other" ) ) {
            return ScaleType.OTHER;
        } else if ( val.equalsIgnoreCase( "unscaled" ) ) {
            return ScaleType.UNSCALED;
        }
        log.error( "Unrecognized Scale " + val );
        return null;
    }

    /**
     * @param mageObj
     * @return
     */
    public Software convertSoftware( org.biomage.Protocol.Software mageObj ) {
        if ( mageObj == null ) return null;
        Software result = Software.Factory.newInstance();
        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );

        return result;
    }

    public edu.columbia.gemma.common.protocol.SoftwareApplication convertSoftwareApplication(
            org.biomage.Protocol.SoftwareApplication mageObj ) {
        edu.columbia.gemma.common.protocol.SoftwareApplication result = edu.columbia.gemma.common.protocol.SoftwareApplication.Factory
                .newInstance();

        result.setReleaseDate( mageObj.getReleaseDate() );
        result.setVersion( mageObj.getVersion() );
        convertDescribable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    public void convertSoftwareApplicationAssociations( org.biomage.Protocol.SoftwareApplication mageObj,
            edu.columbia.gemma.common.protocol.SoftwareApplication gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "Software" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }

    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertSoftwareAssociations( org.biomage.Protocol.Software mageObj, Software gemmaObj, Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "Hardware" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else if ( associationName.equals( "SoftwareManufacturers" ) ) {
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        } else if ( associationName.equals( "Softwares" ) ) {
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        } else if ( associationName.equals( "Type" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     * @see convertQuantitationType
     */
    public edu.columbia.gemma.common.quantitationtype.QuantitationType convertSpecializedQuantitationType(
            SpecializedQuantitationType mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertSpecializedQuantitationTypeAssociations( SpecializedQuantitationType mageObj,
            edu.columbia.gemma.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        convertQuantitationTypeAssociations( mageObj, gemmaObj, getter );
    }

    /**
     * @param mageObj
     * @return
     * @see convertQuantitationType
     */
    public edu.columbia.gemma.common.quantitationtype.QuantitationType convertStandardQuantitationType(
            org.biomage.QuantitationType.StandardQuantitationType mageObj ) {
        return convertQuantitationType( mageObj );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertStandardQuantitationTypeAssociations(
            org.biomage.QuantitationType.StandardQuantitationType mageObj,
            edu.columbia.gemma.common.quantitationtype.QuantitationType gemmaObj, Method getter ) {
        convertQuantitationTypeAssociations( mageObj, gemmaObj, getter );
    }

    /**
     * @param mageObj
     * @return
     */
    public Treatment convertTreatment( org.biomage.BioMaterial.Treatment mageObj ) {
        if ( mageObj == null ) return null;
        Treatment result = Treatment.Factory.newInstance();
        Integer order = mageObj.getOrder();
        if ( order != null ) {
            result.setOrderApplied( order.intValue() );
        } else {
            result.setOrderApplied( 1 );
        }
        convertIdentifiable( mageObj, result );
        convertAssociations( mageObj, result );
        return result;
    }

    /**
     * @param mageObj
     * @param gemmaObj
     * @param getter
     */
    public void convertTreatmentAssociations( org.biomage.BioMaterial.Treatment mageObj, Treatment gemmaObj,
            Method getter ) {
        Object associatedObject = intializeConversion( mageObj, getter );
        String associationName = getterToPropertyName( getter );
        if ( associatedObject == null ) return;
        if ( associationName.equals( "Action" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else if ( associationName.equals( "ActionMeasurement" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else if ( associationName.equals( "CompoundMeasurement" ) ) {
            simpleFillIn( associatedObject, gemmaObj, getter );
        } else if ( associationName.equals( "ProtocolApplications" ) ) {
            assert associatedObject instanceof List;
            simpleFillIn( ( List ) associatedObject, gemmaObj, getter, CONVERT_ALL );
        } else {
            log.debug( "Unsupported or unknown association: " + associationName );
        }
    }

    /**
     * @param mageObj
     * @return
     */
    public edu.columbia.gemma.common.description.OntologyEntry convertType( OntologyEntry mageObj ) {
        return convertOntologyEntry( mageObj );
    }

    /**
     * @param unit
     * @return
     */
    public edu.columbia.gemma.common.measurement.Unit convertUnit( Unit unit ) {
        if ( unit == null ) return null;

        edu.columbia.gemma.common.measurement.Unit result = edu.columbia.gemma.common.measurement.Unit.Factory
                .newInstance();
        result.setUnitNameCV( unit.getUnitName() );
        return result;
    }

    /**
     * @param mageObj
     * @return
     * @see convertOntologyEntry
     */
    public edu.columbia.gemma.common.description.OntologyEntry convertValue( OntologyEntry mageObj ) {
        return convertOntologyEntry( mageObj );
    }

    /**
     * Special case: Convert a ReporterCompositeMaps (list) to a Collection of Reporters.
     * 
     * @param reporterCompositeMaps
     * @return Collection of Gemma Reporters.
     */
    public void specialConvertFeatureReporterMaps( List featureReporterMaps, Reporter rep ) {

        if ( featureReporterMaps.size() > 1 ) log.warn( "**** More than one FeatureReporterMap for a Reporter!" );

        for ( Iterator iter = featureReporterMaps.iterator(); iter.hasNext(); ) {
            FeatureReporterMap rcp = ( FeatureReporterMap ) iter.next();
            List rcpps = rcp.getFeatureInformationSources();
            for ( Iterator iterator = rcpps.iterator(); iterator.hasNext(); ) {
                log.debug( "Found feature information for reporter: " + rep.getName() );
                FeatureInformation rps = ( FeatureInformation ) iterator.next();
                org.biomage.DesignElement.Feature repr = rps.getFeature();
                rep.setCol( repr.getPosition().getX().intValue() );
                rep.setRow( repr.getPosition().getY().intValue() );
                // strand...
            }
            break; // only take the first one
        }
    }

    /**
     * Special case: Convert a ReporterCompositeMaps (list) to a Collection of Reporters.
     * 
     * @param reporterCompositeMaps
     * @return Collection of Gemma Reporters.
     */
    public Collection specialConvertReporterCompositeMaps( List reporterCompositeMaps ) {

        if ( reporterCompositeMaps.size() > 1 ) log.warn( "**** More than one ReporterCompositeMaps for a Reporter!" );

        Collection result = new HashSet();
        for ( Iterator iter = reporterCompositeMaps.iterator(); iter.hasNext(); ) {
            ReporterCompositeMap rcp = ( ReporterCompositeMap ) iter.next();
            List rcpps = rcp.getReporterPositionSources();
            log.debug( "Found reporters for composite sequence" );
            for ( Iterator iterator = rcpps.iterator(); iterator.hasNext(); ) {
                ReporterPosition rps = ( ReporterPosition ) iterator.next();

                if ( rps == null ) continue;

                org.biomage.DesignElement.Reporter repr = rps.getReporter();

                if ( repr == null ) continue;

                Reporter conv = convertReporter( repr );

                if ( conv == null ) {
                    log.error( "Null converted reporter!" );
                    continue;
                }

                result.add( conv );

                Integer m = rps.getStart();
                if ( m == null ) continue;

                conv.setStartInBioChar( m.longValue() );
            }
            break; // only take the first one;
        }
        return result;
    }

    /**
     * Generic method to convert associations of a Mage object. The association is resolved into a call to an
     * appropriate method to convert the particular type of association.
     * 
     * @param mageClass The class of the MAGE object to be converted.
     * @param mageObj The MAGE object to be converted.
     * @param gemmaObj The Gemma object whose associations need to be filled in.
     */
    private void convertAssociations( Class mageClass, Object mageObj, Object gemmaObj ) {

        if ( mageObj == null || gemmaObj == null ) return;

        Class classToSeek = ReflectionUtil.getBaseForImpl( gemmaObj );
        String gemmaObjName = classToSeek.getSimpleName();

        try {
            Class[] interfaces = mageClass.getInterfaces();

            if ( interfaces.length == 0 ) return;

            for ( int i = 0; i < interfaces.length; i++ ) {
                Class infc = interfaces[i];
                String infcName = infc.getSimpleName();

                if ( !infcName.startsWith( "Has" ) ) continue;

                String propertyName = infcName.substring( 3 );

                Method getter = mageClass.getMethod( "get" + propertyName, new Class[] {} );

                if ( getter != null ) {
                    try {
                        Method converter = this.getClass().getMethod(
                                "convert" + ReflectionUtil.objectToTypeName( mageObj ) + "Associations",
                                new Class[] { mageObj.getClass(), classToSeek, getter.getClass() } );

                        if ( converter == null ) throw new NoSuchMethodException();

                        converter.invoke( this, new Object[] { mageObj, gemmaObj, getter } );

                    } catch ( NoSuchMethodException e ) {
                        log.warn( "Conversion of associations -- Operation not yet supported: " + "convert"
                                + ReflectionUtil.objectToTypeName( mageObj ) + "Associations("
                                + mageObj.getClass().getName() + ", " + gemmaObjName + ", "
                                + getter.getClass().getName() + ")" );
                    }
                }

            }

            Class superclazz = mageClass.getSuperclass();
            if ( superclazz == null ) return;

            String superclassName = superclazz.getName();
            if ( superclassName.startsWith( "org.biomage" ) && !superclassName.endsWith( "Extendable" )
                    && !superclassName.endsWith( "Describable" ) && !superclassName.endsWith( "Identifiable" ) )
                convertAssociations( superclazz, mageObj, gemmaObj );

        } catch ( NoSuchMethodException e ) {
            log.error( e, e );
        } catch ( IllegalArgumentException e ) {
            log.error( e, e );
        } catch ( IllegalAccessException e ) {
            log.error( e, e );
        } catch ( InvocationTargetException e ) {
            log.error( "InvocationTargetException For: " + gemmaObjName, e );
        }
    }

    // /**
    // * If this object has a slot for an OntologyEntry, fill it in.
    // *
    // * @param associatedGemmaObj
    // */
    // private void fillInOntologyEntry( Object gemmaObj ) {
    // Method setter = findOntologyEntrySetter( gemmaObj );
    // if ( setter == null ) {
    // log.debug( "No ontologyEntry associated with " + gemmaObj.getClass().getSimpleName() );
    // return;
    // }
    //
    // try {
    // setter.invoke( gemmaObj, new Object[] { edu.columbia.gemma.common.description.OntologyEntry.Factory
    // .newInstance() } );
    // } catch ( IllegalArgumentException e ) {
    // log.error( e, e );
    // } catch ( IllegalAccessException e ) {
    // log.error( e, e );
    // } catch ( InvocationTargetException e ) {
    // log.error( e, e );
    // }
    //
    // }

    /**
     * Locate a converter for a MAGE object.
     * 
     * @param mageObj
     * @return Converted object. If the source object is null, the return value is null.
     */
    private Object findAndInvokeConverter( Object mageObj ) {

        if ( mageObj == null ) return null;
        Object convertedGemmaObj = null;
        Method gemmaConverter = null;
        try {
            gemmaConverter = findConverter( mageObj );
            if ( gemmaConverter == null ) return null;
            convertedGemmaObj = gemmaConverter.invoke( this, new Object[] { mageObj } );
        } catch ( IllegalArgumentException e ) {
            log.error( e, e );
        } catch ( IllegalAccessException e ) {
            log.error( e, e );
        } catch ( InvocationTargetException e ) {
            log.error( "InvocationTargetException caused by " + e.getCause() + " when invoking "
                    + gemmaConverter.getName() + " on a " + mageObj.getClass().getName(), e );
            throw new RuntimeException( e );
        }
        return convertedGemmaObj;
    }

    /**
     * Locate a converter for a MAGE object and invoke it.
     * 
     * @param objectToConvert
     * @param converterBaseName
     * @param mageTypeToConvert
     * @return Converted object. If the input mageObj is null, the return value is null.
     */
    private Object findAndInvokeConverter( Object mageObj, String converterBaseName, Class mageTypeToConvert ) {

        if ( mageObj == null ) return null;

        Method gemmaConverter = findConverter( converterBaseName, mageTypeToConvert );
        if ( gemmaConverter == null ) return null;

        Object convertedGemmaObj = null;
        try {
            convertedGemmaObj = gemmaConverter.invoke( this, new Object[] { mageObj } );
        } catch ( IllegalArgumentException e ) {
            log.error( e, e );
        } catch ( IllegalAccessException e ) {
            log.error( e, e );
        } catch ( InvocationTargetException e ) {
            log.error( "InvocationTargetException caused by " + e.getCause() + " when invoking  "
                    + gemmaConverter.getName() + " on a " + mageObj.getClass().getName(), e );
            throw new RuntimeException( e );
        }
        return convertedGemmaObj;
    }

    /**
     * @param getterObject
     * @param propertyName
     * @return
     */
    private Object findAndInvokeGetter( Object getterObject, String propertyName ) {
        Method gemmaGetter = findGetter( getterObject, propertyName );
        try {
            return gemmaGetter.invoke( getterObject, new Object[] {} );
        } catch ( IllegalArgumentException e ) {
            log.error( e );
        } catch ( IllegalAccessException e ) {
            log.error( e );
        } catch ( InvocationTargetException e ) {
            log.error( e );
        }
        return null;
    }

    /**
     * Locate and invoke a setter method.
     * 
     * @param setterObj The object on which we will call the setter
     * @param settee The object we want to set
     * @param setteeClass The class accepted by the setter - not necessarily the class of the setterObj (example:
     *        DatabaseEntry vs. DatabaseEntryImpl)
     * @param propertyName The property name corresponding to the setteeClass
     */
    private void findAndInvokeSetter( Object setterObj, Object settee, Class setteeClass, String propertyName ) {

        Method gemmaSetter = findSetter( setterObj, propertyName, setteeClass );
        if ( gemmaSetter == null ) return;

        try {
            // PropertyUtils.setProperty(setterObj, propertyName, settee); // this would work if we didn't have this
            // Impl vs. base problem.
            gemmaSetter.invoke( setterObj, new Object[] { settee } );
        } catch ( IllegalArgumentException e ) {
            log.error( e );
        } catch ( IllegalAccessException e ) {
            log.error( e );
        } catch ( InvocationTargetException e ) {
            log.error( e );
        }
    }

    /**
     * Find a converter method for a MAGE object.
     * 
     * @param mageObj
     * @return
     */
    private Method findConverter( Object mageObj ) {
        String mageTypeName = ReflectionUtil.objectToTypeName( mageObj );
        Method converter = null;
        try {
            converter = this.getClass().getMethod( "convert" + mageTypeName, new Class[] { mageObj.getClass() } );
        } catch ( NoSuchMethodException e ) {
            log.warn( "Conversion operation not yet supported: " + "convert" + mageTypeName + "("
                    + mageObj.getClass().getName() + ")" );
        }
        return converter;
    }

    /**
     * Find a converter for an association.
     * 
     * @param associationName
     * @param mageAssociatedType
     * @return
     */
    private Method findConverter( String associationName, Class mageAssociatedType ) {
        Method gemmaConverter = null;
        try {
            gemmaConverter = this.getClass()
                    .getMethod( "convert" + associationName, new Class[] { mageAssociatedType } );
            // log.debug( "Found converter: convert" + associationName + "( " + mageAssociatedType.getSimpleName() + "
            // )" );
        } catch ( NoSuchMethodException e ) {
            log.warn( "Conversion operation not yet supported: " + "convert" + associationName + "("
                    + mageAssociatedType.getName() + ")" );
        }
        return gemmaConverter;
    }

    /**
     * Given an object and a property name, get the getter method for that property.
     * 
     * @param gettee
     * @param propertyName
     * @return
     */
    private Method findGetter( Object gettee, String propertyName ) {
        Method gemmaGetter = null;
        try {
            gemmaGetter = ReflectionUtil.getBaseForImpl( gettee ).getMethod( "get" + propertyName, new Class[] {} );
        } catch ( SecurityException e ) {
            log.error( e );
        } catch ( NoSuchMethodException e ) {
            log.error( "No such getter: " + gettee.getClass().getSimpleName() + ".get" + propertyName + "(" + ")", e );
        }
        return gemmaGetter;
    }

    /**
     * Find a setter for a property.
     * 
     * @param setter - The object on which we want to call the setter
     * @param propertyName - The name of the property we want to set
     * @param setee - The object which we want to enter as an argument to the setter.
     * @return
     */
    private Method findSetter( Object setter, String propertyName, Class setee ) {
        Method gemmaSetter = null;
        try {
            gemmaSetter = ReflectionUtil.getBaseForImpl( setter ).getMethod( "set" + propertyName,
                    new Class[] { setee } );
        } catch ( SecurityException e ) {
            log.error( e );
        } catch ( NoSuchMethodException e ) {
            log.error( "No such setter: " + "set" + propertyName + "(" + setee.getSimpleName() + ")", e );
        }
        return gemmaSetter;
    }

    // /**
    // * Given a Gemma object, and a class name, try to get an instance of the named class, first assuming that the new
    // * object is in the same package as the Gemma object but then looking in other packages of edu.columbia.gemma.
    // *
    // * @param gemmaObj The object whose package we expect to find the new object's class
    // * @param className The unqualified name of the class to be instantiated (e.g., "BioMaterial")
    // * @return an instance of className.
    // * @throws ClassNotFoundException
    // * @throws NoSuchMethodException
    // * @throws IllegalAccessException
    // * @throws InvocationTargetException
    // */
    // private Object getInstanceFromStaticInnerFactory( Object gemmaObj, String className ) throws
    // NoSuchMethodException,
    // IllegalAccessException, InvocationTargetException, ClassNotFoundException {
    // String factoryClassName = gemmaObj.getClass().getPackage().getName() + "." + className + "$Factory";
    // Class clazz = null;
    // try {
    // clazz = Class.forName( factoryClassName );
    // } catch ( ClassNotFoundException e ) {
    // Package[] allPackages = Package.getPackages();
    // for ( int i = 0; i < allPackages.length; i++ ) {
    // String packageName = allPackages[i].getName();
    // if ( !packageName.startsWith( "edu.columbia.gemma." ) ) continue;
    //
    // factoryClassName = packageName + "." + className + "$Factory";
    // try {
    // clazz = Class.forName( factoryClassName );
    // if ( clazz != null ) {
    // break;
    // }
    // } catch ( ClassNotFoundException e1 ) {
    // ; // that's okay.
    // }
    // }
    // }
    //
    // if ( clazz == null ) {
    // throw new ClassNotFoundException( "No support found for " + className );
    // }
    //
    // Method factoryMethod = clazz.getMethod( "newInstance", new Class[] {} );
    // return factoryMethod.invoke( null, new Object[] {} );
    // }

    /**
     * For a method like "getFoo", returns "Foo".
     * 
     * @param getter
     * @return
     */
    private String getterToPropertyName( Method getter ) {
        if ( !getter.getName().startsWith( "get" ) ) throw new IllegalArgumentException( "Not a getter" );
        return getter.getName().substring( 3 );
    }

    /**
     * @param gemmaObj
     * @return
     */
    private Collection<Reporter> initializeReporterCollection(
            edu.columbia.gemma.expression.arrayDesign.ArrayDesign gemmaObj ) {
        Collection<Reporter> designObjs;
        if ( gemmaObj.getReporters() == null ) {
            designObjs = new HashSet<Reporter>();
            gemmaObj.setReporters( designObjs );
        } else {
            designObjs = gemmaObj.getReporters();
        }
        return designObjs;
    }

    /**
     * @param gemmaObj
     * @return
     */
    private Collection<CompositeSequence> initializeCompositeSequenceCollection(
            edu.columbia.gemma.expression.arrayDesign.ArrayDesign gemmaObj ) {
        Collection<CompositeSequence> designObjs;
        if ( gemmaObj.getCompositeSequences() == null ) {
            designObjs = new HashSet<CompositeSequence>();
            gemmaObj.setCompositeSequences( designObjs );
        } else {
            designObjs = gemmaObj.getCompositeSequences();
        }
        return designObjs;
    }

    /**
     * Initialize the conversion process by calling the getter and getting the association name
     * 
     * @param mageObj
     * @param gemmaObj
     * @param getter
     * @return The name of the association, taken from the getter.
     */
    private Object intializeConversion( Object mageObj, Method getter ) {
        Object associatedObject = invokeGetter( mageObj, getter );

        if ( associatedObject == null ) {
            log.debug( "Getter called on " + mageObj.getClass().getName() + " but failed to return a value: "
                    + getter.getName() + " (Probably no data)" );
            return null;
        }

        log.debug( mageObj.getClass().getName() + "--->" + getterToPropertyName( getter ) );
        return associatedObject;
    }

    /**
     * Call a 'get' method to retrieve an associated MAGE object for conversion.
     * 
     * @param mageObj
     * @param getter
     * @return A MAGE domain object, or a List of MAGE domain objects. The caller has to figure out which.
     */
    private Object invokeGetter( Object mageObj, Method getter ) {
        Object associatedObject = null;

        if ( getter == null ) throw new IllegalArgumentException( "Null getter passed" );
        if ( mageObj == null )
            throw new IllegalArgumentException( "Attempt to run " + getter.getName() + " on null object" );

        try {
            associatedObject = getter.invoke( mageObj, new Object[] {} );
        } catch ( IllegalArgumentException e ) {
            log.error( e, e );
        } catch ( IllegalAccessException e ) {
            log.error( e, e );
        } catch ( InvocationTargetException e ) {
            log.error( mageObj.getClass().getName() + "." + getter.getName() + " threw an exception: " + e.getCause(),
                    e );
        }
        return associatedObject;
    }

    /**
     * Generic method to fill in a Gemma object where the association in Mage has cardinality of >1.
     * 
     * @param associatedList - The result of the Getter call.
     * @param gemmObj - The Gemma object in which to place the converted Mage object(s).
     * @param onlyTakeOne - This indicates that the cardinality in the Gemma object is at most 1. Therefore we pare down
     *        the Mage object list to take just the first one.
     */
    private void simpleFillIn( List<Object> associatedList, Object gemmaObj, Method getter, boolean onlyTakeOne ) {
        this.simpleFillIn( associatedList, gemmaObj, getter, onlyTakeOne, null );
    }

    /**
     * Generic method to fill in a Gemma object where the association in Mage has cardinality of >1.
     * 
     * @param associatedList - The result of the Getter call.
     * @param gemmObj - The Gemma object in which to place the converted Mage object(s).
     * @param onlyTakeOne - This indicates that the cardinality in the Gemma object is at most 1. Therefore we pare down
     *        the Mage object list to take just the first one.
     * @param actualGemmaAssociationName - for example, a BioSequence hasa "SequenceDatabaseEntry", not a
     *        "DatabaseEntry". If null, the name is inferred.
     */
    private void simpleFillIn( List<Object> associatedList, Object gemmaObj, Method getter, boolean onlyTakeOne,
            String actualGemmaAssociationName ) {

        if ( associatedList == null || gemmaObj == null || getter == null )
            throw new IllegalArgumentException( "Null objects" );

        if ( associatedList.size() == 0 ) {
            log.debug( "List was not null, but empty" );
            return;
        }

        // This could be refactored to share more code with the other simpleFillIn methods.
        String associationName = actualGemmaAssociationName;
        if ( associationName == null ) associationName = getterToPropertyName( getter );

        try {
            if ( onlyTakeOne ) {
                Object mageObj = associatedList.get( 0 );
                Object convertedGemmaObj = findAndInvokeConverter( mageObj );
                if ( convertedGemmaObj == null ) return; // not supported.
                Class convertedGemmaClass = ReflectionUtil.getBaseForImpl( convertedGemmaObj );
                log.debug( "Converting a MAGE list to a single instance of " + convertedGemmaClass.getSimpleName() );
                findAndInvokeSetter( gemmaObj, convertedGemmaObj, convertedGemmaClass, associationName );
            } else {
                Class gemmaClass = ReflectionUtil.getBaseForImpl( gemmaObj );
                log.debug( "Converting a MAGE list to a Gemma list associated with a " + gemmaClass.getSimpleName() );
                Collection<Object> gemmaObjList = ( Collection<Object> ) findAndInvokeGetter( gemmaObj, associationName );
                if ( gemmaObjList == null ) {
                    gemmaObjList = new HashSet<Object>();
                }

                // avoid adding the same object twice.
                for ( Object mageObj : associatedList ) {
                    Object convertedGemmaObj = findAndInvokeConverter( mageObj );
                    if ( convertedGemmaObj == null ) continue; // not supported.
                    if ( !gemmaObjList.contains( convertedGemmaObj ) ) {
                        log.debug( "Adding " + convertedGemmaObj );
                        gemmaObjList.add( convertedGemmaObj );
                    }
                }
                findAndInvokeSetter( gemmaObj, gemmaObjList, Collection.class, associationName );
            }
        } catch ( SecurityException e ) {
            log.error( e, e );
        } catch ( IllegalArgumentException e ) {
            log.error( e, e );
        }

    }

    /**
     * Generic method to fill in a Gemma object's association with a Mage object where the name can be predicted from
     * the associated object type. E.g., the Gemma object with an association to "BioSequence" has a "bioSequence"
     * property; sometimes instead we have things like ImmobilizedCharacteristic.
     * 
     * @param associatedMageObject The associated object we need to find a place for.
     * @param gemmaObj The Gemma object in which to place the converted Mage object.
     * @param getter The getter for the Mage object
     */
    private void simpleFillIn( Object associatedMageObject, Object gemmaObj, Method getter ) {
        this.simpleFillIn( associatedMageObject, gemmaObj, getter, null );
    }

    /**
     * Generic method to fill in a Gemma object's association with a Mage object where the name might be predicted from
     * the associated object type. E.g., the Gemma object with an association to "BioSequence" has a "bioSequence"
     * property; sometimes instead we have things like ImmobilizedCharacteristic.
     * 
     * @param associatedMageObject The associated object we need to find a place for.
     * @param gemmaObj The Gemma object in which to place the converted Mage object.
     * @param getter The getter for the Mage object
     * @param actualGemmaAssociationName - Replacement name for the Gemma association. This is to handle situations
     *        where the getter does not have a name that can be figured out. If null, the name is figured out from the
     *        getter.
     */
    private void simpleFillIn( Object associatedMageObject, Object gemmaObj, Method getter,
            String actualGemmaAssociationName ) {

        if ( associatedMageObject == null ) return;
        String associationName = getterToPropertyName( getter );
        String inferredGemmaAssociationName = actualGemmaAssociationName == null ? associationName
                : actualGemmaAssociationName;

        try {
            Class mageAssociatedType = associatedMageObject.getClass();
            Object gemmaAssociatedObj = findAndInvokeConverter( associatedMageObject, associationName,
                    mageAssociatedType );
            if ( gemmaAssociatedObj == null ) return;

            Class gemmaClass = ReflectionUtil.getBaseForImpl( gemmaAssociatedObj.getClass() );
            // inferredGemmaAssociationName = convertAssociationName( actualGemmaAssociationName,
            // gemmaAssociatedObj );
            // log.info( "Filling in " + gemmaObj.getClass().getSimpleName() + "." + inferredGemmaAssociationName );
            findAndInvokeSetter( gemmaObj, gemmaAssociatedObj, gemmaClass, inferredGemmaAssociationName );

        } catch ( SecurityException e ) {
            log.error( e, e );
        } catch ( IllegalArgumentException e ) {
            log.error( e, e );
        }

    }

    /**
     * Special case to convert BioAssayData associations of a BioAssay object. We only store references to the data
     * files - there is no BioAssayData object in Gemma.
     * 
     * @param list BioAssayData objects to be handled.
     * @param gemmaObj Gemma BioAssay object to attach data files to.
     */
    private void specialConvertBioAssayBioAssayDataAssociations( List<BioAssayData> bioAssayData,
            edu.columbia.gemma.expression.bioAssay.BioAssay gemmaObj ) {

        for ( BioAssayData bioAssayDatum : bioAssayData ) {
            LocalFile lf = convertBioAssayData( bioAssayDatum );
            if ( lf == null ) continue;

            if ( bioAssayDatum instanceof DerivedBioAssayData ) {
                if ( gemmaObj.getDerivedDataFiles() == null ) gemmaObj.setDerivedDataFiles( new HashSet() );

                gemmaObj.getDerivedDataFiles().add( lf );

                Transformation transformation = ( ( DerivedBioAssayData ) bioAssayDatum ).getProducerTransformation();
                List<BioAssayData> sources = transformation.getBioAssayDataSources();

                if ( sources.size() > 1 ) {
                    log.warn( "Derived bioassayData maps to more than one other bioassaydata!" );
                }

                for ( BioAssayData sourceData : sources ) {
                    if ( sourceData instanceof MeasuredBioAssayData ) {
                        MeasuredBioAssayData measuredSourceData = ( MeasuredBioAssayData ) sourceData;
                        gemmaObj.setRawDataFile( convertMeasuredBioAssayData( measuredSourceData ) );
                    }
                }
            } else if ( bioAssayDatum instanceof MeasuredBioAssayData ) {
                log.debug( "Got raw data file" );
                gemmaObj.setRawDataFile( lf );
            } else {
                throw new IllegalArgumentException( "Unknown BioAssayData class: " + bioAssayDatum.getClass().getName() );
            }
            log.debug( bioAssayDatum.getIdentifier() );
        }
    }

    /**
     * Extract compositeSequence information from the ArrayDesign package. The ArrayDesign package doesn't have any
     * information about the compositeSequences, other than the fact that they belong to this arrayDesign.
     * 
     * @param compositeGroups
     * @param gemmaObj
     */
    private void specialConvertCompositeGroups( List<CompositeGroup> compositeGroups,
            edu.columbia.gemma.expression.arrayDesign.ArrayDesign gemmaObj ) {

        Collection<CompositeSequence> designObjs = initializeCompositeSequenceCollection( gemmaObj );

        for ( CompositeGroup rg : compositeGroups ) {
            List<org.biomage.DesignElement.CompositeSequence> reps = rg.getCompositeSequences();
            for ( org.biomage.DesignElement.CompositeSequence compseq : reps ) {
                CompositeSequence csconv = convertCompositeSequence( compseq );
                csconv.setArrayDesign( gemmaObj );
                if ( !designObjs.contains( csconv ) ) designObjs.add( csconv );
            }
        }
        gemmaObj.setAdvertisedNumberOfDesignElements( designObjs.size() );
    }

    /**
     * @param mageObj
     * @param gemmaObj
     */
    private void specialConvertFeatureExtraction( FeatureExtraction mageObj,
            edu.columbia.gemma.expression.bioAssay.BioAssay gemmaObj ) {
        PhysicalBioAssay pba = mageObj.getPhysicalBioAssaySource();
        specialConvertBioAssayCreation( pba, gemmaObj );
        convertAssociations( pba, gemmaObj );
    }

    // /**
    // * Special conversion method for MAGE OntologyEntries
    // *
    // * @param entry - the MAGE OntologyEntry to be converted.
    // * @param gemmaObj The Gemma object which will have the association to the resulting converted object.
    // */
    // private void specialConvertOntologyEntryAssociation( OntologyEntry entry, Object gemmaObj, List elmList ) {
    // if ( gemmaObj == null ) throw new IllegalArgumentException( "Null Gemma object passed" );
    // log.debug( "Entering specialConvertOntologyEntryAssociation" );
    // // this won't be called
    // }

    /**
     * In Gemma, an OntologyEntry is a DatabaseEntry, while in Mage, an OntologyEntry hasa DatabaseEntry.
     * 
     * @param associatedObject
     * @param gemmaObj
     */
    private void specialConvertOntologyEntryDatabaseEntry( org.biomage.Description.DatabaseEntry databaseEntry,
            edu.columbia.gemma.common.description.OntologyEntry gemmaObj ) {
        ExternalDatabase ed = convertDatabase( databaseEntry.getDatabase() );
        assert ed != null : "Null externalDatabase for MAGE version of " + gemmaObj;
        gemmaObj.setExternalDatabase( ed );
        gemmaObj.setAccession( databaseEntry.getAccession() );
    }

    /**
     * Convert all the reporters via the reporter groups.
     * 
     * @param reporterGroups
     * @param gemmaObj
     */
    private void specialConvertReporterGroups( List reporterGroups,
            edu.columbia.gemma.expression.arrayDesign.ArrayDesign gemmaObj ) {

        Collection<Reporter> designObjs;
        designObjs = initializeReporterCollection( gemmaObj );

        for ( Iterator iter = reporterGroups.iterator(); iter.hasNext(); ) {
            ReporterGroup rg = ( ReporterGroup ) iter.next();
            List reps = rg.getReporters();
            for ( Iterator iterator = reps.iterator(); iterator.hasNext(); ) {
                org.biomage.DesignElement.Reporter reporter = ( org.biomage.DesignElement.Reporter ) iterator.next();
                Reporter convertedReporter = convertReporter( reporter );
                convertedReporter.setArrayDesign( gemmaObj );
                if ( !designObjs.contains( convertedReporter ) ) designObjs.add( convertedReporter );
            }
        }
        gemmaObj.setAdvertisedNumberOfDesignElements( designObjs.size() );
    }

    /**
     * Special case, OntologyEntry maps to an Enum.
     * 
     * @param mageObj
     * @return SequenceType
     */
    private void specialConvertSequenceType( OntologyEntry mageObj, BioSequence gemmaObj ) {

        if ( mageObj == null ) return;
        String value = mageObj.getValue();
        if ( value.equalsIgnoreCase( "bc" ) ) {
            gemmaObj.setType( SequenceType.BAC );
        } else if ( value.equalsIgnoreCase( "est" ) ) {
            gemmaObj.setType( SequenceType.EST );
        } else if ( value.equalsIgnoreCase( "affyprobe" ) ) {
            gemmaObj.setType( SequenceType.AFFY_PROBE );
        } else if ( value.equalsIgnoreCase( "affytarget" ) ) {
            gemmaObj.setType( SequenceType.AFFY_TARGET );
        } else if ( value.equalsIgnoreCase( "mrna" ) ) {
            gemmaObj.setType( SequenceType.mRNA );
        } else if ( value.equalsIgnoreCase( "refseq" ) ) {
            gemmaObj.setType( SequenceType.REFSEQ );
        } else if ( value.equalsIgnoreCase( "chromosome" ) ) {
            gemmaObj.setType( SequenceType.WHOLE_CHROMOSOME );
        } else if ( value.equalsIgnoreCase( "genome" ) ) {
            gemmaObj.setType( SequenceType.WHOLE_GENOME );
        } else if ( value.equalsIgnoreCase( "orf" ) ) {
            gemmaObj.setType( SequenceType.ORF );
        } else if ( value.equalsIgnoreCase( "dna" ) ) {
            gemmaObj.setType( SequenceType.DNA );
        } else {
            gemmaObj.setType( SequenceType.OTHER );
        }
    }

    /**
     * From a PhysicalBioAssay, find the associated ArrayDesign.
     * 
     * @param mageObj
     * @param result
     */
    private void specialConvertBioAssayCreation( PhysicalBioAssay mageObj,
            edu.columbia.gemma.expression.bioAssay.BioAssay result ) {
        
        BioAssayCreation bac = mageObj.getBioAssayCreation();
        if ( bac == null ) return;

        ArrayDesign ad = bac.getArray().getArrayDesign();
        if ( ad == null ) return;
        edu.columbia.gemma.expression.arrayDesign.ArrayDesign conv = convertArrayDesign( ad );

        if ( result.getArrayDesignsUsed() == null ) {
            result.setArrayDesignsUsed( new HashSet() );
        }

        // check to make sure we aren't addin the same array design twice.
        Set<String> alreadyLinkedArrayDesigns = new HashSet<String>();
        for ( edu.columbia.gemma.expression.arrayDesign.ArrayDesign arrayDesign : result.getArrayDesignsUsed() ) {
            alreadyLinkedArrayDesigns.add( arrayDesign.getName() );
        }

        if ( alreadyLinkedArrayDesigns.contains( conv.getName() ) ) return;

        log.debug( "Adding array design used " + ad.getName() + " to " + result.getName() );
        result.getArrayDesignsUsed().add( conv );
        

        //add biomaterials to the resulting bioassay
        Collection<BioMaterialMeasurement> measurements = ( Collection<BioMaterialMeasurement> ) bac
                .getSourceBioMaterialMeasurements();
        
        Vector<BioMaterial> biomaterials = new Vector<BioMaterial>();
        
        for (BioMaterialMeasurement bmm : measurements)
            biomaterials.add(convertBioMaterial(bmm.getBioMaterial()));
        
        result.setSamplesUsed(biomaterials);
    }

    /**
     * Extract the feature location information for a MAGE reporter and fill it into the Gemma Reporter.
     * 
     * @param mageObj
     * @param result
     */
    private void specialGetReporterFeatureLocations( org.biomage.DesignElement.Reporter mageObj, Reporter result ) {
        if ( mageObj == null ) return;
        if ( result == null ) throw new IllegalArgumentException( "Null Reporter passed" );
        List<FeatureReporterMap> featureReporterMaps = mageObj.getFeatureReporterMaps();
        if ( featureReporterMaps == null ) return;
        for ( FeatureReporterMap featureReporterMap : featureReporterMaps ) {
            if ( featureReporterMap == null ) continue;
            List<FeatureInformation> featureInformationSources = featureReporterMap.getFeatureInformationSources();
            for ( FeatureInformation featureInformation : featureInformationSources ) {
                if ( featureInformation == null ) continue;

                if ( featureInformation.getFeature() == null || featureInformation.getFeature().getPosition() == null )
                    continue;

                result.setCol( featureInformation.getFeature().getPosition().getX().intValue() );
                result.setRow( featureInformation.getFeature().getPosition().getY().intValue() );
                break;
            }
            break;
        }

    }

    /**
     * @return Returns the bioAssayDimensions.
     */
    public BioAssayDimensions getBioAssayDimensions() {
        return this.bioAssayDimensions;
    }

}
