package uk.ac.starlink.fits;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.logging.Logger;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.Loader;

/**             
 * StarTable based on a single-row FITS BINTABLE which contains the
 * data for an entire column in each cell of the table.
 * The BINTABLE must be the first extension of a FITS file.
 *
 * <p>Some instances of this class hang on to file descriptors.
 * If you are in danger of running out of that resource before
 * insstances are garbage collected, you can call the {@link #close}
 * method to release them.  Attempting to read data following
 * such a call may result in an exception.
 *
 * @author   Mark Taylor
 * @since    21 Jun 2006
 */
public class ColFitsStarTable extends AbstractStarTable {

    private final int ncol_;
    private final long nrow_;
    private final ValueReader[] valReaders_;
    private final InputFactory[] inputFacts_;
    private final boolean isRandom_;
    private final ColumnReader[] randomColReaders_;
    private final Closeable closer_;

    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.fits" );

    /**
     * Constructor.
     *
     * @param   datsrc  data source containing the FITS data
     * @param   hdr   header of the HDU containing the table
     * @param   dataPos  offset into <code>file</code> of the start of the
     *          data part of the HDU
     * @param   force  true to make a table if we possibly can,
     *                 false to reject if it doesn't look very much like one
     * @param   wide  convention for representing extended columns;
     *                use null to avoid use of extended columns
     */
    public ColFitsStarTable( DataSource datsrc, FitsHeader hdr, long dataPos,
                             boolean force, WideFits wide )
            throws IOException {

        /* Check it's a BINTABLE. */
        if ( ! "BINTABLE".equals( hdr.getStringValue( "XTENSION" ) ) ) {
            throw new TableFormatException( "HDU 1 not BINTABLE" );
        }

        /* Check it has exactly one row. */
        if ( hdr.getRequiredIntValue( "NAXIS2" ) != 1 ) {
            throw new TableFormatException( "Doesn't have exactly one row" );
        }

        /* Find the number of columns. */
        int ncolStd = hdr.getRequiredIntValue( "TFIELDS" );
        ncol_ = wide == null
              ? ncolStd
              : wide.getExtendedColumnCount( hdr, ncolStd );
        boolean hasExtCol = ncol_ > ncolStd;
        if ( hasExtCol ) {
            assert wide != null;
            AbstractWideFits.logWideRead( logger_, ncolStd, ncol_ );
        }

        /* Nasty hack.
         * LDAC is a somewhat eccentric FITS variant that has a single-cell
         * BINTABLE as HDU1 containing an array of FITS header cards.
         * That looks like colfits, and causes the later HDU to be
         * ignored in format auto-detection mode, since colfits is not
         * a multi-table-capable format.  So reject this HDU explicitly here,
         * in auto-detection format it will get passed on to some other
         * format handler (basic FITS).
         * See http://marvinweb.astro.uni-bonn.de/
         *     data_products/THELIWWW/LDAC/LDAC_concepts.html */
        if ( ! force &&
             "LDAC_IMHEAD".equals( hdr.getStringValue( "EXTNAME" ) ) ) {
            throw new TableFormatException( "Reject LDAC_IMHEAD table" );
        }

        /* Read metadata for each column from the FITS header. */
        long nrow = 0;
        valReaders_ = new ValueReader[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            int jcol = icol + 1;
            BintableColumnHeader colhead =
                  hasExtCol && jcol >= ncolStd
                ? wide.createExtendedHeader( ncolStd, jcol )
                : BintableColumnHeader.createStandardHeader( jcol );
            ColumnInfo cinfo = new ColumnInfo( "col" + jcol );

            /* Format character and length. */
            String tform =
                hdr.getRequiredStringValue( colhead.getKeyName( "TFORM" ) )
                   .trim();
            char formatChar = tform.charAt( tform.length() - 1 );

            /* Use a special value if we have byte values offset by 128
             * (which allows one to represent signed bytes as unsigned ones). */
            if ( formatChar == 'B' ) {
                Number tzero =
                    hdr.getNumberValue( colhead.getKeyName( "TZERO" ) );
                Number tscale =
                    hdr.getNumberValue( colhead.getKeyName( "TSCALE" ) );
                if ( ( tzero != null && tzero.doubleValue() == -128.0 ) &&
                     ( tscale == null || tscale.doubleValue() == 1.0 ) ) {
                   formatChar = 'b';
                }
            }

            long nitem;
            try {
                nitem =
                    Long.parseLong( tform.substring( 0, tform.length() - 1 ) );
            }
            catch ( NumberFormatException e ) {
                throw new TableFormatException( "Bad "
                                              + colhead.getKeyName( "TFORM" )
                                              + " '" + tform + '"' );
            }

            /* Row count and item shape. */
            String tdims = hdr.getStringValue( colhead.getKeyName( "TDIM" ) );
            long[] dims = parseTdim( tdims );
            if ( dims == null ) {
                logger_.info( "No " + colhead.getKeyName( "TDIM" )
                            + "; assume (1," + nitem + ")" );
                dims = new long[] { 1, nitem };
            }
            if ( multiply( dims ) != nitem ) {
                throw new TableFormatException( colhead.getKeyName( "TDIM" )
                                              + " doesn't match "
                                              + colhead.getKeyName( "TFORM" ) );
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

            /* Null value. */
            Long blank = hdr.getLongValue( colhead.getKeyName( "TNULL" ) );

            /* Informational metadata. */
            String ttype = hdr.getStringValue( colhead.getKeyName( "TTYPE" ) );
            if ( ttype != null ) {
                cinfo.setName( ttype );
            }
            String tunit = hdr.getStringValue( colhead.getKeyName( "TUNIT" ) );
            if ( tunit != null ) {
                cinfo.setUnitString( tunit );
            }
            String tcomm = hdr.getStringValue( colhead.getKeyName( "TCOMM" ) );
            if ( tcomm != null ) {
                cinfo.setDescription( tcomm );
            }
            String tucd = hdr.getStringValue( colhead.getKeyName( "TUCD" ) );
            if ( tucd != null ) {
                cinfo.setUCD( tucd );
            }
            String tutype = hdr.getStringValue( colhead.getKeyName( "TUTYP" ) );
            if ( tutype != null ) {
                cinfo.setUtype( tutype );
            }

            /* Create a column reader for the current column. */
            valReaders_[ icol ] =
                createValueReader( formatChar, cinfo, itemShape, blank );
        }
        nrow_ = nrow;

        /* Prepare an InputFactory for the region of the input file
         * corresponding to each column. */
        inputFacts_ = new InputFactory[ ncol_ ];

        /* Use random access if possible. */
        boolean isFile = datsrc instanceof FileDataSource;
        if ( isFile &&
             datsrc.getCompression() == Compression.NONE &&
             ( Loader.is64Bit() ||
               ((FileDataSource) datsrc).getFile().length() < 1 << 29 ) ) {
            isRandom_ = true;

            /* Use a single file channel for all columns, though each will
             * have its own InputFactory. */
            File file = ((FileDataSource) datsrc).getFile();
            RandomAccessFile raf = new RandomAccessFile( file, "r" );
            final FileChannel chan = raf.getChannel();
            closer_ = new Closeable() {
                public void close() throws IOException {
                    chan.close();
                }
            };
            long pos = dataPos;
            for ( int icol = 0; icol < ncol_; icol++ ) {
                final long offset = pos;
                final long leng = valReaders_[ icol ].getItemBytes() * nrow_;
                pos += leng;
                String logName =
                    file.getName() + ":col" + ( icol + 1 ) + "/" + ncol_;
                inputFacts_[ icol ] =
                    createInputFactory( chan, offset, leng, logName );
            }
            if ( pos > chan.size() ) {
                logger_.warning( "HDU too short for data"
                               + ": FITS file corrupted/truncated" );
            }
        }

        /* Otherwise use a different stream for each column. */
        else {
            if ( isFile && datsrc.getCompression() != Compression.NONE ) {
                logger_.warning( "Can't map compressed file " + datsrc.getName()
                               + " - uncompressing may improve performance" );
            }
            isRandom_ = false;
            long pos = dataPos;
            for ( int icol = 0; icol < ncol_; icol++ ) {
                final long offset = pos;
                final long leng = valReaders_[ icol ].getItemBytes() * nrow_;
                pos += leng;
                inputFacts_[ icol ] =
                    InputFactory
                   .createSequentialFactory( datsrc, offset, leng );
            }
            closer_ = new Closeable() {
                public void close() {
                }
            };
        }

        /* Get table name. */
        String tname = hdr.getStringValue( "EXTNAME" );
        if ( tname != null ) {
            String extver = hdr.getStringValue( "EXTVER" );
            if ( extver != null ) {
                tname += "-" + extver;
            }
            setName( tname );
        }

        /* Add table params containing header card information. */
        getParameters().addAll( Arrays.asList( hdr.getUnusedParams() ) );

        /* Prepare readers for random access. */
        if ( isRandom_ ) {
            randomColReaders_ = new ColumnReader[ ncol_ ];
            for ( int icol = 0; icol < ncol_; icol++ ) {
                final BasicInputThreadLocal bitl =
                    new BasicInputThreadLocal( inputFacts_[ icol ], false );
                randomColReaders_[ icol ] =
                       new ColumnReader( valReaders_[ icol ] ) {
                    protected BasicInput getInput() {
                        return bitl.get();
                    }
                    public void close() throws IOException {
                        bitl.close();
                    }
                };
            }
        }
        else {
            randomColReaders_ = null;
        }
    }

    public int getColumnCount() {
        return ncol_;
    }

    public long getRowCount() {
        return nrow_;
    }

    public boolean isRandom() {
        return isRandom_;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return valReaders_[ icol ].getColumnInfo();
    }

    public Object getCell( long irow, int icol ) throws IOException {
        if ( randomColReaders_ != null ) {
            return randomColReaders_[ icol ].readIndexedCell( irow );
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    public Object[] getRow( long irow ) throws IOException {
        if ( randomColReaders_ != null ) {
            Object[] row = new Object[ ncol_ ];
            for ( int icol = 0; icol < ncol_; icol++ ) {
                row[ icol ] = randomColReaders_[ icol ].readIndexedCell( irow );
            }
            return row;
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    public RowSequence getRowSequence() throws IOException {
        return new ColFitsRowSequence();
    }

    public RowAccess getRowAccess() throws IOException {
        if ( isRandom_ ) {
            return new ColFitsRowAccess();
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    public void close() throws IOException {
        closer_.close();
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
     * Factory method to create ValueReader instances.
     *
     * @param  formatChar  FITS format character indicating data type
     * @param  info    column metadata object
     * @param  itemShape  dimensions of each cell in the column
     * @param  blank   null value for column, if any
     */
    private static ValueReader createValueReader( char formatChar,
                                                  ColumnInfo info,
                                                  int[] itemShape,
                                                  Number blank )
            throws IOException {
        final int itemSize = Tables.checkedLongToInt( multiply( itemShape ) );
        final int[] SCALAR = new int[ 0 ];

        /* Scalar column types. */
        if ( itemSize == 1 ) {
     
            if ( formatChar == 'L' ) { 
                info.setContentClass( Boolean.class );
                return new ValueReader( info, 1, SCALAR ) {
                    Object readValue( BasicInput in ) throws IOException {
                        switch ( in.readByte() ) {
                            case 'T':
                                return Boolean.TRUE;
                            case 'F': 
                                return Boolean.FALSE;
                            default: 
                                return null;
                        }
                    }
                };
            }

            else if ( formatChar == 'A' ) {
                info.setContentClass( Character.class );
                info.setNullable( false );
                return new ValueReader( info, 1, SCALAR ) {
                    Object readValue( BasicInput in ) throws IOException {
                        char chr = (char) ( in.readByte() & 0xff );
                        return chr == '\0'
                             ? null
                             : Character.valueOf( chr );
                    }
                };
            }

            else if ( formatChar == 'B' ) {
                info.setContentClass( Short.class );
                final boolean hasBad = blank != null;
                final byte badval = hasBad ? blank.byteValue() : (byte) 0;
                info.setNullable( hasBad );
                return new ValueReader( info, 1, SCALAR ) {
                    Object readValue( BasicInput in ) throws IOException {
                        byte val = in.readByte();
                        return ( hasBad && val == badval )
                             ? null
                             : Short.valueOf( (short) ( val & 0xff ) );
                    }
                };
            }

            else if ( formatChar == 'b' ) {
                info.setContentClass( Short.class );
                final boolean hasBad = blank != null;
                final byte badval = hasBad ? blank.byteValue() : (byte) 0;
                info.setNullable( hasBad );
                return new ValueReader( info, 1, SCALAR ) {
                    Object readValue( BasicInput in ) throws IOException {
                        byte val = in.readByte();
                        return ( hasBad && val == badval )
                             ? null
                             : Short.valueOf( (short) val );
                    }
                };
            }

            else if ( formatChar == 'I' ) {
                info.setContentClass( Short.class );
                final boolean hasBad = blank != null;
                final short badval = hasBad ? blank.shortValue() : (short) 0;
                info.setNullable( hasBad );
                return new ValueReader( info, 2, SCALAR ) {
                    Object readValue( BasicInput in ) throws IOException {
                        short val = in.readShort();
                        return ( hasBad && val == badval )
                             ? null
                             : Short.valueOf( val );
                    }
                };
            }

            else if ( formatChar == 'J' ) {
                info.setContentClass( Integer.class );
                final boolean hasBad = blank != null;
                final int badval = hasBad ? blank.intValue() : 0;
                info.setNullable( hasBad );
                return new ValueReader( info, 4, SCALAR ) {
                    Object readValue( BasicInput in ) throws IOException {
                        int val = in.readInt();
                        return ( hasBad && val == badval )
                             ? null
                             : Integer.valueOf( val );
                    }
                };
            }

            else if ( formatChar == 'K' ) {
                info.setContentClass( Long.class );
                final boolean hasBad = blank != null;
                final long badval = hasBad ? blank.longValue() : 0L;
                info.setNullable( hasBad );
                return new ValueReader( info, 8, SCALAR ) {
                    Object readValue( BasicInput in ) throws IOException {
                        long val = in.readLong();
                        return ( hasBad && val == badval )
                             ? null
                             : Long.valueOf( val );
                    }
                };
            }

            else if ( formatChar == 'E' ) {
                info.setContentClass( Float.class );
                return new ValueReader( info, 4, SCALAR ) {
                    Object readValue( BasicInput in ) throws IOException {
                        return new Float( in.readFloat() );
                    }
                };
            }

            else if ( formatChar == 'D' ) {
                info.setContentClass( Double.class );
                return new ValueReader( info, 8, SCALAR ) {
                    Object readValue( BasicInput in ) throws IOException {
                        return new Double( in.readDouble() );
                    }
                };
            }
        }

        /* Array column types. */
        else {
            info.setShape( itemShape );
            info.setNullable( false );

            if ( formatChar == 'L' ) {
                info.setContentClass( boolean[].class );
                return new ValueReader( info, 1, itemShape ) {
                    Object readValue( BasicInput in ) throws IOException {
                        boolean[] val = new boolean[ itemSize ];
                        for ( int i = 0; i < itemSize; i++ ) {
                            val[ i ] = in.readByte() == (byte) 'T';
                        }
                        return val;
                    }
                };
            }

            else if ( formatChar == 'B' ) {
                info.setContentClass( short[].class );
                return new ValueReader( info, 1, itemShape ) {
                    Object readValue( BasicInput in ) throws IOException {
                        short[] val = new short[ itemSize ];
                        for ( int i = 0; i < itemSize; i++ ) {
                            val[ i ] = (short) ( in.readByte() & 0xff );
                        }
                        return val;
                    }
                };
            }

            else if ( formatChar == 'b' ) {
                info.setContentClass( short[].class );
                return new ValueReader( info, 1, itemShape ) {
                    Object readValue( BasicInput in ) throws IOException {
                        short[] val = new short[ itemSize ];
                        for ( int i = 0; i < itemSize; i++ ) {
                            val[ i ] = (short) in.readByte();
                        }
                        return val;
                    }
                };
            }

            else if ( formatChar == 'I' ) {
                info.setContentClass( short[].class );
                return new ValueReader( info, 2, itemShape ) {
                    Object readValue( BasicInput in ) throws IOException {
                        short[] val = new short[ itemSize ];
                        for ( int i = 0; i < itemSize; i++ ) {
                            val[ i ] = in.readShort();
                        }
                        return val;
                    }
                };
            }

            else if ( formatChar == 'J' ) {
                info.setContentClass( int[].class );
                return new ValueReader( info, 4, itemShape ) {
                    Object readValue( BasicInput in ) throws IOException {
                        int[] val = new int[ itemSize ];
                        for ( int i = 0; i < itemSize; i++ ) {
                            val[ i ] = in.readInt();
                        }
                        return val;
                    }
                };
            }

            else if ( formatChar == 'K' ) {
                info.setContentClass( long[].class );
                return new ValueReader( info, 8, itemShape ) {
                    Object readValue( BasicInput in ) throws IOException {
                        long[] val = new long[ itemSize ];
                        for ( int i = 0; i < itemSize; i++ ) {
                            val[ i ] = in.readLong();
                        }
                        return val;
                    }
                };
            }

            else if ( formatChar == 'E' ) {
                info.setContentClass( float[].class );
                return new ValueReader( info, 4, itemShape ) {
                    Object readValue( BasicInput in ) throws IOException {
                        float[] val = new float[ itemSize ];
                        for ( int i = 0; i < itemSize; i++ ) {
                            val[ i ] = in.readFloat();
                        }
                        return val;
                    }
                };
            }

            else if ( formatChar == 'D' ) {
                info.setContentClass( double[].class );
                return new ValueReader( info, 8, itemShape ) {
                    Object readValue( BasicInput in ) throws IOException {
                        double[] val = new double[ itemSize ];
                        for ( int i = 0; i < itemSize; i++ ) {
                            val[ i ] = in.readDouble();
                        }
                        return val;
                    }
                };
            }

            else if ( formatChar == 'A' ) {
                final int sleng = itemShape[ 0 ];
                info.setElementSize( sleng );
                info.setNullable( true );
                if ( itemShape.length == 1 ) {
                    info.setContentClass( String.class );
                    return new ValueReader( info, sleng, SCALAR ) {
                        Object readValue( BasicInput in ) throws IOException {
                            final char[] charBuf = new char[ sleng ];
                            int iend = 0;
                            boolean end = false;
                            for ( int i = 0; i < sleng; i++ ) {
                                byte b = in.readByte();
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
                    };
                }
                else {
                    info.setContentClass( String[].class );
                    int[] sshape = new int[ itemShape.length - 1 ];
                    System.arraycopy( itemShape, 1, sshape, 0, sshape.length );
                    info.setShape( sshape );
                    final int nstring = itemSize / sleng;
                    assert nstring * sleng == itemSize;
                    return new ValueReader( info, sleng, sshape ) {
                        Object readValue( BasicInput in ) throws IOException {
                            String[] val = new String[ nstring ];
                            char[] charBuf = new char[ sleng ];
                            for ( int is = 0; is < nstring; is++ ) {
                                int iend = 0;
                                boolean end = false;
                                for ( int ic = 0; ic < sleng; ic++ ) {
                                    byte b = in.readByte();
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
                    };
                }
            }
        }

        /* Unknown. */
        throw new IOException( "Unknown TFORM character '" + formatChar + "'" );
    }

    /**
     * Returns an input factory suitable for accessing column data.
     * Closing the factory will not close the channel.
     *
     * @param  chan  file channel
     * @param  offset  offset into file of column data start
     * @param  leng   size of column data in bytes
     * @param  logName  column description suitable for logging messages
     * @return   input factory
     */
    private static InputFactory createInputFactory( FileChannel chan,
                                                    long offset, long leng,
                                                    String logName ) {
        Unmapper unmapper = Unmapper.getInstance();
        if ( leng <= BlockManager.DEFAULT_BLOCKSIZE ) {
            final BufferManager bufManager =
                new BufferManager( chan, offset, (int) leng, logName, unmapper);
            return new InputFactory() {
                public boolean isRandom() {
                    return true;
                }
                public BasicInput createInput( boolean isSeq )
                        throws IOException {
                    return new SimpleMappedInput( bufManager );
                }
                public void close() {
                    bufManager.close();
                }
            };
        }
        else {
            final BlockManager blockManager =
                new BlockManager( chan, offset, leng, logName, unmapper,
                                  BlockManager.DEFAULT_BLOCKSIZE );
            return new InputFactory() {
                public boolean isRandom() {
                    return true;
                }
                public BasicInput createInput( boolean isSeq )
                        throws IOException {
                    return BlockMappedInput.createInput( blockManager, !isSeq );
                }
                public void close() {
                    blockManager.close();
                }
            };
        }
    }

    /**
     * RowSequence implementation for this table.
     */
    private class ColFitsRowSequence implements RowSequence {
        private final ColumnReader[] seqColReaders_;
        private final long[] cursors_;
        private final Object[] lastValues_;
        private long irow_;

        /**
         * Constructor.
         */
        ColFitsRowSequence() throws IOException {
            seqColReaders_ = new ColumnReader[ ncol_ ];
            cursors_ = new long[ ncol_ ];
            for ( int icol = 0; icol < ncol_; icol++ ) {
                final InputFactory inputFact = inputFacts_[ icol ];
                seqColReaders_[ icol ] =
                        new ColumnReader( valReaders_[ icol ] ) {
                    BasicInput input_;
                    protected BasicInput getInput() throws IOException {
                        if ( input_ == null ) {
                            input_ = inputFact.createInput( true );
                        }
                        return input_;
                    }
                    public void close() throws IOException {
                        if ( input_ != null ) {
                            input_.close();
                        }
                    }
                };
                cursors_[ icol ] = -1;
            }
            lastValues_ = new Object[ ncol_ ];
            irow_ = -1;
        }

        public boolean next() {
            return ++irow_ < nrow_;
        }

        public Object getCell( int icol ) throws IOException {
            ColumnReader colReader = seqColReaders_[ icol ];
            long nskip = irow_ - cursors_[ icol ];
            if ( nskip > 0 ) {
                if ( nskip > 1 ) {
                    colReader.skipCells( nskip - 1 );
                }
                lastValues_[ icol ] = colReader.readNextCell();
                cursors_[ icol ] = irow_;
            }
            else if ( irow_ < 0 ) {
                throw new IllegalStateException();
            }
            return lastValues_[ icol ];
        }

        public Object[] getRow() throws IOException {
            Object[] row = new Object[ ncol_ ];
            for ( int icol = 0; icol < ncol_; icol++ ) {
                row[ icol ] = getCell( icol );
            }
            return row;
        }

        public void close() throws IOException {
            for ( ColumnReader colReader : seqColReaders_ ) {
                colReader.close();
            }
        }
    }

    /**
     * RowAccess implementation for this table.
     */
    private class ColFitsRowAccess implements RowAccess {
        private final ColumnReader[] colReaders_;
        private final Object[] row_;
        private long irow_;

        ColFitsRowAccess() throws IOException {
            colReaders_ = new ColumnReader[ ncol_ ];
            for ( int icol = 0; icol < ncol_; icol++ ) {
                final InputFactory inFact = inputFacts_[ icol ];
                colReaders_[ icol ] = new ColumnReader( valReaders_[ icol ] ) {
                    BasicInput input_;
                    protected BasicInput getInput() throws IOException {
                        if ( input_ == null ) {
                            input_ = inFact.createInput( false );
                        }
                        return input_;
                    }
                    public void close() throws IOException {
                        if ( input_ != null ) {
                            input_.close();
                        }
                    }
                };
            }
            row_ = new Object[ ncol_ ];
        }

        public void setRowIndex( long irow ) {
            irow_ = irow;
        }

        public Object getCell( int icol ) throws IOException {
            return colReaders_[ icol ].readIndexedCell( irow_ );
        }

        public Object[] getRow() throws IOException {
            for ( int icol = 0; icol < ncol_; icol++ ) {
                row_[ icol ] = colReaders_[ icol ].readIndexedCell( irow_ );
            }
            return row_;
        }

        public void close() throws IOException {
            for ( ColumnReader colReader : colReaders_ ) {
                colReader.close();
            }
        }
    }

    /**
     * Knows how to read data items of a particular type from a byte store.
     * Instances of this class are thread-safe.
     */
    private static abstract class ValueReader {

        private final ColumnInfo info_;
        private final int typeBytes_;
        private final int[] itemShape_;
        private final int itemBytes_;

        /**
         * Constructor.
         *
         * @param   info  column metadata
         * @param   typeBytes  number of bytes per scalar element
         * @param   itemShape  dimensions of column cells
         */
        ValueReader( ColumnInfo info, int typeBytes, int[] itemShape ) {
            info_ = info;
            typeBytes_ = typeBytes;
            itemShape_ = itemShape;
            itemBytes_ = Tables.checkedLongToInt( multiply( itemShape ) )
                       * typeBytes;
        }

        /**
         * Reads an object from a byte buffer.
         *
         * @param    in   input stream, positioned at read point
         * @throws   IOException  in case of a read error
         */
        abstract Object readValue( BasicInput in ) throws IOException;

        /**
         * Returns the number of bytes for a single cell of this type.
         *
         * @return  bytes per cell
         */
        public int getItemBytes() {
            return itemBytes_;
        }

        /**
         * Returns the column metadata associated with this reader.
         *
         * @return  column metadata
         */
        public ColumnInfo getColumnInfo() {
            return info_;
        }
    }

    /**
     * Aggregates a ValueReader and a BasicInput to read cell values for
     * a given column.
     */
    private static abstract class ColumnReader implements Closeable {
        private final ValueReader valReader_;
        private final long itemBytes_;

        /**
         * Constructor.
         *
         * @param  valueReader  understands data format
         */
        ColumnReader( ValueReader valReader ) {
            valReader_ = valReader;
            itemBytes_ = valReader.getItemBytes();
        }

        /**
         * Returns the object that provides byte data.
         *
         * @return  basic input
         */
        protected abstract BasicInput getInput() throws IOException;

        /**
         * Positions ready to read the value for a given row.
         *
         * @param  irow  row index
         */
        Object readIndexedCell( long irow ) throws IOException {
            BasicInput input = getInput();
            input.seek( irow * itemBytes_ );
            return valReader_.readValue( input );
        }

        /**
         * Reads the next cell value.
         *
         * @return   cell value
         */
        Object readNextCell() throws IOException {
            return valReader_.readValue( getInput() );
        }

        void skipCells( long nrow ) throws IOException {
            getInput().skip( itemBytes_ * nrow );
        }
    }
}
