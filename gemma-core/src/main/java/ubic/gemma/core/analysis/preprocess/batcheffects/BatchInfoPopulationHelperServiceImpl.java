/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.analysis.preprocess.batcheffects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.text.DateFormat;
import java.util.*;

/**
 * @author paul
 */
@Service
public class BatchInfoPopulationHelperServiceImpl implements BatchInfoPopulationHelperService {

    /**
     * How many hours do we allow to pass between samples, before we consider them to be a separate batch (if they are
     * not run on the same day). This 'slack' is necessary to allow for the possibility that all the hybridizations were
     * run together, but the scanning took a while to complete. Of course we are still always recording the actual
     * dates, this is only used for the creation of ExperimentalFactors. Making this value too small causes the data to
     * be broken into many batches. I experimented with a value of 2, but this seemed too low. Anything greater than 24
     * doesn't make much sense.
     */
    private static final int MAX_GAP_BETWEEN_SAMPLES_TO_BE_SAME_BATCH = 8;

    private static final Log log = LogFactory.getLog( BatchInfoPopulationHelperServiceImpl.class );

    @Autowired
    private BioMaterialService bioMaterialService = null;

    @Autowired
    private ExperimentalDesignService experimentalDesignService;

    @Autowired
    private ExpressionExperimentService experimentService;

    @Override
    @Transactional
    public ExperimentalFactor createBatchFactor( ExpressionExperiment ee, Map<BioMaterial, Date> dates ) {

        /*
         * Go through the dates and convert to factor values.
         */
        Collection<Date> allDates = new HashSet<>( dates.values() );

        Map<String, Collection<Date>> datesToBatch = this.convertDatesToBatches( allDates );

        Map<Date, FactorValue> d2fv = new HashMap<>();
        ExperimentalFactor ef = null;
        if ( datesToBatch == null || datesToBatch.size() < 2 ) {
            if ( datesToBatch != null ) {
                BatchInfoPopulationHelperServiceImpl.log.info( "There is only one 'batch'" );
            }
            // we still put the processing dates in, below.
        } else {
            ef = this.makeFactorForBatch( ee );
            // assert ef.getId() != null;

            for ( String batchId : datesToBatch.keySet() ) {
                FactorValue fv = FactorValue.Factory.newInstance();
                fv.setIsBaseline( false ); /* we could set true for the first batch, but nobody cares. */
                fv.setValue( batchId );
                Collection<Characteristic> chars = new HashSet<>();
                VocabCharacteristic c = VocabCharacteristic.Factory.newInstance();
                c.setCategory( ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_NAME );
                c.setValue( batchId );
                c.setCategoryUri( ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_URI );
                c.setEvidenceCode( GOEvidenceCode.IIA );

                chars.add( c );
                fv.setCharacteristics( chars );
                fv.setExperimentalFactor( ef );

                /*
                 * persist
                 */
                fv.setCharacteristics( chars );
                experimentService.addFactorValue( ee, fv );
                for ( Date d : datesToBatch.get( batchId ) ) {
                    d2fv.put( d, fv );
                }
            }
        }

        bioMaterialService.associateBatchFactor( dates, d2fv );

        return ef;
    }

    /**
     * Apply some heuristics to condense the dates down to batches. For example, we might assume dates very close
     * together (for example, in the same day or within MAX_GAP_BETWEEN_SAMPLES_TO_BE_SAME_BATCH, and we avoid singleton
     * batches) are to be treated as the same batch (see implementation for details).
     *
     * @param allDates all dates
     * @return map
     */
    Map<String, Collection<Date>> convertDatesToBatches( Collection<Date> allDates ) {
        List<Date> lDates = new ArrayList<>( allDates );
        Collections.sort( lDates );
        Map<String, Collection<Date>> result = new LinkedHashMap<>();

        int batchNum = 1;
        DateFormat df = DateFormat.getDateInstance( DateFormat.SHORT );
        String batchDateString = "";

        boolean mergedAnySingletons = false;

        /*
         * Iterate over dates
         */
        Date lastDate = null;
        Date nextDate;
        for ( int i = 0; i < lDates.size(); i++ ) {
            Date currentDate = lDates.get( i );

            if ( i < lDates.size() - 1 ) {
                nextDate = lDates.get( i + 1 );
            } else {
                nextDate = null;
            }

            if ( lastDate == null ) {
                // Start our first batch.
                batchDateString = this.formatBatchName( batchNum, df, currentDate );
                result.put( batchDateString, new HashSet<Date>() );
                result.get( batchDateString ).add( currentDate );
                lastDate = currentDate;
                continue;
            }

            /*
             * Decide whether we have entered a new batch.
             *
             * Rules:
             *
             * - Processing on the same day is always considered the same batch.
             *
             * - Gaps of less then MAX_GAP_BETWEEN_SAMPLES_TO_BE_SAME_BATCH hours are considered the same batch even if
             * the day is different. Allows for "overnight running" of batches.
             *
             * And then two rules that keep us from having batches with just one sample. Such batches buy us nothing at
             * all.
             *
             * - A "singleton" batch at the end of the series is always combined with the last batch.
             *
             * - A "singleton" batch in the middle is combined with either the next or previous batch, whichever is
             * nearer in time.
             */
            if ( this.gapIsLarge( lastDate, currentDate ) && result.get( batchDateString ).size() > 1 ) {

                if ( nextDate == null ) {
                    /*
                     * We're at the last sample, and it's a singleton. We fall through and allow adding it to the end of
                     * the last batch.
                     */
                    BatchInfoPopulationHelperServiceImpl.log
                            .warn( "Singleton at the end of the series, combining with the last batch: gap is " + String
                                    .format( "%.2f",
                                            ( currentDate.getTime() - lastDate.getTime() ) / ( double ) ( 1000 * 60 * 60
                                                    * 24 ) ) + " hours." );
                    mergedAnySingletons = true;
                } else if ( this.gapIsLarge( currentDate, nextDate ) ) {
                    /*
                     * Then we have a singleton that will be stranded when we go to the next date. Do we combine it
                     * forwards or backwards? We choose the smaller gap.
                     */
                    long backwards = currentDate.getTime() - lastDate.getTime();
                    long forwards = nextDate.getTime() - currentDate.getTime();

                    if ( forwards < backwards ) {
                        // Start a new batch.
                        BatchInfoPopulationHelperServiceImpl.log
                                .warn( "Singleton resolved by waiting for the next batch: gap is " + String
                                        .format( "%.2f",
                                                ( nextDate.getTime() - currentDate.getTime() ) / ( double ) ( 1000 * 60
                                                        * 60 * 24 ) ) + " hours." );
                        batchNum++;
                        batchDateString = this.formatBatchName( batchNum, df, currentDate );
                        result.put( batchDateString, new HashSet<Date>() );
                        mergedAnySingletons = true;
                    } else {
                        BatchInfoPopulationHelperServiceImpl.log
                                .warn( "Singleton resolved by adding to the last batch: gap is " + String
                                        .format( "%.2f",
                                                ( currentDate.getTime() - lastDate.getTime() ) / ( double ) ( 1000 * 60
                                                        * 60 * 24 ) ) + " hours." );
                        // don't start a new batch, fall through.
                    }

                } else {
                    batchNum++;
                    batchDateString = this.formatBatchName( batchNum, df, currentDate );
                    result.put( batchDateString, new HashSet<Date>() );
                }

            }
            // else we fall through and add the current date to the current batch.

            // express the constraint that we don't allow batches of size 1, even if we would have normally left it in
            // its own batch.
            if ( result.get( batchDateString ).size() == 1 && this.gapIsLarge( lastDate, currentDate ) ) {
                mergedAnySingletons = true;
                BatchInfoPopulationHelperServiceImpl.log
                        .warn( "Stranded singleton automatically being merged into a larger batch" );
            }

            result.get( batchDateString ).add( currentDate );
            lastDate = currentDate;
        }

        if ( mergedAnySingletons && result.size() == 1 ) {
            // The implication is that if we didn't have the singleton merging, we would have more than one batch.
            BatchInfoPopulationHelperServiceImpl.log.warn( "Singleton merging resulted in all batches being combined" );
        }

        return result;

    }

    private ExperimentalFactor makeFactorForBatch( ExpressionExperiment ee ) {
        ExperimentalDesign ed = ee.getExperimentalDesign();
        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
        ef.setType( FactorType.CATEGORICAL );
        ef.setCategory( this.getBatchFactorCategory() );
        ef.setExperimentalDesign( ed );
        ef.setName( ExperimentalDesignUtils.BATCH_FACTOR_NAME );
        ef.setDescription(
                "Scan date or similar proxy for 'sample processing batch'" + " extracted from the raw data files." );

        ef = this.persistFactor( ee, ef );
        return ef;
    }

    private ExperimentalFactor persistFactor( ExpressionExperiment ee, ExperimentalFactor factor ) {
        ExperimentalDesign ed = experimentalDesignService.load( ee.getExperimentalDesign().getId() );

        if ( ed == null ) {
            throw new IllegalStateException( "No experimental design for " + ee );
        }

        return this.experimentService.addFactor( ee, factor );

    }

    private String formatBatchName( int batchNum, DateFormat df, Date d ) {
        String batchDateString;
        batchDateString = ExperimentalDesignUtils.BATCH_FACTOR_NAME_PREFIX + StringUtils
                .leftPad( Integer.toString( batchNum ), 2, "0" ) + "_" + df
                .format( DateUtils.truncate( d, Calendar.HOUR ) );
        return batchDateString;
    }

    /**
     * @param earlierDate earlier date
     * @param date        data
     * @return false if 'date' is considered to be in the same batch as 'earlierDate', true if we should treat it as a
     * separate batch.
     */
    private boolean gapIsLarge( Date earlierDate, Date date ) {
        return !DateUtils.isSameDay( date, earlierDate ) && DateUtils
                .addHours( earlierDate, BatchInfoPopulationHelperServiceImpl.MAX_GAP_BETWEEN_SAMPLES_TO_BE_SAME_BATCH )
                .before( date );
    }

    private VocabCharacteristic getBatchFactorCategory() {
        VocabCharacteristic c = VocabCharacteristic.Factory.newInstance();
        c.setCategory( ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_NAME );
        c.setValue( ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_NAME );
        c.setValueUri( ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_URI );
        c.setCategoryUri( ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_URI );
        c.setEvidenceCode( GOEvidenceCode.IIA );
        return c;
    }

}