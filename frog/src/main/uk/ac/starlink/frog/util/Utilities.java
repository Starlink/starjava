package uk.ac.starlink.frog.util;

import java.awt.Color;
import java.awt.Frame;
import java.io.File;
import java.lang.System;
import java.util.Properties;


/**
 * Class of static members that provide utility functions. The major
 * set of these are to do with the name of this program and the
 * location of any configuration files. This code was blatently stolen
 * from Peter Draper's SPLAT application and modified for FROG.
 *
 * @since $Date$
 * @author Peter Draper & Alasdair Allan
 * @version $Id$
 */
public class Utilities 
{

    /**
     * System properties
     */
     static Properties javaProp = System.getProperties();

    /**
     * Class of static methods, so no construction.
     */
    private Utilities()
    {
        //  Do nothing.
    }

//
// System properties
//
 
   /**
    * Get Java SDK version
    */
    public static String getJavaVersion() 
    {
        return javaProp.getProperty( "java.version" );

    }
 
   /**
    * Get operating system name, arctitecture and revision.
    */
    public static String getOperatingSystem() 
    {
        String os = javaProp.getProperty( "os.name" ) + " " +
                    javaProp.getProperty( "os.arch" ) + " " +
                    javaProp.getProperty( "os.version" );
        return os;
    }    
//
// Program names and version.
//
    /**
     * The version of this program when released.
     */
    public static String getReleaseVersion()
    {
        return "0.7.0 + Web Services";
    }

    /**
     * The full description of this program when released!
     */
    public static String getFullDescription(){
        return getReleaseName() + ": A Time Series Analysis Package";
    }

    /**
     * The name of this program when released!
     */
    public static String getReleaseName()
    {
        return "FROG";
    }

    /**
     * The name of this application (lower case for stores etc.).
     */
    public static String getApplicationName()
    {
        return "frog";
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
        File dir = new File( System.getProperty( "user.home" ), ".frog" );
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
