package uk.ac.starlink.table.formats;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.TableFormatException;

/**
 * Reads and parses so-called tables in the so-called "Machine-Readable Table"
 * format.
 * This class converts an InputStream containing an MRT table into a
 * {@link uk.ac.starlink.table.RowSequence} containing the data values,
 * extracting and exposing all the available metadata.
 * If the rows from the RowSequence are not iterated over,
 * only the metadata part of the input stream is read.
 *
 * @author   Mark Taylor
 * @since    30 Apr 2021
 * @see  <a href="https://journals.aas.org/mrt-standards/"
 *               >https://journals.aas.org/mrt-standards/</a>
 */
class MrtReader implements RowSequence {

    private final InputStream in_;
    private final ErrorMode errorMode_;
    private final LineSequence lseq_;
    private final ColumnReader<?>[] colReaders_;
    private final Pattern lineRegex_;
    private final DescribedValue[] params_;
    private boolean dataStarted_;
    private String dataLine_;
    private long irow_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.formats" );
    private static final int MAX_METALINE = 1000;

    private static final Predicate<String> IS_DASHLINE =
        txt -> txt.matches( "-----+-----\\s*" );
    private static final Pattern ANY_REGEX =
        Pattern.compile( ".*" );
    private static final Pattern BLANK_REGEX =
        Pattern.compile( "[?]=([^ ]+) +(.*)" );
    private static final Pattern PARAM_REGEX =
        Pattern.compile( "(^[A-Za-z]+): +([^ ].*)" );
    private static final Pattern BB_REGEX =
        Pattern.compile( "[Bb]yte-by-byte [Dd]escription.*" );
    private static final Pattern BBF_REGEX =
        Pattern.compile( "[Bb]yte-by-byte [Dd]escription of file: +([^ ]+).*" );

    /**
     * Constructor.
     *
     * @param   in  input stream containing complete MRT file
     * @param  errorMode  defines how parsing errors are treated
     * @param  useFloat   true to attempt use of single-precision floating
     *                    point values where it looks like they should be
     *                    appropriate
     */
    public MrtReader( InputStream in, ErrorMode errorMode, boolean useFloat )
            throws IOException {
        in_ = in;
        errorMode_ = errorMode;

        /* MRT files are supposed to be 7-bit ASCII, not e.g. UTF-8. */
        lseq_ = new LineSequence( in );
        irow_ = -1;

        /* Try to advance to the format description section.
         * This doesn't feel very reliable, it's not documented robustly,
         * but there doesn't seem to be any other way to do it. */
        String[] preLines =
            linesUntil( lseq_, line -> BB_REGEX.matcher( line ).matches() );

        /* The lines we've found up till now may contain table parameters like
         * title and authors, try to extract them on a best-efforts basis. */
        Matcher fnameMatcher =
            BBF_REGEX.matcher( preLines[ preLines.length - 1 ] );
        Map<String,String> paramMap = new LinkedHashMap<>();
        if ( fnameMatcher.matches() ) {
            paramMap.put( "filename", fnameMatcher.group( 1 ) );
        }
        paramMap.putAll( readParams( preLines ) );
        params_ = paramMap.entrySet().stream()
                 .map( ent -> new DescribedValue(
                                  new DefaultValueInfo( ent.getKey(),
                                                        String.class, null ),
                                  ent.getValue() ) )
                 .collect( Collectors.toList() )
                 .toArray( new DescribedValue[ 0 ] );

        /* Then there's probably a rubric line delimited by
         * two sets of dashes.*/
        linesUntil( lseq_, IS_DASHLINE );
        linesUntil( lseq_, IS_DASHLINE );

        /* The next section, if we've counted correctly,
         * is where the action is.
         * Read this to understand how to make sense of the cell data. */
        String[] fmtLines = linesUntil( lseq_, IS_DASHLINE );
        List<ColumnReader<?>> rdrList = new ArrayList<>();
        int nl = fmtLines.length - 1;
        for ( int il = 0; il < nl; il++ ) {

            /* Read and make sense of the line which explains how to read
             * the next column in sequence. */
            ParsedFormatLine pfl = new ParsedFormatLine( fmtLines[ il ] );

            /* The final field of that line is unformatted "explanation" text;
             * that might overflow onto subsequent lines.
             * There's no way to know for sure whether that is going to
             * happen or has happened, but if we can't make sense of the
             * next line(s) as a new column definition, assume it is this
             * overflowed text and retain it to add to the explanation text .*/
            StringBuffer extraTxt = new StringBuffer();
            while ( il + 1 < nl &&
                    ! ParsedFormatLine.isFormatLine( fmtLines[ il + 1 ] ) &&
                    fmtLines[ il + 1 ].startsWith( "       " ) &&
                    fmtLines[ il + 1 ].trim().length() > 0 ) {
                extraTxt.append( ' ' )
                        .append( fmtLines[ il + 1 ].trim() );
                il++;
            }

            /* We now have enough information to make an object that knows
             * how to read the column in question. */
            rdrList.add( createColumnReader( pfl, extraTxt.toString(),
                                             useFloat ) );
        }
        colReaders_ = rdrList.toArray( new ColumnReader<?>[ 0 ] );
        lineRegex_ = createLinePattern( colReaders_, errorMode_ );
    }

    /**
     * Return per-table metadata extracted by this reader.
     *
     * @return   array giving zero or more table parameters
     */
    public DescribedValue[] getParameters() {
        return params_;
    }

    /**
     * Return the number of columns read by this reader.
     *
     * @return  column count
     */
    public int getColumnCount() {
        return colReaders_.length;
    }

    /**
     * Return the column metadata for a given column.
     *
     * @param   icol   column index
     * @return  column metadata
     */
    public ColumnInfo getColumnInfo( int icol ) {
        return colReaders_[ icol ].info_;
    }

    public boolean next() throws IOException {
        dataLine_ = lseq_.nextLine();

        /* After the column formatting information and before the data,
         * there may be (one or more??) dash-delimited "Notes" sections.
         * Use a heuristic to see if we are apparently in one,
         * and try to skip it if so. */
        while ( ! dataStarted_ &&
                dataLine_ != null && dataLine_.startsWith( "Note (" ) ) {
            linesUntil( lseq_, IS_DASHLINE );
            dataLine_ = lseq_.nextLine();
        }
        dataStarted_ = true;

        /* We should now have the actual row data in dataLine_. */ 
        irow_++;

        /* Maybe... check that the line looks like it's written with spaces
         * in the places where the formatting header hasn't defined
         * data values. */
        if ( dataLine_ != null && errorMode_.isReport() ) {
            if ( ! lineRegex_.matcher( dataLine_ ).matches() ) {
                errorMode_.report( "Input line does not match format: \""
                                 + dataLine_ + "\"" );
            }
        }
        return dataLine_ != null;
    }

    public Object getCell( int icol ) throws IOException {
        ColumnReader<?> crdr = colReaders_[ icol ];
        try {
            return crdr.readCell( dataLine_ );
        }

        /* An unchecked exception may result if the column header lied about
         * the values that will be found in the relevant part of the row text.
         * In that case, do something according to configuration with
         * the error. */
        catch ( RuntimeException e ) {
            if ( errorMode_.isReport() ) {
                errorMode_.report( new StringBuffer()
                                  .append( "Cell read failure at row " )
                                  .append( irow_ )
                                  .append( " for column " )
                                  .append( crdr.info_.getName() )
                                  .append( ": " )
                                  .append( '"' )
                                  .append( crdr.cellText( dataLine_ ) )
                                  .append( '"' )
                                  .toString() );
            }

            /* Having treated the error, return a blank value for this cell;
             * though according to the error mode, it's possible that
             * an exception will have been thrown so we don't get here. */
            return null;
        }
    }

    public Object[] getRow() throws IOException {
        int ncol = colReaders_.length;
        Object[] row = new Object[ ncol ];
        for ( int ic = 0; ic < ncol; ic++ ) {
            row[ ic ] = getCell( ic );
        }
        return row;
    }

    public void close() throws IOException {
        in_.close();
    }

    /**
     * Reads lines from this reader's input stream until one is found
     * matching the given predicate.  Since there are all sorts of
     * possibilities for this running out of control, if more than
     * {@link #MAX_METALINE} rows are read, it bails out with an exception.
     *
     * @param  lseq  line sequence
     * @param  lineTest   stop when this line is reached
     * @return   array of lines, no more than {@link #MAX_METALINE} long,
     *           from the current position up to and including
     *           a line matching <code>lineText</code>
     * @throws  TableFormatException  if no matching line can be found in range
     */
    private static String[] linesUntil( LineSequence lseq,
                                        Predicate<String> lineTest )
            throws IOException {
        List<String> lines = new ArrayList<>();
        int il = 0;
        while ( true ) {
            String line = lseq.nextLine();
            lines.add( line );
            if ( line == null ) {
                throw new TableFormatException( "End of file while scanning"
                                              + " MRT headers" );
            }
            else if ( ++il > MAX_METALINE ) {
                throw new TableFormatException( "Too many lines"
                                              + " in MRT headers" );
            }
            else if ( lineTest.test( line ) ) {
                return lines.toArray( new String[ 0 ] );
            }
        }
    }

    /**
     * Returns an object that knows how to extract values for a given column
     * from a table data text line, based on information from the
     * column header format definition.
     *
     * @param   fmt   parsed information from column format definition line
     * @param   extraExplanation  appended explanation text that overran
     *             onto the next line from the format definition
     * @param  useFloat   true to attempt use of single-precision floating
     *                    point values where it looks like they should be
     *                    appropriate
     */
    private static ColumnReader<?> createColumnReader( ParsedFormatLine fmt,
                                                       String extraExplanation,
                                                       boolean useFloat )
            throws TableFormatException {

        /* The most important information is what kind of data is found
         * in this column, given by the first character of the format word.
         * The rest is formatting precision information, which we don't
         * pay attention to, since the read procedure will be the same
         * in any case. */
        char fmtChar = fmt.fmtWord_.charAt( 0 );

        /* Use zero-based not 1-based indices. */
        int iStart0 = fmt.iStart_ - 1;
        int iEnd0 = fmt.iEnd_;
        int nchr = iEnd0 - iStart0;

        /* Make sense of the "explanation" string.  This may contain
         * information about how nulls are encoded, though the formatting
         * seems to be pretty ad hoc in practice.  Do what we can. */
        String explanTxt = fmt.explanation_ + extraExplanation;
        ParsedExplanation parsedExplan = parseExplanation( explanTxt );
        String blankTxt = parsedExplan.blankTxt_;

        /* Prepare a basic column metadata object.
         * Initialise it with a dummy content class; the actual class
         * will be filled in by the ColumnReader construction. */
        ColumnInfo info = new ColumnInfo( fmt.label_, Object.class,
                                          parsedExplan.description_ );

        /* No units seems to be represented as "---". */
        if ( fmt.unit_ != null && fmt.unit_.trim().length() > 0 &&
             ! allDash( fmt.unit_ ) ) {
            info.setUnitString( fmt.unit_ );
        }

        /* Return column reader instances based on format character.
         * Work out what type length to use based on the maximum number
         * of characters allocated to the text representation of values. */
        if ( 'A' == fmtChar ) {
            return new ColumnReader<String>( String.class, info, iStart0, iEnd0,
                                             blankTxt, txt -> txt );
        }
        else if ( 'I' == fmtChar ) {
            if ( nchr <= 4 ) {
                return new ColumnReader<Short>( Short.class, info,
                                                iStart0, iEnd0, blankTxt,
                                                Short::valueOf );
            }
            else if ( nchr <= 9 ) {
                return new ColumnReader<Integer>( Integer.class, info,
                                                  iStart0, iEnd0, blankTxt,
                                                  Integer::valueOf );
            }
            else {
                return new ColumnReader<Long>( Long.class, info,
                                               iStart0, iEnd0, blankTxt,
                                               Long::valueOf );
            }
        }
        else if ( 'E' == fmtChar || 'F' == fmtChar ) {
            int nsf = 'E' == fmtChar ? nchr - 2 : nchr;
            if ( useFloat && nsf <= 6 ) {
                return new ColumnReader<Float>( Float.class, info,
                                                iStart0, iEnd0, blankTxt,
                                                MrtReader::readFloat );
            }
            else {
                return new ColumnReader<Double>( Double.class, info,
                                                 iStart0, iEnd0, blankTxt,
                                                 Double::valueOf );
            }
        }
        else {
            throw new AssertionError( "Bad format char '" + fmtChar + "'??" );
        }
    }

    /**
     * Utility method to test whether a string is just a string of
     * "-" characters.  This seems to be used by MRT in various places to
     * indicate blank values.
     *
     * @param  txt   value to test
     * @return   true if it's just dashes
     */
    private static boolean allDash( CharSequence txt ) {
        int leng = txt.length();
        for ( int i = 0; i < leng; i++ ) {
            if ( txt.charAt( i ) != '-' ) {
                return false;
            }
        }
        return leng > 0;
    }

    /**
     * Attempts to read header text as name-&gt;value pairs.
     * The format seems to be "<code>Name: freeform value text</code>"
     * on a line, with possible additional value text with leading
     * whitespace on subsequent lines.
     *
     * @param  lines  input text
     * @return   map of name-&gt;value pairs
     */
    private static Map<String,String> readParams( String[] lines ) {
        Map<String,String> map = new LinkedHashMap<>();
        int nl = lines.length;
        for ( int il = 0; il < nl; il++ ) {
            Matcher matcher = PARAM_REGEX.matcher( lines[ il ] );
            if ( matcher.matches() ) {
                String key = matcher.group( 1 );
                StringBuffer vbuf =
                    new StringBuffer( matcher.group( 2 ).trim() );
                while ( il + 1 < nl &&
                        lines[ il + 1 ].startsWith( " " ) &&
                        lines[ il + 1 ].trim().length() > 0 ) {
                    vbuf.append( ' ' )
                        .append( lines[ il + 1 ].trim() );
                    il++;
                }
                map.put( key, vbuf.toString() );
            }
        }
        return map;
    }

    /**
     * Returns a minimal regular expression which data lines ought to match.
     * This just ensures that whitespace is in the right place,
     * i.e. values are not leaking into inter-field positions.
     * 
     * @param  crdrs   column readers
     * @param  errorMode   error mode
     * @return   regular expression that every data input line ought to match
     */
    private static Pattern createLinePattern( ColumnReader<?>[] crdrs,
                                              ErrorMode errorMode )
            throws IOException {
        StringBuffer sbuf = new StringBuffer();
        for ( ColumnReader<?> crdr : crdrs ) {
            if ( sbuf.length() > crdr.iStart0_ ) {
                errorMode.report( "Byte positions out of sequence" );
                return ANY_REGEX;
            }
            while ( sbuf.length() < crdr.iStart0_ ) {
                sbuf.append( ' ' );
            }
            if ( sbuf.length() > crdr.iEnd0_ ) {
                errorMode.report( "Byte positions out of sequence" );
                return ANY_REGEX;
            }
            while ( sbuf.length() < crdr.iEnd0_ ) {
                sbuf.append( '.' );
            }
        }
        sbuf.append( "\\s*" );
        return Pattern.compile( sbuf.toString() );
    }

    /**
     * Reads a 32-bit floating point value, with error if it's out of range.
     *
     * @param  txt  textual representation
     * @return  float value
     * @throws  RuntimeException  if absolute value is too large for 32-bit IEEE
     */
    private static float readFloat( String txt ) {
        float f = Float.valueOf( txt );
        if ( Float.isInfinite( f ) ) {
            double d = Double.valueOf( txt );
            if ( !Double.isInfinite( d ) ) {
                throw new RuntimeException( "Large value " + d
                                          + "can't be represented "
                                          + "in float column" );
            }
        }
        return f;
    }

    /**
     * Try to make sense of the mostly-freeform "explanation" field in
     * the column definitions.
     * There's all sorts of magic characters that the CDS documentation
     * say can go at the start and end of this text to mean stuff,
     * not observed very well by actual files as far as I can tell.
     * Don't attempt to make sense of everything, but try to pick up
     * null values if they are encoded in a comprehensible way
     * (e.g. "?=99.9" at the start of the string).
     *
     * @param  expTxt   explanation text
     * @return  parsed explanation
     */
    private static ParsedExplanation parseExplanation( String expTxt ) {
        Matcher matcher = BLANK_REGEX.matcher( expTxt );
        if ( matcher.matches() ) {
            String blankTxt = matcher.group( 1 );
            String descrip = matcher.group( 2 );
            return new ParsedExplanation( descrip,
                                          "\"\"".equals( blankTxt ) ? null
                                                                    : blankTxt);
        }
        else {
            return new ParsedExplanation( expTxt, null );
        }
    }

    /**
     * Aggregates description text and a null value representation.
     */
    private static class ParsedExplanation {
        final String description_;
        final String blankTxt_;

        /**
         * Constructor.
         *
         * @param  description   freeform description text
         * @param  blankTxt   character representation of null values
         *                    that might appear in cell data
         */
        ParsedExplanation( String description, String blankTxt ) {
            description_ = description;
            blankTxt_ = blankTxt;
        }
    }

    /**
     * Represents parsed content of a line indicating column format.
     */
    private static class ParsedFormatLine {

        final int iStart_;         // 1-based character start index
        final int iEnd_;           // 1-based character end index (inclusive)
        final String fmtWord_;     // value format descriptor Xn.n
        final String unit_;        // unit string
        final String label_;       // column label (name)
        final String explanation_; // freeform text giving description etc

        private static final Pattern FMT_REGEX =
            Pattern.compile( " *(?:([0-9]+)-)?"   // start (1-based)
                           + " *([0-9]+)"         // end   (1-based, inclusive)
                           + " +([AIFE][0-9.]+)"  // format
                           + " +([^ ]+)"          // units
                           + " +([^ ]+)"          // label
                           + " +(.*)" );          // explanation

        /**
         * Constructor.
         *
         * @param  line   input text line
         * @throws  TableFormatException  if it can't be parsed
         */
        ParsedFormatLine( String line ) throws TableFormatException {
            Matcher matcher = FMT_REGEX.matcher( line );
            if ( ! matcher.matches() ) {
                throw new TableFormatException( "Unparsable MRT format line \""
                                              + line + "\"" ); 
            }
            iEnd_ = Integer.parseInt( matcher.group( 2 ) );
            iStart_ = matcher.group( 1 ) == null
                  ? iEnd_
                  : Integer.parseInt( matcher.group( 1 ) );
            fmtWord_ = matcher.group( 3 );
            unit_ = matcher.group( 4 );
            label_ = matcher.group( 5 );
            explanation_ = matcher.group( 6 );
        }
 
        /**
         * Indicates whether a given line can be parsed.
         *
         * @param  line   input line
         * @return   true iff it can be fed to ParsedFormatLine without a
         *           TableFormatException
         */
        static boolean isFormatLine( String line ) {
            return FMT_REGEX.matcher( line ).matches();
        }
    }

    /**
     * Object that knows about a single column in an MRT file.
     */
    private static class ColumnReader<T> {
        final ColumnInfo info_;
        final int iStart0_;
        final int iEnd0_;
        final Function<String,T> readCell_;
        final Predicate<String> isBlank_;

        /**
         * Constructor.
         *
         * @param  clazz   content class for data output
         * @param  info    template column metadata object;
         *                 this constructor will fill in the content class
         * @param  iStart0   index of first character in field in line text
         * @param  iEnd0     index of first character after field in line text
         * @param  blankTxt  representation of null value, or null
         * @param  readCell  maps a string representation of the field to
         *                   its typed value; this may throw an unchecked
         *                   exception if the content doesn't match expectation
         */
        ColumnReader( Class<T> clazz, ColumnInfo info, int iStart0, int iEnd0,
                      String blankTxt, Function<String,T> readCell ) {
            info.setContentClass( clazz );
            info_ = info;
            iStart0_ = iStart0;
            iEnd0_ = iEnd0;
            readCell_ = readCell;

            /* If there is a null representation, test for that. */
            if ( blankTxt != null ) {
                isBlank_ = txt -> txt.startsWith( blankTxt );
            }

            /* If the field is longer than one character, take a sequence
             * of "-" characters to mean null, which is common practice in
             * some MRT files. */
            else if ( iEnd0 - iStart0 > 1 ) {
                isBlank_ = txt -> allDash( txt );
            }

            /* But don't do that for a single-character field.
             * The reason is that in some MRT files a single character field
             * is assigned for the "+" or "-" sign qualifying a sexagesimal
             * latitude, e.g. with column name "DE-", and we don't want to
             * interpret those particular minus signs as nulls. */
            else {
                isBlank_ = txt -> false;
            }
        }

        /**
         * Reads the value of this column from a given input file data line.
         *
         * @param  line  input table data line
         * @throws  RuntimeException if the field content is not as expected
         */
        T readCell( String line ) {
            if ( line.length() >= iEnd0_ ) {
                int is = iStart0_;
                while ( is < iEnd0_ && line.charAt( is ) == ' ' ) {
                    is++;
                }
                int ie = iEnd0_;
                while ( ie > is && line.charAt( ie - 1 ) == ' ' ) {
                    ie--;
                }
                if ( ie > is ) {
                    String cellTxt = line.subSequence( is, ie ).toString();
                    if ( isBlank_.test( cellTxt ) ) {
                        return null;
                    }
                    return readCell_.apply( cellTxt );
                }
                else {
                    assert is == ie;
                    return null;
                }
            }
            else {
                return null;
            }
        }

        /**
         * Returns the text representation of this column in a given line.
         *
         * @param  line  input table data line
         * @return   text field value
         */
        String cellText( String line ) {
            return line.subSequence( iStart0_, iEnd0_ ).toString();
        }
    }
}
