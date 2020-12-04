package uk.ac.starlink.ttools.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Quantiler that retains all data samples, sorts them when ready,
 * and uses the sorted array to answer questions about quantiles.
 * This is exact, but consumes approximately 8 bytes per sample,
 * so not suited for unlimited sized datasets.
 *
 * @author   Mark Taylor
 * @since    3 Dec 2020
 */
public class SortQuantiler implements Quantiler {

    private final int blocksize_;
    private List<double[]> fullBlocks_;
    private int ix_;
    private double[] block_;
    private double[] sorted_;
    private int nsort_;

    /** Default block size. */
    public static final int DFLT_BLOCKSIZE = 16 * 1024;

    /**
     * Constructor with default block size.
     */
    public SortQuantiler() {
        this( DFLT_BLOCKSIZE );
    }

    /**
     * Constructor with supplied block size.
     *
     * @param blocksize  tuning parameter, block size in bytes
     */
    public SortQuantiler( int blocksize ) {
        blocksize_ = blocksize;
        fullBlocks_ = new ArrayList<double[]>();
        ix_ = blocksize_;
    }

    public void acceptDatum( double value ) {
        if ( ! Double.isNaN( value ) ) {
            if ( ix_ > blocksize_ - 1 ) {
                if ( block_ != null ) {
                    fullBlocks_.add( block_ );
                }
                block_ = new double[ blocksize_ ];
                ix_ = 0;
            }
            block_[ ix_++ ] = value;
        }
    }

    public void addQuantiler( Quantiler o ) {
        SortQuantiler other = (SortQuantiler) o;
        fullBlocks_.addAll( other.fullBlocks_ );
        if ( other.block_ != null ) {
            for ( int i = 0; i < other.ix_; i++ ) {
                acceptDatum( other.block_[ i ] );
            }
        }
    }

    public void ready() {
        if ( sorted_ == null ) {
            final double[] all;
            final int nall;
            if ( block_ == null ) {
                assert fullBlocks_.size() == 0;
                nall = 0;
                all = new double[ 0 ];
            }
            else if ( ix_ > 0 || fullBlocks_.size() > 1 ) {
                int ns = ix_;
                for ( double[] block : fullBlocks_ ) {
                    ns += block.length;
                }
                all = new double[ ns ];
                nall = ns;
                int ipos = 0;
                for ( double[] block : fullBlocks_ ) {
                    System.arraycopy( block, 0, all, ipos, block.length );
                    ipos += block.length;
                }
                if ( block_ != null ) {
                    System.arraycopy( block_, 0, all, ipos, ix_ );
                }
                ipos += ix_;
                assert ipos == nall;
            }
            else {
                all = fullBlocks_.get( 0 );
                nall = ix_;
            }
            block_ = null;
            fullBlocks_ = null;
            Arrays.parallelSort( all, 0, nall );
            sorted_ = all;
            nsort_ = nall;
        }
    }

    public double getValueAtQuantile( double quantile ) {
        if ( sorted_ == null ) {
            throw new IllegalStateException( "Not ready" );
        }
        if ( quantile >= 0 && quantile <= 1 ) {
            if ( nsort_ > 1 ) {
                double dpos = quantile * ( nsort_ - 1 );
                int ipos = (int) dpos;
                double frac = dpos - ipos;
                double value = sorted_[ ipos ];
                if ( frac > 0 ) {
                    value += frac * ( sorted_[ ipos + 1 ] - sorted_[ ipos ] );
                }
                return value;
            }
            else if ( nsort_ == 1 ) {
                return sorted_[ 0 ];
            }
            else {
                assert nsort_ == 0;
                return Double.NaN;
            }
        }
        else {
            throw new IllegalArgumentException( "Quantile out of range 0..1" );
        }
    }
}
