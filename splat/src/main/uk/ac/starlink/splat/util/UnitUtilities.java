/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     24-11-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import java.util.StringTokenizer;

/**
 * Utility class for various issues related to working with units as used in
 * spectral data.
 * <p>
 * Consists of two static methods that uses heuristics to fix up problems
 * that have been encountered with units strings that do not match the FITS
 * paper I standard (as used by AST).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class UnitUtilities
{
    /**
     * Constructor, private so it cannot be used.
     */
    private UnitUtilities()
    {
        //  Do nothing.
    }

    /**
     * Attempt to repair a string that is supposed to describe the units
     * of a spectrum. These can be data or coordinate units. XXX may change
     * that stance, coords should be simpler.
     *
     * @param dodgyUnits the units string to repair
     * @return the fixed string, unchanged if nothing seemed to need fixing
     */
    public static String fixUpUnits( String dodgyUnits )
    {
        if ( dodgyUnits == null ) return null;

        // Need to parse into words and check each of the components. Word
        // delimeters are: " ", "*", ".", and "/". The "*" may not be a
        // delimeter when repeated, happily we can replace this sequence with
        // "^" (raised to the power). Also some mathematical function can be
        // used log(), mostly so we use ( and ) as delimeters.
        String result = dodgyUnits.replaceAll( "\\*\\*", "\\^" );
        StringTokenizer st = new StringTokenizer( result, " *./()", true );
        String word;
        StringBuffer cleaned = new StringBuffer();
        while ( st.hasMoreTokens() ) {
            word = st.nextToken();
            word = fixUp( word );
            cleaned.append( word );
        }
        result = cleaned.toString();
        
        //  Fixup completely wrong strings... FUSE server misses /s completely.
        if ( "erg/cm^2/Angstrom".equals( result ) ) {
            result = "erg/cm^2/s/Angstrom";
        }

        return result;
    }

    /**
     * Do the fix up of a single unit.
     */
    private static String fixUp( String unit )
    {
        if ( unit.equals( "*" ) ||
             unit.equals( " " ) || 
             unit.equals( "." ) || 
             unit.equals( "(" ) ||
             unit.equals( "/" ) ||
             unit.equals( ")" ) ) {
            //  Delimeter.
            return unit;
        }
        
        //  Do test in lowercase. AST is case sensitive (which is were a lot
        //  of these problems start).
        String lunit = unit.toLowerCase();

        //  All forms of angstrom(s). People do like that trailing s.
        if ( lunit.startsWith( "ang" ) ) {
            return "Angstrom";
        }

        //  Single A. Taken as angstroms, but could be Ampere. Unlikely for
        //  spectra.
        if ( lunit.equals( "a" ) ) {
            return "Angstrom";
        }

        //  Missing powers or case problems (CM^2).
        if ( lunit.equals( "cm2" ) || lunit.equals( "cm^2" ) ) {
            return "cm^2";
        }
        if ( lunit.equals( "m2" )  || lunit.equals( "m^2" ) ) {
            return "m^2";
        }

        //  Hz.
        if ( lunit.equals( "hz" ) ) {
            return "Hz";
        }

        //  kHz.
        if ( lunit.equals( "khz" ) ) {
            return "kHz";
        }

        //  GHz.
        if ( lunit.equals( "ghz" ) ) {
            return "GHz";
        }

        //  Jy.
        if ( lunit.equals( "jy" ) || lunit.startsWith( "jans" ) ) {
            return "Jy";
        }

        //  eV.
        if ( lunit.equals( "ev" ) ) {
            return "eV";
        }

        //  keV.
        if ( lunit.equals( "kev" ) ) {
            return "keV";
        }
             
        //  MeV.
        if ( lunit.equals( "mev" ) ) {
            return "MeV";
        }
             
        //  erg.
        if ( lunit.equals( "erg" ) ) {
            return "erg";
        }
             
        //  Seconds.
        if ( lunit.equals( "s" ) || lunit.startsWith( "sec" ) ) {
            return "s";
        }

        //  Micron(s).
        if ( lunit.equals( "um" ) || lunit.startsWith( "micron" ) ) {
            return "um";
        }
             
        //  Watt(s).
        if ( lunit.equals( "w" ) || lunit.startsWith( "watt" ) ) {
            return "W";
        }

        //  Joules
        if ( lunit.equals( "j" ) || lunit.startsWith( "joul" ) ) {
            return "J";
        }
             
        //  Magnitude.
        if ( lunit.startsWith( "mag" ) ) {
            return "mag";
        }
             
        //  Metre/meter/M.
        if ( lunit.equals( "m" ) || lunit.startsWith( "met" ) ) {
            return "m";
        }

        // Centimetre/CM.
        if ( lunit.equals( "cm" ) ) {
            return "cm";
        }

        //  Kilometre/meter/KM.
        if ( lunit.equals( "km" ) || lunit.startsWith( "kilomet" ) ) {
            return "km";
        }
             
        //  Degrees.
        if ( lunit.startsWith( "deg" ) ) {
            return "deg";
        }
             
        //  Counts.
        if ( lunit.equals( "ct" ) || lunit.startsWith( "count" ) ) {
            return "count";
        }
             
        //  Pixels.
        if ( lunit.startsWith( "pix" ) ) {
            return "pixel";
        }

        //  Steradians
        if ( lunit.equals( "sr" ) || lunit.startsWith( "ster" ) ) {
            return "sr";
        }
             
        //  Nothing to do.
        return unit;
    }

    //  test of things I've seen!
    public static void main( String[] args )
    {
        String[] testUnits = {
            "Angstroms",
            "Janskys",
            "counts", 
            "GHZ",
            "KHZ",
            "hz",
            "kev",
            "KEV",
            "1/m",
            "log(hz)",
            "log(ghz)",
            "pixels",
            "log10(Jy/sr)",
            "log10(Janskies/steradian)",
            "log10(10**-3*micron)",
            "0.01 micron",
            "ERG/CM2/S/A",
            "ERG/CM**2/S/A",
            "W/CM2/UM",
            "WATT/M2",
            "watts/cm^2/micron",
            "ANGSTROMS", 
            "10-18W/m2/nm",
            "erg /s /cm2 /angstroms",
            "0.1nm",
            "COUNTS",
            "1.0E-17 erg/cm^2/s/Ang",
            "erg/cm**2/A"
        };

        System.out.println( "Checking units fixups" );
        String fixed = null;
        for ( int i = 0; i < testUnits.length; i++ ) {
            fixed = fixUpUnits( testUnits[i] );
            System.out.println( testUnits[i] + " = " + fixed );
        }
    }
}
