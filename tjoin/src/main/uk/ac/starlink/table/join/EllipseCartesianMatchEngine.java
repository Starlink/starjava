package uk.ac.starlink.table.join;

import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.logging.Logger;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * MatchEngine implementation for plane elliptical figures.
 * A match is detected if the ellipses touch or overlap.
 * Tuples are 5-element: (X,Y,A,B,theta), where (X,Y) is the centre position,
 * A and B are the semi-major and semi-minor radii, and theta is the
 * orientation angle in radians, being the angle from the X axis to the
 * semi-major radius measured towards the Y axis.
 *
 * <p>The calculations are currently done using numerical optimisation.
 *
 * @author   Mark Taylor
 * @since    30 Aug 2011
 */
public class EllipseCartesianMatchEngine extends AbstractCartesianMatchEngine {

    private final DescribedValue[] matchParams_;
    private boolean recogniseCircles_;
    private static final double NaN = Double.NaN;
    private static final ValueInfo SCORE_INFO =
        new DefaultValueInfo( "Separation", Double.class,
                              "Normalised distance between ellipses in range "
                            + "0-2; 0 is concentric, 1 is centre-on-edge, "
                            + "2 is edges touching" );
    private static final DefaultValueInfo ERRSCALE_INFO =
        new DefaultValueInfo( "Scale", Number.class,
                              "Rough average of per-object error distance; "
                            + "just used for tuning in conjunction with "
                            + "bin factor" );
    private static final DefaultValueInfo X_INFO =
        new DefaultValueInfo( "X", Number.class, "X coordinate of centre" );
    private static final DefaultValueInfo Y_INFO =
        new DefaultValueInfo( "Y", Number.class, "Y coordinate of centre" );
    private static final DefaultValueInfo A_INFO =
        new DefaultValueInfo( "Primary Radius", Number.class,
                              "Length of ellipse semi-major axis" );
    private static final DefaultValueInfo B_INFO =
        new DefaultValueInfo( "Secondary Radius", Number.class,
                              "Length of ellipse semi-minor axis" );
    private static final DefaultValueInfo THETA_INFO =
        new DefaultValueInfo( "Orientation Angle", Number.class,
                              "Angle from X axis towards Y axis "
                            + "of semi-major axis" );
    static {
        A_INFO.setUCD( "phys.size.smajAxis" );
        B_INFO.setUCD( "phys.size.sminAxis" );
        THETA_INFO.setUnitString( "radians" );
        THETA_INFO.setUCD( "pos.posAng" );
    }

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.join" );

    /**
     * Constructor.
     *
     * @param  scale  rough scale of ellipse dimensions
     */
    @SuppressWarnings("this-escape")
    public EllipseCartesianMatchEngine( double scale ) {
        super( 2 );
        matchParams_ = new DescribedValue[] {
                           new IsotropicScaleParameter( ERRSCALE_INFO ) };
        setIsotropicScale( scale );
        setRecogniseCircles( true );
    }

    /**
     * Sets the rough average of per-object error distance.
     * This is just used in conjunction with the bin factor for tuning.
     *
     * @param   scale  characteristic scale of errors
     */
    public void setScale( double scale ) {
        super.setIsotropicScale( scale );
    }

    /**
     * Returns distance scale.
     *
     * @return  characteristic scale of errors
     */
    public double getScale() {
        return super.getIsotropicScale();
    }

    /**
     * Determines whether short cuts should be taken in the calculations
     * when the ellipses are actually circles.  This is generally a good
     * idea, since it is faster and improves accuracy; the default is
     * therefore true.  But you might want to turn it off for purposes
     * of debugging or testing.
     *
     * @param  recogniseCircles  whether to take circle-specific short cuts
     */
    public void setRecogniseCircles( boolean recogniseCircles ) {
        recogniseCircles_ = recogniseCircles;
    }

    public ValueInfo[] getTupleInfos() {
        return new ValueInfo[] {
            X_INFO, Y_INFO, A_INFO, B_INFO, THETA_INFO,
        };
    }

    public DescribedValue[] getMatchParameters() {
        return matchParams_;
    }

    public ValueInfo getMatchScoreInfo() {
        return SCORE_INFO;
    }

    public double getScoreScale() {
        return 1.0;
    }

    public String toString() {
        return "2-d Cartesian Ellipses";
    }

    public Supplier<MatchKit> createMatchKitFactory() {
        final Supplier<CartesianBinner> binnerFact = createBinnerFactory();
        final ToDoubleFunction<Number> thetaReader = this::getTheta;
        final boolean recogniseCircles = recogniseCircles_;
        return () -> new EllipseCartesianMatchKit( binnerFact.get(),
                                                   thetaReader,
                                                   recogniseCircles );
    }

    public Supplier<Coverage> createCoverageFactory() {
        final CuboidCoverage.PointDecoder pointDecoder =
            CuboidCoverage.createCartesianPointDecoder( 2 );
        final CuboidCoverage.ErrorDecoder errDecoder =
            tuple -> Math.max( getNumberValue( tuple[ 2 ] ),
                               getNumberValue( tuple[ 3 ] ) );
        return () -> CuboidCoverage
                    .createVariableErrorCoverage( 2, pointDecoder, errDecoder );
    }

    /**
     * Returns the position angle in radians given the corresponding
     * tuple element.
     *
     * @param  thetaNum  tuple element giving position angle value, not null
     * @return   angle in radians
     */
    double getTheta( Number thetaNum ) {
        return thetaNum.doubleValue();
    }

    /**
     * Determines whether there is a match between two given ellipses,
     * and returns an object characterising it if there is.
     *
     * @param   e1  ellipse 1
     * @param   e2  ellipse 2
     * @param  recogniseCircles  whether to take short cuts in the calculations
     *                           for circular ellipses
     * @return   description of match, or null if no overlap
     */
    static Match getMatch( Ellipse e1, Ellipse e2, boolean recogniseCircles ) {
        double x1 = e1.x_;
        double y1 = e1.y_;
        double x2 = e2.x_;
        double y2 = e2.y_;

        /* If the centres are more distant than the sum of the major radii,
         * there is no match. */
        if ( sq( x2 - x1 ) + sq( y2 - y1 )
             > sq( e1.getMaxRadius() + e2.getMaxRadius() ) ) {
            return null;
        }

        /* If one of the ellipses is dimensionless, it's a match only if it
         * falls inside the other.  In this case just use the scaled distance
         * as the score. */
        boolean isPoint1 = e1.isPoint();
        boolean isPoint2 = e2.isPoint();
        if ( isPoint1 && isPoint2 ) {
            return ( x1 == x2 && y1 == y2 )
                 ? new Match( 0, NaN, NaN, NaN, NaN )
                 : null;
        }
        else if ( isPoint1 ) {
            double s = scaledDistance( e2, x1, y1 );
            return s <= 1 ? new Match( s, NaN, NaN, x1, y1 ) : null;
        }
        else if ( isPoint2 ) {
            double s = scaledDistance( e1, x2, y2 );
            return s <= 1 ? new Match( s, x2, x2, NaN, NaN ) : null;
        }

        /* If the centre of one of the ellipses is inside the other one,
         * use the scaled distance. */ 
        double sc1 = scaledDistance( e1, x2, y2 );
        double sc2 = scaledDistance( e2, x1, y1 );
        boolean isCenterInside1 = sc1 <= 1.0;
        boolean isCenterInside2 = sc2 <= 1.0;
        if ( isCenterInside1 && isCenterInside2 ) {
            return sc1 < sc2 ? new Match( sc1, x2, y2, NaN, NaN )
                             : new Match( sc2, NaN, NaN, x1, y1 );
        }
        else if ( isCenterInside1 ) {
            return new Match( sc1, x2, y2, NaN, NaN );
        }
        else if ( isCenterInside2 ) {
            return new Match( sc2, NaN, NaN, x1, y1 );
        }

        /* Otherwise, find the closest edge point on one ellipse to the
         * inside (scaled) of the other.  If this point is inside the other
         * ellipse, they overlap.  That criterion is robust, though
         * calculating a score from it is a bit arbitrary.
         * In the case that they are both circles we can do some short cuts
         * in the working. */
        if ( e1.isCircle() && e2.isCircle() && recogniseCircles ) {
            double r1 = e1.a_;
            double r2 = e2.a_;
            assert r1 == e1.b_;
            assert r2 == e2.b_;
            double dx = x2 - x1;
            double dy = y2 - y1;
            double s = Math.sqrt( dx * dx + dy * dy );
            if ( s > r1 + r2 ) {
                return null;
            }
            else {
                double fx = dx / s;
                double fy = dy / s;
                return new Match( 1 + 0.5 * ( (s-r2)/r1 + (s-r1)/r2 ),
                                  x2 - r2 * fx, y2 - r2 * fy,
                                  x1 + r1 * fx, y1 + r1 * fy );
            }
        }
        else {
            double[] p1 = findClosestEdgePoint( e1, e2 );
            double sp1 = scaledDistance( e1, p1[ 0 ], p1[ 1 ] );
            if ( sp1 > 1 ) {
                return null;
            }
            else {
                double[] p2 = findClosestEdgePoint( e2, e1 );
                double sp2 = scaledDistance( e2, p2[ 0 ], p2[ 1 ] );
                assert sp2 <= 1;
                return new Match( 1. + 0.5 * ( sp1 + sp2 ),
                                  p1[ 0 ], p1[ 1 ], p2[ 0 ], p2[ 1 ] );
            }
        }
    }

    /**
     * Returns the scaled distance from the centre of an ellipse to a given
     * point on the plane.  This is an analogue of the distance from the
     * centre of a circle; it evaluates to 0 at the centre of the ellipse
     * and 1 on the circumference.
     *
     * @param   e  ellipse
     * @param   x  X coordinate of point
     * @param   y  Y coordinate of point
     * @return   scaled distance
     */
    static double scaledDistance( Ellipse e, double x, double y ) {
        double rx = x - e.x_;
        double ry = y - e.y_;
        double c = Math.cos( e.theta_ );
        double s = Math.sin( e.theta_ );
        double dx = ( rx * c - ry * s ) / e.a_;
        double dy = ( rx * s + ry * c ) / e.b_;
        return Math.sqrt( dx * dx + dy * dy );
    }

    /**
     * Returns the point on the circumference of a given ellipse parameterised
     * an angle phi.  The whole circumference is covered by varying phi
     * from 0 to 2pi (or equivalent).
     *
     * @param   e  ellipse
     * @param  phi   angle parameter
     * @return   (x,y) coordinates of edge point
     */
    static double[] edgePoint( Ellipse e, double phi ) {
        double cp = Math.cos( phi );
        double sp = Math.sin( phi );
        double ct = Math.cos( e.theta_ );
        double st = Math.sin( e.theta_ );
        double px = e.x_ + e.a_*ct*cp + e.b_*st*sp;
        double py = e.y_ + e.b_*ct*sp - e.a_*st*cp;
        return new double[] { px, py };
    }

    /**
     * Returns the point on the circumference of a given ellipse
     * <code>e2</code> which is closest (scaled distance) to the
     * centre of ellipse <code>e1</code>.
     * This value is calculated using a numerical optimisation technique.
     *
     * @param  e1   ellipse whose centre is sought
     * @param  e2   ellipse whose circumference is varied over
     * @return  point on the circumference of <code>e2</code>
     */
    static double[] findClosestEdgePoint( final Ellipse e1, final Ellipse e2 ) {

        /* Calculate the angle representing the closest point numerically.
         * There are multiple stationary points of the function being
         * minimised, and there may be more than one (maximum two?) minima.
         * Getting a suitable starting point for the optimisation is therefore
         * essential to correctness.  The procedure adopted here appears
         * to be robust, but I haven't proved that it will always work.
         * See the EllipseToy class for an interactive test. */
        AngleOptimiser opt = new AngleOptimiser( 1e-8, 40, 4 ) {
            public double[] calcDerivs( double phi ) {
                return calcSeparationDerivs( e1, e2, phi );
            }
        };
        double phi0 = phiTowardsPoint( e2, e1.x_, e1.y_ );
        double optPhi = opt.findExtremum( phi0, Boolean.TRUE );

        /* Treat optimisation failure. */
        if ( Double.isNaN( optPhi ) ) {

            /* Optimisation can fail if the centre of e2 is very close to the
             * edge of e1, since the result is effectively degenerate in phi.
             * In that case, return the centre of e2, which is about right. */
            if ( Math.abs( scaledDistance( e1, e2.x_, e2.y_ ) - 1 ) < 1e-3 ) {
                return new double[] { e2.x_, e2.y_ };
            }

            /* Otherwise, not sure what happened.
             * Return a best guess and issue a warning. */
            else {
                logger_.warning( "Ellipse optimisation failed for "
                               + e1 + ", " + e2 );
                return edgePoint( e2, phi0 );
            }
        }
        return edgePoint( e2, optPhi );
    }

    /**
     * Ellipse parameter phi which corresponds to a point on the circumference
     * of the ellipse directly towards the point (x, y).  Note this is
     * not just atan2(dy,dx), since the ellipse parameter phi is not a
     * geometric angle.
     *
     * @param   e  ellipse
     * @param   x  X coord of direction point
     * @param   y  Y coord of direction point
     */
    static double phiTowardsPoint( Ellipse e, double x, double y ) {
        double psi = Math.atan2( x - e.x_, y - e.y_ );
        double phi = Math.atan2( e.a_ * Math.cos( psi - e.theta_ ),
                                 e.b_ * Math.sin( psi - e.theta_ ) );
        assert isCollinear( new double[] { e.x_, e.y_ },
                            edgePoint( e, phi ),
                            new double[] { x, y } );
        return phi;
    }

    /**
     * Calculates the function s12(phi2) and its derivatives.
     * s12 is scaledDistance(e1,x2(phi2),y2(phi2)), x2(phi2) and y2(phi2)
     * are the outputs of edgePoint(e2,phi2).  The zeroth, first and second
     * derivatives w.r.t. phi2 are calculated.  These can be used for
     * numerical optimisation of s12 w.r.t. phi2.
     *
     * @param   e1  ellipse whose centre is sought
     * @param   e2  ellipse whose circumference is parameterised
     * @param  phi2  parameter for circumference of <code>e2</code>
     * @return  array of first three derivatives of s12 w.r.t. phi2
     */
    private static double[] calcSeparationDerivs( Ellipse e1, Ellipse e2,
                                                  double phi2 ) {
        double a1 = e1.a_;
        double b1 = e1.b_;
        double x1 = e1.x_;
        double y1 = e1.y_;
        double c1 = Math.cos( e1.theta_ );
        double s1 = Math.sin( e1.theta_ );

        double a2 = e2.a_;
        double b2 = e2.b_;
        double x2 = e2.x_;
        double y2 = e2.y_;
        double c2 = Math.cos( e2.theta_ );
        double s2 = Math.sin( e2.theta_ );

        double x12 = x2 - x1;
        double y12 = y2 - y1;
        double c12 = Math.cos( e2.theta_ - e1.theta_ );
        double s12 = Math.sin( e2.theta_ - e1.theta_ );

        double raa1 = 1.0 / (a1*a1);
        double rbb1 = 1.0 / (b1*b1);

        double cp = Math.cos( phi2 );
        double sp = Math.sin( phi2 );

        double tcc = a2*a2*(c12*c12*raa1 + s12*s12*rbb1);
        double tss = b2*b2*(s12*s12*raa1 + c12*c12*rbb1);
        double tcs = 2*a2*b2*c12*s12*(raa1-rbb1);
        double tc = 2*a2*(c12*raa1*(x12*c1-y12*s1)-s12*rbb1*(x12*s1+y12*c1));
        double ts = 2*b2*(s12*raa1*(x12*c1-y12*s1)+c12*rbb1*(x12*s1+y12*c1));
        double t = raa1*sq(x12*c1-y12*s1) + rbb1*sq(x12*s1+y12*c1);

        double c2p = Math.cos( 2*phi2 );
        double s2p = Math.sin( 2*phi2 );

        return new double[] {
            tcc*cp*cp + tss*sp*sp + tcs*cp*sp + tc*cp + ts*sp + t,
            (tss-tcc)*s2p + tcs*c2p - tc*sp + ts*cp,
            2*(tss-tcc)*c2p - 2*tcs*s2p - tc*cp - ts*sp,
        };
    }

    /**
     * Square.
     *
     * @param   x  parameter
     * @return   <code>x*x</code>
     */
    private static double sq( double x ) {
        return x * x;
    }

    /**
     * Indicates whether three points are (or nearly are) on the same line.
     *
     * @param  r1  (x,y) coords of point 1
     * @param  r2  (x,y) coords of point 2
     * @param  r3  (x,y) coords of point 3
     * @return   true iff r1, r2, r3 fall on a single line, or nearly do so
     */
    private static boolean isCollinear( double[] r1, double[] r2,
                                        double[] r3 ) {
        double xa = r3[0] - r2[0];
        double ya = r3[1] - r2[1];
        double xb = r2[0] - r1[0];
        double yb = r2[1] - r1[1];
        double crossprod = xa * yb - ya * xb;
        return Math.abs( crossprod ) < 1e-10;
    }

    /**
     * MatchKit implementation for use with this class.
     */
    private static class EllipseCartesianMatchKit implements MatchKit {

        final CartesianBinner binner_;
        final ToDoubleFunction<Number> thetaReader_;
        final boolean recogniseCircles_;
        final double[] workXy_;

        /**
         * Constructor.
         *
         * @param  binner  binner
         * @param  thetaRader  turns a tuple element into an angle in radians
         * @param  recogniseCircles  true to take circle-specific short cuts
         */
        EllipseCartesianMatchKit( CartesianBinner binner,
                                  ToDoubleFunction<Number> thetaReader,
                                  boolean recogniseCircles ) {
            binner_ = binner;
            thetaReader_ = thetaReader;
            recogniseCircles_ = recogniseCircles;
            workXy_ = new double[ 2 ];
        }

        public double matchScore( Object[] tuple1, Object[] tuple2 ) {
            Match match = getMatch( toEllipse( tuple1 ), toEllipse( tuple2 ),
                                    recogniseCircles_ );
            return match == null ? -1 : match.score_;
        }

        public Object[] getBins( Object[] tuple ) {
            Ellipse ellipse = toEllipse( tuple );
            workXy_[ 0 ] = ellipse.x_;
            workXy_[ 1 ] = ellipse.y_;
            return binner_.getRadiusBins( workXy_, ellipse.getMaxRadius() );
        }

        /**
         * Turns a tuple as accepted by this match engine into an Ellipse object
         * as used by the internal calculations.
         */
        private Ellipse toEllipse( Object[] tuple ) {
            double x = ((Number) tuple[ 0 ]).doubleValue();
            double y = ((Number) tuple[ 1 ]).doubleValue();
            if ( tuple[ 2 ] instanceof Number &&
                 tuple[ 3 ] instanceof Number &&
                 tuple[ 4 ] instanceof Number ) {
                double a = ((Number) tuple[ 2 ]).doubleValue();
                double b = ((Number) tuple[ 3 ]).doubleValue();
                double theta =
                    thetaReader_.applyAsDouble( ((Number) tuple[ 4 ]) );
                return new Ellipse( x, y, a, b, theta );
            }
            else {
                return new Ellipse( x, y );
            }
        }
    }

    /**
     * MatchEngine class that behaves like EllipseCartesianSkyMatchEngine but
     * uses human-friendly units (degrees and arcseconds) rather than radians
     * for tuple elements and match parameters.
     * The difference is only in position angle.
     */
    public static class InDegrees extends EllipseCartesianMatchEngine {
        private final ValueInfo[] tupleInfos_;

        /**
         * Constructor.
         *
         * @param  scale  rough scale of ellipse dimensions
         */
        @SuppressWarnings("this-escape")
        public InDegrees( double scale) {
            super( scale );
            ValueInfo[] infos0 = super.getTupleInfos();
            tupleInfos_ = new ValueInfo[] {
                infos0[ 0 ],
                infos0[ 1 ],
                infos0[ 2 ],
                infos0[ 3 ],
                AbstractSkyMatchEngine.inDegreeInfo( infos0[ 4 ] ),
            };
            assert tupleInfos_.length == infos0.length;
        }
        @Override
        public ValueInfo[] getTupleInfos() {
            return tupleInfos_;
        }
        @Override
        double getTheta( Number thetaNum ) {
            return thetaNum.doubleValue() * AbstractSkyMatchEngine.FROM_DEG;
        }
    }

    /**
     * Characterises a successful match between two ellipses.
     * As well as the score (between 0 and 2, 0 is best), some interesting
     * points may be included.  There are two of these, one for each 
     * ellipse, and they represent line segments which contribute to
     * the match.  Either or both may be blank (represented by NaN
     * coordinates).  These are provided for illustration, and may be
     * used for graphical feedback, or may be ignored.
     */
    static class Match {

        /** Match score, between 0 and 2, 0 is best. */
        final double score_;

        /** X coordinate of line end from centre of ellipse 1, or NaN. */
        final double x1_;

        /** Y coordinate of line end from centre of ellipse 1, or NaN. */
        final double y1_;

        /** X coordinate of line end from centre of ellipse 2, or NaN. */
        final double x2_;

        /** Y coordinate of line end from centre of ellipse 2, or NaN. */
        final double y2_;

        /**
         * Constructor.
         *
         * @param   score  match score
         * @param   x1  x coord of line end from centre of ellipse 1
         * @param   y1  y coord of line end from centre of ellipse 1
         * @param   x2  x coord of line end from centre of ellipse 2
         * @param   y2  y coord of line end from centre of ellipse 2
         */
        Match( double score, double x1, double y1, double x2, double y2 ) {
            score_ = score;
            x1_ = x1;
            y1_ = y1;
            x2_ = x2;
            y2_ = y2;
        }
    }

    /**
     * Represents an ellipse which can be matched with other ellipses
     * by this match engine.
     *
     * <p>The two radii are labelled major and minor for convenience -
     * it is permitted for the minor radius to be larger than the major one.
     * The orientation is measured in the positive (anticlockwise)
     * direction from the X axis to the major radius; note that this
     * is not the same convention used for a Position Angle on the sky.
     */
    static class Ellipse {

        /** X coordinate of centre. */
        final double x_;

        /** Y coordinate of centre. */
        final double y_;

        /** Major radius. */
        final double a_;

        /** Minor radius. */
        final double b_;

        /** Angle of major radius from X axis in radians. */
        final double theta_;

        /**
         * Constructs a general ellipse.
         *
         * @param  x  x coordinate of centre
         * @param  y  y coordinate of centre
         * @param  a  major radius
         * @param  b  minor radius
         * @param  theta  angle of major radius from X axis in radians
         */
        Ellipse( double x, double y, double a, double b, double theta ) {
            x_ = x;
            y_ = y;
            a_ = a;
            b_ = b;
            theta_ = theta;
        }

        /**
         * Constructs a point-like ellipse.
         *
         * @param  x  x coordinate of centre
         * @param  y  y coordinate of centre
         */
        Ellipse( double x, double y ) {
            this( x, y, 0, 0, 0 );
        }

        /**
         * Indicates whether this ellipse is point-like.
         *
         * @return   true iff this ellipse is dimensionless
         */
        boolean isPoint() {
            return ! ( ( a_ > 0 || b_ > 0 ) && ! Double.isNaN( theta_ ) );
        }

        /**
         * Indicates whether this ellipse is circle-like.
         *
         * @return  true iff this ellipse has equal radii
         */
        boolean isCircle() {
            return a_ == b_;
        }

        /**
         * Returns a distance from the centre of this ellipse beyond which
         * points are definitely outside it.
         *
         * @return   maximum of semi-major radii
         */
        double getMaxRadius() {
            return Math.max( a_, b_ );
        }

        public String toString() {
            return "(x=" + x_ + ", y=" + y_ + ", a=" + a_ + ", b=" + b_
                 + ", theta=" + theta_;
        }
    }
}
