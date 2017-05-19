/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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
package ubic.gemma.core.analysis.expression.diff;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerServiceImpl.AnalysisType;
import ubic.gemma.core.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.core.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.core.loader.expression.simple.SimpleExpressionDataLoaderService;
import ubic.gemma.core.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.core.testing.BaseSpringContextTest;

/**
 * Test based on GSE8441
 * 
 * @author paul
 * @version $Id$
 */
public class TwoWayAnovaWithInteractionTest2 extends BaseSpringContextTest {

    @Autowired
    private SimpleExpressionDataLoaderService dataLoaderService;

    @Autowired
    private ExperimentalDesignImporter designImporter;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private DiffExAnalyzer analyzer;

    @Autowired
    private AnalysisSelectionAndExecutionService analysisService = null;

    @Autowired
    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService = null;

    private ExpressionExperiment ee;

    @Before
    public void setup() throws IOException {
        try (InputStream io = this.getClass().getResourceAsStream(
                "/data/analysis/expression/GSE8441_expmat_8probes.txt" );) {

            SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();
            metaData.setShortName( RandomStringUtils.randomAlphabetic( 10 ) );
            metaData.setTaxon( taxonService.findByCommonName( "mouse" ) );
            metaData.setQuantitationTypeName( "whatever" );
            // metaData.setScale( ScaleType.LOG2 ); // this is actually wrong!
            metaData.setScale( ScaleType.LINEAR );

            ArrayDesign f = ArrayDesign.Factory.newInstance();
            f.setShortName( "GSE8441_test" );
            f.setTechnologyType( TechnologyType.ONECOLOR );
            f.setPrimaryTaxon( metaData.getTaxon() );
            metaData.getArrayDesigns().add( f );

            ee = dataLoaderService.create( metaData, io );

            designImporter.importDesign( ee,
                    this.getClass().getResourceAsStream( "/data/analysis/expression/606_GSE8441_expdesign.data.txt" ) );

            ee = expressionExperimentService.thaw( ee );
        }
    }

    /**
     * NOTE I added a constant probe to this data after I set this up.
     * 
     * <pre>
     * expMatFile <- "GSE8441_expmat_8probes.txt"
     * expDesignFile <- "606_GSE8441_expdesign.data.txt"
     * expMat <- log2(read.table(expMatFile, header = TRUE, row.names = 1, sep = "\t", quote=""))
     * expDesign <- read.table(expDesignFile, header = TRUE, row.names = 1, sep = "\t", quote="")
     * 
     * expData <- expMat[rownames(expDesign)]
     * 
     * names(expData) == row.names(expDesign)
     * attach(expDesign)
     * lf<-lm(unlist(expData["217757_at",])~Treatment*Sex )
     * summary(lf)
     * anova(lf)
     * 
     * summary(lm(unlist(expData["202851_at",])~Treatment*Sex ))
     * anova(lm(unlist(expData["202851_at",])~Treatment*Sex ))  
     * 
     * # etc.
     * </pre>
     * 
     * @throws Exception
     */
    @Test
    public void test() throws Exception {

        AnalysisType aa = analysisService.determineAnalysis( ee, ee.getExperimentalDesign().getExperimentalFactors(),
                null, true );

        assertEquals( AnalysisType.TWO_WAY_ANOVA_WITH_INTERACTION, aa );

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();

        Collection<ExperimentalFactor> factors = ee.getExperimentalDesign().getExperimentalFactors();

        assertEquals( 2, factors.size() );
        config.setAnalysisType( aa );
        config.setFactorsToInclude( factors );
        config.getInteractionsToInclude().add( factors );
        config.setQvalueThreshold( null );

        analyzer = this.getBean( DiffExAnalyzer.class );
        Collection<DifferentialExpressionAnalysis> result = analyzer.run( ee, config );
        assertEquals( 1, result.size() );

        DifferentialExpressionAnalysis analysis = result.iterator().next();

        checkResults( analysis );

        Collection<DifferentialExpressionAnalysis> persistent = differentialExpressionAnalyzerService
                .runDifferentialExpressionAnalyses( ee, config );

        DifferentialExpressionAnalysis refetched = differentialExpressionAnalysisService.load( persistent.iterator()
                .next().getId() );

        differentialExpressionAnalysisService.thaw( refetched );
        for ( ExpressionAnalysisResultSet ears : refetched.getResultSets() ) {
            ears = differentialExpressionResultService.thaw( ears );

        }

        checkResults( refetched );

        differentialExpressionAnalyzerService.redoAnalysis( ee, refetched, true );

    }

    /**
     * @param analysis
     */
    public void checkResults( DifferentialExpressionAnalysis analysis ) {
        Collection<ExpressionAnalysisResultSet> resultSets = analysis.getResultSets();

        assertEquals( 3, resultSets.size() );

        boolean found1 = false, found2 = false, found3 = false, found4 = false;

        for ( ExpressionAnalysisResultSet rs : resultSets ) {
            boolean interaction = false;
            boolean sexFactor = false;
            Collection<DifferentialExpressionAnalysisResult> results = rs.getResults();

            if ( rs.getExperimentalFactors().size() == 1 ) {
                ExperimentalFactor factor = rs.getExperimentalFactors().iterator().next();
                if ( factor.getName().equals( "Sex" ) ) {
                    sexFactor = true;
                } else {
                    sexFactor = false;
                }
            } else {
                interaction = true;
            }

            assertEquals( 8, results.size() );

            /*
             * Test values here are computed in R, using anova(lm(unlist(expData["205969_at",])~Treatment*Sex )) etc.
             */
            for ( DifferentialExpressionAnalysisResult r : results ) {
                CompositeSequence probe = r.getProbe();
                Double pvalue = r.getPvalue();
                if ( probe.getName().equals( "205969_at" ) ) {
                    if ( sexFactor ) {
                        found1 = true;
                        assertEquals( 0.3333, pvalue, 0.001 );
                    } else if ( interaction ) {
                        found2 = true;
                        assertEquals( 0.8480, pvalue, 0.001 );
                    } else {
                        found3 = true;
                        assertEquals( 0.1323, pvalue, 0.001 );
                    }
                } else if ( probe.getName().equals( "217757_at" ) ) {
                    if ( interaction ) {
                        found4 = true;
                        assertEquals( 0.7621, pvalue, 0.001 );
                    }
                } else if ( probe.getName().equals( "constant" ) ) {
                    fail( "Should not have found a result for constant probe" );
                }
            }

        }

        assertTrue( found1 && found2 && found3 && found4 );
    }
}
