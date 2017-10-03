/*
 * The Gemma project
 * 
 * Copyright (c) 2008-2009 University of British Columbia
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

package ubic.gemma.core.visualization;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrixFactory;
import ubic.basecode.graphics.ColorMap;
import ubic.basecode.graphics.ColorMatrix;
import ubic.basecode.graphics.MatrixDisplay;
import ubic.gemma.core.datastructure.matrix.EmptyExpressionMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrixColumnSort;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionValueObject;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tools for visualizing experimental designs. The idea is to generate a overview of the design that can be put over
 * heat maps or line graphs.
 *
 * @author paul
 */
@Component
public class ExperimentalDesignVisualizationServiceImpl implements ExperimentalDesignVisualizationService {

    /**
     * Cache of layouts for experiments, keyed by experiment ID. TODO: use ehcache so we can manage this.
     */
    private final Map<Long, LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>>> cachedLayouts = new ConcurrentHashMap<>();
    private final Log log = LogFactory.getLog( getClass().getName() );
    private final ExpressionExperimentService expressionExperimentService;

    @Autowired
    public ExperimentalDesignVisualizationServiceImpl( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    @Override
    public void clearCaches() {
        this.cachedLayouts.clear();
    }

    @Override
    public void clearCaches( Long eeId ) {
        this.clearCachedLayouts( eeId );
    }

    @Override
    public Map<Long, LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>>> sortVectorDataByDesign(
            Collection<DoubleVectorValueObject> dedVs ) {

        // cachedLayouts.clear(); // uncomment FOR DEBUGGING.

        if ( dedVs == null ) {
            return new HashMap<>( 0 );
        }

        Map<Long, LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>>> returnedLayouts = new HashMap<>(
                dedVs.size() );

        StopWatch timer = new StopWatch();
        timer.start();

        /*
         * This is shared across experiments that might show up in the dedVs; this should be okay...saves computation.
         * This is the only slow part.
         */
        prepare( dedVs );

        /*
         * This loop is not a performance issue.
         */
        Map<DoubleVectorValueObject, List<BioAssayValueObject>> newOrderingsForBioAssayDimensions = new HashMap<>();
        for ( DoubleVectorValueObject vec : dedVs ) {

            if ( vec.isReorganized() ) {
                continue;
            }

            assert !vec.getBioAssays().isEmpty();

            LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> layout;

            if ( cachedLayouts.containsKey( vec.getExpressionExperiment().getId() ) ) {
                layout = cachedLayouts.get( vec.getExpressionExperiment().getId() );
            } else {
                // subset.
                assert vec.getExpressionExperiment().getSourceExperiment() != null;
                layout = cachedLayouts.get( vec.getExpressionExperiment().getSourceExperiment() );
            }

            assert layout != null;
            assert !layout.isEmpty();

            List<BioAssayValueObject> newOrdering = new ArrayList<>( layout.keySet() );

            /*
             * FIXME we don't want to do this? We want to fill in NaN for values that are gaps when there are multiple
             * bioassay dimensions.
             */
            newOrdering.retainAll( vec.getBioAssays() );

            /*
             * This can happen if the vectors are out of whack with the bioassays - e.g. two platforms were used but
             * merging is not done. See bug 3775. Skipping the ordering is not the right thing to do.
             */
            if ( newOrdering.isEmpty() ) {

                boolean allNaN = allNaN( vec );

                if ( allNaN ) {
                    // reordering will have no effect.
                    continue;
                }

                /*
                 * Add to the layout.
                 */
                layout = extendLayout( vec, vec.getExpressionExperiment().getId() );
                newOrdering = new ArrayList<>( layout.keySet() );
                newOrdering.retainAll( vec.getBioAssays() );
                assert !newOrdering.isEmpty();
            }

            newOrderingsForBioAssayDimensions.put( vec, newOrdering );

            Map<BioAssayValueObject, Integer> ordering = getOrdering( newOrdering );

            Long eeId;
            eeId = vec.getExpressionExperiment().getId(); // might be subset id.

            if ( !returnedLayouts.containsKey( eeId ) ) {
                if ( vec.isSliced() ) {
                    LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> trimmedLayout = new LinkedHashMap<>();

                    for ( BioAssayValueObject baVo : newOrdering ) {
                        trimmedLayout.put( baVo, layout.get( baVo ) );
                    }

                    returnedLayouts.put( eeId, trimmedLayout );

                } else {
                    returnedLayouts.put( eeId, layout );
                }
            }

            /*
             * Might be a faster way.
             */
            double[] data = vec.getData();
            double[] dol = ArrayUtils.clone( data );

            // assert ordering.size() == data.length : "got " + ordering.size() + " expected " + data.length;

            List<BioAssayValueObject> oldOrdering = vec.getBioAssayDimension().getBioAssays();
            int j = 0;
            if ( log.isTraceEnabled() )
                log.trace( "Old order: " + StringUtils.join( ArrayUtils.toObject( data ), "," ) );
            for ( BioAssayValueObject ba : oldOrdering ) {

                if ( ordering.get( ba ) == null ) {
                    assert Double.isNaN( dol[j] );
                    j++;
                    continue;
                }

                assert ordering.containsKey( ba );
                assert ordering.get( ba ) != null;

                Integer targetIndex = ordering.get( ba );

                data[targetIndex] = dol[j++];

            }
            if ( log.isTraceEnabled() )
                log.trace( "New order: " + StringUtils.join( ArrayUtils.toObject( data ), "," ) );

            vec.setReorganized( true );

        }

        for ( DoubleVectorValueObject vec : dedVs ) {
            if ( vec.getBioAssayDimension().isReordered() )
                continue;
            List<BioAssayValueObject> newOrdering = newOrderingsForBioAssayDimensions.get( vec );
            if ( newOrdering == null )
                continue; // data was empty, etc.
            vec.getBioAssayDimension().reorder( newOrdering );
        }

        if ( timer.getTime() > 1500 ) {
            log.info( "Sort vectors by design: " + timer.getTime() + "ms" );
        }

        return returnedLayouts;

    }

    /**
     * Test method for now, shows how this can be used.
     * @param e ee
     */
    @SuppressWarnings("unused") // Test method for now, shows how this can be used.
    protected void plotExperimentalDesign( ExpressionExperiment e ) {
        LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> layout = getExperimentalDesignLayout(
                e );

        List<String> efStrings = new ArrayList<>();
        List<String> baStrings = new ArrayList<>();
        List<double[]> rows = new ArrayList<>();
        boolean first = true;
        int i = 0;
        for ( BioAssayValueObject ba : layout.keySet() ) {
            baStrings.add( ba.getName() );

            int j = 0;
            for ( ExperimentalFactor ef : layout.get( ba ).keySet() ) {
                if ( first ) {
                    double[] nextRow = new double[layout.size()];
                    rows.add( nextRow );
                    efStrings.add( ef.getName() + " ( id=" + ef.getId() + ")" ); // make sure they are unique.
                }
                double d = layout.get( ba ).get( ef );

                rows.get( j )[i] = d;
                j++;
            }
            i++;
            first = false;
        }

        double[][] mat = rows.toArray( new double[][] {} );

        DoubleMatrix<String, String> data = DoubleMatrixFactory.dense( mat );
        data.setRowNames( efStrings );
        data.setColumnNames( baStrings );

        ColorMatrix<String, String> cm = new ColorMatrix<>( data, ColorMap.GREENRED_COLORMAP, Color.GRAY );

        try {
            writeImage( cm, File.createTempFile( e.getShortName() + "_", ".png" ) );
        } catch ( IOException e1 ) {
            throw new RuntimeException( e1 );
        }
    }

    private boolean allNaN( DoubleVectorValueObject vec ) {
        boolean allNaN = true;
        for ( double d : vec.getData() ) {
            if ( !Double.isNaN( d ) ) {
                allNaN = false;
                break;
            }
        }
        return allNaN;
    }

    private void clearCachedLayouts( Long eeId ) {
        this.cachedLayouts.remove( eeId );
    }

    /**
     * See bug 3775. For experiments which have more than one bioassay dimension, we typically have to "extend" the
     * layout to include more bioassays. Because the ordering is defined by the factor values associated with the
     * underlying biomaterials, this is going to be okay.
     */
    private LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> extendLayout(
            DoubleVectorValueObject vec, Long eeId ) {
        BioAssayDimensionValueObject bioAssayDimension = getBioAssayDimensionForVector( vec );

        ExpressionExperimentValueObject ee = vec.getExpressionExperiment();
        ExpressionExperiment actualEe = getExperimentForVector( vec, ee );

        LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> extension = getExperimentalDesignLayout(
                actualEe, expressionExperimentService.getBioAssayDimensions( actualEe ) );

        for ( BioAssayValueObject vbaVo : bioAssayDimension.getBioAssays() ) {
            assert extension.containsKey( vbaVo );
        }

        for ( BioAssayValueObject vbaVo : vec.getBioAssays() ) {
            assert extension.containsKey( vbaVo );
        }

        cachedLayouts.get( eeId ).putAll( extension );

        return cachedLayouts.get( eeId );
    }

    private BioAssayDimensionValueObject getBioAssayDimensionForVector( DoubleVectorValueObject vec ) {
        BioAssayDimensionValueObject bioAssayDimension = vec.getBioAssayDimension();

        if ( vec.getBioAssayDimension().getSourceBioAssayDimension() != null ) {
            bioAssayDimension = vec.getBioAssayDimension().getSourceBioAssayDimension();
        }

        assert bioAssayDimension.getId() != null;

        // this actually doesn't really matter, but we're wasting time redoing it.
        assert !bioAssayDimension.isReordered();
        assert !bioAssayDimension.getIsSubset();
        return bioAssayDimension;
    }

    private LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> getExperimentalDesignLayout(
            ExpressionExperiment e ) {

        if ( cachedLayouts.containsKey( e.getId() ) ) {
            return cachedLayouts.get( e.getId() );
        }

        Collection<BioAssayDimension> bds = expressionExperimentService.getBioAssayDimensions( e );

        /*
         * FIXME if there are multiple bioassay dimensions...they had better match up. This should be the case, but
         * might not be if curation is incomplete.
         */
        // if ( bds.size() > 1 ) {
        // log.debug( "More than one bioassay dimension for visualization, only using the first one" );
        // }

        // BioAssayDimensionValueObject bd = new BioAssayDimensionValueObject( bds.iterator().next() );

        // needed?
        e = this.expressionExperimentService.thawLite( e );

        LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> result = this
                .getExperimentalDesignLayout( e, bds );

        cachedLayouts.put( e.getId(), result );

        return result;
    }

    /**
     * @param bds a BioAssayDimension that represents the BioAssayDimensionValueObject. This is only needed to avoid
     *            making ExpressionMatrix use value objects, otherwise we could use the BioAssayDimensionValueObject
     * @return A "Layout": a map of bioassays to map of factors to doubles that represent the position in the layout.
     */
    private LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> getExperimentalDesignLayout(
            ExpressionExperiment experiment, Collection<BioAssayDimension> bds ) {

        LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> result = new LinkedHashMap<>();

        ExpressionDataMatrix<Object> mat = new EmptyExpressionMatrix( bds );

        // This is the place the actual sort order is determined.
        List<BioMaterial> bms = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( mat );

        Map<Long, Double> fvV = new HashMap<>();

        assert experiment != null;
        assert experiment.getExperimentalDesign() != null;
        if ( experiment.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
            // Case of no experimental design; just put in a dummy factor.
            ExperimentalFactor dummyFactor = ExperimentalFactor.Factory.newInstance();
            dummyFactor.setName( "No factors" );
            for ( BioMaterial bm : bms ) {
                int j = mat.getColumnIndex( bm );

                Collection<BioAssay> bas = mat.getBioAssaysForColumn( j );

                for ( BioAssay ba : bas ) {
                    BioAssayValueObject baVo = new BioAssayValueObject( ba );
                    result.put( baVo, new LinkedHashMap<ExperimentalFactor, Double>() );
                    result.get( baVo ).put( dummyFactor, 0.0 );
                }

            }

            return result;
        }

        assert !experiment.getExperimentalDesign().getExperimentalFactors().isEmpty();
        /*
         * Choose values to use as placeholders.
         */
        // Map<ExperimentalFactor, Map<FactorValue, Double>> continuousRanges = new HashMap<>();
        for ( ExperimentalFactor ef : experiment.getExperimentalDesign().getExperimentalFactors() ) {
            if ( ef.getFactorValues().isEmpty() ) {
                // this can happen if the design isn't complete.
                continue;
            }

            for ( FactorValue fv : ef.getFactorValues() ) {
                assert fv.getId() != null;
                // the id is just used as a convenience.
                fvV.put( fv.getId(), new Double( fv.getId() ) );

            }

        }

        assert !fvV.isEmpty();
        assert !bms.isEmpty();

        // if the same biomaterial was used in more than one bioassay (thus more than one bioassay dimension), and they
        // are in the same column, this is resolved here; we assign the same layout value for both bioassays, so the
        // ordering is the same for vectors coming from
        // either bioassay dimension.
        for ( BioMaterial bm : bms ) {
            int j = mat.getColumnIndex( bm );

            Collection<BioAssay> bas = mat.getBioAssaysForColumn( j );

            Collection<FactorValue> fvs = bm.getFactorValues();

            for ( BioAssay ba : bas ) {
                BioAssayValueObject baVo = new BioAssayValueObject( ba );
                result.put( baVo, new LinkedHashMap<ExperimentalFactor, Double>( fvs.size() ) );
                for ( FactorValue fv : fvs ) {
                    assert fv.getId() != null;
                    assert fvV.containsKey( fv.getId() );
                    ExperimentalFactor ef = fv.getExperimentalFactor();
                    Double value;
                    if ( fv.getMeasurement() != null ) {
                        try {
                            value = Double.parseDouble( fv.getMeasurement().getValue() );
                        } catch ( NumberFormatException e ) {
                            value = fvV.get( fv.getId() ); // not good.
                        }
                    } else {
                        value = fvV.get( fv.getId() );
                    }
                    assert result.containsKey( baVo );
                    assert value != null;
                    result.get( baVo ).put( ef, value );

                }
            }

        }
        return result;
    }

    /**
     * @return the experiment; if the vector is for a subset, we return the source experiment
     */
    private ExpressionExperiment getExperimentForVector( DoubleVectorValueObject vec,
            ExpressionExperimentValueObject ee ) {
        /*
         * The following is the really slow part if we don't use a cache.
         */
        ExpressionExperiment actualEe;
        if ( vec.isSliced() ) {
            /*
             * Then we are looking at a subset, so associate it with the original experiment.
             */
            assert vec.getExpressionExperiment().getSourceExperiment() != null;
            actualEe = expressionExperimentService.load( vec.getExpressionExperiment().getSourceExperiment() );
            actualEe = expressionExperimentService.thawLiter( actualEe );
        } else {
            actualEe = expressionExperimentService.load( ee.getId() );
            actualEe = expressionExperimentService.thawLiter( actualEe );
            // plotExperimentalDesign( ee ); // debugging/testing
        }
        return actualEe;
    }

    /**
     * Get the order that bioassays need to be in for the 'real' data.
     */
    private Map<BioAssayValueObject, Integer> getOrdering( List<BioAssayValueObject> bioAssaysInOrder ) {
        assert !bioAssaysInOrder.isEmpty();
        Map<BioAssayValueObject, Integer> ordering = new HashMap<>();

        int i = 0;
        for ( BioAssayValueObject bbb : bioAssaysInOrder ) {
            ordering.put( bbb, i++ );
        }
        return ordering;
    }

    /**
     * Gets the bioassay dimensions for the experiments associated with the given vectors. These are cached for later
     * re-use.
     */
    private void prepare( Collection<DoubleVectorValueObject> dvvOs ) {

        if ( dvvOs == null )
            return;

        for ( DoubleVectorValueObject vec : dvvOs ) {
            if ( vec == null ) {
                log.debug( "DoubleVectorValueObject is null" );
                continue;
            }

            if ( vec.isReorganized() ) {
                // wouldn't normally be the case...
                continue;
            }

            ExpressionExperimentValueObject ee = vec.getExpressionExperiment();

            /*
             * Problem: we can't have two layouts for one experiment, which is actually required if there is more than
             * one bioassay dimension. However, this rarely matters. See bug 3775
             */
            if ( cachedLayouts.containsKey( ee.getId() ) || ( ee.getSourceExperiment() != null && cachedLayouts
                    .containsKey( ee.getSourceExperiment() ) ) ) {
                continue;
            }

            BioAssayDimensionValueObject bioAssayDimension = getBioAssayDimensionForVector( vec );

            ExpressionExperiment actualEe = getExperimentForVector( vec, ee );

            assert bioAssayDimension != null;
            LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> experimentalDesignLayout = getExperimentalDesignLayout(
                    actualEe, expressionExperimentService.getBioAssayDimensions( actualEe ) );

            cachedLayouts.put( ee.getId(), experimentalDesignLayout );

        }

    }

    /**
     * Test method.
     */
    private void writeImage( ColorMatrix<String, String> matrix, File outputFile ) throws IOException {
        MatrixDisplay<String, String> writer = new MatrixDisplay<>( matrix );
        writer.setCellSize( new Dimension( 18, 18 ) );
        writer.saveImage( matrix, outputFile.getAbsolutePath(), true, false, true );
    }

}
