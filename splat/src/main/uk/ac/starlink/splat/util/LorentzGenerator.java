/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     12-MAR-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

/**
 * Class for generating a lorenztian profile.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class LorentzGenerator
    extends LorentzFitter
    implements FunctionGenerator
{
    /**
     * Generate positions for a lorentzian
     *
     * @param scale the scale height.
     * @param centre the centre.
     * @param width the lorentzian width.
     */
    public LorentzGenerator( double scale, double centre, double width )
    {
        params[SCALE] = scale;
        params[CENTRE] = centre;
        params[WIDTH] = width;
    }

    //
    // Just repeat these for clarity.
    //

    /**
     * Evaluate the Lorenztian function at a set of given X positions.
     *
     * @param x array of X positions at which to evaluate.
     * @return array of values at given X's.
     */
    public double[] evalYDataArray( double[] x )
    {
        return super.evalYDataArray( x );
    }

    /**
     * Evaluate the Lorenztian function at a single position.
     *
     * @param x X position at which to evaluate.
     * @return value at X
     */
    public double evalYData( double x )
    {
        return super.evalYData( x );
    }
}
