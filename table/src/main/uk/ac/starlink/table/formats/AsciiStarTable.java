package uk.ac.starlink.table.formats;

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
public class AsciiStarTable extends StreamStarTable {

    private List comments_;
    private boolean dataStarted_;

    /**
     * Constructs a new AsciiStarTable from a datasource.
     *
     * @param  datsrc  the data source containing the table text
     * @throws TableFormatException  if the input stream doesn't appear to
     *         form a ASCII-format table
     * @throws IOException if some I/O error occurs
     */
    public AsciiStarTable( DataSource datsrc )
            throws TableFormatException, IOException {
        super( datsrc );
    }

    protected Metadata obtainMetadata()
            throws TableFormatException, IOException {

        /* Get an input stream. */
        PushbackInputStream in = getInputStream();

        /* Look at each row in it counting cells and assessing what sort of
         * data they look like. */
        RowEvaluator evaluator = new RowEvaluator();
        comments_ = new ArrayList();
        long lrow = 0;
        try {
            for ( List row; ( row = readRow( in ) ) != null; ) {
                lrow++;
                evaluator.submitRow( row );
            }
        }
        catch ( TableFormatException e ) {
            throw new TableFormatException( e.getMessage() + " at row " + lrow,
                                            e );
        }
        finally {
            if ( in != null ) {
                in.close();
            }
        }

        /* Get and check the metadata. */
        Metadata meta = evaluator.getMetadata();
        if ( meta.nrow_ == 0 ) {
            throw new TableFormatException( "No rows" );
        }

        /* Try to make use of any comment lines we read. */
        interpretComments( meta.colInfos_ );
        comments_ = null;

        return meta;
    }

    /**
     * Tries to make sense of any comment lines which have been read.
     * It may make changes to the initial <tt>colInfos</tt> set with
     * which it is provided.
     *
     * @param  colInfos  column infos already worked out for this table
     */
    private void interpretComments( ColumnInfo[] colInfos ) throws IOException {
        trimLines( comments_ );
        int ncol = colInfos.length;

        /* Try to interpret the last remaining comment line as a set of
         * column headings. */
        if ( comments_.size() > 0 ) {
            String hline = (String) comments_.get( comments_.size() - 1 );
            List headings = readHeadings( new PushbackInputStream(
                              new ByteArrayInputStream( hline.getBytes() ) ) );

            /* If this line looks like a set of headings (there are the
             * right number of fields) modify the colinfos accordingly and
             * remove it from the set of comments. */
            if ( headings.size() == ncol ) {
                comments_.remove( comments_.size() - 1 );
                for ( int i = 0; i < ncol; i++ ) {
                    colInfos[ i ].setName( (String) headings.get( i ) );
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
    protected List readRow( PushbackInputStream in ) throws IOException {
        List cellList = new ArrayList();
        while ( cellList.size() == 0 ) {
            for ( boolean endLine = false; ! endLine; ) {
                int c = in.read();
                switch ( (char) c ) {
                    case '\r':
                    case '\n':
                    case END:
                        if ( cellList.size() == 0 ) {
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
                        cellList.add( readString( in ) );
                        break;
                    default:
                        in.unread( c );
                        cellList.add( readToken( in ) );
                }
            }
        }
        dataStarted_ = true;
        return cellList;
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
     * Reads a row of headings from a stream.  This is speculative; it
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
