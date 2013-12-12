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
package ubic.gemma.loader.genome;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import ubic.gemma.analysis.sequence.SequenceWriter;
import ubic.gemma.loader.util.parser.BasicLineParser;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

/**
 * Loader to handle results generated by Jim Kent's Blat.
 * <p>
 * The PSL file format is described at {@link http://genome.ucsc.edu/FAQ/FAQformat#format2} and {@link http
 * ://genome.ucsc.edu/goldenPath/help/blatSpec.html}. Blank lines are skipped as are valid PSL headers.
 * <p>
 * Target sequences are assumed to be chromosomes. If a chromosome name (chr10 or chr10.fa) is detected, the name is
 * stripped to be a chromosome number only (e.g. 10). Otherwise, the value is used as is. If the query name starts with
 * "target:", this is removed.
 * <p>
 * Results can be filtered by setting the scoreThreshold parameter.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BlatResultParser extends BasicLineParser<BlatResult> {
    private static final int BLOCKCOUNT_FIELD = 17;

    private static final int BLOCKSIZES_FIELD = 18;
    private static final int MATCHES_FIELD = 0;
    private static final int MISMATCHES_FIELD = 1;
    private static final int NS_FIELD = 3;
    private static final int NUM_BLAT_FIELDS = 21;
    private static final int QEND_FIELD = 12;
    private static final int QGAPBASES_FIELD = 5; // bases in gaps
    private static final int QGAPCOUNT_FIELD = 4; // number of gaps
    private static final int QNAME_FIELD = 9;
    private static final int QSIZE_FIELD = 10;
    private static final int QSTART_FIELD = 11;
    private static final int QSTARTS_FIELD = 19;
    private static final int REPMATCHES_FIELD = 2;
    private static final int STRAND_FIELD = 8;
    private static final int TEND_FIELD = 16;
    private static final int TGAPBASES_FIELD = 7;
    private static final int TGAPCOUNT_FIELD = 6;
    private static final int TNAME_FIELD = 13;
    private static final int TSIZE_FIELD = 14;
    private static final int TSTART_FIELD = 15;
    private static final int TSTARTS_FIELD = 20;

    /**
     * @param queryName
     * @return
     */
    public static String cleanUpQueryName( String queryName ) {
        queryName = queryName.replace( "target:", "" );
        queryName = queryName.replaceFirst( ";$", "" );
        queryName = queryName.replaceAll( SequenceWriter.SPACE_REPLACEMENT, " " );
        return queryName;
    }

    private int numSkipped = 0;

    private Collection<BlatResult> results = new ArrayList<BlatResult>();
    private double scoreThreshold = 0.0;

    /**
     * Reference to the sequence database that was searched.
     */
    private ExternalDatabase searchedDatabase = null;

    /**
     * Reference to the taxon for that was searched.
     */
    private Taxon taxon;

    public int getNumSkipped() {
        return numSkipped;
    }

    @Override
    public Collection<BlatResult> getResults() {
        return results;
    }

    /**
     * @return the searchedDatabase
     */
    public ExternalDatabase getSearchedDatabase() {
        return this.searchedDatabase;
    }

    /**
     * @return the taxon
     */
    public Taxon getTaxon() {
        return this.taxon;
    }

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.BasicParser#parseOneLine(java.lang.String)
     */
    @Override
    public BlatResult parseOneLine( String line ) {

        if ( StringUtils.isBlank( line ) ) return null;

        try {
            // header format (starts of lines only are shown:
            // psLayout version 3
            //
            // match mis- rep. N's Q gap Q gap T gap T gap strand Q Q Q Q T T T T block blockSizes qStarts tStarts
            // (spaces)match match
            // ------------------
            // check if it is a header line.
            if ( line.startsWith( "psLayout" ) || line.startsWith( "match" ) || line.startsWith( "    " )
                    || line.startsWith( "-----------------------" ) ) {
                return null;
            }

            String[] f = line.split( "\t" );
            if ( f.length == 0 ) return null;
            if ( f.length != NUM_BLAT_FIELDS )
                throw new IllegalArgumentException( f.length + " fields in line, expected " + NUM_BLAT_FIELDS
                        + " (starts with " + line.substring( 0, Math.max( line.length(), 25 ) ) );

            BlatResult result = BlatResult.Factory.newInstance();
            result.setQuerySequence( BioSequence.Factory.newInstance() );
            Long queryLength = Long.parseLong( f[QSIZE_FIELD] );
            assert queryLength != null;
            result.getQuerySequence().setLength( queryLength );

            result.setMatches( Integer.parseInt( f[MATCHES_FIELD] ) );
            result.setMismatches( Integer.parseInt( f[MISMATCHES_FIELD] ) );
            result.setRepMatches( Integer.parseInt( f[REPMATCHES_FIELD] ) );
            result.setNs( Integer.parseInt( f[NS_FIELD] ) );
            result.setQueryGapCount( Integer.parseInt( f[QGAPCOUNT_FIELD] ) );
            result.setQueryGapBases( Integer.parseInt( f[QGAPBASES_FIELD] ) );
            result.setTargetGapBases( Integer.parseInt( f[TGAPBASES_FIELD] ) );
            result.setTargetGapCount( Integer.parseInt( f[TGAPCOUNT_FIELD] ) );
            result.setStrand( f[STRAND_FIELD] );

            result.setQueryStart( Integer.parseInt( f[QSTART_FIELD] ) );
            result.setQueryEnd( Integer.parseInt( f[QEND_FIELD] ) );
            result.setTargetStart( Long.parseLong( f[TSTART_FIELD] ) );
            result.setTargetEnd( Long.parseLong( f[TEND_FIELD] ) );
            result.setBlockCount( Integer.parseInt( f[BLOCKCOUNT_FIELD] ) );

            result.setBlockSizes( f[BLOCKSIZES_FIELD] );
            result.setQueryStarts( f[QSTARTS_FIELD] );
            result.setTargetStarts( f[TSTARTS_FIELD] );

            String queryName = f[QNAME_FIELD];
            queryName = cleanUpQueryName( queryName );
            assert StringUtils.isNotBlank( queryName );
            result.getQuerySequence().setName( queryName );

            String chrom = f[TNAME_FIELD];
            if ( chrom.startsWith( "chr" ) ) {
                chrom = chrom.substring( chrom.indexOf( "chr" ) + 3 );
                if ( chrom.endsWith( ".fa" ) ) {
                    chrom = chrom.substring( 0, chrom.indexOf( ".fa" ) );
                }
            }
            if ( scoreThreshold > 0.0 && result.score() < scoreThreshold ) {
                numSkipped++;
                return null;
            }
            result.setTargetChromosome( Chromosome.Factory.newInstance( chrom, null, BioSequence.Factory.newInstance(),
                    taxon ) );
            result.getTargetChromosome().getSequence().setName( chrom );
            result.getTargetChromosome().getSequence().setLength( Long.parseLong( f[TSIZE_FIELD] ) );
            result.getTargetChromosome().getSequence().setTaxon( taxon );

            if ( searchedDatabase != null ) {
                result.setSearchedDatabase( searchedDatabase );
            }

            result.setTargetAlignedRegion( this.makePhysicalLocation( result ) );

            return result;
        } catch ( NumberFormatException e ) {
            log.error( "Invalid number format", e );
            return null;
        } catch ( IllegalArgumentException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Define a threshold, below which results are ignored. By default all results are read in.
     * 
     * @param score
     */
    public void setScoreThreshold( double score ) {
        this.scoreThreshold = score;
    }

    /**
     * @param searchedDatabase the searchedDatabase to set
     */
    public void setSearchedDatabase( ExternalDatabase searchedDatabase ) {
        this.searchedDatabase = searchedDatabase;
    }

    /**
     * @param taxon the taxon to set
     */
    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

    @Override
    protected void addResult( BlatResult obj ) {
        results.add( obj );

    }

    private PhysicalLocation makePhysicalLocation( BlatResult blatResult ) {
        PhysicalLocation pl = PhysicalLocation.Factory.newInstance();
        pl.setChromosome( blatResult.getTargetChromosome() );
        pl.setNucleotide( blatResult.getTargetStart() );
        pl.setNucleotideLength( ( int ) ( blatResult.getTargetEnd() - pl.getNucleotide() ) );
        pl.setStrand( blatResult.getStrand() );
        return pl;
    }
}
