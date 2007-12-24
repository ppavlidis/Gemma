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
package ubic.gemma.analysis.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.analysis.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.expression.analysis.ExpressionAnalysis;
import ubic.gemma.model.expression.analysis.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.analysis.ProbeAnalysisResult;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * A two way anova base class as described by P. Pavlidis, Methods 31 (2003) 282-289.
 * <p>
 * See http://www.bioinformatics.ubc.ca/pavlidis/lab/docs/reprints/anova-methods.pdf.
 * <p>
 * For specific implementations with and without interactions, see the {@link TwoWayAnovaWithInteractionsAnalyzer} and
 * {@link TwoWayAnovaWithoutInteractionsAnalyzer} respectively.
 * 
 * @author keshav
 * @version $Id$
 */
public abstract class AbstractTwoWayAnovaAnalyzer extends AbstractDifferentialExpressionAnalyzer {

    protected Collection<ExpressionAnalysisResultSet> resultSets = new HashSet<ExpressionAnalysisResultSet>();

    private final int mainEffectAIndex = 0;
    private final int mainEffectBIndex = 1;
    private final int mainEffectInteractionIndex = 2;
    private final int maxResults = 3;

    /**
     * Creates and returns an {@link ExpressionAnalysis} and fills in the expression analysis results.
     * 
     * @param dmatrix
     * @param filteredPvalues
     * @param filteredFStatistics
     * @param numResultsFromR
     * @param experimentalFactorA
     * @param experimentalFactorB
     * @param quantitationType
     * @return
     */
    protected DifferentialExpressionAnalysis createExpressionAnalysis( ExpressionDataDoubleMatrix dmatrix,
            double[] filteredPvalues, double[] filteredFStatistics, int numResultsFromR,
            ExperimentalFactor experimentalFactorA, ExperimentalFactor experimentalFactorB,
            QuantitationType quantitationType ) {

        /* Create the expression analysis and pack the results. */
        DifferentialExpressionAnalysis expressionAnalysis = DifferentialExpressionAnalysis.Factory.newInstance();

        Collection<ExpressionExperiment> experimentsAnalyzed = new HashSet<ExpressionExperiment>();
        expressionAnalysis.setExperimentsAnalyzed( experimentsAnalyzed );

        /* All results for the first main effect */
        List<DifferentialExpressionAnalysisResult> analysisResultsMainEffectA = new ArrayList<DifferentialExpressionAnalysisResult>();

        /* All results for the second main effect */
        List<DifferentialExpressionAnalysisResult> analysisResultsMainEffectB = new ArrayList<DifferentialExpressionAnalysisResult>();

        /* Interaction effect */
        List<DifferentialExpressionAnalysisResult> analysisResultsInteractionEffect = new ArrayList<DifferentialExpressionAnalysisResult>();

        int k = 0;
        for ( int i = 0; i < dmatrix.rows(); i++ ) {

            /* Each probe has all results (ie. 2 - without interactions; 3 - with interactions) */
            List<DifferentialExpressionAnalysisResult> analysisResultsPerProbe = new ArrayList<DifferentialExpressionAnalysisResult>();

            DesignElement de = dmatrix.getDesignElementForRow( i );

            CompositeSequence cs = ( CompositeSequence ) de;

            for ( int j = 0; j < numResultsFromR; j++ ) {

                ProbeAnalysisResult probeAnalysisResult = ProbeAnalysisResult.Factory.newInstance();
                probeAnalysisResult.setProbe( cs );
                probeAnalysisResult.setQuantitationType( quantitationType );

                probeAnalysisResult.setPvalue( filteredPvalues[k] );
                probeAnalysisResult.setScore( filteredFStatistics[k] );
                // probeAnalysisResult.setCorrectedPvalue( correctedPvalue );
                // probeAnalysisResult.setParameters( parameters );

                analysisResultsPerProbe.add( probeAnalysisResult );

                if ( j % numResultsFromR == mainEffectAIndex ) analysisResultsMainEffectA.add( probeAnalysisResult );

                if ( j % numResultsFromR == mainEffectBIndex ) analysisResultsMainEffectB.add( probeAnalysisResult );

                if ( j % numResultsFromR == mainEffectInteractionIndex )
                    analysisResultsInteractionEffect.add( probeAnalysisResult );

                k++;
            }

            ExpressionAnalysisResultSet resultSet = ExpressionAnalysisResultSet.Factory.newInstance(
                    analysisResultsPerProbe, expressionAnalysis, null );

            resultSets.add( resultSet );
        }

        /* main effects */
        Collection<ExperimentalFactor> mainA = new HashSet<ExperimentalFactor>();
        mainA.add( experimentalFactorA );
        ExpressionAnalysisResultSet mainEffectResultSetA = ExpressionAnalysisResultSet.Factory.newInstance(
                analysisResultsMainEffectA, expressionAnalysis, mainA );
        resultSets.add( mainEffectResultSetA );

        Collection<ExperimentalFactor> mainB = new HashSet<ExperimentalFactor>();
        mainB.add( experimentalFactorB );
        ExpressionAnalysisResultSet mainEffectResultSetB = ExpressionAnalysisResultSet.Factory.newInstance(
                analysisResultsMainEffectB, expressionAnalysis, mainB );
        resultSets.add( mainEffectResultSetB );

        if ( numResultsFromR == maxResults ) {
            Collection<ExperimentalFactor> interAB = new HashSet<ExperimentalFactor>();
            interAB.add( experimentalFactorA );
            interAB.add( experimentalFactorB );
            ExpressionAnalysisResultSet interactionEffectResultSet = ExpressionAnalysisResultSet.Factory.newInstance(
                    analysisResultsInteractionEffect, expressionAnalysis, interAB );
            resultSets.add( interactionEffectResultSet );
        }

        expressionAnalysis.setResultSets( resultSets );
        this.setAnalysisMetadata( expressionAnalysis, this.getClass().getSimpleName(), DIFFERENTIAL_EXPRESSION );

        return expressionAnalysis;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.AbstractAnalyzer#getExpressionAnalysis(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public DifferentialExpressionAnalysis getDifferentialExpressionAnalysis( ExpressionExperiment expressionExperiment ) {

        Collection<ExperimentalFactor> experimentalFactors = expressionExperiment.getExperimentalDesign()
                .getExperimentalFactors();

        if ( experimentalFactors.size() != mainEffectInteractionIndex )
            throw new RuntimeException( "Two way anova supports 2 experimental factors.  Received "
                    + experimentalFactors.size() + "." );

        Iterator iter = experimentalFactors.iterator();
        ExperimentalFactor experimentalFactorA = ( ExperimentalFactor ) iter.next();
        ExperimentalFactor experimentalFactorB = ( ExperimentalFactor ) iter.next();

        return twoWayAnova( expressionExperiment, experimentalFactorA, experimentalFactorB );
    }

    /**
     * Two be implemented by the two way anova analyzer.
     * <p>
     * See class level javadoc of two way anova anlayzer for R Call.
     * 
     * @param expressionExperiment
     * @param experimentalFactorA
     * @param experimentalFactorB
     * @return
     */
    public abstract DifferentialExpressionAnalysis twoWayAnova( ExpressionExperiment expressionExperiment,
            ExperimentalFactor experimentalFactorA, ExperimentalFactor experimentalFactorB );

}
