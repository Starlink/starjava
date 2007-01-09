package uk.ac.starlink.table.formats;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
public class TextTableWriter extends StreamStarTableWriter {

    private boolean writeParams = true;

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

    /**
     * Returns true for <tt>location</tt> argument which ends in ".txt"
     * or is equal to "-", indicating standard output.
     *
     * @param   location  the intended destination of the output
     * @return  whether it looks suitable for this class
     */
    public boolean looksLikeFile( String location ) {
        return location.equals( "-" ) 
            || location.endsWith( ".txt" );
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
        int sr = getSampledRows();
        while ( rseq.next() ) {
            sampleList.add( rseq.getRow() );
            if ( --sr <= 0 ) {
                allRowsSampled = true;
                break;
            }
        }

        /* Get the column headers and prepare to work out column widths 
         * for formatting. */
        int ncol = startab.getColumnCount();
        ColumnInfo[] cinfos = new ColumnInfo[ ncol ];
        int[] cwidths = new int[ ncol ];
        int[] maxWidths = new int[ ncol ];
        for ( int i = 0; i < ncol; i++ ) {
            cinfos[ i ] = startab.getColumnInfo( i );
            cwidths[ i ] = cinfos[ i ].getName().length();
            maxWidths[ i ] = getMaxWidth( cinfos[ i ].getContentClass() );
        }

        /* Go through the sample to determine field widths. */
        for ( Iterator it = sampleList.iterator(); it.hasNext(); ) {
            Object[] row = (Object[]) it.next();
            for ( int i = 0; i < ncol; i++ ) {
                String formatted = cinfos[ i ]
                                  .formatValue( row[ i ], maxWidths[ i ] );
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
                        cwidths[ icol ] = nchar;
                    }
                }
            }
        }

        /* Apply sensible maximum field widths. */
        for ( int i = 0; i < ncol; i++ ) {
            cwidths[ i ] = Math.min( maxWidths[ i ], cwidths[ i ] );
        }

        /* Print parameters. */
        if ( writeParams ) {
            String name = startab.getName();
            if ( name != null && name.trim().length() > 0 ) {
                printParam( strm, "Table name", name );
            }
            for ( Iterator it = startab.getParameters().iterator();
                  it.hasNext(); ) {
                DescribedValue param = (DescribedValue) it.next();
                printParam( strm, param.getInfo().getName(),
                                  param.getValueAsString( 160 ) );
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
        this.writeParams = writeParams;
    }

    /**
     * Finds out whether the output will include table parameters.
     *
     * @return  true iff the table parameters will be output as well as the
     *          table data
     */
    public boolean getWriteParameters() {
        return writeParams;
    }

    /**
     * Returns the maximum width for a given column.  Values longer than
     * this may be truncated.
     *
     * @return  maximum permitted column width in characters
     */
    public int getMaxWidth() {
        return 40;
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
    protected String formatValue( Object val, ValueInfo vinfo, int width ) {
        return vinfo.formatValue( val, width );
    }

    /**
     * Outputs a decorative separator line, of the sort you might find
     * between the column headings and the table data.
     *
     * @param  strm   stream to write into
     * @param  colwidths  column widths in characters
     */
    protected void printSeparator( OutputStream strm, int[] colwidths )
            throws IOException {
        for ( int i = 0; i < colwidths.length; i++ ) {
            strm.write( '+' );
            strm.write( '-' );
            for ( int j = 0; j < colwidths[ i ]; j++ ) {
                strm.write( '-' );
            }
            strm.write( '-' );
        }
        strm.write( '+' );
        strm.write( '\n' );
    }

    /**
     * Outputs headings for the table columns.
     *
     * @param   strm  stream to write into
     * @param   colwidths   column widths in characters
     * @param   cinfos   array of column headings
     */
    protected void printColumnHeads( OutputStream strm, int[] colwidths,
                                     ColumnInfo[] cinfos ) throws IOException {
        int ncol = cinfos.length;
        String[] heads = new String[ ncol ];
        for ( int i = 0; i < ncol; i++ ) {
            heads[ i ] = cinfos[ i ].getName();
        }
        printSeparator( strm, colwidths );
        printLine( strm, colwidths, heads );
        printSeparator( strm, colwidths );
    }

    /**
     * Outputs a line of table data.
     *
     * @param  strm  stream to write into
     * @param  colwidths  column widths in characters
     * @param  data  array of strings to be output, one per column
     */
    protected void printLine( OutputStream strm, int[] colwidths,
                              String[] data ) 
            throws IOException {
        for ( int i = 0; i < colwidths.length; i++ ) {
            strm.write( '|' );
            strm.write( ' ' );
            String datum = ( data[ i ] == null ) ? "" : data[ i ];
            int padding = colwidths[ i ] - datum.length();
            strm.write( getBytes( datum ), 0,
                        Math.min( colwidths[ i ], datum.length() ) );
            if ( padding > 0 ) {
                for ( int j = 0; j < padding; j++ ) {
                    strm.write( ' ' );
                }
            }
            strm.write( ' ' );
        }
        strm.write( '|' );
        strm.write( '\n' );
    }

    /**
     * Outputs a parameter and its value.
     *
     * @param   strm  stream to write into
     * @param   name  parameter name
     * @param   value  formatted parameter value
     */
    protected void printParam( OutputStream strm, String name, String value )
            throws IOException {
        strm.write( getBytes( name ) );
        strm.write( ':' );
        strm.write( ' ' );
        strm.write( getBytes( value ) );
        strm.write( '\n' );
    }

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

    private int getMaxWidth( Class clazz ) {
        if ( clazz == Double.class ) {
            return 13;
        }
        else if ( clazz == Float.class ) {
            return 11;
        }
        else {
            return getMaxWidth();
        }
    }
}
