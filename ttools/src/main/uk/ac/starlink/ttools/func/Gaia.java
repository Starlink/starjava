// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

import uk.ac.starlink.dpac.epoch.EpochTransformation;
import uk.ac.starlink.dpac.math.Edsd;
import uk.ac.starlink.dpac.math.Function;
import uk.ac.starlink.dpac.math.FuncUtils;
import uk.ac.starlink.dpac.math.NumericFunction;
import uk.ac.starlink.ttools.plot.Matrices;

/**
 * Functions related to astrometry suitable for use with data from the
 * Gaia astrometry mission.
 *
 * <p>The methods here are not specific to the Gaia mission,
 * but the parameters of the functions and their units are specified
 * in a form that is convenient for use with Gaia data,
 * in particular the <code>gaia_source</code>
 * catalogue available from
 * <a href="http://gea.esac.esa.int/archive/"
 *         >http://gea.esac.esa.int/archive/</a>
 * and copies or mirrors.
 *
 * <p>There are currently three main sets of functions here:
 * <ul>
 * <li>position and velocity vector calculation and manipulation</li>
 * <li>distance estimation from parallaxes</li>
 * <li>astrometry propagation to different epochs</li>
 * </ul>
 *
 * <p><strong>Position and velocity vectors</strong></p>
 *
 * <p>Functions are provided for converting the astrometric parameters
 * contained in the Gaia catalogue to ICRS Cartesian position (XYZ)
 * and velocity (UVW) vectors.  Functions are also provided to convert
 * these vectors between ICRS and Galactic or Ecliptic coordinates.
 * The calculations are fairly straightforward, and follow the
 * equations laid out in section 1.5.6 of
 * <em>The Hipparcos and Tycho Catalogues</em>,
 * <a href="https://www.cosmos.esa.int/web/hipparcos/catalogues"
 *    >ESA SP-1200</a> (1997)
 * and also section 3.1.7 of the
 * <a href="http://gea.esac.esa.int/archive/documentation/GDR2/"
 *    >Gaia DR2 documentation</a> (2018).
 *
 * <p>These functions will often be combined; for instance to calculate
 * the position and velocity in galactic coordinates from Gaia catalogue
 * values, the following expressions may be useful:
 * <pre>
 *    xyz_gal = icrsToGal(astromXYZ(ra,dec,parallax))
 *    uvw_gal = icrsToGal(astromUVW(array(ra,dec,parallax,pmra,pmdec,radial_velocity)))
 * </pre>
 * though note that these particular examples simply invert
 * parallax to provide distance estimates, which is not generally valid.
 * Note also that these functions do not attempt to correct for
 * solar motion.  Such adjustments should be carried out by hand
 * on the results of these functions if they are required.
 *
 * <p>Functions for calculating errors on the Cartesian components
 * based on the error and correlation quantities from the Gaia catalogue
 * are not currently provided.  They would require fairly complicated
 * invocations.  If there is demand they may be implemented in the future.
 *
 * <p><strong>Distance estimation</strong></p>
 *
 * <p>Gaia measures parallaxes, but some scientific use cases require
 * the radial distance instead. 
 * While distance in parsec is in principle the reciprocal
 * of parallax in arcsec, in the presence of non-negligable errors on
 * measured parallax, this inversion does not give a good estimate of
 * distance.  A thorough discussion of this topic and approaches to
 * estimating distances for Gaia-like data can be found in the papers
 * <ul>
 * <li>C.A.L.Bailer-Jones,
 *     "<em>Estimating distances from parallaxes</em>",
 *     PASP <em>127</em>, p994 (2015)
 *     <a href="http://adsabs.harvard.edu/abs/2015PASP..127..994B"
 *                                           >2015PASP..127..994B</a></li>
 * <li>T.L.Astraatmadja and C.A.L.Bailer-Jones,
 *     "<em>Estimating Distances from Parallaxes.
 *          II. Performance of Bayesian Distance Estimators
 *          on a Gaia-like Catalogue</em>",
 *     ApJ <em>832</em>, a137 (2016)
 *     <a href="http://adsabs.harvard.edu/abs/2016ApJ...832..137A"
 *                                           >2016ApJ...832..137A</a></li>
 * <li>X.Luri et al., "<em>Gaia Data Release 2: Using Gaia Parallaxes</em>",
 *     A&amp;A <em>in press</em> (2018)
 *     <a href="https://arxiv.org/abs/1804.09376"
 *                             >arXiv:1804.09376</a></li>
 * </ul>
 *
 * <p>The functions provided here correspond to calculations from
 * Astraatmadja &amp; Bailer-Jones,
 * "<em>Estimating Distances from Parallaxes.
 *      III. Distances of Two Million Stars in the Gaia DR1 Catalogue</em>",
 * ApJ <em>833</em>, a119 (2016)
 * <a href="http://adsabs.harvard.edu/abs/2016ApJ...833..119A"
 *                                       >2016ApJ...833..119A</a>
 * based on the
 * <strong>Exponentially Decreasing Space Density</strong> prior
 * defined therein.
 * This implementation was written with reference to the Java implementation
 * by Enrique Utrilla (DPAC).
 *
 * <p>These functions are parameterised by a length scale <em>L</em>
 * that defines the exponential decay (the mode of the prior PDF is at
 * <em>r</em>=2<em>L</em>).
 * Some value for this length scale, specified in parsec, must be supplied
 * to the functions as the <code>lpc</code> parameter.
 *
 * <p><strong>Note</strong> that the values provided by these functions
 * do <em>not</em> match those from the paper
 * Bailer-Jones et al.
 * "<em>Estimating Distances from Parallaxes IV:
 *  Distances to 1.33 Billion stars in Gaia Data Release 2"</em>,
 * accepted for AJ (2018)
 * <a href="https://arxiv.org/abs/1804.10121"
 *    >arXiv:1804.10121</a>.
 * The calculations of that paper differ from the ones presented here in
 * several ways:
 * it uses a galactic model for the direction-dependent length scale
 * not currently available here,
 * it pre-applies a parallax correction of -0.029mas, and
 * it uses different uncertainty measures and in some cases (bimodal PDF)
 * a different best distance estimator.
 *
 * <p><strong>Epoch Propagation</strong></p>
 *
 * <p>The Gaia source catalogue provides, for at least some sources,
 * the six-parameter astrometric solution
 * (Right Ascension, Declination, Parallax,
 * Proper motion in RA and Dec, and Radial Velocity),
 * along with errors on these values and correlations between these errors.
 * While a crude estimate of the position at an earlier or later epoch
 * than that of the measurement can be made by multiplying
 * the proper motion components by epoch difference and adding to the
 * measured position, a more careful treatment is required for
 * accurate propagation between epochs of the astrometric parameters,
 * and if required their errors and correlations.
 * The expressions for this are set out in section 1.5.5 (Volume 1) of
 * <em>The Hipparcos and Tycho Catalogues</em>,
 * <a href="https://www.cosmos.esa.int/web/hipparcos/catalogues"
 *    >ESA SP-1200</a> (1997)
 * (but see below), and the code is based on an implementation by
 * Alexey Butkevich and Daniel Michalik (DPAC).
 * A correction is applied to the SP-1200 treatment of
 * radial velocity uncertainty following <em>Michalik et al. 2014</em>
 * <a href="http://ukads.nottingham.ac.uk/abs/2014A%26A...571A..85M"
 *                                           >2014A&amp;A...571A..85M</a>
 * because of their better handling of small radial velocities or parallaxes.
 *
 * <p>The calculations give the same results, though not exactly in
 * the same form, as the epoch propagation functions available
 * in the Gaia archive service.
 *
 * @author   Mark Taylor
 * @since    2 Mar 2018
 */
public class Gaia {

    /**
     * This quantity is A_v, the Astronomical Unit expressed in km.yr/sec.
     * See the Hipparcos catalogue (ESA SP-1200) table 1.2.2 and Eq. 1.5.24.
     */
    public static final double AU_YRKMS = 4.740470446;

    /** Parsec in Astronomical Units, equal to 648000/PI. */
    public static final double PC_AU = ( 180 / Math.PI ) * 60 * 60;

    /** Parsec in units of km.yr/sec. */
    public static final double PC_YRKMS = AU_YRKMS * PC_AU;

    /** The speed of light in km/s (exact). */
    public static final double C_KMS = 299792.458;

    private static final double RVNORM = AU_YRKMS;
    private static final double RVNORM1 = 1.0 / RVNORM;
    private static final double C1_KMS = 1.0 / C_KMS;
    private static final double DEG2RAD = Math.PI / 180.;
    private static final double MAS2RAD = DEG2RAD / ( 3600. * 1000. );
    private static final double RAD2DEG = 1.0 / DEG2RAD;
    private static final double RAD2MAS = 1.0 / MAS2RAD;

    /**
     * ICRS-Galactic transformation matrix, quoted from Gaia DR2
     * documentation (http://gea.esac.esa.int/archive/documentation/GDR2/)
     * Eq 3.61 (sec 3.1.7.1.1).  That refers back to Hipparcos Eq 1.5.11,
     * which provides the transpose with fewer digits of precision.
     */
    private static final double[] AG_PRIME = new double[] {
        -0.0548755604162154, -0.8734370902348850, -0.4838350155487132,
        +0.4941094278755837, -0.4448296299600112, +0.7469822444972189,
        -0.8676661490190047, -0.1980763734312015, +0.4559837761750669,
    };
    private static final double[] AG = Matrices.transpose( AG_PRIME );

    /**
     * ICRS-Ecliptic transformation matrix.
     *
     * <p>Although the Gaia DR2 documentation at time of writing claims the
     * ecliptic transformation uses "the transformation defined in
     * Section 1.5.3" of SP-1200, that's misleading since the coefficients
     * and form of the A_K matrix differ from the Hipparcos document.
     *
     * <p>This matrix is GaiaParam.Nature.TRANSFORMATIONMATRIX_ICRSTOECLIPTIC
     * from GaiaTools class gaia.cu1.params.GaiaParam a.k.a.
     * gaia.cu1.params.BasicParam.  That contains (at DR2) the
     * following detail:
     * <blockquote>
     *   Transformation matrix which transforms the unit-direction vector
     *   r_ecl, expressed in ecliptic coordinates, into the unit-direction
     *   vector r_equ in equatorial coordinates (ICRS): r_equ = A_K times
     *   r_ecl (see also ESA, 1997, 'The Hipparcos and Tycho Catalogues',
     *   ESA SP-1200, Volume 1, Section 1.5.3, inverse of Equation
     *   1.5.12). Note that the ICRS origin is shifted in the equatorial
     *   plane from \Gamma by \phi = 0.05542 arcsec, positive from \Gamma
     *   to the ICRS origin (see J. Chapront, M. Chapront-Touze, G. Francou,
     *   2002, 'A new determination of lunar orbital parameters, precession
     *   constant, and tidal acceleration from LLR measurements', A&amp;A, 387,
     *   700). The ICRS has an unambiguous definition with an origin in the
     *   ICRF equator defined by the realisation of the ICRF. The ecliptic
     *   system is less well-defined, potentially depending on additional
     *   conventions in dynamical theories. The transformation quantified
     *   here corresponds to the inertial mean ecliptic with obliquity (see
     *   parameter :Nature:ObliquityOfEcliptic_J2000) and \Gamma defined by
     *   reference to the ICRS equator (other possibilities include the mean
     *   equator for J2000 or one of the JPL ephemerides equators). Both the
     *   obliquity and the position of \Gamma on the ICRS equator with respect
     *   to the ICRS origin have been obtained from LLR measurements. The
     *   transformation quantified here has no time dependence (there is no
     *   secular variation of the obliquity and no precession): it simply
     *   defines the relative situation of the various planes at J2000.0
     * </blockquote>
     */
    private static final double[] AK = new double[] {
        +0.9999999999999639, +0.0000002465125329, -0.0000001068762105,
        -0.0000002686837421, +0.9174821334228226, -0.3977769913529863,
        +0.0000000000000000, +0.3977769913530006, +0.9174821334228557,
    };
    private static final double[] AK_PRIME = Matrices.transpose( AK );

    /**
     * Private constructor prevents instantiation.
     */
    private Gaia() {
    }

    /**
     * Converts from spherical polar to Cartesian coordinates.
     *
     * @example  <code>polarXYZ(ra, dec, distance_estimate)</code>
     * @example  <code>polarXYZ(l, b, 3262./parallax)</code>
     *           - calculates vector components in units of light year
     *             in the galactic system, on the assumption that distance is
     *             the inverse of parallax
     *
     * @param  phi    longitude in degrees
     * @param  theta  latitude in degrees
     * @param  r      radial distance
     * @return  3-element vector giving Cartesian coordinates
     */
    public static double[] polarXYZ( double phi, double theta, double r ) {
        double cosPhi = TrigDegrees.cosDeg( phi );
        double sinPhi = TrigDegrees.sinDeg( phi );
        double cosTheta = TrigDegrees.cosDeg( theta );
        double sinTheta = TrigDegrees.sinDeg( theta );
        return new double[] { 
            r * cosTheta * cosPhi,
            r * cosTheta * sinPhi,
            r * sinTheta,
        };
    }

    /**
     * Calculates Cartesian components of position from RA, Declination and
     * parallax.  This is a convenience function, equivalent to:
     * <pre>
     *    polarXYZ(ra, dec, 1000./parallax)
     * </pre>
     *
     * <p>Note that this performs distance scaling using a simple
     * inversion of parallax, which is not in general reliable
     * for parallaxes with non-negligable errors.  Use at your own risk.
     *
     * @example  <code>astromXYZ(ra, dec, parallax)</code>
     * @example  <code>icrsToGal(astromXYZ(ra, dec, parallax))</code>
     *
     * @param   ra   Right Ascension in degrees
     * @param   dec  Declination in degrees
     * @param   parallax  parallax in mas
     * @return  3-element vector giving equatorial space coordinates in parsec
     */
    public static double[] astromXYZ( double ra, double dec, double parallax ) {
        double r = 1000. / parallax;
        return polarXYZ( ra, dec, r );
    }

    /**
     * Converts a 3-element vector representing ICRS (equatorial) coordinates
     * to galactic coordinates.
     * This can be used with position or velocity vectors.
     *
     * <p>The input vector is multiplied by the matrix
     * <strong>A<sub>G</sub>'</strong>,
     * given in Eq. 3.61 of the Gaia DR2 documentation, following
     * Eq. 1.5.13 of the Hipparcos catalogue.
     *
     * <p>The output coordinate system is right-handed,
     * with the three components positive in the directions of
     * the Galactic center, Galactic rotation, and the North Galactic Pole
     * respectively.
     *
     * @example  <code>icrsToGal(polarXYZ(ra, dec, distance))</code>
     *
     * @param  xyz  3-element vector giving ICRS Cartesian components
     * @return  3-element vector giving Galactic Cartesian components
     */
    public static double[] icrsToGal( double[] xyz ) {
        return Matrices.mvMult( AG_PRIME, xyz );
    }

    /**
     * Converts a 3-element vector representing galactic coordinates to
     * ICRS (equatorial) coordinates.
     * This can be used with position or velocity vectors.
     *
     * <p>The input vector is multiplied by the matrix
     * <strong>A<sub>G</sub></strong>,
     * given in Eq. 3.61 of the Gaia DR2 documentation, following
     * Eq. 1.5.13 of the Hipparcos catalogue.
     *
     * <p>The input coordinate system is right-handed,
     * with the three components positive in the directions of
     * the Galactic center, Galactic rotation, and the North Galactic Pole
     * respectively.
     *
     * @example  <code>galToIcrs(polarXYZ(l, b, distance))</code>
     *
     * @param  xyz  3-element vector giving Galactic Cartesian components
     * @return  3-element vector giving ICRS Cartesian components
     */
    public static double[] galToIcrs( double[] xyz ) {
        return Matrices.mvMult( AG, xyz );
    }

    /**
     * Converts a 3-element vector representing ICRS (equatorial) coordinates
     * to ecliptic coordinates.
     * This can be used with position or velocity vectors.
     *
     * <p>The transformation corresponds to that between the coordinates
     * <code>(ra,dec)</code> and <code>(ecl_lon,ecl_lat)</code> in the
     * Gaia source catalogue (DR2).
     *
     * @example  <code>icrsToEcl(polarXYZ(ra, dec, distance))</code>
     *
     * @param  xyz  3-element vector giving ICRS Cartesian components
     * @return  3-element vector giving ecliptic Cartesian components
     */
    public static double[] icrsToEcl( double[] xyz ) {
        return Matrices.mvMult( AK_PRIME, xyz );
    }

    /**
     * Converts a 3-element vector representing ecliptic coordinates to
     * ICRS (equatorial) coordinates.
     * This can be used with position or velocity vectors.
     *
     * <p>The transformation corresponds to that between the coordinates
     * <code>(ecl_lon,ecl_lat)</code> and <code>(ra,dec)</code> in the
     * Gaia source catalogue (DR2).
     *
     * @example  <code>eclToIcrs(polarXYZ(ecl_lon, ecl_lat, distance))</code>
     *
     * @param  xyz  3-element vector giving ecliptic Cartesian coordinates
     * @return  3-element vector giving ICRS Cartesian coordinates
     */
    public static double[] eclToIcrs( double[] xyz ) {
        return Matrices.mvMult( AK, xyz );
    }

    /**
     * Calculates Cartesian components of velocity from quantities available
     * in the Gaia source catalogue.
     * The output is in the same coordinate system as the inputs,
     * that is ICRS for the correspondingly-named Gaia quantities.
     *
     * <p>The input astrometry parameters are represented
     * by a 6-element array, with the following elements:
     * <pre>
     * index  gaia_source name  unit    description
     * -----  ----------------  ----    -----------
     *   0:   ra                deg     right ascension
     *   1:   dec               deg     declination
     *   2:   parallax          mas     parallax
     *   3:   pmra              mas/yr  proper motion in ra * cos(dec)
     *   4:   pmdec             mas/yr  proper motion in dec
     *   5:   radial_velocity   km/s    barycentric radial velocity
     * </pre>
     * The units used by this function are the units used in
     * the <code>gaia_source</code> table.
     *
     * <p>This convenience function just invokes the 7-argument
     * <code>astromUVW</code> function
     * using the inverted parallax for the radial distance,
     * and without invoking the Doppler correction.
     * It is exactly equivalent to:
     * <pre>
     *    astromUVW(a[0], a[1], a[3], a[4], a[5], 1000./a[2], false)
     * </pre>
     * Note this naive inversion of parallax to estimate distance is not
     * in general reliable for parallaxes with non-negligable errors.
     *
     * @example  <code>astromUVW(array(ra, dec, parallax, pmra, pmdec,
     *                                 radial_velocity))</code>
     * @example  <code>icrsToGal(astromUVW(array(ra, dec, parallax, pmra, pmdec,
     *                                           radial_velocity)))</code>
     *
     * @param   astrom6   vector of 6 astrometric parameters
     *                    as provided by the Gaia source catalogue
     * @return  3-element vector giving equatorial velocity components in km/s
     */
    public static double[] astromUVW( double[] astrom6 ) {
        double ra = astrom6[ 0 ];
        double dec = astrom6[ 1 ];
        double parallax = astrom6[ 2 ];
        double pmra = astrom6[ 3 ];
        double pmdec = astrom6[ 4 ];
        double radial_velocity = astrom6[ 5 ];
        double r_parsec = 1000. / parallax;
        return astromUVW( ra, dec, pmra, pmdec, radial_velocity, r_parsec,
                          false );
    }

    /**
     * Calculates Cartesian components of velocity from the observed
     * position and proper motion, radial velocity and radial distance,
     * with optional light-time correction.
     * The output is in the same coordinate system as the inputs,
     * that is ICRS for the correspondingly-named Gaia quantities.
     *
     * <p>The radial distance must be supplied using the <code>r_parsec</code>
     * parameter.  A naive estimate from quantities in the Gaia
     * source catalogue may be made with the expression
     * <code>1000./parallax</code>,
     * though note that this simple inversion of parallax
     * is not in general reliable for parallaxes with non-negligable errors.
     *
     * <p>The calculations are fairly straightforward,
     * following Eq. 1.5.74 from the Hipparcos catalogue.
     * A (usually small) Doppler factor accounting for light-time effects
     * can also optionally be applied.  The effect of this is to multiply
     * the returned vector by a factor of <code>1/(1-radial_velocity/c)</code>,
     * as discussed in Eq. 1.2.21 of the Hipparcos catalogue.
     *
     * <p>Note that no attempt is made to adjust for solar motion.
     *
     * @example  <code>astromUVW(ra, dec, pmra, pmdec,
     *                           radial_velocity, dist, true)</code>
     * @example  <code>icrsToGal(astromUVW(ra, dec, pmra, pmdec,
     *                           radial_velocity, 1000./parallax, false))</code>
     *
     * @param  ra     Right Ascension in degrees
     * @param  dec    Declination in degrees
     * @param  pmra   proper motion in RA * cos(dec) in mas/yr
     * @param  pmdec  proper motion in declination in mas/yr
     * @param  radial_velocity  radial velocity in km/s
     * @param  r_parsec   radial distance in parsec
     * @param  useDoppler  whether to apply the Doppler factor to account
     *                     for light-time effects
     * @return  3-element vector giving equatorial velocity components in km/s
     */
    public static double[] astromUVW( double ra, double dec,
                                      double pmra, double pmdec,
                                      double radial_velocity,
                                      double r_parsec, boolean useDoppler ) {
        double cosRa = TrigDegrees.cosDeg( ra );
        double sinRa = TrigDegrees.sinDeg( ra );
        double cosDec = TrigDegrees.cosDeg( dec );
        double sinDec = TrigDegrees.sinDeg( dec );
        double px = -sinRa;
        double qx = -sinDec * cosRa;
        double rx =  cosDec * cosRa;
        double py =  cosRa;
        double qy = -sinDec * sinRa;
        double ry =  cosDec * sinRa;
        double pz =      0;
        double qz =  cosDec;
        double rz =  sinDec;
        double v1 = pmra * AU_YRKMS * 0.001 * r_parsec;
        double v2 = pmdec * AU_YRKMS * 0.001 * r_parsec;
        double v3 = radial_velocity;
        if ( useDoppler ) {
            double kDoppler = 1.0 / ( 1.0 - C1_KMS * radial_velocity );
            v1 *= kDoppler;
            v2 *= kDoppler;
            v3 *= kDoppler;
        }
        return new double[] {
            px * v1 + qx * v2 + rx * v3,
            py * v1 + qy * v2 + ry * v3,
            pz * v1 + qz * v2 + rz * v3,
        };
    }

    /**
     * Propagates the astrometry parameters, supplied as a 6-element array,
     * to a different epoch.
     *
     * <p>The input and output astrometry parameters are each represented
     * by a 6-element array, with the following elements:
     * <pre>
     * index  gaia_source name  unit    description
     * -----  ----------------  ----    -----------
     *   0:   ra                deg     right ascension
     *   1:   dec               deg     declination
     *   2:   parallax          mas     parallax
     *   3:   pmra              mas/yr  proper motion in ra * cos(dec)
     *   4:   pmdec             mas/yr  proper motion in dec
     *   5:   radial_velocity   km/s    barycentric radial velocity
     * </pre>
     * The units used by this function are the units used in
     * the <code>gaia_source</code> table.
     *
     * <p>Null values for <code>parallax</code>, <code>pmra</code>,
     * <code>pmdec</code> and <code>radial_velocity</code>
     * are treated as if zero for the purposes of propagation.
     * The documentation of the equivalent function in the Gaia archive
     * comments <em>"This is a reasonable choice for most stars because
     * those quantities would be either small (parallax and proper motion)
     * or irrelevant (radial velocity).
     * However, this is not true for stars very close to the Sun,
     * where appropriate values need to be taken from the literature
     * (e.g. average velocity field in the solar neighbourhood)."</em>
     *
     * <p>The effect is that the output represents the best estimates
     * available for propagated astrometry; proper motions, parallax and
     * RV are applied if present, but if not the output values are calculated
     * or simply copied across as if those quantities were zero.
     *
     * @example
     *    <code>epochProp(-15.5,
     *          array(ra,dec,parallax,pmra,pmdec,radial_velocity))</code>
     *           - calculates the astrometry at 2000.0 of gaia_source values
     *             that were observed at 2015.5
     *
     * @param   tYr    epoch difference in years
     * @param   astrom6  astrometry at time t0,
     *                   represented by a 6-element array as above
     *                   (a 5-element array is also permitted where
     *                   radial velocity is zero or unknown)
     * @return   astrometry at time <code>t0+tYr</code>,
     *           represented by a 6-element array as above
     */
    public static double[] epochProp( double tYr, double[] astrom6 ) {
        if ( astrom6 == null || astrom6.length < 5 ) {
            return null;
        }

        /* If position or PM is missing, no propagation is possible;
         * return the input values. */
        if ( Double.isNaN( astrom6[ 0 ] ) ||
             Double.isNaN( astrom6[ 1 ] ) ||
             Double.isNaN( astrom6[ 3 ] ) ||
             Double.isNaN( astrom6[ 4 ] ) ) {
            return new double[] {
                astrom6[ 0 ],
                astrom6[ 1 ],
                astrom6[ 2 ],
                astrom6[ 3 ],
                astrom6[ 4 ],
                astrom6.length > 5 ? astrom6[ 5 ] : Double.NaN,
            };
        }
        
        /* Otherwise, attempt to propagate.  NaN parallax and RV are
         * interpreted as zero for this purpose.  */
        boolean hasPlx = ! Double.isNaN( astrom6[ 2 ] );
        boolean hasRv = astrom6.length > 5 && !Double.isNaN( astrom6[ 5 ] );
        double[] out6 = epochProp( tYr, new AstrometryParams( astrom6 ) )
                       .params;

        /* Postprocess the output: ensure that NaN inputs are represented
         * in the output as NaN, rather than some definite value that
         * results from taking them as zero on input.  In absence of
         * parallax, propagation of RV won't make sense either. */
        if ( ! hasPlx ) {
            out6[ 2 ] = Double.NaN;
            out6[ 5 ] = astrom6[ 5 ];
        }
        if ( ! hasRv ) {
            out6[ 5 ] = Double.NaN;
        }
        return out6;
    }

    /**
     * Propagates the astrometry parameters and their associated errors
     * and correlations, supplied as a 22-element array,
     * to a different epoch.
     *
     * <p>The input and output astrometry parameters with associated
     * error and correlation information are each represented by
     * a 22-element array, with the following elements:
     * <pre>
     * index  gaia_source name      unit    description
     * -----  ----------------      ----    -----------
     *   0:   ra                    deg     right ascension
     *   1:   dec                   deg     declination
     *   2:   parallax              mas     parallax
     *   3:   pmra                  mas/yr  proper motion in RA * cos(dec)
     *   4:   pmdec                 mas/yr  proper motion in Declination
     *   5:   radial_velocity       km/s    barycentric radial velocity
     *   6:   ra_error              mas     error in right ascension
     *   7:   dec_error             mas     error in declination
     *   8:   parallax_error        mas     error in parallax
     *   9:   pmra_error            mas/yr  error in RA proper motion * cos(dec)
     *  10:   pmdec_error           mas/yr  error in Declination proper motion
     *  11:   radial_velocity_error km/s    error in barycentric radial velocity
     *  12:   ra_dec_corr                   correlation between ra and dec
     *  13:   ra_parallax_corr              correlation between ra and parallax
     *  14:   ra_pmra_corr                  correlation between ra and pmra
     *  15:   ra_pmdec_corr                 correlation between ra and pmdec
     *  16:   dec_parallax_corr             correlation between dec and parallax
     *  17:   dec_pmra_corr                 correlation between dec and pmra
     *  18:   dec_pmdec_corr                correlation between dec and pmdec
     *  19:   parallax_pmra_corr            correlation between parallax and pmra
     *  20:   parallax_pmdec_corr           correlation between parallax and pmdec
     *  21:   pmra_pmdec_corr               correlation between pmra and pmdec
     * </pre>
     * Note the correlation coefficients, always in the range -1..1,
     * are dimensionless.
     *
     * <p>Null values for <code>parallax</code>, <code>pmra</code>,
     * <code>pmdec</code> and <code>radial_velocity</code>
     * are treated as if zero for the purposes of propagation.
     * The documentation of the equivalent function in the Gaia archive
     * comments <em>"This is a reasonable choice for most stars because
     * those quantities would be either small (parallax and proper motion)
     * or irrelevant (radial velocity).
     * However, this is not true for stars very close to the Sun,
     * where appropriate values need to be taken from the literature
     * (e.g. average velocity field in the solar neighbourhood)."</em>
     *
     * <p>The effect is that the output represents the best estimates
     * available for propagated astrometry; proper motions, parallax and
     * RV are applied if present, but if not the output values are calculated
     * or simply copied across as if those quantities were zero.
     *
     * <p>This transformation is only applicable for radial velocities
     * determined independently of the astrometry, such as those
     * obtained with a spectrometer. It is not applicable for the
     * back-transformation of data already propagated to another epoch.
     *
     * <p>This is clearly an unwieldy function to invoke,
     * but if you are using it with the gaia_source catalogue itself,
     * or other similar catalogues with the same column names and
     * units, you can invoke it by just copying and pasting the
     * example shown in this documentation.
     *
     * @example <code>epochPropErr(-15.5, array(
     *    ra,dec,parallax,pmra,pmdec,radial_velocity,
     *    ra_error,dec_error,parallax_error,pmra_error,pmdec_error,radial_velocity_error,
     *    ra_dec_corr,ra_parallax_corr,ra_pmra_corr,ra_pmdec_corr,
     *    dec_parallax_corr,dec_pmra_corr,dec_pmdec_corr,
     *    parallax_pmra_corr,parallax_pmdec_corr,
     *    pmra_pmdec_corr))</code>
     *    - calculates the astrometry with all errors and correlations at
     *    2000.0 for gaia_source values that were observed at 2015.5.
     *
     * @param  tYr     epoch difference in years
     * @param  astrom22  astrometry at time t0,
     *                   represented by a 22-element array as above
     * @return  astrometry at time t0+tYr,
     *          represented by a 22-element array as above
     */
    public static double[] epochPropErr( double tYr, double[] astrom22 ) {
        if ( astrom22 == null || astrom22.length != 22 ) {
            return null;
        }

        /* If position or PM is missing ,no propagation is possible;
         * return the input values. */
        if ( Double.isNaN( astrom22[ 0 ] ) ||
             Double.isNaN( astrom22[ 1 ] ) ||
             Double.isNaN( astrom22[ 3 ] ) ||
             Double.isNaN( astrom22[ 4 ] ) ) {
            return astrom22.clone();
        }

        /* Otherwise, attempt to propagate.  NaN parallax and RV are
         * interpreted as zero for this purpose. */
        boolean hasPlx = ! Double.isNaN( astrom22[ 2 ] );
        boolean hasRv = ! Double.isNaN( astrom22[ 5 ] );

        /* Prepare the inputs to the CU1 epoch propagation routine,
         * that is the astrometric parameter vector and its covariance
         * matrix.  For the first 5 astrometric parameters, this is just
         * a matter of getting the units right - radians and years.*/
        double ra0 = astrom22[ 0 ] * DEG2RAD;
        double dec0 = astrom22[ 1 ] * DEG2RAD;
        double plx0 = hasPlx ? astrom22[ 2 ] * MAS2RAD : 0;
        double pmra0 = astrom22[ 3 ] * MAS2RAD;
        double pmdec0 = astrom22[ 4 ] * MAS2RAD;
        double rvkms0 = hasRv ? astrom22[ 5 ] : 0.0;
        double errRa0 = astrom22[ 6 ] * MAS2RAD;
        double errDec0 = astrom22[ 7 ] * MAS2RAD;
        double errPlx0 = astrom22[ 8 ] * MAS2RAD;
        double errPmra0 = astrom22[ 9 ] * MAS2RAD;
        double errPmdec0 = astrom22[ 10 ] * MAS2RAD;
        double errRvkms0 = hasRv ? astrom22[ 11 ] : 0.0;
        double corrRaDec0 = astrom22[ 12 ];
        double corrRaPlx0 = astrom22[ 13 ];
        double corrRaPmra0 = astrom22[ 14 ];
        double corrRaPmdec0 = astrom22[ 15 ];
        double corrDecPlx0 = astrom22[ 16 ];
        double corrDecPmra0 = astrom22[ 17 ];
        double corrDecPmdec0 = astrom22[ 18 ];
        double corrPlxPmra0 = astrom22[ 19 ];
        double corrPlxPmdec0 = astrom22[ 20 ];
        double corrPmraPmdec0 = astrom22[ 21 ];
        double zeta0 = rvkms0 * plx0 * RVNORM1;
        double[] a0 = new double[ 6 ];
        a0[ 0 ] = ra0;
        a0[ 1 ] = dec0;
        a0[ 2 ] = plx0;
        a0[ 3 ] = pmra0;
        a0[ 4 ] = pmdec0;
        a0[ 5 ] = zeta0;
        double[][] cov0 = new double[ 6 ][ 6 ];
        cov0[ 0 ][ 0 ] = errRa0 * errRa0;
        cov0[ 1 ][ 1 ] = errDec0 * errDec0;
        cov0[ 2 ][ 2 ] = errPlx0 * errPlx0;
        cov0[ 3 ][ 3 ] = errPmra0 * errPmra0;
        cov0[ 4 ][ 4 ] = errPmdec0 * errPmdec0;
        cov0[ 0 ][ 1 ] = cov0[ 1 ][ 0 ] = errRa0 * errDec0 * corrRaDec0;
        cov0[ 0 ][ 2 ] = cov0[ 2 ][ 0 ] = errRa0 * errPlx0 * corrRaPlx0;
        cov0[ 0 ][ 3 ] = cov0[ 3 ][ 0 ] = errRa0 * errPmra0 * corrRaPmra0;
        cov0[ 0 ][ 4 ] = cov0[ 4 ][ 0 ] = errRa0 * errPmdec0 * corrRaPmdec0;
        cov0[ 1 ][ 2 ] = cov0[ 2 ][ 1 ] = errDec0 * errPlx0 * corrDecPlx0;
        cov0[ 1 ][ 3 ] = cov0[ 3 ][ 1 ] = errDec0 * errPmra0 * corrDecPmra0;
        cov0[ 1 ][ 4 ] = cov0[ 4 ][ 1 ] = errDec0 * errPmdec0 * corrDecPmdec0;
        cov0[ 2 ][ 3 ] = cov0[ 3 ][ 2 ] = errPlx0 * errPmra0 * corrPlxPmra0;
        cov0[ 2 ][ 4 ] = cov0[ 4 ][ 2 ] = errPlx0 * errPmdec0 * corrPlxPmdec0;
        cov0[ 3 ][ 4 ] = cov0[ 4 ][ 3 ] = errPmra0 * errPmdec0 * corrPmraPmdec0;

        /* Radial velocity covariance matrix elements are more complicated.
         * The CU1 routine wants the normalised RV (zeta) in radians/year,
         * which requires some non-trivial conversion to get from the
         * gaia_source radial_velocity in barycentric km/s.
         * The formulae are provided by Eq. 17 of Michalik et al. 2014
         * (2014A&A...571A..85M), which themselves are a generalisation
         * of those from Eq. 1.5.69 of the Hipparcos Catalogue (SP-1200). */
        if ( hasRv ) {
            double rva = rvkms0 * RVNORM1;  // units of yr^-1
            for ( int i = 0; i < 5; i++ ) {
                cov0[ i ][ 5 ] = cov0[ 5 ][ i ] = rva * cov0[ i ][ 2 ];
            }
            cov0[ 5 ][ 5 ] = RVNORM1 * RVNORM1
                           * ( Maths.square( plx0 * errRvkms0 ) +
                               Maths.square( rvkms0 * errPlx0 ) +
                               Maths.square( errRvkms0 * errPlx0 ) );
        }

        /* Invoke the actual CU1 propagation routine. */
        double[] a1 = new double[ 6 ];
        double[][] cov1 = new double[ 6 ][ 6 ];
        EpochTransformation.propagate( tYr, a0, cov0, a1, cov1 );

        /* Extract named values from the returned matrices. */
        double ra1 = a1[ 0 ];
        double dec1 = a1[ 1 ];
        double plx1 = a1[ 2 ];
        double pmra1 = a1[ 3 ];
        double pmdec1 = a1[ 4 ];
        double zeta1 = a1[ 5 ];
        double errRa1 = Math.sqrt( cov1[ 0 ][ 0 ] );
        double errDec1 = Math.sqrt( cov1[ 1 ][ 1 ] );
        double errPlx1 = Math.sqrt( cov1[ 2 ][ 2 ] );
        double errPmra1 = Math.sqrt( cov1[ 3 ][ 3 ] );
        double errPmdec1 = Math.sqrt( cov1[ 4 ][ 4 ] );
        double errZeta1 = Math.sqrt( cov1[ 5 ][ 5 ] );
        double corrRaDec1 = cov1[ 0 ][ 1 ] / ( errRa1 * errDec1 );
        double corrRaPlx1 = cov1[ 0 ][ 2 ] / ( errRa1 * errPlx1 );
        double corrRaPmra1 = cov1[ 0 ][ 3 ] / ( errRa1 * errPmra1 );
        double corrRaPmdec1 = cov1[ 0 ][ 4 ] / ( errRa1 * errPmdec1 );
        double corrDecPlx1 = cov1[ 1 ][ 2 ] / ( errDec1 * errPlx1 );
        double corrDecPmra1 = cov1[ 1 ][ 3 ] / ( errDec1 * errPmra1 );
        double corrDecPmdec1 = cov1[ 1 ][ 4 ] / ( errDec1 * errPmdec1 );
        double corrPlxPmra1 = cov1[ 2 ][ 3 ] / ( errPlx1 * errPmra1 );
        double corrPlxPmdec1 = cov1[ 2 ][ 4 ] / ( errPlx1 * errPmdec1 );
        double corrPmraPmdec1 = cov1[ 3 ][ 4 ] / ( errPmra1 * errPmdec1 );

        /* Invert Michalik et al. 2014 Eq. 17. */
        final double rvkms1;
        final double errRvkms1;
        boolean hasRv1 = hasRv && hasPlx && plx1 != 0;
        if ( hasRv1 ) {
            rvkms1 = RVNORM * zeta1 / plx1;
            errRvkms1 =
                Math.sqrt( ( RVNORM * RVNORM * cov1[ 5 ][ 5 ] -
                             rvkms1 * rvkms1 * cov1[ 2 ][ 2 ] )
                         / ( plx1 * plx1 + cov1[ 2 ][ 2 ] ) );
        }
        else {
            rvkms1 = Double.NaN;
            errRvkms1 = Double.NaN;
        }

        /* Assign the values to the output array, with unit conversions
         * as required.  For quantities that may have received spurious
         * values as a consequence of NaN->0 mapping on input, copy the
         * input values across to the output unchanged. */
        double[] out22 = new double[ 22 ];
        out22[ 0 ] = ra1 * RAD2DEG;
        out22[ 1 ] = dec1 * RAD2DEG;
        out22[ 2 ] = hasPlx ? plx1 * RAD2MAS : Double.NaN;
        out22[ 3 ] = pmra1 * RAD2MAS;
        out22[ 4 ] = pmdec1 * RAD2MAS;
        out22[ 5 ] = hasRv1 ? rvkms1 : astrom22[ 5 ];
        out22[ 6 ] = errRa1 * RAD2MAS;
        out22[ 7 ] = errDec1 * RAD2MAS;
        out22[ 8 ] = errPlx1 * RAD2MAS;
        out22[ 9 ] = errPmra1 * RAD2MAS;
        out22[ 10 ] = errPmdec1 * RAD2MAS;
        out22[ 11 ] = hasRv1 ? errRvkms1 : astrom22[ 11 ];
        out22[ 12 ] = corrRaDec1;
        out22[ 13 ] = hasPlx ? corrRaPlx1 : astrom22[ 13 ];
        out22[ 14 ] = corrRaPmra1;
        out22[ 15 ] = corrRaPmdec1;
        out22[ 16 ] = hasPlx ? corrDecPlx1 : astrom22[ 16 ];
        out22[ 17 ] = corrDecPmra1;
        out22[ 18 ] = corrDecPmdec1;
        out22[ 19 ] = hasPlx ? corrPlxPmra1 : astrom22[ 19 ];
        out22[ 20 ] = hasPlx ? corrPlxPmdec1 : astrom22[ 20 ];
        out22[ 21 ] = corrPmraPmdec1;
        return out22;
    }

    /**
     * Converts from normalised radial velocity in mas/year to 
     * unnormalised radial velocity in km/s.
     *
     * <p>The output is calculated as
     * <code>AU_YRKMS * rvMasyr / plxMas</code>,
     * where <code>AU_YRKMS=4.740470446</code>
     * is one Astronomical Unit in km.yr/sec.
     *
     * @param  rvMasyr  normalised radial velocity, in mas/year
     * @param  plxMas   parallax in mas
     * @return  radial velocity in km/s
     */
    public static double rvMasyrToKms( double rvMasyr, double plxMas ) {
        return plxMas == 0 ? Double.NaN : rvMasyr * RVNORM / plxMas;
    }

    /**
     * Converts from unnormalised radial velocity in km/s to
     * normalised radial velocity in mas/year.
     *
     * <p>The output is calculated as
     * <code>rvKms * plxMas / AU_YRKMS</code>,
     * where <code>AU_YRKMS=4.740470446</code>
     * is one Astronomical Unit in km.yr/sec.
     *
     * @param  rvKms  unnormalised radial velocity, in mas/year
     * @param  plxMas  parallax in mas
     * @return  radial velocity in mas/year
     */
    public static double rvKmsToMasyr( double rvKms, double plxMas ) {
        return rvKms * plxMas * RVNORM1;
    }

    /**
     * Propagates the astrometry parameters, supplied as an AstrometryParams
     * object, to a different epoch.
     *
     * @param  tYr   epoch difference in years
     * @param  ap  astrometry parameters at time t0
     * @return  astrometry parameters at time t0+tYr
     */
    private static AstrometryParams epochProp( double tYr,
                                               AstrometryParams ap ) {
        double[] a0 = fromDm( ap );
        double[] a1 = new double[ 6 ];
        EpochTransformation.propagate( tYr, a0, a1 );
        return toDm( a1 );
    }

    /**
     * Best estimate of distance using the Exponentially Decreasing
     * Space Density prior.
     * This estimate is provided by the mode of the PDF.
     *
     * @param   plxMas  parallax in mas
     * @param   plxErrorMas   parallax error in mas
     * @param   lPc    length scale in parsec
     * @return   best distance estimate in parsec
     */
    public static double distanceEstimateEdsd( double plxMas,
                                               double plxErrorMas,
                                               double lPc ) {
        return 1000.0 
             * new Edsd( plxMas, plxErrorMas, lPc * 0.001 )
              .getBestEstimation();
    }

    /**
     * Calculates the 5th and 95th percentile confidence intervals
     * on the distance estimate using the Exponentially Decreasing
     * Space Density prior.
     *
     * <p>Note this function has to numerically integrate the PDF
     * to determine quantile values, so it is relatively slow.
     *
     * @param  plxMas  parallax in mas
     * @param  plxErrorMas  parallax error in mas
     * @param   lPc    length scale in parsec
     * @return  2-element array giving the 5th and 95th percentiles in parsec
     *          of the EDSD distance PDF 
     */
    public static double[] distanceBoundsEdsd( double plxMas,
                                               double plxErrorMas,
                                               double lPc ) {
        return distanceQuantilesEdsd( plxMas, plxErrorMas, lPc, 0.05, 0.95 );
    }

    /**
     * Calculates arbitrary quantiles for the distance estimate
     * using the Exponentially Decreasing Space Density prior.
     *
     * <p>Note this function has to numerically integrate the PDF
     * to determine quantile values, so it is relatively slow.
     *
     * @example
     *     <code>distanceQuantilesEdsd(parallax, parallax_error,
     *                                 1350, 0.5)[0]</code>
     *     calculates the median of the EDSD distance PDF
     *     using a length scale of 1.35kpc
     *
     * @example
     *     <code>distanceQuantilesEdsd(parallax, parallax_error,
     *                                 3000, 0.01, 0.99)</code>
     *     returns a 2-element array giving the 1st and 99th percentile
     *     of the distance estimate using a length scale of 3kpc
     *
     * @param  plxMas  parallax in mas
     * @param  plxErrorMas  parallax error in mas
     * @param  lPc    length scale in parsec
     * @param  qpoints  one or more required quantile cut points,
     *                  each in the range 0..1
     * @return   array with one element for each of the supplied
     *           <code>qpoints</code>
     *           giving the corresponding distance in parsec
     */
    public static double[] distanceQuantilesEdsd( double plxMas,
                                                  double plxErrorMas,
                                                  double lPc,
                                                  double... qpoints ) {
        int nq = qpoints.length;
        Edsd edsd = new Edsd( plxMas, plxErrorMas, lPc * 0.001 );
        double tol = 1e-6;
        NumericFunction ncdf = edsd.calculateCdf( tol );

        /* Quadratic interpolation works better here than e.g. splines,
         * look at the results for e.g. plx=40, plxError=0.75. */
        Function scdf = FuncUtils.interpolateQuadratic( ncdf );
        double[] qvs = new double[ nq ];
        double rmin = 0;
        double rmax = ncdf.getX( ncdf.getCount() - 1 );
        double ytol = 0.00001;
        for ( int i = 0; i < nq; i++ ) {
            qvs[ i ] = 1000.
                     * FuncUtils.findValueMonotonic( scdf, rmin, rmax,
                                                     qpoints[ i ], ytol );
        }
        return qvs;
    }

    /**
     * Converts a distance in parsec to a distance modulus.
     * The formula is <code>5*log10(distPc)-5</code>.
     *
     * @param  distPc  distance in parsec
     * @return  distance modulus in magnitudes
     */
    public static double distanceToModulus( double distPc ) {
        return 5.0 * Math.log10( distPc ) - 5.0;
    }

    /**
     * Converts a distance modulus to a distance in parsec.
     * The formula is <code>10^(1+distmod/5)</code>.
     *
     * @param   distmod  distance modulus in magnitudes
     * @return    distance in parsec
     */
    public static double modulusToDistance( double distmod ) {
        return Math.pow( 10, 1.0 + 0.2 * distmod );
    }

    /**
     * Converts from CU9-style astrometry parameter object to
     * a CU1-style vector.
     *
     * @param   ap  CU9-style astrometry parameter object
     * @return   CU1-style astrometry vector
     */
    private static double[] fromDm( AstrometryParams ap ) {
        double raDeg = ap.ra;
        double decDeg = ap.dec;
        double plxMas = ap.plx;
        double pmraMasyr = ap.pmra;
        double pmdecMasyr = ap.pmdec;
        double rvKms = ap.rv;
        double raRad = raDeg * DEG2RAD;
        double decRad = decDeg * DEG2RAD;
        double plxRad = plxMas * MAS2RAD;
        double pmraRadyr = pmraMasyr * MAS2RAD;
        double pmdecRadyr = pmdecMasyr * MAS2RAD;
        double normRvRadyr = rvKms * plxRad * RVNORM1;
        return new double[] {
            raRad, decRad, plxRad, pmraRadyr, pmdecRadyr, normRvRadyr,
        };
    }

    /**
     * Converts from CU1-style astrometry vector to
     * a CU9-style object.
     *
     * @param  a  CU1-style astrometry vector
     * @return  CU9-styel astrometry parameter object
     */
    private static AstrometryParams toDm( double[] a ) {
        double raRad = a[ 0 ];
        double decRad = a[ 1 ];
        double plxRad = a[ 2 ];
        double pmraRadyr = a[ 3 ];
        double pmdecRadyr = a[ 4 ];
        double normRvRadyr = a[ 5 ];
        double raDeg = raRad * RAD2DEG;
        double decDeg = decRad * RAD2DEG;
        double plxMas = plxRad * RAD2MAS;
        double pmraMasyr = pmraRadyr * RAD2MAS;
        double pmdecMasyr = pmdecRadyr * RAD2MAS;
        double rvKms = plxRad == 0 ? Double.NaN
                                   : normRvRadyr / ( plxRad * RVNORM1 );
        return new AstrometryParams( raDeg, decDeg, plxMas,
                                     pmraMasyr, pmdecMasyr, rvKms );
    }
}
