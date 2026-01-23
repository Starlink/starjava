package uk.ac.starlink.table.formats;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
 * <li>Bytes in the file are interpreted as ASCII characters</li>
 * <li>Each table row is represented by a single line of text</li>
 * <li>Lines are terminated by one or more contiguous line termination
 *     characters: line feed (0x0A) or carriage return (0x0D)</li>
 * <li>Within a line, fields are separated by one or more whitespace
 *     characters: space (" ") or tab (0x09)</li>
 * <li>A field is either an unquoted sequence of non-whitespace characters,
 *     or a sequence of non-newline characters between matching
 *     single (') or double (") quote characters -
 *     spaces are therefore allowed in quoted fields</li>
 * <li>Within a quoted field, whitespace characters are permitted and are
 *     treated literally</li>
 * <li>Within a quoted field, any character preceded by a backslash character
 *     ("\") is treated literally.  This allows quote characters to appear
 *     within a quoted string.</li>
 * <li>An empty quoted string (two adjacent quotes) or the string 
 *     "<code>null</code>" (unquoted) represents the null value</li>
 * <li>All data lines must contain the same number of fields (this is the
 *     number of columns in the table)</li>
 * <li>The data type of a column is guessed according to the fields that
 *     appear in the table.  If all the fields in one column can be parsed
 *     as integers (or null values), then that column will turn into an
 *     integer-type column.  The types that are tried, in order of
 *     preference, are: 
 *        <code>Boolean</code>,
 *        <code>Short</code>
 *        <code>Integer</code>,
 *        <code>Long</code>,  
 *        <code>Float</code>,
 *        <code>Double</code>,
 *        <code>String</code>
 *     </li>
 * <li>Empty lines are ignored</li>
 * <li>Anything after a hash character "#" (except one in a quoted string)
 *     on a line is ignored as far as table data goes;
 *     any line which starts with a "!" is also ignored.
 *     However, lines which start with a "#" or "!" at the start of the table 
 *     (before any data lines) will be interpreted as metadata as follows:
 *     <ul>
 *     <li>The last "#"/"!"-starting line before the first data line may 
 *         contain
 *         the column names.  If it has the same number of fields as
 *         there are columns in the table, each field will be taken to be
 *         the title of the corresponding column.  Otherwise, it will be
 *         taken as a normal comment line.</li>
 *     <li>Any comment lines before the first data line not covered by the
 *         above will be concatenated to form the "description" parameter
 *         of the table.</li>
 *     </ul>    
 *     </li>    
 * </ul>
 *
 * @author   Mark Taylor (Starlink)
 */
public class AsciiStarTable extends StreamStarTable {

    private final int maxSample_;
    private final RowEvaluator.Decoder<?>[] decoders_;
    private List<String> comments_;
    private boolean dataStarted_;

    /**
     * Constructor with default options.
     *
     * @param  datsrc  the data source containing the table text
     * @throws TableFormatException  if the input stream doesn't appear to
     *         form a ASCII-format table
     * @throws IOException if some I/O error occurs
     */
    public AsciiStarTable( DataSource datsrc )
            throws TableFormatException, IOException {
        this( datsrc, StandardCharsets.UTF_8, 0,
              RowEvaluator.getStandardDecoders() );
    }

    /**
     * Constructor with configuration option.
     *
     * @param  datsrc  the data source containing the table text
     * @param  encoding   character encoding
     * @param  maxSample  maximum number of rows sampled to determine
     *                    column data types; if &lt;=0, all rows are sampled
     * @param  decoders   permitted data type decoders
     * @throws TableFormatException  if the input stream doesn't appear to
     *         form a ASCII-format table
     * @throws IOException if some I/O error occurs
     */
    @SuppressWarnings("this-escape")
    public AsciiStarTable( DataSource datsrc, Charset encoding, int maxSample,
                           RowEvaluator.Decoder<?>[] decoders )
            throws TableFormatException, IOException {
        super( encoding );
        maxSample_ = maxSample;
        decoders_ = decoders;
        init( datsrc );
    }

    protected RowEvaluator.Metadata obtainMetadata()
            throws TableFormatException, IOException {

        /* Get an input stream. */
        BufferedReader in = getReader();

        /* Look at each row in it counting cells and assessing what sort of
         * data they look like. */
        RowEvaluator evaluator = new RowEvaluator( decoders_ );
        comments_ = new ArrayList<String>();
        long lrow = 0;
        try {
            for ( List<String> row;
                  ( ( row = readRow( in ) ) != null &&
                    ( maxSample_ <= 0 || lrow < maxSample_ ) ); ) {
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
        boolean isSampleLimited = maxSample_ > 0 && lrow >= maxSample_;

        /* Get and check the metadata. */
        RowEvaluator.Metadata meta = evaluator.getMetadata();
        if ( meta.nrow_ == 0 ) {
            throw new TableFormatException( "No rows" );
        }

        /* Try to make use of any comment lines we read. */
        interpretComments( meta.colInfos_ );
        comments_ = null;

        return new RowEvaluator.Metadata( meta.colInfos_, meta.decoders_,
                                          isSampleLimited ? -1 : meta.nrow_ );
    }

    /**
     * Tries to make sense of any comment lines which have been read.
     * It may make changes to the initial <code>colInfos</code> set with
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
            String hline = comments_.get( comments_.size() - 1 );
            List<String> headings = readHeadings( hline );

            /* If this line looks like a set of headings (there are the
             * right number of fields) modify the colinfos accordingly and
             * remove it from the set of comments. */
            if ( headings.size() == ncol ) {
                comments_.remove( comments_.size() - 1 );
                for ( int i = 0; i < ncol; i++ ) {
                    colInfos[ i ].setName( headings.get( i ) );
                }
                trimLines( comments_ );
            }
        }

        /* If there are any other comment lines, concatenate them and bung
         * them into a description parameter. */
        if ( comments_.size() > 0 ) {
            StringBuffer dbuf = new StringBuffer();
            for ( Iterator<String> it = comments_.iterator(); it.hasNext(); ) {
                dbuf.append( it.next() );
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
     *          <code>null</code> for end of stream
     */
    @SuppressWarnings("fallthrough")
    protected List<String> readRow( BufferedReader in ) throws IOException {
        List<String> cellList = new ArrayList<>();
        while ( cellList.size() == 0 ) {
            String line = in.readLine();
            if ( line == null ) {
                return null;
            }
            int leng = line.length();
            for ( int ic = 0; ic < leng; ic++ ) {
                char c = line.charAt( ic );
                switch ( c ) {
                    case '#':
                        if ( ! dataStarted_ ) {
                            comments_.add( line.substring( ic + 1 ).trim() );
                        }
                        ic = leng - 1;
                        break;
                    case ' ':
                    case '\t':
                        break;
                    case '"':
                    case '\'':
                        Field qfield = readString( line, ic );
                        cellList.add( qfield.txt_ );
                        ic += qfield.nchar_;
                        break;
                    case '!':
                        if ( ic == 0 ) {
                            if ( ! dataStarted_ ) {
                                comments_.add( line.substring( ic + 1 )
                                                   .trim() );
                            }
                            ic = leng - 1;
                            break;
                        }
                        // if not at start of line fall through to ...
                    default:
                        Field tfield = readToken( line, ic );
                        String tok = tfield.txt_;
                        cellList.add( "null".equals( tok ) ? "" : tok );
                        ic += tfield.nchar_;
                }
            }
        }
        assert cellList.size() > 0;
        dataStarted_ = true;
        return cellList;
    }

    /**
     * Reads a quoted string from a given position in a given line.
     * The string may be delimited by single or double quotes.
     * Any character following a backslash will be included literally.
     * It is an error for the line or stream to end inside the string.
     *
     * @param   line  the line to read from
     * @param   ioff  character at which to start reading
     * @return  the (undelimited) string
     * @throws  TableFormatException  if the line finishes inside the string
     * @throws  IOException  if some I/O error occurs
     */
    private Field readString( String line, int ioff )
            throws TableFormatException {
        StringBuffer buffer = new StringBuffer();
        char delimiter = line.charAt( ioff );
        int leng = line.length();
        for ( int ic = ioff + 1; ic < leng; ic++ ) {
            char c = line.charAt( ic );
            if ( c == delimiter ) {
                return new Field( buffer.toString(), ic + 1 - ioff );
            }
            else if ( c == '\\' && ic < leng - 1 ) {
                ic++;
                buffer.append( line.charAt( ic ) );
            }
            else {
                buffer.append( line.charAt( ic ) );
            }
        }
        throw new TableFormatException( "End of line within a string literal" );
    }

    /**
     * Reads a token from a given position in a given line.
     * All consecutive non-whitespace characters from the given point are
     * read and returned as a single string.
     *
     * @param   line  the line to read from
     * @param   ioff  character at which to begin reading
     * @return  the token that was read
     * @throws  IOException  if an I/O error occurs
     */
    private Field readToken( String line, int ioff ) {
        int leng = line.length();
        for ( int ic = ioff; ic < leng; ic++ ) {
            switch ( line.charAt( ic ) ) {
                case ' ':
                case '\t':
                case '\r':
                case '\n':
                    return new Field( line.substring( ioff, ic ), ic - ioff );
                default:
            }
        }
        return new Field( line.substring( ioff, leng ), leng - ioff );
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
    private List<String> readHeadings( String line )
            throws TableFormatException {
        List<String> headings = new ArrayList<>();
        int leng = line.length();
        for ( int ic = 0; ic < leng; ic++ ) {
            char c = line.charAt( ic );
            switch ( c ) {
                case ' ':
                case '\t':
                    break;
                case '"':
                case '\'':
                    Field qfield = readString( line, ic );
                    headings.add( qfield.txt_ );
                    ic += qfield.nchar_;
                    break;
                default:
                    Field tfield = readToken( line, ic );
                    headings.add( tfield.txt_ );
                    ic += tfield.nchar_;
            }
        }
        return headings;
    }

    /**
     * Trims blank strings from the top and bottom of a list of strings.
     *
     * @param  lines  a List of String objects to trim
     */
    private static void trimLines( List<String> lines ) {

        /* Strip any blank lines from the top. */
        for ( ListIterator<String> it = lines.listIterator( 0 );
              it.hasNext(); ) {
            String line = it.next();
            if ( line.trim().length() == 0 ) {
                it.remove();
            }
            else {
                break;
            }
        }

        /* Strip any blank lines from the bottom. */
        for ( ListIterator<String> it = lines.listIterator( lines.size() );
              it.hasPrevious(); ) {
            String line = it.previous();
            if ( line.trim().length() == 0 ) {
                it.remove();
            }
            else {
                break;
            }
        }
    }

    /**
     * Characterises a single item read from a row.
     */
    private static class Field {
        final String txt_;
        final int nchar_;

        /**
         * Constructor.
         *
         * @param   txt  field content
         * @param   nchar  number of characters read to acquire content
         */
        Field( String txt, int nchar ) {
            txt_ = txt;
            nchar_ = nchar;
        }
    }
}
