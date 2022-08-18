package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Defines how to map an XYArrayData object to a definite position
 * in data space.
 *
 * @author   Mark Taylor
 * @since    18 Aug 2022
 */
@Equality
public abstract class XYArrayPlacement {

    private final String name_;
    private final String description_;

    /** Uses first X,Y position in arrays. */
    public static final XYArrayPlacement FIRST =
        createFractionPlacement( "First", "first point in arrays", 0.0 );

    /** Uses middle X,Y position in arrays. */
    public static final XYArrayPlacement MID = 
        createFractionPlacement( "Mid", "middle point in arrays", 0.5 );

    /** Uses final X,Y position in arrays. */
    public static final XYArrayPlacement LAST =
        createFractionPlacement( "Last", "final point in arrays", 1.0 );

    /** Uses the position with the maximal X value. */
    public static final XYArrayPlacement XMAX =
        createExtremumPlacement( "XMax", false, true );

    /** Uses the position with the minimal X value. */
    public static final XYArrayPlacement XMIN =
        createExtremumPlacement( "XMin", false, false );

    /** Uses the position with the maximal Y value. */
    public static final XYArrayPlacement YMAX =
        createExtremumPlacement( "YMax", true, true );

    /** Uses the position with the minimal Y value. */
    public static final XYArrayPlacement YMIN =
        createExtremumPlacement( "YMin", true, false );

    /** Uses the center of gravity of all the (X,Y) values. */
    public static final XYArrayPlacement XYMEAN =
        createMeanPlacement( "XYMean" );

    /**
     * Constructor.
     *
     * @param   name   short name for presentation to users
     * @param   description  XML-friendly description of behaviour
     */
    protected XYArrayPlacement( String name, String description ) {
        name_ = name;
        description_ = description;
    }

    /**
     * Returns this placement policy's name.
     *
     * @return name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns this placement policy's description.
     *
     * @return  XML-friendly description text
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Attempts to determine the reference position of an XYArrayData object.
     * On success, the position in data coordinates is written into the
     * supplied 2-element array.
     *
     * @param  xyData  XY data, not null
     * @param  dpos  2-element array for X, Y output on success
     * @return  true for success
     */
    public abstract boolean readPosition( XYArrayData xyData, double[] dpos );

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns an instance that uses an X,Y value at a given fraction
     * of the way through the arrays.
     *
     * @param  name  name
     * @param  descrip  description
     * @param  fraction  fractional value in range 0..1
     */
    @Equality
    public static XYArrayPlacement
            createFractionPlacement( String name, String descrip,
                                     final double fraction ) {
        return new FractionPlacement( name, descrip, fraction );
    }

    /**
     * Returns an instance that uses the position where one of the
     * coordinates has a minimum or maximum value.
     *
     * @param  name  name
     * @param  isY   true to locate extremum in Y, false for X
     * @param  isMax  true for maximum, false for minimum
     * @return  new placement policy
     */
    private static XYArrayPlacement createExtremumPlacement( String name,
                                                             boolean isY,
                                                             boolean isMax ) {
        String descrip = new StringBuffer()
            .append( "(X,Y) position at which the " )
            .append( isMax ? "maximum " : "minimum " )
            .append( isY ? "Y " : "X " )
            .append( "value is located" )
            .toString();
        return new XYArrayPlacement( name, descrip ) {
            public boolean readPosition( XYArrayData xyData, double[] dpos ) {
                int n = xyData.getLength();
                double pos0 = Double.NaN;
                double pos1 = isMax ? Double.NEGATIVE_INFINITY
                                    : Double.POSITIVE_INFINITY;
                for ( int i = 0; i < n; i++ ) {
                    double v1 = isY ? xyData.getY( i ) : xyData.getX( i );
                    if ( isMax ? v1 > pos1 : v1 < pos1 ) {
                        double v0 = isY ? xyData.getX( i ) : xyData.getY( i );
                        pos0 = v0;
                        pos1 = v1;
                    }
                }
                if ( ! Double.isNaN( pos0 ) ) {
                    dpos[ isY ? 0 : 1 ] = pos0;
                    dpos[ isY ? 1 : 0 ] = pos1;
                    return true;
                }
                else {
                    return false;
                }
            }
        };
    }

    /**
     * Returns an instance that uses the mean X,Y value of all the
     * XYArrayData positions.
     *
     * @param  name  name
     * @return  new placement policy
     */
    private static XYArrayPlacement createMeanPlacement( String name ) {
        String descrip = "center of gravity of all the (X,Y) points";
        return new XYArrayPlacement( name, descrip ) {
            public boolean readPosition( XYArrayData xyData, double[] dpos ) {
                int n = xyData.getLength();
                int s0 = 0;
                double sx = 0;
                double sy = 0;
                for ( int i = 0; i < n; i++ ) {
                    double x = xyData.getX( i );
                    double y = xyData.getY( i );
                    if ( !Double.isNaN( x ) && !Double.isNaN( y ) ) {
                        s0++;
                        sx += x;
                        sy += y;
                    }
                }
                if ( s0 > 0 ) {
                    double s1 = 1.0 / s0;
                    dpos[ 0 ] = sx * s1;
                    dpos[ 1 ] = sy * s1;
                    return true;
                }
                else {
                    return false;
                }
            }
        };
    }

    /**
     * Placement implementation that uses an X,Y value at a given fraction
     * of the way through the arrays.
     */
    private static class FractionPlacement extends XYArrayPlacement {

        private final double fraction_;

        /**
         * Constructor.
         *
         * @param  name  name
         * @param  descrip  description
         * @param  fraction  fractional value in range 0..1
         */
        FractionPlacement( String name, String descrip, double fraction ) {
            super( name, descrip );
            fraction_ = fraction;
        }

        public boolean readPosition( XYArrayData xyData, double[] dpos ) {
            int leng = xyData.getLength();
            if ( leng > 0 ) {
                int index = (int) Math.round( fraction_ * ( leng - 1 ) );
                double x = xyData.getX( index );
                double y = xyData.getY( index );
                if ( ! Double.isNaN( x ) && ! Double.isNaN( y ) ) {
                    dpos[ 0 ] = x;
                    dpos[ 1 ] = y;
                    return true;
                }
                else {
                    return false;
                }
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Float.floatToIntBits( (float) fraction_ );
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof FractionPlacement ) {
                FractionPlacement other = (FractionPlacement) o;
                return this.fraction_ == other.fraction_;
            }
            else {
                return false;
            }
        }
    }
}
