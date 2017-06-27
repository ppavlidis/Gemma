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
package ubic.gemma.core.analysis.preprocess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.core.analysis.expression.AnalysisUtilService;
import ubic.gemma.core.analysis.service.ExpressionExperimentVectorManipulatingService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.ExpressionExperimentVectorMergeEvent;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Tackles the problem of concatenating DesignElementDataVectors for a single experiment. This is necessary When a study
 * uses two or more similar array designs without 'replication'. Typical of the genre is GSE60 ("Diffuse large B-cell
 * lymphoma"), with 31 BioAssays on GPL174, 35 BioAssays on GPL175, and 66 biomaterials. A more complex one: GSE3500,
 * with 13 ArrayDesigns. In that (and others) case, there are quantitation types which do not appear on all array
 * designs, leaving gaps in the vectors that have to be filled in.
 * <p>
 * The algorithm for dealing with this is a preprocessing step:
 * <ol>
 * <li>Generate a merged set of vectors for each of the (important) quantitation types.</li>
 * <li>Create a merged BioAssayDimension</li>
 * <li>Persist the new vectors, which are now tied to a <em>single DesignElement</em>. This is, strictly speaking,
 * incorrect, but because the design elements used in the vector all point to the same sequence, there is no major
 * problem in analyzing this. However, there is a potential loss of information.
 * <li>Cleanup: remove old vectors, analyses, and BioAssayDimensions.
 * <li>Postprocess: Recreate the processed datavectors, including masking missing values if necesssary.
 * </ol>
 * <p>
 * Vectors which are empty (all missing values) are not persisted. If problems are found during merging, an exception
 * will be thrown, though this may leave things in a bad state requiring a reload of the data.
 * 
 * @author pavlidis
 * @version $Id$
 * @see ExpressionDataMatrixBuilder
 */
@Component
public class VectorMergingServiceImpl extends ExpressionExperimentVectorManipulatingService implements
        VectorMergingService {

    private static Log log = LogFactory.getLog( VectorMergingServiceImpl.class.getName() );

    private static final String MERGED_DIM_DESC_PREFIX = "Generated by the merger of";

    @Autowired
    private AnalysisUtilService analysisUtilService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private PreprocessorService preprocessorService;

    @Autowired
    private VectorMergingHelperService vectorMergingHelperService;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.core.analysis.preprocess.VectorMergingService#mergeVectors(ubic.gemma.model.expression.experiment.
     * ExpressionExperiment)
     */
    @Override
    public ExpressionExperiment mergeVectors( ExpressionExperiment expExp ) {

        Collection<ArrayDesign> arrayDesigns = expressionExperimentService.getArrayDesignsUsed( expExp );

        if ( arrayDesigns.size() > 1 ) {
            throw new IllegalArgumentException( "Cannot cope with more than one platform" );
        }

        expExp = expressionExperimentService.thaw( expExp );
        Collection<QuantitationType> qts = expressionExperimentService.getQuantitationTypes( expExp );
        log.info( qts.size() + " quantitation types for potential merge" );
        /*
         * Load all the bioassay dimensions, which will be merged.
         */
        Collection<BioAssayDimension> allOldBioAssayDims = new HashSet<>();
        for ( BioAssay ba : expExp.getBioAssays() ) {
            Collection<BioAssayDimension> oldBioAssayDims = bioAssayService.findBioAssayDimensions( ba );
            for ( BioAssayDimension bioAssayDim : oldBioAssayDims ) {
                if ( bioAssayDim.getDescription().startsWith( MERGED_DIM_DESC_PREFIX ) ) {
                    // not foolproof, but avoids some artifacts - e.g. if there were previous failed attempts at this.
                    continue;
                }
                allOldBioAssayDims.add( bioAssayDim );
            }
        }

        if ( allOldBioAssayDims.size() == 0 ) {
            throw new IllegalStateException(
                    "No bioassaydimensions found to merge (previously merged ones are filtered, data may be corrupt?" );
        }

        if ( allOldBioAssayDims.size() == 1 ) {
            log.warn( "Experiment already has only a single bioassaydimension, nothing seems to need merging. Bailing" );
            return expExp;
        }

        log.info( allOldBioAssayDims.size() + " bioassaydimensions to merge" );
        List<BioAssayDimension> sortedOldDims = sortedBioAssayDimensions( allOldBioAssayDims );

        BioAssayDimension newBioAd = getNewBioAssayDimension( sortedOldDims );
        int totalBioAssays = newBioAd.getBioAssays().size();
        assert totalBioAssays == expExp.getBioAssays().size() : "experiment has " + expExp.getBioAssays().size()
                + " but new bioassaydimension has " + totalBioAssays;

        Map<QuantitationType, Collection<DesignElementDataVector>> qt2Vec = getVectors( expExp, qts, allOldBioAssayDims );

        /*
         * This will run into problems if there are excess quantitation types
         */
        int numSuccessfulMergers = 0;
        for ( QuantitationType type : qt2Vec.keySet() ) {

            Collection<DesignElementDataVector> oldVecs = qt2Vec.get( type );

            if ( oldVecs.isEmpty() ) {
                log.warn( "No vectors for " + type );
                continue;
            }

            Map<CompositeSequence, Collection<DesignElementDataVector>> deVMap = getDevMap( oldVecs );

            if ( deVMap == null ) {
                log.info( "Vector merging will not be done for " + type
                        + " as there is only one vector per element already" );
                continue;
            }

            log.info( "Processing " + oldVecs.size() + " vectors  for " + type );

            Collection<DesignElementDataVector> newVectors = new HashSet<>();
            int numAllMissing = 0;
            int missingValuesForQt = 0;
            for ( CompositeSequence de : deVMap.keySet() ) {

                RawExpressionDataVector vector = initializeNewVector( expExp, newBioAd, type, de );
                Collection<DesignElementDataVector> dedvs = deVMap.get( de );

                /*
                 * these ugly nested loops are to ENSURE that we get the vector reconstructed properly. For each of the
                 * old bioassayDimensions, find the designelementdatavector that uses it. If there isn't one, fill in
                 * the values for that dimension with missing data. We go through the dimensions in the same order that
                 * we joined them up.
                 */

                List<Object> data = new ArrayList<Object>();
                int totalMissingInVector = makeMergedData( sortedOldDims, newBioAd, type, de, dedvs, data );
                missingValuesForQt += totalMissingInVector;
                if ( totalMissingInVector == totalBioAssays ) {
                    numAllMissing++;
                    continue; // we don't save data that is all missing.
                }

                if ( data.size() != totalBioAssays ) {
                    throw new IllegalStateException( "Wrong number of values for " + de + " / " + type + ", expected "
                            + totalBioAssays + ", got " + data.size() );
                }

                byte[] newDataAr = converter.toBytes( data.toArray() );

                vector.setData( newDataAr );

                newVectors.add( vector );

            }

            // TRANSACTION
            vectorMergingHelperService.persist( expExp, type, newVectors );

            if ( numAllMissing > 0 ) {
                log.info( numAllMissing + " vectors had all missing values and were junked for " + type );
            }

            if ( missingValuesForQt > 0 ) {
                log.info( missingValuesForQt + " total missing values: " + type );
            }

            log.info( "Removing " + oldVecs.size() + " old vectors for " + type );
            designElementDataVectorService.remove( oldVecs );

            expExp.getRawExpressionDataVectors().removeAll( oldVecs );
            numSuccessfulMergers++;
        } // for each quantitation type

        if ( numSuccessfulMergers == 0 ) {
            throw new IllegalStateException( "Nothing was merged. Maybe all the vectors are effectively merged already" );
        }

        expressionExperimentService.update( expExp );

        // Several transactions
        cleanUp( expExp, allOldBioAssayDims, newBioAd );

        // transaction
        audit( expExp,
                "Vector merging peformed, merged " + allOldBioAssayDims + " old bioassay dimensions for " + qts.size()
                        + " quantitation types." );

        // several transactions
        try {

            preprocessorService.process( expExp );
        } catch ( PreprocessingException e ) {
            log.error( "Error during postprocessing", e );
        }

        return expExp;
    }

    /**
     * @param arrayDesign
     */
    private void audit( ExpressionExperiment ee, String note ) {
        AuditEventType eventType = ExpressionExperimentVectorMergeEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( ee, eventType, note );
    }

    /**
     * @param expExp
     * @param allOldBioAssayDims
     * @param newBioAd
     */
    private void cleanUp( ExpressionExperiment expExp, Collection<BioAssayDimension> allOldBioAssayDims,
            BioAssayDimension newBioAd ) {

        analysisUtilService.deleteOldAnalyses( expExp );

        /*
         * Delete the experimental design? Actually it _should_ be okay, since the association is with biomaterials.
         */

        // remove the old BioAssayDimensions
        for ( BioAssayDimension oldDim : allOldBioAssayDims ) {
            // careful, the 'new' bioassaydimension might be one of the old ones that we're reusing.
            if ( oldDim.equals( newBioAd ) ) continue;
            try {
                bioAssayDimensionService.remove( oldDim );
            } catch ( Exception e ) {
                log.warn( "Could not remove an old bioAssayDimension with ID=" + oldDim.getId() );
            }
        }
    }

    /**
     * Create a new one or use an existing one. (an existing one might be found if this process was started once before
     * and aborted partway through).
     * 
     * @param oldDims in the sort order to be used.
     * @return
     */
    private BioAssayDimension combineBioAssayDimensions( List<BioAssayDimension> oldDims ) {

        List<BioAssay> bioAssays = new ArrayList<BioAssay>();
        for ( BioAssayDimension bioAd : oldDims ) {
            for ( BioAssay bioAssay : bioAd.getBioAssays() ) {
                if ( bioAssays.contains( bioAssay ) ) {
                    throw new IllegalStateException( "Duplicate bioassay for biodimension: " + bioAssay );
                }
                bioAssays.add( bioAssay );

            }
        }

        // first see if we already have an equivalent one.
        boolean found = true;
        for ( BioAssayDimension newDim : oldDims ) {
            // size should be the same.
            List<BioAssay> assaysInExisting = newDim.getBioAssays();
            if ( assaysInExisting.size() != bioAssays.size() ) {
                continue;
            }

            for ( int i = 0; i < bioAssays.size(); i++ ) {
                if ( !assaysInExisting.get( i ).equals( bioAssays.get( i ) ) ) {
                    found = false;
                    break;
                }
            }
            if ( !found ) continue;
            log.info( "Already have a dimension created that fits the bill - removing it from the 'old' list." );
            oldDims.remove( newDim );
            return newDim;
        }

        BioAssayDimension newBioAd = BioAssayDimension.Factory.newInstance();
        newBioAd.setName( "" );
        newBioAd.setDescription( MERGED_DIM_DESC_PREFIX + " " + oldDims.size() + " dimensions: " );

        for ( BioAssayDimension bioAd : oldDims ) {
            newBioAd.setName( newBioAd.getName() + bioAd.getName() + " " );
            newBioAd.setDescription( newBioAd.getDescription() + bioAd.getName() + " " );
        }

        newBioAd.setName( StringUtils.abbreviate( newBioAd.getName(), 255 ) );
        newBioAd.setBioAssays( bioAssays );

        newBioAd = bioAssayDimensionService.create( newBioAd );
        log.info( "Created new bioAssayDimension with " + newBioAd.getBioAssays().size() + " bioassays." );
        return newBioAd;
    }

    /**
     * @param de
     * @param data
     * @param oldDim
     * @param representation
     * @return The number of missing values which were added.
     */
    private int fillMissingValues( CompositeSequence de, List<Object> data, BioAssayDimension oldDim,
            PrimitiveType representation ) {
        int nullsNeeded = oldDim.getBioAssays().size();
        for ( int i = 0; i < nullsNeeded; i++ ) {
            // FIXME this code taken from GeoConverter
            if ( representation.equals( PrimitiveType.DOUBLE ) ) {
                data.add( Double.NaN );
            } else if ( representation.equals( PrimitiveType.STRING ) ) {
                data.add( "" );
            } else if ( representation.equals( PrimitiveType.INT ) ) {
                data.add( 0 );
            } else if ( representation.equals( PrimitiveType.BOOLEAN ) ) {
                data.add( false );
            } else {
                throw new UnsupportedOperationException( "Missing values in data vectors of type " + representation
                        + " not supported (when processing " + de );
            }
        }
        return nullsNeeded;
    }

    /**
     * @param oldVectors
     * @return map of design element to vectors.
     */
    private Map<CompositeSequence, Collection<DesignElementDataVector>> getDevMap(
            Collection<? extends DesignElementDataVector> oldVectors ) {
        Map<CompositeSequence, Collection<DesignElementDataVector>> deVMap = new HashMap<CompositeSequence, Collection<DesignElementDataVector>>();
        boolean atLeastOneMatch = false;
        assert !oldVectors.isEmpty();

        for ( DesignElementDataVector vector : oldVectors ) {
            CompositeSequence designElement = vector.getDesignElement();
            if ( !deVMap.containsKey( designElement ) ) {
                if ( log.isDebugEnabled() )
                    log.debug( "adding " + designElement + " " + designElement.getBiologicalCharacteristic() );
                deVMap.put( designElement, new HashSet<DesignElementDataVector>() );
            }
            deVMap.get( designElement ).add( vector );

            if ( deVMap.get( designElement ).size() > 1 ) {
                atLeastOneMatch = true;
            }
        }

        if ( !atLeastOneMatch ) {
            return null;
        }
        return deVMap;
    }

    /**
     * @param existing - can be null, in which case an existing dimension is used.
     * @param sortedOldDims
     * @return
     */
    private BioAssayDimension getNewBioAssayDimension( List<BioAssayDimension> sortedOldDims ) {
        return combineBioAssayDimensions( sortedOldDims );
    }

    /**
     * Get the current set of vectors that need to be updated.
     * 
     * @param expExp
     * @param qts - only used to check for problems.
     * @param allOldBioAssayDims
     * @return
     */
    private Map<QuantitationType, Collection<DesignElementDataVector>> getVectors( ExpressionExperiment expExp,
            Collection<QuantitationType> qts, Collection<BioAssayDimension> allOldBioAssayDims ) {
        Collection<DesignElementDataVector> oldVectors = new HashSet<DesignElementDataVector>();

        for ( BioAssayDimension dim : allOldBioAssayDims ) {
            oldVectors.addAll( super.designElementDataVectorService.find( dim ) );
        }

        if ( oldVectors.isEmpty() ) {
            throw new IllegalStateException( "No vectors" );
        }

        designElementDataVectorService.thaw( oldVectors );
        Map<QuantitationType, Collection<DesignElementDataVector>> qt2Vec = new HashMap<QuantitationType, Collection<DesignElementDataVector>>();
        Collection<QuantitationType> qtsToAdd = new HashSet<QuantitationType>();
        for ( DesignElementDataVector v : oldVectors ) {

            QuantitationType qt = v.getQuantitationType();
            if ( !qts.contains( qt ) ) {
                /*
                 * Guard against QTs that are broken. Sometimes the QTs for the EE don't include the ones that the DEDVs
                 * have, due to corruption.
                 */
                qtsToAdd.add( qt );
            }
            if ( !qt2Vec.containsKey( qt ) ) {
                qt2Vec.put( qt, new HashSet<DesignElementDataVector>() );
            }

            qt2Vec.get( qt ).add( v );
        }

        if ( !qtsToAdd.isEmpty() ) {
            expExp.getQuantitationTypes().addAll( qtsToAdd );
            log.info( "Adding " + qtsToAdd.size() + " missing quantitation types to experiment" );
            expressionExperimentService.update( expExp );
        }

        return qt2Vec;
    }

    /**
     * Make a (non-persistent) vector that has the right bioassaydimension, designelement and quantitationtype.
     * 
     * @param expExp
     * @param newBioAd
     * @param type
     * @param de
     * @return
     */
    private RawExpressionDataVector initializeNewVector( ExpressionExperiment expExp, BioAssayDimension newBioAd,
            QuantitationType type, CompositeSequence de ) {
        RawExpressionDataVector vector = RawExpressionDataVector.Factory.newInstance();
        vector.setBioAssayDimension( newBioAd );
        vector.setDesignElement( de );
        vector.setQuantitationType( type );
        vector.setExpressionExperiment( expExp );
        return vector;
    }

    /**
     * @param sortedOldDims
     * @param newBioAd
     * @param type
     * @param de
     * @param dedvs
     * @param data
     * @param mergedData
     * @return
     */
    private int makeMergedData( List<BioAssayDimension> sortedOldDims, BioAssayDimension newBioAd,
            QuantitationType type, CompositeSequence de, Collection<DesignElementDataVector> dedvs,
            List<Object> mergedData ) {
        int totalMissingInVector = 0;
        PrimitiveType representation = type.getRepresentation();

        for ( BioAssayDimension oldDim : sortedOldDims ) {
            // careful, the 'new' bioassaydimension might be one of the old ones that we're reusing.
            if ( oldDim.equals( newBioAd ) ) continue;
            boolean found = false;

            for ( DesignElementDataVector oldV : dedvs ) {
                assert oldV.getDesignElement().equals( de );
                assert oldV.getQuantitationType().equals( type );

                if ( oldV.getBioAssayDimension().equals( oldDim ) ) {
                    found = true;
                    convertFromBytes( mergedData, representation, oldV );
                    break;
                }
            }
            if ( !found ) {
                int missing = fillMissingValues( de, mergedData, oldDim, representation );
                totalMissingInVector += missing;
            }
        }
        return totalMissingInVector;
    }

    /**
     * Just for debugging.
     * 
     * @param newVectors
     */
    @SuppressWarnings("unused")
    private void print( Collection<DesignElementDataVector> newVectors ) {
        StringBuilder buf = new StringBuilder();
        ByteArrayConverter conv = new ByteArrayConverter();
        for ( DesignElementDataVector vector : newVectors ) {
            buf.append( vector.getDesignElement() );
            QuantitationType qtype = vector.getQuantitationType();
            if ( qtype.getRepresentation().equals( PrimitiveType.DOUBLE ) ) {
                double[] vals = conv.byteArrayToDoubles( vector.getData() );
                for ( double d : vals ) {
                    buf.append( "\t" + d );
                }
            } else if ( qtype.getRepresentation().equals( PrimitiveType.INT ) ) {
                int[] vals = conv.byteArrayToInts( vector.getData() );
                for ( int i : vals ) {
                    buf.append( "\t" + i );
                }
            } else if ( qtype.getRepresentation().equals( PrimitiveType.BOOLEAN ) ) {
                boolean[] vals = conv.byteArrayToBooleans( vector.getData() );
                for ( boolean d : vals ) {
                    buf.append( "\t" + d );
                }
            } else if ( qtype.getRepresentation().equals( PrimitiveType.STRING ) ) {
                String[] vals = conv.byteArrayToStrings( vector.getData() );
                for ( String d : vals ) {
                    buf.append( "\t" + d );
                }

            }
            buf.append( "\n" );
        }

        log.info( "\n" + buf );
    }

    /**
     * Provide a sorted list of bioassaydimensions for merging. The actual order doesn't matter, just so long as we are
     * consistent further on.
     * 
     * @param oldBioAssayDims
     * @return
     */
    private List<BioAssayDimension> sortedBioAssayDimensions( Collection<BioAssayDimension> oldBioAssayDims ) {
        List<BioAssayDimension> sortedOldDims = new ArrayList<BioAssayDimension>();
        sortedOldDims.addAll( oldBioAssayDims );
        Collections.sort( sortedOldDims, new Comparator<BioAssayDimension>() {
            @Override
            public int compare( BioAssayDimension o1, BioAssayDimension o2 ) {
                return o1.getId().compareTo( o2.getId() );
            }
        } );
        return sortedOldDims;
    }
}
