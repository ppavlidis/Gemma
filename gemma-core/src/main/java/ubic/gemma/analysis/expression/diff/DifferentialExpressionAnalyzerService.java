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
package ubic.gemma.analysis.expression.diff;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.writer.MatrixWriter;
import ubic.basecode.math.distribution.Histogram;
import ubic.basecode.util.FileTools;
import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.analysis.service.ExpressionDataFileService;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedDifferentialExpressionAnalysisEvent;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.PersisterHelper;

/**
 * Differential expression service to run the differential expression analysis (and persist the results using the
 * appropriate data access objects).
 * <p>
 * FIXME this is very confusingly named as there is also a DifferentialExpressionAnalysisService and a
 * DifferentialExpressionAnalyzer
 * 
 * @author keshav
 * @version $Id$
 */
@Service
public class DifferentialExpressionAnalyzerService {

    public enum AnalysisType {
        GENERICLM, OSTTEST, OWA, TTEST, TWA, TWIA
    }

    @Autowired
    private AuditTrailService auditTrailService = null;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;

    @Autowired
    private DifferentialExpressionAnalyzer differentialExpressionAnalyzer;

    @Autowired
    ExpressionDataFileService expressionDataFileService;

    @Autowired
    DifferentialExpressionResultService differentialExpressionResultService;

    @Autowired
    private ExpressionExperimentReportService expressionExperimentReportService;

    private Log log = LogFactory.getLog( this.getClass() );

    @Autowired
    private PersisterHelper persisterHelper = null;

    /**
     * Delete all the differential expression analyses for the experiment.
     * 
     * @param expressionExperiment
     * @return how many analyses were deleted.
     */
    public int deleteOldAnalyses( ExpressionExperiment expressionExperiment ) {
        Collection<DifferentialExpressionAnalysis> diffAnalysis = differentialExpressionAnalysisService
                .findByInvestigation( expressionExperiment );

        int result = 0;
        if ( diffAnalysis == null || diffAnalysis.isEmpty() ) {
            log.debug( "No differential expression analyses to delete for " + expressionExperiment.getShortName() );
            return result;
        }

        for ( DifferentialExpressionAnalysis de : diffAnalysis ) {
            log.info( "Deleting old differential expression analysis for experiment "
                    + expressionExperiment.getShortName() );
            differentialExpressionAnalysisService.delete( de );

            deleteOldDistributionMatrices( expressionExperiment, de );
            result++;
        }

        /*
         * Delete old flat files.
         */
        expressionDataFileService.deleteDiffExFile( expressionExperiment );

        return result;
    }

    /**
     * Delete all the differential expression analyses for the experiment that use the given set of factors.
     * 
     * @param expressionExperiment
     * @param newAnalysis
     * @param factors
     * @return how many analyses were deleted.
     */
    public int deleteOldAnalyses( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysis newAnalysis, Collection<ExperimentalFactor> factors ) {
        Collection<DifferentialExpressionAnalysis> diffAnalyses = differentialExpressionAnalysisService
                .findByInvestigation( expressionExperiment );
        int numDeleted = 0;
        if ( diffAnalyses == null || diffAnalyses.isEmpty() ) {
            log.info( "No differential expression analyses to delete for " + expressionExperiment.getShortName() );
            return numDeleted;
        }

        this.differentialExpressionAnalysisService.thaw( diffAnalyses );

        for ( DifferentialExpressionAnalysis existingAnalysis : diffAnalyses ) {

            Collection<ExperimentalFactor> factorsInAnalysis = new HashSet<ExperimentalFactor>();

            for ( ExpressionAnalysisResultSet resultSet : existingAnalysis.getResultSets() ) {
                factorsInAnalysis.addAll( resultSet.getExperimentalFactors() );
            }

            FactorValue subsetFactorValueForExisting = existingAnalysis.getSubsetFactorValue();

            /*
             * Match if: factors are the same, and if this is a subset, it's the same subset factorvalue.
             */
            if ( factorsInAnalysis.size() == factors.size()
                    && factorsInAnalysis.containsAll( factors )
                    && ( subsetFactorValueForExisting == null || subsetFactorValueForExisting.equals( newAnalysis
                            .getSubsetFactorValue() ) ) ) {

                log.info( "Deleting old differential expression analysis for experiment "
                        + expressionExperiment.getShortName() );
                differentialExpressionAnalysisService.delete( existingAnalysis );
                numDeleted++;

                /*
                 * Delete old flat files. This deletes them all, could be fixed but not a big deal.
                 */
                expressionDataFileService.deleteDiffExFile( expressionExperiment );

                /*
                 * Delete the old statistic distributions.
                 */
                deleteOldDistributionMatrices( expressionExperiment, existingAnalysis );
            }
        }

        if ( numDeleted == 0 ) {
            log.info( "None of the existing analyses were eligible for deletion" );
        }
        return numDeleted;
    }

    /**
     * Run differential expression on the {@link ExpressionExperiment}.
     * 
     * @param expressionExperiment
     */
    public Collection<DifferentialExpressionAnalysis> doDifferentialExpressionAnalysis(
            ExpressionExperiment expressionExperiment ) {

        Collection<ExperimentalFactor> experimentalFactors = expressionExperiment.getExperimentalDesign()
                .getExperimentalFactors();
        if ( experimentalFactors.size() > MAX_FACTORS_FOR_AUTO_ANALYSIS ) {
            throw new IllegalArgumentException( "Has more than " + MAX_FACTORS_FOR_AUTO_ANALYSIS
                    + " factors, please specify the factors" );
        }

        return differentialExpressionAnalyzer.analyze( expressionExperiment );
    }

    /**
     * Run differential expression on the {@link ExpressionExperiment} using the given factor(s)
     * 
     * @param expressionExperiment
     * @param factors
     * @return
     */
    public Collection<DifferentialExpressionAnalysis> doDifferentialExpressionAnalysis(
            ExpressionExperiment expressionExperiment, Collection<ExperimentalFactor> factors ) {
        return differentialExpressionAnalyzer.analyze( expressionExperiment, factors );
    }

    /**
     * @param expressionExperiment
     * @param config
     * @return
     */
    public Collection<DifferentialExpressionAnalysis> doDifferentialExpressionAnalysis(
            ExpressionExperiment expressionExperiment, DifferentialExpressionAnalysisConfig config ) {
        return differentialExpressionAnalyzer.analyze( expressionExperiment, config );
    }

    /**
     * Run differential expression on the {@link ExpressionExperiment} using the given factor(s) and analysis type.
     * 
     * @param expressionExperiment
     * @param factors
     * @param type
     * @return
     */
    public Collection<DifferentialExpressionAnalysis> doDifferentialExpressionAnalysis(
            ExpressionExperiment expressionExperiment, Collection<ExperimentalFactor> factors, AnalysisType type ) {
        return differentialExpressionAnalyzer.analyze( expressionExperiment, factors, type );
    }

    /**
     * @param expressionExperiment
     * @return
     * @throws Exception
     */
    public Collection<ExpressionAnalysisResultSet> getResultSets( ExpressionExperiment expressionExperiment ) {
        return differentialExpressionAnalysisService.getResultSets( expressionExperiment );
    }

    /**
     * @param expressionExperiment
     * @return
     */
    public Collection<DifferentialExpressionAnalysis> getAnalyses( ExpressionExperiment expressionExperiment ) {
        Collection<DifferentialExpressionAnalysis> expressionAnalyses = differentialExpressionAnalysisService
                .getAnalyses( expressionExperiment );
        differentialExpressionAnalysisService.thaw( expressionAnalyses );
        return expressionAnalyses;
    }

    /**
     * @param expressionExperiment
     * @param config
     * @return
     */
    public Collection<DifferentialExpressionAnalysis> runDifferentialExpressionAnalyses(
            ExpressionExperiment expressionExperiment, DifferentialExpressionAnalysisConfig config ) {
        try {
            Collection<DifferentialExpressionAnalysis> diffExpressionAnalyses = doDifferentialExpressionAnalysis(
                    expressionExperiment, config );

            for ( DifferentialExpressionAnalysis analysis : diffExpressionAnalyses ) {
                deleteOldAnalyses( expressionExperiment, analysis, config.getFactorsToInclude() );
            }

            return persistAnalyses( expressionExperiment, diffExpressionAnalyses );
        } catch ( Exception e ) {
            auditTrailService.addUpdateEvent( expressionExperiment, FailedDifferentialExpressionAnalysisEvent.Factory
                    .newInstance(), ExceptionUtils.getFullStackTrace( e ) );
            throw new RuntimeException( e );
        }
    }

    private static final int MAX_FACTORS_FOR_AUTO_ANALYSIS = 3;

    /**
     * Run the differential expression analysis, attempting to identify the appropriate analysis automatically. First
     * deletes ALL the old differential expression analyses for this experiment, if any.
     * 
     * @param expressionExperiment
     * @return
     */
    public Collection<DifferentialExpressionAnalysis> runDifferentialExpressionAnalyses(
            ExpressionExperiment expressionExperiment ) {

        try {
            Collection<DifferentialExpressionAnalysis> diffExpressionAnalyses = doDifferentialExpressionAnalysis( expressionExperiment );
            deleteOldAnalyses( expressionExperiment );
            return persistAnalyses( expressionExperiment, diffExpressionAnalyses );
        } catch ( Exception e ) {
            auditTrailService.addUpdateEvent( expressionExperiment, FailedDifferentialExpressionAnalysisEvent.Factory
                    .newInstance(), ExceptionUtils.getFullStackTrace( e ) );
            throw new RuntimeException( e );
        }
    }

    /**
     * Run the differential expression analysis. First deletes the matching existing differential expression analysis,
     * if any.
     * 
     * @param expressionExperiment
     * @param factors
     * @return
     */
    public Collection<DifferentialExpressionAnalysis> runDifferentialExpressionAnalyses(
            ExpressionExperiment expressionExperiment, Collection<ExperimentalFactor> factors ) {
        try {
            Collection<DifferentialExpressionAnalysis> diffExpressionAnalyses = doDifferentialExpressionAnalysis(
                    expressionExperiment, factors );
            for ( DifferentialExpressionAnalysis analysis : diffExpressionAnalyses ) {
                deleteOldAnalyses( expressionExperiment, analysis, factors );
            }
            return persistAnalyses( expressionExperiment, diffExpressionAnalyses );
        } catch ( Exception e ) {
            auditTrailService.addUpdateEvent( expressionExperiment, FailedDifferentialExpressionAnalysisEvent.Factory
                    .newInstance(), ExceptionUtils.getFullStackTrace( e ) );
            throw new RuntimeException( e );
        }
    }

    /**
     * Runs the differential expression analysis, then deletes the matching old differential expression analysis (if
     * any).
     * 
     * @param expressionExperiment
     * @param factors
     * @param type
     * @return
     */
    public Collection<DifferentialExpressionAnalysis> runDifferentialExpressionAnalyses(
            ExpressionExperiment expressionExperiment, Collection<ExperimentalFactor> factors, AnalysisType type ) {

        try {
            Collection<DifferentialExpressionAnalysis> diffExpressionAnalyses = doDifferentialExpressionAnalysis(
                    expressionExperiment, factors, type );

            for ( DifferentialExpressionAnalysis analysis : diffExpressionAnalyses ) {
                deleteOldAnalyses( expressionExperiment, analysis, factors );
            }

            Collection<DifferentialExpressionAnalysis> results = persistAnalyses( expressionExperiment,
                    diffExpressionAnalyses );
            return results;
        } catch ( Exception e ) {
            auditTrailService.addUpdateEvent( expressionExperiment, FailedDifferentialExpressionAnalysisEvent.Factory
                    .newInstance(), ExceptionUtils.getFullStackTrace( e ) );
            throw new RuntimeException( e );
        }

    }

    private Collection<DifferentialExpressionAnalysis> persistAnalyses( ExpressionExperiment expressionExperiment,
            Collection<DifferentialExpressionAnalysis> diffExpressionAnalyses ) {
        Collection<DifferentialExpressionAnalysis> results = new HashSet<DifferentialExpressionAnalysis>();
        for ( DifferentialExpressionAnalysis analysis : diffExpressionAnalyses ) {
            DifferentialExpressionAnalysis persistentAnalysis = persistAnalysis( expressionExperiment, analysis );
            results.add( persistentAnalysis );
        }
        return results;
    }

    /**
     * @param ee
     * @throws IOException
     */
    public void updateScoreDistributionFiles( ExpressionExperiment ee ) throws IOException {

        Collection<DifferentialExpressionAnalysis> analyses = this.getAnalyses( ee );

        if ( analyses.size() == 0 ) {
            log.info( "No  analyses for experiment " + ee.getShortName()
                    + ".  The differential expression analysis may not have been run on this experiment yet." );
            return;
        }

        for ( DifferentialExpressionAnalysis differentialExpressionAnalysis : analyses ) {
            writeDistributions( ee, differentialExpressionAnalysis );
        }

    }

    /**
     * Returns true if any differential expression data exists for the experiment, else false.
     * 
     * @param ee
     * @return
     */
    public boolean wasDifferentialAnalysisRun( ExpressionExperiment ee ) {

        Collection<DifferentialExpressionAnalysis> expressionAnalyses = differentialExpressionAnalysisService
                .findByInvestigation( ee );

        return !expressionAnalyses.isEmpty();
    }

    /**
     * Returns true if differential expression data exists for the experiment with the given factors, else false.
     * 
     * @param ee
     * @param factors
     * @return
     */
    public boolean wasDifferentialAnalysisRun( ExpressionExperiment ee, Collection<ExperimentalFactor> factors ) {

        Collection<DifferentialExpressionAnalysis> expressionAnalyses = differentialExpressionAnalysisService
                .findByInvestigation( ee );

        for ( DifferentialExpressionAnalysis de : expressionAnalyses ) {
            Collection<ExperimentalFactor> factorsInAnalysis = new HashSet<ExperimentalFactor>();
            for ( ExpressionAnalysisResultSet resultSet : de.getResultSets() ) {
                factorsInAnalysis.addAll( resultSet.getExperimentalFactors() );
            }

            if ( factorsInAnalysis.size() == factors.size() && factorsInAnalysis.containsAll( factors ) ) {
                return true;
            }

        }
        return false;
    }

    /**
     * Print the p-value and score distributions. Note that if there are multiple analyses for the experiment, each one
     * will get a set of files.
     * 
     * @param expressionExperiment
     * @param diffExpressionAnalysis - could be on a subset of the experiment.
     */
    protected void writeDistributions( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysis diffExpressionAnalysis ) {

        /*
         * write histograms
         * 
         * Of 1) pvalues, 2) scores, 3) qvalues
         * 
         * Put all pvalues in one file etc so we don't get 9 files for a 2x anova with interactions.
         */

        List<Histogram> pvalueHistograms = new ArrayList<Histogram>();
        List<Histogram> qvalueHistograms = new ArrayList<Histogram>();
        List<Histogram> scoreHistograms = new ArrayList<Histogram>();

        List<ExpressionAnalysisResultSet> resultSetList = new ArrayList<ExpressionAnalysisResultSet>();
        resultSetList.addAll( diffExpressionAnalysis.getResultSets() );

        List<String> factorNames = new ArrayList<String>();

        for ( ExpressionAnalysisResultSet resultSet : resultSetList ) {
            differentialExpressionResultService.thaw( resultSet );

            String factorName = "";

            // these will be headings on the
            for ( ExperimentalFactor factor : resultSet.getExperimentalFactors() ) {
                factorName = factorName + ( factorName.equals( "" ) ? "" : ":" ) + factor.getName();
            }
            factorNames.add( factorName );

            Histogram pvalHist = new Histogram( factorName, 100, 0.0, 1.0 );
            pvalueHistograms.add( pvalHist );

            Histogram qvalHist = new Histogram( factorName, 100, 0.0, 1.0 );
            qvalueHistograms.add( qvalHist );

            Histogram scoreHist = new Histogram( factorName, 200, -20, 20 );
            scoreHistograms.add( scoreHist );

            for ( DifferentialExpressionAnalysisResult result : resultSet.getResults() ) {
                Double correctedPvalue = result.getCorrectedPvalue();
                if ( correctedPvalue != null ) qvalHist.fill( correctedPvalue );

                /*
                 * FIXME do contrasts?
                 */
                // Double effectSize = result.getEffectSize();
                // if ( effectSize != null ) scoreHist.fill( effectSize );

                Double pvalue = result.getPvalue();
                if ( pvalue != null ) pvalHist.fill( pvalue );
            }

        }

        DoubleMatrix<String, String> pvalueDists = new DenseDoubleMatrix<String, String>( 100, resultSetList.size() );
        DoubleMatrix<String, String> qvalueDists = new DenseDoubleMatrix<String, String>( 100, resultSetList.size() );
        DoubleMatrix<String, String> scoreDists = new DenseDoubleMatrix<String, String>( 200, resultSetList.size() );

        int i = 0;
        for ( Histogram h : pvalueHistograms ) {
            if ( i == 0 ) {
                pvalueDists.setRowNames( Arrays.asList( h.getBinEdgesStrings() ) );
                pvalueDists.setColumnNames( factorNames );

            }

            double[] binHeights = h.getArray();
            for ( int j = 0; j < binHeights.length; j++ ) {
                pvalueDists.set( j, i, binHeights[j] );

            }
            i++;
        }
        i = 0;
        for ( Histogram h : qvalueHistograms ) {
            if ( i == 0 ) {
                qvalueDists.setRowNames( Arrays.asList( h.getBinEdgesStrings() ) );
                qvalueDists.setColumnNames( factorNames );
            }
            double[] binHeights = h.getArray();
            for ( int j = 0; j < binHeights.length; j++ ) {
                qvalueDists.set( j, i, binHeights[j] );
            }
            i++;
        }
        i = 0;
        for ( Histogram h : scoreHistograms ) {
            if ( i == 0 ) {
                scoreDists.setRowNames( Arrays.asList( h.getBinEdgesStrings() ) );
                scoreDists.setColumnNames( factorNames );
            }
            double[] binHeights = h.getArray();
            for ( int j = 0; j < binHeights.length; j++ ) {
                scoreDists.set( j, i, binHeights[j] );
            }
            i++;
        }

        saveDistributionMatrixToFile( "pvalues", pvalueDists, expressionExperiment, resultSetList );
        saveDistributionMatrixToFile( "qvalues", qvalueDists, expressionExperiment, resultSetList );
        saveDistributionMatrixToFile( "scores", scoreDists, expressionExperiment, resultSetList );

    }

    /**
     * Remove old files which will otherwise be cruft.
     * 
     * @param ee
     * @param analysis
     */
    private void deleteOldDistributionMatrices( ExpressionExperiment ee, DifferentialExpressionAnalysis analysis ) {

        File f = prepareDirectoryForDistributions( ee );

        boolean deleted = false;

        String histFileName = cleanShortName( ee ) + ".an" + analysis.getId() + "." + "pvalues"
                + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;
        File oldf = new File( f, histFileName );
        if ( oldf.exists() ) {
            deleted = oldf.delete();
            if ( !deleted ) {
                log.warn( "Could not delete: " + oldf );
            }
        }

        histFileName = cleanShortName( ee ) + ".an" + analysis.getId() + "." + "qvalues"
                + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;
        oldf = new File( f, histFileName );
        if ( oldf.exists() ) {
            deleted = oldf.delete();
            if ( !deleted ) {
                log.warn( "Could not delete: " + oldf );
            }
        }
        histFileName = cleanShortName( ee ) + ".an" + analysis.getId() + "." + "scores"
                + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;
        oldf = new File( f, histFileName );
        if ( oldf.exists() ) {
            deleted = oldf.delete();
            if ( !deleted ) {
                log.warn( "Could not delete: " + oldf );
            }
        }
    }

    /**
     * @param expressionExperiment; it is possible that the analysis is on a subset of the given experiment.
     * @param diffExpressionAnalysis
     * @return saved results.
     */
    private DifferentialExpressionAnalysis persistAnalysis( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysis diffExpressionAnalysis ) {
        log.info( "Saving new results" );
        DifferentialExpressionAnalysis savedResults = ( DifferentialExpressionAnalysis ) persisterHelper
                .persist( diffExpressionAnalysis );

        /*
         * Audit event
         */
        auditTrailService.addUpdateEvent( expressionExperiment, DifferentialExpressionAnalysisEvent.Factory
                .newInstance(), savedResults.getDescription() + "; analysis id=" + savedResults.getId() );

        /*
         * Save histograms
         */
        writeDistributions( expressionExperiment, savedResults );

        /*
         * Update the report
         */
        expressionExperimentReportService.generateSummaryObject( expressionExperiment.getId() );

        return savedResults;
    }

    /**
     * @param expressionExperiment
     * @return the directory where the files should be written.
     */
    private File prepareDirectoryForDistributions( ExpressionExperiment expressionExperiment ) {
        File dir = DifferentialExpressionFileUtils.getBaseDifferentialDirectory( expressionExperiment.getShortName() );
        FileTools.createDir( dir.toString() );
        return dir;
    }

    /**
     * Avoid getting file names with spaces etc.
     * 
     * @param ee
     * @return
     */
    private String cleanShortName( ExpressionExperiment ee ) {
        return ee.getShortName().replaceAll( "[\\s\'\";,]", "_" );
    }

    /**
     * Print the distributions to a file.
     * 
     * @param extraSuffix eg qvalue, pvalue, score
     * @param histograms each column is a histogram
     * @param expressionExperiment
     * @param resulstsets in the same order as columns in the
     */
    private void saveDistributionMatrixToFile( String extraSuffix, DoubleMatrix<String, String> histograms,
            ExpressionExperiment expressionExperiment, List<ExpressionAnalysisResultSet> resultSetList ) {

        Long analysisId = resultSetList.iterator().next().getAnalysis().getId();

        File f = prepareDirectoryForDistributions( expressionExperiment );

        String histFileName = cleanShortName( expressionExperiment ) + ".an" + analysisId + "." + extraSuffix
                + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;

        File outputFile = new File( f, histFileName );

        // log.info( outputFile );
        try {
            FileWriter out = new FileWriter( outputFile, true /*
                                                               * clobber, but usually won't since file names are per
                                                               * analyssi
                                                               */);
            out.write( "# Gemma: differential expression statistics - " + extraSuffix + "\n" );
            out.write( "# Generated=" + ( new Date() ) + "\n" );
            out.write( "# If you use this file for your research, please cite the Gemma web site\n" );
            out.write( "# exp=" + expressionExperiment.getId() + " " + expressionExperiment.getShortName() + " "
                    + expressionExperiment.getName() + " \n" );

            for ( ExpressionAnalysisResultSet resultSet : resultSetList ) {
                BioAssaySet analyzedSet = resultSet.getAnalysis().getExperimentAnalyzed();

                if ( analyzedSet instanceof ExpressionExperimentSubSet ) {
                    out.write( "# SubSet id=" + analyzedSet.getId() + " " + analyzedSet.getName() + " "
                            + analyzedSet.getDescription() + "\n" );
                }

                Long resultSetId = resultSet.getId();
                out.write( "# ResultSet id=" + resultSetId + ", analysisId=" + analysisId + "\n" );

            }

            MatrixWriter<String, String> writer = new MatrixWriter<String, String>( out );
            writer.writeMatrix( histograms, true );

        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

    }
}
