/*
 * The Gemma project.
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
package ubic.gemma.core.apps;

import gemma.gsec.SecurityService;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationServiceImpl;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationServiceImpl.OutputType;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignAnnotationFileEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.core.ontology.providers.GeneOntologyService;

import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * Given an array design creates a Gene Ontology Annotation file
 * Given a batch file creates all the Annotation files for the AD's specified in the batch file
 * Given nothing creates annotation files for every AD that isn't subsumed or merged into another AD.
 *
 * @author klc
 */
public class ArrayDesignAnnotationFileCli extends ArrayDesignSequenceManipulatingCli {

    private static final String GENE_NAME_LIST_FILE_OPTION = "genefile";
    // file info
    private String batchFileName;
    private String fileName = null;
    /**
     * Clobber existing file, if any.
     */
    private boolean overWrite = false;
    private boolean processAllADs = false;
    private OutputType type = OutputType.SHORT;
    private ArrayDesignAnnotationService arrayDesignAnnotationService;
    private CompositeSequenceService compositeSequenceService;
    private boolean doAllTypes = false;
    private String geneFileName;
    private GeneOntologyService goService;
    private String taxonName;

    public static void main( String[] args ) {
        ArrayDesignAnnotationFileCli p = new ArrayDesignAnnotationFileCli();
        tryDoWork( p, args );
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.PLATFORM;
    }

    @Override
    public String getShortDesc() {
        return "Generate annotation files for platforms.";
    }

    @Override
    public String getCommandName() {
        return "makePlatformAnnotFiles";
    }

    @SuppressWarnings("static-access") // This is a much more readable syntax in this case
    @Override
    protected void buildOptions() {
        super.buildOptions();

        Option annotationFileOption = OptionBuilder.hasArg().withArgName( "Annotation file name" )
                .withDescription( "The name of the Annotation file to be generated [Default = Accession number]" )
                .withLongOpt( "annotation" ).create( 'f' );

        Option annotationType = OptionBuilder.hasArg().withArgName( "Type of annotation file" ).withDescription(
                "Which GO terms to add to the annotation file:  short, long, or bioprocess; 'all' to generate all 3 "
                        + "[Default=short (no parents)]. If you select bioprocess, parents are not included." )
                .withLongOpt( "type" ).create( 't' );

        Option fileLoading = OptionBuilder.hasArg().withArgName( "Batch Generating of annotation files" )
                .withDescription( "Use specified file for batch generating annotation files.  "
                        + "specified File format (per line): shortName,outputFileName,[short|long|biologicalprocess] Note:  Overrides -a,-t,-f command line options " )
                .withLongOpt( "load" ).create( 'l' );

        Option batchLoading = OptionBuilder.withArgName( "Generating all annotation files" ).withDescription(
                "Generates annotation files for all Array Designs (omits ones that are subsumed or merged) uses accession as annotation file name."
                        + "Creates 3 zip files for each AD, no parents, parents, biological process. Overrides all other settings except '--taxon'." )
                .withLongOpt( "batch" ).create( 'b' );

        Option geneListFile = OptionBuilder.hasArg()
                .withDescription( "Create from a file containing a list of gene symbols instead of probe ids" )
                .create( GENE_NAME_LIST_FILE_OPTION );
        addOption( geneListFile );

        Option taxonNameOption = OptionBuilder.hasArg().withDescription(
                "Taxon short name e.g. 'mouse' (use with --genefile, or alone to process all "
                        + "known genes for the taxon, or with --batch to process all arrays for the taxon." )
                .create( "taxon" );
        addOption( taxonNameOption );

        Option overWriteOption = OptionBuilder.withArgName( "Overwrites existing files" )
                .withDescription( "If set will overwrite existing annotation files in the output directory" )
                .withLongOpt( "overwrite" ).create( 'o' );

        addOption( annotationFileOption );
        addOption( annotationType );
        addOption( fileLoading );
        addOption( batchLoading );
        addOption( overWriteOption );
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( args );
        if ( err != null )
            return err;

        try {
            this.goService.init( true );
            waitForGeneOntologyReady();

            if ( StringUtils.isNotBlank( geneFileName ) ) {
                processGeneList();
            } else if ( processAllADs ) {
                processAllADs();
            } else if ( batchFileName != null ) {
                processBatchFile();
            } else if ( this.taxonName != null ) {
                processGenesForTaxon();
            } else {
                if ( this.arrayDesignsToProcess.isEmpty() ) {
                    throw new IllegalArgumentException( "You must specify a platform, a taxon, gene file, or batch." );
                }
                for ( ArrayDesign arrayDesign : this.arrayDesignsToProcess ) {
                    if ( doAllTypes ) {
                        // make all three
                        processOneAD( arrayDesign );
                    } else {
                        processAD( arrayDesign, this.fileName, type );
                    }
                }
            }

        } catch ( Exception e ) {
            return e;
        }

        return null;
    }

    private boolean processAD( ArrayDesign arrayDesign, String fileBaseName, OutputType outputType )
            throws IOException {

        log.info( "Loading gene information for " + arrayDesign );
        ArrayDesign thawed = unlazifyArrayDesign( arrayDesign );

        if ( thawed.getCurationDetails().getTroubled() ) {
            log.warn( "Troubled: " + arrayDesign );
            return false;
        }

        Collection<CompositeSequence> compositeSequences = thawed.getCompositeSequences();

        Map<CompositeSequence, Collection<BioSequence2GeneProduct>> genesWithSpecificity = compositeSequenceService
                .getGenesWithSpecificity( compositeSequences );

        boolean hasAtLeastOneGene = false;
        for ( CompositeSequence c : genesWithSpecificity.keySet() ) {
            if ( genesWithSpecificity.get( c ).isEmpty() ) {
                continue;
            }
            hasAtLeastOneGene = true;
            break;
        }

        if ( !hasAtLeastOneGene ) {
            log.warn( "No genes: " + thawed );
            return false;
        }

        log.info( "Preparing file" );
        return processCompositeSequences( thawed, fileBaseName, outputType, genesWithSpecificity );

    }

    /**
     * Goes over all the AD's in the database (possibly limited by taxon) and creates annotation 3 annotation files for
     * each AD that is not merged into or subsumed by another AD. Uses the Accession ID (GPL???) for the name of the
     * annotation file. Appends noParents, bioProcess, allParents to the file name.
     */
    private void processAllADs() throws IOException {

        Collection<ArrayDesign> allADs = this.arrayDesignService.loadAll();

        for ( ArrayDesign ad : allADs ) {

            ad = arrayDesignService.thawLite( ad );
            if ( ad.getCurationDetails().getTroubled() ) {
                log.warn( "Troubled: " + ad + " (skipping)" );
                continue;
            }

            Taxon taxon = null;
            if ( this.taxonName != null ) {
                TaxonService taxonService = getBean( TaxonService.class );
                taxon = taxonService.findByCommonName( taxonName );
                if ( taxon == null ) {
                    throw new IllegalArgumentException( "Unknown taxon: " + taxonName );
                }
            }

            Collection<Taxon> adTaxa = arrayDesignService.getTaxa( ad.getId() );

            /*
             * If using taxon, check it.
             */
            if ( taxon != null && !adTaxa.contains( taxon ) ) {
                continue;
            }
            processOneAD( ad );

        }

    }

    /**
     * @throws IOException used for batch processing
     */
    private void processBatchFile() throws IOException {

        log.info( "Loading platforms to annotate from " + this.batchFileName );
        InputStream is = new FileInputStream( this.batchFileName );
        try (BufferedReader br = new BufferedReader( new InputStreamReader( is ) )) {

            String line;
            int lineNumber = 0;
            while ( ( line = br.readLine() ) != null ) {
                lineNumber++;
                if ( StringUtils.isBlank( line ) ) {
                    continue;
                }

                String[] arguments = StringUtils.split( line, ',' );

                String accession = arguments[0];
                String annotationFileName = arguments[1];
                String type = arguments[2];

                // Check the syntax of the given line
                if ( ( accession == null ) || StringUtils.isBlank( accession ) ) {
                    log.warn( "Incorrect line format in Batch Annotation file: Line " + lineNumber
                            + "Platform is required: " + line );
                    log.warn( "Unable to process that line. Skipping to next." );
                    continue;
                }
                if ( ( annotationFileName == null ) || StringUtils.isBlank( annotationFileName ) ) {
                    annotationFileName = accession;
                    log.warn( "No annotation file name specified on line: " + lineNumber
                            + " Using platform name as default annotation file name" );
                }
                if ( StringUtils.isBlank( type ) ) {
                    this.type = OutputType.SHORT;
                    log.warn( "No type specified for line: " + lineNumber + " Defaulting to short" );
                } else {
                    this.type = OutputType.valueOf( type.toUpperCase() );
                }

                // need to set these so processing ad works correctly
                // TODO: make process type take all 3 parameter
                ArrayDesign arrayDesign = locateArrayDesign( accession, arrayDesignService );

                try {
                    processAD( arrayDesign, annotationFileName, this.type );
                } catch ( Exception e ) {
                    log.error( "**** Exception while processing " + arrayDesign + ": " + e.getMessage() + " ********" );
                    log.error( e, e );
                    cacheException( e );
                    errorObjects.add( arrayDesign + ": " + e.getMessage() );
                }

            }
        }

        summarizeProcessing();

    }

    @Override
    protected void processOptions() {

        if ( this.hasOption( 'f' ) ) {
            this.fileName = this.getOptionValue( 'f' );
        }

        if ( this.hasOption( 't' ) ) {
            if ( this.getOptionValue( 't' ).equalsIgnoreCase( "all" ) ) {
                this.doAllTypes = true;
            } else {
                this.type = OutputType.valueOf( this.getOptionValue( 't' ).toUpperCase() );
            }
        }

        if ( this.hasOption( 'l' ) ) {
            this.batchFileName = this.getOptionValue( 'l' );
        }

        if ( this.hasOption( 'b' ) ) {
            this.processAllADs = true;

            if ( this.hasOption( 'a' ) ) {
                throw new IllegalArgumentException(
                        "--batch overrides -a to run all platforms. If you want to run like --batch but for selected platforms use -a with -t all" );
            }

        }

        if ( this.hasOption( GENE_NAME_LIST_FILE_OPTION ) ) {
            this.geneFileName = this.getOptionValue( GENE_NAME_LIST_FILE_OPTION );
            if ( !this.hasOption( "taxon" ) ) {
                throw new IllegalArgumentException( "You must specify the taxon when using --genefile" );
            }
            this.taxonName = this.getOptionValue( "taxon" );

        }

        if ( this.hasOption( "taxon" ) ) {
            this.taxonName = this.getOptionValue( "taxon" );
            if ( this.hasOption( 'b' ) ) {
                log.info( "Will batch process array designs for " + this.taxonName );
            }
        }

        if ( this.hasOption( 'o' ) )
            this.overWrite = true;

        super.processOptions();

        this.arrayDesignAnnotationService = getBean( ArrayDesignAnnotationService.class );
        this.goService = getBean( GeneOntologyService.class );
        this.compositeSequenceService = getBean( CompositeSequenceService.class );
    }

    private void audit( ArrayDesign arrayDesign, String note ) {

        SecurityService ss = this.getBean( SecurityService.class );
        if ( !ss.isEditable( arrayDesign ) )
            return;

        AuditEventType eventType = ArrayDesignAnnotationFileEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( arrayDesign, eventType, note );
    }

    private boolean processCompositeSequences( ArrayDesign arrayDesign, String fileBaseName, OutputType outputType,
            Map<CompositeSequence, Collection<BioSequence2GeneProduct>> genesWithSpecificity ) throws IOException {

        if ( genesWithSpecificity.size() == 0 ) {
            log.info( "No sequence information for " + arrayDesign + ", skipping" );
            return false;
        }

        try (Writer writer = arrayDesignAnnotationService.initOutputFile( arrayDesign, fileBaseName, this.overWrite )) {

            // if no writer then we should abort (this could happen in case where we don't want to overwrite files)
            if ( writer == null ) {
                log.info( arrayDesign.getName() + " annotation file already exits.  Skipping. " );
                return false;
            }

            log.info( arrayDesign.getName() + " has " + genesWithSpecificity.size() + " composite sequences" );

            int numProcessed = arrayDesignAnnotationService
                    .generateAnnotationFile( writer, genesWithSpecificity, outputType );

            log.info( "Finished processing platform: " + arrayDesign.getName() );

            successObjects.add( String.format( "%s (%s)", arrayDesign.getName(), arrayDesign.getShortName() ) );

            if ( StringUtils.isBlank( fileBaseName ) ) {
                log.info( "Processed " + numProcessed + " composite sequences" );
                audit( arrayDesign, "Processed " + numProcessed + " composite sequences" );
            } else {
                String filename = fileBaseName + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX;
                log.info( "Created file:  " + filename + " with " + numProcessed + " values" );
                audit( arrayDesign, "Created file: " + filename + " with " + numProcessed + " values" );
            }
            return true;
        }
    }

    private void processGeneList() throws IOException {
        log.info( "Loading genes to annotate from " + geneFileName );
        InputStream is = new FileInputStream( geneFileName );
        try (BufferedReader br = new BufferedReader( new InputStreamReader( is ) )) {
            String line;
            GeneService geneService = getBean( GeneService.class );
            TaxonService taxonService = getBean( TaxonService.class );
            Taxon taxon = taxonService.findByCommonName( taxonName );
            if ( taxon == null ) {
                throw new IllegalArgumentException( "Unknown taxon: " + taxonName );
            }
            Collection<Gene> genes = new HashSet<>();
            while ( ( line = br.readLine() ) != null ) {
                if ( StringUtils.isBlank( line ) ) {
                    continue;
                }
                String[] arguments = StringUtils.split( line, '\t' );
                String gene = arguments[0];
                Gene g = geneService.findByOfficialSymbol( gene, taxon );
                if ( g == null ) {
                    log.info( "Gene: " + gene + " not found." );
                    continue;
                }
                genes.add( g );
            }
            log.info( "File contained " + genes.size() + " potential gene symbols" );
            int numProcessed = arrayDesignAnnotationService
                    .generateAnnotationFile( new PrintWriter( System.out ), genes, OutputType.SHORT );
            log.info( "Processed " + numProcessed + " genes that were found" );
        }
    }

    private void processGenesForTaxon() {
        GeneService geneService = getBean( GeneService.class );
        TaxonService taxonService = getBean( TaxonService.class );
        Taxon taxon = taxonService.findByCommonName( taxonName );
        if ( taxon == null ) {
            throw new IllegalArgumentException( "Unknown taxon: " + taxonName );
        }
        log.info( "Processing all genes for " + taxon );
        Collection<Gene> genes = geneService.loadAll( taxon );
        log.info( "Taxon has " + genes.size() + " 'known' genes" );
        int numProcessed = arrayDesignAnnotationService
                .generateAnnotationFile( new PrintWriter( System.out ), genes, type );
        log.info( "Processed " + numProcessed + " genes that were found" );
    }

    private boolean processOneAD( ArrayDesign inputAd ) throws IOException {
        ArrayDesign ad = unlazifyArrayDesign( inputAd );

        log.info( "Processing AD: " + ad.getName() );

        String shortFileBaseName = ArrayDesignAnnotationServiceImpl.mungeFileName( ad.getShortName() )
                + ArrayDesignAnnotationService.NO_PARENTS_FILE_SUFFIX;
        File sf = ArrayDesignAnnotationServiceImpl.getFileName( shortFileBaseName );
        String biocFileBaseName = ArrayDesignAnnotationServiceImpl.mungeFileName( ad.getShortName() )
                + ArrayDesignAnnotationService.BIO_PROCESS_FILE_SUFFIX;
        File bf = ArrayDesignAnnotationServiceImpl.getFileName( biocFileBaseName );
        String allparFileBaseName = ArrayDesignAnnotationServiceImpl.mungeFileName( ad.getShortName() )
                + ArrayDesignAnnotationService.STANDARD_FILE_SUFFIX;
        File af = ArrayDesignAnnotationServiceImpl.getFileName( allparFileBaseName );

        if ( !overWrite && sf.exists() && bf.exists() && af.exists() ) {
            log.info( "Files exist already, will not overwrite (use --overwrite option to override)" );
            return false;
        }

        Collection<CompositeSequence> compositeSequences = ad.getCompositeSequences();
        log.info( "Starting getting probe specificity" );

        // lmd test

        Map<CompositeSequence, Collection<BioSequence2GeneProduct>> genesWithSpecificity = compositeSequenceService
                .getGenesWithSpecificity( compositeSequences );

        log.info( "Done getting probe specificity" );

        boolean hasAtLeastOneGene = false;
        for ( CompositeSequence c : genesWithSpecificity.keySet() ) {
            if ( genesWithSpecificity.get( c ).isEmpty() ) {
                continue;
            }
            hasAtLeastOneGene = true;
            break;
        }

        if ( !hasAtLeastOneGene ) {
            log.warn( "No genes: " + ad + ", skipping" );
            return false;
        }

        boolean didAnything = false;
        if ( overWrite || !sf.exists() ) {
            processCompositeSequences( ad, shortFileBaseName, OutputType.SHORT, genesWithSpecificity );
            didAnything = true;
        } else {
            log.info( sf + " exists, will not overwrite" );
        }

        if ( overWrite || !bf.exists() ) {
            processCompositeSequences( ad, biocFileBaseName, OutputType.BIOPROCESS, genesWithSpecificity );
            didAnything = true;
        } else {
            log.info( bf + " exists, will not overwrite" );
        }

        if ( overWrite || !af.exists() ) {
            processCompositeSequences( ad, allparFileBaseName, OutputType.LONG, genesWithSpecificity );
            didAnything = true;
        } else {
            log.info( af + " exists, will not overwrite" );
        }
        return didAnything;
    }

    private void waitForGeneOntologyReady() {
        while ( !goService.isReady() ) {
            try {
                Thread.sleep( 10000 );
            } catch ( InterruptedException e ) {
                e.printStackTrace();
            }
            log.info( "Waiting for Gene Ontology to load ..." );
        }
        log.info( "GO is ready." );
    }
}