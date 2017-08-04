/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.core.apps;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.model.common.search.SearchSettingsImpl;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.AbstractCLIContextCLI;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Base class for CLIs that needs one or more expression experiment as an input. It offers the following ways of reading
 * them in:
 * <ul>
 * <li>All EEs
 * <li>All EEs for a particular taxon.
 * <li>A specific ExpressionExperimentSet, identified by name</li>
 * <li>A comma-delimited list of one or more EEs identified by short name given on the command line
 * <li>From a file, with one short name per line.
 * <li>EEs matching a query string (e.g., 'brain')
 * <li>(Optional) 'Auto' mode, in which experiments to analyze are selected automatically based on their workflow state.
 * This can be enabled and modified by subclasses who override the "needToRun" method.
 * <li>All EEs that were last processed after a given date, similar to 'auto' otherwise.
 * </ul>
 * Some of these options can be (or should be) combined, and modified by a (optional) "force" option, and will have
 * customized behavior.
 * In addition, EEs can be excluded based on a list given in a separate file.
 *
 * @author Paul
 */
public abstract class ExpressionExperimentManipulatingCLI extends AbstractCLIContextCLI {
    protected ExpressionExperimentService eeService;
    protected Set<BioAssaySet> expressionExperiments = new HashSet<>();
    protected boolean force = false;
    protected GeneService geneService;
    protected SearchService searchService;
    protected Taxon taxon = null;
    protected TaxonService taxonService;

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.EXPERIMENT;
    }

    protected void addForceOption() {
        this.addForceOption( null );
    }

    protected void addForceOption( String explanation ) {
        String defaultExplanation = "Ignore other reasons for skipping experiments (e.g., trouble) and overwrite existing data (see documentation for this tool to see exact behavior if not clear)";
        String usedExpl = explanation == null ? defaultExplanation : explanation;
        @SuppressWarnings("static-access")
        Option forceOption = OptionBuilder.withArgName( "Force processing" ).withLongOpt( "force" )
                .withDescription( usedExpl ).create( "force" );
        addOption( forceOption );
    }

    @Override
    @SuppressWarnings("static-access")
    protected void buildOptions() {
        Option expOption = OptionBuilder.hasArg().withArgName( "shortname" ).withDescription(
                "Expression experiment short name. Most tools recognize comma-delimited values given on the command line, "
                        + "and if this option is omitted (and none other provided), the tool will be applied to all expression experiments." )
                .withLongOpt( "experiment" ).create( 'e' );

        addOption( expOption );

        Option eeFileListOption = OptionBuilder.hasArg().withArgName( "file" ).withDescription(
                "File with list of short names or IDs of expression experiments (one per line; use instead of '-e')" )
                .withLongOpt( "eeListfile" ).create( 'f' );
        addOption( eeFileListOption );

        Option eeSetOption = OptionBuilder.hasArg().withArgName( "eeSetName" )
                .withDescription( "Name of expression experiment set to use" ).create( "eeset" );

        addOption( eeSetOption );

        Option taxonOption = OptionBuilder.hasArg().withDescription( "taxon name" )
                .withDescription( "Taxon of the expression experiments and genes" ).withLongOpt( "taxon" )
                .create( 't' );
        addOption( taxonOption );

        Option excludeEeOption = OptionBuilder.hasArg().withArgName( "file" )
                .withDescription( "File containing list of expression experiments to exclude" )
                .withLongOpt( "excludeEEFile" ).create( 'x' );
        addOption( excludeEeOption );

        Option eeSearchOption = OptionBuilder.hasArg().withArgName( "expressionQuery" )
                .withDescription( "Use a query string for defining which expression experiments to use" )
                .withLongOpt( "expressionQuery" ).create( 'q' );
        addOption( eeSearchOption );

    }

    protected Gene findGeneByOfficialSymbol( String symbol, Taxon t ) {
        Collection<Gene> genes = geneService.findByOfficialSymbolInexact( symbol );
        for ( Gene gene : genes ) {
            if ( t.equals( gene.getTaxon() ) )
                return gene;
        }
        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        eeService = this.getBean( ExpressionExperimentService.class );
        geneService = this.getBean( GeneService.class );
        taxonService = this.getBean( TaxonService.class );
        searchService = this.getBean( SearchService.class );
        this.auditEventService = getBean( AuditEventService.class );
        if ( hasOption( 't' ) ) {
            this.taxon = setTaxonByName( taxonService );
        }

        if ( hasOption( "force" ) ) {
            this.force = true;
        }

        if ( this.hasOption( "eeset" ) ) {
            experimentsFromEeSet( getOptionValue( "eeset" ) );
        } else if ( this.hasOption( 'e' ) ) {
            experimentsFromCliList();
        } else if ( hasOption( 'f' ) ) {
            String experimentListFile = getOptionValue( 'f' );
            log.info( "Reading experiment list from " + experimentListFile );
            try {
                this.expressionExperiments = readExpressionExperimentListFile( experimentListFile );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        } else if ( hasOption( 'q' ) ) {
            log.info( "Processing all experiments that match query " + getOptionValue( 'q' ) );
            this.expressionExperiments = this.findExpressionExperimentsByQuery( getOptionValue( 'q' ) );
        } else if ( taxon != null ) {
            if ( !hasOption( "dataFile" ) ) {
                log.info( "Processing all experiments for " + taxon.getCommonName() );
                this.expressionExperiments = new HashSet<BioAssaySet>( eeService.findByTaxon( taxon ) );
            }
        } else {
            if ( !hasOption( "dataFile" ) ) {
                log.info( "Processing all experiments (further filtering may modify)" );
                this.expressionExperiments = new HashSet<BioAssaySet>( eeService.loadAll() );
            }
        }

        if ( hasOption( 'x' ) ) {
            excludeFromFile();
        }

        if ( expressionExperiments != null && expressionExperiments.size() > 0 && !force ) {

            if ( hasOption( AUTO_OPTION_NAME ) ) {
                this.autoSeek = true;
                if ( this.autoSeekEventType == null ) {
                    throw new IllegalStateException( "Programming error: there is no 'autoSeekEventType' set" );
                }
                log.info( "Filtering for experiments lacking a " + this.autoSeekEventType.getSimpleName() + " event" );
                auditEventService.retainLackingEvent( this.expressionExperiments, this.autoSeekEventType );
            }

            removeTroubledEes( expressionExperiments );
        }

        if ( expressionExperiments.size() > 1 ) {
            log.info( "Final list: " + this.expressionExperiments.size()
                    + " expressionExperiments (futher filtering may modify)" );
        } else if ( expressionExperiments.size() == 0 ) {
            if ( hasOption( "dataFile" ) ) {
                log.info( "Expression matrix from data file selected" );
            } else {
                log.info( "No experiments selected" );
            }
        }

    }

    /**
     * Read in a list of genes
     *
     * @param inFile - file name to read
     * @return collection of genes
     */
    protected Collection<Gene> readGeneListFile( String inFile, Taxon t ) throws IOException {
        log.info( "Reading " + inFile );

        Collection<Gene> genes = new ArrayList<>();
        try (BufferedReader in = new BufferedReader( new FileReader( inFile ) )) {
            String line;
            while ( ( line = in.readLine() ) != null ) {
                if ( line.startsWith( "#" ) )
                    continue;
                String s = line.trim();
                Gene gene = findGeneByOfficialSymbol( s, t );
                if ( gene == null ) {
                    log.error( "ERROR: Cannot find genes for " + s );
                    continue;
                }
                genes.add( gene );
            }
            return genes;
        }
    }

    /**
     *
     */
    private void excludeFromFile() {
        String excludeEeFileName = getOptionValue( 'x' );
        Collection<BioAssaySet> excludeExperiments;
        try {
            excludeExperiments = readExpressionExperimentListFile( excludeEeFileName );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        assert expressionExperiments.size() > 0;

        int before = expressionExperiments.size();

        expressionExperiments.removeAll( excludeExperiments );
        int removed = before - expressionExperiments.size();

        if ( removed > 0 )
            log.info( "Excluded " + removed + " expression experiments" );
    }

    /**
     *
     */
    private void experimentsFromCliList() {
        String experimentShortNames = this.getOptionValue( 'e' );
        String[] shortNames = experimentShortNames.split( "," );

        for ( String shortName : shortNames ) {
            ExpressionExperiment expressionExperiment = locateExpressionExperiment( shortName );
            if ( expressionExperiment == null ) {
                log.warn( shortName + " not found" );
                continue;
            }
            eeService.thawLite( expressionExperiment );
            expressionExperiments.add( expressionExperiment );
        }
        if ( expressionExperiments.size() == 0 ) {
            log.error( "There were no valid experimnents specified" );
            bail( ErrorCode.INVALID_OPTION );
        }
    }

    private void experimentsFromEeSet( String optionValue ) {

        if ( StringUtils.isBlank( optionValue ) ) {
            throw new IllegalArgumentException( "Please provide an eeset name" );
        }

        ExpressionExperimentSetService expressionExperimentSetService = this
                .getBean( ExpressionExperimentSetService.class );
        Collection<ExpressionExperimentSet> sets = expressionExperimentSetService.findByName( optionValue );
        if ( sets.size() > 1 ) {
            throw new IllegalArgumentException( "More than on EE set has name '" + optionValue + "'" );
        } else if ( sets.size() == 0 ) {
            throw new IllegalArgumentException( "No EE set has name '" + optionValue + "'" );
        }
        ExpressionExperimentSet set = sets.iterator().next();
        this.expressionExperiments = new HashSet<>( set.getExperiments() );

    }

    /**
     * Use the search engine to locate expression experiments.
     */
    private Set<BioAssaySet> findExpressionExperimentsByQuery( String query ) {
        Set<BioAssaySet> ees = new HashSet<>();
        Collection<SearchResult> eeSearchResults = searchService
                .search( SearchSettingsImpl.expressionExperimentSearch( query ) ).get( ExpressionExperiment.class );

        log.info( ees.size() + " Expression experiments matched '" + query + "'" );

        // Filter out all the ee that are not of correct taxon
        for ( SearchResult sr : eeSearchResults ) {
            ExpressionExperiment ee = ( ExpressionExperiment ) sr.getResultObject();
            Taxon t = eeService.getTaxon( ee );
            if ( t != null && t.getCommonName().equalsIgnoreCase( taxon.getCommonName() ) ) {
                ees.add( ee );
            }
        }
        return ees;

    }

    private ExpressionExperiment locateExpressionExperiment( String name ) {

        if ( name == null ) {
            errorObjects.add( "Expression experiment short name must be provided" );
            return null;
        }

        ExpressionExperiment experiment = eeService.findByShortName( name );

        if ( experiment == null ) {
            log.error( "No experiment " + name + " found" );
            bail( ErrorCode.INVALID_OPTION );
        }
        return experiment;
    }

    /**
     * Load expression experiments based on a list of short names or IDs in a file. Only the first column of the file is
     * used, comments (#) are allowed.
     */
    private Set<BioAssaySet> readExpressionExperimentListFile( String fileName ) throws IOException {
        Set<BioAssaySet> ees = new HashSet<>();
        for ( String eeName : readListFileToStrings( fileName ) ) {
            ExpressionExperiment ee = eeService.findByShortName( eeName );
            if ( ee == null ) {

                try {
                    Long id = Long.parseLong( eeName );
                    ee = eeService.load( id );
                    if ( ee == null ) {
                        log.error( "No experiment " + eeName + " found" );
                        continue;
                    }
                } catch ( NumberFormatException e ) {
                    log.error( "No experiment " + eeName + " found" );
                    continue;

                }

            }
            ees.add( ee );
        }
        return ees;
    }

    /**
     * removes EEs that are troubled, or their parent Array design is troubled.
     */
    private void removeTroubledEes( Collection<BioAssaySet> ees ) {
        if ( ees == null || ees.size() == 0 ) {
            log.warn( "No experiments to remove troubled from" );
            return;
        }

        BioAssaySet theOnlyOne = null;
        if ( ees.size() == 1 ) {
            theOnlyOne = ees.iterator().next();
        }
        int size = ees.size();

        CollectionUtils.filter( ees, new Predicate() {
            @Override
            public boolean evaluate( Object object ) {
                return !( ( ExpressionExperiment ) object ).getCurationDetails().getTroubled();
            }
        } );
        int newSize = ees.size();
        if ( newSize != size ) {
            assert newSize < size;
            if ( size == 1 && theOnlyOne != null ) {
                log.info( theOnlyOne.getName() + " has an active trouble flag" );
            } else {
                log.info( "Removed " + ( size - newSize ) + " experiments with 'trouble' flags, leaving " + newSize );
            }
        }
    }

}