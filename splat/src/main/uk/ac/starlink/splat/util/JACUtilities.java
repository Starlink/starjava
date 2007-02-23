/*
 * Copyright (C) 2007 Particle Physics and Astronomy Reseach Council
 *
 *  History:
 *     23-FEB-2007 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import java.text.DecimalFormat;
import uk.ac.starlink.splat.data.SpecData;

/**
 * Class of static members that provide utility functions related to the JAC
 * and it's telescopes.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see Sort
 */
public class JACUtilities
{
    /**
     * Class of static methods, so no construction.
     */
    private JACUtilities()
    {
        //  Do nothing.
    }

    /**
     * Calculate an estimate for the TSYS (system temperature).
     * This method is sub-millimetre specific, don't expect it to work for
     * any data other tham that produced on the JCMT as it uses JAC specific
     * keywords for some parameters.
     * 
     * @param specData an instance of {@link SpecData} should be from data
     *                 taken at the JAC.
     */
    public static String calculateTsys( SpecData specData )
    {
        String result = "";
        double[] errors = specData.getYDataErrors();
        if ( errors == null ) {
            return result;
        }

        //  Need the channel spacing and the efficiency factor.
        String prop = specData.getProperty( "BEDEGFAC" );
        if ( "".equals( prop ) ) {
            return result;
        }
        double K = Double.parseDouble( prop );

        //  Work out the channel spacing from the coordinates. These are in
        //  the axis units, but we need this in Hz.
        double dnu = specData.channelSpacing( "System=FREQ,Unit=Hz" );

        //  Effective exposure time (x4).
        prop = specData.getProperty( "EXEFFT" );
        if ( "".equals( prop ) ) {
            return result;
        }
        double exposure = 0.25 * Double.parseDouble( prop );

        //  Variance, need one value for whole spectrum.
        int n = errors.length;
        double sum = 0.0;
        int count = 0;
        for ( int i = 0; i < n; i++ ) {
            if ( errors[i] != SpecData.BAD ) {
                sum += ( errors[i] * errors[i] );
                count++;
            }
        }
        if ( count > 0 ) {
            double sigma = Math.sqrt ( sum / (double) count );

            //  I make this from Jamie's original paper:
            // double tsys =
            //    0.5 * sigma * Math.sqrt( 2.0 * dnu * exposure / K );

            //  SpecX says:
            // double tsys = 0.5 * sigma * Math.sqrt( dnu * exposure );
            //

            //  Tim says (current ACSIS docs, circa December 2006):
            double tsys = sigma * Math.sqrt( dnu * exposure ) / K;
            DecimalFormat f  = new DecimalFormat( "###.####" );
            result = f.format( tsys );
        }
        return result;
    }
}
