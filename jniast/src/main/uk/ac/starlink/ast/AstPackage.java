package uk.ac.starlink.ast;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides information about the status of the JNIAST package.
 * This class offers a method which can be invoked to determine whether
 * the classes in the <tt>uk.ac.starlink.ast</tt> package are available
 * for use.
 *
 * @author   Mark Taylor (Starlink)
 */
public class AstPackage {

    private static Boolean loaded;
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.ast" );

    /** Private sole constructor to prevent instantiation. */
    private AstPackage() {}

    /**
     * Indicates whether the JNIAST package is available or not.
     * This will return <tt>true</tt> if the JNIAST classes can be
     * used, but <tt>false</tt> if the requisite native code is not
     * available (the shared library is not on the <tt>java.library.path</tt>).
     * If the classes are not available, then the first time it is
     * invoked it will write a warning to that effect via the 
     * logger.
     *
     * @return  <tt>true</tt> iff the uk.ac.starlink.ast.* classes are available
     */
    public static boolean isAvailable() {
        if ( loaded == null ) {
            try {
                UnitMap umap = new UnitMap( 1 );
                loaded = Boolean.TRUE;
            }
            catch ( LinkageError e ) {
                loaded = Boolean.FALSE;
                logger.log( Level.INFO, e.toString(), e );
                logger.warning( "JNIAST load failed - no WCS processing" );

            }
        }
        return loaded.booleanValue();
    }
}
