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
package ubic.gemma.web.controller.diff;

import java.io.Serializable;

/**
 * @author keshav
 * @version $Id$
 */
public class DiffExpressionSearchCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private String geneOfficialSymbol = null;

    private double threshold;

    private String taxonName = null;

    /**
     * @return
     */
    public String getGeneOfficialSymbol() {
        return geneOfficialSymbol;
    }

    /**
     * @param geneOfficialSymbol
     */
    public void setGeneOfficialSymbol( String geneOfficialSymbol ) {
        this.geneOfficialSymbol = geneOfficialSymbol;
    }

    /**
     * @return
     */
    public double getThreshold() {
        return threshold;
    }

    /**
     * @param threshold
     */
    public void setThreshold( double threshold ) {
        this.threshold = threshold;
    }

    /**
     * @return
     */
    public String getTaxonName() {
        return taxonName;
    }

    /**
     * @param taxonName
     */
    public void setTaxonName( String taxonName ) {
        this.taxonName = taxonName;
    }

}
