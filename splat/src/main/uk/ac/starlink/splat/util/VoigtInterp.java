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
 * Creates {@link Interpolator}s for drawing interactive Voigt profiles.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class VoigtInterp
    extends LineInterpolator
{
    /**
     * Create an instance with no coordinates. A call to
     * {@link #setValues} must be made before any other methods.
     */
    public VoigtInterp()
    {
        super( 5 );
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
    public VoigtInterp( double[] x, double[] y )
    {
        super( 5 );
        setValues( x, y );
    }

    protected void bootstrap( double[] x, double[] y )
    {
        this.x = new double[5];
        this.y = new double[5];
        
        // Anchor.
        this.x[0] = x[1] - 2.0 * ( x[1] - x[0] );
        
        // Gaussian FWHM
        this.x[1] = x[0];
        
        //  Center.
        this.x[2] = x[1];
        
        //  Lorentz FWHM.
        this.x[3] = x[1] + ( x[1] - x[0] );
        
        // Anchor.
        this.x[4] = x[1] + 2.0 * ( x[1] - x[0] );
        
        this.y[0] = y[1] - 2.0 * ( y[1] - y[0] );
        this.y[1] = y[0];
        this.y[2] = y[1];
        this.y[3] = y[0];
        this.y[4] = this.y[0];    
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
     *    position), the Gaussian FWHM of the line. 
     *    The width is therefore x[2]-x[1]
     *    and the scale 2*(y[2]-y[1]). Note the width returned is the
     *    Gaussian width, not the FWHM.</ul>
     *    <ul> 2: the X coordinate defines the centre of the line and
     *    the Y coordinate the peak.
     *    <ul> 3: the X and Y coordinates define (with respect to the 2
     *    position), the Lorentzian FWHM of the line. 
     *    The width is therefore x[2]-x[3] Note the width returned is the
     *    Lorentzian width, not the FWHM.</ul>
     *    <ul> 4: (usually present, but not used here) the X
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
        props[SCALE] = 2.0 * ( y[2] - y[1] );
        props[CENTRE] = x[2];
        props[GWIDTH] = Math.abs( 2.0 * ( x[2] - x[1] ) /
                                  GaussianGenerator.FWHMFAC );
        props[LWIDTH] = Math.abs( 2.0 * ( x[2] - x[3] ) /
                                  LorentzGenerator.FWHMFAC );

        return props;
    }

    public void setProps( double[] props )
    {
        generator = new VoigtGenerator( props[SCALE], props[CENTRE],
                                        props[GWIDTH], props[LWIDTH] );
    }
}
