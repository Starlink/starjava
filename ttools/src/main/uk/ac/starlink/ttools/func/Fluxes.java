// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

/**
 * Functions for conversion between flux and magnitude values.
 * Functions are provided for conversion between flux in Janskys and
 * AB magnitudes.
 * <p>Some constants for approximate conversions between different magnitude
 * scales are also provided:
 * <ul>
 * <li>Constants <code>JOHNSON_AB_*</code>, for Johnson &lt;-&gt; AB magnitude
 *     conversions, from
 *     Frei and Gunn, Astronomical Journal <em>108</em>, 1476 (1994),
 *     Table 2
 *     (<a href="https://ui.adsabs.harvard.edu/abs/1994AJ....108.1476F"
 *                                                >1994AJ....108.1476F</a>).
 *     </li>
 * <li>Constants <code>VEGA_AB_*</code>, for Vega &lt;-&gt; AB magnitude
 *     conversions, from
 *     Blanton et al., Astronomical Journal <em>129</em>, 2562 (2005),
 *     Eqs. (5)
 *     (<a href="https://ui.adsabs.harvard.edu/abs/2005AJ....129.2562B"
 *                                                >2005AJ....129.2562B</a>).
 *     </li>
 * </ul>
 *
 * @author   Mark Taylor
 * @since    30 Jun 2006
 */
public class Fluxes {

    /**
     * Private constructor prevents instantiation.
     */
    private Fluxes() {
    }

    /**
     * Converts AB magnitude to flux in Jansky.
     * <p>F/Jy=10<sup>(23-(AB+48.6)/2.5)</sup>
     *
     * @param   magAB   AB magnitude value
     * @return  equivalent flux in Jansky
     */
    public static double abToJansky( double magAB ) {
        return Math.pow( 10.0, 23 - ( magAB + 48.6 ) / 2.5 );
    }

    /**
     * Converts flux in Jansky to AB magnitude.
     * <p>AB=2.5*(23-log<sub>10</sub>(F/Jy))-48.6
     *
     * @param   fluxJansky   flux in Jansky
     * @return  equivalent AB magnitude
     */
    public static double janskyToAb( double fluxJansky ) {
        return 2.5 * ( 23.0 - Maths.log10( fluxJansky ) ) - 48.6;
    }

    /**
     * Converts luminosity to flux given a luminosity distance.
     * <p>F=lumin/(4 x Pi x dist<sup>2</sup>)
     *
     * @param   lumin  luminosity
     * @param   dist   luminosity distance
     * @return  equivalent flux
     */
    public static double luminosityToFlux( double lumin, double dist ) {
        return 0.25 * lumin /  Math.PI / ( dist * dist );
    }

    /**
     * Converts flux to luminosity given a luminosity distance.
     * <p>lumin=(4 x Pi x dist<sup>2</sup>) F
     *
     * @param   flux   flux
     * @param   dist   luminosity distance
     * @return  equivalent luminosity
     */
    public static double fluxToLuminosity( double flux, double dist ) {
        return 4.0 * Math.PI * dist * dist * flux;
    }

    /**
     * Approximate offset between Johnson and AB magnitudes in V band.
     * V<sub>J</sub>~=V<sub>AB</sub>+<code>JOHNSON_AB_V</code>.
     */
    public static final double JOHNSON_AB_V = +0.044;

    /**
     * Approximate offset between Johnson and AB magnitudes in B band.
     * B<sub>J</sub>~=B<sub>AB</sub>+<code>JOHNSON_AB_B</code>.
     */
    public static final double JOHNSON_AB_B = +0.163;

    /**
     * Approximate offset between Johnson and AB magnitudes in Bj band.
     * Bj<sub>J</sub>~=Bj<sub>AB</sub>+<code>JOHNSON_AB_Bj</code>.
     */
    public static final double JOHNSON_AB_Bj = +0.139;

    /**
     * Approximate offset between Johnson and AB magnitudes in R band.
     * R<sub>J</sub>~=R<sub>AB</sub>+<code>JOHNSON_AB_R</code>.
     */
    public static final double JOHNSON_AB_R = -0.055;

    /**
     * Approximate offset between Johnson and AB magnitudes in I band.
     * I<sub>J</sub>~=I<sub>AB</sub>+<code>JOHNSON_AB_I</code>.
     */
    public static final double JOHNSON_AB_I = -0.309;

    /**
     * Approximate offset between Johnson and AB magnitudes in g band.
     * g<sub>J</sub>~=g<sub>AB</sub>+<code>JOHNSON_AB_g</code>.
     */
    public static final double JOHNSON_AB_g = +0.013;

    /**
     * Approximate offset between Johnson and AB magnitudes in r band.
     * r<sub>J</sub>~=r<sub>AB</sub>+<code>JOHNSON_AB_r</code>.
     */
    public static final double JOHNSON_AB_r = +0.226;

    /**
     * Approximate offset between Johnson and AB magnitudes in i band.
     * i<sub>J</sub>~=i<sub>AB</sub>+<code>JOHNSON_AB_i</code>.
     */
    public static final double JOHNSON_AB_i = +0.296;

    /**
     * Approximate offset between Johnson and AB magnitudes in Rc band.
     * Rc<sub>J</sub>~=Rc<sub>AB</sub>+<code>JOHNSON_AB_Rc</code>.
     */
    public static final double JOHNSON_AB_Rc = -0.117;

    /**
     * Approximate offset between Johnson and AB magnitudes in Ic band.
     * Ic<sub>J</sub>~=Ic<sub>AB</sub>+<code>JOHNSON_AB_Ic</code>.
     */
    public static final double JOHNSON_AB_Ic = -0.342;

    /**
     * Offset between Johnson and AB magnitudes in u' band (zero).
     * u'<sub>J</sub>=u'<sub>AB</sub>+<code>JOHNSON_AB_uPrime</code>=u'<sub>AB</sub>.
     */
    public static final double JOHNSON_AB_uPrime = 0.0;

    /**
     * Offset between Johnson and AB magnitudes in g' band (zero).
     * g'<sub>J</sub>=g'<sub>AB</sub>+<code>JOHNSON_AB_gPrime</code>=g'<sub>AB</sub>.
     */
    public static final double JOHNSON_AB_gPrime = 0.0;

    /**
     * Offset between Johnson and AB magnitudes in r' band (zero).
     * r'<sub>J</sub>=r'<sub>AB</sub>+<code>JOHNSON_AB_rPrime</code>=r'<sub>AB</sub>.
     */
    public static final double JOHNSON_AB_rPrime = 0.0;

    /**
     * Offset between Johnson and AB magnitudes in i' band (zero).
     * i'<sub>J</sub>=i'<sub>AB</sub>+<code>JOHNSON_AB_iPrime</code>=i'<sub>AB</sub>.
     */
    public static final double JOHNSON_AB_iPrime = 0.0;

    /**
     * Offset between Johnson and AB magnitudes in z' band (zero).
     * z'<sub>J</sub>=z'<sub>AB</sub>+<code>JOHNSON_AB_zPrime</code>=z'<sub>AB</sub>.
     */
    public static final double JOHNSON_AB_zPrime = 0.0;

    /**
     * Approximate offset between Vega (as in 2MASS) and AB magnitudes 
     * in J band.
     * J<sub>Vega</sub>~=J<sub>AB</sub>+<code>VEGA_AB_J</code>.
     */
    public static final double VEGA_AB_J = -0.91;

    /**
     * Approximate offset between Vega (as in 2MASS) and AB magnitudes 
     * in H band.
     * H<sub>Vega</sub>~=H<sub>AB</sub>+<code>VEGA_AB_H</code>.
     */
    public static final double VEGA_AB_H = -1.39;

    /**
     * Approximate offset between Vega (as in 2MASS) and AB magnitudes
     * in K band.
     * K<sub>Vega</sub>~=K<sub>AB</sub>+<code>VEGA_AB_K</code>.
     */
    public static final double VEGA_AB_K = -1.85;
}
