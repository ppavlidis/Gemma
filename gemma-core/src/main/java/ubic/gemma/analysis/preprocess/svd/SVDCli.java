/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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

package ubic.gemma.analysis.preprocess.svd;

import ubic.gemma.apps.ExpressionExperimentManipulatingCLI;
import ubic.gemma.model.common.auditAndSecurity.eventType.PCAAnalysisEvent;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author paul
 * @version $Id$
 */
public class SVDCli extends ExpressionExperimentManipulatingCLI {

    @Override
    protected void buildOptions() {
        super.buildOptions();
        super.addForceOption();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception ee = super.processCommandLine( "SVD", args );

        if ( ee != null ) return ee;

        SVDService svdser = ( SVDService ) this.getBean( "sVDService" );

        for ( BioAssaySet bas : this.expressionExperiments ) {

            if ( !force && !needToRun( bas, PCAAnalysisEvent.class ) ) {
                continue;
            }

            try {
                log.info( "Processing: " + bas );
                svdser.svd( ( ExpressionExperiment ) bas );
                this.successObjects.add( bas.toString() );
            } catch ( Exception e ) {
                log.error( e, e );
                this.errorObjects.add( ee + ": " + e.getMessage() );
            }
        }
        summarizeProcessing();
        return null;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        SVDCli s = new SVDCli();
        Exception e = s.doWork( args );

        if ( e != null ) {
            log.error( e, e );
        }

    }

}
