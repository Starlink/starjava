package uk.ac.starlink.fits;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.logging.Logger;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.ImageData;
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
 * Note this applies to all String-type columns, since they are stored as
 * an array of characters (bytes) in FITS tables.
 * <p>
 * Null cell values are written using some zero-like value, not a proper
 * blank value.  Doing this right requires some fixes in the tables
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

        /* Work out column shapes.  For FITS output a String is a 
         * character array. */
        int[][] shapes = new int[ ncol ][];
        boolean[] useCols = new boolean[ ncol ];
        Arrays.fill( useCols, true );
        boolean hasVarShapes = false;
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo colinfo = colinfos[ icol ];
            int[] shape;
            Class clazz = colinfo.getContentClass();
            int[] cshape = colinfo.getShape();
            if ( clazz.equals( String[].class ) ) {
                shape = new int[ cshape.length + 1 ];
                shape[ 0 ] = -1;
                System.arraycopy( cshape, 0, shape, 1, cshape.length );
                hasVarShapes = true;
            }
            else if ( clazz.getComponentType() != null ) {
                if ( clazz.getComponentType().getComponentType() == null ) {
                    shape = colinfo.getShape();
                    if ( shape[ shape.length - 1 ] < 0 ) {
                        hasVarShapes = true;
                    }
                }
                else {
                    useCols[ icol ] = false;
                    shape = null;
                    logger.warning( "Can't write column " + icol + " of type "
                                  + clazz );
                }
            }
            else if ( clazz.equals( String.class ) ) {
                shape = new int[] { -1 };
                hasVarShapes = true;
            }
            else {
                shape = new int[] { 1 };
            }
            shapes[ icol ] = shape;
        }

        /* First pass through table data to find out the size of variable
         * length fields and the length of the table if necessary. */
        if ( hasVarShapes || nrow < 0 ) {
            nrow = 0L;

            /* Get the maximum dimensions. */
            int[] maxElements = new int[ ncol ];
            int[] maxChars = new int[ ncol ];
            for ( RowSequence rseq = startab.getRowSequence(); 
                  rseq.hasNext(); ) {
                rseq.next();
                nrow++;
                Object[] row = rseq.getRow();
                for ( int icol = 0; icol < ncol; icol++ ) {
                    if ( useCols[ icol ] ) {
                        Object cell = row[ icol ];
                        if ( cell != null ) {
                            Class clazz = cell.getClass();
                            if ( clazz == String.class ) {
                                maxChars[ icol ] =
                                    Math.max( maxChars[ icol ],
                                              ((String) cell).length() );
                            }
                            else {
                                Class eclazz = clazz.getComponentType();
                                if ( eclazz != null ) {
                                    int nel = Array.getLength( cell );
                                    maxElements[ icol ] = 
                                        Math.max( maxElements[ icol ], nel );
                                    if ( eclazz == String.class ) {
                                        String[] els = (String[]) cell;
                                        for ( int i = 0; i < nel; i++ ) {
                                            maxChars[ icol ] = 
                                                Math.max( maxChars[ icol ], 
                                                          els[ i ].length() );
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            /* Work out the actual shapes for columns which have variable
             * ones, based on the shapes that we encountered in the rows. */
            if ( hasVarShapes ) {
                for ( int icol = 0; icol < ncol; icol++ ) {
                    if ( useCols[ icol ] ) {
                        ColumnInfo colinfo = colinfos[ icol ];
                        Class clazz = colinfo.getContentClass();
                        int[] shape = shapes[ icol ];
                        int ndim = shape == null ? -1 : shape.length;
                        if ( clazz.equals( String[].class ) ) {
                            shape[ 0 ] = maxChars[ icol ];
                            if ( shape[ ndim - 1 ] < 0 ) {
                                int nel = 1;
                                for ( int i = 0; i < ndim - 1; i++ ) {
                                    nel *= shape[ i ];
                                }
                                shape[ ndim - 1 ] = Math.max( 1,
                                    ( maxElements[ icol ] + nel - 1 ) / nel );
                            }
                        }
                        else if ( clazz.getComponentType() != null ) {
                            if ( shape[ ndim - 1 ] < 0 ) {
                                int nel = 1;
                                for ( int i = 0; i < ndim - 1; i++ ) {
                                    nel *= shape[ i ];
                                }
                                shape[ ndim - 1 ] = Math.max( 1, 
                                    ( maxElements[ icol ] + nel - 1 ) / nel );
                            }
                        }
                        else if ( clazz.equals( String.class ) ) {
                            shape[ 0 ] = Math.max( 1, maxChars[ icol ] );
                        }
                    }
                }
            }
        }

        /* Work out the number of elements, format string and value translator
         * for each column. */
        int[] nElements = new int[ ncol ];
        String[] tforms = new String[ ncol ];
        String[] tdims = new String[ ncol ];
        boolean[] isBytes = new boolean[ ncol ];
        ValueTranslator[] translators = new ValueTranslator[ ncol ];
        int byteCount = 0;
        for ( int icol = 0; icol < ncol; icol++ ) {
            if ( useCols[ icol ] ) {
                int[] shape = shapes[ icol ];

                /* Construct the TDIM header string for multidimensional
                 * arrays. */
                if ( shape.length >= 2 ) {
                    StringBuffer sbuf = new StringBuffer();
                    for ( int i = 0; i < shape.length; i++ ) {
                        sbuf.append( i == 0 ? '(' : ',' );
                        sbuf.append( shape[ i ] );
                    }
                    sbuf.append( ')' );
                    tdims[ icol ] = sbuf.toString();
                }

                /* Get the number of elements */
                int nel = 1;
                for ( int i = 0; i < shape.length; i++ ) {
                    assert shape[ i ] >= 0;
                    nel *= shape[ i ];
                }
                nElements[ icol ] = nel;

                /* Work out its format descriptor and
                 * number of bytes per element, and construct a 
                 * ValueTranslator object to deal with turning it from 
                 * the object we get out of the column into an object
                 * which can get written down an ArrayDataOutput. */
                Class clazz = colinfos[ icol ].getContentClass();
                ValueTranslator trans;
                char fchar;
                int nbyte;

                if ( clazz == Boolean.class ) {
                    fchar = 'L';
                    nbyte = 1;
                    trans = new ValueTranslator() {
                        byte[] cell = new byte[ 1 ];
                        byte[] blank = new byte[] { (byte) 'F' };
                        public Object translate( Object base ) {
                            if ( base == null ) {
                                return blank;
                            }
                            cell[ 0 ] = ((Boolean) base).booleanValue()
                                      ? (byte) 'T' 
                                      : (byte) 'F';
                            return cell;
                        }
                    };
                }
                else if ( clazz == boolean[].class ) {
                    fchar = 'L';
                    nbyte = 1;
                    trans = new ArrayValueTranslator( new boolean[ nel ] );
                }

                else if ( clazz == Character.class ) {
                    fchar = 'A';
                    nbyte = 1;
                    trans = new ValueTranslator() {
                        char[] chars = new char[ 1 ];
                        char[] blank = new char[] { (char) 0 };
                        String[] cell = new String[ 1 ];
                        public Object translate( Object base ) {
                            if ( base == null ) {
                                return blank;
                            }
                            chars[ 0 ] = ((Character) base).charValue();
                            cell[ 0 ] = new String( chars );
                            return cell;
                        }
                    };
                }
                else if ( clazz == char[].class ) {
                    fchar = 'A';
                    nbyte = 1;
                    final byte[] blank = new byte[ nel ];
                    Arrays.fill( blank, (byte) ' ' );
                    trans = new StringValueTranslator( new byte[ nel ] ) {
                        public Object translate( Object base ) {
                            if ( base == null ) {
                                return blank;
                            }
                            return super
                                  .translate( new String( (char[]) base ) );
                        }
                    };
                }

                else if ( clazz == String.class ) {
                    fchar = 'A';
                    nbyte = 1;
                    trans = new StringValueTranslator( new byte[ nel ] );
                }
                else if ( clazz == String[].class ) {
                    fchar = 'A';
                    nbyte = 1;
                    final int sleng = shape[ 0 ];
                    int mstr = 1;
                    for ( int i = 1; i < shape.length; i++ ) {
                        mstr *= shape[ i ];
                    }
                    final int maxstr = mstr;
                    final int fnel = nel;
                    trans = new ValueTranslator() {
                        ValueTranslator auxTrans = 
                            new StringValueTranslator( new byte[ sleng ] );
                        byte[] cell = new byte[ fnel ];
                        public Object translate( Object base ) {
                            String[] strings = (String[]) base;
                            int nstr = ( strings == null ) 
                                     ? 0
                                     : Math.min( strings.length, maxstr );
                            for ( int i = 0; i < nstr; i++ ) {
                                byte[] buf =
                                    (byte[]) auxTrans.translate( strings[ i ] );
                                System.arraycopy( buf, 0, cell, i * sleng, 
                                                  sleng );
                            }
                            Arrays.fill( cell, nstr * sleng, fnel, (byte) ' ' );
                            return cell;
                        }
                    };
                }

                else if ( clazz == Byte.class ) {
                    fchar = 'B';
                    nbyte = 1;
                    trans = new ValueTranslator() {
                        byte[] cell = new byte[ 1 ];
                        byte[] blank = new byte[] { (byte) 0 };
                        public Object translate( Object base ) {
                            if ( base == null ) {
                                return blank;
                            }
                            cell[ 0 ] = 
                                prepareByte( ((Byte) base).byteValue() );
                            return cell;
                        }
                    };
                }
                else if ( clazz == byte[].class ) {
                    fchar = 'B';
                    nbyte = 1;
                    trans = new ArrayValueTranslator( new byte[ nel ] ) {
                        public Object translate( Object base ) {
                            byte[] cell = (byte[]) super.translate( base );
                            for ( int i = 0; i < cell.length; i++ ) {
                                cell[ i ] = prepareByte( cell[ i ] );
                            }
                            return cell;
                        }
                    };
                }

                else if ( clazz == Short.class  ) {
                    fchar = 'I';
                    nbyte = 2;
                    trans = new ValueTranslator() {
                        short[] cell = new short[ 1 ];
                        short[] blank = new short[] { (short) 0 };
                        public Object translate( Object base ) {
                            if ( base == null ) {
                                return blank;
                            }
                            cell[ 0 ] = ((Short) base).shortValue();
                            return cell;
                        }
                    };
                }
                else if ( clazz == short[].class ) {
                    fchar = 'I';
                    nbyte = 2;
                    trans = new ArrayValueTranslator( new short[ nel ] );
                }

                else if ( clazz == Integer.class ) {
                    fchar = 'J';
                    nbyte = 4;
                    trans = new ValueTranslator() {
                        int[] cell = new int[ 1 ];
                        int[] blank = new int[] { 0 };
                        public Object translate( Object base ) {
                            if ( base == null ) {
                                return blank;
                            }
                            cell[ 0 ] = ((Integer) base).intValue();
                            return cell;
                        }
                    };
                }
                else if ( clazz == int[].class ) {
                    fchar = 'J';
                    nbyte = 4;
                    trans = new ArrayValueTranslator( new int[ nel ] );
                }

                else if ( clazz == Float.class ) {
                    fchar = 'E';
                    nbyte = 4;
                    trans = new ValueTranslator() {
                        float[] cell = new float[ 1 ];
                        float[] blank = new float[] { Float.NaN };
                        public Object translate( Object base ) {
                            if ( base == null ) {
                                return blank;
                            }
                            cell[ 0 ] = ((Float) base).floatValue();
                            return cell;
                        }
                    };
                }
                else if ( clazz == float[].class ) {
                    fchar = 'E';
                    nbyte = 4;
                    trans = new ArrayValueTranslator( new float[ nel ],
                                              new float[] { Float.NaN } );
                }

                else if ( clazz == Double.class ) {
                    fchar = 'D';
                    nbyte = 8;
                    trans = new ValueTranslator() {
                        double[] cell = new double[ 1 ];
                        double[] blank = new double[] { Double.NaN };
                        public Object translate( Object base ) {
                            if ( base == null ) {
                                return blank;
                            }
                            cell[ 0 ] = ((Double) base).doubleValue();
                            return cell;
                        }
                    };
                }
                else if ( clazz == double[].class ) {
                    fchar = 'D';
                    nbyte = 8; 
                    trans = new ArrayValueTranslator( new double[ nel ],
                                              new double[] { Double.NaN } );
                }

                /* Longs are slightly different - you can't write them to
                 * FITS directly so we write them as Integers. */
                else if ( clazz == Long.class ) {
                    logger.info( "Truncating type of column " + icol +
                                 " from Long to Integer" );
                    fchar = 'J';
                    nbyte = 4;
                    trans = new ValueTranslator() {
                        int[] cell = new int[ 1 ];
                        int[] blank = new int[] { 0 };
                        public Object translate( Object base ) {
                            if ( base == null ) {
                                return blank;
                            }
                            cell[ 0 ] = ((Long) base).intValue();
                            return cell;
                        }
                    };
                }
                else if ( clazz == long[].class ) {
                    logger.info( "Truncating type of column " + icol +
                                 " from long[] to int[]" );
                    fchar = 'J';
                    nbyte = 4;
                    final int fnel = nel;
                    trans = new ValueTranslator() {
                        private long[] fill = new long[ fnel ];
                        private long[] cell = new long[ fnel ];
                        public Object translate( Object base ) {
                            if ( base == null ) {
                                return fill;
                            }
                            long[] lbase = (long[]) base;
                            for ( int i = 0; i < fnel; i++ ) {
                                cell[ i ] = i < lbase.length ? (int) lbase[ i ]
                                                             : 0;
                            }
                            return cell;
                        }
                    };
                }
       
                else {
                    useCols[ icol ] = false;
                    logger.warning( "Can't write column " + icol + " of type "
                                  + clazz );
                    fchar = '*';
                    nbyte = 0;
                    trans = null;
                }
                translators[ icol ] = trans;

                /* Work out the format string. */
                byteCount += nElements[ icol ] * nbyte;
                tforms[ icol ] = fchar + Integer.toString( nel );
            }
        }

        /* See how many columns we are really going to write. */
        int nUseCol = 0;
        for ( int icol = 0; icol < ncol; icol++ ) {
            if ( useCols[ icol ] ) {
                nUseCol++;
            }
        }

        /* Check that we have some rows to write. */
        if ( nUseCol == 0 ) {
            throw new IOException( "No columns to write in this table" );
        }

        /* Obtain a stream for output. */
        ArrayDataOutput strm = getOutputStream( location );

        /* Construct a header for the table HDU.  We could use the
         * BinaryHDU.manufactureHeader method for this, but we get more
         * control doing it by hand. */
        Header hdr = new Header();
        try {
            hdr.addValue( "XTENSION", "BINTABLE", "binary table extension" );
            hdr.addValue( "BITPIX", 8, "8-bit bytes" );
            hdr.addValue( "NAXIS", 2, "2-dimensional table" );
            hdr.addValue( "NAXIS1", byteCount, "width of table in bytes" );
            hdr.addValue( "NAXIS2", nrow, "number of rows in table" );
            hdr.addValue( "PCOUNT", 0, "size of special data area" );
            hdr.addValue( "GCOUNT", 1, "one data group" );
            hdr.addValue( "TFIELDS", nUseCol, "number of columns" );
            int jcol = 0;
            for ( int icol = 0; icol < ncol; icol++ ) {
                if ( useCols[ icol ] ) {
                    jcol++;
                    String forcol = " for column " + jcol;
                    ColumnInfo colinfo = colinfos[ icol ];
                    String name = colinfo.getName();
                    String unit = colinfo.getUnitString();
                    String tform = tforms[ icol ];
                    String tdim = tdims[ icol ];
                    if ( name != null && name.trim().length() > 0 ) {
                        hdr.addValue( "TTYPE" + jcol, name, "label" + forcol );
                    }
                    if ( unit != null && unit.trim().length() > 0 ) {
                        hdr.addValue( "TUNIT" + jcol, unit, "units" + forcol );
                    }
                    hdr.addValue( "TFORM" + jcol, tform, "format" + forcol );
                    if ( tdim != null ) {
                        hdr.addValue( "TDIM" + jcol, tdim, 
                                      "dimensions" + forcol );
                    }
                    if ( isBytes[ icol ] ) {
                        hdr.addValue( "TZERO" + jcol, -128, "base" + forcol );
                        hdr.addValue( "TSCAL" + jcol, 1, "factor" + forcol );
                    }
                }
            }

            /* Write the table header out. */
            hdr.write( strm );

        }
        catch ( FitsException e ) {
            throw (IOException) new IOException( e.toString() )
                               .initCause( e );
        }

        /* Iterate over the table rows writing each one. */
        for ( RowSequence rseq = startab.getRowSequence(); rseq.hasNext(); ) {
            rseq.next();
            for ( int icol = 0; icol < ncol; icol++ ) {
                if ( useCols[ icol ] ) {
                    strm.writeArray( translators[ icol ]
                                    .translate( rseq.getCell( icol ) ) );
                }
            }
        }

        /* Write padding to the end of the block. */
        long nbyte = nrow * hdr.getIntValue( "NAXIS1" );
        int extra = (int) ( nbyte % (long) 2880 );
        if ( extra > 0 ) {
            strm.write( new byte[ 2880 - extra ] );
        }

        /* Terminate the output. */
        strm.close();
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
            strm = new BufferedFile( location, "rw" );
            ((BufferedFile) strm).setLength( 0L );
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
     * Used to turn a java signed byte into a FITS unsigned byte.
     * In this case we use a TZERO of -128 to provide the offset but
     * still need to mess about with the bits to get the right value.
     */
    private static byte prepareByte( byte base ) {
        return (byte) ( base ^ (byte) 0x80 );
    }

    /**
     * Helper interface to define the action of translating from the 
     * object in a base StarTable cell to the object which needs to
     * get written to an output stream.
     */
    static interface ValueTranslator {
        public Object translate( Object base );
    }

    /**
     * ValueTranslator for arrays.
     */
    static class ArrayValueTranslator implements ValueTranslator {

        private int nel;
        private Object cell;
        private Object fill;

        /**
         * Constructs a new translator which will return the object
         * <tt>cell</tt>, padded with elements copied from 
         * <tt>fillValue[0]</tt> if necessary.
         */
        public ArrayValueTranslator( Object cell, Object fillValue ) {
            Class clazz = cell.getClass().getComponentType();
            assert fillValue.getClass().getComponentType() == clazz;
            assert Array.getLength( fillValue ) == 1;

            nel = Array.getLength( cell );
            this.cell = cell;
            this.fill = Array.newInstance( clazz, nel );
            for ( int i = 0; i < nel; i++ ) {
                System.arraycopy( fillValue, 0, fill, i, 1 );
            }
        }

        /**
         * Constructs a new ArrayValueTranslator with a default fill value.
         */
        public ArrayValueTranslator( Object cell ) {
            this( cell, Array.newInstance( cell.getClass().getComponentType(),
                                           1 ) );
        }

        public Object translate( Object base ) {
            if ( base == null ) {
                return fill;
            }
            assert base.getClass() == cell.getClass();
            int nb = Array.getLength( base );
            if ( nb == nel ) {
                return base;
            }
            else {
                int nup = Math.min( nel, nb );
                System.arraycopy( base, 0, cell, 0, nup );
                System.arraycopy( fill, nup, cell, nup, nel - nup );
                return cell;
            }
        }
    }

    /**
     * Turns a String into a byte array of fixed length.
     */
    static class StringValueTranslator extends ArrayValueTranslator {
        public StringValueTranslator( byte[] cell ) {
            super( cell, new byte[] { (byte) ' ' } );
        }
        public Object translate( Object base ) {
            return super.translate( ((String) base).getBytes() );
        }
    }


}
