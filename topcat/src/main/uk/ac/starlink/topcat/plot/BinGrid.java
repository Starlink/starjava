package uk.ac.starlink.topcat.plot;

/**
 * Stores a grid of bins which contain counts.  This is effectively
 * the data model for a two-dimensional histogram or, to put it another
 * way, it's an image.
 *
 * @author   Mark Taylor
 * @since    1 Dec 2005
 */
public class BinGrid {

    private final int xsize_;
    private final int ysize_;
    private final int npix_;
    private final int[] counts_;
    private int maxCount_;
    private long totalPoints_;
    private int[] binOccupancies_;

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
        counts_ = new int[ npix_ ];
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
     * Returns the count in a given bin (value at a given pixel).
     *
     * @param  ix  x coordinate
     * @param  iy  y coordinate
     */
    public int getCount( int ix, int iy ) {
        return counts_[ getIndex( ix, iy ) ];
    }

    /**
     * Returns the largest value which exists in any of the bins.
     *
     * @return   maximum pixel value
     */
    public int getMaxCount() {
        return maxCount_;
    }

    /**
     * Returns the cut value associated with a given fractional value
     * (between 0 and 1).  If <code>frac=0.25</code> you'll get the first
     * quartile of count values currently in the histogram.
     *
     * @param  frac  fraction for which cut is required (0..1)
     * @return  value below which <code>frac</code> of the bins have occupancies
     */
    public int getCut( double frac ) {
        if ( frac < 0.0 || frac > 1.0 ) {
            throw new IllegalArgumentException();
        }
        int[] binocs = getBinOccupancies();
        long sum = 0;
        for ( int i = 0; i < binocs.length; i++ ) {
            if ( (double) sum / (double) npix_ > frac ) {
                return i;
            }
            sum += binocs[ i ];
        }
        assert sum == npix_ : ("sum: " + sum + " npix: " + npix_);
        return binocs.length;
    }

    /**
     * Returns an array which represents a histogram of the counts currently
     * in this grid.  The <code>n</code><sup>th</sup> element 
     * of this array gives the number of bins with a value of 
     * exactly <code>n</code>.
     *
     * @return   bin occupancy histogram array 
     */
    private int[] getBinOccupancies() {
        if ( binOccupancies_ == null ) {
            int[] binocs = new int[ maxCount_ + 1 ];
            for ( int ipix = 0; ipix < npix_; ipix++ ) {
                int count = counts_[ ipix ];
                binocs[ count ]++;
            }
            binOccupancies_ = binocs;
        }
        return binOccupancies_;
    }

    /**
     * Adds a data point to this histogram.  The count in the bin 
     * <code>(ix,iy)</code> is incremented by 1.
     *
     * @param  ix  X grid coordinate
     * @param  iy  Y grid coordinate
     */
    public void submitDatum( int ix, int iy ) {
        int ip = getIndex( ix, iy );
        counts_[ ip ]++;
        maxCount_ = Math.max( maxCount_, counts_[ ip ] );
        totalPoints_++;
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
     * @param   loCut  lowest distinguished count value
     * @param   hiCut  highest distinguished count value
     * @return  scaled array of unsigned byte values representing grid data
     */
    public byte[] getBytes( int loCut, int hiCut ) {
        double scale = 255.0 / Math.max( hiCut - loCut + 2, 1 );
        byte[] buf = new byte[ npix_ ];
        for ( int ipix = 0; ipix < npix_; ipix++ ) {
            int count = counts_[ ipix ];
            int ival;
            if ( count < loCut ) {
                ival = 0;
            }
            else if ( count > hiCut ) {
                ival = 255;
            }
            else {
                ival = (int) ( ( count - loCut + 1 ) * scale );
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
     * @return  histogram count array
     */
    public int[] getCounts() {
        return counts_;
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
