/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
//
// Attention: Generated code! Do not modify by hand!
// Generated by: SpringService.vsl in andromda-spring-cartridge.
//
package ubic.gemma.model.analysis.expression.diff;

import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * 
 */
public interface DifferentialExpressionAnalysisService extends ubic.gemma.model.analysis.AnalysisService {

    /**
     * 
     */
    public ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis create(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis analysis );

    /**
     * 
     */
    public void thaw( java.util.Collection expressionAnalyses );

    /**
     * <p>
     * Return a collection of experiments in which the given gene was analyzed.
     * </p>
     */
    public java.util.Collection findExperimentsWithAnalyses( ubic.gemma.model.genome.Gene gene );

    /**
     * Returns a map of a collection of {@link ProbeAnalysisResult}s keyed by {@link ExpressionExperiment}.
     * 
     * @param gene
     * @param experimentsAnalyzed
     * @return Map<ExpressionExperiment, Collection<ProbeAnalysisResult>>
     */
    public java.util.Map<ubic.gemma.model.expression.experiment.ExpressionExperiment, java.util.Collection<ProbeAnalysisResult>> findResultsForGeneInExperiments(
            ubic.gemma.model.genome.Gene gene,
            java.util.Collection<ubic.gemma.model.expression.experiment.ExpressionExperiment> experimentsAnalyzed );

    /**
     * Find differential expression for a gene in given data sets, exceeding a given significance level (using the
     * corrected pvalue field)
     * 
     * @param gene
     * @param experimentsAnalyzed
     * @param threshold
     * @return
     */
    public java.util.Map<ubic.gemma.model.expression.experiment.ExpressionExperiment, java.util.Collection<ProbeAnalysisResult>> findResultsForGeneInExperimentsMetThreshold(
            ubic.gemma.model.genome.Gene gene,
            java.util.Collection<ubic.gemma.model.expression.experiment.ExpressionExperiment> experimentsAnalyzed,
            double threshold );

    /**
     * 
     */
    public void delete( java.lang.Long idToDelete );

    /**
     * 
     */
    public void thaw(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis differentialExpressionAnalysis );

    /**
     * 
     */
    public java.util.Collection find( ubic.gemma.model.genome.Gene gene,
            ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet resultSet, double threshold );

    /**
     * 
     */
    public java.util.Collection getResultSets(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

    /**
     * <p>
     * Given a collection of ids, return a map of id -> differential expression analysis (one per id).
     * </p>
     */
    public java.util.Map findByInvestigationIds( java.util.Collection investigationIds );

}
