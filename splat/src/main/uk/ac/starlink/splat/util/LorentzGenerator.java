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
     * Generate positions for a Lorentzian
     *
     * @param scale the scale height.
     * @param centre the centre.
     * @param width the lorentzian width.
     */
    public LorentzGenerator( double scale, double centre, double width )
    {
        this( scale, false, centre, false, width, false );
    }

    /**
     * Generate positions for a Lorentzian. Also records which parameters
     * should be considered fixed by any fitting procedures.
     *
     * @param scale the scale height.
     * @param scaleFixed should scale be considered fixed.
     * @param centre the centre.
     * @param centreFixed should centre be considered fixed.
     * @param width the lorentzian width.
     * @param widthFixed should width be considered fixed.
     */
    public LorentzGenerator( double scale, boolean scaleFixed,
                             double centre, boolean centreFixed,
                             double width, boolean widthFixed )
    {
        params[SCALE] = scale;
        params[CENTRE] = centre;
        params[WIDTH] = width;

        fixed[SCALE] = scaleFixed;
        fixed[CENTRE] = centreFixed;
        fixed[WIDTH] = widthFixed;
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
