/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
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

package ubic.gemma.model.analysis;

import ubic.gemma.model.analysis.expression.ExpressionAnalysis;
import ubic.gemma.model.expression.experiment.BioAssaySet;

/**
 * 
 */
public abstract class SingleExperimentAnalysis extends ExpressionAnalysis implements gemma.gsec.model.SecuredChild {

    private Integer numberOfElementsAnalyzed;

    private BioAssaySet experimentAnalyzed;

    /**
     * 
     */
    public BioAssaySet getExperimentAnalyzed() {
        return this.experimentAnalyzed;
    }

    /**
     * The number of probes or genes (or other elements) used in the analysis. The exact meaning is determined by the
     * subclass + implementation.
     */
    public Integer getNumberOfElementsAnalyzed() {
        return this.numberOfElementsAnalyzed;
    }

    public void setExperimentAnalyzed( ubic.gemma.model.expression.experiment.BioAssaySet experimentAnalyzed ) {
        this.experimentAnalyzed = experimentAnalyzed;
    }

    public void setNumberOfElementsAnalyzed( Integer numberOfElementsAnalyzed ) {
        this.numberOfElementsAnalyzed = numberOfElementsAnalyzed;
    }

}