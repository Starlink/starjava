package uk.ac.starlink.topcat.plot;

/**
 * Object which represents a point to be plotted on a PlotVolume.
 * This class handles only object comparison; it will have to be subclassed
 * to contain some more information before it can do anything much useful.
 *
 * @since   26 Mar 2007
 * @author  Mark Taylor
 */
public class Point3D implements Comparable {

    private final int iseq_;
    private final float z_;
    private final int plus_;
    private final int minus_;

    /**
     * Constructs a new Point3D.
     *
     * @param   iseq   sequence value, used as a tie-breaker for comparisons
     * @param   z   Z coordinate, used for sorting
     * @param   sortAscending  sense of the comparison operation
     */
    Point3D( int iseq, double z, boolean sortAscending ) {
        z_ = (float) z;
        iseq_ = iseq;
        plus_ = sortAscending ? +1 : -1;
        minus_ = - plus_;
    }

    /**
     * Returns the Z coordinate.
     *
     * @return   z
     */
    public double getZ() {
        return (double) z_;
    }

    /**
     * Compares Point3D objects according to their Z value (ascending or
     * descending as per constructor).  Objects with the same Z value
     * use sequence ID as a tie breaker.
     */
    public int compareTo( Object other ) {
        Point3D o = (Point3D) other;
        if ( this.z_ > o.z_ ) {
            return plus_;
        }
        else if ( this.z_ < o.z_ ) {
            return minus_;
        }
        else {
            if ( this.iseq_ < o.iseq_ ) {
                return plus_;
            }
            else if ( this.iseq_ > o.iseq_ ) {
                return minus_;
            }
            else {
                int ht = System.identityHashCode( this );
                int ho = System.identityHashCode( other );
                if ( ht > ho ) {
                    return plus_;
                }
                else if ( ht < ho ) {
                    return minus_;
                }
                else {
                    assert this == other;
                    return 0;
                }
            }
        }
    }
}
