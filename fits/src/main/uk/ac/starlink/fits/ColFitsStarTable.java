package uk.ac.starlink.fits;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.logging.Logger;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.BufferedFile;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.Loader;

/**
 * StarTable based on a single-row FITS BINTABLE which contains the
 * data for an entire column in each cell of the table.
 * The BINTABLE must be the first extension of an uncompressed 
 * FITS table on disk.
 *
 * @author   Mark Taylor
 * @since    21 Jun 2006
 */
public class ColFitsStarTable extends ColumnStarTable {

    private final long nrow_;

    private static long mappedBytes_;
    private static boolean mapWarned_;
    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.fits" );
    private final static int MAX_SECTION_BYTES = Integer.MAX_VALUE;
    // private final static int MAX_SECTION_BYTES = 100 * 1024 * 1024;

    /**
     * Constructor.
     *
     * @param   file  file containing the FITS data
     * @param   hdr   header of the HDU containing the table
     * @param   dataPos  offset into <code>file</code> of the start of the
     *          data part of the HDU
     */
    public ColFitsStarTable( File file, Header hdr, long dataPos )
            throws IOException {
        HeaderCards cards = new HeaderCards( hdr );

        /* Check it's a BINTABLE. */
        if ( ! cards.getStringValue( "XTENSION" ).equals( "BINTABLE" ) ) {
            throw new TableFormatException( "HDU 1 not BINTABLE" );
        }

        /* Check it has exactly one row. */
        if ( cards.getIntValue( "NAXIS2" ).intValue() != 1 ) {
            throw new TableFormatException( "Doesn't have exactly one row" );
        }

        /* Find the number of columns. */
        int ncol = cards.getIntValue( "TFIELDS" ).intValue();

        /* Read metadata for each column from the FITS header cards. */
        long nrow = 0;
        ColumnInfo[] infos = new ColumnInfo[ ncol ];
        char[] formatChars = new char[ ncol ];
        int[][] itemShapes = new int[ ncol ][];
        Long[] blanks = new Long[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            int jcol = icol + 1;
            ColumnInfo cinfo = new ColumnInfo( "col" + jcol );

            /* Format character and length. */
            String tform = cards.getStringValue( "TFORM" + jcol ).trim();
            char formatChar = tform.charAt( tform.length() - 1 );

            /* Use a special value if we have byte values offset by 128
             * (which allows one to represent signed bytes as unigned ones). */
            if ( formatChar == 'B' &&
                 ( cards.containsKey( "TZERO" + jcol )
                   && cards.getDoubleValue( "TZERO" + jcol ).doubleValue()
                                                            == -128.0 ) &&
                 ( ! cards.containsKey( "TSCALE" + jcol )
                   || cards.getDoubleValue( "TSCALE" + jcol ).doubleValue()
                                                             == 1.0 ) ) {
                formatChar = 'b';
            }
  
            long nitem;
            try {
                nitem =
                    Long.parseLong( tform.substring( 0, tform.length() - 1 ) );
            }
            catch ( NumberFormatException e ) {
                throw new TableFormatException( "Bad TFORM " + tform );
            }
            formatChars[ icol ] = formatChar;

            /* Row count and item shape. */
            String tdims = cards.getStringValue( "TDIM" + jcol );
            long[] dims = parseTdim( tdims );
            if ( dims == null ) {
                throw new TableFormatException( "Bad TDIM value " + tdims );
            }
            if ( multiply( dims ) != nitem ) {
                throw new TableFormatException( "TDIM doesn't match TFORM" );
            }
            int[] itemShape = new int[ dims.length - 1 ];
            for ( int i = 0; i < dims.length - 1; i++ ) {
                itemShape[ i ] = Tables.checkedLongToInt( dims[ i ] );
            }
            long nr = dims[ dims.length - 1 ];
            if ( icol == 0 ) {
                nrow = nr;
            }
            else {
                if ( nr != nrow ) {
                    throw new TableFormatException( "Row count mismatch" );
                }
            }
            itemShapes[ icol ] = itemShape;

            /* Null value. */
            String blankKey = "TNULL" + jcol;
            Long blank = cards.containsKey( blankKey )
                       ? cards.getLongValue( blankKey )
                       : null;
            blanks[ icol ] = blank;

            /* Informational metadata. */
            String ttype = cards.getStringValue( "TTYPE" + jcol );
            if ( ttype != null ) {
                cinfo.setName( ttype );
            }
            String tunit = cards.getStringValue( "TUNIT" + jcol );
            if ( tunit != null ) {
                cinfo.setUnitString( tunit );
            }
            String tcomm = cards.getStringValue( "TCOMM" + jcol );
            if ( tcomm != null ) {
                cinfo.setDescription( tcomm );
            }
            String tucd = cards.getStringValue( "TUCD" + jcol );
            if ( tucd != null ) {
                cinfo.setUCD( tucd );
            }
            String tutype = cards.getStringValue( "TUTYP" + jcol );
            if ( tutype != null ) {
                cinfo.setUtype( tutype );
            }
            infos[ icol ] = cinfo;
        }
        nrow_ = nrow;

        /* Create ColumnData for each cell in the FITS BINTABLE, 
         * i.e. each column in the represented table, based on the
         * metadata we have ascertained from the header. */
        FileChannel chan = new RandomAccessFile( file, "r" ).getChannel();
        long pos = dataPos;
        for ( int icol = 0; icol < ncol; icol++ ) {
            MappedColumnData colData =
                createColumn( formatChars[ icol ], infos[ icol ],
                              itemShapes[ icol ], nrow_, blanks[ icol ],
                              chan, pos );
            addColumn( colData );
            pos += colData.getItemBytes() * nrow_;
        }

        /* Get table name. */
        if ( cards.containsKey( "EXTNAME" ) ) {
            String tname = cards.getStringValue( "EXTNAME" );
            if ( cards.containsKey( "EXTVER" ) ) {
                tname += "-" + cards.getStringValue( "EXTVER" );
            }
            setName( tname );
        }

        /* Add table params containing header card information. */
        getParameters().addAll( Arrays.asList( cards.getUnusedParams() ) );
    }

    public long getRowCount() {
        return nrow_;
    }

    /**
     * Parse the content of a FITS TDIMnn header card.
     * This has the form (a,b,c,..), where a, b, c are integer values.
     * Returns null if <code>tdim</code> is not of the expected form.
     *
     * @param   tdim   header card value
     * @return  array of values
     */
    static long[] parseTdim( String tdim ) {
        if ( tdim == null ) {
            return null;
        }
        tdim = tdim.trim();
        if ( tdim.charAt( 0 ) != '(' ||
             tdim.charAt( tdim.length() - 1 ) != ')' ) {
            return null;
        }
        String[] sdims = tdim.substring( 1, tdim.length() - 1 ).split( "," );
        long[] dims = new long[ sdims.length ];
        for ( int i = 0; i < sdims.length; i++ ) {
            try {
                dims[ i ] = Long.parseLong( sdims[ i ].trim() );
            }
            catch ( NumberFormatException e ) {
                return null;
            }
        }
        return dims;
    }

    /**
     * Utility method to multiply elements of a <code>long[]</code> array.
     *
     * @param   dims  array
     * @return  product of elements of <code>dims</code>
     */
    private static long multiply( long[] dims ) {
        long product = 1;
        for ( int i = 0; i < dims.length; i++ ) {
            product *= dims[ i ];
        }
        return product;
    }

    /**
     * Utility method to multiply elements of an <code>int[]</code> array.
     *
     * @param   dims  array
     * @return  product of elements of <code>dims</code>
     */
    private static long multiply( int[] dims ) {
        long product = 1;
        for ( int i = 0; i < dims.length; i++ ) {
            product *= dims[ i ];
        }
        return product;
    }

    /**
     * Factory method to create <code>MappedColumnData</code> instances.
     *
     * @param  formatChar  FITS format character indicating data type
     * @param  info    column metadata object
     * @param  itemShape  dimensions of each cell in the column
     * @param  nrow    number of rows in the table
     * @param  blank   null value for column, if any
     * @param  chan    file channel from which to map data
     * @param  pos     offset into file at which data for this column starts
     */
    private static MappedColumnData createColumn( char formatChar,
                                                  ColumnInfo info,
                                                  int[] itemShape, long nrow,
                                                  Number blank,
                                                  FileChannel chan, long pos ) 
            throws IOException {
        final int itemSize = Tables.checkedLongToInt( multiply( itemShape ) );
        final int[] SCALAR = new int[ 0 ];

        /* Scalar column types. */
        if ( itemSize == 1 ) {

            if ( formatChar == 'L' ) {
                info.setContentClass( Boolean.class );
                return createColumnData( info, 1, SCALAR, nrow, chan, pos,
                        new ValueReader() {
                    Object readValue( ByteBuffer buf, int offset ) {
                        switch ( buf.get( offset ) ) {
                            case 'T':
                                return Boolean.TRUE;
                            case 'F':
                                return Boolean.FALSE;
                            default: 
                                return null;
                        }
                    }
                } );
            }

            else if ( formatChar == 'A' ) {
                info.setContentClass( Character.class );
                info.setNullable( false );
                return createColumnData( info, 1, SCALAR, nrow, chan, pos,
                        new ValueReader() {
                    protected Object readValue( ByteBuffer buf, int offset ) {
                        return new Character( (char) 
                                              ( buf.get( offset ) & 0xff ) );
                    }
                } );
            }

            else if ( formatChar == 'B' ) {
                info.setContentClass( Short.class );
                final boolean hasBad = blank != null;
                final byte badval = hasBad ? blank.byteValue() : (byte) 0;
                info.setNullable( hasBad );
                return createColumnData( info, 1, SCALAR, nrow, chan, pos,
                        new ValueReader() {
                    Object readValue( ByteBuffer buf, int offset ) {
                        byte val = buf.get( offset );
                        return ( hasBad && val == badval )
                             ? null
                             : new Short( (short) ( val & 0xff ) );
                    }
                } );
            }

            else if ( formatChar == 'b' ) {
                info.setContentClass( Short.class );
                final boolean hasBad = blank != null;
                final byte badval = hasBad ? blank.byteValue() : (byte) 0;
                info.setNullable( hasBad );
                return createColumnData( info, 1, SCALAR, nrow, chan, pos,
                        new ValueReader() {
                    Object readValue( ByteBuffer buf, int offset ) {
                        byte val = buf.get( offset );
                        return ( hasBad && val == badval )
                             ? null
                             : new Short( (short) val );
                    }
                } );
            }

            else if ( formatChar == 'I' ) {
                info.setContentClass( Short.class );
                final boolean hasBad = blank != null;
                final short badval = hasBad ? blank.shortValue() : (short) 0;
                info.setNullable( hasBad );
                return createColumnData( info, 2, SCALAR, nrow, chan, pos,
                        new ValueReader() {
                    Object readValue( ByteBuffer buf, int offset ) {
                        short val = buf.getShort( offset );
                        return ( hasBad && val == badval )
                             ? null
                             : new Short( val );
                    }
                } );
            }

            else if ( formatChar == 'J' ) {
                info.setContentClass( Integer.class );
                final boolean hasBad = blank != null;
                final int badval = hasBad ? blank.intValue() : 0;
                info.setNullable( hasBad );
                return createColumnData( info, 4, SCALAR, nrow, chan, pos,
                        new ValueReader() {
                    Object readValue( ByteBuffer buf, int offset ) {
                        int val = buf.getInt( offset );
                        return ( hasBad && val == badval )
                             ? null
                             : new Integer( val );
                    }
                } );
            }

            else if ( formatChar == 'K' ) {
                info.setContentClass( Long.class );
                final boolean hasBad = blank != null;
                final long badval = hasBad ? blank.longValue() : 0L;
                info.setNullable( hasBad );
                return createColumnData( info, 8, SCALAR, nrow, chan, pos,
                        new ValueReader() {
                    Object readValue( ByteBuffer buf, int offset ) {
                        long val = buf.getLong( offset );
                        return ( hasBad && val == badval )
                             ? null
                             : new Long( val );
                    }
                } );
            }

            else if ( formatChar == 'E' ) {
                info.setContentClass( Float.class );
                return createColumnData( info, 4, SCALAR, nrow, chan, pos,
                        new ValueReader() {
                    Object readValue( ByteBuffer buf, int offset ) {
                        return new Float( buf.getFloat( offset ) );
                    }
                } );
            }

            else if ( formatChar == 'D' ) {
                info.setContentClass( Double.class );
                return createColumnData( info, 8, SCALAR, nrow, chan, pos,
                        new ValueReader() {
                    Object readValue( ByteBuffer buf, int offset ) {
                        return new Double( buf.getDouble( offset ) );
                    }
                } );
            }
        }

        /* Array column types. */
        else {
            info.setShape( itemShape );
            info.setNullable( false );

            if ( formatChar == 'L' ) {
                info.setContentClass( boolean[].class );
                return createColumnData( info, 1, itemShape, nrow, chan, pos,
                        new ValueReader() {
                    protected synchronized Object readValue( ByteBuffer buf,
                                                             int offset ) {
                        buf.position( offset );
                        boolean[] val = new boolean[ itemSize ];
                        for ( int i = 0; i < itemSize; i++ ) {
                            val[ i ] = buf.get() == (byte) 'T';
                        }
                        return val;
                    }
                } );
            }

            else if ( formatChar == 'B' ) {
                info.setContentClass( short[].class );
                return createColumnData( info, 1, itemShape, nrow, chan, pos,
                        new ValueReader() {
                    protected synchronized Object readValue( ByteBuffer buf,
                                                             int offset ) {
                        buf.position( offset );
                        short[] val = new short[ itemSize ];
                        for ( int i = 0; i < itemSize; i++ ) {
                            val[ i ] = (short) ( buf.get() & 0xff );
                        }
                        return val;
                    }
                } );
            }

            else if ( formatChar == 'b' ) {
                info.setContentClass( short[].class );
                return createColumnData( info, 1, itemShape, nrow, chan, pos,
                        new ValueReader() {
                    protected synchronized Object readValue( ByteBuffer buf,
                                                             int offset ) {
                        buf.position( offset );
                        short[] val = new short[ itemSize ];
                        for ( int i = 0; i < itemSize; i++ ) {
                            val[ i ] = (short) buf.get();
                        }
                        return val;
                    }
                } );
            }

            else if ( formatChar == 'I' ) {
                info.setContentClass( short[].class );
                return createColumnData( info, 2, itemShape, nrow, chan, pos,
                        new ValueReader() {
                    protected synchronized Object readValue( ByteBuffer buf,
                                                             int offset ) {
                        buf.position( offset );
                        short[] val = new short[ itemSize ];
                        for ( int i = 0; i < itemSize; i++ ) {
                            val[ i ] = buf.getShort();
                        }
                        return val;
                    }
                } );
            }

            else if ( formatChar == 'J' ) {
                info.setContentClass( int[].class );
                return createColumnData( info, 4, itemShape, nrow, chan, pos,
                        new ValueReader() {
                    protected synchronized Object readValue( ByteBuffer buf,
                                                             int offset ) {
                        buf.position( offset );
                        int[] val = new int[ itemSize ];
                        for ( int i = 0; i < itemSize; i++ ) {
                            val[ i ] = buf.getInt();
                        }
                        return val;
                    }
                } );
            }

            else if ( formatChar == 'K' ) {
                info.setContentClass( long[].class );
                return createColumnData( info, 8, itemShape, nrow, chan, pos,
                        new ValueReader() {
                    protected synchronized Object readValue( ByteBuffer buf,
                                                             int offset ) {
                        buf.position( offset );
                        long[] val = new long[ itemSize ];
                        for ( int i = 0; i < itemSize; i++ ) {
                            val[ i ] = buf.getLong();
                        }
                        return val;
                    }
                } );
            }

            else if ( formatChar == 'E' ) {
                info.setContentClass( float[].class );
                return createColumnData( info, 4, itemShape, nrow, chan, pos,
                        new ValueReader() {
                    protected synchronized Object readValue( ByteBuffer buf,
                                                             int offset ) {
                        buf.position( offset );
                        float[] val = new float[ itemSize ];
                        for ( int i = 0; i < itemSize; i++ ) {
                            val[ i ] = buf.getFloat();
                        }
                        return val;
                    }
                } );
            }

            else if ( formatChar == 'D' ) {
                info.setContentClass( double[].class );
                return createColumnData( info, 8, itemShape, nrow, chan, pos,
                        new ValueReader() {
                    protected synchronized Object readValue( ByteBuffer buf,
                                                             int offset ) {
                        buf.position( offset );
                        double[] val = new double[ itemSize ];
                        for ( int i = 0; i < itemSize; i++ ) {
                            val[ i ] = buf.getDouble();
                        }
                        return val;
                    }
                } );
            }

            else if ( formatChar == 'A' ) {
                final int sleng = itemShape[ 0 ];
                final char[] charBuf = new char[ sleng ];
                info.setElementSize( sleng );
                info.setNullable( true );
                if ( itemShape.length == 1 ) {
                    info.setContentClass( String.class );
                    return createColumnData( info, sleng, SCALAR, nrow,
                                             chan, pos,
                            new ValueReader() {
                        protected synchronized Object readValue( ByteBuffer buf,
                                                                 int offset ) {
                            buf.position( offset );
                            int iend = 0;
                            boolean end = false;
                            for ( int i = 0; i < sleng; i++ ) {
                                byte b = buf.get();
                                if ( b == 0 ) {
                                    end = true;
                                }
                                if ( ! end ) {
                                    charBuf[ i ] = (char) ( b & 0xff );
                                    if ( b != (byte) ' ' ) {
                                        iend = i + 1;
                                    }
                                }
                            }
                            return iend > 0 ? new String( charBuf, 0, iend )
                                            : null;
                        }
                    } );
                }
                else {
                    info.setContentClass( String[].class );
                    int[] sshape = new int[ itemShape.length - 1 ];
                    System.arraycopy( itemShape, 1, sshape, 0, sshape.length );
                    info.setShape( sshape );
                    final int nstring = itemSize / sleng;
                    assert nstring * sleng == itemSize;
                    return createColumnData( info, sleng, sshape, nrow,
                                             chan, pos,
                            new ValueReader() {
                        protected synchronized Object readValue( ByteBuffer buf,
                                                                 int offset ) {
                            buf.position( offset );
                            String[] val = new String[ nstring ];
                            for ( int is = 0; is < nstring; is++ ) {
                                int iend = 0;
                                boolean end = false;
                                for ( int ic = 0; ic < sleng; ic++ ) {
                                    byte b = buf.get();
                                    if ( b == 0 ) {
                                        end = true;
                                    }
                                    if ( ! end ) {
                                        charBuf[ ic ] = (char) ( b & 0xff );
                                        if ( b != (byte) ' ' ) {
                                            iend = ic + 1;
                                        }
                                    }
                                }
                                val[ is ] = iend > 0
                                          ? new String( charBuf, 0, iend )
                                          : null;
                            }
                            return val;
                        }
                    } );
                }
            }
        }

        /* Unknown. */
        throw new IOException( "Unknown TFORM character '" + formatChar + "'" );
    }

    /**
     * Creates a new mapped column data object.
     *
     * @param   info  column metadata
     * @param   typeBytes  number of bytes per scalar element
     * @param   itemShape  dimensions of column cells
     * @param   nrow       number of entries in the column
     * @param   chan       file channel to map data from
     * @param   pos        offset into file of start of column
     * @param   reader     object for reading data from byte buffer
     */
    private static MappedColumnData createColumnData( ColumnInfo info,
                                                      int typeBytes,
                                                      int[] itemShape,
                                                      long nrow,
                                                      FileChannel chan,
                                                      long pos,
                                                      ValueReader reader )
            throws IOException {

        /* Work out the size of the region to map. */
        long itemBytes = Tables.checkedLongToInt( multiply( itemShape ) )
                       * typeBytes;
        long colBytes = itemBytes * nrow;

        /* Issue a warning if we're in danger of running out of address space.
         * Since this is a kind of failure that many users will be unfamiliar
         * with, the more warning/explanation the better.  Only need to 
         * issue the warning once per run though. */
        if ( mappedBytes_ + colBytes > ( 1L << 30 ) &&
             ! Loader.is64Bit() &&
             ! mapWarned_ ) {
            logger_.warning( "Doing a lot of mapping - "
                           + "may run out of address space on 32-bit JVM" );
            mapWarned_ = true;
        }

        /* Attempt to construct a column data object using either monolithic
         * or sectioned mapping. */
        MappedColumnData cdata;
        if ( colBytes <= MAX_SECTION_BYTES ) {
            cdata = new SingleMappedColumnData( info, typeBytes, itemShape,
                                                nrow, chan, pos, reader );
        }
        else {
            int nsec = (int) ( colBytes / MAX_SECTION_BYTES ) + 1;
            cdata = new SectionsMappedColumnData( info, typeBytes, itemShape,
                                                  nrow, chan, pos, nsec,
                                                  reader );
        }

        /* If successful, update the total number of mapped bytes.
         * Exact accounting of this figure is not essential, so we 
         * don't bother to update it in MappedColumnData finalizers. */
        mappedBytes_ += colBytes;

        /* Return the column. */
        return cdata;
    }

    /**
     * Recasts a <code>long</code> value which is known to be in range 
     * to an <code>int</code>.
     *
     * @param   lval  long value, must be between Integer.MIN_VALUE
     *                and Integer.MAX_VALUE
     * @return  <code>int</code> equivalent of <code>lval</code>
     * @param   throws   AssertionError if <code>lval</code> is out of range
     *          (and asssertions are enabled)
     */
    private static int toInt( long lval ) {
        int ival = (int) lval;
        assert (long) ival == lval;
        return ival;
    }

    /**
     * Interface for reading column cells from a mapped buffer.
     */
    private static abstract class ValueReader {

        /**
         * Reads an object from a byte buffer.
         * 
         * @param    buf  mapped buffer
         * @param    offset  position in <code>buf</code> of cell start
         * @throws   RuntimeException
         *           (as thrown by {@link java.nio.ByteBuffer#read})
         *           in case of a read error
         */
        abstract Object readValue( ByteBuffer buf, int offset );
    }

    /**
     * Abstract superclass for ColumnData implementations used by this class.
     */
    private static abstract class MappedColumnData extends ColumnData {

        protected final int itemBytes_;
        protected final long nrow_;
        private final long pos_;
        private final FileChannel chan_;
        private final ValueReader reader_;

        /**
         * Constructor.
         *
         * @param   info  column metadata
         * @param   typeBytes  number of bytes per scalar element
         * @param   itemShape  dimensions of column cells
         * @param   nrow       number of entries in the column
         * @param   chan       file channel to map data from
         * @param   pos        offset into file of start of column
         * @param   reader     object for reading data from byte buffer
         */
        MappedColumnData( ColumnInfo info, int typeBytes, int[] itemShape,
                          long nrow, FileChannel chan, long pos,
                          ValueReader reader ) {
            super( info );
            itemBytes_ = Tables.checkedLongToInt( multiply( itemShape ) )
                       * typeBytes;
            chan_ = chan;
            nrow_ = nrow;
            pos_ = pos;
            reader_ = reader;
        }

        /**
         * Reads a single value from the mapped buffer.
         *
         * @param  buf  mapped buffer
         * @param  offset  byte offset into <code>buf</code> of item to read
         */
        protected Object readValue( ByteBuffer buf, int offset )
                throws IOException {
            try {
                return reader_.readValue( buf, offset );
            }
            catch ( RuntimeException e ) {
                throw (IOException)
                      new IOException( "Failed buffer read attempt" )
                     .initCause( e );
            }
        }

        /**
         * Maps a region of the file associated with this object into a buffer.
         *
         * @param   offset  start of region to map,
         *           relative to the start of this buffer's column in the file
         * @param   size    length of region to map
         * @return  mapped buffer
         */
        protected ByteBuffer mapBuffer( long offset, int size )
                throws IOException {
            return chan_.map( FileChannel.MapMode.READ_ONLY,
                              pos_ + offset, size );
        }

        /**
         * Returns the size in bytes of a scalar element of this column's type.
         *
         * @return  scalar item size
         */
        public int getItemBytes() {
            return itemBytes_;
        }
    }

    /**
     * ColumnData subclass which works by mapping FITS-format
     * data from a file channel as a single mapped region.
     */
    private static class SingleMappedColumnData extends MappedColumnData {

        private ByteBuffer buf_;

        /**
         * Constructor.
         *
         * @param   info  column metadata
         * @param   typeBytes  number of bytes per scalar element
         * @param   itemShape  dimensions of column cells
         * @param   nrow       number of entries in the column
         * @param   chan       file channel to map data from
         * @param   pos        offset into file of start of column
         * @param   reader     object for reading data from byte buffer
         */
        SingleMappedColumnData( ColumnInfo info, int typeBytes,
                                int[] itemShape, long nrow, FileChannel chan,
                                long pos, ValueReader reader )
                throws IOException {
            super( info, typeBytes, itemShape, nrow, chan, pos, reader );
            assert itemBytes_ * nrow_ <= MAX_SECTION_BYTES;
        }

        /**
         * Returns the buffer which maps the data for this column.
         * It is mapped lazily.
         *
         * @return   mapped data of column
         */
        private ByteBuffer getBuffer() throws IOException {
            if ( buf_ == null ) {
                buf_ = mapBuffer( 0, (int) (nrow_ * itemBytes_) );
                logger_.config( "Mapping column " + getColumnInfo() );
            }
            return buf_;
        }

        public synchronized Object readValue( long irow ) throws IOException {
            return readValue( getBuffer(),
                              Tables.checkedLongToInt( irow ) * itemBytes_ );
        }
    }

    /**
     * ColumnData subclass which works by mapping FITS-format data from 
     * a file channel as a number of mapped regions.
     */
    private static class SectionsMappedColumnData extends MappedColumnData {

        private final int nsec_;
        private final int secRows_;
        private final int secBytes_;
        private final ByteBuffer[] bufs_;

        /**
         * Constructor.
         *
         * @param   info  column metadata
         * @param   typeBytes  number of bytes per scalar element
         * @param   itemShape  dimensions of column cells
         * @param   nrow       number of entries in the column
         * @param   chan       file channel to map data from
         * @param   pos        offset into file of start of column
         * @param   nsec       number of sections to use
         * @param   reader     object for reading data from byte buffer
         */
        SectionsMappedColumnData( ColumnInfo info, int typeBytes,
                                  int[] itemShape, long nrow, FileChannel chan,
                                  long pos, int nsec, ValueReader reader ) {
            super( info, typeBytes, itemShape, nrow, chan, pos, reader );
            nsec_ = nsec;
            secRows_ = toInt( (long) ( nrow_ + ( nsec_ - 1 ) ) / (long) nsec_ );
            secBytes_ = toInt( (long) secRows_ * (long) itemBytes_ );
            bufs_ = new ByteBuffer[ nsec_ ];
        }

        /**
         * Returns the buffer used for one of the sections of this column.
         * It is mapped lazily.
         *
         * @param  isec  section index
         * @return   mapped data of section
         */
        private ByteBuffer getBuffer( int isec ) throws IOException {
            if ( bufs_[ isec ] == null ) {
                long offset = (long) isec * (long) secBytes_;
                int leng = toInt( Math.min( nrow_ * (long) itemBytes_ - offset,
                                            (long) secBytes_ ) );
                bufs_[ isec ] = mapBuffer( offset, leng );
                logger_.config( "Mapping column region " + ( isec + 1 ) + "/"
                              + nsec_ + " of " + getColumnInfo() );
            }
            return bufs_[ isec ];
        }

        public synchronized Object readValue( long lrow ) throws IOException {
            if ( lrow < nrow_ ) {
                int isec = (int) ( lrow / secRows_ );
                int ioff = (int) ( ( lrow % secRows_ ) * itemBytes_ );
                return readValue( getBuffer( isec ), ioff );
            }
            else {
                throw new IOException( "Row " + lrow + " out of range" );
            }
        }
    }
}
