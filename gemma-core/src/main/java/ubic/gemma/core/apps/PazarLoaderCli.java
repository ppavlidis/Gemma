/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.core.apps;

import java.io.File;

import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang3.StringUtils;

import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.loader.pazar.PazarLoader;
import ubic.gemma.persistence.service.association.TfGeneAssociationService;
import ubic.gemma.core.util.AbstractCLIContextCLI;

/**
 * @author paul
 * @version $Id$
 */
public class PazarLoaderCli extends AbstractCLIContextCLI {

    public static void main( String[] args ) {
        PazarLoaderCli c = new PazarLoaderCli();
        Exception e = c.doWork( args );
        if ( e != null ) {
            log.fatal( e );
        }
    }

    private File file = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.AbstractCLI#getCommandName()
     */
    @Override
    public String getCommandName() {
        return "loadPazar";
    }

    @Override
    public String getShortDesc() {
        return "Loads Pazar records into the database";
    }
    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.MISC;
    }
    @Override
    protected void buildOptions() {
        OptionBuilder.isRequired();
        OptionBuilder.withLongOpt( "file" );
        OptionBuilder.hasArg();
        super.addOption( OptionBuilder.create( 'f' ) );
    }

    @Override
    protected Exception doWork( String[] args ) {
        super.processCommandLine( args );

        TfGeneAssociationService tfs = this.getBean( TfGeneAssociationService.class );
        tfs.removeAll();

        PazarLoader l = this.getBean( PazarLoader.class );

        try {
            l.load( file );
        } catch ( Exception e ) {
            return e;
        }

        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 'f' ) ) {
            String fileName = this.getOptionValue( 'f' );
            if ( StringUtils.isBlank( fileName ) ) {
                throw new IllegalArgumentException( "file name required" );
            }
            this.file = new File( fileName );
            if ( !file.canRead() ) {
                throw new IllegalArgumentException( "Cannot read from " + file );
            }
        }
    }

}