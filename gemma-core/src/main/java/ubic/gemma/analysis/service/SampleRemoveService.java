/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.analysis.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.SampleRemovalEvent;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

/**
 * Service for removing sample(s) from an expression experiment. This can be done in the interest of quality control.
 * <p>
 * NOTE currently this does not actually remove the samples. It just replaces the data with "missing values". This means
 * the data are not recoverable. The reason we don't simply mark it as missing in the "absent-present" data (and leave
 * the regular data alone) is that for many data sets we either 1) don't already have an absent-present data type or 2)
 * the absent-present data is not used in analysis. We will probably change this behavior to preserve the data in
 * question.
 * <p>
 * In the meantime, this should be used very judiciously!
 * 
 * @spring.bean id="sampleRemoveService"
 * @spring.property ref="expressionExperimentService" name="expressionExperimentService"
 * @spring.property ref="auditTrailService" name="auditTrailService"
 * @spring.property name="bioAssayService" ref = "bioAssayService"
 * @author pavlidis
 * @version $Id$
 */
public class SampleRemoveService extends ExpressionExperimentVectorManipulatingService {

    private static Log log = LogFactory.getLog( SampleRemoveService.class.getName() );

    BioAssayService bioAssayService;

    AuditTrailService auditTrailService;

    ExpressionExperimentService expressionExperimentService;

    public void setBioAssayService( BioAssayService bioAssayService ) {
        this.bioAssayService = bioAssayService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * This does not actually remove the sample; rather, it sets all values to "missing".
     * 
     * @param expExp
     * @param bioAssay
     */
    public void markAsMissing( ExpressionExperiment expExp, BioAssay bioAssay ) {
        Collection<BioAssay> bms = new HashSet<BioAssay>();
        bms.add( bioAssay );
        this.markAsMissing( expExp, bms );
    }

    /**
     * This does not actually remove the sample; rather, it sets all values to "missing".
     * 
     * @param expExp
     * @param bioAssay
     */
    public void markAsMissing( BioAssay bioAssay ) {
        Collection<BioAssay> bms = new HashSet<BioAssay>();
        bms.add( bioAssay );
        bioAssayService.thaw( bioAssay );
        ExpressionExperiment expExp = expressionExperimentService.findByBioMaterial( bioAssay.getSamplesUsed()
                .iterator().next() );
        assert expExp != null;
        this.markAsMissing( expExp, bms );
    }

    /**
     * This does not actually remove the sample; rather, it sets all values to "missing".
     * 
     * @param expExp
     * @param assaysToRemove
     */
    @SuppressWarnings("unchecked")
    public void markAsMissing( ExpressionExperiment expExp, Collection<BioAssay> assaysToRemove ) {

        if ( assaysToRemove == null || assaysToRemove.size() == 0 ) return;

        // thaw vectors for each QT
        expressionExperimentService.thawLite( expExp );

        Collection<QuantitationType> qts = expressionExperimentService.getQuantitationTypes( expExp );

        Collection<ArrayDesign> arrayDesigns = expressionExperimentService.getArrayDesignsUsed( expExp );

        if ( arrayDesigns.size() > 1 ) {
            throw new IllegalArgumentException( "Cannot cope with more than one platform: merge vectors first!" );
        }

        Collection<BioAssayDimension> dims = new HashSet<BioAssayDimension>();
        for ( QuantitationType type : qts ) {
            log.info( "Marking outlier for " + type + "; loading vectors ..." );
            Collection<? extends DesignElementDataVector> oldVectors = getVectorsForOneQuantitationType( type );
            PrimitiveType representation = type.getRepresentation();

            int count = 0;
            for ( DesignElementDataVector vector : oldVectors ) {

                BioAssayDimension bad = vector.getBioAssayDimension();
                dims.add( bad );
                List<BioAssay> vectorAssays = ( List<BioAssay> ) bad.getBioAssays();

                if ( !CollectionUtils.containsAny( vectorAssays, assaysToRemove ) ) continue;

                LinkedList<Object> data = new LinkedList<Object>();
                convertFromBytes( data, representation, vector );

                // now set data as missing.
                int i = 0;
                for ( BioAssay vecAs : vectorAssays ) {
                    if ( assaysToRemove.contains( vecAs ) ) {

                        if ( representation.equals( PrimitiveType.DOUBLE ) ) {
                            data.set( i, Double.NaN );
                        } else if ( representation.equals( PrimitiveType.BOOLEAN ) ) {
                            data.set( i, Boolean.FALSE );
                        } else if ( representation.equals( PrimitiveType.INT ) ) {
                            data.set( i, 0 );
                        } else if ( representation.equals( PrimitiveType.STRING ) ) {
                            data.set( i, "" );
                        } else {
                            throw new UnsupportedOperationException(
                                    "Don't know how to make a missing value placeholder for " + representation
                                            + " QT =" + type );
                        }
                    }
                    i++;
                }

                // convert it back.
                byte[] newDataAr = converter.toBytes( data.toArray() );
                vector.setData( newDataAr );
                if ( ++count % 5000 == 0 ) {
                    log.info( "Edited " + count + " vectors ... " );
                }
            }

            log.info( "Committing changes to " + oldVectors.size() + " vectors" );

            designElementDataVectorService.update( oldVectors );

        }

        log.info( "Logging event." );
        for ( BioAssay ba : assaysToRemove ) {
            audit( ba, "Sample " + ba.getName() + " marked as missing data." );
        }
    }

    public void setAuditTrailService( AuditTrailService auditTrailService ) {
        this.auditTrailService = auditTrailService;
    }

    /**
     * @param arrayDesign
     */
    private void audit( BioAssay bioAssay, String note ) {
        AuditEventType eventType = SampleRemovalEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( bioAssay, eventType, note );
    }

}
