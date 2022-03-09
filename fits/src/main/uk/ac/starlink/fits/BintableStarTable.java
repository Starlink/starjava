package uk.ac.starlink.fits;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.HealpixTableInfo;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;

/**
 * An implementation of the StarTable interface which uses a FITS BINTABLE
 * extension.
 *
 * <p>The implementation varies according to whether random or sequential-only
 * access is provided by the underlying data access.
 * A factory method is provided to create an appropriate instance.
 *
 * <p>Some instances of this class hang on to file descriptors.
 * If you are in danger of running out of that resource before
 * insstances are garbage collected, you can call the {@link #close}
 * method to release them.  Attempting to read data following
 * such a call may result in an exception.
 *
 * <p>Limited support is provided for the
 * <a href="https://healpix.sourceforge.io/data/examples/healpix_fits_specs.pdf"
 *    >HEALPix-FITS</a> convention;
 * the relevant {@link uk.ac.starlink.table.HealpixTableInfo} table parameters
 * are added, but any BAD_DATA keyword value is ignored,
 * and the 1024-element array-valued column variant of the format is not
 * understood.
 *
 * @author   Mark Taylor
 */
public abstract class BintableStarTable extends AbstractStarTable {

    private final int ncol_;
    private final long nrow_;
    private final ColumnInfo[] colInfos_;
    private final ColumnReader[] colReaders_;
    private final long rowLength_;
    private final long[] colOffsets_;

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

    /** Column aux metadata key for stringified longs offset. */
    public final static ValueInfo LONGOFF_INFO = new DefaultValueInfo(
        "LONG_OFFSET",
        String.class,
        "Offset value added when turning 64-bit integer into string; " +
        "only present for stringified long values with non-zero offset" );

    /** Known aux data infos. */
    private static final ValueInfo[] AUX_DATA_INFOS = new ValueInfo[] {
        TNULL_INFO, TSCAL_INFO, TZERO_INFO, TDISP_INFO, TBCOL_INFO, TFORM_INFO,
        LONGOFF_INFO,
    };

    /** BigInteger equal to 2^63 (== Long.MAX_VALUE + 1). */
    static final BigInteger TWO63 = BigInteger.ONE.shiftLeft( 63 );

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.fits" );

    /**
     * Constructor.
     *
     * @param   hdr   FITS header cards
     * @param   isRandom  true if the data access will be random-access,
     *                    false for sequential-only
     * @param   wide  convention for representing extended columns;
     *                use null to avoid use of extended columns
     */
    protected BintableStarTable( FitsHeader hdr, boolean isRandom,
                                 WideFits wide )
            throws IOException {

        /* Check we have a BINTABLE header. */
        if ( ! hdr.getStringValue( "XTENSION" ).equals( "BINTABLE" ) ) {
            throw new TableFormatException( "Not a binary table header" );
        }

        /* Get Table characteristics. */
        nrow_ = hdr.getRequiredLongValue( "NAXIS2" );
        int ncolStd = hdr.getRequiredIntValue( "TFIELDS" );
        ncol_ = wide != null
              ? wide.getExtendedColumnCount( hdr, ncolStd )
              : ncolStd;
        boolean hasExtCol = ncol_ > ncolStd;
        if ( hasExtCol ) {
            assert wide != null;
            AbstractWideFits.logWideRead( logger_, ncolStd, ncol_ );
        }

        /* Record heap start if available. */
        final long heapOffset;
        if ( isRandom ) {
            Long theap = hdr.getLongValue( "THEAP" );
            heapOffset =
                  theap != null ? theap.longValue()
                                : nrow_ * hdr.getRequiredLongValue( "NAXIS1" );
        }
        else {
            heapOffset = -1;
        }

        /* Get column characteristics. */
        colInfos_ = new ColumnInfo[ ncol_ ];
        colReaders_ = new ColumnReader[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            int jcol = icol + 1;
            BintableColumnHeader colhead =
                  hasExtCol && jcol >= ncolStd
                ? wide.createExtendedHeader( ncolStd, jcol )
                : BintableColumnHeader.createStandardHeader( jcol );
            ColumnInfo cinfo = new ColumnInfo( "col" + jcol );
            List<DescribedValue> auxdata = cinfo.getAuxData();
            colInfos_[ icol ] = cinfo;

            /* Name. */
            String ttype = hdr.getStringValue( colhead.getKeyName( "TTYPE" ) );
            if ( ttype != null ) {
                cinfo.setName( ttype );
            }
    
            /* Units. */
            String tunit = hdr.getStringValue( colhead.getKeyName( "TUNIT" ) );
            if ( tunit != null ) {
                cinfo.setUnitString( tunit );
            }
    
            /* Format string. */
            String tdisp = hdr.getStringValue( colhead.getKeyName( "TDISP" ) );
            if ( tdisp != null ) {
                auxdata.add( new DescribedValue( TDISP_INFO, tdisp ) );
            }
            
            /* Blank value. */
            final long blank;
            final boolean hasBlank;
            Long tnull = hdr.getLongValue( colhead.getKeyName( "TNULL" ) );
            if ( tnull != null ) {
                blank = tnull.longValue();
                hasBlank = true; 
                auxdata.add( new DescribedValue( TNULL_INFO, tnull ) );
            }
            else {
                cinfo.setNullable( false );
                blank = 0L;
                hasBlank = false;
            }
            
            /* Shape. */ 
            int[] dims = null;
            String tdim = hdr.getStringValue( colhead.getKeyName( "TDIM" ) );
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
                                dims[ i ] =
                                    Integer.parseInt( sdims[ i ].trim() );
                            }
                        }
                        catch ( NumberFormatException e ) {
                            dims = null;
                            // can't set shape
                        }
                    }
                }
            }

            /* Scaling. */
            final double scale;
            final Number zero;
            Double tScal = hdr.getDoubleValue( colhead.getKeyName( "TSCAL" ) );
            if ( tScal != null ) {
                scale = tScal.doubleValue();
                auxdata.add( new DescribedValue( TSCAL_INFO, tScal ) );
            }
            else {
                scale = 1.0;
            }
            Number tZero = hdr.getNumberValue( colhead.getKeyName( "TZERO" ) );
            if ( tZero != null ) {

                /* Careful here.  For unsigned long values, the TZERO value
                 * is 9223372036854775808 == 2^63 == Long.MAX_VALUE+1,
                 * i.e. not storable in a long (out of range) or a double
                 * (loses precision).  So we need to be prepared to use
                 * arbitrary precision numbers.  Check the javadocs when
                 * manipulating these, behaviour is sometimes surprising. */
                BigDecimal zbig = new BigDecimal( tZero.toString() );
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
                    zval = tZero;
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
                zero = Long.valueOf( 0 );
            }

            /* Format code (recorded but otherwise ignored). */
            String tbcol = hdr.getStringValue( colhead.getKeyName( "TBCOL" ) );
            if ( tbcol != null ) {
                int bcolval = Integer.parseInt( tbcol );
                auxdata.add( new DescribedValue( TBCOL_INFO,
                                                 new Integer( bcolval ) ) );
            }

            /* Data type. */
            final String tform =
                hdr.getRequiredStringValue( colhead.getKeyName( "TFORM" ) );
            auxdata.add( new DescribedValue( TFORM_INFO, tform ) );

            /* Comment (non-standard). */
            String tcomm = hdr.getStringValue( colhead.getKeyName( "TCOMM" ) );
            if ( tcomm != null ) {
                cinfo.setDescription( tcomm );
            }

            /* UCD (non-standard). */
            String tucd = hdr.getStringValue( colhead.getKeyName( "TUCD" ) );
            if ( tucd != null ) {
                cinfo.setUCD( tucd );
            }

            /* Utype (non-standard). */
            String tutype = hdr.getStringValue( colhead.getKeyName( "TUTYP" ) );
            if ( tutype != null ) {
                cinfo.setUtype( tutype );
            }

            /* Construct a data reader for this column. */
            ColumnReader reader;
            try {
                reader = ColumnReader
                        .createColumnReader( tform, scale, zero, hasBlank,
                                             blank, dims, ttype, heapOffset );
            }
            catch ( TableFormatException e ) {
                throw new TableFormatException( "Error parsing header line "
                                              + "TFORM" + jcol + " = " + tform,
                                                e );
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

            /* Don't try to set the shape for zero-element readers;
             * ValueInfo doesn't allow zero values in the dimensions array.
             * You could argue this is not correct behaviour,
             * but I'd be surprised if it inconveniences anybody. */
            if ( reader.getLength() > 0 ) {
                cinfo.setShape( reader.getShape() );
            }
            cinfo.setElementSize( reader.getElementSize() );
            if ( reader.isUnsignedByte() ) {
                cinfo.setAuxDatum( new DescribedValue( Tables.UBYTE_FLAG_INFO,
                                                       Boolean.TRUE ) );
            }
            BigInteger longOff = reader.getLongOffset();
            if ( longOff != null ) {
                assert ! BigInteger.ZERO.equals( longOff );
                cinfo.setAuxDatum( new DescribedValue( LONGOFF_INFO,
                                                       longOff.toString() ) );
            }
            colReaders_[ icol ] = reader;
        }

        /* Calculate offsets so we know where to look for each cell. */
        long leng = 0;
        colOffsets_ = new long[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            colOffsets_[ icol ] = leng;
            leng += colReaders_[ icol ].getLength();
        }

        /* Set the row length in bytes. */
        rowLength_ = hdr.getRequiredLongValue( "NAXIS1" );

        /* Check it against the sum of column lengths, unless we are
         * using extended lengths, in which case they are not guaranteed
         * to match.  In this case it would be a good idea to validate
         * using the extension container column's declared length,
         * but it's not very easy to do here. */
        if ( ! hasExtCol ) {
            if ( rowLength_ != leng ) {
                throw new TableFormatException( "Got wrong row length: "
                                              + rowLength_ + " != " + leng );
            }
        }

        /* Get table name. */
        String extname = hdr.getStringValue( "EXTNAME" );
        if ( extname != null ) {
            String tname = extname;
            String extver = hdr.getStringValue( "EXTVER" );
            if ( extver != null ) {
                tname += "-" + extver;
            }
            setName( tname );
        }

        /* Look for headers specific to the HEALPix-FITS encoding.
         * Note a MOC is not suitable, since it won't have an NSIDE. */
        if ( "HEALPIX".equals( hdr.getStringValue( "PIXTYPE" ) ) &&
             ! ( hdr.getIntValue( "MOCORDER" ) != null  ||      // MOC v1
                 hdr.getStringValue( "MOCVERS" ) != null ) ) {  // MOC v2
            HealpixTableInfo hpxInfo = null;
            try {
                hpxInfo = extractHealpixInfo( hdr, colInfos_ );
            }
            catch ( Exception e ) {
                logger_.log( Level.WARNING,
                             "HEALPix header parse failure: " + e.getMessage(),
                             e );
            }
            if ( hpxInfo != null ) {
                getParameters().addAll( Arrays.asList( hpxInfo.toParams() ) );
            }
        }

        /* Any unused header cards become table parameters. */
        getParameters().addAll( Arrays.asList( hdr.getUnusedParams() ) );
    }

    public long getRowCount() {
        return nrow_;
    }

    public int getColumnCount() {
        return ncol_;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colInfos_[ icol ];
    }

    public List<ValueInfo> getColumnAuxDataInfos() {
        return Arrays.asList( AUX_DATA_INFOS );
    }

    /**
     * Reads a cell from a given column from the current position in
     * a stream.
     *
     * @param  icol  the column index corresponding to the cell to be read
     * @param  stream  a stream containing the byte data, positioned to
     *                 the right place
     */
    protected Object readCell( BasicInput stream, int icol )
            throws IOException {
        return colReaders_[ icol ].readValue( stream );
    }

    /**
     * Reads a whole row of the table from the current position in a stream,
     * returning a new Object[] array.
     *
     * @param  stream a stream containing the byte data, positioned to
     *                the right place
     * @return  <tt>ncol</tt>-element array of cells for this row
     */
    protected Object[] readRow( BasicInput stream ) throws IOException {
        Object[] row = new Object[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            row[ icol ] = colReaders_[ icol ].readValue( stream );
        }
        return row;
    }

    /**
     * Returns the number of bytes occupied in the data stream by a single
     * row of the table.  This is equal to the sum of the column offsets array.
     *
     * @return  row length in bytes
     */
    protected long getRowLength() {
        return rowLength_;
    }

    /**
     * Returns the array of byte offsets from the start of the row at
     * which each column starts.
     *
     * @return  <tt>ncol</tt>-element array of byte offsets
     */
    protected long[] getColumnOffsets() {
        return colOffsets_;
    }

    /**
     * Returns an instance of this class given a data access instance.
     *
     * @param  hdr  FITS header cards
     * @param  inputFact  factory for access to the data part of the
     *                    HDU representing a FITS BINTABLE extension
     * @param   wide  convention for representing extended columns;
     *                use null to avoid use of extended columns
     * @return  StarTable instance; it will be random-access according to
     *                    whether the input factory is
     */
    public static BintableStarTable createTable( FitsHeader hdr,
                                                 InputFactory inputFact,
                                                 WideFits wide )
            throws IOException {
        return inputFact.isRandom()
             ? new RandomBintableStarTable( hdr, inputFact, wide )
             : new SequentialBintableStarTable( hdr, inputFact, wide );
    }

    /**
     * Reads a BINTABLE extension from a stream and writes the result to
     * a table sink.
     *
     * @param   hdr  FITS header object describing the BINTABLE extension
     * @param   input   input stream positioned at the start of the
     *                  data part of the BINTABLE extension
     * @param   wide  convention for representing extended columns;
     *                use null to avoid use of extended columns
     * @param   sink   destination for the table
     */
    public static void streamStarTable( FitsHeader hdr, BasicInput input,
                                        WideFits wide, TableSink sink )
            throws IOException {
        InputFactory dummyFact = new InputFactory() {
            public boolean isRandom() {
                return false;
            }
            public BasicInput createInput( boolean isSeq ) {
                throw new UnsupportedOperationException( "Metadata only" );
            }
            public void close() {
            }
        };
        BintableStarTable meta =
            new SequentialBintableStarTable( hdr, dummyFact, wide );
        sink.acceptMetadata( meta );
        long nrow = meta.getRowCount();
        for ( long i = 0; i < nrow; i++ ) {
            Object[] row = meta.readRow( input );
            sink.acceptRow( row );
        }
        sink.endRows();
    }

    /**
     * Investigates headers for cards specific to the HEALPix-FITS convention.
     * If it looks like the headers describe a HEALPix map,
     * a corresponding HealpixTableInfo object is returned,
     * otherwise a RuntimeException with an informative message is thrown.
     *
     * @param  hdr  header
     * @param  infos  column metadata
     * @return   healpix metadata object if the headers look appropriate
     * @throws   RuntimeException  if the headers don't look appropriate
     * @see <a
     href="https://healpix.sourceforge.io/data/examples/healpix_fits_specs.pdf"
     *         >HEALPix-FITS convention</a>
     */
    private static HealpixTableInfo extractHealpixInfo( FitsHeader hdr,
                                                        ColumnInfo[] infos ) {

        /* Get NSIDE/level value. */
        Long nSide = hdr.getLongValue( "NSIDE" );
        if ( nSide == null ) {
            throw new IllegalStateException( "No HEALPix NSIDE header" );
        }
        long nside = nSide.longValue();
        final int level = Long.numberOfTrailingZeros( nside );
        if ( 1 << level != nside ) {
            throw new IllegalStateException( "Invalid HEALPix header value "
                                           + "NSIDE=" + nside );
        }

        /* Get ordering scheme. */
        final boolean isNest;
        String ordering = hdr.getStringValue( "ORDERING" );
        if ( ordering == null ) {
            throw new IllegalStateException( "Missing HEALPix header "
                                           + "ORDERING" );
        }
        else if ( "NESTED".equals( ordering ) ) {
            isNest = true;
        }
        else if ( "RING".equals( ordering ) ) {
            isNest = false;
        }
        else {
            throw new IllegalStateException( "Unrecognised HEALPix header "
                                           + "ORDERING='" + ordering + "'" );
        }

        /* Get HEALPix index column, if any. */
        final String ipixColName;
        String indxschm = hdr.getStringValue( "INDXSCHM" );
        if ( indxschm == null ) {
            logger_.warning( "Missing nominally required HEALPix header "
                           + "INDXSCHM, assuming IMPLICIT" );
            ipixColName = null;
        }
        else if ( "IMPLICIT".equals( indxschm ) ) {
            ipixColName = null;
        }
        else if ( "EXPLICIT".equals( indxschm ) ) {
            ipixColName = infos[ 0 ].getName();
        }
        else {
            throw new IllegalStateException( "Unrecognised HEALPix header "
                                           + "INDXSCHM='" + indxschm + "'" );
        }

        /* Get coordinate system indicator, if any. */
        final HealpixTableInfo.HpxCoordSys csys;
        String coordsys = hdr.getStringValue( "COORDSYS" );
        if ( coordsys != null ) {
            if ( coordsys.trim().length() == 1 ) {
                csys = HealpixTableInfo.HpxCoordSys
                      .fromCharacter( coordsys.charAt( 0 ) );
            }
            else {
                csys = guessHealpixCoordSys( coordsys.trim() );
                if ( csys != null ) {
                    logger_.warning( "Guessing HEALPix header COORDSYS='"
                                   + coordsys + "' -> " + csys.getWord() );
                }
            }
            if ( csys == null ) {
                logger_.warning( "Unknown HEALPix header COORDSYS='"
                               + coordsys + "'" );
            }
        }
        else {
            csys = null;
        }

        /* Note that the nominally required BAD_DATA value is currently
         * ignored.  It is against the spirit of normal FITS bad value
         * handling, and would require considerable complication
         * and inefficiency to handle correctly. */

        /* Return a new metadata object. */
        return new HealpixTableInfo( level, isNest, ipixColName, csys );
    }

    /**
     * Makes a best guess at a HEALPix coordinate system given a COORDSYS
     * header value.  There is a "standard" for this, as per the
     * HealpixTableInfo.fromCharacter method, but some providers use
     * unambiguous but non-standard values, e.g. "GALACTIC" has been seen
     * in Planck Legacy Archive data.
     *
     * @param  coordsysTxt   non-null content of FITS COORDSYS header
     * @param  coordsys value, or null if nothing obvious
     */
    private static HealpixTableInfo.HpxCoordSys
            guessHealpixCoordSys( String coordsysTxt ) {
        String ctxt = coordsysTxt.toUpperCase();
        if ( ctxt.startsWith( "GAL" ) ) {
            return HealpixTableInfo.HpxCoordSys.GALACTIC;
        }
        else if ( ctxt.startsWith( "ECL" ) ) {
            return HealpixTableInfo.HpxCoordSys.ECLIPTIC;
        }
        else if ( ctxt.startsWith( "EQU" ) ||
                  ctxt.startsWith( "CEL" ) ) {
            return HealpixTableInfo.HpxCoordSys.CELESTIAL;
        }
        else {
            return null;
        }
    }

    /**
     * Sequential-only BintableStarTable concrete subclass.
     */
    private static class SequentialBintableStarTable extends BintableStarTable {
        private final InputFactory inputFact_;

        /**
         * Constructor.
         *
         * @param   hdr  FITS header object describing the BINTABLE extension
         * @param   inputFact   creates input streams positioned at the start
         *                      of thedata part of the BINTABLE extension
         * @param   wide  convention for representing extended columns;
         *                use null to avoid use of extended columns
         */
        public SequentialBintableStarTable( FitsHeader hdr,
                                            InputFactory inputFact,
                                            WideFits wide )
                throws IOException {
            super( hdr, false, wide );
            inputFact_ = inputFact;
        }

        public boolean isRandom() {
            return false;
        }

        public Object getCell( long lrow, int icol ) throws IOException {
            throw new UnsupportedOperationException();
        }

        public Object[] getRow( long lrow ) throws IOException {
            throw new UnsupportedOperationException();
        }

        public RowSequence getRowSequence() throws IOException {
            final BasicInput input = inputFact_.createInput( true );
            final Object[] beforeStart = new Object[ 0 ];
            final long nrow = getRowCount();
            final long rowLength = getRowLength();
            return new RowSequence() {
                long lrow_ = -1;
                Object[] row_ = beforeStart;
                long nskip_ = 0;

                public boolean next() throws IOException {
                    if ( lrow_ < nrow - 1 ) {
                        if ( row_ == null ) {
                            nskip_ += rowLength;
                        }
                        row_ = null;
                        lrow_++;
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
                    if ( row_ == beforeStart ) {
                        throw new IllegalStateException();
                    }
                    if ( row_ == null ) {
                        if ( nskip_ != 0 ) {
                            input.skip( nskip_ );
                            nskip_ = 0;
                        }
                        row_ = readRow( input );
                    }
                    return row_;
                }

                public void close() throws IOException {
                    if ( nskip_ != 0 ) {
                        input.skip( nskip_ );
                        nskip_ = 0;
                    }
                    input.close();
                }
            };
        }

        public void close() throws IOException {
            inputFact_.close();
        }
    }

    /**
     * Random-access BintableStarTable concrete subclass.
     */
    private static class RandomBintableStarTable extends BintableStarTable {
        private final InputFactory inputFact_;
        private final BasicInputThreadLocal randomInputThreadLocal_;
        private final long rowLength_;
        private final long[] colOffsets_;

        /**
         * Constructor.
         *
         * @param   hdr  FITS header object describing the BINTABLE extension
         * @param   inputFact   creates input streams positioned at the start
         *                      of thedata part of the BINTABLE extension;
         *                      must be random-access
         * @param   wide  convention for representing extended columns;
         *                use null to avoid use of extended columns
         */
        RandomBintableStarTable( FitsHeader hdr, InputFactory inputFact,
                                 WideFits wide )
                throws IOException {
            super( hdr, true, wide );
            inputFact_ = inputFact;
            if ( ! inputFact.isRandom() ) {
                throw new IllegalArgumentException( "not random" );
            }
            rowLength_ = getRowLength();
            colOffsets_ = getColumnOffsets();
            randomInputThreadLocal_ =
                new BasicInputThreadLocal( inputFact, false );
        }

        public boolean isRandom() {
            return true;
        }

        public Object getCell( long lrow, int icol ) throws IOException {
            BasicInput randomInput = randomInputThreadLocal_.get();
            randomInput.seek( lrow * rowLength_ + colOffsets_[ icol ] );
            return readCell( randomInput, icol );
        }

        public Object[] getRow( long lrow ) throws IOException {
            BasicInput randomInput = randomInputThreadLocal_.get();
            randomInput.seek( lrow * rowLength_ );
            return readRow( randomInput );
        }

        public RowSequence getRowSequence() throws IOException {
            final BasicInput input = inputFact_.createInput( true );
            assert input.isRandom();
            final long endPos = getRowCount() * rowLength_;
            return new RowSequence() {
                long pos = -rowLength_;
                public boolean next() {
                    pos += rowLength_;
                    return pos < endPos;
                }
                public Object getCell( int icol ) throws IOException {
                    if ( pos >= 0 && pos < endPos ) {
                        input.seek( pos + colOffsets_[ icol ] );
                        return readCell( input, icol );
                    }
                    else {
                        throw new IllegalStateException();
                    }
                }
                public Object[] getRow() throws IOException {
                    if ( pos >= 0 && pos < endPos ) {
                        input.seek( pos );
                        return readRow( input );
                    }
                    else {
                        throw new IllegalStateException();
                    }
                }
                public void close() throws IOException {
                    input.close();
                }
            };
        }

        public RowAccess getRowAccess() throws IOException {
            final BasicInput input = inputFact_.createInput( false );
            assert input.isRandom();
            return new RowAccess() {
                long irow_ = -1;
                public void setRowIndex( long irow ) {
                    irow_ = irow;
                }
                public Object getCell( int icol ) throws IOException {
                    input.seek( irow_ * rowLength_ + colOffsets_[ icol ] );
                    return readCell( input, icol );
                }
                public Object[] getRow() throws IOException {
                    input.seek( irow_ * rowLength_ );
                    return readRow( input );
                }
                public void close() throws IOException {
                    input.close();
                }
            };
        }

        public void close() throws IOException {
            randomInputThreadLocal_.close();
            inputFact_.close();
        }
    }
}
