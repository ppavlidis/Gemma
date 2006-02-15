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
package edu.columbia.gemma.web.controller.expression.experiment;

import java.io.Serializable;

/**
 * Command class for expression experiment loading.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionExperimentLoadCommand  implements Serializable  {

    private String datasourceName;

    private String accession;

    /**
     * @return Returns the accession.
     */
    public String getAccession() {
        return this.accession;
    }

    /**
     * @param accession The accession to set.
     *     @spring.validator type="required"
     * @spring.validator-args arg0resource="expressionExperiment.accession"
     */
    public void setAccession( String accession ) {
        this.accession = accession;
    }

    /**
     * @return Returns the datasourceName.
     */
    public String getDatasourceName() {
        return this.datasourceName;
    }

    /**
     * @param datasourceName The datasourceName to set.
     */
    public void setDatasourceName( String datasourceName ) {
        this.datasourceName = datasourceName;
    }

}
