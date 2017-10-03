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

package ubic.gemma.core.analysis.preprocess.batcheffects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author paul
 */
@SuppressWarnings("WeakerAccess") // Possibly accessed via CLI tools
public abstract class BaseScanDateExtractor implements ScanDateExtractor {

    protected static final String GENEPIX_DATETIME_HEADER_REGEXP = "\"?DateTime=.*";
    private static final String STANDARD_FORMAT_REGEX_2 = ".+?([0-9]{2}[\\/-][0-9]{2}[\\/-]\\s[0-9]\\s[0-9]{2}:[0-9]{2}:[0-9]{2}).+";
    private static final String STANDARD_FORMAT_REGEX = ".+?([0-9]{2}[\\/-][0-9]{2}[\\/-][0-9]{2}\\s[0-9]{2}:[0-9]{2}:[0-9]{2}).+";
    private static final String LONG_FORMAT_REGEX = "\\s*Date\\s*(.+)";
    private static final Log log = LogFactory.getLog( BaseScanDateExtractor.class );

    /**
     * This method should be generic for GenePix/GPR/ATR file formats. Has DateType at the top formatted with quotes:
     * "DateTime=2005/11/09 11:36:27". Example GSE15739
     * For more information see <a href='http://mdc.custhelp.com/app/answers/detail/a_id/18886'>here</a>.
     */
    protected Date extractGenePix( BufferedReader reader ) throws IOException, ParseException {
        String line;
        // GPR/ATF file. Read a few lines to find the datetime (the header tells us how long the header is, but
        // this is probably okay)
        Date d = null;
        while ( ( line = reader.readLine() ) != null ) {

            if ( line.matches( GENEPIX_DATETIME_HEADER_REGEXP ) ) {
                d = parseGenePixDateTime( line );
                break;
            }
        }

        if ( d == null ) {
            throw new IllegalStateException( "Failed to find the 'DateTime' line" );
        }
        reader.close();
        return d;
    }

    /**
     * @param line like "DateTime=2005/11/09 11:36:27" (with the quotes) possibly with trailing whitespace.
     */
    protected Date parseGenePixDateTime( String line ) {
        String dateString = line.trim().replaceAll( "\"", "" ).replaceFirst( "DateTime=", "" );
        try {

            DateFormat f = new SimpleDateFormat( "yyyy/MM/dd HH:mm:ss" ); // 2005/11/09 11:36:27, 2006/04/07 14:18:18
            return f.parse( dateString );
        } catch ( ParseException e ) {
            try {
                /*
                 * Another format we see in GPR files ... 2008:11:27 10:27:42
                 */
                DateFormat f = new SimpleDateFormat( "yyyy:MM:dd HH:mm:ss" ); // 2005/11/09 11:36:27, 2006/04/07
                // 14:18:18
                return f.parse( dateString );
            } catch ( ParseException e1 ) {
                throw new RuntimeException( e1 );
            }

        }
    }

    /**
     * ISO 8601 date time in WSTRING format based on Universal Time Clock UTC (UTC is also known as GMT, or Greenwich
     * Mean Time) E.g. "2005-11-23T13:45:53Z"
     */
    protected Date parseISO8601( String string ) {
        try {
            Calendar f;
            f = javax.xml.bind.DatatypeConverter.parseDateTime( string );
            return f.getTime();
        } catch ( Exception e ) {
            return null;
        }

    }

    /**
     * E.g. "Mon Jun 17 21:26:34 CST 2002", but line has to have Date at start (possibly white-space padded) Shows up in
     * Imagene files.
     */
    protected Date parseLongFormat( String string ) {
        // // FIXME time-zone dependent.
        try {
            DateFormat f = new SimpleDateFormat( "EEE MMM dd HH:mm:ss zzz yyyy" );

            Pattern regex = Pattern.compile( LONG_FORMAT_REGEX );

            Matcher matcher = regex.matcher( string );
            if ( matcher.matches() ) {
                String tok = matcher.group( 1 );
                return f.parse( tok );
            }

            return null;
        } catch ( ParseException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Parse a common format, "MM[/-]dd[/-]yy hh:mm:ss", found for example in the "DatHeader" line from a CEL file and
     * extract the date found there.
     */
    protected Date parseStandardFormat( String string ) {

        try {
            DateFormat f = new SimpleDateFormat( "MM/dd/yy HH:mm:ss" );

            Pattern regex = Pattern.compile( STANDARD_FORMAT_REGEX );

            Matcher matcher = regex.matcher( string );
            if ( matcher.matches() ) {
                String tok = matcher.group( 1 );
                return f.parse( tok );
            }

            /*
             * For some reason, it is common to get things like "08/26/ 3 12:30:45" - I infer that is supposed to be a
             * 03.
             */
            Pattern regex2 = Pattern.compile( STANDARD_FORMAT_REGEX_2 );
            matcher = regex2.matcher( string );
            if ( matcher.matches() ) {
                String tok = matcher.group( 1 );
                tok = tok.replaceFirst( "\\s", "0" );
                Date d = f.parse( tok );
                log.warn( "Year was partly missing from date line: " + string + ", inferred " + d );
                return d;
            }

            return null;
        } catch ( ParseException e ) {
            throw new RuntimeException( e );
        }

    }

}
