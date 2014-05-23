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
package ubic.gemma.model.analysis.expression.diff;

import java.util.Collection;
import java.util.Map;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.analysis.AnalysisService;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;

/**
 * @author kelsey
 * @version $Id$
 */
public interface DifferentialExpressionAnalysisService extends AnalysisService<DifferentialExpressionAnalysis> {

    /**
     * @param par
     * @param threshold for corrected pvalue. Results may not be accurate for 'unreasonable' thresholds.
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Integer countDownregulated( ExpressionAnalysisResultSet par, double threshold );

    /**
     * @param ExpressionAnalysisResultSet
     * @param threshold (double) for corrected pvalue. Results may not be accurate for 'unreasonable' thresholds.
     * @return an integer count of all the probes that met the given threshold in the given expressionAnalysisResultSet
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Integer countProbesMeetingThreshold( ExpressionAnalysisResultSet ears, double threshold );

    /**
     * @param par
     * @param threshold for corrected pvalue. Results may not be accurate for 'unreasonable' thresholds.
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Integer countUpregulated( ExpressionAnalysisResultSet par, double threshold );

    /**
     * 
     */

    @Secured({ "GROUP_USER" })
    public DifferentialExpressionAnalysis create( DifferentialExpressionAnalysis analysis );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<DifferentialExpressionAnalysis> find( ubic.gemma.model.genome.Gene gene,
            ExpressionAnalysisResultSet resultSet, double threshold );

    /**
     * @param ef
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<DifferentialExpressionAnalysis> findByFactor( ExperimentalFactor ef );

    /**
     * Given a collection of ids, return a map of id -> collection of differential expression analysis
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    public Map<Long, Collection<DifferentialExpressionAnalysis>> findByInvestigationIds(
            Collection<Long> investigationIds );

    /**
     * 
     */
    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<DifferentialExpressionAnalysis> findByParentTaxon( Taxon taxon );

    /**
     * 
     */
    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<DifferentialExpressionAnalysis> findByTaxon( Taxon taxon );

    /**
     * Return a collection of experiments in which the given gene was analyzed.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<BioAssaySet> findExperimentsWithAnalyses( ubic.gemma.model.genome.Gene gene );

    /**
     * @param expressionExperiment
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ", "AFTER_ACL_COLLECTION_READ" })
    public Collection<DifferentialExpressionAnalysis> getAnalyses( BioAssaySet expressionExperiment );

    /**
     * @param expressionExperiments
     * @return quite deeply thawed analyses (not the results themselves, but metadata)
     */
    public Map<ExpressionExperiment, Collection<DifferentialExpressionAnalysis>> getAnalyses(
            Collection<? extends BioAssaySet> expressionExperiments );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ" })
    public void thaw( Collection<DifferentialExpressionAnalysis> expressionAnalyses );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public void thaw( DifferentialExpressionAnalysis differentialExpressionAnalysis );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public DifferentialExpressionAnalysis thawFully( DifferentialExpressionAnalysis differentialExpressionAnalysis );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( DifferentialExpressionAnalysis o );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( ExpressionAnalysisResultSet a );

    /**
     * Is the analysis deleteable, or is it tied up with another entity that keeps it from being removed.
     * 
     * @param toDelete
     * @return
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public boolean canDelete( DifferentialExpressionAnalysis differentialExpressionAnalysis );

    /**
     * @param ids
     * @return
     */
    public Map<ExpressionExperimentValueObject, Collection<DifferentialExpressionAnalysisValueObject>> getAnalysesByExperiment(
            Collection<Long> ids );

}
