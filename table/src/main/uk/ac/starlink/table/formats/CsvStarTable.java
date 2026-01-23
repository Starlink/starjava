package uk.ac.starlink.table.formats;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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

    private final Boolean fixHasHeaderLine_;
    private final int maxSample_;
    private final RowEvaluator.Decoder<?>[] decoders_;
    private final char delimiter_;
    private boolean hasHeading_;

    /**
     * Constructor with default options.
     *
     * @param  datsrc   data source
     */
    public CsvStarTable( DataSource datsrc )
            throws TableFormatException, IOException {
        this( datsrc, StandardCharsets.UTF_8, null, 0, ',',
              RowEvaluator.getStandardDecoders() );
    }

    /**
     * Constructor with configuration option.
     *
     * @param  datsrc   data source
     * @param  encoding  character encoding
     * @param  fixHasHeaderLine  indicates whether initial line is known
     *                           to be column names: yes, no or auto-determine
     * @param  maxSample  maximum number of rows sampled to determine
     *                    column data types; if &lt;=0, all rows are sampled
     * @param  decoders   permitted data type decoders
     * @param  delimiter  field delimiter character
     */
    @SuppressWarnings("this-escape")
    public CsvStarTable( DataSource datsrc, Charset encoding,
                         Boolean fixHasHeaderLine,
                         int maxSample, char delimiter,
                         RowEvaluator.Decoder<?>[] decoders )
            throws TableFormatException, IOException {
        super( encoding );
        fixHasHeaderLine_ = fixHasHeaderLine;
        maxSample_ = maxSample;
        delimiter_ = delimiter;
        decoders_ = decoders;
        init( datsrc );
    }

    @Override
    protected BufferedReader getReader() throws IOException {
        BufferedReader in = super.getReader();

        /* If the first row is known to be a non-data row, skip it. */
        if ( hasHeading_ ) {
            readRow( in );
        }
        return in;
    }

    protected RowEvaluator.Metadata obtainMetadata()
            throws TableFormatException, IOException {

        /* Get an input stream. */
        BufferedReader in = super.getReader();

        /* Read and store the first column.  It could be a special header
         * row, or it could be just data. */
        long lrow = 0;
        String[] row0 = readRow( in ).toArray( new String[ 0 ] );
        lrow++;

        /* Look at each subsequent row assessing what sort of data they
         * look like. */
        RowEvaluator evaluator = new RowEvaluator( decoders_ );
        try {
            for ( List<String> row;
                  ( ( row = readRow( in ) ) != null &&
                    ( maxSample_ <= 0 || lrow < maxSample_ ) ); )  {
                evaluator.submitRow( row );
                lrow++;
            }
        }
        catch ( TableFormatException e ) {
            throw new TableFormatException( e.getMessage() + " at line "
                                          + ( lrow + 1 ), e );
        }
        finally {
            if ( in != null ) {
                in.close();
            }
        }
        boolean isSampleLimited = maxSample_ > 0 && lrow >= maxSample_;

        /* Get a first look at the metadata (may be adjusted later). */
        RowEvaluator.Metadata meta = evaluator.getMetadata();
        if ( meta.nrow_ == 0 ) {
            throw new TableFormatException( "No rows" );
        }
        RowEvaluator.Decoder<?>[] decoders = meta.decoders_;
        int ncol = meta.ncol_;

        /* Now return to the first row.  See if it's a data row. */
        if ( row0.length == ncol ) {
            boolean isDataRow;
            if ( fixHasHeaderLine_ == null ) {
                isDataRow = true;
                for ( int icol = 0; icol < ncol; icol++ ) {
                    String cell = row0[ icol ];
                    if ( cell != null && cell.length() > 0 ) {
                        isDataRow = isDataRow
                                 && decoders[ icol ].isValid( cell );
                    }
                }
            }
            else {
                isDataRow = ! fixHasHeaderLine_.booleanValue();
            }
            hasHeading_ = ! isDataRow;

            /* If it is a data row, present it to the row evaluator like
             * the other rows, and return the metadata thus constructed. */
            if ( isDataRow ) {
                evaluator.submitRow( Arrays.asList( row0 ) );
                RowEvaluator.Metadata meta1 = evaluator.getMetadata();
                return new RowEvaluator
                          .Metadata( meta1.colInfos_, meta1.decoders_, -1 );
            }

            /* If it's a headings row, get column names from it, and
             * construct and return a suitable metadata item. */
            else {
                assert ! isDataRow;
                ColumnInfo[] colinfos = meta.colInfos_;
                for ( int icol = 0; icol < ncol; icol++ ) {
                    String h = row0[ icol ];
                    if ( h != null && h.trim().length() > 0 ) {
                        colinfos[ icol ].setName( h );
                    }
                }
                return new RowEvaluator
                          .Metadata( colinfos, decoders,
                                     isSampleLimited ? -1 : meta.nrow_ );
            }
        }

        /* If the first row has the wrong number of elements just ignore it 
         * (some sort of comment?) and use the metadata we've got. */
        else {
            hasHeading_ = true;
            return new RowEvaluator
                      .Metadata( meta.colInfos_, meta.decoders_,
                                 isSampleLimited ? -1 : meta.nrow_ );
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
    protected List<String> readRow( BufferedReader in ) throws IOException {
        List<String> cellList = new ArrayList<>();
        int icField = 0;
        boolean afterQuote = false;
        while ( cellList.size() == 0 ) {
            String line = in.readLine();
            if ( line == null ) {
                return null;
            }
            else if ( line.length() == 0 ) {
                // empty lines ignored; I don't know if this is a good idea,
                // but it's long been documented behaviour
            }
            else {
                for ( int ic = 0; ic < line.length(); ic++ ) {
                    char c = line.charAt( ic );
                    if ( c == delimiter_ ) {
                        if ( afterQuote ) {
                            afterQuote = false;
                        }
                        else {
                            cellList.add( line.substring( icField, ic ) );
                        }
                        icField = ic + 1;
                    }
                    else if ( c == '"' ) {
                        if ( ic > icField &&
                             line.substring( icField, ic )
                                 .trim().length() > 0 ) {
                            String msg = "Mixed quoted/unquoted cell: "
                                       + line.substring( icField, ic + 1 );
                            throw new TableFormatException( msg );
                        }
                        ic++;
                        for ( StringBuffer qword = new StringBuffer();
                              qword != null; ) {
                            if ( ic < line.length() ) {
                                if ( line.charAt( ic ) == '"' ) {
                                    if ( ic + 1 < line.length() &&
                                         line.charAt( ic + 1 ) == '"' ) {
                                        qword.append( '"' );
                                        ic += 2;
                                    }
                                    else {
                                        cellList.add( qword.toString() );
                                        afterQuote = true;
                                        qword = null;
                                    }
                                }
                                else {
                                    qword.append( line.charAt( ic ) );
                                    ic++;
                                }
                            }
                            else {
                                qword.append( '\n' );
                                line = in.readLine();
                                if ( line == null ) {
                                    String msg = "EOF in quoted string";
                                    throw new TableFormatException( msg );
                                }
                                ic = 0;
                            }
                        }
                    }
                    else {
                        if ( afterQuote && c != ' ' && c != '\t' ) {
                            String msg = "Mixed quoted/unquoted cell: "
                                       + line.substring( icField, ic + 1 );
                            throw new TableFormatException( msg );
                        }
                    }
                }
                if ( ! afterQuote ) {
                    cellList.add( line.substring( icField, line.length() ) );
                }
            }
        }
        return cellList.size() == 0 ? null : cellList;
    }
}
