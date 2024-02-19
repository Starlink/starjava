package uk.ac.starlink.vo;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Stores icons used by the VO package.
 *
 * @author   Mark Taylor
 * @since    15 Jul 2015
 */
public class ResourceIcon {

    /**
     * Sole class instance.
     * This doesn't do anything but may be useful for referencing members.
     */
    public static final ResourceIcon REF = new ResourceIcon();

    public static final Icon

        /* Icons. */
        TLD_CONE = readIcon( "cone.png" ),
        TLD_SIA = readIcon( "sia.png" ),
        TLD_SSA = readIcon( "ssa.png" ),
        TLD_REGISTRY = readIcon( "registry.png" ),
        TLD_TAP = readIcon( "tap.png" ),
        ADQL_UNDO = readIcon( "undo.png" ),
        ADQL_REDO = readIcon( "redo.png" ),
        ADQL_ADDTAB = readIcon( "add_tab.png" ),
        ADQL_COPYTAB = readIcon( "copy_tab.png" ),
        ADQL_REMOVETAB = readIcon( "remove_tab.png" ),
        ADQL_TITLETAB = readIcon( "title_tab.png" ),
        ADQL_ERROR = readIcon( "error.png" ),
        ADQL_FIXUP = readIcon( "fixup.png" ),
        ADQL_CLEAR = readIcon( "clear.png" ),
        ADQL_INSERTTABLE = readIcon( "insert_table.png" ),
        ADQL_INSERTCOLS = readIcon( "insert_columns.png" ),
        NODE_SERVICE = readIcon( "service_node.png" ),
        NODE_TABLE = readIcon( "table_node.png" ),
        NODE_FUNCTION = readIcon( "function_node.png" ),
        NODE_SIGNATURE = readIcon( "signature_node.png" ),
        NODE_DOC = readIcon( "doc_node.png" ),
        EXTLINK = readIcon( "extlink.png" ),
        RELOAD = readIcon( "reload.png" ),
        AUTH_NO = readIcon( "auth-no.png" ),
        AUTH_YES = readIcon( "auth-yes.png" ),
        AUTH_REQNO = readIcon( "auth-reqno.png" ),
        AUTH_NONE = readIcon( "auth-none.png" ),

        /* Placeholder and terminator. */
        VO_DOWHAT = readIcon( "burst.png" );

    /**
     * Private constructor prevents instantiation.
     */
    ResourceIcon() {
    }

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
