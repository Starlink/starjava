/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     12-MAR-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

/**
 * Class for generating a gaussian profile.
 *
 * @author Peter W. Draper.
 * @version $Id$
 */
public class GaussianGenerator
    extends GaussianFitter
    implements FunctionGenerator
{
    //
    // Implementation: just sub-class GaussianFitter avoiding the fitting
    // part.
    //

    /**
     * Generate positions for a gaussian profile.
     *
     * @param scale the scale height.
     * @param centre the centre.
     * @param sigma the gaussian sigma.
     */
    public GaussianGenerator( double scale, double centre, double sigma )
    {
        super();
        params[SCALE] = scale;
        params[CENTRE] = centre;
        params[SIGMA] = sigma;
    }

    //
    // Just repeat these for clarity.
    //

    /**
     * Evaluate the Gaussian function at a set of given X positions.
     *
     * @param x array of X positions at which to evaluate.
     * @return array of values at given X's.
     */
    public double[] evalYDataArray( double[] x )
    {
        return super.evalYDataArray( x );
    }

    /**
     * Evaluate the Gaussian function at a single position.
     *
     * @param x X position at which to evaluate.
     * @return value at X
     */
    public double evalYData( double x )
    {
        return super.evalYData( x );
    }
}
