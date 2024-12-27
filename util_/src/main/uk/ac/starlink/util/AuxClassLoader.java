package uk.ac.starlink.util;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * This class provides a ClassLoader which looks on a given class path.
 * In cases where an application is invoked using the extension mechanism
 * (normally using <code>java -jar</code>) and hence gets its classpath
 * from somewhere other than the environment (e.g. $CLASSPATH), this 
 * can pick up a different set of classes than the default/system
 * ClassLoader.  This will typically be of use where code wants to
 * permit 'pluggability', that is allowing the user to make classes
 * available to the system other than those known about at the 
 * build/install time for the rest of the application/infrastructure
 * framework.
 * <p>
 * The default delegation ClassLoader is used as the parent of this one,
 * so that the system classloader(?) will be queried for any classes
 * before they are searched for on the given class.
 *
 * @author   Mark Taylor (Starlink)
 */
public class AuxClassLoader extends URLClassLoader {

    /**
     * Constructs a ClassLoader which will look on a given path.
     *
     * @param  classpath  a class path specified in the same format 
     *         as the <code>java.class.path</code> system property
     */
    public AuxClassLoader( String classpath ) {
        super( getURLs( classpath ) );
    }

    /**
     * Turns a classpath-style path into an array of URLs.
     */
    private static URL[] getURLs( String path ) {
        if ( path == null ) {
            return new URL[ 0 ];
        }

        String[] pathEls = path.split( File.pathSeparator );
        int nel = pathEls.length;
        URL[] urls = new URL[ nel ];
        for ( int i = 0; i < nel; i++ ) {
            if ( pathEls[ i ].length() == 0 ) {
                pathEls[ i ] = ".";
            }
            urls[ i ] = URLUtils.makeURL( pathEls[ i ] );
        }
        return urls;
    }
}
