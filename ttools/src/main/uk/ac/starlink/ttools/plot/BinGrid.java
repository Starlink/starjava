package uk.ac.starlink.ttools.plot;

import java.util.Arrays;

/**
 * Stores a grid of bins which contain sums, which may be simple counts or
 * weighted sums.  This is effectively the data model for a two-dimensional 
 * possibly weighted histogram or, to put it another way, it's an image.
 *
 * @author   Mark Taylor
 * @since    1 Dec 2005
 */
public class BinGrid {

    private final int xsize_;
    private final int ysize_;
    private final int npix_;
    private final double[] sums_;
    private double minSum_;
    private double maxSum_;
    private double[] blockSorted_;

    /**
     * Constructs a new grid.
     *
     * @param   xsize  number of bins (pixels) in X direction
     * @param   ysize  number of bins (pixels) in Y direction
     */
    public BinGrid( int xsize, int ysize ) {
        xsize_ = xsize;
        ysize_ = ysize;
        npix_ = xsize_ * ysize_;
        sums_ = new double[ npix_ ];
    }

    /**
     * Returns the number of bins (pixels) in the X direction.
     *
     * @return  x dimension
     */
    public int getSizeX() {
        return xsize_;
    }

    /**
     * Returns the number of bins (pixels) in the Y direction.
     *
     * @return  y dimension
     */
    public int getSizeY() {
        return ysize_;
    }

    /**
     * Returns the sum in a given bin (value at a given pixel).
     *
     * @param  ix  x coordinate
     * @param  iy  y coordinate
     */
    public double getSum( int ix, int iy ) {
        return sums_[ getIndex( ix, iy ) ];
    }

    /**
     * Returns the largest value which exists in any of the bins.
     *
     * @return   maximum pixel value
     */
    public double getMaxSum() {
        return maxSum_;
    }

    /**
     * Returns the smallest value which exists in any of the bins.
     *
     * @return   minimum pixel value
     */
    public double getMinSum() {
        return minSum_;
    }

    /**
     * Returns the cut value associated with a given fractional value
     * (between 0 and 1).  If <code>frac=0.25</code> you'll get the first
     * quartile of sum values currently in the histogram.
     *
     * @param  frac  fraction for which cut is required (0..1)
     * @return  value below which <code>frac</code> of the bins have occupancies
     */
    public double getCut( double frac ) {
        if ( frac < 0.0 || frac > 1.0 ) {
            throw new IllegalArgumentException();
        }
        double[] blox = getBlockSorted();
        int index = (int) ( blox.length * frac );
        return blox[ index ];
    }

    /**
     * Returns a sorted array which represents the values in this grid.
     * The values may be blocked, so that each element of the returned array
     * corresponds to a number of pixels of adjacent values (its mean or
     * similar).
     *
     * @return   blocked sorted array
     */
    private double[] getBlockSorted() {
        if ( blockSorted_ == null ) {
            double[] sorted = (double[]) sums_.clone();
            Arrays.sort( sorted );
            int blockSize = Math.max( 1, npix_ / 1000 );
            int nblock = npix_ / blockSize;
            int b2 = blockSize / 2;
            double[] blocked = new double[ nblock ];
            int ipix = 0;
            for ( int ib = 0; ib < nblock; ib++ ) {
                int ip = Math.min( ib * blockSize + b2, npix_ - 1 );
                blocked[ ib ] = sorted[ ip ];
            }
            blockSorted_ = blocked;
        }
        return blockSorted_;
    }

    /**
     * Adds a data point to this histogram.  The sum in the bin 
     * <code>(ix,iy)</code> is incremented by <code>weight</code>.
     *
     * @param  ix  X grid coordinate
     * @param  iy  Y grid coordinate
     * @param  weight  weight
     */
    public void submitDatum( int ix, int iy, double weight ) {
        int ip = getIndex( ix, iy );
        double sum = sums_[ ip ];
        sum += weight;
        sums_[ ip ] = sum;
        minSum_ = Math.min( minSum_, sum );
        maxSum_ = Math.max( maxSum_, sum );
    }

    /**
     * Recalculates invariants.  This must be called if the sums arrray
     * is modified directly.
     */
    public void recalculate() {
        minSum_ = 0;
        maxSum_ = 0;
        for ( int ip = 0; ip < npix_; ip++ ){ 
            double sum = sums_[ ip ];
            minSum_ = Math.min( minSum_, sum );
            maxSum_ = Math.max( maxSum_, sum );
        }
    }

    /**
     * Returns an array of bytes representing the values in this grid.
     * The values are scaled to occupy the full range 0-255 or
     * roughly so.
     * Cut values are supplied; any bin values below <code>loCut</code>
     * will get a result value of 0, any ones above <code>hiCut</code>
     * will get a result value of 255.
     * The ordering of pixels is that X values change most rapidly
     * and Y values decrease.  This is the order suitable for AWT images.
     *
     * <p><strong>Note</strong> the values must be interpreted as 
     * unsigned 8-bit values (value = 0x000000ff & (int)getBytes()[i]).
     *
     * @param   loCut  lowest distinguished sum value
     * @param   hiCut  highest distinguished sum value
     * @param   log    true iff you want logarithmic scalling of the colours
     * @return  scaled array of unsigned byte values representing grid data
     */
    public byte[] getBytes( double loCut, double hiCut, boolean log ) {
        double scale1 = log ? Math.log( hiCut / loCut )
                            : hiCut - loCut;
        double scale = 255.0 / scale1;
        byte[] buf = new byte[ npix_ ];
        for ( int ipix = 0; ipix < npix_; ipix++ ) {
            double sum = sums_[ ipix ];
            int ival;
            if ( sum <= loCut ) {
                ival = 0;
            }
            else if ( sum >= hiCut ) {
                ival = 255;
            }
            else {
                double val = log ? Math.log( sum / loCut )
                                 : sum - loCut;
                ival = (int) ( val * scale );
            }
            buf[ ipix ] = (byte) ival;
        }
        return buf;
    }

    /**
     * Returns the raw histogram data held by this grid.
     * The result is an array where each element holds the number of 
     * points which have fallen in the corresponding bin.
     * Note the ordering is image-like, as for {@link #getBytes} - 
     * X values change most rapidly and Y values decrease.
     *
     * @return  sum array
     */
    public double[] getSums() {
        return sums_;
    }

    /**
     * Maps the X and Y coordinates to a flat array index.
     *
     * @param   ix  X coordinate
     * @param   iy  Y coordinate
     * @return   pixel index
     */
    private int getIndex( int ix, int iy ) {
        return ix + xsize_ * ( ysize_ - 1 - iy );
    }
}
