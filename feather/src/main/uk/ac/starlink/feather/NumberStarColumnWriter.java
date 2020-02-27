package uk.ac.starlink.feather;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import uk.ac.bristol.star.feather.FeatherType;
import uk.ac.starlink.table.ByteStore;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;

/**
 * StarColumnWriter implementation for numeric values.
 *
 * @author   Mark Taylor
 * @since    27 Feb 2020
 */
public abstract class NumberStarColumnWriter extends StarColumnWriter {

    private final byte[] blank_;
    private final int itemSize_;

    /**
     * Constructor.
     *
     * @param  table  input table
     * @param  icol   column index
     * @param  featherType   output data type
     * @param  isNullable  true iff no in-band blank representation exists
     * @param  blank  byte pattern of blank value;
     *                the length of this array also defines the output item
     *                size in bytes
     */
    protected NumberStarColumnWriter( StarTable table, int icol,
                                      FeatherType featherType,
                                      boolean isNullable, byte[] blank ) {
        super( table, icol, featherType, isNullable );
        blank_ = blank.clone();
        itemSize_ = blank.length;
    }

    /**
     * Writes the bytes for a given typed value.
     *
     * @param  out  destination stream
     * @param  value   non-null typed value to write
     */
    protected abstract void writeNumber( OutputStream out, Number value )
            throws IOException;

    public DataStat writeDataBytes( OutputStream out ) throws IOException {
        final int icol = getColumnIndex();
        RowSequence rseq = getTable().getRowSequence();
        long nrow = 0;
        try {
            while ( rseq.next() ) {
                nrow++;
                Object item = rseq.getCell( icol );
                if ( item != null ) {
                    writeNumber( out, (Number) item );
                }
                else {
                    out.write( blank_ );
                }
            }
        }
        finally {
            rseq.close();
        }
        long nbyte = nrow * itemSize_;
        return new DataStat( nbyte, nrow );
    }

    public ItemAccumulator createItemAccumulator( StoragePolicy storage ) {
        final ByteStore dataStore = storage.makeByteStore();
        final OutputStream dataOut =
            new BufferedOutputStream( dataStore.getOutputStream() );
        return new AbstractItemAccumulator( storage, isNullable() ) {
            long nbData;
            public void addDataItem( Object item ) throws IOException {
                if ( item != null ) {
                    writeNumber( dataOut, (Number) item );
                }
                else {
                    dataOut.write( blank_ );
                }
                nbData += itemSize_;
            }
            public long writeDataBytes( OutputStream out ) throws IOException {
                dataOut.close();
                dataStore.copy( out );
                dataStore.close();
                return nbData;
            }
            public void closeData() throws IOException {
                dataOut.close();
                dataStore.close();
            }
        };
    }
}
