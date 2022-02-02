package uk.ac.starlink.tfcat;

/**
 * Represents a linear ring as defined in the TFCat specification
 * (from the GeoJSON specification).
 *
 * @author   Mark Taylor
 * @since    9 Feb 2022
 */
public class LinearRing {

    private final Position[] distinctPositions_;

    /**
     * Constructor.
     *
     * @param  distinctPositions  positions defining the ring;
     *                            the last one is <em>not</em> a duplicate
     *                            of the first
     */
    protected LinearRing( Position[] distinctPositions ) {
        distinctPositions_ = distinctPositions;
    }

    /**
     * Returns an array of the positions defining this ring;
     * the last one is <em>not</em> a duplicate of the first.
     *
     * @return  array of distinct positions, length of the number of vertices
     */
    public Position[] getDistinctPositions() {
        return distinctPositions_;
    }

    /**
     * Indicates the winding direction for this ring.
     *
     * @return   true for clockwise winding direction, false for anticlockwise
     */
    public boolean isClockwise() {
        int np = distinctPositions_.length;
        double sum = 0;
        for ( int ip = 0; ip < np; ip++ ) {
            Position p0 = distinctPositions_[ ip ];
            Position p1 = distinctPositions_[ ( ip + 1 ) % np ];
            sum += ( p1.getTime() - p0.getTime() )
                 * ( p1.getSpectral() + p0.getSpectral() );
        }
        return sum > 0;
    }
}
