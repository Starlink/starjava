package uk.ac.starlink.table.join;

import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.pal.AngleDR;
import uk.ac.starlink.pal.Pal;
import uk.ac.starlink.pal.palError;

public class SkyEllipseMatchEngine {

    private static final double NaN = Double.NaN;
    private static final Pal pal_ = new Pal();
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.join" );

    // alpha, delta, mu,    nu,    zeta
    // ra,    dec,   majax, minax, pa
    // zeta = P.A. = angle from north pole (Dec=Pi/2) towards positive RA axis.
    public double matchScore( Object[] tuple1, Object[] tuple2 ) {
        Match match = getMatch( toSkyEllipse( tuple1 ),
                                toSkyEllipse( tuple2 ), false );
        return match == null ? -1 : match.score_;
    }

    static Match getMatch( SkyEllipse se1, SkyEllipse se2,
                           boolean needPoints ) {
        double alpha1 = se1.alpha_;
        double delta1 = se1.delta_;
        double alpha2 = se2.alpha_;
        double delta2 = se2.delta_;

        /* If the centres are more distant than the sum of the major radii,
         * there is no match. */
        double maxSep = se1.getMaxRadius() + se2.getMaxRadius();
        if ( Math.abs( delta2 - delta1 ) > maxSep ||
             SkyMatchEngine.calculateSeparation( alpha1, delta1,
                                                 alpha2, delta2 ) > maxSep ) {
            return null;
        }

        /* If they are both points, then it's a match (a perfect one) only
         * if both centres are identical. */
        boolean isPoint1 = se1.isPoint();
        boolean isPoint2 = se2.isPoint();
        if ( isPoint1 && isPoint2 ) {
            return ( normalizeAlpha( alpha1 ) == normalizeAlpha( alpha2 ) &&
                     normalizeDelta( delta1 ) == normalizeDelta( delta2 ) )
                 ? new Match( 0, NaN, NaN, NaN, NaN )
                 : null;
        }

        /* If just one is a point, then it's a match only if it falls
         * inside the other. */
        else if ( isPoint1 ) {
            double s = scaledDistance( se2, alpha1, delta1 );
            return s <= 1 ? new Match( s, NaN, NaN, alpha1, delta1 ) : null;
        }
        else if ( isPoint2 ) {
            double s = scaledDistance( se1, alpha2, delta2 );
            return s <= 1 ? new Match( s, alpha2, delta2, NaN, NaN ) : null;
        }

        /* If the centre of one of the ellipses is inside the other one,
         * use the scaled distance. */
        double sc1 = scaledDistance( se1, alpha2, delta2 );
        double sc2 = scaledDistance( se2, alpha1, delta1 );
        boolean isCenterInside1 = sc1 <= 1.0;
        boolean isCenterInside2 = sc2 <= 1.0;
        if ( isCenterInside1 && isCenterInside2 ) {
            return sc1 < sc2 ? new Match( sc1, alpha2, delta2, NaN, NaN )
                             : new Match( sc2, NaN, NaN, alpha1, delta1 );
        }
        else if ( isCenterInside1 ) {
            return new Match( sc1, alpha2, delta2, NaN, NaN );
        }
        else if ( isCenterInside2 ) {
            return new Match( sc2, NaN, NaN, alpha2, delta2 );
        }

        /* Otherwise, it's complicated.  I haven't managed to make much of
         * the spherical trigonometry required to do this properly, so
         * perform an approximate projection of both ellipses onto a plane
         * and treat them in Cartesian coordinates.
         * The projection is onto a plane tangent to the midpoint between
         * the two centres.  This is somewhat arbitrary, but it is at least
         * symmetric between the two input ellipses. */
        double[] pt = bisect( alpha1, delta1, alpha2, delta2 );
        Projector projector = new Projector( pt[ 0 ], pt[ 1 ] );
        EllipseMatchEngine.Match cmatch =
            EllipseMatchEngine.getMatch( projectEllipse( projector, se1 ),
                                         projectEllipse( projector, se2 ) );
        if ( cmatch == null ) {
            return null;
        }
        else {
            double score = cmatch.score_;
            if ( needPoints ) {
                double[] ad1 = projector.unproject( cmatch.x1_, cmatch.y1_ );
                double[] ad2 = projector.unproject( cmatch.x2_, cmatch.y2_ );
                return new Match( score, ad1[ 0 ], ad1[ 1 ],
                                         ad2[ 0 ], ad2[ 1 ] );
            }
            else {
                return new Match( score, NaN, NaN, NaN, NaN );
            }
        }
    }

    static double scaledDistance( SkyEllipse se, double alpha, double delta ) {
        // This is effectively projecting an flat ellipse onto the sphere.
        // Since we haven't really defined what we mean by an ellipse,
        // and it's bound to be small for sensible data, it's probably
        // as good as anything.
        double[][] rot =
            pal_.Deuler( "zxz",
                         se.alpha_ + 0.5 * Math.PI, 0.5 * Math.PI - se.delta_,
                         Math.PI / 2 - se.zeta_ );
        double[] xyz =
            pal_.Dmxv( rot, pal_.Dcs2c( new AngleDR( alpha, delta ) ) );
        boolean anti = xyz[ 2 ] < 0;
        double dm = Math.asin( Math.abs( xyz[ 0 ] ) );
        double dn = Math.asin( Math.abs( xyz[ 1 ] ) );
        if ( anti ) {
            dm = Math.PI - dm;
            dn = Math.PI - dn;
        }
        double dx = dm / se.mu_;
        double dy = dn / se.nu_;
        return Math.sqrt( dx * dx + dy * dy );
    }

    static double[] bisect( double alpha1, double delta1,
                            double alpha2, double delta2 ) {
        double[] p1 = pal_.Dcs2c( new AngleDR( alpha1, delta1 ) );
        double[] p2 = pal_.Dcs2c( new AngleDR( alpha2, delta2 ) );
        double[] pc = new double[] { ( p1[ 0 ] + p2[ 0 ] ) * 0.5,
                                     ( p1[ 1 ] + p2[ 1 ] ) * 0.5,
                                     ( p1[ 2 ] + p2[ 2 ] ) * 0.5 };
        AngleDR cSph = pal_.Dcc2s( pc );
        return new double[] { cSph.getAlpha(), cSph.getDelta() };
    }

    private static double normalizeAlpha( double alpha ) {
        final double base = 2 * Math.PI;
        return ( ( alpha % base ) + base ) % base;
    }

    private static double normalizeDelta( double delta ) {
        return delta;
    }

    public static EllipseMatchEngine.Ellipse
                  projectEllipse( Projector projector, SkyEllipse se ) {
        double[] center = projector.project( se.alpha_, se.delta_ );
        double x = center[ 0 ];
        double y = center[ 1 ];
        double a = se.mu_;
        double b = se.nu_;
        double theta = 0.5 * Math.PI + se.zeta_;
        return new EllipseMatchEngine.Ellipse( x, y, a, b, theta );
    }

    public static SkyEllipse toSkyEllipse( Object[] tuple ) {
        double alpha = ((Number) tuple[ 0 ]).doubleValue();
        double delta = ((Number) tuple[ 1 ]).doubleValue();
        if ( tuple[ 2 ] instanceof Number &&
             tuple[ 3 ] instanceof Number &&
             tuple[ 4 ] instanceof Number ) {
            double mu = ((Number) tuple[ 2 ]).doubleValue();
            double nu = ((Number) tuple[ 3 ]).doubleValue();
            double zeta = ((Number) tuple[ 4 ]).doubleValue();
            return new SkyEllipse( alpha, delta, mu, nu, zeta );
        }
        else {
            return new SkyEllipse( alpha, delta );
        }
    }

    static class Match {
        final double score_;
        final double alpha1_;
        final double delta1_;
        final double alpha2_;
        final double delta2_;

        Match( double score, double alpha1, double delta1,
               double alpha2, double delta2 ) {
            score_ = score;
            alpha1_ = alpha1;
            delta1_ = delta1;
            alpha2_ = alpha2;
            delta2_ = delta2;
        }
    }

    static class SkyEllipse {
        final double alpha_;
        final double delta_;
        final double mu_;
        final double nu_;
        final double zeta_;

        SkyEllipse( double alpha, double delta, double mu, double nu,
                    double zeta ) {
            alpha_ = alpha;
            delta_ = delta;
            mu_ = mu;
            nu_ = nu;
            zeta_ = zeta;
        }

        SkyEllipse( double alpha, double delta ) {
            this( alpha, delta, 0, 0, 0 );
        }

        boolean isPoint() {
            return ! ( ( mu_ > 0 || nu_ > 0 ) && ! Double.isNaN( zeta_ ) );
        }

        double getMaxRadius() {
            return Math.max( mu_, nu_ );
        }

        public String toString() {
            return "(alpha=" + alpha_
                 + ", delta=" + delta_
                 + ", mu=" + mu_
                 + ", nu=" + nu_
                 + ", zeta=" + zeta_
                 + ")";
        }
    }

    static class Projector {
        private final AngleDR ad0_;

        Projector( double alpha0, double delta0 ) {
            ad0_ = new AngleDR( alpha0, delta0 );
        }

        public double[] project( double alpha, double delta ) {
            try {
                AngleDR ad = pal_.Ds2tp( new AngleDR( alpha, delta ), ad0_ );
                return new double[] { ad.getAlpha(), ad.getDelta() };
            }
            catch ( palError e ) {
                return new double[] { Double.NaN, Double.NaN };
            }
        }

        public double[] unproject( double x, double y ) {
            AngleDR ad = pal_.Dtp2s( new AngleDR( x, y ), ad0_ );
            return new double[] { ad.getAlpha(), ad.getDelta() };
        }
    }
}
