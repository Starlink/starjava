package uk.ac.starlink.hapi;

import java.util.Arrays;
import uk.ac.starlink.table.ColumnInfo;

/**
 * Reads values specified by a particular parameter from a HAPI data stream.
 * Instances of this class are safe for concurrent use from multiple threads.
 *
 * @author   Mark Taylor
 * @since    12 Jan 2024
 */
public class ParamReader {

    private final ColumnInfo[] colInfos_;
    private final int nfield_;
    private final int nbyte_;
    private final StringReader stringReader_;
    private final BinaryReader binaryReader_;

    /**
     * Constructor.
     *
     * @param  colInfos  metadata for StarTable columns read by this reader
     * @param  nfield    number of HAPI columns read by this reader
     * @param  nbyte     number of bytes in HAPI binary stream read by
     *                   this reader
     * @param  stringReader  reads column values from a CSV stream
     * @param  binaryReader  reads column values from a binary stream
     */
    private ParamReader( ColumnInfo[] colInfos, int nfield, int nbyte,
                         StringReader stringReader,
                         BinaryReader binaryReader ) {
        colInfos_ = colInfos;
        nfield_ = nfield;
        nbyte_ = nbyte;
        stringReader_ = stringReader;
        binaryReader_ = binaryReader;
    }

    /**
     * Returns the number of StarTable columns read by this reader.
     *
     * @return  STIL column count
     */
    public int getColumnCount() {
        return colInfos_.length;
    }

    /**
     * Returns the column metadata for one of the StarTable columns
     * read by this reader.
     *
     * @param  icol  column index, in range 0..getColumnCount()
     * @return  column metadata
     */
    public ColumnInfo getColumnInfo( int icol ) {
        return colInfos_[ icol ];
    }

    /**
     * Returns the number of HAPI table fields read by this reader.
     *
     * @return  field count
     */
    public int getFieldCount() {
        return nfield_;
    }

    /**
     * Returns the number of bytes in a HAPI binary stream read by this reader.
     *
     * @return  binary stream byte read count
     */
    public int getByteCount() {
        return nbyte_;
    }

    /**
     * Converts from HAPI CSV text cell values to StarTable cell values.
     *
     * @param  cells  array of CSV field values
     * @param  istart  index of first CSV field of interest in cells
     * @param  result   columnCount-element array into which output values
     *                  will be written
     */
    public void readStringValues( String[] cells, int istart, Object[] result ){
        stringReader_.readStringValues( cells, istart, result );
    }

    /**
     * Converts from HAPI binary stream bytes to StarTable cell values.
     *
     * @param  buf  buffer containing HAPI binary stream data
     * @param  istart  index of first byte of interest in buffer
     * @param  result   columnCount-element array into which output values
     *                  will be written
     */
    public void readBinaryValues( byte[] buf, int istart, Object[] result ) {
        binaryReader_.readBinaryValues( buf, istart, result );
    }

    /**
     * Returns a ParamReader instance for a given HAPI parameter.
     *
     * @param  param  HAPI parameter
     */
    public static ParamReader createReader( HapiParam param ) {
        int[] size = param.getSize();
        HapiType<?,?> type = param.getType();
        String[] units = param.getUnits();
        String fill = param.getFill();
        int nel = size == null
                ? 1
                : Arrays.stream( size ).reduce( 1, (a, b) -> a * b );

        /* In principle we could have columns which are arrays in HAPI
         * but should be multiple scalar columns in a StarTable
         * (some kind of inhomogeneous array with different labels and/or
         * units for each element).
         * In that case we'd just need to produce a ParamReader with
         * multiple ColumnInfos.  However, I haven't seen any HAPI tables
         * that look like this is necessary so far, so defer implementation
         * of this case until that happens. */
        final boolean multiScalar = false;

        /* Empty parameter, shouldn't happen. */
        if ( nel == 0 ) {
            return new ParamReader( new ColumnInfo[ 0 ], 0, 0, null, null );
        }

        /* Scalar parameter. */
        else if ( nel == 1 ) {
            ColumnInfo info = new ColumnInfo( param.getName(),
                                              type.getScalarClass(),
                                              param.getDescription() );
            type.adjustInfo( info );
            if ( units != null && units.length == 1 ) {
                info.setUnitString( adjustUnits( units[ 0 ] ) );
            }
            if ( String.class.equals( type.getScalarClass() ) ) {
                int stringLeng = param.getLength();
                if ( stringLeng > 0 ) {
                    info.setElementSize( stringLeng );
                }
            }
            final StringReader stringReader;
            if ( fill == null ) {
                stringReader = (cells, ipos, result) -> {
                    result[ 0 ] = type.readStringScalar( cells[ ipos ] );
                };
            }
            else {
                stringReader = (cells, ipos, result) -> {
                    String cell = cells[ ipos ];
                    result[ 0 ] = fill.equals( cell )
                                ? null
                                : type.readStringScalar( cell );
                };
            }
            final BinaryReader binaryReader;
            int nbyte = type.getByteCount( param.getLength() );
            Object fillValue = fill == null ? null
                                            : type.readStringScalar( fill );
            if ( fillValue == null ) {
                binaryReader = (buf, ipos, result) -> {
                    result[ 0 ] = type.readBinaryScalar( buf, ipos, nbyte );
                };
            }
            else {
                binaryReader = (buf, ipos, result) -> {
                    Object out = type.readBinaryScalar( buf, ipos, nbyte );
                    result[ 0 ] = out.equals( fillValue ) ? null : out;
                };
            }
            return new ParamReader( new ColumnInfo[] { info }, 1, nbyte,
                                    stringReader, binaryReader );
        }

        /* Array in HAPI, treated as multiple columns in StarTable.
         * Not currently implemented, but could be. */
        else if ( multiScalar ) {
            throw new AssertionError();
        }

        /* Array in HAPI treated as array-valued column in StarTable. */
        else {
            ColumnInfo info = new ColumnInfo( param.getName(),
                                              type.getArrayClass(),
                                              param.getDescription() );

            /* HAPI has C-like array indices (last varying fastest), and
             * STIL has FITS/FORTRAN-like indices (first varying fastest),
             * so we reverse the dimension order here. */
            int[] stilShape = reverse( size );
            info.setShape( stilShape );
            type.adjustInfo( info );
            if ( units != null &&
                 ( units.length == 1 ||
                   Arrays.stream( units ).distinct().count() == 1 ) ) {
                info.setUnitString( adjustUnits( units[ 0 ] ) );
            }
            int elSize = type.getByteCount( param.getLength() );
            BinaryReader binaryReader =
                createBinaryArrayReader( type, elSize, nel, fill );
            StringReader stringReader =
                createStringArrayReader( type, nel, fill );
            return new ParamReader( new ColumnInfo[] { info }, nel,
                                    nel * elSize, stringReader, binaryReader );
        }
    }

    /**
     * Tweak units to deal with some HAPI idiosyncracies that we don't
     * want to see in STIL.
     *
     * @param  hapiUnits  units from HAPI parameter
     * @return  unit string suitable for ValueInfo
     */
    private static String adjustUnits( String hapiUnits ) {
        if ( "dimensionless".equals( hapiUnits ) ||
             "UTC".equals( hapiUnits ) ) {
            return null;
        }
        else {
            return hapiUnits;
        }
    }

    /**
     * Returns an array in the opposite order.
     *
     * @param  array  input array
     * @return   array with elements in reversed order
     */
    private static int[] reverse( int[] array ) {
        int n = array.length;
        int[] array1 = new int[ n ];
        for ( int i = 0; i < n; i++ ) {
            array1[ n - i - 1 ] = array[ i ];
        }
        return array1;
    }

    /**
     * Returns a reader that will read multiple cells from
     * a string input array and store them as an array object
     * in the first element of a supplied result array.
     *
     * @param  type  data type
     * @param  nel   number of fields to read
     * @param  fillTxt   fill string value
     * @return  string reader
     */
    private static <S,A> StringReader 
            createStringArrayReader( HapiType<S,A> type, int nel,
                                     String fillTxt ) {
        if ( fillTxt == null || type.readStringScalar( fillTxt ) == null ) {
            return (cells, istart, result) -> {
                result[ 0 ] = type.readStringArray( cells, istart, nel );
            };
        }
        else {
            final S fillValue = type.readStringScalar( fillTxt );
            return (cells, istart, result) -> {
                A outArray = type.readStringArray( cells, istart, nel );
                type.applyFills( outArray, fillValue );
                result[ 0 ] = outArray;
            };
        }
    }

    /**
     * Returns a reader that will read multiple cells from a
     * binary input buffer and store them as an array Object
     * in the first element of a supplied result array.
     *
     * @param   type  data type
     * @param   elSize  size of each input field in bytes
     * @param   nel   number of fields to read
     * @param  fillTxt   fill string value
     * @return  binary reader
     */
    private static <S,A> BinaryReader
            createBinaryArrayReader( HapiType<S,A> type, int elSize, int nel,
                                     String fillTxt ) {
        if ( fillTxt == null || type.readStringScalar( fillTxt ) == null ) {
            return (buf, istart, result) -> {
                result[ 0 ] = type.readBinaryArray( buf, istart, elSize, nel );
            };
        }
        else {
            final S fillValue = type.readStringScalar( fillTxt );
            return (buf, istart, result) -> {
                A outArray = type.readBinaryArray( buf, istart, elSize, nel );
                type.applyFills( outArray, fillValue );
                result[ 0 ] = outArray;
            };
        }
    }

    /**
     * Reads CSV-like string values and converts them to STIL-friendly objects.
     */
    @FunctionalInterface
    private static interface StringReader {

        /**
         * Reads cells from CSV-like strings and stores the resulting objects
         * in a supplied object array.
         *
         * @param  cells  text values read from a CSV row
         * @param  istart  first element of cells to read
         * @param  result  array into which converted values should be stored,
         *                 starting from the first element
         */
        void readStringValues( String[] cells, int istart, Object[] result );
    }

    /**
     * Reads from a binary buffer and converts to STIL-friendly objects.
     */
    @FunctionalInterface
    private static interface BinaryReader {

        /**
         * Reads bytes from a buffer and stores the resulting objects
         * in a supplied object array.
         *
         * @param  buf  input byte buffer
         * @param  istart  first element of buf to read from
         * @param  result  array into which converted values should be stored,
         *                 starting from the first element
         */
        void readBinaryValues( byte[] buf, int istart, Object[] result );
    }
}
