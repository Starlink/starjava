//=== File Prolog===========================================================
//    This code was developed by NASA, Goddard Space Flight Center, Code 588
//    for the Scientist's Expert Assistant (SEA) project for Next Generation
//    Space Telescope (NGST).
//
//--- Development History---------------------------------------------------
//    Date              Author          Reference
//    11/17/97          S Grosvenor
//          Initial packaging of class into science package
//    5/23/98
//          Added support for converting between flux and magnitude,
//          brought back the hashtable of standardwavelengths
//
//    7/20/99           S Grosvenor
//          Moved Redshift units conversion to new class Redshift
//
//    5/25/00           S. Grosvenor
//          Moved/generalized for jsky.science
//
//--- DISCLAIMER---------------------------------------------------------------
//
//	This software is provided "as is" without any warranty of any kind, either
//	express, implied, or statutory, including, but not limited to, any
//	warranty that the software will conform to specification, any implied
//	warranties of merchantability, fitness for a particular purpose, and
//	freedom from infringement, and any warranty that the documentation will
//	conform to the program, or any warranty that the software will be error
//	free.
//
//	In no event shall NASA be liable for any damages, including, but not
//	limited to direct, indirect, special or consequential damages, arising out
//	of, resulting from, or in any way connected with this software, whether or
//	not based upon warranty, contract, tort or otherwise, whether or not
//	injury was sustained by persons or property or otherwise, and whether or
//	not loss was sustained from or arose out of the results of, or use of,
//	their software or services provided hereunder.
//=== End File Prolog=======================================================


package jsky.science;

import java.util.*;
import java.text.*;
import java.lang.*;

/**
 * A static class that contains astronomical
 * math utilies or constants that are not specfically related to a
 * ScienceObject sub-class.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 *    for the Scientist's Expert Assistant (SEA) project for Next Generation
 *    Space Telescope (NGST).
 *
 * @version	5.25.00
 * @author	Sandy Grosvenor
 *
 **/
public class MathUtilities {

    /**
     * log of 10, so we don't have to keep recalculating it
     */
    private static final double logOf10 = Math.log(10.0);

    /**
     * Returns "inverse" of log10
     * @param inVal the power to raise 10 to
     */
    public static double antilog10(double inVal) {
        return Math.pow(10.0, inVal);
    }

    /**
     * Returns a log base 10.  Formula is Math.log( inVal) / Math.log(10);
     */
    public static double log10(double inVal) {
        return Math.log(inVal) / logOf10;
    }


    /**
     * standard Vega temperature (9900.0)
     */
    private static final double vegaTemp = 9900.0;

    /**
     * standard Vega magnitude (0.5)
     */
    private static final double vegaMag = 0.5;

    /**
     * standard Vega flux 5500 e7
     */
    private static final double vegaFlux = 5500 * 1.0e7;

    /**
     * Converts input flux into apparent magnitude.
     * Flux should be in units: ergs s-1 cm-2
     **/
    public static double convertFluxToMagnitude(double inFlux) {
        return convertFluxToMagnitude(inFlux, vegaMag, vegaFlux);
    }

    /**
     * Converts input flux into apparent magnitude.
     * Flux should be in units: ergs s-1 cm-2
     **/
    public static double convertFluxToMagnitude(double inFlux, double baseMag, double baseFlux) {
        return baseMag - 2.5 * log10(inFlux / baseFlux);
    }

    /**
     * Converts input flux into apparent magnitude.
     * Flux will be in units: ergs s-1 cm-2
     **/
    public static double convertMagnitudeToFlux(double inMag) {
        return convertMagnitudeToFlux(inMag, vegaMag, vegaFlux);
    }

    public static double convertMagnitudeToFlux(double inMag, double baseMag, double baseFlux) {
        return baseFlux * antilog10((baseMag - inMag) / 2.5);
    }

}

