package uk.ac.starlink.table.formats;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.ConfigMethod;

/**
 * A StarTableWriter which outputs to Comma-Separated Value format.
 * This format is readable by {@link CsvTableBuilder}.
 *
 * @author   Mark Taylor (Starlink)
 * @since    21 Sep 2004
 */
public class CsvTableWriter extends DocumentedStreamStarTableWriter {

    private boolean writeHeader_ = true;
    private int maxFieldChars_ = Integer.MAX_VALUE;

    /**
     * Constructs a default CSV table writer.
     * This doesn't write a header.
     */
    public CsvTableWriter() {
        this( true );
    }

    /**
     * Constructs a CSV table writer which optionally writes headers.
     *
     * @param  writeHeader  true iff you want the first output line to contain
     *         column names
     */
    public CsvTableWriter( boolean writeHeader ) {
        super( new String[] { "csv" } );
        setWriteHeader( writeHeader );
    }

    /**
     * Indicate whether an initial row containing column names should be
     * written.
     *
     * @param  writeHeader  true iff you want the first output line to contain
     *         column names
     */
    @ConfigMethod(
        property = "header",
        doc = "<p>If true, the first line of the CSV output will be "
            + "a header containing the column names; "
            + "if false, no header line is written and all lines "
            + "represent data rows.</p>",
        sequence = 1
    )
    public void setWriteHeader( boolean writeHeader ) {
        writeHeader_ = writeHeader;
    }

    /**
     * Indicates whether an initial row containing column names will be
     * written.
     *
     * @return   whether the first output line will contain column names
     */
    public boolean getWriteHeader() {
        return writeHeader_;
    }

    /**
     * Sets a limit on the number of characters that will be written
     * in a single field.  Fields beyond this length will be truncated.
     *
     * @param  maxFieldChars  new limit
     */
    @ConfigMethod(
        property = "maxCell",
        doc = "<p>Maximum width in characters of an output table cell. "
            + "Cells longer than this will be truncated.</p>",
        example = "160",
        sequence = 2
    )
    public void setMaxFieldChars( int maxFieldChars ) {
        maxFieldChars_ = maxFieldChars;
    }

    /**
     * Returns the limit on the number of characters that will be written
     * in a single field.  Fields beyond this length will be truncated.
     *
     * @return  current limit
     */
    public int getMaxFieldChars() {
        return maxFieldChars_;
    }

    /**
     * Returns "CSV" or "CSV-noheader".
     */
    public String getFormatName() {
        return writeHeader_ ? "CSV" : "CSV-noheader";
    }

    public String getMimeType() {
        return "text/csv; header=\""
             + ( writeHeader_ ? "present" : "absent" )
             + "\"";
    }

    public boolean docIncludesExample() {
        return true;
    }

    public String getXmlDescription() {
        return readText( "CsvTableWriter.xml" );
    }

    public void writeStarTable( StarTable table, OutputStream ostrm )
            throws IOException {
        Writer out = new OutputStreamWriter( ostrm );
        int ncol = table.getColumnCount();
        ColumnInfo[] cinfos = Tables.getColumnInfos( table );
        RowSequence rseq = table.getRowSequence();
        try {

            /* Write the headings if required. */
            if ( getWriteHeader() ) {
                String[] headRow = new String[ ncol ];
                for ( int icol = 0; icol < ncol; icol++ ) {
                    headRow[ icol ] = cinfos[ icol ].getName();
                }
                writeRow( out, headRow );
            }

            /* Write the data. */
            String[] dataRow = new String[ ncol ];
            while ( rseq.next() ) {
                Object[] row = rseq.getRow();
                for ( int icol = 0; icol < ncol; icol++ ) {
                    dataRow[ icol ] = cinfos[ icol ]
                                     .formatValue( row[ icol ],
                                                   maxFieldChars_ );
                }
                writeRow( out, dataRow );
            }
        }
        finally {
            out.flush();
            rseq.close();
        }
    }

    /**
     * Writes an array of strings as one row of CSV output.
     *
     * @param  out  output stream
     * @param  row  array of strings, one for each cell in the row
     */
    private void writeRow( Writer out, String[] row ) throws IOException {
        int ncol = row.length;
        for ( int icol = 0; icol < ncol; icol++ ) {
            writeField( out, row[ icol ] );
            out.write( icol < ncol - 1 ? ',' : '\n' );
        }
    }

    /**
     * Writes a single field of CSV output.  Any special characters in 
     * the <tt>value</tt> are escaped as necessary.
     *
     * @param  out  output stream
     * @param  value  field to write
     */
    @SuppressWarnings("fallthrough")
    private void writeField( Writer out, String value ) throws IOException {

        /* Empty or null string, no output required. */
        if ( value == null || value.length() == 0 ) {
            return;
        }
        else {

            /* Find out if we need to quote this string. */
            int nchar = value.length();
            boolean quoted = false;
            switch ( value.charAt( 0 ) ) {
                case ' ':
                case '\t':
                    quoted = true;
            }
            switch ( value.charAt( nchar - 1 ) ) {
                case ' ':
                case '\t':
                    quoted = true;
            }
            for ( int i = 0; i < nchar && ! quoted; i++ ) {
                switch ( value.charAt( i ) ) {
                    case '\n':
                    case '\r':
                    case ',':
                    case '"':
                       quoted = true;
                }
            }

            /* If unquoted, it's very easy. */
            if ( ! quoted ) {
                out.write( value );
            }

            /* Otherwise we need to make a bit of effort. */
            else {
                out.write( '"' );
                for ( int i = 0; i < nchar; i++ ) {
                    char c = value.charAt( i );
                    switch ( c ) {
                        case '"':
                            out.write( '"' );
                        default:
                            out.write( c );
                    }
                }
                out.write( '"' );
            }
        }
    }

    /**
     * Returns a selection of useful CsvTableWriters.
     *
     * @return   array containing one writer with headers and one without
     */
    public static StarTableWriter[] getStarTableWriters() {
        return new StarTableWriter[] {
            new CsvTableWriter( true ),
            new CsvTableWriter( false ),
        };
    }
}
