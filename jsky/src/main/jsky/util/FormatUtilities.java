//=== File Prolog===========================================================
//    This code was developed by NASA, Goddard Space Flight Center, Code 588
//    for the Scientist's Expert Assistant (SEA) project for Next Generation
//    Space Telescope (NGST).
//
//--- Development History---------------------------------------------------
//    Date              Author          Reference
//    11/17/97          S Grosvenor
//          Initial packaging of class into science package
//    5/25/00           S. Grosvenor
//          Moved/generalized for jsky.science - only formating stuff in here
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


package jsky.util;

import jsky.science.MathUtilities;

/**
 * This is an all-static class that contains methods
 * are generic in value and related to format objects.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 *    for the Scientist's Expert Assistant (SEA) project for Next Generation
 *    Space Telescope (NGST).
 *
 * @version	5.25.00
 * @author	Sandy Grosvenor
 *
 **/
public class FormatUtilities {

    /**
     * Display a simple error message to System.err
     */
    public static void writeError(Object source, Object message) {
        StringBuffer sb = new StringBuffer("[ERROR] ");
        sb.append(source);
        sb.append(": ");
        sb.append(message);
        System.err.println(sb.toString());
    }

    /**
     * Formats a double value to specified number of decimal places, including
     * using scientific notation if appropriate.
     * Uses default value of 14 places to left of decimal.
     * @param inVal double number to be formatted
     * @param inLeftOfDec integer of max number of places to left of decimal
     */
    public static String formatDouble(double inVal, int inDecs) {
        return formatDouble(inVal, inDecs, 14, false);
    }

    /**
     * Formats a double value to specified number of decimal places, including
     * using scientific notation if appropriate.
     * @param inVal double number to be formatted
     * @param inDecs integer of number of decimal places
     * @param inLeftOfDec integer of max number of places to left of decimal
     */
    public static String formatDouble(double inVal, int inDecs, int inLeftOfDec) {
        return formatDouble(inVal, inDecs, inLeftOfDec, false);
    }

    /**
     * Standard string representation for positive infinity
     */
    private static String INFINITY = Double.toString(Double.POSITIVE_INFINITY);

    /**
     * Standard string represetnation for zero.
     */
    private static String ZERO = "0";

    /**
     * Standard string value for NotANumber.
     */
    private static String NAN = Double.toString(Double.NaN);

    /**
     * Formats a double value to specified number of decimal places
     * handles need (or not) for scientific notation
     * @param inVal double number to be formatted
     * @param inDecs integer of number of decimal places
     * @param inLeftOfDec integer of max number of places to left of decimal
     * @param recurring true when formatDouble is being called recursively from itself
     */
    private static String formatDouble(double inVal, int inDecs, int inLeftOfDec, boolean recursing) {
        String returnVal = "";

        if (Double.isInfinite(inVal)) {
            returnVal = INFINITY;
        }
        else if (Double.isNaN(inVal)) {
            returnVal = NAN;
        }
        else if (inVal == 0) {
            StringBuffer sb = new StringBuffer("0");
            if (inDecs > 0) sb.append(".");
            for (int i = 0; i < inDecs; i++) sb.append("0");
            returnVal = sb.toString();
        }
        else {
            // dont let digits to left of decimal be less than 1
            inLeftOfDec = Math.max(inLeftOfDec, 1);

            int maxExp = inLeftOfDec - inDecs;  // max 10 power before going to sci notat
            if (inDecs == 0) maxExp++;
            if (inVal < 0) maxExp--;

            int minExp = -inDecs;           // min 10 power before going to sci notat

            boolean doSN = (Math.abs(inVal) > pow10(maxExp)
                    ? true
                    : (Math.abs(inVal) < pow10(minExp)));

            if (!doSN) {
                String ret = null;
                // not doing scientific notation
                if (inDecs == 0) {
                    ret = Long.toString(Math.round(inVal));
                }
                else {
                    double p10 = pow10(inDecs);
                    ret = Double.toString(Math.round(inVal * p10) / p10);
                    int dotLoc = ret.indexOf(".");
                    if (inDecs > 0 && dotLoc > 0) {
                        ret = ret.substring(0, Math.min(ret.length(), dotLoc + inDecs + 1));
                    }
                }
                return ret;
            }
            else {
                // dont let digits to left of decimal be less than 1
                inLeftOfDec = Math.max(inLeftOfDec, 1);

                // is scientific notation, still use recommended decs
                int sign = 1;
                if (inVal < 0) {
                    sign = -1;
                    inVal = -inVal;
                }

                int p10 = 0;
                if (inVal != 0) p10 = (int) Math.floor(MathUtilities.log10(inVal));

                double adjVal = sign * inVal / pow10(p10);

                if (Double.isNaN(adjVal)) {
                    returnVal = NAN;
                }
                else {
                    // to avoid an endless loop
                    StringBuffer sb = new StringBuffer(
                            ((recursing) ? Double.toString(inVal) : formatDouble(adjVal, inDecs, inLeftOfDec, true)));
                    sb.append("e");
                    sb.append(p10);
                    returnVal = sb.toString();
                }
            }
        }
        return returnVal;
    }

    /**
     * Returns value of 10 raised to the n-th power.  Used by formatDouble and
     * implemented to be easy on resources
     */
    private static double pow10(int n) {
        if (n == 0)
            return 1;
        else if (n > 0) {
            double d = 1;
            for (int i = 0; i < n; i++) d = d * 10.;
            return d;
        }
        else {
            double d = 1;
            for (int i = 0; i > n; i--) d = d / 10.;
            return d;
        }
    }
}

