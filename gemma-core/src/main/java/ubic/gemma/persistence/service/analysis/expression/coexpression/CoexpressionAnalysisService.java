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
package ubic.gemma.persistence.service.analysis.expression.coexpression;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.analysis.expression.coexpression.CoexpCorrelationDistribution;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.analysis.AnalysisService;

import java.util.Collection;

/**
 * Deals with the Analysis objects for Coexpression - not the coexpression results themselves.
 *
 * @author kelsey
 */
public interface CoexpressionAnalysisService extends AnalysisService<CoexpressionAnalysis> {

    @SuppressWarnings("unused") // Possible external use
    @Secured({ "GROUP_USER" })
    CoexpressionAnalysis create( CoexpressionAnalysis coexpressionAnalysis );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<CoexpressionAnalysis> findByTaxon( Taxon taxon );

    /**
     * Not secured: for internal use only
     *
     * @param idsToFilter starting list of bioassayset ids.
     * @return the ones which have a coexpression analysis.
     */
    @Override
    Collection<Long> getExperimentsWithAnalysis( Collection<Long> idsToFilter );

    /**
     * Not secured: for internal use only
     *
     * @return ids of bioassaysets from the given taxon that have a coexpression analysis
     */
    @Override
    Collection<Long> getExperimentsWithAnalysis( Taxon taxon );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void update( CoexpressionAnalysis o );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    CoexpCorrelationDistribution getCoexpCorrelationDistribution( ExpressionExperiment expressionExperiment );

    /**
     * For backfilling of the coexpression distributions from flat files - remove when no longer needed.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_EDIT" })
    void addCoexpCorrelationDistribution( ExpressionExperiment expressionExperiment,
            CoexpCorrelationDistribution coexpd );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Boolean hasCoexpCorrelationDistribution( ExpressionExperiment ee );

}