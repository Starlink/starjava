package uk.ac.starlink.jpcs;

import java.io.File;
import java.lang.System;
import java.util.Properties;


/**
 * Class of static members that provide utility functions. This code is
 * a cut down version of the code inherited by FROG from SPLAT. The original
 * version can be found there. I've put a copy here so that we can access
 * configuration files easily from the JPCS package. The parameter system
 * should write its *.par files to a standard directory in the users home
 * space.
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

    /**
     * The name of the directory used for storing configuration
     * information. This directory is created if it doesn't exist
     * already.
     */
    public static File getConfigDirectory()
    {
        File dir = null;
        if ( javaProp.getProperty("adam.user") != null ) {
           dir = new File ( javaProp.getProperty( "adam.user" ) );
        } else {
           dir = new File( javaProp.getProperty( "user.home" ), ".soap" );
        }
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

}
