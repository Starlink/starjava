package uk.ac.starlink.feather;

import java.io.IOException;
import java.io.OutputStream;
import org.json.JSONObject;
import uk.ac.bristol.star.feather.BufUtils;
import uk.ac.bristol.star.feather.ColStat;
import uk.ac.bristol.star.feather.FeatherColumnWriter;
import uk.ac.bristol.star.feather.FeatherType;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;

/**
 * FeatherColumnWriter for use with StarTables.
 *
 * @author   Mark Taylor
 * @since    26 Feb 2020
 */
public abstract class StarColumnWriter implements FeatherColumnWriter {

    private final StarTable table_;
    private final int icol_;
    private final FeatherType featherType_;
    private final boolean isNullable_;

    /**
     * Constructor.
     *
     * The <code>isNullable</code> parameter only needs to be given true
     * if the writeData method cannot represent null values in its
     * byte representation.
     *
     * @param  table  inupt table
     * @param  icol   column index in input table
     * @param  featherType  data type for output column
     * @param  isNullable  true iff writeData cannot write in-band null values
     */
    protected StarColumnWriter( StarTable table, int icol,
                                FeatherType featherType, boolean isNullable ) {
        table_ = table;
        icol_ = icol;
        featherType_ = featherType;
        isNullable_ = isNullable;
    }

    /**
     * Writes the bytes consituting the data stream for this column,
     * excluding any optional validity mask.
     * Note the output does not need to be aligned on an 8-byte boundary.
     *
     * @param   out  destination stream
     * @return   information about what was actually written
     */
    public abstract DataStat writeDataBytes( OutputStream out )
            throws IOException;

    /**
     * Returns the table on which this writer is operating.
     *
     * @return   input table
     */
    public StarTable getTable() {
        return table_;
    }

    /**
     * Returns the index of the input table column which is being written.
     *
     * @return  input column index
     */
    public int getColumnIndex() {
        return icol_;
    }

    /**
     * Returns the output data type code.
     *
     * @return  feather output type
     */
    public FeatherType getFeatherType() {
        return featherType_;
    }

    /**
     * Indicates whether this writer may write a validity mask.
     *
     * @return   true iff output type supports masked null values
     */
    public boolean isNullable() {
        return isNullable_;
    }

    public String getName() {
        return table_.getColumnInfo( icol_ ).getName();
    }

    public String getUserMetadata() {
        ColumnInfo info = table_.getColumnInfo( icol_ );
        JSONObject json = new JSONObject();
        addJsonEntry( json, FeatherStarTable.UNIT_KEY, info.getUnitString() );
        addJsonEntry( json, FeatherStarTable.UCD_KEY, info.getUCD() );
        addJsonEntry( json, FeatherStarTable.UTYPE_KEY, info.getUtype() );
        addJsonEntry( json, FeatherStarTable.DESCRIPTION_KEY,
                      info.getDescription() );
        addJsonEntry( json, FeatherStarTable.SHAPE_KEY,
                      DefaultValueInfo.formatShape( info.getShape() ) );
        return json.length() > 0
             ? json.toString( 0 ).replaceAll( "\n", " " )
             : null;
    }

    public ColStat writeColumnBytes( OutputStream out ) throws IOException {

        /* Write mask, if applicable. */
        long nNull = 0;
        final long maskBytes;
        if ( isNullable_ && table_.getColumnInfo( icol_ ).isNullable() ) {
            int mask = 0;
            int ibit = 0;
            long nrow = 0;
            RowSequence rseq = table_.getRowSequence();
            try {
                while ( rseq.next() ) {
                    nrow++;
                    if ( rseq.getCell( icol_ ) == null ) {
                        nNull++;
                    }
                    else {
                        mask |= 1 << ibit;
                    }
                    if ( ++ibit == 8 ) {
                        out.write( mask );
                        ibit = 0;
                        mask = 0;
                    }
                }
                if ( ibit > 0 ) {
                    out.write( mask );
                }
            }
            finally {
                rseq.close();
            }
            long mb = ( nrow + 7 ) / 8;
            maskBytes = mb + BufUtils.align8( out, mb );
        }
        else {
            maskBytes = 0;
        }

        /* Write data. */
        DataStat dataStat = writeDataBytes( out );

        /* Package and return statistics. */
        long db = dataStat.getByteCount();
        final long rowCount = dataStat.getRowCount();
        long dataBytes = db + BufUtils.align8( out, db );
        boolean hasNull = nNull > 0;
        final long byteCount = maskBytes + dataBytes;
        final long dataOffset = maskBytes;
        final long nullCount = nNull;
        return new ColStat() {
            public long getRowCount() {
                return rowCount;
            }
            public long getByteCount() {
                return byteCount;
            }
            public long getDataOffset() {
                return dataOffset;
            }
            public long getNullCount() {
                return nullCount;
            }
        };
    }

    public abstract ItemAccumulator
        createItemAccumulator( StoragePolicy storage );

    /**
     * Adds a key-value entry to a supplied JSON object.
     *
     * @param  json  json object (map), altered on exit
     * @param  key   key to add
     * @param  value  value to add
     */
    private static void addJsonEntry( JSONObject json,
                                      String key, String value ) {
        if ( value != null && value.trim().length() > 0 ) {
            json.put( key, value );
        }
    }

    /**
     * Aggregates information about column output.
     */
    public static class DataStat {

        private final long byteCount_;
        private final long rowCount_;

        /**
         * Constructor.
         *
         * @param  byteCount  number of bytes written
         * @param  rowCount  number of rows written
         */
        public DataStat( long byteCount, long rowCount ) {
            byteCount_ = byteCount;
            rowCount_ = rowCount;
        }

        /**
         * Returns the number of bytes written.
         *
         * @return  byte count
         */
        public long getByteCount() {
            return byteCount_;
        }

        /**
         * Returns the number of rows written.
         *
         * @return  row count
         */
        public long getRowCount() {
            return rowCount_;
        }
    }
}
