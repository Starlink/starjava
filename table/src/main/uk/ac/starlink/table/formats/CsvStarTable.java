package uk.ac.starlink.table.formats;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.util.DataSource;

/**
 * Comma-Separated Value table.
 * This reader tries to read data in the semi-standard CSV format.
 * The intention is that it understands the version of that dialect 
 * spoken by MS Excel, though the documentation on which it is based 
 * was not obtained directly from MicroSoft.
 *
 * <p>Here are the rules:
 * <ul>
 * <li>Each row must have the same number of comma-separated fields.
 * <li>Whitespace (space or tab) adjacent to a comma is ignored.
 * <li>Adjacent commas, or a comma at the start or end of a line
 *     (whitespace apart) indicates a null field.
 * <li>Lines are terminated by any sequence of carriage-return or newline
 *     characters ('\r' or '\n')
 *     (a corollary of this is that blank lines are ignored).
 * <li>Cells may be enclosed in double quotes; quoted values may contain 
 *     linebreaks (or any other character); a double quote character within
 *     a quoted value is represented by two adjacent double quotes.
 * <li>The first line <em>may</em> be a header line containing column names
 *     rather than a row of data.  Exactly the same syntactic rules are
 *     followed for such a row as for data rows.
 * </ul>
 *
 * @author   Mark Taylor (Starlink)
 * @since    21 Sep 2004
 */
public class CsvStarTable extends StreamStarTable {

    private boolean hasHeading_;

    public CsvStarTable( DataSource datsrc )
            throws TableFormatException, IOException {
        super( datsrc );
    }

    protected PushbackInputStream getInputStream() throws IOException {
        PushbackInputStream in = super.getInputStream();

        /* If the first row is known to be a non-data row, skip it. */
        if ( hasHeading_ ) {
            readRow( in );
        }
        return in;
    }

    protected Metadata obtainMetadata()
            throws TableFormatException, IOException {

        /* Get an input stream. */
        PushbackInputStream in = super.getInputStream();

        /* Read and store the first column.  It could be a special header
         * row, or it could be just data. */
        String[] row0 = (String[]) readRow( in ).toArray( new String[ 0 ] );

        /* Look at each subsequent row assessing what sort of data they
         * look like. */
        RowEvaluator evaluator = new RowEvaluator();
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
        ColumnInfo[] colinfos = meta.colInfos_;
        Decoder[] decoders = meta.decoders_;
        int ncol = meta.ncol_;
        long nrow = meta.nrow_;

        /* Now return to the first row.
         * Try to parse it as a data row.  If this fails, then interpret
         * it as a row of headings. */
        if ( row0.length == ncol ) {
            boolean isDataRow;
            try {
                for ( int icol = 0; icol < ncol; icol++ ) {
                    decoders[ icol ].decode( row0[ icol ] );
                }
                isDataRow = true;
            }
            catch ( Exception e ) {
                isDataRow = false;
            }

            /* If it's data note that we have one more row than we thought. */
            if ( isDataRow ) {
                nrow++;
            }

            /* If it's headings get column names from it. */
            else {
                assert ! isDataRow;
                hasHeading_ = true;
                for ( int icol = 0; icol < ncol; icol++ ) {
                    String h = row0[ icol ];
                    if ( h != null && h.trim().length() > 0 ) {
                        colinfos[ icol ].setName( h );
                    }
                }
            }
        }

        /* If the first row has the wrong number of elements just ignore it 
         * (some sort of comment?) */
        else {
            hasHeading_ = true;
        }

        /* Return the, possibly modified, metadata. */
        return new Metadata( colinfos, decoders, nrow );
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
        StringBuffer buffer = new StringBuffer();
        boolean discard = false;
        boolean endFile = false;
        while ( cellList.size() == 0 && ! endFile ) {
            for ( boolean endLine = false; ! endLine; ) {
                char c = (char) in.read();
                switch ( c ) {
                    case END:
                        endFile = true;
                    case '\r':
                    case '\n':
                        if ( discard ) {
                            discard = false;
                        }
                        else if ( cellList.size() > 0 ) {
                            cellList.add( buffer.toString().trim() );
                        }
                        buffer.setLength( 0 );
                        endLine = true;
                        break;
                    case ',':
                        if ( discard ) {
                            discard = false;
                        }
                        else {
                            cellList.add( buffer.toString().trim() );
                        }
                        buffer.setLength( 0 );
                        break;
                    case '"':
                        if ( buffer.toString().trim().length() > 0 ) {
                            throw new TableFormatException(
                                "Mixed quoted/unquoted cell '" + buffer + "'" );
                        }
                        cellList.add( readString( in ) );
                        discard = true;
                        break;
                    case ' ':
                    case '\t':
                        if ( ! discard ) {
                            buffer.append( c );
                        }
                        break;
                    default:
                        if ( discard ) {
                            throw new TableFormatException(
                                "Mixed quoted/unquoted cell '" + c + "'" );
                        }
                        else {
                            buffer.append( c );
                        }
                }
            }
        }
        return cellList.size() == 0 ? null : cellList;
    }

    /**
     * Reads a double-quoted string from a stream.  The string, may contain
     * line breaks (or any other character) but it is an error for the 
     * stream to finish within it.  A double quote may be represented by
     * an adjacent pair of double quotes.
     * 
     * @param   stream  the stream to read from
     * @return  the (undelimited) string
     * @throws  TableFormatException  if stream finishes inside the string
     * @throws  IOException  if some I/O error occurs
     */
    private String readString( PushbackInputStream in ) throws IOException {
        StringBuffer buffer = new StringBuffer();
        while ( true ) {
            char c = (char) in.read();
            switch ( c ) {
                case '"':
                    char c2 = (char) in.read();
                    if ( c2 == '"' ) {
                        buffer.append( '"' );
                    }
                    else {
                        if ( c2 != END ) {
                            in.unread( c2 );
                        }
                        return buffer.toString();
                    }
                    break;
                case END:
                    throw new TableFormatException(
                        "End of file within a string literal" );
                default:
                    buffer.append( c );
            }
        }
    }
}
