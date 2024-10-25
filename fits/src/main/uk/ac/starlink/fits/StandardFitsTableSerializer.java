package uk.ac.starlink.fits;

import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.CountCheckRowSequence;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.HealpixTableInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.Tables;

/**
 * Class which knows how to do the various bits of serializing a StarTable
 * to FITS BINTABLE format.  A normal (row-oriented) organisation of the
 * data is used.  
 * Array-valued columns are all written as fixed size arrays.
 * This class does the hard work for FitsTableWriter.
 *
 * <p>When writing tables that are marked up using the headers defined in
 * {@link uk.ac.starlink.table.HealpixTableInfo},
 * this serializer will attempt to insert FITS headers corresponding
 * to the HEALPix-FITS convention.
 *
 * @author   Mark Taylor (Starlink)
 * @see
 * <a href="https://healpix.sourceforge.io/data/examples/healpix_fits_specs.pdf"
 *    >HEALPix-FITS convention</a>
 */
public class StandardFitsTableSerializer implements FitsTableSerializer {

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.fits" );

    private final FitsTableSerializerConfig config_;
    private StarTable table_;
    private ColumnWriter[] colWriters_;
    private ColumnInfo[] colInfos_;
    private long rowCount_;

    /**
     * Package-private constructor intended for use by subclasses.
     * The {@link #init} method must be called after this constructor
     * is invoked.
     *
     * @param  config  configuration
     */
    StandardFitsTableSerializer( FitsTableSerializerConfig config ) {
        config_ = config;
    }

    /**
     * Constructor.
     *
     * @param  config  configuration
     * @param  table   table to serialize
     * @throws IOException if it won't be possible to write the given table
     */
    @SuppressWarnings("this-escape")
    public StandardFitsTableSerializer( FitsTableSerializerConfig config,
                                        StarTable table )
            throws IOException {
        this( config );
        init( table );
    }

    /**
     * Returns the configuration information for this serializer.
     *
     * @return  config
     */
    public FitsTableSerializerConfig getConfig() {
        return config_;
    }

    /**
     * Configures this serializer for use with a given table and column writer
     * factory.  Should be called before this object is ready for use;
     * in a constructor would be a good place.
     * Calls {@link #createColumnWriter}.
     *
     * @param  table  table to be written
     * @throws IOException if it won't be possible to write the given table,
     *                       for instance if it has too many columns
     */
    final void init( StarTable table ) throws IOException {
        if ( table_ != null ) {
            throw new IllegalStateException( "Table already initialised" );
        }
        table_ = table;

        /* Get table dimensions (though we may need to calculate the row
         * count directly later. */
        int ncol = table.getColumnCount();
        long nrow = table.getRowCount();

        /* Store column infos. */
        colInfos_ = Tables.getColumnInfos( table );

        /* Work out column shapes, and check if any are unknown (variable
         * last dimension). */
        boolean hasVarShapes = false;
        boolean checkForNullableInts = false;
        int[][] shapes = new int[ ncol ][];
        int[] maxChars = new int[ ncol ];
        int[] maxElements = new int[ ncol ];
        long[] totalElements = new long[ ncol ];
        boolean[] useCols = new boolean[ ncol ];
        boolean[] varShapes = new boolean[ ncol ];
        boolean[] varChars = new boolean[ ncol ];
        boolean[] varElementChars = new boolean[ ncol ];
        boolean[] mayHaveNullableInts = new boolean[ ncol ];
        Arrays.fill( useCols, true );
        boolean[] hasNulls = new boolean[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo colinfo = colInfos_[ icol ];
            Class<?> clazz = colinfo.getContentClass();
            if ( clazz.isArray() ) {
                shapes[ icol ] = colinfo.getShape().clone();
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
                mayHaveNullableInts[ icol ] = true;

                /* Only set the flag which forces a first pass if we need
                 * to work out whether nulls actually exist.  If the
                 * aux datum giving a null value exists we will use it in
                 * any case, so finding out whether there are in fact null
                 * values by scanning the data is not necessary. */
                if ( colinfo.getAuxDatumValue( Tables.NULL_VALUE_INFO,
                                               Number.class ) != null ) {
                    hasNulls[ icol ] = true;
                }
                else {
                    checkForNullableInts = true;
                }
            }
        }

        /* If necessary, make a first pass through the table data to
         * find out the maximum size of variable length fields and the length
         * of the table. */
        if ( hasVarShapes || checkForNullableInts || nrow < 0 ) {
            StringBuffer sbuf = new StringBuffer( "First pass needed: " );
            if ( hasVarShapes ) {
                sbuf.append( "(variable array shapes) " );
            }
            if ( checkForNullableInts ) {
                sbuf.append( "(nullable ints) " );
            }
            if ( nrow < 0 ) {
                sbuf.append( "(unknown row count) " );
            }
            logger.config( sbuf.toString() );
            long drow = nrow;
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
                               ( mayHaveNullableInts[ icol ] && 
                                 ! hasNulls[ icol ] ) ) ) {
                            Object cell = rseq.getCell( icol );
                            if ( cell == null ) {
                                if ( mayHaveNullableInts[ icol ] ) {
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
                                        String sv = svals[ i ];
                                        if ( sv != null ) {
                                            maxChars[ icol ] =
                                                Math.max( maxChars[ icol ],
                                                          sv.length() );
                                        }
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

            /* Warn in case of row count mismatch.  In this case, the
             * rows actually present in the RowSequence will be written.
             * If this count has not been performed, the discrepancy will
             * be reported and corrected later. */
            if ( drow >= 0 && drow != nrow ) {
                logger.warning( "Row count discrepancy in FITS output: "
                              + drow + " rows declared, "
                              + nrow + " rows found/written." );
            }

            /* In the case of variable string lengths and no non-null data
             * in any of the cells, maxChars could still be set negative.
             * Fix that here. */
            for ( int icol = 0; icol < ncol; icol++ ) {
                if ( maxChars[ icol ] < 0 ) {
                    maxChars[ icol ] = 0;
                }
            }

            /* If required, make sure that all output strings have a length
             * of at least 1. */
            if ( ! config_.allowZeroLengthString() ) {
                for ( int icol = 0; icol < ncol; icol++ ) {
                    if ( maxChars[ icol ] == 0 ) {
                        maxChars[ icol ] = 1;
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

        /* Store the row count, which we must have got by now. */
        assert nrow >= 0;
        rowCount_ = nrow;

        /* We now have all the information we need about the table.
         * Construct and store a custom writer for each column which 
         * knows about the characteristics of the column and how to 
         * write values to the stream.  For columns which can't be 
         * written in FITS format store a null in the writers array
         * and log a message. */
        colWriters_ = new ColumnWriter[ ncol ];
        int rbytes = 0;
        int nUseCol = 0;
        for ( int icol = 0; icol < ncol; icol++ ) {
            if ( useCols[ icol ] ) {
                ColumnInfo cinfo = colInfos_[ icol ];
                ColumnWriter writer =
                    createColumnWriter( cinfo, shapes[ icol ],
                                        varShapes[ icol ], maxChars[ icol ],
                                        maxElements[ icol ],
                                        totalElements[ icol ],
                                        mayHaveNullableInts[ icol ] 
                                        && hasNulls[ icol ] );
                if ( writer == null ) {
                    logger.warning( "Ignoring column " + cinfo.getName() +
                                    " - don't know how to write to FITS" );
                }
                else {
                    nUseCol++;
                }
                colWriters_[ icol ] = writer;
            }
        }

        /* Check column count is permissible. */
        FitsUtil.checkColumnCount( config_.getWide(), nUseCol );
    }

    /**
     * Returns the array of column writers used by this serializer.
     * The list is generated once by the sole call of the 
     * <code>init</code> method.
     *
     * @return  column writer array
     */
    ColumnWriter[] getColumnWriters() {
        return colWriters_;
    }

    public CardImage[] getHeader() {

        /* Work out the dimensions in columns and bytes of the table. */
        int rowLength = 0;
        int extLength = 0;
        int nUseCol = 0;
        int ncol = table_.getColumnCount();
        WideFits wide = config_.getWide();
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnWriter writer = colWriters_[ icol ];
            if ( writer != null ) {
                nUseCol++;
                int leng = writer.getLength();
                rowLength += leng;
                if ( wide != null &&
                     nUseCol >= wide.getContainerColumnIndex() ) {
                    extLength += leng;
                }
            }
        }

        /* Work out the number of standard and extended columns.
         * We know it is actually possible to write the given number of
         * columns using the current wide value, because of a check carried
         * out during initialisation. */
        int nStdCol = wide != null && nUseCol > wide.getContainerColumnIndex()
                    ? wide.getContainerColumnIndex()
                    : nUseCol;
        boolean hasExtCol = nUseCol > nStdCol;

        /* Add HDU layout metadata. */
        CardFactory cfact = CardFactory.DEFAULT;
        List<CardImage> cards = new ArrayList<>();
        cards.addAll( Arrays.asList( new CardImage[] {
            cfact.createStringCard( "XTENSION", "BINTABLE",
                                    "binary table extension" ),
            cfact.createIntegerCard( "BITPIX", 8, "8-bit bytes" ),
            cfact.createIntegerCard( "NAXIS", 2, "2-dimensional table" ),
            cfact.createIntegerCard( "NAXIS1", rowLength,
                                     "width of table in bytes" ),
            cfact.createIntegerCard( "NAXIS2", rowCount_,
                                     "number of rows in table" ),
            cfact.createIntegerCard( "PCOUNT", 0, "size of special data area" ),
            cfact.createIntegerCard( "GCOUNT", 1, "one data group" ),
            cfact.createIntegerCard( "TFIELDS", nStdCol, "number of columns" ),
        } ) );

        /* Add EXTNAME record containing table name. */
        String tname = table_.getName();
        if ( tname != null && tname.trim().length() > 0 ) {
            cards.add( cfact
                      .createStringCard( "EXTNAME", tname, "table name" ) );
        }

        /* Add extended column header information if applicable. */
        if ( hasExtCol ) {
            cards.addAll( Arrays.asList( wide.getExtensionCards( nUseCol ) ) );
            AbstractWideFits.logWideWrite( logger, nStdCol, nUseCol );
        }

        /* If the table is explicitly annotated as a HEALPix map,
         * add HEALPix-specific headers according to the HEALPix-FITS
         * convention. */
        List<DescribedValue> tparams = table_.getParameters();
        if ( HealpixTableInfo.isHealpix( tparams ) ) {
            HealpixTableInfo hpxInfo = HealpixTableInfo.fromParams( tparams );
            CardImage[] hpxCards = null;
            try {
                hpxCards = getHealpixHeaders( hpxInfo );
            }
            catch ( TableFormatException e ) {
                logger.log( Level.WARNING,
                            "Failed to write HEALPix-specific FITS headers: "
                            + e.getMessage(), e );
            }
            if ( hpxCards != null && hpxCards.length > 0 ) {
                logger.info( "Adding HEALPix-specific FITS headers" );
                cards.addAll( Arrays.asList( hpxCards ) );
            }
        }

        /* Add HDU metadata describing columns. */
        int jcol = 0;
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnWriter colwriter = colWriters_[ icol ];
            if ( colwriter != null ) {
                jcol++;
                if ( hasExtCol && jcol == nStdCol ) {
                    cards.addAll( Arrays.asList(
                        wide.getContainerColumnCards( extLength, 0 ) ) );
                }
                BintableColumnHeader colhead =
                      hasExtCol && jcol >= nStdCol
                    ? wide.createExtendedHeader( nStdCol, jcol )
                    : BintableColumnHeader.createStandardHeader( jcol );
                cards.addAll( Arrays.asList( getHeaders( colhead,
                                                         colInfos_[ icol ],
                                                         colwriter, jcol ) ) );
            }
        }
        return cards.toArray( new CardImage[ 0 ] );
    }

    /**
     * Returns header information for a given column.
     *
     * @param  colhead   header key handler for column to write
     * @param  colinfo   column metadata
     * @param  colwriter   column writer
     * @param  column index; first column is 1
     * @return  header cards
     */
    private CardImage[] getHeaders( BintableColumnHeader colhead,
                                    ColumnInfo colinfo, ColumnWriter colwriter,
                                    int jcol ) {
        CardFactory cfact = colhead.getCardFactory();
        String forcol = " for column " + jcol;
        List<CardImage> cards = new ArrayList<>();

        /* Name. */
        String name = colinfo.getName();
        if ( name != null && name.trim().length() > 0 ) {
            cards.add( cfact.createStringCard( colhead.getKeyName( "TTYPE" ), 
                                               name, "label" + forcol ) );
        }

        /* Format. */
        String form = colwriter.getFormat();
        cards.add( cfact.createStringCard( colhead.getKeyName( "TFORM" ), form,
                                           "format" + forcol ) );

        /* Units. */
        String unit = colinfo.getUnitString();
        if ( unit != null && unit.trim().length() > 0 ) {
            cards.add( cfact.createStringCard( colhead.getKeyName( "TUNIT" ),
                                               unit, "units" + forcol ) );
        }

        /* Blank. */
        Number bad = colwriter.getBadNumber();
        if ( bad != null ) {
            cards.add( cfact.createIntegerCard( colhead.getKeyName( "TNULL" ),
                                                bad.longValue(),
                                                "blank value" + forcol ) );
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
            cards.add( cfact.createStringCard( colhead.getKeyName( "TDIM" ),
                                               sbuf.toString(),
                                               "dimensions" + forcol ) );
        }

        /* Scaling. */
        BigDecimal zero = colwriter.getZero();
        double scale = colwriter.getScale();
        if ( zero != null && ! BigDecimal.ZERO.equals( zero ) ) {
            cards.add( cfact.createLiteralCard( colhead.getKeyName( "TZERO" ),
                                                zero.toString(),
                                                "base" + forcol ) );
        }
        if ( scale != 1.0 ) {
            cards.add( cfact.createRealCard( colhead.getKeyName( "TSCALE" ),
                                             scale, "factor" + forcol ) );
        }

        /* Comment (non-standard). */
        String comm = colinfo.getDescription();
        if ( comm != null && comm.trim().length() > 0 ) {
            cards.add( cfact.createStringCard( colhead.getKeyName( "TCOMM" ),
                                               comm, null ) );
        }

        /* UCD (non-standard). */
        String ucd = colinfo.getUCD();
        if ( ucd != null && ucd.trim().length() > 0 ) {
            cards.add( cfact
                      .createStringCard( colhead.getKeyName( "TUCD" ), ucd,
                                         "VO Unified Content Descriptor"
                                       + forcol ) );
        }

        /* Utype (non-standard). */
        String utype = colinfo.getUtype();
        if ( utype != null && utype.trim().length() > 0 ) {
            cards.add( cfact.createStringCard( colhead.getKeyName( "TUTYP" ),
                                               utype, "VO Utype" + forcol ) );
        }

        /* Xtype (non-standard). */
        String xtype = colinfo.getXtype();
        if ( xtype != null && xtype.trim().length() > 0 ) {
            cards.add( cfact
                      .createStringCard( colhead.getKeyName( "TXTYP" ), xtype,
                                         "VO/DALI extended type" + forcol ) );
        }
        return cards.toArray( new CardImage[ 0 ] );
    }

    public void writeData( DataOutput strm ) throws IOException {
        long nWritten = writeDataOnly( strm );

        /* Write padding. */
        int extra = (int) ( nWritten % (long) 2880 );
        if ( extra > 0 ) {
            strm.write( new byte[ 2880 - extra ] );
        }
    }

    /**
     * Writes the table data content without any trailing padding.
     *
     * @param  strm  destination stream
     * @return   number of bytes written
     */
    public long writeDataOnly( DataOutput strm ) throws IOException {

        /* Work out the length of each row in bytes. */
        int rowBytes = 0;
        int ncol = table_.getColumnCount();
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnWriter writer = colWriters_[ icol ];
            if ( writer != null ) {
                rowBytes += writer.getLength();
            }
        }

        /* Write the data cells, delegating the item in each column to
         * the writer that knows how to handle it. */
        long nWritten = 0L;
        RowSequence rseq = CountCheckRowSequence
                          .getSafeRowSequence( table_.getRowSequence(),
                                               table_.getColumnCount(),
                                               rowCount_ );
        try {
            while ( rseq.next() ) {
                Object[] row = rseq.getRow();
                for ( int icol = 0; icol < ncol; icol++ ) {
                    ColumnWriter writer = colWriters_[ icol ];
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
        return nWritten;
    }

    public char getFormatChar( int icol ) {
        if ( colWriters_[ icol ] == null ) {
            return (char) 0;
        }
        else {
            return colWriters_[ icol ].getFormatChar();
        }
    }

    public int[] getDimensions( int icol ) {
        if ( colWriters_[ icol ] == null ) {
            return null;
        }
        else {
            int[] dims = colWriters_[ icol ].getDims();
            return dims == null ? new int[ 0 ] : dims;
        }
    }

    public String getBadValue( int icol ) {
        if ( colWriters_[ icol ] == null ) {
            return null;
        }
        else {
            Number badnum = colWriters_[ icol ].getBadNumber();
            return badnum == null ? null : badnum.toString();
        }
    }

    public long getRowCount() {
        return rowCount_;
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
     * @return  a suitable column writer, or <code>null</code> if we don't
     *          know how to write this to FITS
     */
    ColumnWriter createColumnWriter( ColumnInfo cinfo, int[] shape,
                                     boolean varShape, int eSize,
                                     final int maxEls, long totalEls,
                                     boolean nullableInt ) {
        Class<?> clazz = cinfo.getContentClass();
        BigInteger longOffset = ScalarColumnWriter.getLongOffset( cinfo );
        if ( clazz == String.class && longOffset == null ) {
            final int maxChars = eSize;
            final int[] dims = new int[] { maxChars };
            final byte[] buf = new byte[ maxChars ];
            final byte[] blankBuf = new byte[ maxChars ];
            final byte padByte = config_.getPadCharacter();
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
                public BigDecimal getZero() {
                    return BigDecimal.ZERO;
                }
                public double getScale() {
                    return 1.0;
                }
                public Number getBadNumber() {
                    return null;
                }
            };
        }
        else if ( clazz == String[].class && longOffset == null ) {
            final int maxChars = eSize;
            final int[] charDims = new int[ shape.length + 1 ];
            charDims[ 0 ] = maxChars;
            System.arraycopy( shape, 0, charDims, 1, shape.length );
            final byte[] buf = new byte[ maxChars ];
            final byte padByte = config_.getPadCharacter();
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
                public BigDecimal getZero() {
                    return BigDecimal.ZERO;
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
            boolean allowSignedByte = config_.allowSignedByte();
            byte padChar = config_.getPadCharacter();
            ColumnWriter cw = ScalarColumnWriter
                             .createColumnWriter( cinfo, nullableInt,
                                                  allowSignedByte, padChar );
            if ( cw != null ) {
                return cw;
            }
            else {
                ArrayWriter aw =
                    ArrayWriter.createArrayWriter( cinfo, allowSignedByte );
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
     * Returns FITS headers specific for a table containing a HEALPix map.
     * If this method is called the assumption is that the table looks like
     * it should be a HEALPix map of some sort.  If there are problems
     * with the metadata that prevent a consistent set of headers from
     * being generated, a TableFormatException with an informative
     * message should be thrown.
     *
     * @param  hpxInfo  non-null healpix description
     * @return   array of FITS headers describing healpix information
     */
    protected CardImage[] getHealpixHeaders( HealpixTableInfo hpxInfo )
            throws TableFormatException {
        String ipixColName = hpxInfo.getPixelColumnName();
        int level = hpxInfo.getLevel();
        String ordering = hpxInfo.isNest() ? "NESTED" : "RING";
        HealpixTableInfo.HpxCoordSys csys = hpxInfo.getCoordSys();

        /* Work out if we have explicit or implicit HEALPix pixel indices. */
        final boolean isExplicit;
        if ( ipixColName == null ) {
            isExplicit = false;
        }
        else if ( ipixColName.equals( table_.getColumnInfo( 0 ).getName() ) ) {
            isExplicit = true;
        }
        else {
             throw new TableFormatException( "HEALPix pixel index column \""
                                           + ipixColName + "\""
                                           + " is not first column" );
        }

        /* Check we have or can guess the HEALPix level (hence NSIDE),
         * and that the table row count is not inconsistent with it. */
        long nrow = rowCount_;
        assert nrow >= 0;
        if ( level < 0 && ! isExplicit ) {
            int clevel = Long.numberOfTrailingZeros( nrow / 12 ) / 2;
            if ( 12 * ( 1L << ( 2 * clevel ) ) == nrow ) {
                level = clevel;
                logger.warning( "Inferred HEALPix level " + clevel );
            }
        }
        if ( level < 0 ) {
            throw new TableFormatException( "No HEALPix level specified" );
        }
        if ( ! isExplicit ) {
            long npix = 12 * ( 1L << ( 2 * level ) );
            if ( npix != nrow ) {
                throw new TableFormatException( "Row count does not match level"
                                              + " for implicitly indexed"
                                              + " HEALPix table ("
                                              + nrow + " != " + npix + ")" );
            }
        }
        final long npix = 12 * ( 1L << ( 2 * level ) );
        final long nside = 1L << level;

        /* Prepare HEALPix format FITS headers. */
        List<CardImage> cards = new ArrayList<>();
        CardFactory cfact = CardFactory.DEFAULT;
        cards.addAll( Arrays.asList( new CardImage[] {
            cfact.createStringCard( "PIXTYPE", "HEALPIX", "HEALPix map" ),
            cfact.createIntegerCard( "NSIDE", nside, "HEALPix level parameter"),
            cfact.createStringCard( "ORDERING", ordering,
                                    "HEALPix index ordering scheme" ),
        } ) );
        if ( csys != null ) {
            cards.add( cfact.createStringCard( "COORDSYS", csys.getCharString(),
                                               "HEALPix coordinate system "
                                             + csys.getWord() ) );
        }
        if ( isExplicit ) {
            cards.add( cfact
                      .createStringCard( "INDXSCHM", "EXPLICIT",
                                         "HEALPix indices given explicitly" ) );
            cards.add( cfact
                      .createIntegerCard( "OBS_NPIX", nrow,
                                          "HEALPix pixel count" ) );
            if ( nrow < npix ) {
                cards.add( cfact.createStringCard( "OBJECT", "PARTIAL",
                                                   "HEALPix sky coverage "
                                                 + "is partial" ) );
            }
        }
        else {
            cards.addAll( Arrays.asList( new CardImage[] {
                cfact.createStringCard( "INDXSCHM", "IMPLICIT",
                                        "HEALPix indices given implicitly" ),
                cfact.createIntegerCard( "FIRSTPIX", 0, "First HEALPix index" ),
                cfact.createIntegerCard( "LASTPIX", npix - 1,
                                         "Last HEALPix index" ),
                cfact.createStringCard( "OBJECT", "FULLSKY",
                                        "HEALPix sky coverage is full" ),
            } ) );
        }
        return cards.toArray( new CardImage[ 0 ] );
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

        public BigDecimal getZero() {
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
