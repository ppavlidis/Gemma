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
package ubic.gemma.analysis.expression.diff;

import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * A two way anova implementation with interactions as described by P. Pavlidis, Methods 31 (2003) 282-289.
 * <p>
 * See http://www.bioinformatics.ubc.ca/pavlidis/lab/docs/reprints/anova-methods.pdf.
 * <p>
 * R Call:
 * <p>
 * apply(matrix,1,function(x){anova(aov(x~farea+ftreat+farea*ftreat))$Pr})
 * <p>
 * apply(matrix,1,function(x){anova(aov(x~farea+ftreat+farea*ftreat))$F})
 * <p>
 * where area and treat are first transposed and then factor is called on each to give farea and ftreat.
 * <p>
 * qvalue(pvals)$qvalues
 * 
 * @spring.bean id="twoWayAnovaWithInteractionsAnalyzer"
 * @author keshav
 * @version $Id$
 * @see AbstractTwoWayAnovaAnalyzer
 */
public class TwoWayAnovaWithInteractionsAnalyzer extends AbstractTwoWayAnovaAnalyzer {

    private Log log = LogFactory.getLog( this.getClass() );

    private static final int ACTUAL_NUM_RESULTS = 3;
    private static final int NUM_RESULTS_FROM_R = ACTUAL_NUM_RESULTS + 1;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.AbstractTwoWayAnovaAnalyzer#twoWayAnova(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      ubic.gemma.model.common.quantitationtype.QuantitationType,
     *      ubic.gemma.model.expression.bioAssayData.BioAssayDimension,
     *      ubic.gemma.model.expression.experiment.ExperimentalFactor,
     *      ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    @Override
    public DifferentialExpressionAnalysis twoWayAnova( ExpressionExperiment expressionExperiment,
            ExperimentalFactor experimentalFactorA, ExperimentalFactor experimentalFactorB ) {

        connectToR();

        Collection<FactorValue> factorValuesA = experimentalFactorA.getFactorValues();
        Collection<FactorValue> factorValuesB = experimentalFactorB.getFactorValues();

        if ( factorValuesA.size() < 2 || factorValuesB.size() < 2 ) {
            throw new RuntimeException(
                    "Two way anova requires 2 or more factor values per experimental factor.  Received "
                            + factorValuesA.size() + " for either experimental factor " + experimentalFactorA.getName()
                            + " or experimental factor " + experimentalFactorB.getName() + "." );
        }

        Collection<DesignElementDataVector> vectorsToUse = analysisHelperService
                .getUsefulVectors( expressionExperiment );
        QuantitationType quantitationType = getPreferredQuantitationType( vectorsToUse );

        ExpressionDataDoubleMatrix dmatrix = this.createMaskedMatrix( vectorsToUse );

        List<BioMaterial> samplesUsed = AnalyzerHelper.getBioMaterialsForBioAssays( dmatrix );

        DoubleMatrixNamed namedMatrix = dmatrix.getNamedMatrix();

        List<String> rFactorsA = AnalyzerHelper.getRFactorsFromFactorValuesForTwoWayAnova( experimentalFactorA,
                samplesUsed );
        List<String> rFactorsB = AnalyzerHelper.getRFactorsFromFactorValuesForTwoWayAnova( experimentalFactorB,
                samplesUsed );

        String factsA = rc.assignStringList( rFactorsA );
        String factsB = rc.assignStringList( rFactorsB );

        String tfactsA = "t(" + factsA + ")";
        String tfactsB = "t(" + factsB + ")";

        String factorA = "factor(" + tfactsA + ")";
        String factorB = "factor(" + tfactsB + ")";

        String matrixName = rc.assignMatrix( namedMatrix );

        /* p-values */
        StringBuffer pvalueCommand = new StringBuffer();

        pvalueCommand.append( "apply(" );
        pvalueCommand.append( matrixName );
        pvalueCommand.append( ", 1, function(x) {anova(aov(x ~ " + factorA + "+" + factorB + "+" + factorA + "*"
                + factorB + "))$Pr}" );
        pvalueCommand.append( ")" );

        log.debug( pvalueCommand.toString() );

        double[] pvalues = rc.doubleArrayEval( pvalueCommand.toString() );
        if ( pvalues == null ) throw new IllegalStateException( "No pvalues returned" );

        // removes NA row TODO R can do this for you with "[1,] or "[2,]" or "[3,] in command"
        double[] filteredPvalues = new double[( pvalues.length / NUM_RESULTS_FROM_R ) * ACTUAL_NUM_RESULTS];

        for ( int i = 0, j = 0; j < filteredPvalues.length; i++ ) {
            if ( i % NUM_RESULTS_FROM_R < ACTUAL_NUM_RESULTS ) {
                filteredPvalues[j] = pvalues[i];
                j++;
            }
        }

        /* write out pvalues to a file */
        writeRawPValues( expressionExperiment, filteredPvalues );

        /* F-statistics */
        StringBuffer fstatisticCommand = new StringBuffer();

        fstatisticCommand.append( "apply(" );
        fstatisticCommand.append( matrixName );
        fstatisticCommand.append( ", 1, function(x) {anova(aov(x ~ " + factorA + "+" + factorB + "+" + factorA + "*"
                + factorB + "))$F}" );
        fstatisticCommand.append( ")" );

        log.debug( fstatisticCommand.toString() );

        double[] fstatistics = rc.doubleArrayEval( fstatisticCommand.toString() );

        // removes NA row
        double[] filteredFStatistics = new double[( fstatistics.length / NUM_RESULTS_FROM_R ) * ACTUAL_NUM_RESULTS];

        for ( int i = 0, j = 0; j < filteredFStatistics.length; i++ ) {
            if ( i % NUM_RESULTS_FROM_R < ACTUAL_NUM_RESULTS ) {
                filteredFStatistics[j] = fstatistics[i];
                j++;
            }
        }

        return createExpressionAnalysis( dmatrix, filteredPvalues, filteredFStatistics, ACTUAL_NUM_RESULTS,
                experimentalFactorA, experimentalFactorB, quantitationType );

    }
}
