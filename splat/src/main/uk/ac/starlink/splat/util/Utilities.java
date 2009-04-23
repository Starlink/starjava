/*
 * Copyright (C) 2001 Central Laboratory of the Research Councils
 *
 *  History:
 *     27-JUL-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.io.File;
import javax.swing.JComponent;
import javax.swing.JViewport;
import java.util.prefs.Preferences;

/**
 * Class of static members that provide utility functions. The major
 * set of these are to do with the name of this program and the
 * location of any configuration files.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see Sort
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
        return Version.getVersion();
    }

    /**
     * The full description of this program when released!
     */
    public static String getFullDescription(){
        return getReleaseName() + ": A Spectral Analysis Tool";
    }

    /**
     * The name of this program when released!
     */
    public static String getReleaseName()
    {
        return "Starlink SPLAT-VO";
    }

    /**
     * The name of this application (lower case for stores etc.).
     */
    public static String getApplicationName()
    {
        return "splat";
    }

    /**
     * The name of this program, plus some other description,
     * formatted as suitable for displaying in window titles
     * (i.e. "PROGRAM: <rest of title>").
     */
    public static String getTitle( String postfix )
    {
        return getReleaseName() + ": " + postfix;
    }

    /**
     * Get the copyright information for the program.
     */
    public static String getCopyright()
    {
        return
            "Copyright (C) 2001-2005 Central Laboratory of the Research Councils<br>"+
            "Copyright (C) 2006-2008 Particle Physics and Astronomy Research Council<br>"+
            "Copyright (C) 2008-2009 Science and Technology Facilities Council<br>"+
            "Supported by the Joint Astronomy Centre Hawaii";
    }

    /**
     * Get the authors of the program.
     */
    public static String getAuthors()
    {
        return "Peter W. Draper &amp; Mark B. Taylor";
    }

    /**
     * Get a short description of the licensing terms.
     */
    public static String getLicense()
    {
        return "SPLAT is free software under the terms of the "+
            "GNU General Public License.";
    }

    /**
     * Get the SPLAT support page URL.
     */
    public static String getSupportURL()
    {
        return "http://www.starlink.ac.uk/splat";
    }

    /**
     * Get the SPLAT support email address.
     */
    public static String getSupportEmail()
    {
        return "starlink@jiscmail.ac.uk";
    }

    /**
     * Get the platform.
     */
    public static String getPlatform()
    {
        return
            System.getProperty( "java.version" ) + " : " +
            System.getProperty( "os.name" ) + " - " +
            System.getProperty( "os.arch" ) + " - " +
            System.getProperty( "os.version" );
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
// Files.
//
    /**
     * Return the extension of a file's name.
     *
     * @param f the File.
     */
    public static String getExtension( File f )
    {
        if ( f != null ) {
            return getExtension( f.getName() );
        }
        return null;
    }

    /**
     * Return the extension of a file's name.
     */
    public static String getExtension( String name )
    {
        if ( name != null ) {
            int i = name.lastIndexOf( '.' );
            if ( i > 0 && i < name.length() - 1 ) {
                return name.substring( i + 1 ).toLowerCase();
            }
        }
        return null;
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
// UI utilities.
//

    /**
     * Raise an existing {@link Frame} so that it is visible. This
     * utility is used so that we can adapt to whatever works for the
     * current JDK (setVisible should be enough, but rarely seems to
     * be).
     */
    public static void raiseFrame( Frame window )
    {
        window.setState( Frame.NORMAL );
        window.setVisible( true );
    }

    /**
     * Set the location of a {@link Frame} using the given default
     * position or by restoring from package Preferences object.
     *
     * The Preference names are "name_x", "name_y", "name_width"
     * and "name_height", where name is some symbolic String (like
     * MainWindow, PlotWindow, etc.). These names are generated by the
     * symmetric method {@link #saveFrameLocation}.
     *
     * If the defaults value is null and no values exist already then
     * the frame is located and sized using pack. If the default position
     * is set to 0,0 then that is ignored (allowing just the initial
     * size to be set).
     */
    public static void setFrameLocation( Frame window, Rectangle defaults,
                                         Preferences prefs, String name )
    {
        if ( defaults == null ) {

            // Just check one value for presence, need them all or none.
            String x = prefs.get( name + "_x", null );
            if ( x == null ) {
                window.pack();
                return;
            }

            //  Defaults do not matter.
            defaults = new Rectangle( 0, 0, 1, 1 );
        }
        int x = prefs.getInt( name + "_x", defaults.x );
        int y = prefs.getInt( name + "_y", defaults.y );
        int width = prefs.getInt( name + "_width", defaults.width );
        int height = prefs.getInt( name + "_height", defaults.height );
        if ( x > 0 && y > 0 ) {
            window.setLocation( x, y );
        }
        window.setSize( width, height );
    }

    /**
     * Save the location of a {@link Frame} to a package Preferences
     * object.
     *
     * The Preference names are "name_x", "name_y", "name_width"
     * and "name_height", where name is some symbolic String (like
     * MainWindow, PlotWindow, etc.). These names are used when
     * restoring by the symmetric method {@link #setFrameLocation}.
     */
    public static void saveFrameLocation( Frame window, Preferences prefs,
                                          String name )
    {
        int x = window.getLocation().x;
        int y = window.getLocation().y;
        int width = window.getSize().width;
        int height = window.getSize().height;
        prefs.putInt( name + "_x", x );
        prefs.putInt( name + "_y", y );
        prefs.putInt( name + "_width", width );
        prefs.putInt( name + "_height", height );
    }

    /**
     * Set the size of a {@link Frame} using the given default
     * sizes or by restoring from package Preferences object.
     *
     * The Preference names are "name_width" and "name_height", where name is
     * some symbolic String (like mainWindow, plotWindow, etc.). These names
     * are generated by the symmetric method {@link #saveFrameSize}.
     *
     * If the default width and height values are 0 and no values exist
     * already then the frame is sized using pack.
     */
    public static void setFrameSize( Frame window, int defWidth, int defHeight,
                                     Preferences prefs, String name )
    {
        if ( defWidth == 0 || defHeight == 0  ) {

            // Just check one value for presence, but we really need both.
            String width = prefs.get( name + "_width", null );
            if ( width == null ) {
                window.pack();
                return;
            }
        }
        int width = prefs.getInt( name + "_width", defWidth );
        int height = prefs.getInt( name + "_height", defHeight );
        window.setSize( width, height );
    }

    /**
     * Save the size of a {@link Frame} to a package Preferences object.
     *
     * The Preference names are "name_width" and "name_height", where name is
     * some symbolic String (like MainWindow, PlotWindow, etc.). These names
     * are used when restoring by the symmetric method
     * {@link #setFrameLocation}.
     */
    public static void saveFrameSize( Frame window, Preferences prefs,
                                      String name )
    {
        int width = window.getSize().width;
        int height = window.getSize().height;
        prefs.putInt( name + "_width", width );
        prefs.putInt( name + "_height", height );
    }

    /**
     * Set the preferred size of a {@link JComponent} by restoring from
     * package Preferences object.
     *
     * The Preference names are "name_width" and "name_height", where name is
     * some symbolic String (like PlotComponent, etc.). These names
     * are generated by the symmetric method {@link #saveComponentSize}.
     *
     * If the default width and height values are 0 and no values exist
     * already then the size is not set.
     */
    public static void setComponentSize( JComponent comp, int defWidth,
                                         int defHeight, Preferences prefs,
                                         String name )
    {
        if ( defWidth == 0 || defHeight == 0  ) {

            // Just check one value for presence, but we really need both.
            String width = prefs.get( name + "_width", null );
            if ( width == null ) {
                return;
            }
        }
        int width = prefs.getInt( name + "_width", defWidth );
        int height = prefs.getInt( name + "_height", defHeight );
        comp.setPreferredSize( new Dimension( width, height ) );
    }

    /**
     * Save the size of a {@link JComponent} to a package Preferences object.
     * If the component is an instance of {@link JViewport} then the
     * visible size is stored.
     *
     * The Preference names are "name_width" and "name_height", where name is
     * some symbolic String (like MainComponent etc.). These names
     * are used when restoring by the symmetric method
     * {@link #setFrameLocation}.
     */
    public static void saveComponentSize( JComponent comp, Preferences prefs,
                                          String name )
    {
        Dimension dims = null;
        if ( comp instanceof JViewport ) {
            dims = ((JViewport)comp).getExtentSize();
        }
        else {
            dims = comp.getSize();
        }
        prefs.putInt( name + "_width", dims.width );
        prefs.putInt( name + "_height", dims.height );
    }

//
// General utilities
//
    /**
     * Join two String arrays.
     */
    public static String[] joinStringArrays( String[] one, String[] two )
    {
        String[] onetwo = new String[one.length + two.length];
        int j = 0;
        for ( int i = 0; i < one.length; i++ ) {
            onetwo[j++] = one[i];
        }
        for ( int i = 0; i < two.length; i++ ) {
            onetwo[j++] = two[i];
        }
        return onetwo;
    }
}
