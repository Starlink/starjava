package uk.ac.starlink.table.formats;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ReaderRowSequence;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.DataSource;

/**
 * Simple ASCII-format table.  This reader attempts to make sensible
 * decisions about what is a table and what is not, but inevitably it
 * will not be able to read ASCII tables in any format.
 * <p>
 * Here are the rules:
 * <ul>
 * <li>Each table row is represented by a single text line
 * <li>Lines are terminated by one or more line termination characters
 *     ('\r' or '\n')
 * <li>Anything after a '#' character on a line is ignored
 *     (except for optional heading line)
 * <li>Blank lines are ignored
 * <li>Within a line, fields are separated by one or more whitespace
 *     characters (space or tab)
 * <li>A field is either a sequence of non-whitespace characters or a
 *     sequence of characters between two matching quote characters
 *     (single (') or double (") quotes) - spaces are therefore allowed
 *     in quoted fields.
 * <li>In a quoted field any character preceded by a backslash is
 *     interpreted literally, so it is possible to insert quotes into
 *     a quoted string
 * <li>An empty string (two adjacent quotes) represents a null element
 * <li>All lines must contain exactly the same number of fields
 * <li>An optional heading line may be included: if the last comment line
 *     (line starting with a '#' before the first data line) contains the
 *     same number of fields as the columns in the table, these fields
 *     are interpreted as the headings (names) of the columns
 * </ul>
 *
 * @author   Mark Taylor (Starlink)
 */
public class TextStarTable extends AbstractStarTable {

    private static final char END = (char) -1;

    private final DataSource datsrc_;
    private int ncol_;
    private long nrow_;
    private ColumnInfo[] colInfos_;
    private Decoder[] decoders_;
    private List comments_;
    private boolean[] maybeBoolean_;
    private boolean[] maybeInteger_;
    private boolean[] maybeFloat_;
    private boolean[] maybeDouble_;
    private boolean[] maybeLong_;
    private int[] stringLength_;
    private boolean dataStarted_;
    private List cellList_ = new ArrayList();

    /**
     * Constructs a new TextStarTable from a datasource.
     *
     * @param  stream  the data stream containing the table text
     */
    public TextStarTable( DataSource datsrc ) throws IOException {
        datsrc_ = datsrc;

        /* Read the stream once to find out all about the columns in the 
         * table.  If we don't have something we can turn into a table,
         * this will throw a TableFormatException  */
        readMetadata();

        /* Configure table characteristics from the data source. */
        setName( datsrc.getName() );
        setURL( datsrc.getURL() );
    }

    public int getColumnCount() {
        return ncol_;
    }

    public long getRowCount() {
        return nrow_;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colInfos_[ icol ];
    }

    public RowSequence getRowSequence() throws IOException {
        final PushbackInputStream in = getInputStream();
        return new ReaderRowSequence() {
            protected Object[] readRow() throws IOException {
                List cellList = TextStarTable.this.readRow( in );
                if ( cellList == null ) {
                    in.close();
                    return null;
                }
                else {
                    Object[] row = new Object[ ncol_ ];
                    for ( int icol = 0; icol < ncol_; icol++ ) {
                        String sval = (String) cellList.get( icol );
                        if ( sval != null ) {
                            row[ icol ] = decoders_[ icol ].decode( sval );
                        }
                    }
                    return row;
                }
            }
        };
    }

    private PushbackInputStream getInputStream() throws IOException {
        return new PushbackInputStream( 
                   new BufferedInputStream( datsrc_.getInputStream() ) );
    }

    private void readMetadata() throws IOException {

        /* Get an input stream. */
        PushbackInputStream in = getInputStream();

        /* Look at each row in it counting cells and assessing what sort of
         * data they look like. */
        comments_ = new ArrayList();
        try {
            long lrow = 0;
            for ( List row; ( row = readRow( in ) ) != null; ) {
                lrow++;
                int nc = row.size();
                if ( lrow == 1 ) {
                    initMetadata( nc );
                }
                if ( nc != ncol_ ) {
                    throw new TableFormatException(
                        "Wrong number of columns at row " + lrow + 
                        " (expecting " + ncol_ + ", found " + nc +  ")" );
                }
                evaluateRow( row );
            }
            if ( lrow == 0 ) {
                throw new TableFormatException( "No rows" );
            }
            nrow_ = lrow;
        }
        finally {
            if ( in != null ) {
                in.close();
            }
        }

        /* Turn the information we have into a list of column headers. */
        setupColumns();
        interpretComments();
        comments_ = null;
    }

    /**
     * Looking at the information we have collected on the way through 
     * the table, sets up the required state to be able to turn the
     * stream into a RowSequence.
     */
    private void setupColumns() {
        colInfos_ = new ColumnInfo[ ncol_ ];
        decoders_ = new Decoder[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            Class clazz;
            Decoder decoder;
            ColumnInfo colinfo;
            String name = "col" + ( icol + 1 );
            if ( maybeBoolean_[ icol ] ) {
                colinfo = new ColumnInfo( name, Boolean.class, null );
                decoder = new Decoder() {
                    Object decode( String value ) {
                        char v1 = value.charAt( 0 );
                        return ( v1 == 't' || v1 == 'T' ) ? Boolean.TRUE
                                                          : Boolean.FALSE;
                    }
                };
            }
            else if ( maybeInteger_[ icol ] ) {
                colinfo = new ColumnInfo( name, Integer.class, null );
                decoder = new Decoder() {
                    Object decode( String value ) {
                        return new Integer( Integer.parseInt( value ) );
                    }
                };
            }
            else if ( maybeFloat_[ icol ] ) {
                colinfo = new ColumnInfo( name, Float.class, null );
                decoder = new Decoder() {
                    Object decode( String value ) {
                        return new Float( Float.parseFloat( value ) );
                    }
                };
            }
            else if ( maybeDouble_[ icol ] ) {
                colinfo = new ColumnInfo( name, Double.class, null );
                decoder = new Decoder() {
                    Object decode( String value ) {
                        return new Double( Double.parseDouble( value ) );
                    }
                };
            }
            else if ( maybeLong_[ icol ] ) {
                colinfo = new ColumnInfo( name, Long.class, null );
                decoder = new Decoder() {
                    Object decode( String value ) {
                        return new Long( Long.parseLong( value ) );
                    }
                };
            }
            else {
                colinfo = new ColumnInfo( name, String.class, null );
                colinfo.setElementSize( stringLength_[ icol ] );
                clazz = String.class;
                decoder = new Decoder() {
                    Object decode( String value ) {
                        return value;
                    }
                };
            }
            colInfos_[ icol ] = colinfo;
            decoders_[ icol ] = decoder;
        }
    }

    /**
     * Asserts that if the rows in this table represent a table, it's
     * one with a given number of columns.  Does any required setup.
     *
     * @param  ncol   number of columns we're looking for
     */
    private void initMetadata( int ncol ) {
        ncol_ = ncol;
        maybeBoolean_ = makeFlagArray( true );
        maybeInteger_ = makeFlagArray( true );
        maybeFloat_ = makeFlagArray( false );
        maybeDouble_ = makeFlagArray( true );
        maybeLong_ = makeFlagArray( true );
        stringLength_ = new int[ ncol ];
        dataStarted_ = true;
    }

    /**
     * Returns a new <tt>ncol</tt>-element boolean array.
     *
     * @param   val  initial value of all flags
     * @return  new flag array initialized to <tt>val</tt>
     */
    private boolean[] makeFlagArray( boolean val ) {
        boolean[] flags = new boolean[ ncol_ ];
        Arrays.fill( flags, val );
        return flags;
    }

    /**
     * Looks at a given row (list of strings) and records information about
     * what sort of things it looks like it contains.
     */
    private void evaluateRow( List row ) {
        assert row.size() == ncol_;
        for ( int icol = 0; icol < ncol_; icol++ ) {
            boolean done = false;
            String cell = (String) row.get( icol );
            int leng = cell.length();
            if ( cell == null || leng == 0 ) {
                done = true;
            }
            if ( leng > stringLength_[ icol ] ) {
                stringLength_[ icol ] = leng;
            }
            if ( ! done && maybeBoolean_[ icol ] ) {
                if ( cell.equalsIgnoreCase( "false" ) ||
                     cell.equalsIgnoreCase( "true" ) ||
                     cell.equalsIgnoreCase( "f" ) ||
                     cell.equalsIgnoreCase( "t" ) ) {
                    done = true;
                }
                else {
                    maybeBoolean_[ icol ] = false;
                }
            }
            if ( ! done && maybeInteger_[ icol ] ) {
                try {
                    Integer.parseInt( cell );
                    done = true;
                }
                catch ( NumberFormatException e ) {
                    maybeInteger_[ icol ] = false;
                }
            }
            if ( ! done && maybeFloat_[ icol ] ) {
                try {
                    Float.parseFloat( cell );
                    done = true;
                }
                catch ( NumberFormatException e ) {
                    maybeFloat_[ icol ] = false;
                }
            }
            if ( ! done && maybeDouble_[ icol ] ) {
                try {
                    Double.parseDouble( cell );
                    done = true;
                }
                catch ( NumberFormatException e ) {
                    maybeDouble_[ icol ] = false;
                }
            }
            if ( ! done && maybeLong_[ icol ] ) {
                try {
                    Long.parseLong( cell );
                    done = true;
                }
                catch ( NumberFormatException e ) {
                    maybeLong_[ icol ] = false;
                }
            }
        }
    }

    /**
     * Tries to make sense of any comment lines which have been read.
     */
    private void interpretComments() throws IOException {
        trimLines( comments_ );

        /* Try to interpret the last remaining comment line as a set of
         * column headings. */
        if ( comments_.size() > 0 ) {
            String hline = (String) comments_.get( comments_.size() - 1 );
            List headings = readHeadings( new PushbackInputStream(
                              new ByteArrayInputStream( hline.getBytes() ) ) );

            /* If this line looks like a set of headings (there are the
             * right number of fields) modify the colinfos accordingly and
             * remove it from the set of comments. */
            if ( headings.size() == ncol_ ) {
                comments_.remove( comments_.size() - 1 );
                for ( int i = 0; i < ncol_; i++ ) {
                    colInfos_[ i ].setName( (String) headings.get( i ) );
                }
                trimLines( comments_ );
            }
        }

        /* If there are any other comment lines, concatenate them and bung
         * them into a description parameter. */
        if ( comments_.size() > 0 ) {
            StringBuffer dbuf = new StringBuffer();
            for ( Iterator it = comments_.iterator(); it.hasNext(); ) {
                dbuf.append( (String) it.next() );
                if ( it.hasNext() ) {
                    dbuf.append( '\n' );
                }
            }
            ValueInfo descriptionInfo =
                new DefaultValueInfo( "Description", String.class,
                                      "Comments included in text file" );
            getParameters().add( new DescribedValue( descriptionInfo,
                                                     dbuf.toString() ) );
        }
    }

    /**
     * Reads the next row of data from a given stream.
     * Ignorable rows are skipped; comments may be stashed away.
     *
     * @param  in  input stream
     * @return  list of Strings one for each cell in the row, or 
     *          <tt>null</tt> for end of stream
     */
    private List readRow( PushbackInputStream in ) throws IOException {
        cellList_.clear();
        while ( cellList_.size() == 0 ) {
            for ( boolean endLine = false; ! endLine; ) {
                int c = in.read();
                switch ( (char) c ) {
                    case '\r':
                    case '\n':
                    case END:
                        if ( cellList_.size() == 0 ) {
                            return null;
                        }
                        endLine = true;
                        break;
                    case '#':
                        if ( ! dataStarted_ ) {
                            comments_.add( eatLine( in ) );
                        }
                        else {
                            eatLine( in );
                        }
                        endLine = true;
                        break;
                    case ' ':
                    case '\t':
                        break;
                    case '"':
                    case '\'':
                        in.unread( c );
                        cellList_.add( readString( in ) );
                        break;
                    default:
                        in.unread( c );
                        cellList_.add( readToken( in ) );
                }
            }
        }
        return cellList_;
    }

    /**
     * Reads and discards any characters up to the end of the line.
     *
     * @param   stream  the stream to read
     */
    private String eatLine( InputStream stream ) throws IOException {
        StringBuffer buffer = new StringBuffer();
        for ( boolean done = false; ! done; ) {
            int c = stream.read();
            switch ( (char) c ) {
                case '\n':
                case '\r':
                case END:
                    done = true;
                    break;
                default:
                    buffer.append( (char) c );
            }
        }
        return buffer.toString();
    }

    /**
     * Reads a quoted string from a given stream.  The string may be
     * delimited by single or double quotes.  Any character following a
     * backslash will be included literally.  It is an error for the
     * line or stream to end inside the string.
     *
     * @param   stream  the stream to read from
     * @return  the (undelimited) string
     * @throws  TableFormatException  if the line or stream finishes
     *          inside the string
     * @throws  IOException  if some I/O error occurs
     */
    private String readString( InputStream stream ) throws IOException {
        char delimiter = (char) stream.read();
        StringBuffer buffer = new StringBuffer();
        while ( true ) {
            int c = stream.read();
            if ( c == delimiter ) {
                break;
            }
            else {
                switch ( (char) c ) {
                    case '\r':
                    case '\n':
                        throw new TableFormatException(
                            "End of line within a string literal" );
                    case '\\':
                        buffer.append( (char) stream.read() );
                        break;
                    case END:
                        throw new TableFormatException(
                            "End of file within a string literal" );
                    default:
                        buffer.append( (char) c );
                }
            }
        }
        return buffer.toString();
    }

    /**
     * Reads a token from the given stream.
     * All consecutive non-whitespace characters from the given point are
     * read and returned as a single string.
     *
     * @param  stream  the stream to read from
     * @return  the token that was read
     * @throws  IOException  if an I/O error occurs
     */
    private String readToken( PushbackInputStream stream ) throws IOException {
        StringBuffer buffer = new StringBuffer();
        for ( boolean done = false; ! done; ) {
            int c = stream.read();
            switch ( (char) c ) {
                case '\n':
                case '\r':
                    stream.unread( c );
                    done = true;
                    break;
                case ' ':
                case '\t':
                case END:
                    done = true;
                    break;
                default:
                    buffer.append( (char) c );
            }
        }
        return buffer.toString();
    }

    /**
     * Reads a row of heaadings from a stream.  This is speculative; it
     * will interpret the remaining characters in a row as if it is a
     * set of text titles for following columns.  When the rest of the
     * table has been read, if the number of items in this array turns
     * out to match the number of columns, we will use these strings
     * as column headings.  Otherwise, we will throw them away.
     *
     * @param  stream  the input stream
     */
    private List readHeadings( PushbackInputStream stream ) throws IOException {
        List headings = new ArrayList();
        for ( boolean done = false; ! done; ) {
            int c = stream.read();
            switch ( (char) c ) {
                case '\r':
                case '\n':
                    done = true;
                    break;
                case ' ':
                case '\t':
                    break;
                case '"':
                case '\'':
                    stream.unread( c );
                    headings.add( readString( stream ) );
                    break;
                case END:
                    done = true;
                    break;
                default:
                    stream.unread( c );
                    headings.add( readToken( stream ) );
            }
        }
        return headings;
    }

    /**
     * Trims blank strings from the top and bottom of a list of strings.
     *
     * @param  lines  a List of String objects to trim
     */
    private static void trimLines( List lines ) {

        /* Strip any blank lines from the top. */
        for ( ListIterator it = lines.listIterator( 0 ); it.hasNext(); ) {
            String line = (String) it.next();
            if ( line.trim().length() == 0 ) {
                it.remove();
            }
            else {
                break;
            }
        }

        /* Strip any blank lines from the bottom. */
        for ( ListIterator it = lines.listIterator( lines.size() );
              it.hasPrevious(); ) {
            String line = (String) it.previous();
            if ( line.trim().length() == 0 ) {
                it.remove();
            }
            else {
                break;
            }
        }
    }

    /**
     * Interface for an object that can turn a string into a cell content
     * object.
     */
    private abstract class Decoder {
        abstract Object decode( String value );
    }
}
