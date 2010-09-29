package uk.ac.starlink.fits;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.util.RandomAccess;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RandomRowSequence;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.IOUtils;

/**
 * An implementation of the StarTable interface which uses a FITS BINTABLE
 * extension.  Reading is done into the random access data (possibly
 * a mapped file); the nom.tam.fits classes are not used.
 * <p>
 * It is safe to read cells from different threads.
 *
 * @author   Mark Taylor
 */
public abstract class BintableStarTable extends AbstractStarTable {

    private final int ncol;
    private final int nrow;
    private final ColumnInfo[] colInfos;
    private final ColumnReader[] colReaders;
    private final int rowLength;
    private final int[] colOffsets;

    /** Column aux metadata key for TNULLn cards. */
    public final static ValueInfo TNULL_INFO = new DefaultValueInfo(
        Tables.NULL_VALUE_INFO.getName(),
        Tables.NULL_VALUE_INFO.getContentClass(),
        "Bad value indicator (TNULLn card)" );

    /** Column aux metadata key for TSCALn cards. */
    public final static ValueInfo TSCAL_INFO = new DefaultValueInfo(
        "Scale",
        Double.class,
        "Multiplier for values (TSCALn card)" );

    /** Column aux metadata key for TZEROn cards. */
    public final static ValueInfo TZERO_INFO = new DefaultValueInfo(
        "Zero",
        Number.class,
        "Offset for values (TZEROn card)" );

    /** Column aux metadata key for TDISPn cards. */
    public final static ValueInfo TDISP_INFO = new DefaultValueInfo(
        "Format",
        String.class,
        "Display format in FORTRAN notation (TDISPn card)" );

    /** Column aux metadata key for TBCOLn cards. */
    public final static ValueInfo TBCOL_INFO = new DefaultValueInfo(
        "Start column",
        Integer.class,
        "Start column for data (TBCOLn card)" );

    /** Column aux metadata key for TFORMn cards. */
    public final static ValueInfo TFORM_INFO = new DefaultValueInfo(
        "Format code",
        String.class,
        "Data type code (TFORMn card)" );
    private final static List auxDataInfos = Arrays.asList( new ValueInfo[] {
        TNULL_INFO, TSCAL_INFO, TZERO_INFO, TDISP_INFO, TBCOL_INFO, TFORM_INFO,
    } );

    /** BigInteger equal to 2^63 (== Long.MAX_VALUE + 1). */
    static final BigInteger TWO63 = BigInteger.ONE.shiftLeft( 63 );

    /**
     * Constructs a StarTable from a given random access stream.
     *
     * @param  hdr  FITS header descrbing the HDU
     * @param  rstream   data stream positioned at the start of the data 
     *                   section of the HDU
     */
    public static StarTable makeRandomStarTable( Header hdr, 
                                                 final RandomAccess rstream )
            throws FitsException, IOException {
        if ( rstream instanceof CopyableRandomAccess ) {
            final CopyableRandomAccess crstream =
                ((CopyableRandomAccess) rstream).copyAccess();
            final long startPos = crstream.getFilePointer();
            return new RandomBintableStarTable( hdr, crstream ) {
                public RowSequence getRowSequence() throws IOException {
                    RandomAccess seqStream;
                    synchronized ( crstream ) {
                        crstream.seek( startPos );
                        seqStream = crstream.copyAccess();
                    }
                    return createUnsafeRowSequence( seqStream );
                }
            };
        }
        else {
            return new RandomBintableStarTable( hdr, rstream ) {
                public RowSequence getRowSequence() {
                    return new RandomRowSequence( this );
                }
            };
        }
    }

    /**
     * Constructs a sequential-only StarTable from a DataSource.
     * Reading is deferred.
     *
     * @param  hdr  FITS header descrbing the HDU
     * @param  datsrc  a data source which supplies the data stream 
     *          containing the table data
     * @param  offset  offset into the stream returned by <tt>datsrc</tt>
     *         at which the table data (not the corresponding HDU header)
     *         starts
     */
    public static StarTable makeSequentialStarTable( Header hdr, 
                                                     final DataSource datsrc,
                                                     final long offset ) 
            throws FitsException {
        final Object[] BEFORE_START = new Object[ 0 ];
        return new BintableStarTable( hdr, -1L ) {
            public RowSequence getRowSequence() throws IOException {
                InputStream istrm = datsrc.getInputStream();
                if ( ! ( istrm instanceof BufferedInputStream ) ) {
                    istrm = new BufferedInputStream( istrm );
                }
                final DataInputStream stream = new DataInputStream( istrm );
                IOUtils.skipBytes( stream, offset );
                return new RowSequence() {
                    final long nrow = getRowCount();
                    final int ncol = getColumnCount();
                    final int rowLength = getRowLength();
                    long lrow = -1L;
                    Object[] row = BEFORE_START;

                    public boolean next() throws IOException {
                        if ( lrow < nrow - 1 ) {
                            if ( row == null ) {
                                IOUtils.skipBytes( stream, (long) rowLength );
                            }
                            row = null;
                            lrow++;
                            return true;
                        }
                        else {
                            return false;
                        }
                    }

                    public Object getCell( int icol ) throws IOException {
                        return getRow()[ icol ];
                    }

                    public Object[] getRow() throws IOException {
                        if ( row == BEFORE_START ) {
                            throw new IllegalStateException(
                                "Attempted read before start of table" );
                        }
                        if ( row == null ) {
                            row = readRow( stream );
                        }
                        return row;
                    }

                    public void close() throws IOException {
                        stream.close();
                    }
                };
            }
        };
    }

    /**
     * Reads a BINTABLE extension from a stream and writes the result to
     * a table sink.
     *
     * @param   hdr  FITS header object describing the BINTABLE extension
     * @param   stream  input stream positioned at the start of the 
     *          data part of the BINTABLE extension
     * @param   sink   destination for the table
     */
    public static void streamStarTable( Header hdr, DataInput stream, 
                                        TableSink sink )
            throws FitsException, IOException {
        BintableStarTable meta = new BintableStarTable( hdr, -1L ) {
            public RowSequence getRowSequence() {
                throw new UnsupportedOperationException( "Metadata only" );
            }
        };
        sink.acceptMetadata( meta );
        long nrow = meta.getRowCount();
        for ( long i = 0; i < nrow; i++ ) {
            Object[] row = meta.readRow( stream );
            sink.acceptRow( row );
        }
        sink.endRows();
        long datasize = nrow * meta.getRowLength();
        int over = (int) ( datasize % (long) 2880 );
        if ( over > 0 ) {
            IOUtils.skipBytes( stream, (long) ( 2880 - over ) );
        }
    }

    /**
     * Constructs a table.
     *
     * @param  hdr  FITS header cards describing this HDU
     */
    BintableStarTable( Header hdr, long dataStart ) throws FitsException {
        HeaderCards cards = new HeaderCards( hdr );

        /* Check we have a BINTABLE header. */
        if ( ! cards.getStringValue( "XTENSION" ).equals( "BINTABLE" ) ) {
            throw new IllegalArgumentException( "Not a binary table header" );
        }

        /* Get Table characteristics. */
        ncol = cards.getIntValue( "TFIELDS" ).intValue();
        nrow = cards.getIntValue( "NAXIS2" ).intValue();

        /* Find heap start if available. */
        long heapStart;
        if ( dataStart >= 0 ) {
            long theap = cards.containsKey( "THEAP" )
                       ? cards.getLongValue( "THEAP" ).longValue()
                       : nrow * cards.getIntValue( "NAXIS1" ).intValue();
            heapStart = dataStart + theap;
        }
        else {
            heapStart = -1;
        }

        /* Get column characteristics. */
        colInfos = new ColumnInfo[ ncol ];
        colReaders = new ColumnReader[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            int jcol = icol + 1;
            ColumnInfo cinfo = new ColumnInfo( "col" + jcol );
            List auxdata = cinfo.getAuxData();
            colInfos[ icol ] = cinfo;

            /* Name. */
            String ttype = cards.getStringValue( "TTYPE" + jcol );
            if ( ttype != null ) {
                cinfo.setName( ttype );
            }

            /* Units. */
            String tunit = cards.getStringValue( "TUNIT" + jcol );
            if ( tunit != null ) {
                cinfo.setUnitString( tunit );
            }

            /* Format string. */
            String tdisp = cards.getStringValue( "TDISP" + jcol );
            if ( tdisp != null ) {
                auxdata.add( new DescribedValue( TDISP_INFO, tdisp ) );
            }

            /* Blank value. */
            String blankKey = "TNULL" + jcol;
            long blank;
            boolean hasBlank;
            if ( cards.containsKey( blankKey ) ) {
                blank = cards.getLongValue( blankKey ).longValue();
                hasBlank = true;
                auxdata.add( new DescribedValue( TNULL_INFO,
                                                 new Long( blank ) ) );
            }
            else {
                cinfo.setNullable( false );
                blank = 0L;
                hasBlank = false;
            }

            /* Shape. */
            int[] dims = null;
            String tdim = cards.getStringValue( "TDIM" + jcol );
            if ( tdim != null ) {
                tdim = tdim.trim();
                if ( tdim.charAt( 0 ) == '(' &&
                     tdim.charAt( tdim.length() - 1 ) == ')' ) {
                    tdim = tdim.substring( 1, tdim.length() - 1 ).trim();
                    String[] sdims = tdim.split( "," );
                    if ( sdims.length > 0 ) {
                        try {
                            dims = new int[ sdims.length ];
                            for ( int i = 0; i < sdims.length; i++ ) {
                                dims[ i ] = Integer.parseInt( sdims[ i ] );
                            }
                        }
                        catch ( NumberFormatException e ) {
                            // can't set shape
                        }
                    }
                }
            }

            /* Scaling. */
            final double scale;
            final Number zero;
            if ( cards.containsKey( "TSCAL" + jcol ) ) {
                scale = cards.getDoubleValue( "TSCAL" + jcol ).doubleValue();
                auxdata.add( new DescribedValue( TSCAL_INFO,
                                                 new Double( scale ) ) );
            }
            else {
                scale = 1.0;
            }
            if ( cards.containsKey( "TZERO" + jcol ) ) {

                /* Careful here.  For unsigned long values, the TZERO value
                 * is 9223372036854775808 == 2^63 == Long.MAX_VALUE+1,
                 * i.e. not storable in a long (out of range) or a double
                 * (loses precision).  So we need to be prepared to use
                 * arbitrary precision numbers.  Check the javadocs when
                 * manipulating these, behaviour is sometimes surprising. */
                String zstr = cards.getStringValue( "TZERO" + jcol );
                BigDecimal zbig = new BigDecimal( zstr );
                boolean zIsInt =
                    zbig.compareTo( new BigDecimal( zbig.toBigInteger() ) )
                    == 0;
                boolean zInLongRange =
                    zbig.compareTo( new BigDecimal(
                             BigInteger.valueOf( Long.MIN_VALUE ) ) ) >= 0 &&
                    zbig.compareTo( new BigDecimal(
                             BigInteger.valueOf( Long.MAX_VALUE ) ) ) <= 0;
                Object zval;
                if ( zbig.compareTo( new BigDecimal( TWO63 ) ) == 0 ) {
                    zero = TWO63;
                    zval = zstr;
                }
                else if ( zIsInt && zInLongRange ) {
                    zero = new Long( zbig.longValue() );
                    zval = zero;
                }
                else {
                    zero = new Double( zbig.doubleValue() );
                    zval = zero;
                }
                DefaultValueInfo zInfo = new DefaultValueInfo( TZERO_INFO );
                zInfo.setContentClass( zval.getClass() );
                auxdata.add( new DescribedValue( zInfo, zval ) );
            }
            else {
                zero = new Long( 0 );
            }

            /* Format code (recorded but otherwise ignored). */
            String tbcol = cards.getStringValue( "TBCOL" + jcol );
            if ( tbcol != null ) {
                int bcolval = Integer.parseInt( tbcol );
                auxdata.add( new DescribedValue( TBCOL_INFO,
                                                 new Integer( bcolval ) ) );
            }

            /* Data type. */
            String tform = cards.getStringValue( "TFORM" + jcol );
            if ( tform != null ) {
                auxdata.add( new DescribedValue( TFORM_INFO, tform ) );
            }

            /* Comment (non-standard). */
            String tcomm = cards.getStringValue( "TCOMM" + jcol );
            if ( tcomm != null ) {
                cinfo.setDescription( tcomm );
            }

            /* UCD (non-standard). */
            String tucd = cards.getStringValue( "TUCD" + jcol );
            if ( tucd != null ) {
                cinfo.setUCD( tucd );
            }

            /* Utype (non-standard). */
            String tutype = cards.getStringValue( "TUTYP" + jcol );
            if ( tutype != null ) {
                cinfo.setUtype( tutype );
            }

            /* Construct a data reader for this column. */
            ColumnReader reader;
            try {
                reader = ColumnReader
                        .createColumnReader( tform, scale, zero, hasBlank,
                                             blank, dims, ttype, heapStart );
            }
            catch ( FitsException e ) {
                throw (FitsException)
                      new FitsException( "Error parsing header line TFORM"
                                         + jcol + " = " + tform )
                     .initCause( e );
            }

            /* Adjust nullability of strings - they can always be 
             * null, since an empty string (all spaces) is interpreted
             * as null. */
            if ( reader.getContentClass().equals( String.class ) ) {
                cinfo.setNullable( true );
            }

            /* Do additional column info configuration as directed 
             * by the reader. */
            cinfo.setContentClass( reader.getContentClass() );
            cinfo.setShape( reader.getShape() );
            cinfo.setElementSize( reader.getElementSize() );
            colReaders[ icol ] = reader;
        }

        /* Calculate offsets so we know where to look for each cell. */
        int leng = 0;
        colOffsets = new int[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            colOffsets[ icol ] = leng;
            leng += colReaders[ icol ].getLength();
        }
        rowLength = leng;
        int nax1 = cards.getIntValue( "NAXIS1" ).intValue();
        if ( rowLength != nax1 ) {
            throw new FitsException( "Got wrong row length: " + nax1 + 
                                     " != " + rowLength );
        }

        /* Get table name. */
        if ( cards.containsKey( "EXTNAME" ) ) {
            String tname = cards.getStringValue( "EXTNAME" );
            if ( cards.containsKey( "EXTVER" ) ) {
                tname += "-" + cards.getStringValue( "EXTVER" );
            }
            setName( tname );
        }

        /* Any unused header cards become table parameters. */
        getParameters().addAll( Arrays.asList( cards.getUnusedParams() ) );
    }

    public long getRowCount() {
        return (long) nrow;
    }

    public int getColumnCount() {
        return ncol;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colInfos[ icol ];
    }

    public List getColumnAuxDataInfos() {
        return auxDataInfos;
    }

    /**
     * Reads a cell from a given column from the current position in 
     * a stream.
     *
     * @param  icol  the column index corresponding to the cell to be read
     * @param  stream  a stream containing the byte data, positioned to
     *                 the right place
     */
    public Object readCell( DataInput stream, int icol ) throws IOException {
        return colReaders[ icol ].readValue( stream );
    }

    /**
     * Reads a whole row of the table from the current position in a stream,
     * returning a new Object[] array.
     *
     * @param  stream a stream containing the byte data, positioned to
     *                the right place
     * @return  <tt>ncol</tt>-element array of cells for this row
     */
    public Object[] readRow( DataInput stream ) throws IOException {
        Object[] row = new Object[ ncol ];
        readRow( stream, row );
        return row;
    }

    /**
     * Reads a whole row of the table into an existing array.
     *
     * @param  stream a stream containing the byte data, positioned to
     *                the right place
     * @param  row  <tt>ncol</tt>-element array, filled with row data
     *              on completion
     */
    public void readRow( DataInput stream, Object[] row ) throws IOException {
        for ( int icol = 0; icol < ncol; icol++ ) {
            row[ icol ] = colReaders[ icol ].readValue( stream );
        }
    }

    /**
     * Returns the number of bytes occupied in the data stream by a single
     * row of the table.  This is equal to the sum of the column offsets array.
     *
     * @return  row length in bytes
     */
    protected int getRowLength() {
        return rowLength;
    }

    /**
     * Returns the array of byte offsets from the start of the row at 
     * which each column starts.
     * 
     * @return  <tt>ncol</tt>-element array of byte offsets
     */
    protected int[] getColumnOffsets() {
        return colOffsets;
    }

    /**
     * Partial implementation of random-access BintableStarTable.
     * Only the getRowSequence method remains to be implemented.
     */
    private static abstract class RandomBintableStarTable
            extends BintableStarTable {
        private final RandomAccess rstream_;
        private final long dataStart_;
        private final long rowLength_;
        private final int[] colOffsets_;
        private final int ncol_;

        /**
         * Constructor.
         *
         * @param  hdr  FITS header descrbing the HDU
         * @param  rstream   data stream positioned at the start of the data 
         *                   section of the HDU
         */
        RandomBintableStarTable( Header hdr, RandomAccess rstream )
                throws FitsException {
            super( hdr, rstream.getFilePointer() );
            rstream_ = rstream;
            dataStart_ = rstream.getFilePointer();
            rowLength_ = getRowLength();
            colOffsets_ = getColumnOffsets();
            ncol_ = getColumnCount();
        }

        public boolean isRandom() {
            return true;
        }

        public Object getCell( long lrow, int icol ) throws IOException {
            synchronized ( rstream_ ) {
                rstream_.seek( dataStart_ + lrow * rowLength_
                                          + colOffsets_[ icol ] );
                return readCell( rstream_, icol );
            }
        }

        public Object[] getRow( long lrow ) throws IOException {
            synchronized ( rstream_ ) {
                rstream_.seek( dataStart_ + lrow * rowLength_ );
                return readRow( rstream_ );
            } 
        }

        /**
         * Returns a new RowSequence for this table based on a stream
         * which will be used without synchronization.
         * So, no other thread or code should use the supplied
         * <code>seqStream</code> during the lifetime of the returned
         * sequence.
         *
         * @param  seqStream  stream for exclusive use of the returned sequence
         * @return  row sequence
         */
        RowSequence createUnsafeRowSequence( final RandomAccess seqStream ) {
            final long startPos = seqStream.getFilePointer();
            final long endPos = startPos + getRowCount() * rowLength_;
            return new RowSequence() {
                long pos = startPos - rowLength_;
                public boolean next() {
                    pos += rowLength_;
                    return pos < endPos;
                }
                public Object getCell( int icol ) throws IOException {
                    if ( pos >= startPos && pos < endPos ) {
                        seqStream.seek( pos + colOffsets_[ icol ] );
                        return readCell( seqStream, icol );
                    }
                    else {
                        throw new IllegalStateException();
                    }
                }
                public Object[] getRow() throws IOException {
                    if ( pos >= startPos && pos < endPos ) {
                        seqStream.seek( pos );
                        return readRow( seqStream );
                    }
                    else {
                        throw new IllegalStateException();
                    }
                }
                public void close() {
                }
            };
        }
    }
}
