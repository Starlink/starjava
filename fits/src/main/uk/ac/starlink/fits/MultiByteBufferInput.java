package uk.ac.starlink.fits;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Random-access BasicInput implementation based on a supplied array
 * of byte buffers.
 *
 * @author   Mark Taylor
 * @since    19 Mar 2021
 */
public class MultiByteBufferInput extends BlockInput {

    private final ByteBuffer[] bufs_;
    private final long[] istarts_;

    /**
     * Constructor.
     *
     * @param   bufs   byte buffers holding data
     */
    public MultiByteBufferInput( ByteBuffer[] bufs ) {
        super( bufs.length );
        bufs_ = bufs;
        int nb = bufs.length;
        istarts_ = new long[ nb + 1 ];
        for ( int ib = 0; ib < nb; ib++ ) {
            istarts_[ ib + 1 ] = istarts_[ ib ] + bufs[ ib ].capacity();
        }
    }

    public int[] getBlockPos( long offset ) {
        int ip = Arrays.binarySearch( istarts_, offset );
        return ip < 0
             ? new int[] { -2 - ip, (int) ( offset - istarts_[ -2 - ip ] ) }
             : new int[] { ip, 0 };
    }

    public long getBlockOffset( int iblock, int offsetInBlock ) {
        return istarts_[ iblock ] + offsetInBlock;
    }

    protected ByteBuffer acquireBlock( int iblock ) {
        return bufs_[ iblock ];
    }

    public void close() {
        super.close();
    }
}
