package uk.ac.starlink.ttools.plot;

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
     * Returns a comparator which can be used to sort Point3D objects.
     *
     * @param   zAscending  true for ascending Z
     * @param   seqAscending  true for ascending sequence ID
     * @return  comparator
     */
    public static Comparator getComparator( boolean zAscending,
                                            boolean seqAscending ) {
        return new Point3DComparator( zAscending, seqAscending );
    }

    /**
     * Compares Point3D objects according to their Z value (ascending or
     * descending as per constructor).  Objects with the same Z value
     * use sequence ID as a tie breaker.
     */
    private static class Point3DComparator implements Comparator {
        private final int zPlus_;
        private final int zMinus_;
        private final int seqPlus_;
        private final int seqMinus_;

        /**
         * Constructor.
         *
         * @param   ascending   true to sort up, false to sort down
         */
        Point3DComparator( boolean zAscending, boolean seqAscending ) {
            zPlus_ = zAscending ? +1 : -1;
            zMinus_ = - zPlus_;
            seqPlus_ = seqAscending ? +1 : -1;
            seqMinus_ = - seqPlus_;
        }

        public int compare( Object o1, Object o2 ) {
            Point3D p1 = (Point3D) o1;
            Point3D p2 = (Point3D) o2;
            float z1 = p1.z_;
            float z2 = p2.z_;
            if ( z1 < z2 ) {
                return zMinus_;
            }
            else if ( z1 > z2 ) {
                return zPlus_;
            }
            else {
                int i1 = p1.iseq_;
                int i2 = p2.iseq_;
                if ( i1 < i2 ) {
                    return seqMinus_;
                }
                else if ( i1 > i2 ) {
                    return seqPlus_;
                }
                else {
                    int h1 = System.identityHashCode( p1 );
                    int h2 = System.identityHashCode( p2 );
                    if ( h1 < h2 ) {
                        return -1;
                    }
                    else if ( h1 > h2 ) {
                        return +1;
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
