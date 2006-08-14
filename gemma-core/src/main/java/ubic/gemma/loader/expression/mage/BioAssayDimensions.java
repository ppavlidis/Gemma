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
package ubic.gemma.loader.expression.mage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to hold information on the dimension data extracted from MAGE-ML files.
 * <p>
 * Implementation note: We have to store things as Maps of Strings to the objects of interest, rather than using the
 * object itself as a key, because hashCode() for our entities looks just at the primary key (the id), which is not
 * filled in in many cases (when working with non-persistent objects).
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BioAssayDimensions {
    private Map<String, LinkedHashMap<String, ubic.gemma.model.common.quantitationtype.QuantitationType>> quantitationTypeDimensions = new HashMap<String, LinkedHashMap<String, ubic.gemma.model.common.quantitationtype.QuantitationType>>();

    private Map<String, LinkedHashMap<String, ubic.gemma.model.expression.designElement.DesignElement>> designElementDimensions = new HashMap<String, LinkedHashMap<String, ubic.gemma.model.expression.designElement.DesignElement>>();

    /**
     * @param ba
     * @return
     */
    public List<ubic.gemma.model.common.quantitationtype.QuantitationType> getQuantitationTypeDimension(
            ubic.gemma.model.expression.bioAssay.BioAssay ba ) {
        List<ubic.gemma.model.common.quantitationtype.QuantitationType> result = new ArrayList<ubic.gemma.model.common.quantitationtype.QuantitationType>();
        Map<String, ubic.gemma.model.common.quantitationtype.QuantitationType> qts = quantitationTypeDimensions.get( ba
                .getName() );
        for ( String key : qts.keySet() ) {
            result.add( qts.get( key ) );
        }
        return result;
    }

    /**
     * @param bioAssayName
     * @param convertedQuantitationTypes
     */
    public void addQuantitationTypeDimension( ubic.gemma.model.expression.bioAssay.BioAssay bioAssay,
            LinkedHashMap<String, ubic.gemma.model.common.quantitationtype.QuantitationType> quantitationTypes ) {
        quantitationTypeDimensions.put( bioAssay.getName(), quantitationTypes );
    }

    /**
     * @param bioAssay
     * @param designElements
     */
    public void addDesignElementDimension( ubic.gemma.model.expression.bioAssay.BioAssay bioAssay,
            LinkedHashMap<String, ubic.gemma.model.expression.designElement.DesignElement> designElements ) {
        designElementDimensions.put( bioAssay.getName(), designElements );

    }

    /**
     * @param ba
     * @return
     */
    public List<ubic.gemma.model.expression.designElement.DesignElement> getDesignElementDimension(
            ubic.gemma.model.expression.bioAssay.BioAssay ba ) {
        List<ubic.gemma.model.expression.designElement.DesignElement> result = new ArrayList<ubic.gemma.model.expression.designElement.DesignElement>();
        Map<String, ubic.gemma.model.expression.designElement.DesignElement> dts = designElementDimensions.get( ba
                .getName() );

        if ( dts == null ) {
            throw new RuntimeException( ba.getName() + " was not found in designElementDimensions ("
                    + designElementDimensions.size() + " dimensions available)" );
        }

        for ( String key : dts.keySet() ) {
            result.add( dts.get( key ) );
        }
        return result;
    }

}
