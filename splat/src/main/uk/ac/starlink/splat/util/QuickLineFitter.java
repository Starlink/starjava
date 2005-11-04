/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     24-JAN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

/**
 * Measure some quick properties of a spectral line. This is based on
 * the FIGARO application ABLINE. The results are:
 * <ul>
 *   <li> median position of the line</li>
 *
 *   <li> width of the line (equiv to gaussian FWHM) </li>
 *
 *   <li> peak value </li>
 * 
 *   <li> flux/integrated intensity </li>
 *
 *   <li> equivalent width </li>
 *
 *   <li> asymmetry parameter </li>
 *
 *   <li> line type (absorption or emission). </li>
 * </ul>
 * Using this class is straight forward, just select an appropriate
 * constructor and then use the getter methods to query the results of
 * the measurements. If no background is available then it is not
 * possible to measure the equivalent width.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class QuickLineFitter
{
    // Does not extend FunctionFitter as very specialised.
    /**
     * The centre of the spectral line.
     */
    private double centre = 0.0;

    /**
     * The equivalent width of the spectral line.
     */
    private double equivalentWidth = 0.0;

    /**
     * The integrated intensity of the spectral line.
     */
    private double flux = 0.0;

    /**
     * The width of the spectral line.
     */
    private double width = 0.0;

    /**
     * The asymmetry of the spectral line.
     */
    private double asymmetry = 0.0;

    /**
     * The peak value of spectrum.
     */
    private double peak = 0.0;

    /**
     * Whether line is absorption or emission.
     */
    private boolean absorption = true;

    /**
     *  Create an instance.
     *
     *  @param xcoords the wavelength coordinates of the data and
     *                 background values (i.e. x coordinates).
     *  @param data the spectra data (extracted parts only, i.e. whole
     *              range is used, no checks for SpecData.BAD values).
     *  @param back the background values for each data value.
     *              Null for none (note equivalent width meaningless).
     *              Note it is assumed these values are already
     *              subtracted from the data.
     *  @param backValue a single value used for the background. Only
     *                   used if back is null. Set to zero if not available.
     *                   Note if used it is assumed that this value is
     *                   subtracted from the background.
     */
    public QuickLineFitter( double[] xcoords, double[] data, double[] back,
                            double backValue )
    {
        doCalc( xcoords, data, back, backValue );
    }

    /**
     * Perform the calculations.
     *
     *  @param xcoords the wavelength coordinates of the data and
     *                 background values.
     *  @param data the background subtracted spectra data.
     *  @param back the background values for each data value. May be
     *              NULL.
     *  @param backValue a background value to be used if back is NULL.
     */
    protected void doCalc( double[] xcoords, double[] data, double[] back,
                           double backValue )
    {
        //  Determine the mean interval between values (TODO: assumes
        //  wavelength step is linear, this may need revision).
        double sum = 0.0;
        double sign = 1.0;
        double scale = ( xcoords[xcoords.length - 1] - xcoords[0] ) /
                               (double)(xcoords.length - 1 );
        boolean warned = false;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        flux = 0.0;

        //  Create the sum of background subtracted and normalised
        //  intensities.
        if ( back != null || backValue != 0.0 ) {
            if ( back != null ) {
                for ( int i = 0; i < xcoords.length; i++ ) {
                    if ( data[i] > max ) {
                        max = data[i];
                    }
                    if ( data[i] < min ) {
                        min = data[i];
                    }
                    sum -= data[i] / back[i];
                    flux += data[i];
                }
            }
            else {
                for ( int i = 0; i < xcoords.length; i++ ) {
                    if ( data[i] > max ) {
                        max = data[i];
                    }
                    if ( data[i] < min ) {
                        min = data[i];
                    }
                    sum -= data[i] / backValue;
                    flux += data[i];
                }
            }
        }
        else {
            //  Background is zero.
            for ( int i = 0; i < xcoords.length; i++ ) {
                if ( data[i] > max ) {
                    max = data[i];
                }
                if ( data[i] < min ) {
                    min = data[i];
                }
                sum -= data[i];
                flux += data[i];
            }
        }

        if ( sum < 0.0 ) {

            //  Must be an emission line.
            absorption = false;
            sum = -sum;
            sign = -1.0;
            peak = max;
        }
        else {

            //  Must be an absorption line.
            absorption = true;
            sign = 1.0;
            peak = -min;
        }
        centre = splitSum( sum, 0.5, data, back, backValue, xcoords, sign );

        // Now find X values for which area up to this value = 0.1587
        // and 0.8413 of the total.  For a Gaussian these give +/- 1
        // standard deviation from the median.  Compute width and
        // asymmetry parameters.
        double xsigl = splitSum( sum, 0.1587, data, back, backValue,
                                 xcoords, sign );
        double xsigu = splitSum( sum, 0.8413, data, back, backValue,
                                 xcoords, sign );

        width = 1.1775 * ( xsigu - xsigl );
        asymmetry = ( xsigu + xsigl ) * 0.5 - centre;
        asymmetry = asymmetry * 100.0 / width;

        // Calculate equivalent width in units of wavelength (xcoords)
        // by scaling sum by dispersion, if known.
        if ( back != null || backValue != 0.0 ) {
            equivalentWidth = sum * scale;
        }
        else {
            equivalentWidth = 0.0;
        }

        // Flux is also scaled by dispersion, so depends on linear spacing.
        flux *= scale;
    }

    /**
     * Get the centre (median) of the line.
     */
    public double getCentre()
    {
        return centre;
    }

    /**
     * Get the equivalent width of the line.
     */
    public double getEquivalentWidth()
    {
        return equivalentWidth;
    }

    /**
     * Get the width of the line (same as gaussian FWHM).
     */
    public double getWidth()
    {
        return width;
    }

    /**
     * Get the asymmetry of the line.
     */
    public double getAsymmetry()
    {
        return asymmetry;
    }

    /**
     * Get the peak value of the line.
     */
    public double getPeak()
    {
        return peak;
    }

    /**
     * Return the type of line, emission or absorption.
     */
    public boolean isAbsorption()
    {
        return absorption;
    }

    /**
     * Get the flux of the line fit. Return the integrated intensity, but
     * clearly this is just an approximation and depends on a linear 
     * interval between measurements for any sense.
     */
    public double getFlux()
    {
        return flux;
    }

    public String toString()
    {
        return "QuickLineFitter[" +
            "centre = " + getCentre() + ", " +
            "peak = " + getPeak() + ", " +
            "width = " + getWidth() + ", " +
            "equivalent width = " + getEquivalentWidth() + ", " +
            "flux = " + getFlux() + ", " +
            "asymmetry = " + getAsymmetry() + ", " +
            "absorption = " + isAbsorption() +
            "]";
    }

    /**
     * Computes data value for which area from is frac times total area.
     * Obviously a frac of 0.5 is the median.
     *
     * @param sum total of 1 - (data+back)/back.
     * @param frac fraction of sum required.
     * @param data the spectral data values (y axis).
     * @param back background values for data value (can be null).
     * @param backValue a background value for all data (used if back is null).
     * @param xcoords the "wavelength" of each data value (X axis).
     * @param sign 1.0 if line is absorption, -1.0 if emission.
     *
     * @return the value that devides the sum into frac and 1-frac parts.
     *
     * @see "Figaro:splitsum"
     */
    protected double splitSum( double sum, double frac, double[] data,
                               double[] back, double backValue,
                               double[] xcoords, double sign )
    {
        if ( sum == 0.0 ) {
            return data[0];
        }
        double result = 0.0;
        double a = 0.0;
        int ubound = data.length;
        int mbound = ubound - 2;
        double exch = 0.0;
        if ( backValue == 0.0 ) backValue = 1.0;

        if ( back != null ) {
            for ( int i = 0; i < ubound - 2; i++ ) {
                if ( back[i] != 0.0 ) {
                    a += ( 1.0 - ( data[i] + back[i] ) / back[i] ) * sign;
                }
                if ( a > frac * sum ) {
                    mbound = i - 1;
                    break;
                }
            }
        }
        else {
            for ( int i = 0; i < ubound - 2; i++ ) {
                a += ( 1.0 - ( data[i] + backValue ) / backValue ) * sign;
                if ( a > frac * sum ) {
                    mbound = i - 1;
                    break;
                }
            }
        }
        if ( mbound == -1 ) mbound = 0;

        double exes = a - frac * sum;
        double step = (xcoords[ubound-1] - xcoords[0])/(double)(ubound);
        if ( back != null ) {
            if ( back[mbound+1] != 0.0 ) {
                exch = 1.0 - exes /
                       ( 1.0 - ( data[mbound+1] + back[mbound+1] ) /
                         back[mbound+1] ) * sign;
            }
            else {
                exch = 0.0;
            }
        }
        else {
            exch = 1.0 - exes /
                ( 1.0 - ( data[mbound+1] + backValue ) / backValue ) * sign;
        }
        result = xcoords[mbound] + step * ( exch + 0.5 );
        return result;
    }
}
