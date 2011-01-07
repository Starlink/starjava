/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     01-FEB-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import uk.ac.starlink.splat.data.AnalyticSpectrum;

/**
 * Interface for functions that perform fitting of a function
 * to a spectral line using a {@link LevMarq} non-linear minimisation.
 * It extends the {@link AnalyticSpectrum} and {@link LevMarqFunc}
 * interfaces.
 * <p>
 * Examples of derived classes are GaussianFitter, LorentzFitter and
 * VoigtFitter. A abstract base class for FunctionFitters is provided in
 * {@link AbstractFunctionFitter}.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see GaussianFitter
 * @see LorentzFitter
 * @see VoigtFitter
 * @see AbstractFunctionFitter
 * @see AnalyticSpectrum
 * @see LevMarqFunc
 */
public interface FunctionFitter
    extends AnalyticSpectrum, LevMarqFunc
{
    /**
     * Get Chi square for the fit.
     */
    public double getChi();

    /**
     * Get the centre of fit.
     */
    public double getCentre();

    /**
     * Get the scale height of fit.
     */
    public double getScale();

    /**
     * Get the integrated intensity of the fit.
     */
    public double getFlux();

    /**
     * Calculate an RMS to the given data for the fit.
     *
     * @param x array of X coordinates.
     * @param y array of data values for each X coordinate.
     */
    public double calcRms( double[] x, double[] y );

    /**
     * Calculate an RMS to the given data and pre-calculated fit.
     *
     * @param x array of X coordinates.
     * @param y array of data values for each X coordinate.
     * @param fity array of model values for each X coordinate.
     */
    public double calcRms( double[] x, double[] y, double[] fity );

    /**
     * Calculate the individual residuals between the given data and the
     * pre-calculated fit.
     *
     * @param x array of X coordinates.
     * @param y array of data values for each X coordinate.
     * @return the residuals. 
     */
    public double[] calcResiduals( double[] x, double[] y );

    /**
     * Return the number of parameters used to describe this
     * function. This is the size of the {@link LevMarqFunc#eval}
     * a parameter.
     *
     * @return number of parameters used to describe this function.
     */
    public int getNumParams();

    /**
     * Return the parameters of this function as a list.
     */
    public double[] getParams();

    /**
     * Set the parameters of this function from an array of doubles.
     */
    public void setParams( double[] params );

    /**
     * Return the parameters error estimates of this function as a list.
     */
    public double[] getPErrors();

    /**
     * Set the parameters error estimates. Not normally used.
     */
    public void setPErrors( double[] params );

    /**
     * Return the fixed or floating states of the various parameters. This
     * determines if the values are fixed and should not be changed,
     * or may float. The default state should be floating.
     */
    public boolean[] getFixed();

    /**
     * Set the fixed state of the various parameters.
     */
    public void setFixed( boolean[] fixed );
}
