/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 *
 *  History:
 *     07-JAN-2005 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Return the version of SPLAT. Reads the value from the local properties file
 * "splat.version", which is generated as part of the build process.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
class Version
{
    private static String version = null;

    public static String getVersion()
    {
        if ( version == null ) {
            readVersion();
        }
        return version;
    }

    private static void readVersion()
    {
        InputStream propstrm = 
            Version.class.getResourceAsStream( "splat.version" );
        Properties props = new Properties();
        try {
            props.load( propstrm );
            version = props.getProperty( "version" );
        }
        catch (IOException e) {
            version = "unknown";
        }
    }
}
