package uk.ac.starlink.table.join;

import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.pal.AngleDR;
import uk.ac.starlink.pal.Pal;
import uk.ac.starlink.pal.palError;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * MatchEngine implementation for ellipses on the surface of a (celestial)
 * sphere.
 * The tuples it uses are five-element arrays of {@link java.lang.Number}
 * objects, as follows:
 * </p>
 * <ol>
 * <li>alpha: right ascension coordinate of ellipse centre in radians
 * <li>delta: declination coordinate of ellipse centre in radians
 * <li>mu: primary radius of ellipse in radians
 * <li>nu: secondary radius of ellipse in radians
 * <li>zeta: position angle in radians (from north pole to primary radius,
 *           in direction of positive alpha axis)
 * </ol>
 * <p>Two tuples are considered to match if their ellipses touch or
 * partially overlap.
 * The match score is a normalized value; it is zero for concentric ellipses,
 * 1 if the centre of one ellipse falls on the circumference of the other,
 * and 2 if the ellipses just touch.  Intermediate values are assumed for
 * intermediate situations.
 *
 * <p>Other RA/Dec-like sky coordinate systems may alternatively be used
 * for the alpha/delta coordinates.
 *
 * <p>The calculations are approximate since in some cases they rely on
 * projecting the ellipses onto a Cartesian plane before evaluating the match,
 * so for large ellipses the criterion will be less exact.
 * For objects the size of most observed stars and galaxies,
 * this approximation is not expected to be problematic.
 *
 * <p>The calculations are currently done using numerical optimisation.
 *
 * @author   Mark Taylor
 * @since    30 Aug 2011
 */
public class EllipseSkyMatchEngine extends AbstractSkyMatchEngine {

    private final DescribedValue[] matchParams_;

    private static final DefaultValueInfo SCALE_INFO =
        new DefaultValueInfo( "Scale", Number.class,
                              "Rough average of ellipse major radius; "
                            + "just used for tuning to set "
                            + "default pixel size" );
    private static final DefaultValueInfo SCORE_INFO =
        new DefaultValueInfo( "Separation", Double.class,
                              "Normalised distance between ellipses; "
                            + "range is 0 (concentric) - 2 (tangent)" );
    private static final DefaultValueInfo ALPHA_INFO =
        new DefaultValueInfo( "RA", Number.class,
                              "Right ascension of centre" );
    private static final DefaultValueInfo DELTA_INFO =
        new DefaultValueInfo( "Dec", Number.class,
                              "Declination of centre" );
    private static final DefaultValueInfo MU_INFO =
        new DefaultValueInfo( "Rmaj", Number.class,
                              "Ellipse primary radius radius" );
    private static final DefaultValueInfo NU_INFO =
        new DefaultValueInfo( "Rmin", Number.class,
                              "Ellipse secondary radius" );
    private static final DefaultValueInfo ZETA_INFO =
        new DefaultValueInfo( "PosAng", Number.class,
                              "Position angle - measured from north pole to "
                            + "primary axis, in direction of positive RA" );
    static {
        ALPHA_INFO.setUnitString( "radians" );
        ALPHA_INFO.setUCD( "pos.eq.ra" );
 
        DELTA_INFO.setUnitString( "radians" );
        DELTA_INFO.setUCD( "pos.eq.dec" );

        MU_INFO.setUnitString( "radians" );
        MU_INFO.setUnitString( "pos.angDistance" );

        NU_INFO.setUnitString( "radians" );
        NU_INFO.setUnitString( "pos.angDistance" );

        ZETA_INFO.setUnitString( "radians" );
        ZETA_INFO.setUCD( "pos.posAng" );
    }

    private static final double NaN = Double.NaN;
    private static final Pal pal_ = new Pal();
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.join" );

    /**
     * Constructor.
     *
     * @param  pixellator  handles sky pixellisation
     * @param  scale       initial value for length scale, in radians
     */
    public EllipseSkyMatchEngine( SkyPixellator pixellator, double scale ) {
        super( pixellator, scale );
        matchParams_ =
            new DescribedValue[] { new SkyScaleParameter( SCALE_INFO ) };
    }

    /**
     * Sets the length scale.
     *
     * @param  scale rough value of per-object errors, in radians
     */
    public void setScale( double scale ) {
        super.setScale( scale );
    }

    /**
     * Returns the length scale.
     *
     * @return  length scale value in radians
     */
    public double getScale() {
        return super.getScale();
    }

    public ValueInfo[] getTupleInfos() {
        return new ValueInfo[] {
            ALPHA_INFO, DELTA_INFO, MU_INFO, NU_INFO, ZETA_INFO
        };
    }

    public DescribedValue[] getMatchParameters() {
        return matchParams_;
    }

    public ValueInfo getMatchScoreInfo() {
        return SCORE_INFO;
    }

    public double matchScore( Object[] tuple1, Object[] tuple2 ) {
        Match match = getMatch( toSkyEllipse( tuple1 ),
                                toSkyEllipse( tuple2 ), false );
        return match == null ? -1 : match.score_;
    }

    public Object[] getBins( Object[] tuple ) {
        SkyEllipse ellipse = toSkyEllipse( tuple );
        return getBins( ellipse.alpha_, ellipse.delta_,
                        ellipse.getMaxRadius() );
    }

    public boolean canBoundMatch() {
        return true;
    }

    public Comparable[][] getMatchBounds( Comparable[] minTuple,
                                          Comparable[] maxTuple ) {
        double maxError = Math.max( getNumberValue( maxTuple[ 2 ] ),
                                    getNumberValue( maxTuple[ 3 ] ) );
        return createExtendedSkyBounds( minTuple, maxTuple, 0, 1,
                                        2 * maxError );
    }

    public String toString() {
        return "Sky Ellipses";
    }

    /**
     * Turns a tuple as accepted by this match engine into a SkyEllipse object
     * as used by the internal calculations.
     */
    private static SkyEllipse toSkyEllipse( Object[] tuple ) {
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

    /**
     * Determines whether there is a match between two given ellipses,
     * and returns an object characterising it if there is.
     *
     * @param   se1  ellipse 1
     * @param   se2  ellipse 2
     * @param   needPoints  true if the caller wants the coordinate information
     *                      filled in in the returned Match object;
     *                      this may be expensive to generate, so if it's
     *                      not required, false can be given
     * @return   description of match, or null if no overlap
     */
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
             AbstractSkyMatchEngine
            .calculateSeparation( alpha1, delta1, alpha2, delta2 ) > maxSep ) {
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
        EllipseCartesianMatchEngine.Match cmatch =
            EllipseCartesianMatchEngine
           .getMatch( projectEllipse( projector, se1 ),
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

    /**
     * Returns the scaled distance from the centre of an ellipse to a given
     * point on the sphere.  This is an analogue of the distance from the
     * centre of a small circle - it evaluates to 0 at the centre of the
     * ellipse and 1 on the circumference.
     *
     * <p>The calculation is only accurate for small distances
     * (ellipse dimensions and distance from the point to the centre
     * small angles), though an attempt is made to return a sensible
     * value for larger dimensions.
     *
     * @param  se  ellipse 
     * @param  alpha   right ascension of point in radians
     * @param  delta   declination of point in radians
     * @return  scaled distance
     */
    static double scaledDistance( SkyEllipse se, double alpha, double delta ) {

        /* Rotate the coordinates so that the ellipse is centred on the
         * north pole. */
        double[][] rot =
            pal_.Deuler( "zxz",
                         se.alpha_ + 0.5 * Math.PI, 0.5 * Math.PI - se.delta_,
                         Math.PI / 2 - se.zeta_ );
        double[] xyz =
            pal_.Dmxv( rot, pal_.Dcs2c( new AngleDR( alpha, delta ) ) );

        /* Work out the angular distances along X and Y axes. */
        double dm = Math.asin( Math.abs( xyz[ 0 ] ) );
        double dn = Math.asin( Math.abs( xyz[ 1 ] ) );

        /* Adjust if the requested point is in the wrong hemisphere. */
        boolean anti = xyz[ 2 ] < 0;
        if ( anti ) {
            dm = Math.PI - dm;
            dn = Math.PI - dn;
        }

        /* Scale for ellipse dimensions and return result. */
        double dx = dm / se.mu_;
        double dy = dn / se.nu_;
        return Math.sqrt( dx * dx + dy * dy );
    }

    /**
     * Find a point midway between two given points.
     *
     * @param   alpha1  RA of point 1
     * @param   delta1  Dec of point 1
     * @param   alpha2  RA of point 2
     * @param   delta2  Dec of point 2
     * @return  (ra,dec) for bisector point
     */
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

    /**
     * Maps RA-type angle to a canonical range.
     *
     * @param  alpha  input angle
     * @return   angle mapped to range 0..2*pi
     */
    private static double normalizeAlpha( double alpha ) {
        final double base = 2 * Math.PI;
        return ( ( alpha % base ) + base ) % base;
    }

    /**
     * Normalises Dec-type angle.
     *
     * @param  delta  input angle
     * @return  same as input angle
     */
    private static double normalizeDelta( double delta ) {
        return delta;
    }

    /**
     * Projects a spherical ellipse onto a plane.
     * Will only work well for small ellipses near the projector's projection
     * point.
     *
     * @param   projector  projector object
     * @param   se  sky ellipse
     * @return  cartesian ellipse
     */
    public static EllipseCartesianMatchEngine.Ellipse
                  projectEllipse( Projector projector, SkyEllipse se ) {
        double[] center = projector.project( se.alpha_, se.delta_ );
        double x = center[ 0 ];
        double y = center[ 1 ];
        double a = se.mu_;
        double b = se.nu_;
        double theta = 0.5 * Math.PI + se.zeta_;
        return new EllipseCartesianMatchEngine.Ellipse( x, y, a, b, theta );
    }

    /**
     * Represents a successful match between two sky ellipses.
     * As well as the score (between 0 and 2, 0 is best), some interesting
     * points may be included.  There are two of these, one for each
     * ellipse, and they represent line segments which contribute to
     * the match.  Either or both may be blank (represented by NaN
     * coordinates).  These are provided for illustration, and may be
     * used for graphical feedback, or may be ignored.
     */
    static class Match {

        /** Match score between 0 and 2, 0 is best. */
        final double score_;

        /** RA of arc end from centre of ellipse 1, or NaN. */
        final double alpha1_;

        /** Dec of arc end from centre of ellipse 1, or NaN. */
        final double delta1_;

        /** RA of arc end from centre of ellipse 2, or NaN. */
        final double alpha2_;

        /** Dec of arc end from centre of ellipse 2, or NaN. */
        final double delta2_;

        /**
         * Constructor.
         *
         * @param  score  match score
         * @param  alpha1  ra coord of arc end from centre of ellipse 1
         * @param  delta1  dec coord of arc end from centre of ellipse 1
         * @param  alpha2  ra coord of arc end from centre of ellipse 2
         * @param  delta2  dec coord of arc end from centre of ellipse 2
         */
        Match( double score, double alpha1, double delta1,
               double alpha2, double delta2 ) {
            score_ = score;
            alpha1_ = alpha1;
            delta1_ = delta1;
            alpha2_ = alpha2;
            delta2_ = delta2;
        }
    }

    /**
     * Represents an ellipse on the surface of a sphere, which can be matched
     * with other ellipses by this match engine.
     *
     * <p>The two radii, which are both measured in radians, are labelled
     * major and minor for convenience - it is permitted for the minor radius
     * to be larger than the major one.
     * The ellipse orientation angle zeta is measured from the 
     * direction towards the north pole to the major radius towards the 
     * positive RA axis, which matches the normal convention
     * for Position Angle on the sky.
     */
    static class SkyEllipse {

        /** RA coordinate of the centre, in radians. */
        final double alpha_;

        /** Declination coordinate of the centre, in radians. */
        final double delta_;

        /** Major radius in radians. */
        final double mu_;

        /** Minor radius in radians. */
        final double nu_;

        /** Angle of major radius from positive delta axis to
         *  positive alpha axis in radians. */
        final double zeta_;

        /**
         * Constructs a general sky ellipse.
         *
         * @param   alpha  RA of centre in radians
         * @param   delta  Dec of centre in radians
         * @param   mu     major radius in radians
         * @param   nu     minor radius in radians
         * @param   zeta   angle from north to major radius in radians
         */
        SkyEllipse( double alpha, double delta, double mu, double nu,
                    double zeta ) {
            alpha_ = alpha;
            delta_ = delta;
            mu_ = mu;
            nu_ = nu;
            zeta_ = zeta;
        }

        /**
         * Constructs a point-like ellipse.
         *
         * @param   alpha  RA of centre in radians
         * @param   delta  Dec of centre in radians
         */
        SkyEllipse( double alpha, double delta ) {
            this( alpha, delta, 0, 0, 0 );
        }

        /**
         * Indicates whether this ellipse is point-like.
         *
         * @return   true iff this ellipse is dimensionless
         */
        boolean isPoint() {
            return ! ( ( mu_ > 0 || nu_ > 0 ) && ! Double.isNaN( zeta_ ) );
        }

        /**
         * Returns an angular distance from the centre of this ellipse
         * beyond which positions are definitely outside it.
         *
         * @return   maximum of semi-major radii in radians
         */
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

    /**
     * Object which can project points from a the surface of a sphere
     * onto a given tangent plane.
     */
    static class Projector {

        private final AngleDR ad0_;

        /**
         * Constructor.
         *
         * @param  alpha0  RA of tangent point
         * @param  delta0  Dec of tangent point
         */
        Projector( double alpha0, double delta0 ) {
            ad0_ = new AngleDR( alpha0, delta0 );
        }

        /**
         * Projects a point from the surface of a sphere onto this projector's
         * plane.  The units of the output coordinates are radians, or at
         * least radian-like.  In the case of a projection error, the
         * returned coordinates will be NaNs.
         *
         * @param  alpha  RA of point to be projected
         * @param  delta  Dec of point to be projected
         * @return  (x,y) Cartesian coordinates of projected point
         */
        public double[] project( double alpha, double delta ) {
            try {
                AngleDR ad = pal_.Ds2tp( new AngleDR( alpha, delta ), ad0_ );
                return new double[] { ad.getAlpha(), ad.getDelta() };
            }
            catch ( palError e ) {
                return new double[] { Double.NaN, Double.NaN };
            }
        }

        /**
         * Takes a point on the tangent plane and works out where it would
         * be on the surface of the sphere.
         *
         * @param  x  X coordinate of projected point
         * @param  y  Y coordinate of projected point
         * @return  (alpha, delta) coordinates of point that projects to (x,y)
         */
        public double[] unproject( double x, double y ) {
            AngleDR ad = pal_.Dtp2s( new AngleDR( x, y ), ad0_ );
            return new double[] { ad.getAlpha(), ad.getDelta() };
        }
    }
}
