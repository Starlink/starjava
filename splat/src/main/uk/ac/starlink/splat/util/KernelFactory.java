/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     12-MAR-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

/**
 * Factory class for standard set of weighting kernels.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class KernelFactory
{
    //  Private constructor as static class.
    private KernelFactory() {}

    /**
     *  Filter widths are always odd number, greater than 1.
     */
    private static int getSetWidth( int width )
    {
        width = ( width / 2 ) * 2 + 1;
        if ( width == 1 ) width = 3;
        return width;
    }

    /**
     * Create gaussian kernel.
     */
    public static double[] gaussianKernel( int width, double fwhm )
    {
        width = getSetWidth( width );
        double[] kernel = new double[width];

        // Centre of kernel.
        int centre = width / 2;

        //  Convert fwhm to sigma (/2*sqrt(2*ln(2)).
        double sigma = fwhm / 2.35482;

        // Generator for positions.
        GaussianGenerator generator =
            new GaussianGenerator( 1.0, centre, sigma );

        for ( int j = 0; j < width; j++ ) {
            kernel[j] = generator.evalYData( (double) j );
        }

        return kernel;
    }

    /**
     * Create lorentzian kernel.
     */
    public static double[] lorentzKernel( int width, double lwidth )
    {
        width = getSetWidth( width );
        double[] kernel = new double[width];

        // Centre of kernel.
        int centre = width / 2;

        // Generator for positions.
        LorentzGenerator generator =
            new LorentzGenerator( 1.0, centre, lwidth );

        for ( int j = 0; j < width; j++ ) {
            kernel[j] = generator.evalYData( (double) j );
        }

        return kernel;
    }

    /**
     * Create voigt kernel.
     */
    public static double[] voigtKernel( int width, double gwidth,
                                        double lwidth )
    {
        width = getSetWidth( width );
        double[] kernel = new double[width];

        // Centre of kernel.
        int centre = width / 2;

        // Generator for positions.
        VoigtGenerator generator =
            new VoigtGenerator( 1.0, centre, gwidth, lwidth );

        for ( int j = 0; j < width; j++ ) {
            kernel[j] = generator.evalYData( (double) j );
        }

        return kernel;
    }

    /**
     * Create Hanning kernel.
     */
    public static double[] hanningKernel( int width )
    {
        return hanHamKernel( width, 0.5, 0.5 );
    }

    /**
     * Create Hamming kernel.
     */
    public static double[] hammingKernel( int width )
    {
        return hanHamKernel( width, 0.54, 0.46 );
    }
    
    /**
     * Hamming or Hanning-like filter with given coefficients.
     */
    public static double[] hanHamKernel( int width, double a, double b )
    {
        width = getSetWidth( width );
        double[] kernel = new double[width];
        
        int centre = width / 2;

        //  Create the first positions up the centre. Keep sum as we need to
        //  normalize the result to 1.
        double sum = 0.0;
        for ( int j = 0; j <= centre; j++ ) {
            kernel[j] = a - b * Math.cos( Math.PI * 2.0 * (double) j /
                                          (double) width );
            sum += kernel[j];
        }

        //  Real sum calculated from symmetry.
        sum = sum * 2.0 - kernel[centre];

        //  Normalise first half.
        for ( int j = 0; j <= centre; j++ ) {
            kernel[j] /= sum;
        }

        //  Copy first half into symmetric positions.
        for ( int j = centre + 1, k = centre - 1; j < width; j++, k-- ) {
            kernel[j] = kernel[k];
        }
        return kernel;
    }

    /**
     * Create a Welch kernel.
     */
    public static double[] welchKernel( int width )
    {
        width = getSetWidth( width );
        double[] kernel = new double[width];
        
        int centre = width / 2;

        //  Create the first positions up the centre. Keep sum as we need to
        //  normalize the result to 1.
        double sum = 0.0;
        double part = 0.0;
        for ( int j = 0; j <= centre; j++ ) {
            part = ( j - centre ) / centre;
            kernel[j] = 1.0 - ( part * part );
            sum += kernel[j];
        }

        //  Real sum calculated from symmetry.
        sum = sum * 2.0 - kernel[centre];

        //  Normalise first half.
        for ( int j = 0; j <= centre; j++ ) {
            kernel[j] /= sum;
        }

        //  Copy first half into symmetric positions.
        for ( int j = centre + 1, k = centre - 1; j < width; j++, k-- ) {
            kernel[j] = kernel[k];
        }
        return kernel;
    }

    /**
     * Create a Barlett kernel.
     */
    public static double[] bartlettKernel( int width )
    {
        width = getSetWidth( width );
        double[] kernel = new double[width];
        
        int centre = width / 2;

        //  Create the first positions up the centre. Keep sum as we need to
        //  normalize the result to 1.
        double sum = 0.0;
        for ( int j = 0; j <= centre; j++ ) {
            kernel[j] = 2.0 * (double) j / (double) width;
            sum += kernel[j];
        }

        //  Real sum calculated from symmetry.
        sum = sum * 2.0 - kernel[centre];

        //  Normalise first half.
        for ( int j = 0; j <= centre; j++ ) {
            kernel[j] /= sum;
        }

        //  Copy first half into symmetric positions.
        for ( int j = centre + 1, k = centre - 1; j < width; j++, k-- ) {
            kernel[j] = kernel[k];
        }
        return kernel;
    }
}
