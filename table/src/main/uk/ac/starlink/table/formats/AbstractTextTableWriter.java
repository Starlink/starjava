package uk.ac.starlink.table.formats;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StreamStarTableWriter;
import uk.ac.starlink.table.ValueInfo;

/**
 * A <tt>StarTableWriter</tt> which outputs text to a human-readable text file.
 * Table parameters (per-table metadata) can optionally be output 
 * as well as the table data themselves.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class AbstractTextTableWriter extends StreamStarTableWriter {

    private boolean writeParams_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.formats" );

    /**
     * Constructor.
     *
     * @param  writeParams  whether parameters will be written by default
     */
    protected AbstractTextTableWriter( boolean writeParams ) {
        writeParams_ = writeParams;
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
        RowSequence rseq = startab.getRowSequence();
        try {
            writeStarTable( startab, rseq, strm );
        }
        finally {
            rseq.close();
        }
    }

    /**
     * Writes the table given a row sequence.
     *
     * @param   startab  table
     * @param   rseq     row sequence
     * @param   strm     output stream
     */
    private void writeStarTable( StarTable startab, RowSequence rseq,
                                 OutputStream strm ) throws IOException {

        /* Fill a buffer with a sample of the rows.  This will be used to
         * work out field widths, prior to being written out as normal
         * row data. */
        List sampleList = new ArrayList();
        boolean allRowsSampled = false;
        int maxSamples = getSampledRows();
        logger_.config( "Reading <=" + maxSamples 
                      + " rows to guess column widths" );
        for ( int ir = 0; ir < maxSamples; ir++ ) {
            if ( ! rseq.next() ) {
                allRowsSampled = true;
                break;
            }
            sampleList.add( rseq.getRow() );
        }
        logger_.config( sampleList.size()
                      + ( allRowsSampled ? " (all)" : "" )
                      + " rows read to guess column widths" );

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

        /* Go through the sample to determine field widths. */
        for ( Iterator it = sampleList.iterator(); it.hasNext(); ) {
            Object[] row = (Object[]) it.next();
            for ( int i = 0; i < ncol; i++ ) {
                String formatted = cinfos[ i ]
                                  .formatValue( row[ i ], maxDataWidths[ i ] );
                if ( formatted.length() > cwidths[ i ] ) {
                    cwidths[ i ] = formatted.length();
                }
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
            String name = startab.getName();
            if ( name != null && name.trim().length() > 0 ) {
                printParam( strm, "Table name", name, String.class );
            }
            for ( Iterator it = startab.getParameters().iterator();
                  it.hasNext(); ) {
                DescribedValue param = (DescribedValue) it.next();
                ValueInfo info = param.getInfo();
                printParam( strm, info.getName(), param.getValueAsString( 160 ),
                            info.getContentClass() );
            }
        }

        /* Print headings. */
        printColumnHeads( strm, cwidths, cinfos );

        /* Print sample rows. */
        String[] data = new String[ ncol ];
        for ( Iterator it = sampleList.iterator(); it.hasNext(); ) {
            Object[] row = (Object[]) it.next();
            for ( int icol = 0; icol < ncol; icol++ ) {
                data[ icol ] = formatValue( row[ icol ], cinfos[ icol ],
                                            cwidths[ icol ] );
            }
            printLine( strm, cwidths, data );
        }

        /* Print remaining rows. */
        if ( ! allRowsSampled ) {
            logger_.config( "Streaming remaining data rows" );
        }
        else {
            assert ! rseq.next();
        }
        while ( rseq.next() ) {
            Object[] row = rseq.getRow();
            for ( int icol = 0; icol < ncol; icol++ ) {
                data[ icol ] = formatValue( row[ icol ], cinfos[ icol ],
                                            cwidths[ icol ] );
            }
            printLine( strm, cwidths, data );
        }

        /* Finish off. */
        printSeparator( strm, cwidths );
    }

    /**
     * Set whether the output should include table parameters.
     * If so they are written as name:value pairs one per line 
     * before the start of the table proper.
     *
     * @param writeParams  true iff you want table parameters to be output as
     *        well as the table data
     */
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
     * Returns the maximum width for a given column.  Values longer than
     * this may be truncated.
     *
     * @return  maximum permitted column width in characters
     */
    public abstract int getMaxWidth();

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
     * Returns the number of columns which will be sampled to 
     * work out the column width.
     *
     * @return   number of rows scanned
     */
    public int getSampledRows() {
        return 200;
    }

    /**
     * Formats a data value for output.
     *
     * @param  val  the value
     * @param  vinfo  the metadata object describing <tt>val</tt>'s type
     * @param  width  maximum preferred width into which the value should
     *         be formatted
     * @return  formatted string meaning <tt>value</tt>, preferably no longer
     *          than <tt>width</tt> characters
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
                                        String value, Class clazz )
            throws IOException;

    /**
     * Returns a byte array corresponding to a given string.
     *
     * @param  str  string to decode
     */
    protected static byte[] getBytes( String str ) {

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

    private int getMaxDataWidth( Class clazz ) {
        if ( clazz == Double.class ) {
            return Math.max( Double.toString( - Double.MAX_VALUE ).length(),
                             Double.toString( - Double.MIN_VALUE ).length() );
        }
        else if ( clazz == Float.class ) {
            return Math.max( Float.toString( - Float.MAX_VALUE ).length(),
                             Float.toString( - Float.MIN_VALUE ).length() );
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
