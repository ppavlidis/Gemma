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
package ubic.gemma.core.externalDb;

import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.MappingSqlQuery;
import ubic.basecode.util.SQLUtils;
import ubic.gemma.core.loader.genome.BlatResultParser;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;

/**
 * @author pavlidis
 */
public class GoldenPathQuery extends GoldenPath {

    BlatResultParser parser = new BlatResultParser();
    private EstQuery estQuery;
    private MrnaQuery mrnaQuery;

    public GoldenPathQuery() {
        super();
    }

    public GoldenPathQuery( int port, String databaseName, String host, String user, String password )
            throws SQLException {
        super( port, databaseName, host, user, password );

    }

    public GoldenPathQuery( Taxon taxon ) {
        super( taxon );
    }

    /**
     * Locate the alignment for the given sequence, if it exists in the goldenpath database.
     * Implementation note: This queries the est and mrna tables only.
     *
     * @param accession The genbank accession of the sequence.
     * @return blat results
     */
    public Collection<BlatResult> findAlignments( String accession ) {
        Collection<BlatResult> results = estQuery.execute( accession );
        if ( results.size() > 0 ) {
            return results;
        }

        return mrnaQuery.execute( accession );
    }

    private BlatResult convertResult( ResultSet rs ) throws SQLException {
        BlatResult result = BlatResult.Factory.newInstance();

        result.setQuerySequence( BioSequence.Factory.newInstance() );
        Long queryLength = rs.getLong( "qSize" );
        assert queryLength != null;
        result.getQuerySequence().setLength( queryLength );

        result.setMatches( rs.getInt( "matches" ) );
        result.setMismatches( rs.getInt( "misMatches" ) );
        result.setRepMatches( rs.getInt( "repMatches" ) );
        result.setNs( rs.getInt( "nCount" ) );
        result.setQueryGapCount( rs.getInt( "qNumInsert" ) );
        result.setQueryGapBases( rs.getInt( "qBaseInsert" ) );
        result.setTargetGapCount( rs.getInt( "tNumInsert" ) );
        result.setTargetGapBases( rs.getInt( "tBaseInsert" ) );
        result.setStrand( rs.getString( "strand" ) );
        result.setQueryStart( rs.getInt( "qStart" ) );
        result.setQueryEnd( rs.getInt( "qEnd" ) );
        result.setTargetStart( rs.getLong( "tStart" ) );
        result.setTargetEnd( rs.getLong( "tEnd" ) );
        result.setBlockCount( rs.getInt( "blockCount" ) );

        result.setBlockSizes( SQLUtils.blobToString( rs.getBlob( "blockSizes" ) ) );
        result.setQueryStarts( SQLUtils.blobToString( rs.getBlob( "qStarts" ) ) );
        result.setTargetStarts( SQLUtils.blobToString( rs.getBlob( "tStarts" ) ) );

        String queryName = rs.getString( "qName" );
        queryName = BlatResultParser.cleanUpQueryName( queryName );
        result.getQuerySequence().setName( queryName );

        String chrom = rs.getString( "tName" );
        if ( chrom.startsWith( "chr" ) ) {
            chrom = chrom.substring( chrom.indexOf( "chr" ) + 3 );
            if ( chrom.endsWith( ".fa" ) ) {
                chrom = chrom.substring( 0, chrom.indexOf( ".fa" ) );
            }
        }

        result.setTargetChromosome(
                Chromosome.Factory.newInstance( chrom, null, BioSequence.Factory.newInstance(), getTaxon() ) );
        result.getTargetChromosome().getSequence().setName( chrom );
        result.getTargetChromosome().getSequence().setLength( rs.getLong( "tSize" ) );
        result.getTargetChromosome().getSequence().setTaxon( getTaxon() );
        result.setSearchedDatabase( getSearchedDatabase() );

        return result;
    }

    @Override
    protected void init() {
        super.init();
        estQuery = new EstQuery( this.jdbcTemplate.getDataSource() );
        mrnaQuery = new MrnaQuery( this.jdbcTemplate.getDataSource() );
    }

    private class EstQuery extends MappingSqlQuery<BlatResult> {

        public EstQuery( DataSource dataSource ) {
            super( dataSource, "SELECT * FROM all_est WHERE qName = ?" );
            super.declareParameter( new SqlParameter( "accession", Types.VARCHAR ) );
            compile();
        }

        @Override
        protected BlatResult mapRow( ResultSet rs, int rowNum ) throws SQLException {
            return convertResult( rs );

        }

    }

    private class MrnaQuery extends MappingSqlQuery<BlatResult> {

        public MrnaQuery( DataSource dataSource ) {
            super( dataSource, "SELECT * FROM all_mrna WHERE qName = ?" );
            super.declareParameter( new SqlParameter( "accession", Types.VARCHAR ) );
            compile();
        }

        @Override
        protected BlatResult mapRow( ResultSet rs, int rowNum ) throws SQLException {
            return convertResult( rs );
        }

    }

}
