package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.Scale;

/**
 * Specifies bin geometry for a 1-dimensional grid.
 *
 * @author   Mark Taylor
 * @since    24 Apr 2026
 */
public class GridSpec {

    private final Scale scale_;
    private final double binWidth_;
    private final double phase_;
    private final double dlo_;
    private final double dhi_;
    private final BinMapper mapper_;
    private final int ilo_;
    private final int ihi_;

    /**
     * Constructor.
     *
     * @param  scale    axis scale
     * @param  binWidth  bin width in scale units
     * @param  phase   scaled phase (non-degenerate range is 0..1)
     * @param  drange  2-element [lo,hi] array giving required
     *                 minimum extent in data coordinates
     */
    public GridSpec( Scale scale, double binWidth, double phase,
                     double[] drange ) {
        scale_ = scale;
        binWidth_ = binWidth;
        phase_ = phase;
        mapper_ = new BinMapper( scale, binWidth, phase, drange[ 0 ] );
        int i0 = mapper_.getBinIndex( drange[ 0 ] );
        int i1 = mapper_.getBinIndex( drange[ 1 ] );
        double[] dlimits0 = mapper_.getBinLimits( i0 );
        double[] dlimits1 = mapper_.getBinLimits( i1 );
        ilo_ = Math.min( i0, i1 );
        ihi_ = Math.max( i0, i1 );
        dlo_ = Math.min( dlimits0[ 0 ], dlimits1[ 0 ] );
        dhi_ = Math.max( dlimits0[ 1 ], dlimits1[ 1 ] );
    }

    /**
     * Returns the axis scaling.
     *
     * @return  scale
     */
    public Scale getScale() {
        return scale_;
    }

    /**
     * Returns the bin width in scale units.
     *
     * @return  bin width
     */
    public double getBinWidth() {
        return binWidth_;
    }

    /**
     * Returns the number of bins corresponding to the data extent
     * of this grid.
     *
     * @return  bin count
     */
    public int getBinCount() {
        return ihi_ - ilo_ + 1;
    }

    /**
     * Returns the mapper from axis data values to bin indices.
     *
     * @return  bin mapper
     */
    public BinMapper getMapper() {
        return mapper_;
    }

    /**
     * Indicates whether this specification is a superset of the given one.
     *
     * @param  other  comparison object
     * @return   true iff this object contains all the information
     *           contained by <code>other</code>
     */
    public boolean contains( GridSpec other ) {
        return this.binWidth_ == other.binWidth_
            && this.phase_ == other.phase_
            && this.dlo_ <= other.dlo_
            && this.dhi_ >= other.dhi_;
    }

    /**
     * Tests whether a given value in data coordinates falls within this
     * grid's bounds.
     *
     * @param   d  test data point
     * @return   true iff d falls within the grid bounds of this object
     */
    public boolean containsDataPoint( double d ) {
        return d >= dlo_ && d < dhi_;
    }

    /**
     * Tests whether a given bin index falls within this grid's bounds.
     *
     * @param   ibin   test bin index
     * @return  true iff ibin identifies one of this grid's bins
     */
    public boolean containsBin( int ibin ) {
        return ibin >= ilo_ && ibin <= ihi_;
    }

    /**
     * Tests whether a given bin index either
     * falls within this grid's bounds or is just outside them.
     *
     * @param  ibin  bin index
     * @return  true iff ibin is no more than one away from this grid
     */
    public boolean nearlyContainsBin( int ibin ) {
        return ibin >= ilo_ - 1 && ibin <= ihi_ + 1;
    }

    /**
     * Returns the offset index for a given bin.
     *
     * @param  ibin  bin index as returned by the mapper
     * @return  offset bin index in the range 0..(ihi_-ilo_)
     */
    public int getBinOffset( int ibin ) {
        return ibin - ilo_;
    }

    /**
     * Returns the range of bin indices corresponding to a range in
     * data coordinates for this grid.
     *
     * @param   dataRange  2-element array (lo,hi) of data values
     * @return  2-element array (lo,hi) of bin indices
     */
    public int[] getBinRange( double[] dataRange ) {
        int i0 = mapper_.getBinIndex( dataRange[ 0 ] );
        int i1 = mapper_.getBinIndex( dataRange[ 1 ] );
        return new int[] { Math.min( i0, i1 ), Math.max( i0, i1 ) };
    }
}
