package uk.ac.starlink.fits;

import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.logging.Logger;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.util.ArrayDataOutput;
import nom.tam.util.BufferedDataOutputStream;
import nom.tam.util.BufferedFile;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.Tables;

/**
 * Handles writing of a StarTable in FITS binary format.
 * Not all columns can be written to a FITS table, only those ones
 * whose <tt>contentClass</tt> is in the following list:
 * <ul>
 * <li>Boolean
 * <li>Character
 * <li>Byte
 * <li>Short
 * <li>Integer
 * <li>Float
 * <li>Double
 * <li>Character
 * <li>String
 * <li>boolean[]
 * <li>char[]
 * <li>byte[]
 * <li>short[]
 * <li>int[]
 * <li>float[]
 * <li>double[]
 * <li>String[]
 * </ul>
 * In all other cases a warning message will be logged and the column
 * will be ignored for writing purposes.
 * <p>
 * Output is currently to fixed-width columns only.  For StarTable columns
 * of variable size, a first pass is made through the table data to
 * determine the largest size they assume, and the size in the output
 * table is set to the largest of these.  Excess space is padded
 * with some sort of blank value (NaN for floating point values,
 * spaces for strings, zero-like values otherwise).
 * <p>
 * Null cell values are written using some zero-like value, not a proper
 * blank value.  Doing this right would require some changes to the tables
 * infrastructure.
 *
 * @author   Mark Taylor (Starlink)
 */
public class FitsTableWriter implements StarTableWriter {

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.fits" );

    /**
     * Returns "FITS".
     *
     * @return  format name
     */
    public String getFormatName() {
        return "FITS";
    }

    /**
     * Returns true if <tt>location</tt> ends with something like ".fit"
     * or ".fits".
     *
     * @param  location  filename
     * @return true if it sounds like a fits file
     */
    public boolean looksLikeFile( String location ) {
        int dotPos = location.lastIndexOf( '.' );
        if ( dotPos > 0 ) {
            String exten = location.substring( dotPos + 1 ).toLowerCase();
            if ( exten.startsWith( "fit" ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes a table in FITS binary format.  Currently the output is
     * to a new file called <tt>location</tt>, in the first extension
     * (HDU 0 is a dummy header, since the primary HDU cannot hold a table).
     *
     * @param  startab  the table to write
     * @param  location  the filename to write to
     */
    public void writeStarTable( StarTable startab, String location ) 
            throws IOException {

        /* Get table dimensions (though we may need to calculate the row
         * count directly later). */
        int ncol = startab.getColumnCount();
        long nrow = startab.getRowCount();

        /* Store column infos. */
        ColumnInfo colinfos[] = Tables.getColumnInfos( startab );

        /* Work out column shapes, and check if any are unknown (variable
         * last dimension). */
        boolean hasVarShapes = false;
        int[][] shapes = new int[ ncol ][];
        int[] maxChars = new int[ ncol ];
        boolean[] useCols = new boolean[ ncol ];
        boolean[] varShapes = new boolean[ ncol ];
        boolean[] varChars = new boolean[ ncol ];
        boolean[] varElementChars = new boolean[ ncol ];
        Arrays.fill( useCols, true );
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo colinfo = colinfos[ icol ];
            Class clazz = colinfo.getContentClass();
            if ( clazz.isArray() ) {
                shapes[ icol ] = (int[]) colinfo.getShape().clone();
                int[] shape = shapes[ icol ];
                if ( shape[ shape.length - 1 ] < 0 ) {
                    varShapes[ icol ] = true;
                    hasVarShapes = true;
                }
                if ( clazz.getComponentType().equals( String.class ) ) {
                    maxChars[ icol ] = colinfo.getElementSize();
                    if ( maxChars[ icol ] <= 0 ) {
                        varElementChars[ icol ] = true;
                        hasVarShapes = true;
                    }
                }
            }
            else if ( clazz.equals( String.class ) ) {
                maxChars[ icol ] = colinfo.getElementSize();
                if ( maxChars[ icol ] <= 0 ) {
                    varChars[ icol ] = true;
                    hasVarShapes = true;
                }
            }
        }

        /* If necessary, make a first pass through the table data to 
         * find out the maximum size of variable length fields and the length
         * of the table. */
        if ( hasVarShapes || nrow < 0 ) {
            int[] maxElements = new int[ ncol ];
            nrow = 0L;

            /* Get the maximum dimensions. */
            for ( RowSequence rseq = startab.getRowSequence();
                  rseq.hasNext(); ) {
                rseq.next();
                nrow++;
                for ( int icol = 0; icol < ncol; icol++ ) {
                    if ( useCols[ icol ] && 
                         ( varShapes[ icol ] || varChars[ icol ] ||
                           varElementChars[ icol ] ) ) {
                        Object cell = rseq.getCell( icol );
                        if ( cell != null ) {
                            if ( varChars[ icol ] ) {
                                int leng = ((String) cell).length();
                                maxChars[ icol ] = 
                                    Math.max( maxChars[ icol ], leng );
                            }
                            else if ( varElementChars[ icol ] ) {
                                String[] svals = (String[]) cell;
                                for ( int i = 0; i < svals.length; i++ ) {
                                    maxChars[ icol ] = 
                                        Math.max( maxChars[ icol ],
                                                  svals[ i ].length() );
                                }
                            }
                            if ( varShapes[ icol ] ) {
                                maxElements[ icol ] =
                                    Math.max( maxElements[ icol ],
                                              Array.getLength( cell ) );
                            }
                        }
                    }
                }
            }

            /* Work out the actual shapes for columns which have variable ones,
             * based on the shapes that we encountered in the rows. */
            if ( hasVarShapes ) {
                for ( int icol = 0; icol < ncol; icol++ ) {
                    if ( useCols[ icol ] ) {
                        if ( varShapes[ icol ] ) {
                            int[] shape = shapes[ icol ];
                            int ndim = shape.length;
                            assert shape[ ndim - 1 ] <= 0;
                            int nel = 1;
                            for ( int i = 0; i < ndim - 1; i++ ) {
                                nel *= shape[ i ];
                            }
                            shape[ ndim - 1 ] =
                                Math.max( 1, ( maxElements[ icol ] 
                                               + nel - 1 ) / nel );
                        }
                    }
                }
            }
        }

        /* We now have all the information we need about the table.
         * Construct a custom writer for each column which knows about the
         * characteristics of the column and how to write values to the
         * stream. */
        int rowLength = 0;
        int nUseCol = 0;
        ColumnWriter[] colWriters = new ColumnWriter[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            if ( useCols[ icol ] ) {
                ColumnInfo cinfo = colinfos[ icol ];
                ColumnWriter writer = 
                    makeColumnWriter( colinfos[ icol ].getContentClass(),
                                      shapes[ icol ], maxChars[ icol ] );
                colWriters[ icol ] = writer;
                if ( writer != null ) {
                    rowLength += writer.getLength();
                    nUseCol++;
                }
                else {
                    logger.warning( "Ignoring column " + cinfo.getName() + 
                                    " - don't know how to write to FITS" );
                    useCols[ icol ] = false;
                }
            }
        }

        /* Prepare a FITS header block. */
        Header hdr = new Header();
        try {

            /* Prepare the overall HDU metadata. */
            hdr.addValue( "XTENSION", "BINTABLE", "binary table extension" );
            hdr.addValue( "BITPIX", 8, "8-bit bytes" );
            hdr.addValue( "NAXIS", 2, "2-dimensional table" );
            hdr.addValue( "NAXIS1", rowLength, "width of table in bytes" );
            hdr.addValue( "NAXIS2", nrow, "number of rows in table" );
            hdr.addValue( "PCOUNT", 0, "size of special data area" );
            hdr.addValue( "GCOUNT", 1, "one data group" );
            hdr.addValue( "TFIELDS", nUseCol, "number of columns" );

            /* Prepare the per-column HDU metadata. */
            int jcol = 0;
            for ( int icol = 0; icol < ncol; icol++ ) {
                if ( useCols[ icol ] ) {
                    jcol++;
                    String forcol = " for column " + jcol;
                    ColumnInfo colinfo = colinfos[ icol ];
                    ColumnWriter colwriter = colWriters[ icol ];

                    /* Name. */
                    String name = colinfo.getName();
                    if ( name != null && name.trim().length() > 0 ) {
                        hdr.addValue( "TTYPE" + jcol, name, "label" + forcol );
                    }

                    /* Format. */
                    String form = colwriter.getFormat();
                    hdr.addValue( "TFORM" + jcol, form, "format" + forcol );

                    /* Units. */
                    String unit = colinfo.getUnitString();
                    if ( unit != null && unit.trim().length() > 0 ) {
                        hdr.addValue( "TUNIT" + jcol, unit, "units" + forcol );
                    }

                    /* Shape. */
                    int[] dims = colwriter.getDims();
                    if ( dims != null && dims.length > 1 ) {
                        StringBuffer sbuf = new StringBuffer();
                        for ( int i = 0; i < dims.length; i++ ) {
                            sbuf.append( i == 0 ? '(' : ',' );
                            sbuf.append( dims[ i ] );
                        }
                        sbuf.append( ')' );
                        hdr.addValue( "TDIM" + jcol, sbuf.toString(), 
                                      "dimensions" + forcol );
                    }

                    /* Scaling. */
                    double zero = colwriter.getZero();
                    double scale = colwriter.getScale();
                    if ( zero != 0.0 ) {
                        hdr.addValue( "TZERO" + jcol, zero, "base" + forcol );
                    }
                    if ( scale != 1.0 ) {
                        hdr.addValue( "TSCALE" + jcol, scale, 
                                      "factor" + forcol );
                    }
                }
            }
        }
        catch ( FitsException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }

        /* Obtain the stream to write the table to. */
        ArrayDataOutput strm = null;
        try {
            strm = getOutputStream( location );

            /* Write the header block out. */
            try {
                hdr.write( strm );
            }
            catch ( FitsException e ) {
                throw (IOException) new IOException( e.getMessage() )
                                   .initCause( e );
            }

            /* Write the actual table cell data. */
            for ( RowSequence rseq = startab.getRowSequence();
                  rseq.hasNext(); ) {
                rseq.next();
                for ( int icol = 0; icol < ncol; icol++ ) {
                    if ( useCols[ icol ] ) {
                        colWriters[ icol ].writeValue( strm, 
                                                       rseq.getCell( icol ) );
                    }
                }
            }

            /* Write padding. */
            long nbyte = nrow * rowLength;
            int extra = (int) ( nbyte % (long) 2880 );
            if ( extra > 0 ) {
                strm.write( new byte[ 2880 - extra ] );
            }
        }
        finally {
            if ( strm != null ) {
                strm.close();
            }
        }
    }

    /**
     * Returns a stream ready to accept a table HDU.
     * Currently just opens a new file at <tt>location</tt> and
     * writes a dummy primary header to it.
     */
    private static ArrayDataOutput getOutputStream( String location )
            throws IOException {

        /* Get a stream. */
        ArrayDataOutput strm;
        if ( location.equals( "-" ) ) {
            strm = new BufferedDataOutputStream( System.out );
        }
        else {
            BufferedFile bstrm = new BufferedFile( location, "rw" );
            strm = bstrm;

            /* Attempting to set the length to zero if it's already zero
             * can sometimes throw an exception (if it's /dev/null).
             * So don't set the length unnecessarily. */
            if ( bstrm.length() > 0L ) {
                bstrm.setLength( 0L );
            }
        }

        /* Write a null header for the primary HDU. */
        try {
            Header dummy = new Header();
            dummy.addValue( "SIMPLE", true, "Standard FITS format" );
            dummy.addValue( "BITPIX", 8, "Character data" );
            dummy.addValue( "NAXIS", 0, "No image, just extensions" );
            dummy.addValue( "EXTEND", true, "There are standard extensions" );
            dummy.insertComment(
                      "Dummy header; see following table extension" );
            dummy.insertCommentStyle( "END", "" );
            dummy.write( strm );
        }
        catch ( FitsException e ) {
            throw (IOException) new IOException( e.toString() )
                               .initCause( e );
        }

        /* Return the stream. */
        return strm;
    }

    /**
     * Returns a column writer capable of writing a given column to 
     * a stream in FITS format.
     *
     * @param   clazz  class of values to write
     * @param   shape  shape for array values
     * @param   elementSize  element size
     * @return  a suitable column writer, or <tt>null</tt> if we don't
     *          know how to write this to FITS
     */
    private static ColumnWriter makeColumnWriter( Class clazz, int[] shape,
                                                  int eSize ) {
        int n1 = 1;
        if ( shape != null ) {
            for ( int i = 0; i < shape.length; i++ ) {
                n1 *= shape[ i ];
            }
        }
        final int nel = n1;

        if ( clazz == Boolean.class ) {
            return new ColumnWriter( 'L', 1 ) {
                void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    boolean flag = value.equals( Boolean.TRUE );
                    stream.writeByte( flag ? (byte) 'T' : (byte) 'F' );
                }
            };
        }
        else if ( clazz == Byte.class ) {

            /* Byte is a bit tricky since a FITS byte is unsigned, while
             * a byte in a StarTable (a java byte) is signed. */
            final byte[] buf = new byte[ 1 ];
            return new ColumnWriter( 'B', 1 ) {
                void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    byte b = (value != null) ? ((Number) value).byteValue()
                                             : (byte) 0;
                    buf[ 0 ] = (byte) ( b ^ (byte) 0x80 );
                    stream.write( buf );
                }
                double getZero() {
                    return -128.0;
                }
            };
        }
        else if ( clazz == Short.class ) {
            return new ColumnWriter( 'I', 2 ) {
                void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    short sval = ( value != null ) 
                               ? ((Number) value).shortValue()
                               : (short) 0;
                    stream.writeShort( sval );
                }
            };
        }
        else if ( clazz == Integer.class ) {
            return new ColumnWriter( 'J', 4 ) {
                void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    int ival = ( value != null ) 
                             ? ((Number) value).intValue()
                             : 0;
                    stream.writeInt( ival );
                }
            };
        }
        else if ( clazz == Float.class ) {
            return new ColumnWriter( 'E', 4 ) {
                void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    float fval = ( value != null ) 
                               ? ((Number) value).floatValue()
                               : Float.NaN;
                    stream.writeFloat( fval );
                }
            };
        }
        else if ( clazz == Double.class ) {
            return new ColumnWriter( 'D', 8 ) {
                void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    double dval = ( value != null ) 
                                ? ((Number) value).doubleValue()
                                : Double.NaN;
                    stream.writeDouble( dval );
                }
            };
        }
        else if ( clazz == Character.class ) {
            return new ColumnWriter( 'A', 1 ) {
                void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    char cval = ( value != null )
                              ? ((Character) value).charValue()
                              : ' ';
                    stream.writeByte( cval );
                }
            };
        }
        else if ( clazz == String.class ) {
            final int maxChars = eSize;
            final byte[] buf = new byte[ maxChars ];
            final byte[] blankBuf = new byte[ maxChars ];
            final byte PAD = (byte) ' ';
            Arrays.fill( blankBuf, PAD );
            return new ColumnWriter( 'A', maxChars ) {
                void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    byte[] bytes;
                    if ( value == null ) {
                        bytes = blankBuf;
                    }
                    else {
                        String sval = (String) value;
                        int i = 0;
                        int leng = Math.min( sval.length(), maxChars );
                        bytes = buf;
                        for ( ; i < leng; i++ ) {
                            bytes[ i ] = (byte) sval.charAt( i );
                        }
                        Arrays.fill( bytes, i, maxChars, PAD );
                    }
                    stream.write( bytes );
                }
                String getFormat() {
                    return Integer.toString( maxChars ) + 'A';
                }
            };
        }
        else if ( clazz == boolean[].class ) {
            final byte[] buf = new byte[ nel ];
            final byte PAD = 'F';
            return new ColumnWriter( 'L', 1, shape ) {
                void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    int i = 0;
                    if ( value != null ) {
                        boolean[] bvals = (boolean[]) value;
                        int leng = Math.min( bvals.length, nel );
                        for ( ; i < leng; i++ ) {
                            buf[ i ] = bvals[ i ] ? (byte) 'T' : (byte) 'F';
                        }
                    }
                    Arrays.fill( buf, i, nel, PAD );
                    stream.write( buf );
                }
            };
        }
        else if ( clazz == byte[].class ) {
            final byte[] buf = new byte[ nel ];
            final byte PAD = (byte) 0;
            return new ColumnWriter( 'B', 1, shape ) {
                void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    int i = 0;
                    if ( value != null ) {
                        byte[] bvals = (byte[]) value;
                        int leng = Math.min( bvals.length, nel );
                        for ( ; i < leng; i++ ) {
                            buf[ i ] = bvals[ i ];
                        }
                    }
                    Arrays.fill( buf, i, nel, PAD );
                    for ( int j = 0; j < nel; j++ ) {
                        buf[ j ] = (byte) ( buf[ j ] ^ (byte) 0x80 );
                    }
                    stream.write( buf );
                }
                double getZero() {
                    return -128.0;
                }
            };
        }
        else if ( clazz == short[].class ) {
            final short PAD = (short) 0;
            return new ColumnWriter( 'I', 2, shape ) {
                void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    int i = 0;
                    if ( value != null ) {
                        short[] svals = (short[]) value;
                        int leng = Math.min( svals.length, nel );
                        for ( ; i < leng; i++ ) {
                            stream.writeShort( svals[ i ] );
                        }
                    }
                    for ( ; i < nel; i++ ) {
                        stream.writeShort( PAD );
                    }
                }
            };
        }
        else if ( clazz == int[].class ) {
            final int PAD = 0;
            return new ColumnWriter( 'J', 4, shape ) {
                void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    int i = 0;
                    if ( value != null ) {
                        int[] ivals = (int[]) value;
                        int leng = Math.min( ivals.length, nel );
                        for ( ; i < leng; i++ ) {
                            stream.writeInt( ivals[ i ] );
                        }
                    }
                    for ( ; i < nel; i++ ) {
                        stream.writeInt( PAD );
                    }
                }
            };
        }
        else if ( clazz == float[].class ) {
            final float PAD = Float.NaN;
            return new ColumnWriter( 'E', 4, shape ) {
                void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    int i = 0;
                    if ( value != null ) {
                        float[] fvals = (float[]) value;
                        int leng = Math.min( fvals.length, nel );
                        for ( ; i < leng; i++ ) {
                            stream.writeFloat( fvals[ i ] );
                        }
                    }
                    for ( ; i < nel; i++ ) {
                        stream.writeFloat( PAD );
                    }
                } 
            };
        }
        else if ( clazz == double[].class ) {
            final double PAD = Double.NaN;
            return new ColumnWriter( 'D', 8, shape ) {
                void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    int i = 0;
                    if ( value != null ) {
                        double[] dvals = (double[]) value;
                        int leng = Math.min( dvals.length, nel );
                        for ( ; i < leng; i++ ) {
                            stream.writeDouble( dvals[ i ] );
                        }
                    }
                    for  ( ; i < nel; i++ ) {
                        stream.writeDouble( PAD );
                    }
                }
            };
        }
        else if ( clazz == String[].class ) {
            final byte PAD = (byte) ' ';
            final int maxChars = eSize;
            int[] charDims = new int[ shape.length + 1 ];
            charDims[ 0 ] = maxChars;
            System.arraycopy( shape, 0, charDims, 1, shape.length );
            final byte[] buf = new byte[ maxChars ];
            return new ColumnWriter( 'A', 1, charDims ) {
                void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    int i = 0;
                    if ( value != null ) {
                        String[] svals = (String[]) value;
                        int leng = Math.min( svals.length, nel );
                        for ( ; i < leng; i++ ) {
                            String str = svals[ i ];
                            int j = 0;
                            if ( str != null ) {
                                int sleng = Math.min( str.length(), maxChars );
                                for ( ; j < sleng; j++ ) {
                                    buf[ j ] = (byte) str.charAt( j );
                                }
                            }
                            Arrays.fill( buf, j, maxChars, PAD );
                            stream.write( buf );
                        }
                    }
                    if ( i < nel ) {
                        Arrays.fill( buf, PAD );
                        for ( ; i < nel; i++ ) {
                            stream.write( buf );
                        }
                    }
                }
                String getFormat() {
                    return Integer.toString( maxChars * nel ) + 'A';
                }
            };
        }
        else {
            return null;
        }
    }

    /**
     * Abstract class defining what needs to be done to write an object
     * to a DataOutput.
     */
    private static abstract class ColumnWriter {
        final int length;
        final int[] dims;
        String format;
        
        /**
         * Constructs a new writer with a given length and dimension array.
         *
         * @param  formatChar  the basic character denoting this writers 
         *         format in a FITS header
         * @param  length  number of bytes per element
         * @param  dims  array to be imposed on written value
         */
        ColumnWriter( char formatChar, int elength, int[] dims ) {
            this.dims = dims;
            int nel = 1;
            if ( dims != null ) {
                for ( int i = 0; i < dims.length; i++ ) {
                    nel *= dims[ i ];
                }
            }
            length = elength * nel;
            format = ( nel == 1 ) ? "" + formatChar
                                  : ( Integer.toString( nel ) + formatChar );
        }

        /**
         * Constructs a new scalar writer with a given length.
         *
         * @param  formatChar  the basic character denoting this writers 
         *         format in a FITS header
         * @param  length  number of bytes per value
         */
        ColumnWriter( char formatChar, int length ) {
            this( formatChar, length, null );
        }
 
        /**
         * Writes a value to an output stream.
         *
         * @param  stream to squirt the value's byte serialization into
         * @param  value  the value to write into <tt>stream</tt>
         */
        abstract void writeValue( DataOutput stream, Object value ) 
                throws IOException;

        /**
         * Returns the TFORM string appropriate for this writer.
         *
         * @return  format string
         */
        String getFormat() {
            return format;
        }

        /**
         * Returns the number of bytes that <tt>writeValue</tt> will write.
         */
        int getLength() {
            return length;
        }

        /**
         * Returns the dimensionality (in FITS terms) of the values 
         * that this writes.  Null for scalars and vectors.
         *
         * @return   dims
         */
        int[] getDims() {
            return dims;
        }

        /**
         * Returns zero offset to be used for interpreting values this writes.
         *
         * @param  zero value
         */
        double getZero() {
            return 0.0;
        }

        /**
         * Returns the scale factor to be used for interpreting values this
         * writes.
         *
         * @param  scale factor
         */
        double getScale() {
            return 1.0;
        }
    }

}
