package uk.ac.starlink.table.view;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.awt.Component;
import java.awt.Graphics;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.plaf.metal.MetalCheckBoxIcon;

/**
 * Handles the procurement of icons and other graphics for the TableViewer
 * and related classes.  All the icons required by these classes are
 * provided as static final members of this class.
 * 
 * @author   Mark Taylor (Starlink)
 */
public class ResourceIcon implements Icon {

    public static final ResourceIcon CLOSE = new ResourceIcon( "multiply3.gif" );
    public static final ResourceIcon LOAD = new ResourceIcon( "open3.gif" );
    public static final ResourceIcon DO_WHAT = new ResourceIcon( "burst.gif" );

    public static final ResourceIcon SAVE = DO_WHAT;
    public static final ResourceIcon DUPLICATE = DO_WHAT;
    public static final ResourceIcon PRINT = DO_WHAT;
    public static final ResourceIcon PARAMS = DO_WHAT;
    public static final ResourceIcon COLUMNS = DO_WHAT;
    public static final ResourceIcon STATS = DO_WHAT;
    public static final ResourceIcon PLOT = DO_WHAT;
    public static final ResourceIcon BROWSER = DO_WHAT;
    public static final ResourceIcon UNSORT = DO_WHAT;
    public static final ResourceIcon REDO = DO_WHAT;
    public static final ResourceIcon RESIZE = DO_WHAT;
    public static final ResourceIcon REPLOT = DO_WHAT;

    public static final String PREFIX = "images/";

    private String location;
    private Icon baseIcon;

    private ResourceIcon( String location ) {
        this.location = location;
    }

    private Icon getBaseIcon() {
        if ( baseIcon == null ) {
            baseIcon = readBaseIcon();
        }
        return baseIcon;
    }

    public int getIconHeight() {
        return getBaseIcon().getIconHeight();
    }

    public int getIconWidth() {
        return getBaseIcon().getIconWidth();
    }

    public void paintIcon( Component c, Graphics g, int x, int y ) {
        getBaseIcon().paintIcon( c, g, x, y );
    }

    /**
     * Returns the URL for the image that forms this icon; it is called
     * PREFIX + location relative to this class.
     *
     * @return  the icon URL
     */
    public URL getURL() {
        return getClass().getResource( PREFIX + location );
    }

    /**
     * Reads the icon found at this object's URL, ready for delegation
     * of Icon interface methods.
     *
     * @return  an icon to which Icon calls can be delegated
     */
    private Icon readBaseIcon() {
        Icon icon = null;
        URL resource = getURL();
        if ( resource != null ) {
            try {
                InputStream istrm = resource.openStream();
                istrm = new BufferedInputStream( istrm );
                ByteArrayOutputStream ostrm = new ByteArrayOutputStream();
                int datum;
                while ( ( datum = istrm.read() ) > -1 ) {
                    ostrm.write( datum );
                }
                istrm.close();
                ostrm.close();
                icon = new ImageIcon( ostrm.toByteArray() );
            }
            catch ( IOException e ) {
            }
        }
        if ( icon == null ) {
            icon = dummyIcon();
        }
        return icon;
    }

    /**
     * Returns some icon or other which can be used as a last resort 
     * if no proper icon can be found.
     *
     * @return   some icon
     */
    private static Icon dummyIcon() {
        return new MetalCheckBoxIcon();
    }
}
