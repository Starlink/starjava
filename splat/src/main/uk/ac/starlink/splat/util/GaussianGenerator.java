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
        this( scale, false, centre, false, sigma, false );
    }

    /**
     * Generate positions for a gaussian profile. Also records which
     * parameters should be considered fixed by any fitting procedures.
     *
     * @param scale the scale height.
     * @param scaleFixed should scale be considered fixed.
     * @param centre the centre.
     * @param centreFixed should centre be considered fixed.
     * @param sigma the gaussian sigma.
     * @param sigmaFixed should sigma be considered fixed.
     */
    public GaussianGenerator( double scale, boolean scaleFixed,
                              double centre, boolean centreFixed,
                              double sigma, boolean sigmaFixed )
    {
        params[SCALE] = scale;
        params[CENTRE] = centre;
        params[SIGMA] = sigma;
        
        fixed[SCALE] = scaleFixed;
        fixed[CENTRE] = centreFixed;
        fixed[SIGMA] = sigmaFixed;
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
