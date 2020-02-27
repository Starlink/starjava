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
 * StarColumnWriter implementation for boolean values.
 *
 * @author   Mark Taylor
 * @since    27 Feb 2020
 */
public class BooleanStarColumnWriter extends StarColumnWriter {

    /**
     * Constructor.
     *
     * @param   table  table
     * @param   icol  column index
     */
    public BooleanStarColumnWriter( StarTable table, int icol ) {
        super( table, icol, FeatherType.BOOL, true );
    }

    public DataStat writeDataBytes( OutputStream out ) throws IOException {
        final int icol = getColumnIndex();
        RowSequence rseq = getTable().getRowSequence();
        int mask = 0;
        int ibit = 0;
        long nrow = 0;
        try {
            while ( rseq.next() ) {
                nrow++;
                if ( Boolean.TRUE.equals( rseq.getCell( icol ) ) ) {
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
        long nbyte = ( nrow + 7 ) / 8;
        return new DataStat( nbyte, nrow );
    }

    public ItemAccumulator createItemAccumulator( StoragePolicy storage ) {
        final ByteStore dataStore = storage.makeByteStore();
        final OutputStream dataOut =
            new BufferedOutputStream( dataStore.getOutputStream() );
        return new AbstractItemAccumulator( storage, true ) {
            int mask;
            int ibit;
            long nbyte;
            long nrow;
            public void addDataItem( Object item ) throws IOException {
                nrow++;
                if ( Boolean.TRUE.equals( item ) ) {
                    mask |= 1 << ibit;
                }
                if ( ++ibit == 8 ) {
                    dataOut.write( mask );
                    nbyte++;
                    ibit = 0;
                    mask = 0;
                }
            }
            public long writeDataBytes( OutputStream out ) throws IOException {
                if ( ibit > 0 ) {
                    dataOut.write( mask );
                    nbyte++;
                }
                dataOut.close();
                dataStore.copy( out );
                dataStore.close();
                assert nbyte == ( nrow + 7 ) / 8;
                return nbyte;
            }
            public void closeData() throws IOException {
                dataOut.close();
                dataStore.close();
            }
        };
    }
}
