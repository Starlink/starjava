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
        TLD_CONE = readIcon( "cone.gif" ),
        TLD_SIA = readIcon( "sia.gif" ),
        TLD_SSA = readIcon( "ssa.gif" ),
        TLD_REGISTRY = readIcon( "registry.gif" ),
        TLD_TAP = readIcon( "tap.gif" ),
        ADQL_UNDO = readIcon( "undo.gif" ),
        ADQL_REDO = readIcon( "redo.gif" ),
        ADQL_ADDTAB = readIcon( "add_tab.gif" ),
        ADQL_COPYTAB = readIcon( "copy_tab.gif" ),
        ADQL_REMOVETAB = readIcon( "remove_tab.gif" ),
        ADQL_TITLETAB = readIcon( "title_tab.gif" ),
        ADQL_ERROR = readIcon( "error.gif" ),
        ADQL_CLEAR = readIcon( "clear.gif" ),
        ADQL_INSERTTABLE = readIcon( "insert_table.gif" ),
        ADQL_INSERTCOLS = readIcon( "insert_columns.gif" ),
        NODE_SERVICE = readIcon( "service_node.gif" ),
        NODE_TABLE = readIcon( "table_node.gif" ),
        EXTLINK = readIcon( "extlink.gif" ),

        /* Placeholder and terminator. */
        VO_DOWHAT = readIcon( "burst.gif" );

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
