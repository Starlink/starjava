/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     14-DEC-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

/**
 * AnalyticSpectrum is an interface for spectral-like objects that
 * themselves using an analytic function or procedure of
 * somekind. Expected uses are to recalculate a spectrum to match the
 * data positions of another spectrum that it is to be compared with
 * in some way (for instance an object defining this interface could
 * be a fit to a continuum that requires redrawing to compare with a
 * different spectrum tha has a different grid).
 *
 * Semi-analytic procedures (such as interpolation of a real spectrum)
 * should also implement this interface.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public interface AnalyticSpectrum
{
    /**
     *  Return the value of the spectrum at an X position.
     */
    public double evalYData( double x );

    /**
     *  Return an array of values at a series of positions.
     */
    public double[] evalYDataArray( double[] x );
}

