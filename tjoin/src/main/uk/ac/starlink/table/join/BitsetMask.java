package uk.ac.starlink.table.join;

import java.util.BitSet;
import uk.ac.starlink.util.IntList;

/**
 * HealpixMask implementation based on a BitSet representing all the
 * pixels at a given HEALPix order.
 *
 * <p>This implementation is not very sophisticated
 * (it doesn't attempt a multi-order representation),
 * but it's fairly efficient as long as the order (resolution)
 * required is modest.
 * A value like 6 is probably reasonable; 49k pixels, 6kbyte storage.
 *
 * @author  Mark Taylor
 * @since   8 Jun 2022
 */
public class BitsetMask implements HealpixMask {

    private final int order_;
    private final int npix_;
    private final BitSet flags_;

    /** Suitable HEALPix order for general use. */
    public static final int DEFAULT_ORDER = 6;

    /**
     * Constructs a new empty mask with the default order.
     */
    public BitsetMask() {
        this( DEFAULT_ORDER );
    }

    /**
     * Constructs a new empty mask with a given order.
     *
     * @param  order  HEALPix order
     */
    public BitsetMask( int order ) {
        order_ = order;
        long npixl = 12L << 2 * order;
        npix_ = (int) npixl;
        if ( npix_ != npixl ) {
            throw new IllegalArgumentException( "Order too high" );
        }
        flags_ = new BitSet( npix_ );
    }

    public boolean isEmpty() {
        return flags_.isEmpty();
    }

    public void intersection( HealpixMask other ) {
        flags_.and( ((BitsetMask) other).flags_ );
    }

    public void union( HealpixMask other ) {
        flags_.or( ((BitsetMask) other).flags_ );
    }

    public double getSkyFraction() {
        return flags_.cardinality() * 1.0 / npix_;
    }

    public void addPixel( int order, long ipix ) {
        int maskOrder = order_;
        int pixOrder = order;
        if ( pixOrder == maskOrder ) {
            flags_.set( (int) ipix );
        }
        else if ( pixOrder > maskOrder ) {
            flags_.set( (int) ( ipix >> 2 * ( pixOrder - maskOrder ) ) );
        }
        else {
            assert maskOrder > pixOrder;
            int shift = 2 * ( maskOrder - pixOrder );
            int iplo = (int) ( ipix << shift );
            int iphi = (int) ( ( ipix + 1 ) << shift );
            flags_.set( iplo, iphi );
        }
    }

    public PixelTester createPixelTester() {
        final int maskOrder = order_;

        /* BitSet claims that it's not thread-safe.
         * But I'm hoping it's OK for concurrent reads. */
        final BitSet flags = (BitSet) flags_.clone();
        return ( pixOrder, ipix ) -> {
            if ( pixOrder == maskOrder ) {
                return flags.get( (int) ipix );
            }
            else if ( pixOrder > maskOrder ) {
                return flags
                      .get( (int) ( ipix >> 2 * ( pixOrder - maskOrder ) ) );
            }
            else {
                assert maskOrder > pixOrder;
                int shift = 2 * ( maskOrder - pixOrder );
                int iplo = (int) ( ipix << shift );
                int iphi = (int) ( ( ipix + 1 ) << shift );
                int nextSet = flags.nextSetBit( iplo );
                return nextSet >= 0 && nextSet < iphi;
            }
        };
    }

    /**
     * Returns the HEALPix order of the pixels stored by this mask.
     *
     * @return  healpix order
     */
    public int getOrder() {
        return order_;
    }

    /**
     * Returns an array of the pixels contained by this mask.
     * This is mainly intended for diagnostic purposes, it may not be
     * very efficient.
     *
     * @return  pixel list at this mask's order
     */
    public int[] getPixels() {
        IntList list = new IntList();
        for ( int ib = flags_.nextSetBit( 0 ); ib >= 0;
              ib = flags_.nextSetBit( ib + 1 ) ) {
            list.add( ib );
            if ( ib == Integer.MAX_VALUE ) {
                break;
            }
        }
        return list.toIntArray();
    }
}
