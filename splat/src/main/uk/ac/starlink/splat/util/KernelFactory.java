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
     * Create gaussian kernel.
     */
    public static double[] gaussianKernel( int width, double fwhm )
    {
        //  Width is always odd number.
        width = ( width / 2 ) * 2 + 1;
        if ( width == 1 ) width= 3;

        double[] kernel = new double[width];

        // Center of kernel.
        int center = width / 2;

        //  Convert fwhm to sigma (/2*sqrt(2*ln(2)).
        double sigma = fwhm / 2.35482;

        // Generator for positions.
        GaussianGenerator generator =
            new GaussianGenerator( 1.0, center, sigma );

        for ( int j = 0; j < width; j++ ) {
            kernel[j] = generator.evalPoint( (double) j );
        }

        return kernel;
    }

    /**
     * Create lorentzian kernel.
     */
    public static double[] lorentzKernel( int width, double lwidth )
    {
        //  Width is always odd number.
        width = ( width / 2 ) * 2 + 1;
        if ( width == 1 ) width= 3;

        double[] kernel = new double[width];

        // Center of kernel.
        int center = width / 2;

        // Generator for positions.
        LorentzGenerator generator =
            new LorentzGenerator( 1.0, center, lwidth );

        for ( int j = 0; j < width; j++ ) {
            kernel[j] = generator.evalPoint( (double) j );
        }

        return kernel;
    }

    /**
     * Create voigt kernel.
     */
    public static double[] voigtKernel( int width, double gwidth, 
                                        double lwidth )
    {
        //  Width is always odd number.
        width = ( width / 2 ) * 2 + 1;
        if ( width == 1 ) width= 3;

        double[] kernel = new double[width];

        // Center of kernel.
        int center = width / 2;

        // Generator for positions.
        VoigtGenerator generator =
            new VoigtGenerator( 1.0, center, gwidth, lwidth );

        for ( int j = 0; j < width; j++ ) {
            kernel[j] = generator.evalPoint( (double) j );
        }

        return kernel;
    }
}
