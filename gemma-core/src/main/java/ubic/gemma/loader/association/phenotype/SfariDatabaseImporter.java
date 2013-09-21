package ubic.gemma.loader.association.phenotype;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import ubic.basecode.util.StringUtil;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ScoreValueObject;

public class SfariDatabaseImporter extends ExternalDatabaseEvidenceImporterAbstractCLI {

    // name of the external database
    public static final String SFARI = "SFARI";
    // messy file, need to be corrected each time not a real csv file, some conflicts with extra character "
    private File autismGeneDataset = null;
    private File geneScore = null;

    // there are found using the score file
    // ****************************************************************************************
    public static String GENE_SYMBOL_SCORE_HEADER = "Gene Symbol";
    public static String SCORE_HEADER = "Score";
    public static String SCORE_DETAILS_HEADER = "Score Details";
    public static String DESCRIPTION_SCORE_HEADER = "Evidence";
    
    // gene Id ---> score
    private HashMap<String, ScoreValueObject> gene2Score = new HashMap<String, ScoreValueObject>();
    // gene Id ---> description
    private HashMap<String, String> gene2Description = new HashMap<String, String>();
    // ****************************************************************************************
    // the gene file
    // DEFNIFE COLUMNS NAMES AS THEY APPEAR IN FILE
    public static String GENE_SYMBOL_HEADER = "Gene Symbol";
    public static String ENTREZ_GENE_ID_HEADER = "Entrez GeneID";
    public static String SUPPORT_FOR_AUTISM_HEADER = "Support for Autism";
    public static String EVIDENCE_OF_SUPPORT_HEADER = "Evidence of Support";
    public static String POSITIVE_REFERENCE_HEADER = "Positive Reference";
    public static String NEGATIVE_REFERENCE_HEADER = "Negative Reference";
    // there is a mistake here but the mistake in in the file Primay should be Primary
    public static String PRIMARY_REFERENCE_HEADER = "Primay Reference";
    public static String MOST_CITED_HEADER = "Most cited";
    public static String MOST_RECENT_HEADER = "Most Recent";
    public static String SUPPORTING_HEADER = "Supporting";

    // ****************************************************************************************

    public SfariDatabaseImporter( String[] args ) throws Exception {
        super( args );
        /* this importer cannot automatically download files it expects the files to already be there */
        checkForSfariFiles();
        processSfariScoreFile();
        processSfariGeneFile();
    }

    public static void main( String[] args ) throws Exception {
        @SuppressWarnings("unused")
        SfariDatabaseImporter importEvidence = new SfariDatabaseImporter( args );
    }

    private void processSfariScoreFile() throws Exception {

        @SuppressWarnings("resource")
        BufferedReader brGeneScore = new BufferedReader( new FileReader( geneScore ) );

        // read headers
        String headersScore = StringUtil.cvs2tsv( brGeneScore.readLine() );

        String[] headersTokens = headersScore.split( "\t" );

        ArrayList<String> headersSet = new ArrayList<String>();

        for ( String token : headersTokens ) {
            headersSet.add( token.trim() );
        }

        if ( !headersSet.contains( GENE_SYMBOL_SCORE_HEADER ) || !headersSet.contains( SCORE_HEADER )
                || !headersSet.contains( SCORE_DETAILS_HEADER ) || !headersSet.contains( DESCRIPTION_SCORE_HEADER ) ) {
            throw new Exception( "Some headers not find" );
        }

        Integer geneSymbolIndex = headersSet.indexOf( GENE_SYMBOL_SCORE_HEADER );
        Integer scoreIndex = headersSet.indexOf( SCORE_HEADER );
        Integer scoreDetailsIndex = headersSet.indexOf( SCORE_DETAILS_HEADER );
        Integer descriptionScoreIndex = headersSet.indexOf( DESCRIPTION_SCORE_HEADER );

        String line = "";
        int lineNumer = 1;

        while ( ( line = brGeneScore.readLine() ) != null ) {

            line = StringUtil.cvs2tsv( line );

            String[] lineTokens = line.split( "\t" );

            log.info( "Reading Score file line: " + lineNumer++ );

            // getting out the info for 1 line
            String geneSymbol = lineTokens[geneSymbolIndex];
            String scoreDetails = lineTokens[scoreDetailsIndex];
            String score = lineTokens[scoreIndex];
            String description = lineTokens[descriptionScoreIndex];

            Double strength = 0D;

            if ( score.equalsIgnoreCase( "S" ) ) {
                score = scoreDetails;
            }

            if ( score.equalsIgnoreCase( "1S" ) || score.equalsIgnoreCase( "1" ) ) {
                strength = 1D;
            } else if ( score.equalsIgnoreCase( "2S" ) || score.equalsIgnoreCase( "2" ) ) {
                strength = 0.8D;
            } else if ( score.equalsIgnoreCase( "3S" ) || score.equalsIgnoreCase( "3" ) ) {
                strength = 0.6D;
            } else if ( score.equalsIgnoreCase( "4S" ) || score.equalsIgnoreCase( "4" ) ) {
                strength = 0.4D;
            } else if ( score.equalsIgnoreCase( "5S" ) || score.equalsIgnoreCase( "5" ) ) {
                strength = 0.2D;
            } else if ( score.equalsIgnoreCase( "6S" ) || score.equalsIgnoreCase( "6" ) ) {
                strength = 0D;
            } else if ( score.equalsIgnoreCase( "S" ) ) {
                strength = 0D;
            } else {
                throw new Exception( "Score: " + score );

            }

            ScoreValueObject s = new ScoreValueObject( strength, score, "SFARIGeneScore" );
            gene2Score.put( geneSymbol, s );
            gene2Description.put( geneSymbol, description );
        }

        brGeneScore.close();
    }

    @SuppressWarnings("resource")
    private void processSfariGeneFile() throws Exception {

        // the results
        outFinalResults = new BufferedWriter( new FileWriter( writeFolder + "/finalResults.tsv" ) );

        outFinalResults
                .write( "GeneSymbol\tGeneId\tPrimaryPubMed\tEvidenceCode\tComments\tIsNegative\tExternalDatabase\tDatabaseLink\tPhenotypes\tScoreType\tScore\tStrength\n" );

        BufferedReader brAutismGeneDataset = new BufferedReader( new FileReader( autismGeneDataset ) );

        String header = StringUtil.cvs2tsv( brAutismGeneDataset.readLine() );

        String[] headersTokens = header.split( "\t" );

        ArrayList<String> headersSet = new ArrayList<String>();

        for ( String token : headersTokens ) {
            headersSet.add( token );
        }

        if ( !headersSet.contains( GENE_SYMBOL_HEADER ) || !headersSet.contains( ENTREZ_GENE_ID_HEADER )
                || !headersSet.contains( SUPPORT_FOR_AUTISM_HEADER )
                || !headersSet.contains( EVIDENCE_OF_SUPPORT_HEADER )
                || !headersSet.contains( POSITIVE_REFERENCE_HEADER )
                || !headersSet.contains( NEGATIVE_REFERENCE_HEADER ) || !headersSet.contains( PRIMARY_REFERENCE_HEADER )
                || !headersSet.contains( MOST_CITED_HEADER ) || !headersSet.contains( MOST_RECENT_HEADER )
                || !headersSet.contains( SUPPORTING_HEADER ) ) {
            throw new Exception( "Some headers not find in the autism gene dataset" );
        }

        Integer geneSymbolIndex = headersSet.indexOf( GENE_SYMBOL_HEADER );
        Integer entrezGeneIDIndex = headersSet.indexOf( ENTREZ_GENE_ID_HEADER );
        Integer supportForAutismIndex = headersSet.indexOf( SUPPORT_FOR_AUTISM_HEADER );
        Integer evidenceOfSupportIndex = headersSet.indexOf( EVIDENCE_OF_SUPPORT_HEADER );
        Integer positiveReferenceIndex = headersSet.indexOf( POSITIVE_REFERENCE_HEADER );
        Integer negativeReferenceIndex = headersSet.indexOf( NEGATIVE_REFERENCE_HEADER );
        Integer primaryReferenceIndex = headersSet.indexOf( PRIMARY_REFERENCE_HEADER );
        @SuppressWarnings("unused")
        Integer mostCitedIndex = headersSet.indexOf( MOST_CITED_HEADER );
        @SuppressWarnings("unused")
        Integer mostRecentIndex = headersSet.indexOf( MOST_RECENT_HEADER );
        Integer supportingIndex = headersSet.indexOf( SUPPORTING_HEADER );

        String line = "";

        while ( ( line = brAutismGeneDataset.readLine() ) != null ) {

            line = StringUtil.cvs2tsv( line ) + "\t end";

            String[] lineTokens = line.split( "\t" );
            String geneSymbol = lineTokens[geneSymbolIndex];
            String nbciID = lineTokens[entrezGeneIDIndex];
            String description = lineTokens[supportForAutismIndex] + " ; " + lineTokens[evidenceOfSupportIndex];
            Set<String> literaturePubMed = new HashSet<String>();
            Set<String> literaturePubMedNegative = new HashSet<String>();

            addAllPubmed( lineTokens[positiveReferenceIndex], literaturePubMed );
            addAllPubmed( lineTokens[primaryReferenceIndex], literaturePubMed );
            // addAllPubmed( lineTokens[mostCitedIndex], this.literaturePubMed );
            // addAllPubmed( lineTokens[mostRecentIndex], this.literaturePubMed );
            addAllPubmed( lineTokens[supportingIndex], literaturePubMed );

            addAllPubmed( lineTokens[negativeReferenceIndex], literaturePubMedNegative );

            nbciID = treatSpecialCases( geneSymbol, nbciID );

            ScoreValueObject scoreVO = gene2Score.get( geneSymbol );

            String descriptionInScore = gene2Description.get( geneSymbol );

            if ( descriptionInScore == null ) {
                descriptionInScore = "";
            } else {
                descriptionInScore = " " + descriptionInScore;
            }

            for ( String pudmed : literaturePubMed ) {

                outFinalResults.write( geneSymbol + "\t" );
                outFinalResults.write( nbciID + "\t" );
                outFinalResults.write( pudmed + "\t" );

                outFinalResults.write( "TAS" + "\t" );
                outFinalResults.write( description + descriptionInScore + "\t" );
                outFinalResults.write( "" + "\t" );

                outFinalResults.write( "SFARI" + "\t" );
                outFinalResults.write( geneSymbol + "\t" );

                outFinalResults.write( "autism spectrum disorder" + "\t" );
                writeScore( scoreVO );
                outFinalResults.newLine();
            }

            for ( String pudmed : literaturePubMedNegative ) {

                outFinalResults.write( geneSymbol + "\t" );
                outFinalResults.write( nbciID + "\t" );
                outFinalResults.write( pudmed + "\t" );

                outFinalResults.write( "TAS" + "\t" );
                outFinalResults.write( description + descriptionInScore + "\t" );
                outFinalResults.write( "1" + "\t" );

                outFinalResults.write( "SFARI" + "\t" );
                outFinalResults.write( geneSymbol + "\t" );

                outFinalResults.write( "autism spectrum disorder" + "\t" );
                writeScore( scoreVO );
                outFinalResults.newLine();
            }

        }

        brAutismGeneDataset.close();
        outFinalResults.close();
    }

    private void checkForSfariFiles() throws Exception {

        writeFolder = WRITE_FOLDER + File.separator + SFARI;

        File folder = new File( writeFolder );

        if ( !folder.exists() ) {
            throw new Exception( "cannot find the SFARI Folder" + folder.getAbsolutePath() );
        }

        // first file expected
        autismGeneDataset = new File( writeFolder + File.separator + "autism-gene-dataset.csv" );
        if ( !autismGeneDataset.exists() ) {
            throw new Exception( "cannot find file: " + autismGeneDataset.getAbsolutePath() );
        }

        // second file expected
        geneScore = new File( writeFolder + File.separator + "gene-score.csv" );
        if ( !geneScore.exists() ) {
            throw new Exception( "cannot find file: " + autismGeneDataset.getAbsolutePath() );
        }
    }

    private void addAllPubmed( String linePubmedIds, Set<String> mySet ) throws Exception {

        String[] pubmedIds = linePubmedIds.split( "," );

        for ( int i = 0; i < pubmedIds.length; i++ ) {

            if ( !pubmedIds[i].trim().equalsIgnoreCase( "" ) ) {

                Integer pubM = new Integer( pubmedIds[i].trim() );

                if ( pubM < 1000 ) {
                    throw new Exception( "Why pubM < 1000" );
                }

                if ( mySet.contains( pubmedIds[i].trim() ) ) {
                    log.error( "SAME FOUND ***" + pubmedIds[i].trim() );
                    System.exit( -1 );
                }

                mySet.add( pubmedIds[i].trim() );
            }
        }
    }

    private String treatSpecialCases( String geneSymbol, String nbciID ) {

        if ( geneSymbol.equalsIgnoreCase( "ATP2B2" ) && nbciID.equalsIgnoreCase( "108733" ) ) {

            return "491";

        } else if ( geneSymbol.equalsIgnoreCase( "TNIP2" ) && nbciID.equalsIgnoreCase( "610669" ) ) {

            return "79155";
        }
        return nbciID;

    }

    private void writeScore( ScoreValueObject scoreVO ) throws IOException {

        if ( scoreVO != null ) {
            outFinalResults.write( scoreVO.getScoreName() + "\t" );
            outFinalResults.write( scoreVO.getScoreValue() + "\t" );
            outFinalResults.write( scoreVO.getStrength() + "\t" );
        }
    }
}