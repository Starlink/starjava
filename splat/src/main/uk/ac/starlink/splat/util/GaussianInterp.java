/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     26-MAR-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import uk.ac.starlink.diva.interp.Interpolator;
import uk.ac.starlink.diva.interp.LinearInterp;

/**
 * Creates {@link Interpolator}s for drawing interactive Gaussian profiles.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class GaussianInterp
    extends LineInterpolator
{
    /**
     * Create an instance with no coordinates. A call to
     * {@link #setValues} must be made before any other methods.
     */
    public GaussianInterp()
    {
        super( 4 );
    }

    /**
     * Create an instance with the given coordinates. Interpolation
     * is by X coordinate see the {@link #interpolate}  method.
     * The X coordinates should be monotonic, either increasing or
     * decreasing. Same value X coordinates are not allowed.
     *
     * @param x the X coordinates.
     * @param y the Y coordinates.
     */
    public GaussianInterp( double[] x, double[] y )
    {
        super( 4 );
        setValues( x, y );
    }

    /**
     * Given a set of reference X and Y coordinates return an array of
     * properties that describe a related spectral line. Usually these
     * are the line position, scale and width, but Voigt will also
     * require another width (Lorentz). The scale will be negative for
     * an absorption line, if the Y coordinate system run from top to
     * bottom (i.e. graphics coordinates).
     * <p>
     * The reference positions are:
     * <li>
     *    <ul> 0: (not used here) the X coordinate is used to define
     *    the extent that the line is drawn out to.</ul>
     *    <ul> 1: the X and Y coordinates define (with respect to the 2
     *    position), the FWHM of the line. The width is therefore x[2]-x[1]
     *    and the scale 2*(y[2]-y[1]). Note the width returned is the
     *    Lorentzian width, not the FWHM.</ul>
     *    <ul> 2: the X coordinate defines the centre of the line and
     *    the Y coordinate the peak.
     *    <ul> 3: (usually present, but not used here) the X
     *    coordinate is used to define the extent that the line is
     *    drawn out to.
     * </li>
     *
     * @param x the x coordinates, must be at least 3.
     * @param y the y coordinates, must be at least 3.
     * @return the scale, centre and width, indexed by SCALE, CENTRE and
     *         GWIDTH.
     */
    public double[] getProps( double[] x, double[] y )
    {
        double[] props = new double[4];
        if ( x.length > 2 ) {
            props[SCALE] = 2.0 * ( y[2] - y[1] );
            props[CENTRE] = x[2];
            props[GWIDTH] = Math.abs( 2.0 * ( x[1] - x[2] ) /
                                      GaussianGenerator.FWHMFAC );
        }
        return props;
    }

    // Set the properties of the generator directly.
    public void setProps( double[] props )
    {
        generator = new GaussianGenerator( props[SCALE], props[CENTRE],
                                           props[GWIDTH] );
    }
}
