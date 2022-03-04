package uk.ac.starlink.fits;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.DoubleFunction;
import java.util.function.LongFunction;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;

/**
 * StarTable implementation for FITS (ASCII) TABLE extensions.
 *
 * @author   Mark Taylor
 * @since    4 Mar 2022
 */
public abstract class AsciiTableStarTable extends AbstractStarTable {

    private final long nrow_;
    private final int ncol_;
    private final int rowLeng_;
    private final ColumnInfo[] colInfos_;
    private final int[] icOffs_;
    private final int[] icLengs_;
    private final String[] trimNulls_;
    private final AsciiColumnReader<?>[] colReaders_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.fits" );

    private static final Pattern TFORM_REGEX =
        Pattern.compile( " *([AIFED])([0-9]+)(?:\\.([0-9]+))? *" );
    private static final Pattern INT_REGEX =
        Pattern.compile( "([+-]?) *([0-9]+)" );
    private static final Pattern FLOAT_REGEX =
        Pattern.compile( "([+-]?(?:[0-9]*\\.[0-9]*|[0-9]+\\.?))"
                       + "[ED]?([+-]?[0-9]+)?" );

    /**
     * Constructor.
     *
     * @param  hdr  ASCII TABLE extension header
     */
    public AsciiTableStarTable( FitsHeader hdr ) throws IOException {

        /* Check we have a TABLE extension. */
        if ( ! hdr.getStringValue( "XTENSION" ).equals( "TABLE" ) ) {
            throw new TableFormatException( "Not an ASCII TABLE extension" );
        }

        /* Get table characteristics. */
        rowLeng_ = hdr.getRequiredIntValue( "NAXIS1" );
        nrow_ = hdr.getRequiredLongValue( "NAXIS2" );
        ncol_ = hdr.getRequiredIntValue( "TFIELDS" );
        if ( ncol_ > 999 ) {
            throw new TableFormatException( "TFIELDS > 999 (" + ncol_ + ")" );
        }

        /* Set up column metadata. */
        colInfos_ = new ColumnInfo[ ncol_ ];
        icOffs_ = new int[ ncol_ ];
        icLengs_ = new int[ ncol_ ];
        trimNulls_ = new String[ ncol_ ];
        colReaders_ = new AsciiColumnReader<?>[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            int jcol = icol + 1;
            ColumnInfo cinfo = new ColumnInfo( "col" + jcol );
            colInfos_[ icol ] = cinfo;

            /* Required items. */
            int tbcol = hdr.getRequiredIntValue( "TBCOL" + jcol );
            String tform = hdr.getRequiredStringValue( "TFORM" + jcol );

            /* Name. */
            String ttype = hdr.getStringValue( "TTYPE" + jcol );
            if ( ttype != null ) {
                cinfo.setName( ttype );
            }

            /* Units. */
            String tunit = hdr.getStringValue( "TUNIT" + jcol );
            if ( tunit != null ) {
                cinfo.setUnitString( tunit );
            }

            /* Scaling. */
            Double tscal = hdr.getDoubleValue( "TSCAL" + jcol );
            double scale = tscal == null ? 1.0 : tscal.doubleValue();
            Double tzero = hdr.getDoubleValue( "TZERO" + jcol );
            double zero = tzero == null ? 0.0 : tzero.doubleValue();

            /* Blank value. */
            String tnull = hdr.getStringValue( "TNULL" + jcol );
            String trimNull = tnull == null ? null : tnull.trim();
            if ( trimNull != null && trimNull.length() > 0 ) {
                trimNulls_[ icol ] = trimNull;
            }
            else if ( tform.trim().startsWith( "I" ) ) {
                cinfo.setNullable( false );
            }

            /* Create column reader. */
            icOffs_[ icol ] = tbcol - 1;
            AsciiColumnReader<?> crdr =
                createColumnReader( tform, scale, zero );
            icLengs_[ icol ] = crdr.getFieldWidth();
            cinfo.setContentClass( crdr.getValueClass() );
            colReaders_[ icol ] = crdr;
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

    /**
     * Returns the length of each row in bytes.
     *
     * @return  row length
     */
    protected int getRowLength() {
        return rowLeng_;
    }

    /**
     * Returns the array of column offsets from the start of the row.
     *
     * @return  ncol-length array of byte offsets from row start
     */
    protected int[] getColumnOffsets() {
        return icOffs_;
    }

    /**
     * Returns the array of column lengths in bytes.
     *
     * @return  ncol-length array of field byte lengths
     */
    protected int[] getColumnLengths() {
        return icLengs_;
    }

    /**
     * Reads the content of a row of this table from a byte buffer.
     *
     * @param   rowBuf  buffer containing rowLeng bytes
     * @return   ncol-element array of cell values
     */
    protected Object[] readRow( byte[] rowBuf ) {
        String rowTxt = new String( rowBuf, StandardCharsets.US_ASCII );
        Object[] row = new Object[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            String cellTxt =
                trimSequence( rowTxt, icOffs_[ icol ], icLengs_[ icol ] );
            String trimNull = trimNulls_[ icol ];
            row[ icol ] = trimNull != null && trimNull.equals( cellTxt )
                        ? null
                        : colReaders_[ icol ].readValue( cellTxt );
        }
        return row;
    }

    /**
     * Reads the content of a cell of this table from a byte buffer.
     *
     * @param  cellBuf  buffer containing bytes for column content
     * @param  icol    column index
     * @return  cell value
     */
    protected Object readCell( byte[] cellBuf, int icol ) {
        int leng = icLengs_[ icol ];
        String cellTxt =
            trimSequence( new String( cellBuf, StandardCharsets.US_ASCII ),
                          0, leng );
        String trimNull = trimNulls_[ icol ];
        return trimNull != null && trimNull.equals( cellTxt )
             ? null
             : colReaders_[ icol ].readValue( cellTxt );
    }

    /**
     * Creates a table instance from a TABLE HDU.
     *
     * @param  hdr   FITS header for ASCII table
     * @param  inputFact  input factory for Data part of HDU
     * @return  new table; it will be random-access according to whether
     *          the input factory is
     */
    public static AsciiTableStarTable createTable( FitsHeader hdr,
                                                   InputFactory inputFact )
            throws IOException {
        return inputFact.isRandom()
             ? new RandomAsciiTableStarTable( hdr, inputFact )
             : new SequentialAsciiTableStarTable( hdr, inputFact );
    }


    /**
     * Reads a TABLE HDU from a stream and writes the result to
     * a table sink.
     *
     * @param   hdr  FITS header object describing the TABLE extension
     * @param   input   input stream positioned at the start of the
     *                  data part of the TABLE extension
     * @param   sink   destination for the table
     */
    public static void streamStarTable( FitsHeader hdr, BasicInput input,
                                        TableSink sink )
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
        AsciiTableStarTable meta =
            new SequentialAsciiTableStarTable( hdr, dummyFact );
        sink.acceptMetadata( meta );
        long nrow = meta.getRowCount();
        int rowLeng = meta.getRowLength();
        byte[] rowBuf = new byte[ rowLeng ];
        for ( long i = 0; i < nrow; i++ ) {
            input.readBytes( rowBuf );
            Object[] row = meta.readRow( rowBuf );
            sink.acceptRow( row ); }
        sink.endRows();
        long datasize = nrow * meta.getRowLength();
        int over = (int) ( datasize % (long) FitsUtil.BLOCK_LENG );
        if ( over > 0 ) {
            input.skip( over );
        }
    }

    /**
     * Trims whitespace from both ends of a string.
     * Should do the same as txt.substring(ioff, ioff+leng).trim()
     * but with less object creation.  Premature optimisation?
     *
     * @param   txt  input text
     * @param   ioff  start position
     * @param   leng  length
     * @return  trimmed text between ioff and ioff+leng
     */
    private static String trimSequence( String txt, int ioff, int leng ) {
        int iend = ioff + leng;
        int i0 = ioff;
        int i1 = iend;
        for ( int i = ioff; i < iend; i++ ) {
            char c = txt.charAt( i );
            if ( c == ' ' ) {
                if ( i0 == i ) {
                    i0++;
                }
            }
            else {
                i1 = i + 1;
            }
        }
        return i1 > i0 ? txt.substring( i0, i1 ) : "";
    }

    /**
     * Creates a column reader given column characteristics.
     *
     * @param  tform  format string as per TFORMnn header
     * @param  scale  scaling constant (1.0 for no scaling)
     * @param  zero   scaling offset (0.0 for no scaling)
     * @param   column reader
     */
    private static AsciiColumnReader<?>
            createColumnReader( String tform, double scale, double zero ) {

        /* Parse format string. */
        Matcher matcher = TFORM_REGEX.matcher( tform );
        if ( ! matcher.matches() ) {
            logger_.warning( "Illegal TFORM \"" + tform + "\"" );
            return createReader( Void.class, 0, txt -> null );
        }
        char fmtChar = matcher.group( 1 ).charAt( 0 );
        int width = Integer.parseInt( matcher.group( 2 ) );
        String dTxt = matcher.group( 3 );
        int dplace = dTxt == null ? 0 : Integer.parseInt( dTxt );

        /* String reader. */
        if ( fmtChar == 'A' ) {
            return createReader( String.class, width, txt -> txt );
        }

        /* Integer reader; may yield floating point values
         * in case of scaling. */
        else if ( fmtChar == 'I' ) {
            LongFunction<?> fromLong;
            if ( scale == 1.0 && zero == 0.0 ) {
                if ( width < 10 ) {
                    return createIntReader( Integer.class, width,
                                            lval -> Integer
                                                   .valueOf( (int) lval ) );
                }
                else {
                    return createIntReader( Long.class, width,
                                            lval -> Long.valueOf( lval ) );
                }
            }
            else {
                return createIntReader( Double.class, width,
                                        lval -> Double
                                               .valueOf( lval * scale + zero ));
            }
        }

        /* Floating point reader; D, E and F are identical. */
        else if ( fmtChar == 'D' || fmtChar == 'E' || fmtChar == 'F' ) {
            if ( scale == 1.0 && zero == 0.0 ) {
                if ( width < 8 ) {
                    return createFloatingReader( Float.class, width, dplace,
                                                 d -> Float
                                                     .valueOf( (float) d ) );
                }
                else {
                    return createFloatingReader( Double.class, width, dplace,
                                                 d -> Double.valueOf( d ) );
                }
            }
            else {
                return createFloatingReader( Double.class, width, dplace,
                                             d -> Double
                                                 .valueOf( d * scale + zero ) );
            }
        }
        else {
            logger_.warning( "Illegal TFORM \"" + tform + "\"" );
            return createReader( Void.class, 0, txt -> null );
        }
    }

    /**
     * Creates a column reader for an integer (A) fields.
     *
     * @param   clazz   output value class
     * @param  width   number of bytes in input field
     * @param  fromLong   converts read integer value to output
     * @return  new reader
     */
    private static <T> AsciiColumnReader<T>
            createIntReader( Class<T> clazz, int width,
                             LongFunction<T> fromLong ) {
        return createReader( clazz, width, txt -> {

            /* Note a blank field is considered equal to zero
             * (FITS 4.0 sec 7.2.5). */
            if ( txt.length() == 0 ) {
                return fromLong.apply( 0L );
            }
            else {
                Matcher intMatcher = INT_REGEX.matcher( txt );
                if ( intMatcher.matches() ) {
                    long val;
                    try {
                        val = Long.parseLong( intMatcher.group( 2 ) );
                    }
                    catch ( NumberFormatException e ) {
                        // shouldn't happen
                        return null;
                    }
                    long sval = "-".equals( intMatcher.group( 1 ) )
                              ? -val
                              : val;
                    return fromLong.apply( sval );
                }
                else {
                    return null;
                }
            }
        } );
    }

    /**
     * Creates a column reader for floating point (D/E/F) fields.
     *
     * @param   clazz   output value class
     * @param  width   number of bytes in input field
     * @param  dplace   d value from [EFD]w.d format string
     * @param  fromDouble   converts read double value to output
     * @return  new reader
     */
    private static <T> AsciiColumnReader<T>
            createFloatingReader( Class<T> clazz, int width, int dplace,
                                  DoubleFunction<T> fromDouble ) {
        double implicitFactor = Math.pow( 10.0, -dplace );
        return createReader( clazz, width, txt -> {
            Matcher floatMatcher = FLOAT_REGEX.matcher( txt );
            if ( floatMatcher.matches() ) {
                String mantissa = floatMatcher.group( 1 );
                String exponent = floatMatcher.group( 2 );
                String numTxt = exponent == null
                              ? mantissa
                              : mantissa + "E" + exponent;
                double dval;
                try {
                    dval = Double.parseDouble( numTxt );
                }
                catch ( NumberFormatException e ) {
                    // shouldn't happen
                    return null;
                }
                if ( mantissa.indexOf( '.' ) < 0 ) {
                    dval *= implicitFactor;
                }
                return fromDouble.apply( dval );
            }
            else {
                return null;
            }
        } );
    }

    /**
     * Utility function to create a typed AsciiColumnReader.
     *
     * @param  clazz   output value class
     * @param  width   number of bytes in input field
     * @param  read    function that converts input characters to output value
     * @return  new reader
     */
    private static <T> AsciiColumnReader<T>
            createReader( Class<T> clazz, int width, Function<String,T> read ) {
        return new AsciiColumnReader<T>() {
            public Class<T> getValueClass() {
                return clazz;
            }
            public int getFieldWidth() {
                return width;
            }
            public T readValue( String trimTxt ) {
                return read.apply( trimTxt );
            }
        };
    }

    /**
     * Converts input character data to typed cell values.
     */
    private interface AsciiColumnReader<T> {

        /**
         * Returns the output value class.
         *
         * @return  value class
         */
        Class<T> getValueClass();

        /**
         * Returns the number of bytes in the input field.
         *
         * @return  field width in bytes
         */
        int getFieldWidth();

        /**
         * Returns the typed output value given a trimmed input string.
         *
         * <p>Numeric strings are certainly supposed to be trimmed of spaces.
         * It's not totally obvious that's the case for Strings,
         * but at least for trailing spaces it makes sense for use in STIL.
         * Present them trimmed at both ends anyway,
         * since it makes the code easier and
         * probably nobody is going to complain.
         *
         * @param  trimTxt  input string value trimmed of whitespace
         *                  at both ends
         * @return   output value
         */
        T readValue( String trimTxt );
    }

    /**
     * Concrete AsciiTableStarTable implementation with sequential data access.
     */
    private static class SequentialAsciiTableStarTable
            extends AsciiTableStarTable {

        private final InputFactory inputFact_;

        /**
         * Constructor.
         *
         * @param  hdr   FITS header for ASCII table
         * @param  inputFact  input factory for Data part of HDU
         */
        SequentialAsciiTableStarTable( FitsHeader hdr,
                                       InputFactory inputFact )
                throws IOException {
            super( hdr );
            inputFact_ = inputFact;
        }

        public RowSequence getRowSequence() throws IOException {
            final BasicInput input = inputFact_.createInput( true );
            final Object[] beforeStart = new Object[ 0 ];
            final long nrow = getRowCount();
            final int rowLength = getRowLength();
            final byte[] rowBuf = new byte[ rowLength ];
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
                        input.readBytes( rowBuf );
                        row_ = readRow( rowBuf );
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
     * Concrete AsciiTableStarTable implementation with random data access.
     */
    private static class RandomAsciiTableStarTable extends AsciiTableStarTable {

        private final InputFactory inputFact_;
        private final BasicInputThreadLocal randomInputThreadLocal_;
        private final int rowLength_;
        private final int[] colOffsets_;
        private final int[] colLengths_;

        /**
         * Constructor.
         *
         * @param  hdr   FITS header for ASCII table
         * @param  inputFact  random access input factory for Data part of HDU
         */
        RandomAsciiTableStarTable( FitsHeader hdr, InputFactory inputFact )
                throws IOException {
            super( hdr );
            inputFact_ = inputFact;
            if ( ! inputFact.isRandom() ) {
                throw new IllegalArgumentException( "not random" );
            }
            rowLength_ = getRowLength();
            colOffsets_ = getColumnOffsets();
            colLengths_ = getColumnLengths();
            randomInputThreadLocal_ =
                new BasicInputThreadLocal( inputFact, false );
        }

        @Override
        public boolean isRandom() {
            return true;
        }

        public Object getCell( long lrow, int icol ) throws IOException {
            BasicInput randomInput = randomInputThreadLocal_.get();
            randomInput.seek( lrow * rowLength_ + colOffsets_[ icol ] );
            byte[] cellBuf = new byte[ colLengths_[ icol ] ];
            randomInput.readBytes( cellBuf );
            return readCell( cellBuf, icol );
        }

        public Object[] getRow( long lrow ) throws IOException {
            BasicInput randomInput = randomInputThreadLocal_.get();
            randomInput.seek( lrow * rowLength_ );
            byte[] rowBuf = new byte[ rowLength_ ];
            randomInput.readBytes( rowBuf );
            return readRow( rowBuf );
        }

        public RowSequence getRowSequence() throws IOException {
            final BasicInput input = inputFact_.createInput( true );
            assert input.isRandom();
            final long endPos = getRowCount() * rowLength_;
            final byte[] rowBuf = new byte[ rowLength_ ];
            int ncol = getColumnCount();
            final byte[][] cellBufs = new byte[ ncol ][];
            for ( int ic = 0; ic < ncol; ic++ ) {
                cellBufs[ ic ] = new byte[ colLengths_[ ic ] ];
            }
            return new RowSequence() {
                long pos = -rowLength_;
                public boolean next() {
                    pos += rowLength_;
                    return pos < endPos;
                }
                public Object getCell( int icol ) throws IOException {
                    if ( pos >= 0 && pos < endPos ) {
                        input.seek( pos + colOffsets_[ icol ] );
                        byte[] cellBuf = cellBufs[ icol ];
                        input.readBytes( cellBuf );
                        return readCell( cellBuf, icol );
                    }
                    else {
                        throw new IllegalStateException();
                    }
                }
                public Object[] getRow() throws IOException {
                    if ( pos >= 0 && pos < endPos ) {
                        input.seek( pos );
                        input.readBytes( rowBuf );
                        return readRow( rowBuf );
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
            final byte[] rowBuf = new byte[ rowLength_ ];
            int ncol = getColumnCount();
            final byte[][] cellBufs = new byte[ ncol ][];
            for ( int ic = 0; ic < ncol; ic++ ) {
                cellBufs[ ic ] = new byte[ colLengths_[ ic ] ];
            }
            return new RowAccess() {
                long irow_ = -1;
                public void setRowIndex( long irow ) {
                    irow_ = irow;
                }
                public Object getCell( int icol ) throws IOException {
                    input.seek( irow_ * rowLength_ + colOffsets_[ icol ] );
                    byte[] cellBuf = cellBufs[ icol ];
                    input.readBytes( cellBuf );
                    return readCell( cellBuf, icol );
                }
                public Object[] getRow() throws IOException {
                    input.seek( irow_ * rowLength_ );
                    input.readBytes( rowBuf );
                    return readRow( rowBuf );
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
