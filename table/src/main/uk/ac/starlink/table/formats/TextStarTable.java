package uk.ac.starlink.table.formats;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RandomStarTable;
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
 * <p>
 * This reader reads the whole table into memory before trying to make
 * sense of it, since otherwise it would have to do two passes, one to
 * work out the type of each column and one to read the data.  This is
 * not suitable for a very large table, but you're not really expected
 * to have very large tables in plain text format.  For similar reasons,
 * it is not coded to provide maximum efficiency for very large tables.
 *
 * @author   Mark Taylor (Starlink)
 */
public class TextStarTable extends RandomStarTable {

    private static final char END = (char) -1;

    private StringBuffer buffer = new StringBuffer();
    private final List rows = new ArrayList();
    private final ColumnInfo[] colinfos;
    private List comments = new ArrayList();
    private List headings;
    private List parameters = new ArrayList();
    private int ncol;
    private int readCol;

    /**
     * Constructs a new TextStarTable from a datasource.
     *
     * @param  stream  the data stream containing the table text
     */
    public TextStarTable( InputStream stream ) throws IOException {

        /* Get a List of Lists of Strings for the table data. */
        readStringCells( stream );

        /* Turn it into a List of Lists of typed objects. */
        Class[] types = typifyCells();

        /* Turn it into a List of arrays of Objects. */
        for ( int i = 0; i < rows.size(); i++ ) {
            rows.set( i, ((List) rows.get( i )).toArray() );
        }

        /* Set up a default set of column info objects. */
        colinfos = new ColumnInfo[ ncol ];
        for ( int i = 0; i < ncol; i++ ) {
            colinfos[ i ] = 
                new ColumnInfo( "col" + ( i + 1 ), types[ i ], null );
        }

        /* Try to make sense of any comment lines. */
        interpretComments();
    }

    public int getColumnCount() {
        return ncol;
    }

    public long getRowCount() {
        return (long) rows.size();
    }

    public Object getCell( long irow, int icol ) {
        return ((Object[]) rows.get( checkedLongToInt( irow ) ))[ icol ];
    }

    public Object[] getRow( long irow ) {
        return (Object[]) rows.get( checkedLongToInt( irow ) );
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colinfos[ icol ];
    }

    public List getParameters() {
        return parameters;
    }

    /**
     * Takes a list of lists of Strings representing a table and replaces
     * the strings with some more type-specific object if possible.
     * By the end of the process, each column will contain only cells
     * of a single class.  These classes will be String, or one of the
     * primitive wrapper types.
     *
     * @param tabledata  a List in which each element represents a row;
     *                   these elements are themselves Lists, containing
     *                   the contents of each cell
     * @return  an array of classes giving the class which each column
     *          contains instances of (the n'th element of which all 
     *          members of the n'th column are instances of)
     */
    private Class[] typifyCells() {
        int ncol = ((List) rows.get( 0 )).size();
        int nrow = rows.size();
        Class[] classes = new Class[ ncol ];

        /* For each column in the table, go through each row and see what
         * is the most restrictive datatype that all rows are compatible 
         * with. */
        for ( int icol = 0; icol < ncol; icol++ ) {
            boolean maybeBoolean = true;
            boolean maybeInteger = true;
            boolean maybeFloat = true;
            boolean maybeDouble = true;
            boolean maybeLong = true;
            for ( Iterator it = rows.iterator(); it.hasNext(); ) {
                List row = (List) it.next();
                String value = (String) row.get( icol );
                if ( value == null || value.length() == 0 ) {
                    continue;
                }
                boolean done = false;
                if ( ! done && maybeBoolean ) {
                    if ( value.equalsIgnoreCase( "false" ) ||
                         value.equalsIgnoreCase( "true" ) ||
                         value.equalsIgnoreCase( "f" ) ||
                         value.equalsIgnoreCase( "t" ) ) {
                        done = true;
                    }
                    else {
                        maybeBoolean = false;
                    }
                }
                if ( ! done && maybeInteger ) {
                    try {
                        Integer.parseInt( value );
                        done = true;
                    }
                    catch ( NumberFormatException e ) {
                        maybeInteger = false;
                    }
                }
                if ( ! done && maybeFloat ) {
                    try {
                        Float.parseFloat( value );
                        done = true;
                    }
                    catch ( NumberFormatException e ) {
                        maybeFloat = false;
                    }
                }
                if ( ! done && maybeDouble ) {
                    try {
                        Double.parseDouble( value );
                        done = true;
                    }
                    catch ( NumberFormatException e ) {
                        maybeDouble = false;
                    }
                }
                if ( ! done && maybeLong ) {
                    try {
                        Long.parseLong( value );
                        done = true;
                    }
                    catch ( NumberFormatException e ) {
                        maybeLong = false;
                    }
                }
            }

            /* Set the type we will use, and an object which can convert from
             * a string to the type in question. */
            abstract class Converter {
                abstract Object convert( String value);
            }
            Converter conv;
            Class clazz;
            if ( maybeBoolean ) {
                clazz = Boolean.class;
                conv = new Converter() {
                    Object convert( String value ) {
                        char v1 = value.charAt( 0 );
                        return ( v1 == 't' || v1 == 'T' ) ? Boolean.TRUE 
                                                          : Boolean.FALSE;
                    }
                };
            }
            else if ( maybeInteger ) {
                clazz = Integer.class;
                conv = new Converter() {
                    Object convert( String value ) {
                        return new Integer( Integer.parseInt( value ) );
                    }
                };
            }
            else if ( maybeFloat ) {
                clazz = Float.class;
                conv = new Converter() {
                    Object convert( String value ) {
                        return new Float( Float.parseFloat( value ) );
                    }
                };
            }
            else if ( maybeDouble ) {
                clazz = Double.class;
                conv = new Converter() {
                    Object convert( String value ) {
                        return new Double( Double.parseDouble( value ) );
                    }
                };
            }
            else if ( maybeLong ) {
                clazz = Long.class;
                conv = new Converter() {
                    Object convert( String value ) {
                        return new Long( Long.parseLong( value ) );
                    }
                };
            }
            else {
                clazz = String.class;
                conv = new Converter() {
                    Object convert( String value ) {
                        return value;
                    }
                };
            }
            classes[ icol ] = clazz;

            /* Do the conversion for each row. */
            for ( Iterator it = rows.iterator(); it.hasNext(); ) {
                List row = (List) it.next();
                String value = (String) row.get( icol );
                if ( value == null || value.length() == 0 ) {
                    row.set( icol, null );
                }
                else {
                    row.set( icol, conv.convert( value ) );
                }
            }
        }

        /* Return the types. */
        return classes;
    }


    /**
     * Tries to make sense of any comment lines which have been read.
     */
    private void interpretComments() throws IOException {
        trimLines( comments );

        /* Try to interpret the last remaining comment line as a set of
         * column headings. */
        if ( comments.size() > 0 ) {
            String hline = (String) comments.get( comments.size() - 1 );
            List headings = readHeadings( new PushbackInputStream( 
                              new ByteArrayInputStream( hline.getBytes() ) ) );

            /* If this line looks like a set of headings (there are the
             * right number of fields) modify the colinfos accordingly and
             * remove it from the set of comments. */
            if ( headings.size() == ncol ) {
                comments.remove( comments.size() - 1 );
                for ( int i = 0; i < ncol; i++ ) {
                    colinfos[ i ].setName( (String) headings.get( i ) );
                }
                trimLines( comments );
            }
        }

        /* If there are any other comment lines, concatenate them and bung
         * them into a description parameter. */
        if ( comments.size() > 0 ) {
            StringBuffer dbuf = new StringBuffer();
            for ( Iterator it = comments.iterator(); it.hasNext(); ) {
                dbuf.append( (String) it.next() );
                if ( it.hasNext() ) {
                    dbuf.append( '\n' );
                }
            }
            ValueInfo descriptionInfo =
                new DefaultValueInfo( "Description", String.class,
                                      "Comments included in text file" );
            parameters.add( new DescribedValue( descriptionInfo, 
                                                dbuf.toString() ) );
        }
    }

    /**
     * Returns the cells of the table as a List of Lists.  Each elements
     * is a string, unparsed except for formatting considerations 
     * (including stripping any quote characters etc).
     * The returned list will be 'square', that is each element will be
     * a list with the same number of elements.
     *
     * @param  istrm  the stream providing the table text
     * @return  a List in which each element represents a row; these 
     *          elements are themselves Lists, containing the contents
     *          of each cell
     * @throws  TableFormatException  if the input stream cannot be
     *          turned into a table
     * @throws  IOException  if some I/O error occurs
     */
    private void readStringCells( InputStream istrm ) throws IOException {
        PushbackInputStream stream =
            new PushbackInputStream( new BufferedInputStream( istrm ) );
        for ( boolean done = false; ! done; ) {
            int c = stream.read();
            switch ( (char) c ) {
                case '\r':
                case '\n':
                    endRow();
                    break;
                case '#':
                    if ( rows.size() == 0 ) {
                        comments.add( eatLine( stream ) );
                    }
                    else  {
                        eatLine( stream );
                    }
                    endRow();
                    break;
                case ' ':
                case '\t':
                    break;
                case '"':
                case '\'':
                    stream.unread( c );
                    addCell( readString( stream ) );
                    break;
                case END:
                    done = true;
                    endRow();
                    break;
                default:
                    stream.unread( c );
                    addCell( readToken( stream ) );
            }
        }
        stream.close();
    }

    /**
     * Indicates that the end of a row has been reached in the input table.
     */
    private void endRow() throws TableFormatException {
        if ( readCol == 0 ) {
            return;
        }
        int nrow = rows.size();
        if ( nrow == 1 ) {
            ncol = ((List) rows.get( 0 )).size();
        }
        else if ( readCol != ncol ) {
            throw new TableFormatException( 
                "Column number mismatch in row " + ( nrow - 1 ) +
                " (" + readCol + " != " + ncol + ")" );
        }
        readCol = 0;
    }

    /**
     * Stores given string value as the content of the next cell to be
     * read in the table.
     *
     * @param  value  the value to store
     */
    private void addCell( String value ) {
        if ( readCol++ == 0 ) {
            rows.add( new ArrayList( ncol ) );
        }
        ((List) rows.get( rows.size() - 1 )).add( value );
    }

    /**
     * Reads and discards any characters up to the end of the line.
     * 
     * @param   stream  the stream to read
     */
    private String eatLine( InputStream stream ) throws IOException {
        buffer.setLength( 0 );
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
        buffer.setLength( 0 );
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
        buffer.setLength( 0 );
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
}
