/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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
/**
 * 
 */
package ubic.gemma.apps;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.analysis.preprocess.TwoChannelMissingValues;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.PersisterHelper;

/**
 * CLI for computing and persisting the 'present' calls for two-channel data
 * 
 * @author Paul
 * @version $Id$
 */
public class TwoChannelMissingValueCLI extends ExpressionExperimentManipulatingCli {

    /**
     * 
     */
    private static final double DEFAULT_SIGNAL_TO_NOISE_THRESHOLD = 2.0;
    private double s2n = DEFAULT_SIGNAL_TO_NOISE_THRESHOLD;
    private boolean doAll = false;
    private boolean force = false;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();

        Option signal2noiseOption = OptionBuilder.hasArg().withArgName( "Signal-to-noise" ).withDescription(
                "Signal to noise ratio, below which values are considered missing; default="
                        + DEFAULT_SIGNAL_TO_NOISE_THRESHOLD ).withLongOpt( "signal2noise" ).create( 's' );

        addOption( signal2noiseOption );

        Option doAllOption = OptionBuilder.withDescription( "Process all two-color experiments" ).create( "all" );

        addOption( doAllOption );

        Option force = OptionBuilder.withDescription(
                "Replace existing missing value data (two-color experiments only)" ).create( "force" );

        addOption( force );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "Two-channel missing values", args );
        if ( err != null ) return err;

        if ( doAll ) {

            Collection<String> errorObjects = new HashSet<String>();
            Collection<String> persistedObjects = new HashSet<String>();

            Collection<ExpressionExperiment> ees = this.getExpressionExperimentService().loadAll();
            for ( ExpressionExperiment ee : ees ) {

                boolean hasTwoColor = false;
                Collection<ArrayDesign> arrayDesignsUsed = this.getExpressionExperimentService().getArrayDesignsUsed(
                        ee );
                for ( ArrayDesign design : arrayDesignsUsed ) {
                    TechnologyType tt = design.getTechnologyType();
                    if ( tt == TechnologyType.TWOCOLOR || tt == TechnologyType.DUALMODE ) {
                        hasTwoColor = true;
                        break;
                    }
                }

                if ( !hasTwoColor ) {
                    continue;
                }

                log.info( ee + " uses a two-color array design, processing..." );

                try {
                    processExperiment( ee );
                    persistedObjects.add( ee.toString() );
                } catch ( Exception e ) {
                    errorObjects.add( ee + ": " + e.getMessage() );
                    log.error( "**** Exception while processing " + ee + ": " + e.getMessage() + " ********" );
                }
            }

            summarizeProcessing( errorObjects, persistedObjects );

        } else {
            ExpressionExperiment ee = locateExpressionExperiment( this.getExperimentShortName() );

            if ( ee == null ) {
                log.error( "No expression experiment with name " + this.getExperimentShortName() );
                bail( ErrorCode.INVALID_OPTION );
            }

            processExperiment( ee );

        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private void processExperiment( ExpressionExperiment ee ) {

        Collection<QuantitationType> types = this.getExpressionExperimentService().getQuantitationTypes( ee );

        if ( !force ) {
            boolean hasMissingValues = false;
            for ( QuantitationType qType : types ) {
                if ( qType.getType() == StandardQuantitationType.PRESENTABSENT ) {
                    hasMissingValues = true;
                }
            }

            if ( hasMissingValues ) {
                log.warn( ee + " already has missing value vectors" );
                return;
            }
        } // FIXME: delete old values if force.

        log.info( "Got " + ee + ", thawing..." );
        this.getExpressionExperimentService().thaw( ee );
        TwoChannelMissingValues tcmv = new TwoChannelMissingValues();

        log.info( "Checking for existing missing value data.." );

        Collection<DesignElementDataVector> vectors = tcmv.computeMissingValues( ee, s2n );

        PersisterHelper persisterHelper = this.getPersisterHelper();

        log.info( "Persisting results..." );
        for ( DesignElementDataVector vector : vectors ) {
            vector.setQuantitationType( ( QuantitationType ) persisterHelper.persist( vector.getQuantitationType() ) );
        }

        ee.getDesignElementDataVectors().addAll( vectors );
        this.getExpressionExperimentService().update( ee );
    }

    public static void main( String[] args ) {
        TwoChannelMissingValueCLI p = new TwoChannelMissingValueCLI();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 's' ) ) {
            this.s2n = this.getDoubleOptionValue( 's' );
        }
        if ( this.hasOption( "all" ) ) {
            this.doAll = true;
        }
        if ( hasOption( "force" ) ) {
            this.force = true;
        }
        // this.designElementDataVectorService = ( DesignElementDataVectorService ) getBean(
        // "designElementDataVectorService" );
    }
}
