package uk.ac.starlink.fits;

import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.logging.Logger;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

/**
 * Class which knows how to do the various bits of serializing a StarTable
 * to FITS BINTABLE format.  This does the hard work for FitsTableWriter.
 *
 * @author   Mark Taylor (Starlink)
 */
public class FitsTableSerializer {

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.fits" );

    private final StarTable table;
    private final ColumnWriter[] colWriters;
    private final ColumnInfo[] colInfos;
    private final long rowCount;

    /**
     * Constructs a serializer which will be able to write a given StarTable.
     *
     * @param  table  the table to be written
     */
    public FitsTableSerializer( StarTable table ) throws IOException {
        this.table = table;

        /* Get table dimensions (though we may need to calculate the row
         * count directly later. */
        int ncol = table.getColumnCount();
        long nrow = table.getRowCount();

        /* Store column infos. */
        colInfos = Tables.getColumnInfos( table );

        /* Work out column shapes, and check if any are unknown (variable
         * last dimension). */
        boolean hasVarShapes = false;
        boolean hasNullableInts = false;
        int[][] shapes = new int[ ncol ][];
        int[] maxChars = new int[ ncol ];
        boolean[] useCols = new boolean[ ncol ];
        boolean[] varShapes = new boolean[ ncol ];
        boolean[] varChars = new boolean[ ncol ];
        boolean[] varElementChars = new boolean[ ncol ];
        boolean[] nullableInts = new boolean[ ncol ];
        Arrays.fill( useCols, true );
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo colinfo = colInfos[ icol ];
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
            else if ( colinfo.isNullable() && 
                      ( clazz == Byte.class || clazz == Short.class ||
                        clazz == Integer.class || clazz == Long.class ) ) {
                nullableInts[ icol ] = true;
                hasNullableInts = true;
            }
        }

        /* If necessary, make a first pass through the table data to
         * find out the maximum size of variable length fields and the length
         * of the table. */
        boolean[] hasNulls = new boolean[ ncol ];
        if ( hasVarShapes || hasNullableInts || nrow < 0 ) {
            int[] maxElements = new int[ ncol ];
            nrow = 0L;

            /* Get the maximum dimensions. */
            RowSequence rseq = table.getRowSequence();
            try {
                while ( rseq.next() ) {
                    nrow++;
                    for ( int icol = 0; icol < ncol; icol++ ) {
                        if ( useCols[ icol ] &&
                             ( varShapes[ icol ] || 
                               varChars[ icol ] ||
                               varElementChars[ icol ] || 
                               ( nullableInts[ icol ] && 
                                 ! hasNulls[ icol ] ) ) ) {
                            Object cell = rseq.getCell( icol );
                            if ( cell == null ) {
                                if ( nullableInts[ icol ] ) {
                                    hasNulls[ icol ] = true;
                                }
                            }
                            else {
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
            }
            finally {
                rseq.close();
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

        /* Store the row count, which we must have got by now. */
        assert nrow >= 0;
        rowCount = nrow;

        /* We now have all the information we need about the table.
         * Construct and store a custom writer for each column which 
         * knows about the characteristics of the column and how to 
         * write values to the stream.  For columns which can't be 
         * written in FITS format store a null in the writers array
         * and log a message. */
        colWriters = new ColumnWriter[ ncol ];
        int rbytes = 0;
        for ( int icol = 0; icol < ncol; icol++ ) {
            if ( useCols[ icol ] ) {
                ColumnInfo cinfo = colInfos[ icol ];
                ColumnWriter writer =
                    makeColumnWriter( cinfo, shapes[ icol ], maxChars[ icol ],
                                      nullableInts[ icol ] 
                                      && hasNulls[ icol ] );
                if ( writer == null ) {
                    logger.warning( "Ignoring column " + cinfo.getName() +
                                    " - don't know how to write to FITS" );
                }
                colWriters[ icol ] = writer;
            }
        }
    }

    /**
     * Writes the header block for the BINTABLE extension HDU which holds
     * the table data.
     *
     * @param  strm   destination stream
     */
    public void writeHeader( DataOutput strm ) throws IOException {

        /* Work out the dimensions in columns and bytes of the table. */
        int rowLength = 0;
        int nUseCol = 0;
        int ncol = table.getColumnCount();
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnWriter writer = colWriters[ icol ];
            if ( writer != null ) {
                nUseCol++;
                rowLength += writer.getLength();
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
            hdr.addValue( "NAXIS2", rowCount, "number of rows in table" );
            hdr.addValue( "PCOUNT", 0, "size of special data area" );
            hdr.addValue( "GCOUNT", 1, "one data group" );
            hdr.addValue( "TFIELDS", nUseCol, "number of columns" );

            /* Prepare the per-column HDU metadata. */
            int jcol = 0;
            for ( int icol = 0; icol < ncol; icol++ ) {
                ColumnWriter colwriter = colWriters[ icol ];
                if ( colwriter != null ) {
                    jcol++;
                    String forcol = " for column " + jcol;
                    ColumnInfo colinfo = colInfos[ icol ];

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

                    /* Blank. */
                    Number bad = colwriter.getBadNumber();
                    if ( bad != null ) {
                        hdr.addValue( "TNULL" + jcol, bad.toString(),
                                      "blank value" + forcol );
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

                    /* Comment (non-standard). */
                    String comm = colinfo.getDescription();
                    if ( comm != null && comm.trim().length() > 0 ) {
                        if ( comm.length() > 67 ) {
                            comm = comm.substring( 0, 68 );
                        }
                        try {
                            hdr.addValue( "TCOMM" + jcol, comm, null );
                        }
                        catch ( HeaderCardException e ) {
                            logger.warning( "Description " + comm +
                                            " too long for FITS header" );
                        }
                    }
                }
            }
        }
        catch ( FitsException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }

        /* Write the header block out. */
        FitsConstants.writeHeader( strm, hdr );
    }

    /**
     * Writes the data part of the BINTABLE extension HDU which holds
     * the table data.
     *
     * @param  strm   destination stream
     */
    public void writeData( DataOutput strm ) throws IOException {

        /* Work out the length of each row in bytes. */
        int rowBytes = 0;
        int ncol = table.getColumnCount();
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnWriter writer = colWriters[ icol ];
            if ( writer != null ) {
                rowBytes += writer.getLength();
            }
        }

        /* Write the data cells, delegating the item in each column to
         * the writer that knows how to handle it. */
        long nWritten = 0L;
        RowSequence rseq = table.getRowSequence();
        try {
            while ( rseq.next() ) {
                Object[] row = rseq.getRow();
                for ( int icol = 0; icol < ncol; icol++ ) {
                    ColumnWriter writer = colWriters[ icol ];
                    if ( writer != null ) {
                        writer.writeValue( strm, row[ icol ] );
                    }
                }
                nWritten += rowBytes;
            }
        }
        finally {
            rseq.close();
        }

        /* Write padding. */
        int extra = (int) ( nWritten % (long) 2880 );
        if ( extra > 0 ) {
            strm.write( new byte[ 2880 - extra ] );
        }
    }

    /**
     * Returns the FITS TFORM letter which describes the type of data
     * output for a given column.  This is as described by the FITS
     * standard - 'J' for 4-byte integer, 'A' for characters, etc.
     * If the column is not being output, <tt>(char)0</tt> will be 
     * returned.
     *
     * @param  icol   column to query
     * @return   format letter for data in column <tt>icol</tt>,
     *           or 0 for a column being skipped
     */
    public char getFormatChar( int icol ) {
        if ( colWriters[ icol ] == null ) {
            return (char) 0;
        }
        else {
            return colWriters[ icol ].getFormatChar();
        }
    }

    /**
     * Returns the dimensions of the items which will be output for a 
     * given column.  This will be <tt>null</tt> only if that column
     * is not being output.  Otherwise it will be a zero-element array
     * for a scalar, 1-element array for a vector, etc.
     *
     * @param  icol  column to query
     * @return   dimensions array for data in column <tt>icol</tt> 
     *           or <tt>null</tt>  for a column being skipped
     */
    public int[] getDimensions( int icol ) {
        if ( colWriters[ icol ] == null ) {
            return null;
        }
        else {
            int[] dims = colWriters[ icol ].getDims();
            return dims == null ? new int[ 0 ] : dims;
        }
    }

    /**
     * Returns a column writer capable of writing a given column to
     * a stream in FITS format.
     *
     * @param   cinfo  describes the column to write
     * @param   shape  shape for array values
     * @param   elementSize  element size
     * @param   nullableInt  true if we are going to have to store nulls in
     *          an integer column
     * @return  a suitable column writer, or <tt>null</tt> if we don't
     *          know how to write this to FITS
     */
    private static ColumnWriter makeColumnWriter( ColumnInfo cinfo, int[] shape,
                                                  int eSize, 
                                                  final boolean nullableInt ) {
        Class clazz = cinfo.getContentClass();

        int n1 = 1;
        if ( shape != null ) {
            for ( int i = 0; i < shape.length; i++ ) {
                n1 *= shape[ i ];
            }
        }
        final int nel = n1;

        Number blankNum = null;
        if ( nullableInt ) {
            DescribedValue blankVal = 
                cinfo.getAuxDatum( Tables.NULL_VALUE_INFO );
            if ( blankVal != null ) {
                Object blankObj = blankVal.getValue();
                if ( blankObj instanceof Number ) {
                    blankNum = (Number) blankObj;
                }
            }
        }

        if ( clazz == Boolean.class ) {
            return new ColumnWriter( 'L', 1 ) {
                void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    boolean flag = Boolean.TRUE.equals( value );
                    stream.writeByte( flag ? (byte) 'T' : (byte) 'F' );
                }
            };
        }
        else if ( clazz == Byte.class ) {

            /* Byte is a bit tricky since a FITS byte is unsigned, while
             * a byte in a StarTable (a java byte) is signed. */
            final byte[] buf = new byte[ 1 ];
            final byte badVal = blankNum == null ? (byte) 0
                                                 : blankNum.byteValue();
            return new ColumnWriter( 'B', 1 ) {
                void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    byte b = (value != null) ? ((Number) value).byteValue()
                                             : badVal;
                    buf[ 0 ] = (byte) ( b ^ (byte) 0x80 );
                    stream.write( buf );
                }
                double getZero() {
                    return -128.0;
                }
                Number getBadNumber() {
                    return nullableInt ? new Byte( badVal ) : null;
                }
            };
        }
        else if ( clazz == Short.class ) {
            final short badVal = blankNum == null ? Short.MIN_VALUE
                                                  : blankNum.shortValue();
            return new ColumnWriter( 'I', 2 ) {
                void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    short sval = ( value != null )
                               ? ((Number) value).shortValue()
                               : badVal;
                    stream.writeShort( sval );
                }
                Number getBadNumber() {
                    return nullableInt ? new Short( badVal ) : null;
                }
            };
        }
        else if ( clazz == Integer.class ) {
            final int badVal = blankNum == null ? Integer.MIN_VALUE
                                                : blankNum.intValue();
            return new ColumnWriter( 'J', 4 ) {
                void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    int ival = ( value != null )
                             ? ((Number) value).intValue()
                             : badVal;
                    stream.writeInt( ival );
                }
                Number getBadNumber() {
                    return nullableInt ? new Integer( badVal ) : null;
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
            final int[] charDims = new int[] { maxChars };
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
                int[] getDims() {
                    return charDims;
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
        final char formatChar;
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
            this.formatChar = formatChar;
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
         * Returns the format character appropriate for this writer.
         *
         * @return  format character
         */
        char getFormatChar() {
            return formatChar;
        }

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
         * that this writes.  Null for scalars.
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

        /**
         * Returns the number to be used for blank field output (TNULLn).
         * Only relevant for integer scalar items.
         *
         * @return  magic bad value 
         */
        Number getBadNumber() {
            return null;
        }
    }
}
