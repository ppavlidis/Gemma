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


package ubic.gemma.apps;

import java.io.File;

import ubic.gemma.analysis.service.ExpressionDataFileService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEvent;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public class ExpressionExperimentDataFileGeneratorCli extends ExpressionExperimentManipulatingCLI {
    
    private static final boolean FORCE_WRITE = true;

    ExpressionDataFileService expressionDataFileService;

    private String DESCRIPTION = "Generate Flat data files (diff expression, co-expression) for a given set of experiments";
    
    @Override
    protected Exception doWork( String[] args ) {

        Exception exp = processCommandLine( DESCRIPTION, args );
        if ( exp != null ) {
            return exp;
        }

        for ( BioAssaySet ee : expressionExperiments ) {
            if ( ee instanceof ExpressionExperiment ) {
                processExperiment( ( ExpressionExperiment ) ee );
            } else {
                throw new UnsupportedOperationException( "Can't handle non-EE BioAssaySets yet" );
            }

        }
        summarizeProcessing();
        return null;

    }


    private void processExperiment( ExpressionExperiment ee ) {

        try {
            this.eeService.thawLite( ee );

            AuditTrailService auditEventService = ( AuditTrailService ) this.getBean( "auditTrailService" );
            AuditEventType type = CommentedEvent.Factory.newInstance();
            
            File coexpressionFile = expressionDataFileService.writeOrLocateCoexpressionDataFile( ee, FORCE_WRITE );
            File diffExpressionFile = expressionDataFileService.writeOrLocateDiffExpressionDataFile( ee, FORCE_WRITE );
            
            auditEventService.addUpdateEvent( ee, type, "Generated Flat data files for downloading"  );            
            super.successObjects.add("Success:  generated data file for " +ee.getShortName() + " ID=" + ee.getId()  );
        
        } catch ( Exception e ) {
            super.errorObjects.add("FAILED: for ee: "  + ee.getShortName() + " ID= "+ ee.getId() + " Error: " + e.getMessage() );
        }
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        ExpressionExperimentDataFileGeneratorCli p = new ExpressionExperimentDataFileGeneratorCli();
        Exception e = p.doWork( args );
        if ( e != null ) {
            log.fatal( e, e );
        }
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        expressionDataFileService = (ExpressionDataFileService ) this.getBean( "expressionDataFileService" );
    }


    @Override
    public String getShortDesc() {
        return DESCRIPTION;
    }

}
