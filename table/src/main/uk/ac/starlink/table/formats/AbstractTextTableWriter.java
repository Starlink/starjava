package uk.ac.starlink.table.formats;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.ConfigMethod;

/**
 * A <code>StarTableWriter</code> which outputs text to
 * a human-readable text file.
 * Table parameters (per-table metadata) can optionally be output 
 * as well as the table data themselves.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class AbstractTextTableWriter
        extends DocumentedStreamStarTableWriter {

    private boolean writeParams_;
    private int maxWidth_;
    private int maxParamLength_;
    private int sampledRows_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.formats" );

    /**
     * Constructor.
     *
     * @param  extensions  list of lower-cased filename extensions,
     *                     excluding the '.' character
     * @param  writeParams  whether parameters will be written by default
     */
    @SuppressWarnings("this-escape")
    protected AbstractTextTableWriter( String[] extensions,
                                       boolean writeParams ) {
        super( extensions );
        setWriteParameters( writeParams );
        setMaxWidth( 160 );
        setMaximumParameterLength( 160 );
    }

    /**
     * Returns "text";
     *
     * @return "text"
     */
    public String getFormatName() {
        return "text";
    }

    public String getMimeType() {
        return "text/plain";
    }

    public void writeStarTable( StarTable startab, OutputStream strm )
            throws IOException {

        /* Get the column headers and prepare to work out column widths 
         * for formatting. */
        int ncol = startab.getColumnCount();
        ColumnInfo[] cinfos = new ColumnInfo[ ncol ];
        int[] cwidths = new int[ ncol ];
        int[] maxDataWidths = new int[ ncol ];
        for ( int i = 0; i < ncol; i++ ) {
            cinfos[ i ] = startab.getColumnInfo( i );
            cwidths[ i ] = getMinNameWidth( cinfos[ i ] );
            maxDataWidths[ i ] =
                getMaxDataWidth( cinfos[ i ].getContentClass() );
        }

        /* Make one pass through (a sample of) the rows
         * to determine field widths. */
        int maxSamples = getSampledRows();
        long irow = 0;
        logger_.config( "Reading"
                      + ( maxSamples <= 0 ? " all" :"" )
                      + " rows to determine formatted column widths" );
        boolean allRowsSampled;
        try ( RowSequence rseq = startab.getRowSequence() ) {
            while ( rseq.next() &&
                    ( maxSamples <= 0 || irow++ < maxSamples ) ) {
                Object[] row = rseq.getRow();
                for ( int ic = 0; ic < ncol; ic++ ) {
                    String formatted =
                        cinfos[ ic ]
                       .formatValue( row[ ic ], maxDataWidths[ ic ] );
                    if ( formatted.length() > cwidths[ ic ] ) {
                        cwidths[ ic ] = formatted.length();
                    }
                }
            }
            allRowsSampled = ! rseq.next();
        }

        /* Report on data sampling. */
        if ( maxSamples > 0 ) {
            if ( allRowsSampled ) {
                logger_.config( "All rows sampled to determine column widths" );
            }
            else {
                StringBuffer sbuf = new StringBuffer()
                    .append( "Subset of rows (" )
                    .append( irow );
                long nt = startab.getRowCount();
                if ( nt >= 0 ) {
                    sbuf.append( "/" )
                        .append( nt );
                }
                sbuf.append( ") sampled to determine column widths" );
                logger_.info( sbuf.toString() );
            }
        }

        /* Add a bit of safety padding if we're only going on a sample. */
        if ( ! allRowsSampled ) {
            for ( int icol = 0; icol < ncol; icol++ ) {
                cwidths[ icol ] += 2;
                ColumnInfo cinfo = cinfos[ icol ];
                if ( cinfo.getContentClass().equals( String.class ) ) {
                    int nchar = cinfo.getElementSize();
                    if ( nchar > 0 ) {
                        cwidths[ icol ] = Math.max( cwidths[ icol ],  nchar );
                    }
                }
            }
        }

        /* Apply sensible maximum field widths. */
        for ( int i = 0; i < ncol; i++ ) {
            cwidths[ i ] = Math.min( getMaxWidth(), cwidths[ i ] );
        }

        /* Print parameters. */
        if ( writeParams_ ) {
            int maxleng = getMaximumParameterLength();
            String name = startab.getName();
            if ( name != null && name.trim().length() > 0 ) {
                printParam( strm, "Table name", name, String.class );
            }
            for ( DescribedValue param : startab.getParameters() ) {
                ValueInfo info = param.getInfo();
                printParam( strm, info.getName(),
                            param.getValueAsString( maxleng ),
                            info.getContentClass() );
            }
        }

        /* Print headings. */
        printColumnHeads( strm, cwidths, cinfos );

        /* Print rows. */
        try ( RowSequence rseq = startab.getRowSequence() ) {
            while ( rseq.next() ) {
                Object[] row = rseq.getRow();
                String[] frow = new String[ ncol ];
                for ( int icol = 0; icol < ncol; icol++ ) {
                    frow[ icol ] = formatValue( row[ icol ], cinfos[ icol ],
                                                cwidths[ icol ] );
                }
                printLine( strm, cwidths, frow );
            }
        }

        /* Finish off. */
        printSeparator( strm, cwidths );
    }

    /**
     * Sets the maximum length for the value of a parameter that will be output.
     *
     * @param  maxParamLength  maximum printable parameter length
     */
    @ConfigMethod(
        property = "maxParam",
        doc = "<p>Maximum width in characters of an output table parameter. "
            + "Parameters with values longer than this will be truncated.</p>"
    )
    public void setMaximumParameterLength( int maxParamLength ) {
        maxParamLength_ = maxParamLength;
    }

    /**
     * Returns the maximum length for the value of a parameter as passed to
     * {@link #printParam}.  The default implementation currently returns 160.
     *
     * @return  maximum length for output string parameters
     */
    public int getMaximumParameterLength() {
        return maxParamLength_;
    }

    /**
     * Set whether the output should include table parameters.
     * If so they are written as name:value pairs one per line 
     * before the start of the table proper.
     *
     * @param writeParams  true iff you want table parameters to be output as
     *        well as the table data
     */
    @ConfigMethod(
        property = "params",
        doc = "<p>Whether to output table parameters as well as row data.</p>"
    )
    public void setWriteParameters( boolean writeParams ) {
        writeParams_ = writeParams;
    }

    /**
     * Finds out whether the output will include table parameters.
     *
     * @return  true iff the table parameters will be output as well as the
     *          table data
     */
    public boolean getWriteParameters() {
        return writeParams_;
    }

    /**
     * Sets the maximum width in characters for any output column.
     * Values longer than this may be truncated.
     *
     * @param  maxWidth  maximum column value width in characters
     */
    @ConfigMethod(
        property = "maxCell",
        doc = "<p>Maximum width in characters of an output table cell. "
            + "Cells longer than this will be truncated.</p>"
    )
    public void setMaxWidth( int maxWidth ) {
        maxWidth_ = maxWidth;
    }

    /**
     * Returns the maximum width for any output column.  Values longer than
     * this may be truncated.
     *
     * @return  maximum permitted column width in characters
     */
    public int getMaxWidth() {
        return maxWidth_;
    }

    /**
     * Returns the minimum width required to output the actual characters
     * of the name for a given column.  Padding applied subsequently
     * by this object's {@link #printColumnHeads} method does not need
     * to be included.
     *
     * @param  info  column metadata
     * @return   minimum number of characters required for column title
     */
    public int getMinNameWidth( ColumnInfo info ) {
        return info.getName().length();
    }

    /**
     * Sets the number of rows which will be sampled before output is
     * commenced to work out the column widths.
     *
     * @param  sampledRows   number of rows to be sampled
     */
    @ConfigMethod(
        property = "sampledRows",
        doc = "<p>The number of rows examined on a first pass "
            + "of the table to determine the width of each column. "
            + "Only a representative number of rows needs to be examined, "
            + "but if a formatted cell value after this limit "
            + "is wider than the cells up to it, then such later wide cells "
            + "may get truncated. "
            + "If the value is &lt;=0, all rows are examined "
            + "in the first pass; "
            + "this is the default, but it can be configured to some "
            + "other value if that takes too long."
            + "</p>"
    )
    public void setSampledRows( int sampledRows ) {
        sampledRows_ = sampledRows;
    }

    /**
     * Returns the number of rows which will be sampled to 
     * work out the column width.
     *
     * @return   number of rows scanned
     */
    public int getSampledRows() {
        return sampledRows_;
    }

    /**
     * Formats a data value for output.
     *
     * @param  val  the value
     * @param  vinfo  the metadata object describing <code>val</code>'s type
     * @param  width  maximum preferred width into which the value should
     *         be formatted
     * @return  formatted string meaning <code>value</code>,
     *          preferably no longer than <code>width</code> characters
     */
    protected abstract String formatValue( Object val, ValueInfo vinfo,
                                           int width );

    /**
     * Outputs a decorative separator line, of the sort you might find
     * between the column headings and the table data.
     *
     * @param  strm   stream to write into
     * @param  colwidths  column widths in characters
     */
    protected abstract void printSeparator( OutputStream strm, int[] colwidths )
            throws IOException;

    /**
     * Outputs headings for the table columns.
     *
     * @param   strm  stream to write into
     * @param   colwidths   column widths in characters
     * @param   cinfos   array of column headings
     */
    protected abstract void printColumnHeads( OutputStream strm,
                                              int[] colwidths,
                                              ColumnInfo[] cinfos )
            throws IOException;

    /**
     * Outputs a line of table data.
     *
     * @param  strm  stream to write into
     * @param  colwidths  column widths in characters
     * @param  data  array of strings to be output, one per column
     */
    protected abstract void printLine( OutputStream strm, int[] colwidths,
                                       String[] data ) 
            throws IOException;

    /**
     * Outputs a parameter and its value.
     *
     * @param   strm  stream to write into
     * @param   name  parameter name
     * @param   value  formatted parameter value
     * @param   clazz  type of value
     */
    protected abstract void printParam( OutputStream strm, String name,
                                        String value, Class<?> clazz )
            throws IOException;

    /**
     * Returns a byte array corresponding to a given string.
     *
     * @param  str  string to decode
     */
    protected byte[] getBytes( String str ) {

        /* The decoding here is not that respectable (doesn't properly
         * handle Unicode), but it makes a big performance difference,
         * e.g. when writing out a table. 
         * Leave it unless we find ourselves using much in the way of
         * unicode characters.
         * The correct way would be do use str.decode(). */
        int leng = str.length();
        byte[] buf = new byte[ leng ];
        for ( int i = 0; i < leng; i++ ) {
            buf[ i ] = (byte) str.charAt( i );
        }
        return buf;
    }

    int getMaxDataWidth( Class<?> clazz ) {

        /* It's not straightforward to find the maximum possible size for
         * floating point stringifications.  This routine returned incorrect
         * (too small) * values for 15 years.
         * The values below, probably 1 larger than actual maxima,
         * are taken from https://stackoverflow.com/questions/1701055/. */
        if ( clazz == Double.class ) {
            return 24;  // 17 + 3 + 4;
        }
        else if ( clazz == Float.class ) {
            return 16;  // 10 + 2 + 4;
        }
        else if ( clazz == Long.class ) {
            return Math.max( Long.toString( Long.MIN_VALUE ).length(),
                             Long.toString( Long.MAX_VALUE ).length() );
        }
        else if ( clazz == Integer.class ) {
            return Math.max( Integer.toString( Integer.MIN_VALUE ).length(),
                             Integer.toString( Integer.MAX_VALUE ).length() );
        }
        else if ( clazz == Short.class ||
                  clazz == Byte.class ||
                  clazz == Character.class ) {
            return Math.max( Short.toString( Short.MIN_VALUE ).length(),
                             Short.toString( Short.MAX_VALUE ).length() );
        }
        else {
            return getMaxWidth();
        }
    }
}
