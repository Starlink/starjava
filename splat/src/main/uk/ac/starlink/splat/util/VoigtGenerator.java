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
        params[SCALE] = scale;
        params[CENTRE] = centre;
        params[GWIDTH] = gwidth;
        params[LWIDTH] = lwidth;
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
