package uk.ac.starlink.table.formats;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.DataSource;


/**
 * StarTable implementation which reads tables in Tab-Separated Table format.
 * This is used by GAIA/SkyCat amongst other software.
 * Documentationof the format can be found in Starlink System Note 75
 * (<a href="http://www.starlink.ac.uk/star/docs/ssn75.htx/ssn75.html"
 *     >SSN/75</a>).
 *
 * @author   Mark Taylor
 * @since    1 Aug 2006
 */
class TstStarTable extends StreamStarTable {

    private int ncol_;

    private static final Pattern COMMENT_REGEX =
        Pattern.compile( "^\\s*#.*" );
    private static final Pattern BLANK_REGEX =
        Pattern.compile( "^\\s*$" );
    private static final Pattern RULER_REGEX =
        Pattern.compile( "^[\\t\\-]*-[\\t\\-]*$" );
    private static final Pattern PARAM_REGEX =
        Pattern.compile( "^(\\S+):\\s*(.*)" );
    private static final Pattern EOD_REGEX =
        Pattern.compile( "^\\s*\\[EOD\\]\\s*$" );

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.formats" );

    /** Key for parameter composed of freeform description lines. */
    public static final ValueInfo DESCRIPTION_INFO =
        new DefaultValueInfo( "Description", String.class, 
                              "Free text description of the table" );

    /**
     * Constructor.
     *
     * @param  datsrc  data source for table text
     */
    public TstStarTable( DataSource datsrc )
            throws IOException, TableFormatException {
        super( StandardCharsets.US_ASCII );
        init( datsrc );
    }

    protected RowEvaluator.Metadata obtainMetadata()
            throws TableFormatException, IOException {

        /* Get an input stream. */
        PushbackReader in = super.getReader();

        /* Read all the text before the data itself. */
        List<String> lineList = readHeaderLines( in );

        /* Acquire and validate the column names. */
        String ruler = lineList.remove( lineList.size() - 1 );
        String colsLine = lineList.remove( lineList.size() - 1 );
        assert RULER_REGEX.matcher( ruler ).matches();
        List<String> colNames = tabSplit( colsLine );

        /* SExtractor likes to add a trailing TAB to this line. */
        if ( colNames.get( colNames.size() - 1 ).length() == 0 ) {
            colNames.remove( colNames.size() - 1 );
        }
        ncol_ = colNames.size();
        if ( colNames.size() != ncol_ ) {
            throw new TableFormatException( "Ruler line and column name line "
                                          + "have different numbers of tabs" );
        }

        /* Get table title from first line. */
        String title = null;
        if ( ! lineList.isEmpty() ) {
            String line0 = lineList.get( 0 );
            if ( ! COMMENT_REGEX.matcher( line0 ).matches() &&
                 ! BLANK_REGEX.matcher( line0 ).matches() &&
                 ! PARAM_REGEX.matcher( line0 ).matches() ) {
                setName( lineList.remove( 0 ).trim() );
            }
        }

        /* Set default values for special column indices. */
        int idIndex = ncol_ > 0 ? 0 : -1;
        int raIndex = ncol_ > 1 ? 1 : -1;
        int decIndex = ncol_ > 2 ? 2 : -1;
        int xIndex = -1;
        int yIndex = -1;

        /* Read table parameters. */
        List<DescribedValue> paramList = new ArrayList<DescribedValue>();
        for ( Iterator<String> it = lineList.iterator(); it.hasNext(); ) {
            String line = it.next();
            Matcher pmatcher = PARAM_REGEX.matcher( line );
            if ( ! COMMENT_REGEX.matcher( line ).matches() &&
                 pmatcher.matches() ) {
                String pname = pmatcher.group( 1 );
                String pval = pmatcher.group( 2 );
                if ( "id_col".equalsIgnoreCase( pname ) ) {
                    idIndex = parseColumnIndex( pval, ncol_ );
                    it.remove();
                }
                else if ( "ra_col".equalsIgnoreCase( pname ) ) {
                    raIndex = parseColumnIndex( pval, ncol_ );
                    it.remove();
                }
                else if ( "dec_col".equalsIgnoreCase( pname ) ) {
                    decIndex = parseColumnIndex( pval, ncol_ );
                    it.remove();
                }
                else if ( "x_col".equalsIgnoreCase( pname ) ) {
                    xIndex = parseColumnIndex( pval, ncol_ );
                    it.remove();
                }
                else if ( "y_col".equalsIgnoreCase( pname ) ) {
                    yIndex = parseColumnIndex( pval, ncol_ );
                    it.remove();
                }
                else {
                    DescribedValue param = createDescribedValue( pname, pval );
                    if ( param != null ) {
                        paramList.add( param );
                    }
                    it.remove();
                }
            }
        }

        /* Get table description from all the other comment lines. */
        StringBuffer descBuf = new StringBuffer();
        for ( String line : lineList ) {
            if ( ! COMMENT_REGEX.matcher( line ).matches() &&
                 ! BLANK_REGEX.matcher( line ).matches() ) {
                if ( descBuf.length() != 0 ) {
                    descBuf.append( '\n' );
                }
                descBuf.append( line );
            }
        }
        String description = descBuf.toString();
        if ( description.trim().length() > 0 ) {
            paramList.add( new DescribedValue( DESCRIPTION_INFO,
                                               description ) );
        }
        setParameters( paramList );

        /* Now read through all the data rows to find out what kind of
         * values each column contains. */
        RowEvaluator evaluator = new RowEvaluator( ncol_ );
        for ( List<String> row; ( row = readRow( in ) ) != null; ) {
            evaluator.submitRow( row );
        }
        RowEvaluator.Metadata metadata = evaluator.getMetadata();

        /* Doctor the column infos according to the information we have
         * acquired. */
        ColumnInfo[] colInfos = metadata.colInfos_;
        for ( int icol = 0; icol < ncol_; icol++ ) {
            colInfos[ icol ].setName( colNames.get( icol ) );
        }
        if ( raIndex >= 0 ) {
            ColumnInfo info = colInfos[ raIndex ];
            info.setUCD( "pos.eq.ra" );
            if ( Number.class.isAssignableFrom( info.getContentClass() ) ) {
                info.setUnitString( "deg" );
            }
        }
        if ( decIndex >= 0 ) {
            ColumnInfo info = colInfos[ decIndex ];
            info.setUCD( "pos.eq.dec" );
            if ( Number.class.isAssignableFrom( info.getContentClass() ) ) {
                info.setUnitString( "deg" );
            }
        }
        if ( idIndex >= 0 ) {
            ColumnInfo info = colInfos[ idIndex ];
            colInfos[ idIndex ].setUCD( "meta.id" );
        }
        if ( xIndex >= 0 ) {
            colInfos[ xIndex ].setUCD( "pos.cartesian.x" );
        }
        if ( yIndex >= 0 ) {
            colInfos[ yIndex ].setUCD( "pos.cartesian.y" );
        }
        return metadata;
    }

    @Override
    protected PushbackReader getReader() throws IOException {

        /* Skip the header lines before returning the superclass implementation
         * stream. */
        PushbackReader in = super.getReader();
        readHeaderLines( in );
        return in;
    }

    @SuppressWarnings("fallthrough")
    protected List<String> readRow( PushbackReader in )
            throws TableFormatException, IOException {
        StringBuffer sbuf = new StringBuffer();
        String line = null;
        while( line == null ) {
            char c = (char) in.read();
            switch ( c ) {
                case END:
                    if ( sbuf.length() == 0 ) {
                        return null;
                    }
                    // fall through
                case '\r':
                case '\n':
                    if ( sbuf.length() > 0 ) {
                        line = sbuf.toString();
                    }
                    break;
                default:
                    sbuf.append( c );
            }
        }

        /* Check for End Of Data marker. */
        if ( EOD_REGEX.matcher( line ).matches() ) {
            return null;
        }

        /* Split the line into fields. */
        List<String> words = tabSplit( line );

        /* SExtractor likes to put a trailing tab at the end of each line. */
        if ( words.size() == ncol_ + 1 && words.get( ncol_ ).length() == 0 ) {
            words.remove( ncol_ );
        }

        /* Check the number of fields and return if OK. */
        if ( words.size() != ncol_ ) {
            throw new TableFormatException( "Wrong number of fields ("
                                          + words.size() + " != " + ncol_
                                          + ") for line: " + line );
        }
        return words;
    }

    /**
     * Reads all header the lines up to and including the last pre-data one
     * which consists only of ----'s and tabs.
     * An array of all the header lines is returned.
     *
     * @param   in  input stream
     * @return  list of strings containing header lines
     */
    private static List<String> readHeaderLines( Reader in ) 
            throws TableFormatException, IOException {
        List<String> lineList = new ArrayList<>();
        while ( lineList.size() < 10000 ) {
            String line = readHeaderLine( in );
            lineList.add( line );
            if ( RULER_REGEX.matcher( line ).matches() ) {
                return lineList;
            }
        }
        throw new TableFormatException( "Header looks too long for TST" );
    }

    /**
     * Reads a line of text from an input stream.
     *
     * @param  in  input stream
     * @return  line (excluding terminators)
     */
    @SuppressWarnings("fallthrough")
    private static String readHeaderLine( Reader in )
            throws TableFormatException, IOException {
        StringBuffer sbuf = new StringBuffer();
        while ( sbuf.length() < 1024 * 1024 ) {
            char c = (char) in.read();
            switch ( c ) {
                case END:
                    if ( sbuf.length() == 0 ) {
                        throw new TableFormatException( "No TST rows" );
                    }
                    // fall through
                case '\r':
                case '\n':
                    return sbuf.toString();
                default:
                    sbuf.append( c );
            }
        }
        throw new TableFormatException( "Too long for a line in a TST table" );
    }

    /**
     * Splits a line of text using tab characters as delimiters.
     * 
     * @param   line  line of text
     * @return  list of strings constituting the tab-separated tokens
     */
    private static List<String> tabSplit( String line ) {
        List<String> fields = new ArrayList<>();
        for ( int start = 0; start >= 0; ) {
            int end = line.indexOf( '\t', start );
            if ( end >= 0 ) {
                fields.add( line.substring( start, end ) );
                start = end + 1;
            }
            else {
                fields.add( line.substring( start ) );
                break;
            }
        }
        return fields;
    }

    /**
     * Returns the column index represented by a string.
     * If it looks wrong, a warning will be logged and -1 will be returned.
     *
     * @param   txt  column index representation
     * @param   ncol  number of columns in table
     * @return  column index or -1
     */
    private static int parseColumnIndex( String txt, int ncol ) {
        Integer index;
        try {
            int ix = Integer.parseInt( txt.trim() );
            index = ( ix >= -1 && ix < ncol ) ? Integer.valueOf( ix ) : null;
        }
        catch ( NumberFormatException e ) {
            index = null;
        }
        if ( index == null ) {
            logger_.warning( "Bad value \"" + txt + "\" for column index - "
                           + "using -1" );
            return -1;
        }
        else {
            return index.intValue();
        }
    }

    /**
     * Turns a name, value pair into a DescribedValue.  Makes a guess about
     * the data type of the value on the basis of what it looks like.
     *
     * @param  name  parameter name
     * @param  sval  parameter string value
     * @return  described value
     */
    private static DescribedValue createDescribedValue( String name,
                                                        String sval ) {
        RowEvaluator re1 = new RowEvaluator( 1 );
        try {
            re1.submitRow( Collections.singletonList( sval ) );
            RowEvaluator.Metadata meta1 = re1.getMetadata();
            DefaultValueInfo info =
                new DefaultValueInfo( meta1.colInfos_[ 0 ] );
            info.setName( name );
            Object value = sval == null || sval.trim().length() == 0 
                         ? null
                         : meta1.decoders_[ 0 ].decode( sval );
            return new DescribedValue( info, value );
        }
        catch ( TableFormatException e ) {   // unlikely
            logger_.warning( "Failed to parse parameter "
                           + name + ": " + sval );
            return null;
        }
    }
}
