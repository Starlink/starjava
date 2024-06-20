// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical in software terms.

package uk.ac.starlink.ttools.func;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Functions for converting between different measures of cosmological 
 * distance.  
 *
 * <p>The following parameters are used:
 * <ul>
 * <li><strong>z</strong>: redshift</li>
 * <li><strong>H0</strong>: Hubble constant in km/sec/Mpc
 *         (example value ~70)</li>
 * <li><strong>omegaM</strong>: Density ratio of the universe
 *         (example value 0.3)</li>
 * <li><strong>omegaLambda</strong>: Normalised cosmological constant
 *         (example value 0.7)</li>
 * </ul>
 * <p>For a flat universe, <code>omegaM</code>+<code>omegaLambda</code>=1
 *
 * <p>The terms and formulae used here are taken from the 
 * paper by D.W.Hogg, <em>Distance measures in cosmology</em>,
 * <a href="http://arxiv.org/abs/astro-ph/9905116">astro-ph/9905116</a> v4 
 * (2000).
 *
 * @author   Mark Taylor
 * @since    26 Jan 2007
 */
public class Distances {

    /** Speed of light in m/s. */
    public static final double SPEED_OF_LIGHT = 2.99792458e8;

    /** Number of metres in a parsec. */
    public static final double METRE_PER_PARSEC = 3.08567758e16;

    /** Number of seconds in a year. */
    public static final double SEC_PER_YEAR = 60 * 60 * 24 * 365.25;

    private static final Map<List<Double>,Integrator> distIntegratorMap_ =
        new HashMap<List<Double>,Integrator>();
    private static final Map<List<Double>,Integrator> timeIntegratorMap_ =
        new HashMap<List<Double>,Integrator>();

    private static final double EXAMPLE_H0 = 71;
    private static final double EXAMPLE_OMEGA_M = 0.27;
    private static final double EXAMPLE_OMEGA_LAMBDA = 1.0 - EXAMPLE_OMEGA_M;

    /**
     * Private constructor prevents instantiation.
     */
    private Distances() {
    }

    /**
     * Converts from MegaParsecs to metres.
     *
     * @param   distMpc  distance in Mpc
     * @return  distance in m
     */
    public static double MpcToM( double distMpc ) {
        return distMpc * 1e6 * METRE_PER_PARSEC;
    }

    /**
     * Converts from metres to MegaParsecs.
     *
     * @param   distM  distance in m
     * @return  distance in Mpc
     */
    public static double mToMpc( double distM ) {
        return distM / METRE_PER_PARSEC / 1e6;
    }

    /**
     * Quick and dirty function for converting from redshift to distance.
     * <p><strong>Warning</strong>: this makes some reasonable
     * assumptions about the cosmology and returns the luminosity
     * distance.  It is only intended for approximate use.  If you care
     * about the details, use one of the more specific functions here.
     *
     * @param   z  redshift
     * @return  some distance measure in Mpc
     */
    public static double zToDist( double z ) {
        return luminosityDistance( z, EXAMPLE_H0, EXAMPLE_OMEGA_M,
                                      EXAMPLE_OMEGA_LAMBDA );
    }

    /**
     * Quick and dirty function for converting from redshift to time.
     * <p><strong>Warning</strong>: this makes some reasonable
     * assumptions about the cosmology.  It is only intended for approximate
     * use.  If you care about the details use one of the more specific
     * functions here.
     *
     * @param   z  redshift
     * @return  'age' of photons from redshift <code>z</code> in Gyr
     */
    public static double zToAge( double z ) {
        return lookbackTime( z, EXAMPLE_H0, EXAMPLE_OMEGA_M,
                                EXAMPLE_OMEGA_LAMBDA );
    }

    /**
     * Line-of-sight comoving distance.
     *
     * @param   z  redshift
     * @param   H0  Hubble constant in km/sec/Mpc
     * @param   omegaM density ratio of the universe
     * @param   omegaLambda  normalised cosmological constant
     * @return  line-of-sight comoving distance in Mpc
     */
    public static double comovingDistanceL( double z, double H0, double omegaM,
                                            double omegaLambda ) {
        return dH( H0 )
             * getDistIntegrator( omegaM, omegaLambda ).integral( z );
    }

    /**
     * Transverse comoving distance.
     *
     * @param   z  redshift
     * @param   H0  Hubble constant in km/sec/Mpc
     * @param   omegaM density ratio of the universe
     * @param   omegaLambda  normalised cosmological constant
     * @return  transverse comoving distance in Mpc
     */
    public static double comovingDistanceT( double z, double H0, double omegaM,
                                            double omegaLambda ) {
        double omegaK = omegaK( omegaM, omegaLambda );
        double dC = comovingDistanceL( z, H0, omegaM, omegaLambda );
        if ( omegaK == 0 ) {
            return dC;
        }
        else if ( omegaK < 0 ) {
            double dH = dH( H0 );
            double oks = Math.sqrt( - omegaK );
            return dH / oks  * Math.sin( oks * dC / dH );

        }
        else if ( omegaK > 0 ) {
            double dH = dH( H0 );
            double oks = Math.sqrt( omegaK );
            return dH / oks * Maths.sinh( oks * dC / dH );
        }
        else {
            assert Double.isNaN( omegaK );
            return Double.NaN;
        }
    }

    /**
     * Angular diameter distance.
     *
     * @param   z  redshift
     * @param   H0  Hubble constant in km/sec/Mpc
     * @param   omegaM density ratio of the universe
     * @param   omegaLambda  normalised cosmological constant
     * @return  angular diameter distance in Mpc
     */
    public static double angularDiameterDistance( double z, double H0,
                                                  double omegaM,
                                                  double omegaLambda ) {
        return comovingDistanceT( z, H0, omegaM, omegaLambda ) / ( 1 + z );
    }

    /**
     * Luminosity distance.
     *
     * @param   z  redshift
     * @param   H0  Hubble constant in km/sec/Mpc
     * @param   omegaM density ratio of the universe
     * @param   omegaLambda  normalised cosmological constant
     * @return  luminosity distance in Mpc
     */
    public static double luminosityDistance( double z, double H0, double omegaM,
                                             double omegaLambda ) {
        return comovingDistanceT( z, H0, omegaM, omegaLambda ) * ( 1 + z );
    }

    /**
     * Lookback time.  This returns the difference between the age of
     * the universe at time of observation (now) and the age of the
     * universe at the time when photons of redshift <code>z</code>
     * were emitted.
     *
     * @param   z  redshift
     * @param   H0  Hubble constant in km/sec/Mpc
     * @param   omegaM density ratio of the universe
     * @param   omegaLambda  normalised cosmological constant
     * @return  lookback time in Gyr
     */
    public static double lookbackTime( double z, double H0, double omegaM,
                                       double omegaLambda ) {
        return tH( H0 )
             * getTimeIntegrator( omegaM, omegaLambda ).integral( z );
    }

    /**
     * Comoving volume.  This returns the all-sky total comoving
     * volume out to a given redshift <code>z</code>.
     *
     *
     * @param   z  redshift
     * @param   H0  Hubble constant in km/sec/Mpc
     * @param   omegaM density ratio of the universe
     * @param   omegaLambda  normalised cosmological constant
     * @return  comoving volume in Gpc<sup>3</sup>
     */
    public static double comovingVolume( double z, double H0, double omegaM,
                                         double omegaLambda ) {
        double dH = dH( H0 ) * 1e-3;
        double dM = comovingDistanceT( z, H0, omegaM, omegaLambda ) * 1e-3;
        double omegaK = omegaK( omegaM, omegaLambda );
        if ( omegaK == 0 ) {
            return 4.0 * Math.PI / 3.0 * dM * dM * dM;
        }
        else if ( omegaK < 0 ) {
            double oks = Math.sqrt( - omegaK );
            double dMH = dM / dH;
            double t1 = dMH * Math.sqrt( 1.0 + omegaK * dMH * dMH );
            double t2 = Math.asin( oks * dMH ) / oks;
            return 2.0 * Math.PI * dH * dH * dH / omegaK * ( t1 - t2 );
        }
        else if ( omegaK > 0 ) {
            double oks = Math.sqrt( omegaK );
            double dMH = dM / dH;
            double t1 = dMH * Math.sqrt( 1.0 + omegaK * dMH * dMH );
            double t2 = Maths.asinh( oks * dMH ) / oks;
            return 2.0 * Math.PI * dH * dH * dH / omegaK * ( t1 - t2 );
        }
        else {
            assert Double.isNaN( omegaK );
            return Double.NaN;
        }
    }

    /**
     * Returns the Hubble distance in Mpc given the Hubble constant in
     * km/sec/Mpc.
     *
     * @param   H0  Hubble constant in km/sec/Mpc
     * @return  Hubble distance in Mpc
     */
    private static double dH( double H0 ) {
        return SPEED_OF_LIGHT * 1e-3 / H0;
    }

    /**
     * Returns the Hubble time in Gyr given the Hubble constant in
     * km/sec/Mpc.
     *
     * @param   H0  Hubble constant in km/sec/Mpc
     * @return  Hubble time in Gyr
     */
    private static double tH( double H0 ) {
        double h0PerSec = H0 * 1e3 / METRE_PER_PARSEC / 1e6;
        double thSec = 1.0 / h0PerSec;
        double thGyr = thSec / SEC_PER_YEAR / 1e9;
        return thGyr;
    }

    /**
     * Calculates the curvature of space given omegaM and omegaLambda.
     *
     * @param   omegaM density ratio of the universe
     * @param   omegaLambda  normalised cosmological constant
     * @return  curvature of space
     */
    private static double omegaK( double omegaM, double omegaLambda ) {
        return 1.0 - omegaM - omegaLambda;
    }

    /**
     * Returns the quantity E defined in eq (14) of Hogg.
     *
     * @param   z  redshift
     * @param   omegaM density ratio of the universe
     * @param   omegaLambda  normalised cosmological constant
     * @return  E
     */
    private static double E( double z, double omegaM, double omegaLambda ) {
        double omegaK = omegaK( omegaM, omegaLambda );
        double zz = 1.0 + z;
        double zz2 = zz * zz;
        double zz3 = zz2 * zz;
        return Math.sqrt( omegaM * zz3 + omegaK * zz2 + omegaLambda );
    }

    /**
     * Returns an integrator object which can integrate 1/E(z)dz 
     * for given values of cosmology.  Integrators are created 
     * lazily and not relinquished.
     *
     * @param   omegaM density ratio of the universe
     * @param   omegaLambda  normalised cosmological constant
     * @return  integrator
     */
    private static Integrator getDistIntegrator( final double omegaM,
                                                 final double omegaLambda ) {
        List<Double> key = createKey( omegaM, omegaLambda );
        if ( ! distIntegratorMap_.containsKey( key ) ) {
            distIntegratorMap_.put( key, new Integrator( 0.0, 0.02 ) {
                public double function( double z ) {
                    return 1.0 / E( z, omegaM, omegaLambda );
                }
            } );
        }
        return distIntegratorMap_.get( key );
    }

    /**
     * Returns an integrator object which can integrate 1/(1+z)/E(z)dz
     * for given values of cosmology.  Integrators are created lazily
     * and not relinquished.
     *
     * @param   omegaM density ratio of the universe
     * @param   omegaLambda  normalised cosmological constant
     * @return  integrator
     */
    private static Integrator getTimeIntegrator( final double omegaM,
                                                 final double omegaLambda ) {
        List<Double> key = createKey( omegaM, omegaLambda );
        if ( ! timeIntegratorMap_.containsKey( key ) ) {
            timeIntegratorMap_.put( key, new Integrator( 0.0, 0.02 ) {
                public double function( double z ) {
                    return 1.0 / ( 1.0 + z ) / E( z, omegaM, omegaLambda );
                }
            } );
        }
        return timeIntegratorMap_.get( key );
    }

    /**
     * Returns an object which can be used as a map key based on the arguments.
     * The returned object will be equal, in the sense of
     * {@link java.lang.Object#equals}, to another object returned by a
     * later call to this method with the same arguments.
     *
     * @param   omegaM  first value
     * @param   omegaLambda  second value
     */
    private static List<Double> createKey( double omegaM, double omegaLambda ) {
        return Arrays.asList( new Double[] { Double.valueOf( omegaM ),
                                             Double.valueOf( omegaLambda ), } );
    }
}
