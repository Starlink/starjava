package uk.ac.starlink.hds;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides information about the status of the JNIHDS package.
 * This class offers a method which can be invoked to determine whether
 * the {@link HDSObject} class is available for use.
 *
 * @author   Mark Taylor (Starlink)
 */
public class HDSPackage {

    private static Boolean loaded;
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.hds" );

    /** Private sol constructor to prevent instantiation. */
    private HDSPackage() {}

    /**
     * Indicates whether the HDSObject class is available or not.
     * This will return <code>true</code> if the JNIHDS classes can be used,
     * buf <code>false</code> if the requisite native code is not
     * available (the shared library is not on <code>java.library.path</code>).
     * If the classes are not available, then the first time it is invoked
     * it will write a warning to that effect via the logger.
     *
     * @return  <code>true</code> iff the HDSObject class is available
     */
    public static boolean isAvailable() {
        if ( loaded == null ) {
            try {
                int i = HDSObject.DAT__SZNAM;
                loaded = Boolean.TRUE;
            }
            catch ( LinkageError e ) {
                logger.log( Level.INFO, e.getMessage(), e );
                logger.warning( "JNIHDS load failed - no NDF/HDS access" );
                loaded = Boolean.FALSE;
            }
        }
        return loaded.booleanValue();
    }
}
