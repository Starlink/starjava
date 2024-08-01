package uk.ac.starlink.topcat;

import java.io.IOException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.MappingRowSplittable;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.RowData;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.table.WrapperRowAccess;
import uk.ac.starlink.table.WrapperRowSequence;

/**
 * Wrapper table which ensures that all its contents have sensible types.
 * In particular, {@link java.lang.Number}s are turned into 
 * {@link java.lang.Double}s, and anything which looks weird is turned
 * into a {@link java.lang.String}.
 *
 * @author   Mark Taylor
 * @since    24 May 2007
 */
public class NormaliseTable extends WrapperStarTable {

    private final Converter[] converters_;

    /**
     * Constructor.
     *
     * @param   base  base table
     */
    @SuppressWarnings("this-escape")
    public NormaliseTable( StarTable base ) {
        super( base );
        int ncol = base.getColumnCount();
        converters_ = new Converter[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            converters_[ icol ] =
                createConverter( super.getColumnInfo( icol ) );
        }
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return converters_[ icol ].getColumnInfo();
    }

    public Object getCell( long irow, int icol ) throws IOException {
        return converters_[ icol ].convert( super.getCell( irow, icol ) );
    }

    public Object[] getRow( long irow ) throws IOException {
        return convertRow( super.getRow( irow ) );
    }

    public RowSequence getRowSequence() throws IOException {
        RowSequence baseSeq = super.getRowSequence();
        return new WrapperRowSequence( baseSeq, convertMapper( baseSeq ) );
    }

    public RowAccess getRowAccess() throws IOException {
        RowAccess baseAcc = super.getRowAccess();
        return new WrapperRowAccess( baseAcc, convertMapper( baseAcc ) );
    }

    public RowSplittable getRowSplittable() throws IOException {
        RowSplittable baseSplit = super.getRowSplittable();
        return new MappingRowSplittable( baseSplit, this::convertMapper );
    }

    /**
     * Converts an unnormlaised to a normalised RowData.
     *
     * @param  baseRow  input RowData
     * @return  normalised RowData
     */
    private RowData convertMapper( RowData baseRow ) {
        return new RowData() {
            public Object getCell( int icol ) throws IOException {
                return converters_[ icol ].convert( baseRow.getCell( icol ) );
            }
            public Object[] getRow() throws IOException {
                return convertRow( baseRow.getRow() );
            }
        };
    }

    /**
     * Converts a table row to suitable types.
     * The conversion is carried out in place.
     *
     * @param   row   row to convert
     * @return   <code>row</code> (for convenience)
     */
    private Object[] convertRow( Object[] row ) {
        for ( int icol = 0; icol < converters_.length; icol++ ) {
            row[ icol ] = converters_[ icol ].convert( row[ icol ] );
        }
        return row;
    }

    /**
     * Constructs a converter suitable for a given column.
     *
     * @param  info   base colum info
     * @return  converter
     */
    private static Converter createConverter( final ColumnInfo info ) {
        Class<?> clazz = info.getContentClass();

        /* Primitives and array - no problem, use a null converter. */
        if ( clazz == Byte.class ||
             clazz == Short.class ||
             clazz == Integer.class ||
             clazz == Long.class ||
             clazz == Float.class ||
             clazz == Double.class ||
             clazz == Boolean.class ||
             clazz == Character.class ||
             clazz == String.class ||
             clazz == byte[].class ||
             clazz == short[].class ||
             clazz == int[].class || 
             clazz == long[].class ||
             clazz == float[].class ||
             clazz == double[].class ||
             clazz == String[].class ) {
            return new Converter() {
                public ColumnInfo getColumnInfo() {
                    return info;
                }
                public Object convert( Object value ) {
                    return value;
                }
            };
        }

        /* Numbers: turn them into Doubles. */
        else if ( clazz == Number.class ) {
            final ColumnInfo dubInfo = new ColumnInfo( info );
            dubInfo.setContentClass( Double.class );
            return new Converter() {
                public ColumnInfo getColumnInfo() {
                    return dubInfo;
                }
                public Object convert( Object value ) {
                    if ( value instanceof Double ) {
                        return value;
                    }
                    else if ( value instanceof Number ) {
                        return Double.valueOf( ((Number) value).doubleValue() );
                    }
                    else {
                        return null;
                    }
                }
            };
        }

        /* Anything else, call it a string. */
        else {
            final ColumnInfo strInfo = new ColumnInfo( info );
            strInfo.setContentClass( String.class );
            return new Converter() {
                public ColumnInfo getColumnInfo() {
                    return strInfo;
                }
                public Object convert( Object value ) {
                    return value == null 
                         ? null
                         : value.toString();
                }
            };
        }
    }

    /**
     * Defines behaviour of a converter.
     */
    private static abstract class Converter {

        /**
         * Returns the column metadata for the converted column.
         *
         * @return  column info
         */
        public abstract ColumnInfo getColumnInfo();

        /**
         * Converts a value into the column's normalised form.
         *
         * @param   value   original value
         * @return  converted value
         */
        public abstract Object convert( Object value );
    }
}
