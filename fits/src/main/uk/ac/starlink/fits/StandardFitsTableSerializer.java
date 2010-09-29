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
 * to FITS BINTABLE format.  A normal (row-oriented) organisation of the
 * data is used.  
 * Array-valued columns are all written as fixed size arrays.
 * This class does the hard work for FitsTableWriter.
 *
 * @author   Mark Taylor (Starlink)
 */
public class StandardFitsTableSerializer implements FitsTableSerializer {

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.fits" );

    private final boolean allowSignedByte;
    private StarTable table;
    private ColumnWriter[] colWriters;
    private ColumnInfo[] colInfos;
    private long rowCount;

    /**
     * Package-private constructor intended for use by subclasses.
     *
     * @param   allowSignedByte  if true, bytes written as FITS signed bytes
     *          (TZERO=-128), if false bytes written as signed shorts
     */
    StandardFitsTableSerializer( boolean allowSignedByte ) {
        this.allowSignedByte = allowSignedByte;
    }

    /**
     * Constructs a serializer to write a given StarTable, with explicit
     * instruction about how to write byte-type columns data.
     * Since FITS bytes are unsigned (unlike, for instance, java bytes), 
     * they can cause trouble in some circumstances, so avoiding writing
     * them may sometimes help.
     *
     * @param  table  the table to be written
     * @param  allowSignedByte  if true, bytes written as FITS signed bytes
     *         (TZERO=-128), if false bytes written as signed shorts
     */
    public StandardFitsTableSerializer( StarTable table,
                                        boolean allowSignedByte )
            throws IOException {
        this( allowSignedByte );
        init( table );
    }

    /**
     * Constructs a serializer which will be able to write a given StarTable.
     * Byte-type columns are written using some default policy.
     *
     * @param  table  the table to be written
     */
    public StandardFitsTableSerializer( StarTable table ) throws IOException {
        this( table, true );
    }

    /**
     * Configures this serializer for use with a given table and column writer
     * factory.  Should be called before this object is ready for use;
     * in a constructor would be a good place.
     * Calls {@link #createColumnWriter}.
     *
     * @param  table  table to be written
     */
    final void init( StarTable table ) throws IOException {
        if ( this.table != null ) {
            throw new IllegalStateException( "Table already initialised" );
        }
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
        int[] maxElements = new int[ ncol ];
        long[] totalElements = new long[ ncol ];
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
                else {
                    int nel = shape.length > 0 ? 1 : 0;
                    for ( int id = 0; id < shape.length; id++ ) {
                        nel *= shape[ id ];
                    }
                    assert nel >= 0;
                    maxElements[ icol ] = nel;
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
            StringBuffer sbuf = new StringBuffer( "First pass needed: " );
            if ( hasVarShapes ) {
                sbuf.append( "(variable array shapes) " );
            }
            if ( hasNullableInts ) {
                sbuf.append( "(nullable ints) " );
            }
            if ( nrow < 0 ) {
                sbuf.append( "(unknown row count) " );
            }
            logger.config( sbuf.toString() );
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
                                    int nel = Array.getLength( cell );
                                    maxElements[ icol ] =
                                        Math.max( maxElements[ icol ], nel );
                                    totalElements[ icol ] += nel;
                                }
                            }
                        }
                    }
                }
            }
            finally {
                rseq.close();
            }

            /* In the case of variable string lengths and no non-null data
             * in any of the cells, maxChars could still be set negative.
             * Fix that here. */
            for ( int icol = 0; icol < ncol; icol++ ) {
                if ( maxChars[ icol ] < 0 ) {
                    maxChars[ icol ] = 0;
                }
            }

            /* Furthermore, zero length strings are probably a bad idea
             * for FITS output.  Make sure that all output strings have
             * a length of at least 1. */
            for ( int icol = 0; icol < ncol; icol++ ) {
                if ( maxChars[ icol ] == 0 ) {
                    maxChars[ icol ] = 1;
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
                    createColumnWriter( cinfo, shapes[ icol ],
                                        varShapes[ icol ], maxChars[ icol ],
                                        maxElements[ icol ],
                                        totalElements[ icol ],
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
     * Returns the array of column writers used by this serializer.
     * The list is generated once by the sole call of the 
     * <code>init</code> method.
     *
     * @return  column writer array
     */
    ColumnWriter[] getColumnWriters() {
        return colWriters;
    }

    public Header getHeader() throws HeaderCardException {

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

        /* Add HDU layout metadata. */
        hdr.addValue( "XTENSION", "BINTABLE", "binary table extension" );
        hdr.addValue( "BITPIX", 8, "8-bit bytes" );
        hdr.addValue( "NAXIS", 2, "2-dimensional table" );
        hdr.addValue( "NAXIS1", rowLength, "width of table in bytes" );
        hdr.addValue( "NAXIS2", rowCount, "number of rows in table" );
        hdr.addValue( "PCOUNT", 0, "size of special data area" );
        hdr.addValue( "GCOUNT", 1, "one data group" );
        hdr.addValue( "TFIELDS", nUseCol, "number of columns" );

        /* Add EXTNAME record containing table name. */
        String tname = table.getName();
        if ( tname != null && tname.trim().length() > 0 ) {
            FitsConstants
           .addTrimmedValue( hdr, "EXTNAME", tname, "table name" );
        }

        /* Add HDU metadata describing columns. */
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
                    FitsConstants
                   .addTrimmedValue( hdr, "TTYPE" + jcol, name,
                                     "label" + forcol );
                }

                /* Format. */
                String form = colwriter.getFormat();
                hdr.addValue( "TFORM" + jcol, form, "format" + forcol );

                /* Units. */
                String unit = colinfo.getUnitString();
                if ( unit != null && unit.trim().length() > 0 ) {
                    FitsConstants
                   .addTrimmedValue( hdr, "TUNIT" + jcol, unit,
                                     "units" + forcol );
                }

                /* Blank. */
                Number bad = colwriter.getBadNumber();
                if ( bad != null ) {
                    hdr.addValue( "TNULL" + jcol, bad.longValue(),
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
                    try {
                        hdr.addValue( "TCOMM" + jcol, comm, null );
                    }
                    catch ( HeaderCardException e ) {
                        // never mind.
                    }
                }

                /* UCD (non-standard). */
                String ucd = colinfo.getUCD();
                if ( ucd != null && ucd.trim().length() > 0 &&
                     ucd.length() < 68 ) {
                    try {
                        hdr.addValue( "TUCD" + jcol, ucd, null );
                    }
                    catch ( HeaderCardException e ) {
                        // never mind.
                    }
                }

                /* Utype (non-standard). */
                String utype = colinfo.getUtype();
                if ( utype != null && utype.trim().length() > 0
                                   && utype.trim().length() < 68 ) {
                    try {
                        hdr.addValue( "TUTYP" + jcol, utype, null );
                    }
                    catch ( HeaderCardException e ) {
                        // never mind.
                    }
                }
            }
        }
        return hdr;
    }

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

    public char getFormatChar( int icol ) {
        if ( colWriters[ icol ] == null ) {
            return (char) 0;
        }
        else {
            return colWriters[ icol ].getFormatChar();
        }
    }

    public int[] getDimensions( int icol ) {
        if ( colWriters[ icol ] == null ) {
            return null;
        }
        else {
            int[] dims = colWriters[ icol ].getDims();
            return dims == null ? new int[ 0 ] : dims;
        }
    }

    public String getBadValue( int icol ) {
        if ( colWriters[ icol ] == null ) {
            return null;
        }
        else {
            Number badnum = colWriters[ icol ].getBadNumber();
            return badnum == null ? null : badnum.toString();
        }
    }

    public long getRowCount() {
        return rowCount;
    }

    /**
     * Returns a column writer capable of writing a given column to
     * a stream in FITS format.
     *
     * @param   cinfo  describes the column to write
     * @param   maxShape  shape for array values
     * @param   varShape  whether shapes are variable
     * @param   elementSize  element size
     * @param   maxEls  maximum number of elements for any array in column
     *          (only applies if varShape is true)
     * @param   totalEls  total number of elements for all array values in col
     *          (only applies if varShape is true)
     * @param   nullableInt  true if we are going to have to store nulls in
     *          an integer column
     * @return  a suitable column writer, or <tt>null</tt> if we don't
     *          know how to write this to FITS
     */
    ColumnWriter createColumnWriter( ColumnInfo cinfo, int[] shape,
                                     boolean varShape, int eSize,
                                     final int maxEls, long totalEls,
                                     boolean nullableInt ) {
        Class clazz = cinfo.getContentClass();
        if ( clazz == String.class ) {
            final int maxChars = eSize;
            final int[] dims = new int[] { maxChars };
            final byte[] buf = new byte[ maxChars ];
            final byte[] blankBuf = new byte[ maxChars ];
            final byte padByte = (byte) ' ';
            Arrays.fill( blankBuf, padByte );
            return new ColumnWriter() {
                public void writeValue( DataOutput out, Object value )
                        throws IOException {
                    final byte[] bytes;
                    if ( value == null ) {
                        bytes = blankBuf;
                    }
                    else {
                        bytes = buf;
                        String sval = (String) value;
                        int leng = Math.min( sval.length(), maxChars );
                        for ( int i = 0; i < leng; i++ ) {
                            bytes[ i ] = (byte) sval.charAt( i );
                        }
                        for ( int i = leng; i < maxChars; i++ ) {
                            bytes[ i ] = padByte;
                        }
                    }
                    out.write( bytes );
                }
                public String getFormat() {
                    return Integer.toString( maxChars ) + 'A';
                }
                public char getFormatChar() {
                    return 'A';
                }
                public int getLength() {
                    return maxChars;
                }
                public int[] getDims() {
                    return dims;
                }
                public double getZero() {
                    return 0.0;
                }
                public double getScale() {
                    return 1.0;
                }
                public Number getBadNumber() {
                    return null;
                }
            };
        }
        else if ( clazz == String[].class ) {
            final int maxChars = eSize;
            final int[] charDims = new int[ shape.length + 1 ];
            charDims[ 0 ] = maxChars;
            System.arraycopy( shape, 0, charDims, 1, shape.length );
            final byte[] buf = new byte[ maxChars ];
            final byte padByte = (byte) ' ';
            return new ColumnWriter() {
                public void writeValue( DataOutput out, Object value )
                        throws IOException {
                    int is = 0;
                    if ( value != null ) {
                        String[] svals = (String[]) value;
                        int leng = Math.min( svals.length, maxEls );
                        for ( ; is < leng; is++ ) {
                            String str = svals[ is ];
                            int ic = 0;
                            if ( str != null ) {
                                int sleng = Math.min( str.length(), maxChars );
                                for ( ; ic < sleng; ic++ ) {
                                    buf[ ic ] = (byte) str.charAt( ic );
                                }
                            }
                            Arrays.fill( buf, ic, maxChars, padByte );
                            out.write( buf );
                        }
                    }
                    if ( is < maxEls ) {
                        Arrays.fill( buf, padByte );
                        for ( ; is < maxEls; is++ ) {
                            out.write( buf );
                        }
                    }
                }
                public String getFormat() {
                    return Integer.toString( maxChars * maxEls ) + 'A';
                }
                public char getFormatChar() {
                    return 'A';
                }
                public int getLength() {
                    return maxChars * maxEls;
                }
                public int[] getDims() {
                    return charDims;
                }
                public double getZero() {
                    return 0.0;
                }
                public double getScale() {
                    return 1.0;
                }
                public Number getBadNumber() {
                    return null;
                }
            };
        }
        else {
            ColumnWriter cw =
                ScalarColumnWriter.createColumnWriter( cinfo, nullableInt,
                                                       allowSignedByte );
            if ( cw != null ) {
                return cw;
            }
            else {
                ArrayWriter aw =
                    ArrayWriter.createArrayWriter( cinfo.getContentClass(),
                                                   allowSignedByte );
                if ( aw != null ) {
                    return new FixedArrayColumnWriter( aw, shape );
                }
                else {
                    return null;
                }
            }
        }
    }

    /**
     * ColumnWriter implementation for arrays with fixed sizes.
     */
    static class FixedArrayColumnWriter implements ColumnWriter {

        private final ArrayWriter arrayWriter_;
        private final int[] shape_;
        private final int nel_;

        /**
         * Constructor.
         *
         * @param  arrayWriter  writer which knows how to output the correct
         *         data type
         * @param  shape  fixed array dimensions of objects to be written
         */
        FixedArrayColumnWriter( ArrayWriter arrayWriter, int[] shape ) {
            arrayWriter_ = arrayWriter;
            shape_ = shape;
            int nel = 1;
            if ( shape != null ) {
                for ( int i = 0; i < shape.length; i++ ) {
                    nel *= shape[ i ];
                }
            }
            nel_ = nel;
        }

        public void writeValue( DataOutput out, Object value )
                throws IOException {
            int leng = Math.min( value == null ? 0 : Array.getLength( value ),
                                 nel_ );
            for ( int i = 0; i < leng; i++ ) {
                arrayWriter_.writeElement( out, value, i );
            }
            for ( int i = leng; i < nel_; i++ ) {
                arrayWriter_.writePad( out );
            }
        }

        public char getFormatChar() {
            return arrayWriter_.getFormatChar();
        }

        public String getFormat() {
            String fc = new String( new char[] { getFormatChar() } );
            return nel_ == 1 ? fc
                             : ( Integer.toString( nel_ ) + fc );
        }

        public int getLength() {
            return nel_ * arrayWriter_.getByteCount();
        }

        public int[] getDims() {
            return shape_;
        }

        public double getZero() {
            return arrayWriter_.getZero();
        }

        public double getScale() {
            return 1.0;
        }

        public Number getBadNumber() {
            return null;
        }
    }
}
