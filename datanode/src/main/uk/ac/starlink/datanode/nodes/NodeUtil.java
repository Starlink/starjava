package uk.ac.starlink.datanode.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.ast.AstPackage;
import uk.ac.starlink.datanode.factory.CreationState;
import uk.ac.starlink.datanode.factory.TamFitsUtil;
import uk.ac.starlink.hds.HDSPackage;

/**
 * Miscellaneous utilities.
 */
public class NodeUtil {

    private static Boolean hasAST_;
    private static Boolean hasHDS_;
    private static Boolean hasJAI_;
    private static Boolean hasGUI_;
    private static Boolean hasTAMFITS_;

    private static Logger logger =
        Logger.getLogger( "uk.ac.starlink.datanode.nodes" );

    /**
     * Indicates whether the bytes in a given buffer look like ASCII text
     * or not.  This is just a guess based on what characters are in there.
     *
     * @param  buf  the buffer to test
     * @return  <code>true</code> iff <code>buf</code> looks like ASCII
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
        if ( hasHDS_ == null ) {
            hasHDS_ = Boolean.valueOf( HDSPackage.isAvailable() );
        }
        return hasHDS_.booleanValue();
    }

    /**
     * Indicates whether the JNIAST package is present.  It might not be
     * if the native libraries for this platform have not been installed.
     *
     * @return  true iff JNIAST is available
     */
    public static boolean hasAST() {
        if ( hasAST_ == null ) {
            hasAST_ = Boolean.valueOf( AstPackage.isAvailable() );
        }
        return hasAST_.booleanValue();
    }

    /**
     * Indicates whether the Java Advanced Imaging classes are available.
     * These are an extension to the J2SE1.4, so may not be present if
     * they have not been installed.
     *
     * @return  true iff JAI is available
     */
    public static boolean hasJAI() {
        if ( hasJAI_ == null ) {
            try {
                /* Use this class because it's lightweight and won't cause a
                 * whole cascade of other classes to be loaded. */
                Class.forName( "javax.media.jai.util.CaselessStringKey" );
                hasJAI_ = Boolean.TRUE;
            }
            catch ( ClassNotFoundException e ) {
                hasJAI_ = Boolean.FALSE;
                logger.warning(
                    "JAI extension not present - no image display" );
            }
        }
        return hasJAI_.booleanValue();
    }

    /**
     * Indicates whether the nom.tam.fits FITS I/O library is available.
     *
     * @return true iff nom.tam.fits is available
     */
    public static boolean hasTAMFITS() {
        if ( hasTAMFITS_ == null ) {
            boolean hasTamfits;
            try {
                new TamFitsUtil();
                hasTamfits = true;
            }
            catch ( NoClassDefFoundError e ) {
                hasTamfits = false;
            }
            hasTAMFITS_ = Boolean.valueOf( hasTamfits );
        }
        return hasTAMFITS_.booleanValue();
    }

    /**
     * Indicates whether applications within this JVM should be considered
     * to be running within a graphical context or not.
     *
     * @return  true  iff this JVM appears to be using graphical components
     */
    public static boolean hasGUI() {
        if ( hasGUI_ != null ) {
            return hasGUI_.booleanValue();
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

    /**
     * Sets whether applications running within this JVM should be 
     * considered to be running within a GUI or not. 
     *
     * @param  hasGUI  true iff this JVM ought to be using graphical components
     */
    public static void setGUI( boolean hasGUI ) {
        hasGUI_ = Boolean.valueOf( hasGUI );
    }

    /**
     * Returns the full path of a node, if possible.
     * The idea is to give a human-readable string indictating what you're
     * looking at, describing position within filesystem, tar archive,
     * XML document, whatever.
     *
     * @param  node  node to trace
     * @return  node path, or null
     */
    public static String getNodePath( DataNode node ) {
        List pathList = accumulatePath( node, new ArrayList() );
        if ( pathList != null ) {
            StringBuffer pathBuf = new StringBuffer();
            Collections.reverse( pathList );
            for ( Iterator it = pathList.iterator(); it.hasNext(); ) {
                pathBuf.append( (String) it.next() );
            }
            return pathBuf.toString();
        }
        else {
            return null;
        }
    }

    /**
     * Recursively accumulates the path of a given datanode into a 
     * list of elements.  The path is returned, or <code>null</code> if
     * a full path is not available.
     *
     * @param  node  the data node whose path is to be accumulated 
     *               into the <code>path</code> list
     * @param  path  a list of path elements; the first element is
     *               furthest away from the root
     * @return  the complete path for <code>node</code> as a list of Strings; 
     *          the root is the last element.  <code>null</code> if no path
     *          can be found
     */
    private static List accumulatePath( DataNode node, List path ) {

        /* Get the contribution from this node. */
        String pathEl = node.getPathElement();
        if ( pathEl == null ) {
            return null;
        }

        /* Get the parent of this node. */
        CreationState creator = node.getCreator();
        if ( creator == null ) {
            return null;
        }
        DataNode parent = creator.getParent();

        /* Get the separator from the parent. */
        String prefix;
        if ( parent == null ) {
            prefix = "";
        }
        else {
            String sep = parent.getPathSeparator();
            if ( sep == null ) {
                return null;
            }
            else {
                prefix = sep;
            }
        }

        /* Add the contribution from this element to the path. */
        path.add( prefix + pathEl );

        /* Return the completed path or recurse. */
        return parent == null ? path : accumulatePath( parent, path );
    }

    /**
     * Returns a short string representation of a DataNode.  This is
     * suitable for use as the string used in rendering the node in the tree.
     *
     * @return   a string summarising the node
     */
    public static String toString( DataNode node ) {
        String result = node.getLabel().trim();
        String desc = node.getDescription();
        if ( desc != null ) {
            desc = desc.trim();
            if ( desc.length() > 0 ) {
                result += "  " + desc;
            }
        }
        return result;
    }
}
