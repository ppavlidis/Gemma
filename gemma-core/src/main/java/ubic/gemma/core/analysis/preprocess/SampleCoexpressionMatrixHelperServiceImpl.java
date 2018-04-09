/*
 * The Gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
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
package ubic.gemma.core.analysis.preprocess;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.coexpression.SampleCoexpressionAnalysisDao;

/**
 * @author Paul
 */
@Service
public class SampleCoexpressionMatrixHelperServiceImpl implements SampleCoexpressionMatrixHelperService {

    @Autowired
    private SampleCoexpressionAnalysisDao sampleCoexpressionMatrixDao;

    @Override
    @Transactional(readOnly = true)
    public DoubleMatrix<BioAssay, BioAssay> load( ExpressionExperiment ee ) {
        return this.sampleCoexpressionMatrixDao.load( ee );
    }

    @Override
    @Transactional
    public void create( DoubleMatrix<BioAssay, BioAssay> matrix, BioAssayDimension bad, ExpressionExperiment ee ) {
        this.sampleCoexpressionMatrixDao.create( matrix, bad, ee );
    }

    @Override
    @Transactional
    public void removeForExperiment( ExpressionExperiment ee ) {
        this.sampleCoexpressionMatrixDao.removeForExperiment( ee );
    }

}