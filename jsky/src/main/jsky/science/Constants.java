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
//    7/20/99           S Grosvenor
//          Moved Redshift units conversion to new class Redshift
//    5/25/00           S. Grosvenor
//          Generalized/move to jsky.science
//    11/27/2000        S. Grosvenor
//          cleaned up javadoc for Jsky submission
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

import java.util.Hashtable;

/**
 * Constants class is an all static class that contains various scientific/
 * and astronomical constants.
 *
 * <P>The comments for each constant should include units and a reference to the
 * source of the value for the constant.
 *
 * References to "AQ" refer to "Allen's Astrophysical Quantities" edited
 * by Arthur N. Cox, Fourth Edition, Published 2000, Springer-Verlag,
 * ISBN # 0-387-98746-0.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 *    for the Scientist's Expert Assistant (SEA) project for Next Generation
 *    Space Telescope (NGST).
 *
 * @version	11.27.2000
 * @author	Sandy Grosvenor
 *
 **/
public class Constants {

    /**
     * Janksy's constant (double).
     * Units: Flux(W) meters^-2 Hertz^-1.
     * Reference: AQ, p. 20.
     */
    public static final double jansky = 1.0e-26;

    /**
     * Boltzmans constant (double) (k).
     * Units: Joule Kelvin^-1.
     * Reference: AQ, p. 9.
     */
    public static final double boltzman = 1.38e-23; // k

    /**
     * @see #boltzman
     */
    public static final double k = boltzman;

    /**
     * Planck's constant (double) (h).
     * Units: joule second^-1.
     * Reference: AQ, p. 8.
     */
    public static final double planck = 6.6260755e-34; // erg s... have also seen 6.626E-27

    /**
     * @see #planck
     */
    public static final double h = planck;

    /**
     * Speed of Light (double) (c).
     * Units: meters second^-1.
     * Reference: AQ, p. 8.
     */
    public static final double c = 2.99792458e8;


    /**
     * Wavelength associated with 1 eV (electron-volt) (lambda sub 0).
     * Units: meter.
     * Source: AQ, p. 11.
     */
    public static final double lambda0 = 1.23984282e-6;

}

