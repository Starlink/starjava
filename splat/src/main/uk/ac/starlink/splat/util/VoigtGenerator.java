/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     28-FEB-2003 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.util;

/**
 * Class for generating a Voigt profile. 
 *
 * @author Peter W. Draper
 * @version $Id$
 */      
public class VoigtGenerator 
    extends VoigtFitter
    implements FunctionGenerator
{
    //
    // Implementation: just sub-class VoigtFitter avoiding the fitting
    // part.
    //

    /**
     * Generate positions for a Voigt profile.
     *
     * @param scale  the scale height.
     * @param centre the centre.
     * @param gwidth the Gaussian width.
     * @param lwidth the Lorentzian width.
     */
    public VoigtGenerator( double scale, double centre, double gwidth, 
                           double lwidth )
    {
        this( scale, false, centre, false, gwidth, false, lwidth, false );
    }

    /**
     * Generate positions for a Voigt profile. Also records which parameters
     * should be considered fixed by any fitting procedures.
     *
     * @param scale  the scale height.
     * @param scaleFixed should scale be considered fixed.
     * @param centre the centre.
     * @param centreFixed should centre be considered fixed.
     * @param gwidth the Gaussian width.
     * @param gwidthFixed should gwidth be considered fixed.
     * @param lwidth the Lorentzian width.
     * @param lwidthFixed should lwidth be considered fixed.
     */
    public VoigtGenerator( double scale, boolean scaleFixed,
                           double centre, boolean centreFixed,
                           double gwidth, boolean gwidthFixed,
                           double lwidth, boolean lwidthFixed )
    {
        params[SCALE] = scale;
        params[CENTRE] = centre;
        params[GWIDTH] = gwidth;
        params[LWIDTH] = lwidth;

        fixed[SCALE] = scaleFixed;
        fixed[CENTRE] = centreFixed;
        fixed[GWIDTH] = gwidthFixed;
        fixed[LWIDTH] = lwidthFixed;

        setPeak();
    }
    
    //
    // Just repeat these for clarity.
    //

    /**
     * Evaluate the Voigt function at a set of given X positions.
     *
     * @param x array of X positions at which to evaluate.
     * @return array of values at given X's.
     */
    public double[] evalYDataArray( double[] x )
    {
        return super.evalYDataArray( x );
    }

    /**
     * Evaluate the Voigt function at a single position.
     *
     * @param x X position at which to evaluate.
     * @return value at X
     */
    public double evalYData( double x )
    {
        return super.evalYData( x );
    }
}
