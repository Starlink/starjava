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
    /** Formatter for double precision as Kelvins */
    private static DecimalFormat dtok  = new DecimalFormat( "###.####" );

    /**
     * Class of static methods, so no construction.
     */
    private JACUtilities()
    {
        //  Do nothing.
    }

    /**
     * Calculate an estimate for the TSYS (system temperature) for the whole
     * of a spectrum.
     * <p>
     * This method is sub-millimetre specific and don't expect it to work for
     * any data other than that produced on the JCMT as it uses JAC specific
     * keywords for some parameters.
     * 
     * @param specData an instance of {@link SpecData} should be from data
     *                 taken at the JAC.
     * @param result the TSYS value formatted as a string (for Kelvins).
     */
    public static String calculateTSYS( SpecData specData )
    {
        String result = "";
        double[] errors = specData.getYDataErrors();
        if ( errors == null ) {
            return result;
        }

        //  Attempt to gather the necessary meta-data for calculating a TSYS.
        double factors[] = gatherTSYSFactors( specData );
        if ( factors == null ) {
            return result;
        }
        double tsys = calculateTSYS( errors, factors );
        if ( tsys != -1.0 ) {
            result = formatTSYS( tsys );
        }
        return result;
    }

    /**
     * Calculate an estimate for the TSYS (system temperature) given 
     * an array of errors from a spectrum and the necessary meta-data
     * from {@link gatherTSYSFactors}.
     * <p>
     * 
     * @param errors the errors extracted from a spectrum.
     * @param factors the various factors from a call to 
     *                {@link gatherTSYSFactors}
     * @param result the TSYS value, -1 if not determined.
     */
    public static double calculateTSYS( double[] errors, double[] factors )
    {
        double tsys = -1.0;

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
            //  The one standard deviation.
            double sigma = Math.sqrt ( sum / (double) count );
            tsys = calculateTSYS( factors[0], factors[1], factors[2], sigma );
        }
        return tsys;
    }


    /**
     * Format a TSYS value for display.
     *
     * @param tsys the TSYS value.
     * @return the TSYS value formatted as a string for Kelvins.
     */
    public static String formatTSYS( double tsys )
    {
        return dtok.format( tsys );
    }

    /**
     * Gather the necessary meta-data required for a JAC TSYS calculation.
     * If all found returns an array containing:
     * <ul>
     * <li> K the backend degradation factor </li>
     * <li> the channel spacing in Hz </li>
     * <li> teff the effective exposure time in seconds </li>
     * </ul>
     * Other a null is returned.
     *
     * @param specData the SpecData instance.
     * @return array of three doubles, otherwise a null.
     */
    public static double[] gatherTSYSFactors( SpecData specData )
    {
        double result[] = null;

        //  Need the channel spacing and the efficiency factor.
        String prop = specData.getProperty( "BEDEGFAC" );
        if ( ! "".equals( prop ) ) {
            double K = Double.parseDouble( prop );

            //  Work out the channel spacing from the coordinates. These are
            //  in the axis units, but we need this in Hz.
            double dnu = specData.channelSpacing( "System=FREQ,Unit=Hz" );

            //  Effective exposure time (x4). This value is from GAIA or
            //  we look for the median value (written by SMURF MAKECUBE).
            prop = specData.getProperty( "EXEFFT" );
            if ( "".equals( prop ) ) {
                prop = specData.getProperty( "EFF_TIME" );
            }
            if ( ! "".equals( prop ) ) {
                double teff = 0.25 * Double.parseDouble( prop );

                // All determined.
                result = new double[3];
                result[0] = K;
                result[1] = dnu;
                result[2] = teff;
            }
        }
        return result;
    }

    /**
     * Calculate an estimate for the TSYS (system temperature) for the given
     * data. The effective exposure time is given by:
     * <pre>
     *      teff = ton * toff / ( ton + toff )
     * </pre>
     * Where ton and toff are the times spent on and off the source for this
     * measurement.
     *
     * @param K the backend degradation factor
     * @param dnu the channel spacing in Hz
     * @param teff the effective exposure time in seconds.
     * @param sigma the data standard deviation
     */
    public static double calculateTSYS( double K, double teff, double dnu,
                                        double sigma )
    {
        //  Tim Jenness says (current ACSIS docs, circa December 2006):
        return ( sigma * Math.sqrt( dnu * teff ) / K );
    }
}
