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
{
    /**
     * Generate positions for a lorentzian
     *
     * @param scale the scale height.
     * @param centre the centre.
     * @param sigma the lorentzian width.
     */
    public LorentzGenerator( double scale, double centre, double width )
    {
        this.scale = scale;
        this.centre = centre;
        this.width = width;
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
    public double[] evalArray( double[] x )
    {
        return super.evalArray( x );
    }

    /**
     * Evaluate the Lorenztian function at a single position.
     *
     * @param x X position at which to evaluate.
     * @return value at X
     */
    public double evalPoint( double x )
    {
        return super.evalPoint( x );
    }
}
