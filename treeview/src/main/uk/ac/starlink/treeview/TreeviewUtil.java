package uk.ac.starlink.treeview;

import java.util.logging.Logger;
import uk.ac.starlink.ast.AstPackage;
import uk.ac.starlink.hds.HDSPackage;

/**
 * Miscellaneous utilities.
 */
public class TreeviewUtil {

    private static Boolean hasAST;
    private static Boolean hasHDS;
    private static Boolean hasJAI;
    private static Boolean hasGUI;

    private static Logger logger =
        Logger.getLogger( "uk.ac.starlink.treeview" );

    /**
     * Indicates whether the bytes in a given buffer look like ASCII text
     * or not.  This is just a guess based on what characters are in there.
     *
     * @param  buf  the buffer to test
     * @return  <tt>true</tt> iff <tt>buf</tt> looks like ASCII
     */
    public static boolean isASCII( byte[] buf ) {
        int leng = buf.length;
        boolean hasUnprintables = false;
        for ( int i = 0; i < leng && ! hasUnprintables; i++ ) {
            int bval = buf[ i ];
            boolean isctl = false;
            switch( bval ) {
                case '\n':
                case '\r':
                case '\t':
                case '\f':
                case (byte) 169:  // copyright symbol
                case (byte) 163:  // pound sign
                    isctl = true;
                    break;
                default:
                    // no action
            }
            if ( bval > 126 || bval < 32 && ! isctl ) {
                hasUnprintables = true;
            }
        }
        return ! hasUnprintables;
    }

    /**
     * Indicates whether the JNIHDS package is present.  It might not be
     * if the native libraries for this platform have not been installed.
     *
     * @return  true iff JNIDHS is availble
     */
    public static boolean hasHDS() {
        if ( hasHDS == null ) {
            hasHDS = Boolean.valueOf( HDSPackage.isAvailable() );
        }
        return hasHDS.booleanValue();
    }

    /**
     * Indicates whether the JNIAST package is present.  It might not be
     * if the native libraries for this platform have not been installed.
     *
     * @return  true iff JNIAST is available
     */
    public static boolean hasAST() {
        if ( hasAST == null ) {
            hasAST = Boolean.valueOf( AstPackage.isAvailable() );
        }
        return hasAST.booleanValue();
    }

    /**
     * Indicates whether the Java Advanced Imaging classes are available.
     * These are an extension to the J2SE1.4, so may not be present if
     * they have not been installed.
     *
     * @return  true iff JAI is available
     */
    public static boolean hasJAI() {
        if ( hasJAI == null ) {
            try {
                /* Use this class because it's lightweight and won't cause a
                 * whole cascade of other classes to be loaded. */
                new javax.media.jai.util.CaselessStringKey( "dummy" );
                hasJAI = Boolean.TRUE;
            }
            catch ( NoClassDefFoundError e ) {
                hasJAI = Boolean.FALSE;
                logger.warning(
                    "JAI extension not present - no image display" );
            }
        }
        return hasJAI.booleanValue();
    }

    /**
     * Indicates whether applications within this JVM should be considered
     * to be running within a graphical context or not.
     *
     * @return  true  iff this JVM appears to be using graphical components
     */
    public static boolean hasGUI() {
        if ( hasGUI != null ) {
            return hasGUI.booleanValue();
        }
        else {
            class XLoader extends ClassLoader {
                public boolean isClassLoaded( String name ) {
                    return findLoadedClass( name ) != null;
                }
            }
            return new XLoader().isClassLoaded( "javax.swing.JFrame" );
        }
    }

    public static void setGUI( boolean hasGUI ) {
        TreeviewUtil.hasGUI = Boolean.valueOf( hasGUI );
    }
}
