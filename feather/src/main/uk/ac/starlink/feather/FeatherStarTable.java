package uk.ac.starlink.feather;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import uk.ac.bristol.star.feather.Decoder;
import uk.ac.bristol.star.feather.FeatherColumn;
import uk.ac.bristol.star.feather.FeatherTable;
import uk.ac.bristol.star.feather.FeatherType;
import uk.ac.bristol.star.feather.Reader;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;

/**
 * Adaptor from FeatherTable to StarTable.
 *
 * @author   Mark Taylor
 * @since    26 Feb 2020
 */
public class FeatherStarTable extends AbstractStarTable {

    private final FeatherTable ftable_;
    private final int ncol_;
    private final long nrow_;
    private final String name_;
    private final FeatherColumn[] fcols_;
    private final ColumnInfo[] colInfos_;
    private final RowReader randomReader_;

    /** JSON key used to store UCDs in column user metadata. */
    public static final String UCD_KEY = "ucd";

    /** JSON key used to store Utypes in column user metadata. */
    public static final String UTYPE_KEY = "utype";

    /** JSON key used to store units in column user metadata. */
    public static final String UNIT_KEY = "unit";

    /** JSON key used to store description text in column user metadata. */
    public static final String DESCRIPTION_KEY = "description";

    /** JSON key to store stringified array shape in column user metadata. */
    public static final String SHAPE_KEY = "shape";

    /** JSON key to store miscellaneous/broken metadata in column metadata. */
    public static final String META_KEY = "meta";

    /** Aux metadata key for column feather type value. */
    public static final ValueInfo FTYPE_INFO =
        new DefaultValueInfo( "feather_type", String.class,
                              "Data type code from Feather format input file" );

    /**
     * Constructs a FeatherStarTable from a FeatherTable.
     *
     * @param   ftable   feather table object
     */
    public FeatherStarTable( FeatherTable ftable ) {
        ftable_ = ftable;
        ncol_ = ftable.getColumnCount();
        nrow_ = ftable.getRowCount();
        name_ = ftable.getDescription();
        fcols_ = new FeatherColumn[ ncol_ ];
        colInfos_ = new ColumnInfo[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            fcols_[ icol ] = ftable.getColumn( icol );
            colInfos_[ icol ] = createColumnInfo( fcols_[ icol ] );
        }
        randomReader_ = new RowReader();
    }

    /**
     * Constructs a FeatherStarTable from a File.
     *
     * @param  file  file
     */
    public FeatherStarTable( File file ) throws IOException {
        this( FeatherTable.fromFile( file ) );
    }

    public int getColumnCount() {
        return ncol_;
    }

    public long getRowCount() {
        return nrow_;
    }

    public boolean isRandom() {
        return true;
    }

    public String getName() {
        return name_;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colInfos_[ icol ];
    }

    public Object getCell( long irow, int icol ) throws IOException {
        return randomReader_.getCell( irow, icol );
    }

    public Object getRow( long irow, int icol ) throws IOException {
        return randomReader_.getRow( irow );
    }

    public RowSequence getRowSequence() {
        final RowReader rowReader = new RowReader();
        return new RowSequence() {
            long irow_ = -1;
            boolean hasData_ = false;
            public boolean next() {
                if ( irow_ < nrow_ - 1 ) {
                    irow_++;
                    hasData_ = true;
                }
                else {
                    hasData_ = false;
                }
                return hasData_;
            }
            public Object getCell( int icol ) throws IOException {
                if ( hasData_ ) {
                    return rowReader.getCell( irow_, icol );
                }
                else {
                    throw new IllegalStateException();
                }
            }
            public Object[] getRow() throws IOException {
                if ( hasData_ ) {
                    return rowReader.getRow( irow_ );
                }
                else {
                    throw new IllegalStateException();
                }
            }
            public void close() {
            }
        };
    }

    /**
     * Adapts a FeatherColumn to a ColumnInfo.
     *
     * @param  fcol  feather column object
     * @return  column metadata
     */
    private static ColumnInfo createColumnInfo( FeatherColumn fcol ) {
        Decoder<?> decoder = fcol.getDecoder();
        Class<?> clazz = decoder.getValueClass();
        FeatherType ftype = decoder.getFeatherType();
        ColumnInfo info = new ColumnInfo( fcol.getName(), clazz, null );
        info.setNullable( fcol.getNullCount() > 0 );
        Map<String,String> metaMap = getColumnMetaMap( fcol.getUserMeta() );
        for ( Map.Entry<String,String> entry : metaMap.entrySet() ) {
            String key = entry.getKey();
            String value = entry.getValue();
            if ( key.equals( UCD_KEY ) ) {
                info.setUCD( value );
            }
            if ( key.equals( UTYPE_KEY ) ) {
                info.setUtype( value );
            }
            if ( key.equals( UNIT_KEY ) ) {
                info.setUnitString( value );
            }
            if ( key.equals( DESCRIPTION_KEY ) ) {
                info.setDescription( value );
            }
            if ( key.equals( SHAPE_KEY ) ) {
                info.setShape( DefaultValueInfo.unformatShape( value ) );
            }
        }
        info.setAuxDatum( new DescribedValue( FTYPE_INFO, ftype.toString() ) );
        if ( ftype == FeatherType.UINT8 && clazz.equals( Short.class ) ) {
            info.setAuxDatum( new DescribedValue( Tables.UBYTE_FLAG_INFO,
                                                  Boolean.TRUE ) );
        }
        return info;
    }

    /**
     * Turns a column user metadata string into a String-&gt;String map.
     * The input string is assumed or hoped to be in JSON format.
     * If it has been written by this package, it will contain well-known
     * keys representing normal column metadata.
     *
     * @param  userMeta  user metadata text, hopefully JSON
     * @return   key-value map
     */
    private static Map<String,String> getColumnMetaMap( String userMeta ) {
        Map<String,String> map = new LinkedHashMap<String,String>();
        if ( userMeta != null && userMeta.trim().length() > 0 ) {
            try {
                JSONObject json = new JSONObject( userMeta );
                for ( String key : json.keySet() ) {
                    if ( key.equals( UCD_KEY ) ||
                         key.equals( UTYPE_KEY ) ||
                         key.equals( UNIT_KEY ) ||
                         key.equals( DESCRIPTION_KEY ) ||
                         key.equals( SHAPE_KEY ) ) {
                        map.put( key, json.get( key ).toString() );
                    }
                }
            }
            catch ( JSONException e ) {
                map.put( META_KEY, userMeta ); 
            }
        }
        return map;
    }

    /**
     * Row/column access object that can acquire data values from table.
     */
    private class RowReader {

        final Reader<?>[] rdrs_ = new Reader<?>[ ncol_ ];

        /**
         * Returns a reader object for a given column.
         *
         * @param  icol  column index
         * @return  column reader
         */
        Reader<?> getReader( int icol ) throws IOException {
            Reader<?> rdr = rdrs_[ icol ];
            if ( rdr != null ) {
                return rdr;
            }
            else {
                rdrs_[ icol ] = fcols_[ icol ].createReader();
                return rdrs_[ icol ];
            }
        }

        /**
         * Returns a cell value.
         *
         * @param  irow  row index
         * @param  icol  column index
         * @return  cell value
         */
        Object getCell( long irow, int icol ) throws IOException {
            return getReader( icol ).getObject( irow );
        }

        /**
         * Returns an array of objects giving the cells in a row.
         *
         * @param  irow  row index
         * @return   cell value array
         */
        Object[] getRow( long irow ) throws IOException {
            Object[] row = new Object[ ncol_ ];
            for ( int ic = 0; ic < ncol_; ic++ ) {
                row[ ic ] = getReader( ic ).getObject( irow );
            }
            return row;
        }
    }
}
