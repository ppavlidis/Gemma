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
package ubic.gemma.persistence.service.analysis.expression.coexpression;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysis;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.BaseDao;

/**
 * @author paul
 */
public interface SampleCoexpressionAnalysisDao extends BaseDao<SampleCoexpressionAnalysis> {

    DoubleMatrix<BioAssay, BioAssay> load( ExpressionExperiment ee );

    SampleCoexpressionAnalysis create( DoubleMatrix<BioAssay, BioAssay> matrix, BioAssayDimension bad,
            ExpressionExperiment ee );

    boolean hasAnalysis( ExpressionExperiment ee );

    /**
     * Remove any associated with the given experiment
     */
    void removeForExperiment( ExpressionExperiment ee );

}