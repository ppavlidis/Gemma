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
package ubic.gemma.apps;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.analysis.stats.ExpressionDataSampleCorrelation;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Create correlation visualizations for expression experiments
 * 
 * @author paul
 * @version $Id$
 */
public class ExpressionDataCorrMatCli extends ExpressionExperimentManipulatingCLI {

    public static void main( String[] args ) {
        try {
            ExpressionDataCorrMatCli e = new ExpressionDataCorrMatCli();
            Exception ex = e.doWork( args );
            log.info( ex, ex );
        } catch ( Exception e ) {
            log.info( e, e );
        }
    }

    private FilterConfig filterConfig = new FilterConfig();

    private ExpressionDataMatrixService analysisHelperService = null;

    @Override
    public String getShortDesc() {
        return "Create visualizations of the sample correlation matrices for expression experiments";
    }

    @Override
    protected void buildOptions() {
        super.buildOptions();
        this.buildFilterConfigOptions();
    }

    @Override
    protected Exception doWork( String[] args ) {
        this.processCommandLine( "corrMat", args );

        for ( BioAssaySet ee : expressionExperiments ) {
            try {
                if ( !( ee instanceof ExpressionExperiment ) ) {
                    errorObjects.add( ee );
                    continue;
                }
                processExperiment( ( ExpressionExperiment ) ee );
                successObjects.add( ee );
            } catch ( Exception e ) {
                log.error( "Error while processing " + ee, e );
                errorObjects.add( ee );
            }

        }
        if ( expressionExperiments.size() > 1 ) summarizeProcessing();
        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        getFilterConfigOptions();
        analysisHelperService = ( ExpressionDataMatrixService ) this.getBean( "expressionDataMatrixService" );
    }

    /**
     * @param arrayDesign
     */
    private void audit( ExpressionExperiment ee, AuditEventType eventType ) {
        auditTrailService.addUpdateEvent( ee, eventType, "Generated correlation matrix images" );
        successObjects.add( ee.toString() );
    }

    @SuppressWarnings("static-access")
    private void buildFilterConfigOptions() {
        Option minPresentFraction = OptionBuilder.hasArg().withArgName( "Missing Value Threshold" ).withDescription(
                "Fraction of data points that must be present in a profile to be retained , default="
                        + FilterConfig.DEFAULT_MINPRESENT_FRACTION ).withLongOpt( "missingcut" ).create( 'm' );
        addOption( minPresentFraction );

        Option lowExpressionCut = OptionBuilder.hasArg().withArgName( "Expression Threshold" ).withDescription(
                "Fraction of expression vectors to reject based on low values, default="
                        + FilterConfig.DEFAULT_LOWEXPRESSIONCUT ).withLongOpt( "lowcut" ).create( 'l' );
        addOption( lowExpressionCut );

        Option lowVarianceCut = OptionBuilder.hasArg().withArgName( "Variance Threshold" ).withDescription(
                "Fraction of expression vectors to reject based on low variance (or coefficient of variation), default="
                        + FilterConfig.DEFAULT_LOWVARIANCECUT ).withLongOpt( "lowvarcut" ).create( "lv" );
        addOption( lowVarianceCut );
    }

    private void getFilterConfigOptions() {
        if ( hasOption( 'm' ) ) {
            filterConfig.setMinPresentFraction( Double.parseDouble( getOptionValue( 'm' ) ) );
        }
        if ( hasOption( 'l' ) ) {
            filterConfig.setLowExpressionCut( Double.parseDouble( getOptionValue( 'l' ) ) );
        }
        if ( hasOption( "lv" ) ) {
            filterConfig.setLowVarianceCut( Double.parseDouble( getOptionValue( "lv" ) ) );
        }
    }

    /**
     * @param ee
     */
    private void processExperiment( ExpressionExperiment ee ) {
        if ( !needToRun( ee, null ) ) {
            return;
        }
        ExpressionDataDoubleMatrix matrix = analysisHelperService.getFilteredMatrix( ee, filterConfig );
        log.info( filterConfig );
        ExpressionDataSampleCorrelation.process( matrix, ee );
        audit( ee, null );

    }

}
