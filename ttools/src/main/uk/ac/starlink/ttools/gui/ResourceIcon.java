package uk.ac.starlink.ttools.gui;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Stores icons used by the ttools package.
 *
 * @author   Mark Taylor
 * @since    9 Feb 2007
 */
public class ResourceIcon {

    /** Node representing a directory. */
    public static final Icon FOLDER_NODE = readIcon( "folder_node.gif" );

    /** Node representing a library. */
    public static final Icon LIBRARY_NODE = readIcon( "book_leaf.gif" );

    /** Node representing a function. */
    public static final Icon FUNCTION_NODE = readIcon( "fx_leaf.gif" );

    /** Node representing a constant. */
    public static final Icon CONSTANT_NODE = readIcon( "c_leaf.gif" );

    /**
     * Reads an icon from a filename representing a resource in this 
     * class's package.
     *
     * @param  name  image name in this package
     */
    private static Icon readIcon( String name ) {
        try {
            URL url = ResourceIcon.class.getResource( name );
            if ( url == null ) {
                return null;
            }
            InputStream in = new BufferedInputStream( url.openStream() );
            byte[] buf = new byte[ 4096 ];
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            try {
                for ( int n; ( n = in.read( buf ) ) >= 0; ) {
                    bout.write( buf, 0, n );
                }
            }
            finally {
                in.close();
                bout.close();
            }
            return new ImageIcon( bout.toByteArray() );
        }
        catch ( IOException e ) {
            return null;
        }
    }
}
