package uk.ac.starlink.topcat.plot;

import java.util.Comparator;

/**
 * Object which represents a point to be plotted on a PlotVolume.
 * This class handles only object comparison; it will have to be subclassed
 * to contain some more information before it can do anything much useful.
 *
 * @since   26 Mar 2007
 * @author  Mark Taylor
 */
public class Point3D {

    private final int iseq_;
    private final float z_;

    /** Comparator which sorts Point3Ds in ascending Z order. */
    public static final Comparator UP = new Point3DComparator( true );

    /** Comparator which sorts Point3Ds in descending Z order. */
    public static final Comparator DOWN = new Point3DComparator( false );

    /**
     * Constructs a new Point3D.
     *
     * @param   iseq   sequence value, used as a tie-breaker for comparisons
     * @param   z   Z coordinate, used for sorting
     */
    Point3D( int iseq, double z ) {
        z_ = (float) z;
        iseq_ = iseq;
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
    private static class Point3DComparator implements Comparator {
        private final int plus_;
        private final int minus_;

        /**
         * Constructor.
         *
         * @param   ascending   true to sort up, false to sort down
         */
        Point3DComparator( boolean ascending ) {
            plus_ = ascending ? +1 : -1;
            minus_ = - plus_;
        }

        public int compare( Object o1, Object o2 ) {
            Point3D p1 = (Point3D) o1;
            Point3D p2 = (Point3D) o2;
            float z1 = p1.z_;
            float z2 = p2.z_;
            if ( z1 < z2 ) {
                return minus_;
            }
            else if ( z1 > z2 ) {
                return plus_;
            }
            else {
                int i1 = p1.iseq_;
                int i2 = p2.iseq_;
                if ( i1 < i2 ) {
                    return minus_;
                }
                else if ( i1 > i2 ) {
                    return plus_;
                }
                else {
                    int h1 = System.identityHashCode( p1 );
                    int h2 = System.identityHashCode( p2 );
                    if ( h1 < h2 ) {
                        return minus_;
                    }
                    else if ( h1 > h2 ) {
                        return plus_;
                    }
                    else {
                        assert p1 == p2;
                        return 0;
                    }
                }
            }
        }
    }
}
