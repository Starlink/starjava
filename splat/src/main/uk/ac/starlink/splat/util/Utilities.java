package uk.ac.starlink.splat.util;

import java.awt.Color;
import java.awt.Frame;
import java.io.File;

/**
 * Class of static members that provide utility functions. The major
 * set of these are to do with the name of this program and the
 * location of any configuration files.
 *
 * @since $Date$
 * @since 27-JUL-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 */
public class Utilities 
{
    /**
     * Class of static methods, so no construction.
     */
    private Utilities()
    {
        //  Do nothing.
    }

//
// Program names and version.
//
    /**
     * The version of this program when released.
     */
    public static String getReleaseVersion()
    {
        return "0.1";
    }

    /**
     * The full description of this program when released!
     */
    public static String getFullDescription(){
        //return "JSPEC: A Spectral Display and Analysis Tool";
        //return "ASDAT: A Spectral Display and Analysis Tool";
        //return "GAFS: Graphical Analysis For Spectroscopy Tool";
        return "SPLAT: A Spectral Analysis Tool";
    }

    /**
     * The name of this program when released!
     */
    public static String getReleaseName()
    {
        //return "JSPEC";
        //return "ASDAT";
        //return "GAFS";
        return "SPLAT";
    }

    /**
     * The name of this program, plus some other description,
     * formatted as suitable for displaying in window titles
     * (i.e. "PROGRAM: <rest of title>").
     */
    public static String getTitle( String postfix )
    {
        //return "JSPEC: " + postfix;
        //return "ASDAT: " + postfix;
        //return "GAFS: " + postfix;
        return "SPLAT: " + postfix;
    }

//
// Configuration files.
//
    /**
     * The name of the directory used for storing configuration
     * information. This directory is created if it doesn't exist
     * already.
     */
    public static File getConfigDirectory()
    {
        File dir = new File( System.getProperty( "user.home" ), ".splat" );
        if ( ! dir.exists() ) {
            try {
                dir.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
                dir = null;
            }
        } else if ( ! dir.isDirectory() ) {
            System.err.println( "Cannot create a directory: " +
                                dir.getName() + "as a file with "+
                                "this name already exists" );
            dir = null;
        }
        return dir;
    }

    /**
     * Construct the proper name of a file stored in the configuration
     * directory. 
     *
     * @param name the name of the file to be stored/located in the
     *             the configuration directory.
     */
    public static File getConfigFile( String name )
    {
        return new File( getConfigDirectory(), name );
    }


//
// Debugging routines.
//
    /**
     * Output a stack trace of the current code position.
     */
    public static void printStackTrace()
    {
        Throwable tracer = new Throwable();
        tracer.printStackTrace();
    }

//
// Memory usage routines.
//
    /**
     * Do a full GC and output memory statistics.
     */
    public static void fullGC( boolean report )
    {
        Runtime rt = Runtime.getRuntime();
        long isFree = rt.freeMemory();
        long origFree = isFree;
        long wasFree = 0;
        do {
            wasFree = isFree;
            rt.gc();
            isFree = rt.freeMemory();
        } while ( isFree > wasFree );
        rt.runFinalization();
        if ( report ) {
            System.out.println( "Freed memory = " + ( isFree - origFree ) );
            memStats();
        }
    }

    /**
     * Show curent memory statistics.
     */
    public static void memStats()
    {
        Runtime rt = Runtime.getRuntime();
        System.out.println( "Free memory = " + rt.freeMemory() );
        System.out.println( "Used memory = " + ( rt.totalMemory() -
                                                 rt.freeMemory() ) );
        System.out.println( "Total memory = " + rt.totalMemory() );
    }

//
// Spectral colouring routines.
//
    private static java.util.Random generator = null;
    /**
     *  Get a random colour from a set of a given size.
     *
     *  @param res number of expected colours in rainbow (i.e. number
     *             you want to select from).
     */
    public static Color getRandomColour( float res )
    {
        if ( generator == null ) {
            generator = new java.util.Random();
        }
        float h = generator.nextFloat() * res;
        return Color.getHSBColor( h, 1.0F, 1.0F );
    }

    /**
     *  Get a rainbow colour as an RGB integer from a set of a given
     *  size.
     *
     *  @param res number of expected colours in rainbow (i.e. number
     *             you want to select from).
     */
    public static int getRandomRGB( float res )
    {
        if ( generator == null ) {
            generator = new java.util.Random();
        }
        float h = generator.nextFloat() * res;
        return Color.HSBtoRGB( h, 1.0F, 1.0F );
    }

//
// UI utilities.
//

    /** 
     * Raise an existing Frame or JFrame so that it is visible. This
     * utility is used so that we can adapt to whatever works for the
     * current JDK (setVisible should be enough, but rarely seems to
     * be).  
     */
    public static void raiseFrame( Frame window )
    {
        window.setState( Frame.NORMAL );
        window.setVisible( true );
    }
}
