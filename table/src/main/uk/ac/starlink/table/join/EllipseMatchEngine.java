package uk.ac.starlink.table.join;

import java.util.logging.Logger;

// theta is the positive (anticlockwise) angle from the X axis.
public abstract class EllipseMatchEngine implements MatchEngine {

    private static final double NaN = Double.NaN;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.join" );

    public EllipseMatchEngine() {
    }

    // x, y, a, b, theta
    public double matchScore( Object[] tuple1, Object[] tuple2 ) {
        Match match = getMatch( toEllipse( tuple1 ), toEllipse( tuple2 ) );
        return match == null ? -1 : match.score_;
    }

    static Match getMatch( Ellipse e1, Ellipse e2 ) {
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
         * calculating a score from it is a bit arbitrary. */
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

    static double scaledDistance( Ellipse e, double x, double y ) {
        double rx = x - e.x_;
        double ry = y - e.y_;
        double c = Math.cos( e.theta_ );
        double s = Math.sin( e.theta_ );
        double dx = ( rx * c - ry * s ) / e.a_;
        double dy = ( rx * s + ry * c ) / e.b_;
        return Math.sqrt( dx * dx + dy * dy );
    }

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
     * Point on edge of e2 closest to the centre of e1.
     */
    static double[] findClosestEdgePoint( final Ellipse e1, final Ellipse e2 ) {

        /* Calculate the angle representing the closest point numerically.
         * There are several stationary points of the function being
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
     * Ellipse parameter phi from the centre of e towards the point (x, y).
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

    private static double sq( double x ) {
        return x * x;
    }

    private static boolean isCollinear( double[] r1, double[] r2,
                                        double[] r3 ) {
        double xa = r3[0] - r2[0];
        double ya = r3[1] - r2[1];
        double xb = r2[0] - r1[0];
        double yb = r2[1] - r1[1];
        double crossprod = xa * yb - ya * xb;
        return Math.abs( crossprod ) < 1e-10;
    }

    static class Match {
        final double score_;
        final double x1_;
        final double y1_;
        final double x2_;
        final double y2_;

        Match( double score, double x1, double y1, double x2, double y2 ) {
            score_ = score;
            x1_ = x1;
            y1_ = y1;
            x2_ = x2;
            y2_ = y2;
        }
    }

    private static Ellipse toEllipse( Object[] tuple ) {
        double x = ((Number) tuple[ 0 ]).doubleValue();
        double y = ((Number) tuple[ 1 ]).doubleValue();
        if ( tuple[ 2 ] instanceof Number &&
             tuple[ 3 ] instanceof Number &&
             tuple[ 4 ] instanceof Number ) {
            double a = ((Number) tuple[ 2 ]).doubleValue();
            double b = ((Number) tuple[ 3 ]).doubleValue();
            double theta = ((Number) tuple[ 4 ]).doubleValue();
            return new Ellipse( x, y, a, b, theta );
        }
        else {
            return new Ellipse( x, y );
        }
    }

    static class Ellipse {
        final double x_;
        final double y_;
        final double a_;
        final double b_;
        final double theta_;

        Ellipse( double x, double y, double a, double b, double theta ) {
            x_ = x;
            y_ = y;
            a_ = a;
            b_ = b;
            theta_ = theta;
        }

        Ellipse( double x, double y ) {
            this( x, y, 0, 0, 0 );
        }

        boolean isPoint() {
            return ! ( ( a_ > 0 || b_ > 0 ) && ! Double.isNaN( theta_ ) );
        }

        double getMaxRadius() {
            return Math.max( a_, b_ );
        }

        public String toString() {
            return "(x=" + x_ + ", y=" + y_ + ", a=" + a_ + ", b=" + b_
                 + ", theta=" + theta_;
        }
    }
}
