package uk.ac.starlink.ttools.plot2.layer;

/**
 * Defines the mapping of coordinates in a bounded region of a
 * 2-d data space to a single bin index.
 *
 * @author   Mark Taylor
 * @since    24 Apr 2026
 */
public class GridPixer {

    private final GridSpec xgrid_;
    private final GridSpec ygrid_;
    private final Gridder gridder_;

    /**
     * Constructor.
     *
     * @param  xgrid  1-d grid specification in X direction
     * @param  ygrid  1-d grid specification in Y direction
     */
    public GridPixer( GridSpec xgrid, GridSpec ygrid ) {
        xgrid_ = xgrid;
        ygrid_ = ygrid;
        gridder_ = new Gridder( xgrid.getBinCount(), ygrid.getBinCount() );
    }

    /**
     * Returns the 1-d grid specification in the X direction.
     *
     * @return   X grid spec
     */
    public GridSpec getGridX() {
        return xgrid_;
    }

    /**
     * Returns the 1-d grid specification in the Y direction.
     *
     * @return  Y grid spec
     */
    public GridSpec getGridY() {
        return ygrid_;
    }

    /**
     * Returns the number of bins covered by this grid.
     *
     * @return   bin count
     */
    public int getBinCount() {
        return gridder_.getLength();
    }

    /**
     * Calculates the grid index for a given position in data space.
     *
     * @param   dpos  position in data coordinates
     * @return   grid index, or negative number if off-grid
     */
    public int getBinIndex( double[] dpos ) {
        double dx = dpos[ 0 ];
        double dy = dpos[ 1 ];

        /* Test against the data bounds we know apply for this GridPixer.
         * It might seem more straightforward to use the bin bounds here,
         * but this avoids having to calculate bin index for values
         * out of range, and and it also avoids some nasty problems
         * with bad data values (e.g. negative values for a
         * logarithmic mapper). */
        if ( xgrid_.containsDataPoint( dx ) &&
             ygrid_.containsDataPoint( dy ) ) {
            int ix = xgrid_.getMapper().getBinIndex( dx );
            int iy = ygrid_.getMapper().getBinIndex( dy );

            /* Unfortunately, small numerical errors in the mapper
             * conversions can lead to values falling just outside the
             * bins in any case.  So it's necessary that the implementation
             * of getBinIndex checks against bin indices as well. */
            assert xgrid_.nearlyContainsBin( ix )
                && ygrid_.nearlyContainsBin( iy );
            return getBinIndex( ix, iy );
        }
        return -1;
    }

    /**
     * Returns the grid index given the X and Y grid indices.
     *
     * @param   ix  X gridder bin index
     * @param   iy  Y gridder bin index
     * @return   grid index for bin list, or negative number if off-grid
     */
    public int getBinIndex( int ix, int iy ) {
        return xgrid_.containsBin( ix ) && ygrid_.containsBin( iy )
             ? gridder_.getIndex( xgrid_.getBinOffset( ix ),
                                  ygrid_.getBinOffset( iy ) )
             : -1;
    }

    /**
     * Returns the area of a bin in data units.
     * If either axis is non-linear, this will be a strange quantity.
     *
     * @param  ctype  combination type
     * @return  multiplication for bin values
     */
    public double getBinFactor( Combiner.Type ctype ) {
        return ctype.getBinFactor( xgrid_.getBinWidth() *
                                   ygrid_.getBinWidth() );
    }

    /**
     * Indicates whether this pixer is a superset of the given pixer.
     *
     * @param  other  comparison object
     * @return   true iff this object contains all the information
     *           contained by <code>other</code>
     */
    public boolean contains( GridPixer other ) {
        return xgrid_.contains( other.xgrid_ )
            && ygrid_.contains( other.ygrid_ );
    }
}
