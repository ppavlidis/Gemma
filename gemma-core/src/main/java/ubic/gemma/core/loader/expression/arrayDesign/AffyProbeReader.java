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
package ubic.gemma.core.loader.expression.arrayDesign;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import ubic.gemma.core.loader.util.parser.BasicLineMapParser;
import ubic.gemma.core.loader.util.parser.LineParser;
import ubic.gemma.core.loader.util.parser.Parser;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.PolymerType;
import ubic.gemma.model.genome.biosequence.SequenceType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Reads Affymetrix Probe files, including exon arrays.
 * <p>
 * Expected format is tabbed, NOT FASTA. A one-line header starting with the word "Probe" is REQUIRED. In later versions
 * of the format the second field of the file (column) is omitted.
 * </p>
 * <p>
 * For 3' arrays, here is an example:
 * </p>
 * 
 * <pre>
 * 1494_f_at 1 325 359 1118 TCCCCATGAGTTTGGCCCGCAGAGT Antisense
 * </pre>
 * 
 * For exon arrays, we create the equivalent files from the GFF files provided by Affymetrix. The files are created
 * off-line using a PERL script.
 *
 * @author pavlidis
 */
public class AffyProbeReader extends BasicLineMapParser<CompositeSequence, Collection<Reporter>> {

    private final Map<CompositeSequence, Collection<Reporter>> reporterMap = new HashMap<>();
    private int sequenceField = 4;

    /**
     * Set the index (starting from zero) of the field where the sequence is found. This varies in the
     * Affymetrix-provided files.
     *
     * @param sequenceField sequence field
     */
    public void setSequenceField( int sequenceField ) {
        this.sequenceField = sequenceField;
    }

    @Override
    public boolean containsKey( CompositeSequence key ) {
        return reporterMap.containsKey( key );
    }

    @Override
    public Collection<Reporter> get( CompositeSequence key ) {
        return reporterMap.get( key );
    }

    @Override
    public Collection<CompositeSequence> getKeySet() {
        return reporterMap.keySet();
    }

    @Override
    public Collection<Collection<Reporter>> getResults() {
        return reporterMap.values(); // make sure we don't get a HashMap$values
    }

    @Override
    public void parse( InputStream is ) throws IOException {
        if ( is == null )
            throw new IllegalArgumentException( "InputStream was null" );
        try (BufferedReader br = new BufferedReader( new InputStreamReader( is ) )) {
            StopWatch timer = new StopWatch();
            timer.start();
            String line;
            int linesParsed = 0;
            while ( ( line = br.readLine() ) != null ) {

                if ( line.startsWith( BasicLineMapParser.COMMENT_MARK ) ) {
                    continue;
                }
                this.parseOneLine( line );

                if ( ++linesParsed % Parser.PARSE_ALERT_FREQUENCY == 0
                        && timer.getTime() > LineParser.PARSE_ALERT_TIME_FREQUENCY_MS ) {
                    String message = "Parsed " + linesParsed + " lines...  ";
                    log.info( message );
                    timer.reset();
                    timer.start();
                }
            }
            log.info( "Parsed " + linesParsed + " lines, " + reporterMap.size() + " reporters generated." );
        }
    }

    @Override
    public Collection<Reporter> parseOneLine( String line ) {

        if ( StringUtils.isEmpty( line ) ) {
            return null;
        }

        String[] sArray = line.split( "\t" );
        if ( sArray.length == 0 )
            throw new IllegalArgumentException( "Line format is not valid (not tab-delimited or no fields found)" );

        String probeSetId = sArray[0];
        if ( probeSetId.startsWith( "Probe" ) ) {
            return null;
        }

        if ( sArray.length < sequenceField + 1 ) {
            throw new IllegalArgumentException(
                    "Too few fields in line, expected at least " + ( sequenceField + 1 ) + " but got "
                            + sArray.length );
        }

        String sequence = sArray[sequenceField];

        if ( StringUtils.isBlank( sequence ) ) {
            log.warn( "No sequence" );
        }

        String xcoord;
        String ycoord;
        String startInSequence;
        String index = null;

        if ( sequenceField == 4 ) {
            xcoord = sArray[1];
            ycoord = sArray[2];
            startInSequence = sArray[3];
        } else {
            index = sArray[1];
            xcoord = sArray[2];
            ycoord = sArray[3];
            startInSequence = sArray[sequenceField - 1];

        }

        Reporter reporter = Reporter.Factory.newInstance();

        try {
            reporter.setRow( Integer.parseInt( xcoord ) );
            reporter.setCol( Integer.parseInt( ycoord ) );

        } catch ( NumberFormatException e ) {
            log.warn( "Invalid row: could not parse coordinates: " + xcoord + ", " + ycoord );
            return null;
        }

        try {
            reporter.setStartInBioChar( Long.parseLong( startInSequence ) );
        } catch ( NumberFormatException e ) {

            if ( startInSequence.equals( "---" ) ) {
                /*
                 * Controls have no start/end information. We really have to bail on these.
                 */
                log.debug( "Control sequence" );
            } else {
                log.warn( "Invalid row: could not parse start in sequence: " + startInSequence );
            }
            return null;
        }

        String reporterName = probeSetId + ( index == null ? "" : "#" + index ) + ":" + xcoord + ":" + ycoord;
        reporter.setName( reporterName );
        BioSequence immobChar = BioSequence.Factory.newInstance();
        immobChar.setSequence( sequence );
        immobChar.setIsApproximateLength( false );
        immobChar.setLength( ( long ) sequence.length() );
        immobChar.setType( SequenceType.AFFY_PROBE );
        immobChar.setPolymerType( PolymerType.DNA );

        reporter.setImmobilizedCharacteristic( immobChar );

        CompositeSequence probeSet = CompositeSequence.Factory.newInstance();
        probeSet.setName( probeSetId );

        if ( !reporterMap.containsKey( probeSet ) ) {
            reporterMap.put( probeSet, new HashSet<Reporter>() );
        }

        reporter.setCompositeSequence( probeSet );
        reporterMap.get( probeSet ).add( reporter );
        return reporterMap.get( probeSet );

    }

    @Override
    protected CompositeSequence getKey( Collection<Reporter> newItem ) {
        return newItem.iterator().next().getCompositeSequence();
    }

    @Override
    protected void put( CompositeSequence key, Collection<Reporter> value ) {
        reporterMap.put( key, value );
    }

}
