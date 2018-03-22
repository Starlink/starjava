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
 * <p>There are currently two main sets of functions here,
 * distance estimation from parallaxes, and astrometry propagation to
 * different epochs.
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
 * <li>X.Luri et al., "<em>On the use of Gaia parallaxes</em>",
 *     A&amp;A <em>in press</em> (2018)</li>
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
 
    private static final double RVNORM = AU_YRKMS;
    private static final double RVNORM1 = 1.0 / RVNORM;
    private static final double DEG2RAD = Math.PI / 180.;
    private static final double MAS2RAD = DEG2RAD / ( 3600. * 1000. );
    private static final double RAD2DEG = 1.0 / DEG2RAD;
    private static final double RAD2MAS = 1.0 / MAS2RAD;

    /**
     * Private constructor prevents instantiation.
     */
    private Gaia() {
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
        return epochProp( tYr, new AstrometryParams( astrom6 ) )
              .params;
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
     * <p>This is clearly an unwieldy function to invoke,
     * but if you are using it with the gaia_source catalogue itself,
     * or other similar catalogues with the same column names and
     * units, you can invoke it by just copying and pasting the
     * example shown in this documentation.
     *
     * <p>This transformation is only applicable for radial velocities
     * determined independently of the astrometry, such as those
     * obtained with a spectrometer. It is not applicable for the
     * back-transformation of data already propagated to another epoch.
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
        boolean hasRv = ! Double.isNaN( astrom22[ 5 ] );

        /* Prepare the inputs to the CU1 epoch propagation routine,
         * that is the astrometric parameter vector and its covariance
         * matrix.  For the first 5 astrometric parameters, this is just
         * a matter of getting the units right - radians and years.*/
        double ra0 = astrom22[ 0 ] * DEG2RAD;
        double dec0 = astrom22[ 1 ] * DEG2RAD;
        double plx0 = astrom22[ 2 ] * MAS2RAD;
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
        if ( hasRv ) {
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
         * as required. */
        double[] out22 = new double[ 22 ];
        out22[ 0 ] = ra1 * RAD2DEG;
        out22[ 1 ] = dec1 * RAD2DEG;
        out22[ 2 ] = plx1 * RAD2MAS;
        out22[ 3 ] = pmra1 * RAD2MAS;
        out22[ 4 ] = pmdec1 * RAD2MAS;
        out22[ 5 ] = rvkms1;
        out22[ 6 ] = errRa1 * RAD2MAS;
        out22[ 7 ] = errDec1 * RAD2MAS;
        out22[ 8 ] = errPlx1 * RAD2MAS;
        out22[ 9 ] = errPmra1 * RAD2MAS;
        out22[ 10 ] = errPmdec1 * RAD2MAS;
        out22[ 11 ] = errRvkms1;
        out22[ 12 ] = corrRaDec1;
        out22[ 13 ] = corrRaPlx1;
        out22[ 14 ] = corrRaPmra1;
        out22[ 15 ] = corrRaPmdec1;
        out22[ 16 ] = corrDecPlx1;
        out22[ 17 ] = corrDecPmra1;
        out22[ 18 ] = corrDecPmdec1;
        out22[ 19 ] = corrPlxPmra1;
        out22[ 20 ] = corrPlxPmdec1;
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
        return rvMasyr * RVNORM / plxMas;
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
        double rvKms = normRvRadyr / ( plxRad * RVNORM1 );
        return new AstrometryParams( raDeg, decDeg, plxMas,
                                     pmraMasyr, pmdecMasyr, rvKms );
    }
}
