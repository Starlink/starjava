/*
 * Copyright (C) 2001-2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     27-JUL-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.ast.gui;

import java.io.File;

/**
 * Class of static members that provide utility functions. 
 * Just provides standard methods for locating configuration files.
 *
 * @author Peter W. Draper
 * @version $Id$
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
     *
     * @param applicationName name of the application, used to
     *                        generate the top-directory name
     */
    public static File getConfigDirectory( String applicationName )
    {
        File dir = new File( System.getProperty( "user.home" ), 
                             "." + applicationName );
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
     * @param applicationName name of the application (same as for
     *                        {@link getConfigDirectory}) 
     * @param name the name of the file to be stored/located in the
     *             the configuration directory.
     */
    public static File getConfigFile( String applicationName, String name )
    {
        return new File( getConfigDirectory( applicationName ), name );
    }
}
