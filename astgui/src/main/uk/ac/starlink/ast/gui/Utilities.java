package uk.ac.starlink.ast.gui;

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

}
