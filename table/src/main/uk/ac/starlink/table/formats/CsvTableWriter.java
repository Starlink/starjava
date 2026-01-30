package uk.ac.starlink.table.formats;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
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
    private char delimiter_;
    private int maxFieldChars_ = Integer.MAX_VALUE;
    private Charset encoding_ = StandardCharsets.UTF_8;

    static final String SET_DELIMITER_DOC =
          "<p>Field delimiter character, by default a comma. "
        + "Permitted values are a single character like \"<code>|</code>\", "
        + "a hexadecimal character code like \"<code>0x7C</code>\", "
        + "or one of the names \"<code>comma</code>\", "
        + "\"<code>space</code>\" or \"<code>tab</code>\". "
        + "Some choices of delimiter, for instance whitespace characters, "
        + "might not work well or might behave in surprising ways."
        + "</p>";

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
    @SuppressWarnings("this-escape")
    public CsvTableWriter( boolean writeHeader ) {
        super( new String[] { "csv" } );
        setDelimiter( ',' );
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
     * Sets the delimiter character.
     * Non-comma delimiters are not guaranteed to work.
     *
     * @param  delimiter  delimiter character
     */
    @ConfigMethod(
        property = "delimiter",
        doc = SET_DELIMITER_DOC,
        example = "|",
        sequence = 2
    )
    public void setDelimiter( char delimiter ) {
        delimiter_ = delimiter;
    }

    /**
     * Returns the delimiter character.
     *
     * @return  delimiter
     */
    public char getDelimiter() {
        return delimiter_;
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
        sequence = 3
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
     * Sets the character encoding used for the output CSV content.
     * The default value is UTF-8.
     *
     * @param   encoding   character encoding
     */
    @ConfigMethod( 
        property = "encoding",
        usage = "ASCII|UTF-8|UTF-16|...", 
        example = "UTF-16",
        doc = "<p>Specifies the character encoding used in "
            + "the output CSV file.\n"
            + "</p>"
    )   
    public void setEncoding( Charset encoding ) {
        encoding_ = encoding;
    }

    /**
     * Returns the encoding used for CSV output.
     *
     * @return character encoding
     */
    public Charset getEncoding() {
        return encoding_;
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
        int ncol = table.getColumnCount();
        ColumnInfo[] cinfos = Tables.getColumnInfos( table );
        Writer out =
            new BufferedWriter( new OutputStreamWriter( ostrm, getEncoding() ));
        try {

            /* Write the headings if required. */
            if ( getWriteHeader() ) {
                String[] headRow = new String[ ncol ];
                for ( int icol = 0; icol < ncol; icol++ ) {
                    headRow[ icol ] = cinfos[ icol ].getName();
                }
                StringBuilder sbuf = new StringBuilder();
                appendRow( sbuf, headRow );
                out.write( sbuf.toString() );
            }

            /* Write the data. */
            try ( RowSequence rseq = table.getRowSequence() ) {
                String[] dataRow = new String[ ncol ];
                StringBuilder sbuf = new StringBuilder();
                while ( rseq.next() ) {
                    sbuf.setLength( 0 );
                    Object[] row = rseq.getRow();
                    for ( int icol = 0; icol < ncol; icol++ ) {
                        dataRow[ icol ] = cinfos[ icol ]
                                         .formatValue( row[ icol ],
                                                       maxFieldChars_ );
                    }
                    appendRow( sbuf, dataRow );
                    out.write( sbuf.toString() );
                }
            }
        }
        finally {
            out.flush();
        }
    }

    /**
     * Formats an array of strings as one row of CSV output,
     * including a line end, and appends it to a supplied StringBuilder.
     *
     * @param  sbuf  buffer
     * @param  row  array of strings, one for each cell in the row
     */
    private void appendRow( StringBuilder sbuf, String[] row ) {
        int ncol = row.length;
        for ( int icol = 0; icol < ncol; icol++ ) {
            appendField( sbuf, row[ icol ] );
            sbuf.append( icol < ncol - 1 ? delimiter_ : '\n' );
        }
    }

    /**
     * Formats a single field of CSV output and appends it to a supplied
     * StringBuilder.
     * Any special characters in the <code>value</code>
     * are escaped as necessary.
     *
     * @param  sbuf  buffer
     * @param  value  field to append
     */
    @SuppressWarnings("fallthrough")
    private void appendField( StringBuilder sbuf, String value ) {

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
                char c = value.charAt( i );
                if ( c == '\n' || c == '\r' || c == '"' || c == delimiter_ ) {
                   quoted = true;
                }
            }

            /* If unquoted, it's very easy. */
            if ( ! quoted ) {
                sbuf.append( value );
            }

            /* Otherwise we need to make a bit of effort. */
            else {
                sbuf.append( '"' );
                for ( int i = 0; i < nchar; i++ ) {
                    char c = value.charAt( i );
                    switch ( c ) {
                        case '"':
                            sbuf.append( '"' );
                        default:
                            sbuf.append( c );
                    }
                }
                sbuf.append( '"' );
            }
        }
    }
}
