package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.help.HelpSet;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.plaf.metal.MetalCheckBoxIcon;

/**
 * Handles the procurement of icons and other graphics for the TableViewer
 * and related classes.  All the icons required by these classes are
 * provided as static final members of this class.
 * <p>
 * This class should really implement {@link javax.swing.Icon} rather
 * than extending {@link javax.swing.ImageIcon}.  However in Sun's J2SE1.4
 * AbstractButton implementation there is a bit where it will only 
 * grey out the icon if it actually is an ImageIcon.  So we inherit
 * from there.
 * 
 * @author   Mark Taylor (Starlink)
 */
public class ResourceIcon implements Icon {

    /** Location of image resource files relative to this class. */
    public static final String PREFIX = "images/";

    /* All the class members are defined here. */
    public static final ImageIcon

        /* Special. */
        DO_WHAT = makeIcon( "burst.gif" ),

        /* Adverts. */
        TOPCAT = makeIcon( "TopCat2.gif" ),
        TOPCAT_LOGO = makeIcon( "tc3.gif" ),
        STARLINK = makeIcon( "starlinklogo3.gif" ),
        TABLE = makeIcon( "browser1.gif" ),

        /* Generic actions. */
        CLOSE = makeIcon( "multiply4.gif"),
        EXIT = makeIcon( "exit.gif" ),
        LOAD = makeIcon( "Open24.gif" ),
        SAVE = makeIcon( "Save24.gif" ),
        PRINT = makeIcon( "Print24.gif" ),
        IMAGE = makeIcon( "picture.gif" ),
        COPY = makeIcon( "Copy24.gif" ),
        REDO = makeIcon( "Redo24.gif" ),
        ADD = makeIcon( "Plus1.gif" ),
        DELETE = makeIcon( "trash2.gif" ),
        HELP = makeIcon( "Help3.gif" ),
        DEMO = makeIcon( "demo.gif" ),
        HIDE = makeIcon( "false.gif" ),
        REVEAL = makeIcon( "true.gif" ),
        MODIFY = makeIcon( "redo3.gif" ),
        HIDE_WINDOWS = makeIcon( "hide1.gif" ),

        /* Windows. */
        COLUMNS = makeIcon( "colmeta0.gif" ),
        STATS = makeIcon( "sigma0.gif" ),
        PLOT = makeIcon( "plot0.gif" ),
        PARAMS = makeIcon( "tablemeta0.gif" ),
        VIEWER = makeIcon( "browser1.gif" ),
        SUBSETS = makeIcon( "venn2.gif" ),
        FUNCTION = makeIcon( "fx2.gif" ),
        MATCH1 = makeIcon( "matchOne2.gif" ),
        MATCH2 = makeIcon( "matchTwo2.gif" ),
        CONCAT = makeIcon( "concat4.gif" ),

        /* Specific actions. */
        UNSORT = DO_WHAT,
        DELETE_COLUMN = makeIcon( "ColumnDelete24.gif" ),
        VISIBLE_SUBSET = makeIcon( "spoints5.gif" ),
        BLOB_SUBSET = makeIcon( "blob2.gif" ),
        BLOB_SUBSET_END = makeIcon( "ublob3b.gif" ),
        RESIZE = makeIcon( "4way3.gif" ),
        GRID_ON = makeIcon( "gridon.gif" ),
        GRID_OFF = makeIcon( "gridoff.gif" ),
        TO_COLUMN = makeIcon( "Column.gif" ),
        COUNT = makeIcon( "ab3.gif" ),
        INVERT = makeIcon( "invert3.gif" ),
        INCLUDE_ROWS = makeIcon( "selrows3.gif" ),
        EXCLUDE_ROWS = makeIcon( "exrows.gif" ),
        UP = makeIcon( "arrow_n_pad.gif" ),
        DOWN = makeIcon( "arrow_s_pad.gif" ),
        UP_TRIM = makeIcon( "arrow_n.gif" ),
        DOWN_TRIM = makeIcon( "arrow_s.gif" ),
        EQUATION = makeIcon( "xeq.gif" ),
        PLOT_LINES = makeIcon( "lines.gif" ),

        FORWARD = makeIcon( "Forward24.gif" ),
        BACKWARD = makeIcon( "Back24.gif" ),
        PAGE_SETUP = makeIcon( "PageSetup24.gif" ),

        /* Treeview (hierarchy browser) icons. */
        COLLAPSED = makeIcon( "handle1.gif" ),
        EXPANDED = makeIcon( "handle2.gif" ),
        HOME = makeIcon( "Home24.gif" ),
        TV_UP = makeIcon( "Up.gif" ),
        TV_DOWN = makeIcon( "Down.gif" ),

        /* Metal. */
        QUERY = makeIcon( "query_message.gif" ),

        /* Dummy terminator. */
        dummy = DO_WHAT;

    public static final Icon BLANK = new Icon() {
        public int getIconHeight() {
            return 24;
        }
        public int getIconWidth() {
            return 24;
        }
        public void paintIcon( Component c, Graphics g, int x, int y ) {
        }
    };

    private String location;
    private Icon baseIcon;
    private Boolean resourceFound;

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
     * Returns an Image for this icon if it can, or <tt>null</tt> if it
     * can't for some reason.
     *
     * @return  an Image
     */
    public Image getImage() {
        Icon icon = getBaseIcon();
        return ( icon instanceof ImageIcon ) 
             ? ((ImageIcon) icon).getImage()
             : null;
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
                resourceFound = Boolean.TRUE;
            }
            catch ( IOException e ) {
            }
        }
        if ( icon == null ) {
            icon = dummyIcon();
            resourceFound = Boolean.FALSE;
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

    /**
     * Checks that all the required resource files are present for 
     * this class.  If any of the image files are not present, it will
     * return throw an informative FileNotFoundException.
     *
     * @throws  FileNotFoundException   if any of the graphics 
     *          files are missing
     */
    public static void checkResourcesPresent() throws FileNotFoundException {
        List notFound = new ArrayList();
        for ( Iterator it = getMemberNameMap().entrySet().iterator(); 
              it.hasNext(); ) {
            ResourceIcon icon =
                (ResourceIcon) ((Map.Entry) it.next()).getValue();
            icon.readBaseIcon();
            if ( ! icon.resourceFound.booleanValue() ) {
                notFound.add( icon.location );
            }
        }
        if ( notFound.size() > 0 ) {
            StringBuffer msg = new StringBuffer()
                              .append( "Resource files not found:" );
            for ( Iterator it = notFound.iterator(); it.hasNext(); ) {
                msg.append( ' ' )
                   .append( it.next() );
            }
            throw new FileNotFoundException( msg.toString() );
        }
    }

    /**
     * Writes the &lt;mapID&gt; elements required for a JavaHelp map
     * file representing the icons represented by this class.
     * The URLs are relative to the location of the help files.
     *
     * @param  ostrm  the destination output stream for the data
     * @param  prefix  a string to prefix to each relative URL
     */
    public static void writeHelpMapXML( OutputStream ostrm, String prefix ) {
        PrintStream pstrm = ( ostrm instanceof PrintStream )
                          ? (PrintStream) ostrm 
                          : new PrintStream( ostrm );

        /* Writer header. */
        pstrm.println( "<?xml version='1.0'?>" );
        pstrm.println( "<!DOCTYPE map" );
        pstrm.println( "  PUBLIC " +
                       "\"-//Sun Microsystems Inc." +
                       "//DTD JavaHelp Map Version 1.0//EN\"" );
        pstrm.println( "         " +
                       "\"http://java.sun.com/products" +
                       "/javahelp/map_1_0.dtd\">" );
        pstrm.println( "\n<!-- Automatically generated by " +
                       ResourceIcon.class.getName() + 
                       " -->" );
        pstrm.println( "\n<map version='1.0'>" );

        /* Write an entry for each known icon. */
        Map iconMap = getMemberNameMap();
        List iconList = new ArrayList( iconMap.keySet() );
        Collections.sort( iconList );
        for ( Iterator it = iconList.iterator(); it.hasNext(); ) {
            String name = (String) it.next();
            ResourceIcon icon = (ResourceIcon) iconMap.get( name );
            String mapID = new StringBuffer()
                .append( "  <mapID target='" )
                .append( name )
                .append( "'" )
                .append( " url='" )
                .append( prefix )
                .append( PREFIX )
                .append( icon.location )
                .append( "'/>" )
                .toString();
            pstrm.println( mapID );
        }

        /* Write footer. */
        pstrm.println( "</map>" );
    }

    /**
     * Returns a map of the ResourceIcon objects declared as public static
     * final members of this class.  Each (key,value) entry of the map
     * is given by the (name,ResourceIcon) pair.
     *
     * @return  member name => member value mapping for all static
     *          ResourceIcon objects defined by this class
     */
    private static Map getMemberNameMap() {
        Map nameMap = new HashMap();
        Field[] fields = ResourceIcon.class.getDeclaredFields();
        for ( int i = 0; i < fields.length; i++ ) {
            Field field = fields[ i ];
            int mods = field.getModifiers();
            String name = field.getName();
            if ( Icon.class.isAssignableFrom( field.getType() ) &&
                 Modifier.isPublic( mods ) &&
                 Modifier.isStatic( mods ) &&
                 Modifier.isFinal( mods ) &&
                 name.equals( name.toUpperCase() ) ) {
                Icon icon;
                try {
                    icon = (Icon) field.get( null );
                }
                catch ( IllegalAccessException e ) {
                    throw new AssertionError( e );
                }
                ResourceIcon ricon;
                if ( icon instanceof ResourceIcon ) {
                    ricon = (ResourceIcon) icon;
                }
                else if ( icon instanceof ResourceImageIcon ) {
                    ricon = ((ResourceImageIcon) icon).getResourceIcon();
                }
                else if ( icon == BLANK ) {
                    ricon = null;
                }
                else {
                    throw new AssertionError();
                }
                if ( ricon != null ) {
                    nameMap.put( name, ricon );
                }
            }
        }
        return nameMap;
    }

    /**
     * Makes an Icon object from the location.  
     * Really, this should only have to invoke the private ResourceIcon 
     * constructor and return the new ResourceIcon.  However, there is a
     * deficiency in Sun's J2SE1.4 AbstractButton implementation that
     * will only grey out a button icon if it is actually an ImageIcon.
     * We therefore make sure that the icons stored in this class 
     * subclass from ImageIcon.  Nevertheless, the important parts of
     * the implementation are taken from this class, not directly 
     * from ImageIcon.
     * 
     * @param   location  the location of the image to make the ResourceIcon
     * @return  a new Icon
     */
    private static ImageIcon makeIcon( String location ) {
        return new ResourceImageIcon( new ResourceIcon( location ) );
    }

    private static class ResourceImageIcon extends ImageIcon {
        ResourceIcon resourceIcon;
        ResourceImageIcon( ResourceIcon resourceIcon ) {
            this.resourceIcon = resourceIcon;
        }
        public ResourceIcon getResourceIcon() {
            return resourceIcon;
        }
        protected void loadImage() {
        }
        public int getImageLoadStatus() {
            return MediaTracker.COMPLETE;
        }
        public Image getImage() { 
            return resourceIcon.getImage();
        }
        public void paintIcon( Component c, Graphics g, int x, int y ) {
            resourceIcon.paintIcon( c, g, x, y );
        }
        public int getIconWidth() {
            return resourceIcon.getIconWidth();
        }
        public int getIconHeight() {
            return resourceIcon.getIconHeight();
        }
    }

    /**
     * Invokes the {@link #writeHelpMapXML} method to standard output.
     */
    public static void main( String[] args ) {
        String mode = args.length == 1 ? args[ 0 ] : null;
        if ( "-map".equals( mode ) ) {
            writeHelpMapXML( System.out, "../" );
        }
        else if ( "-files".equals( mode ) ) {
            Map iconMap = getMemberNameMap();
            for ( Iterator it = iconMap.keySet().iterator(); it.hasNext(); ) {
                ResourceIcon icon = (ResourceIcon) iconMap.get( it.next() );
                System.out.println( icon.location );
            }
        }
        else if ( "-entities".equals( mode ) ) {
            Map iconMap = getMemberNameMap();
            String t1 = "  <!ENTITY IMG.";
            String t2 = " '<img src=\"../" + PREFIX;
            String t3 = "\"/>'>";
            for ( Iterator it = iconMap.keySet().iterator(); it.hasNext(); ) {
                String name = (String) it.next();
                ResourceIcon icon = (ResourceIcon) iconMap.get( name );
                System.out.println( t1 + name + t2 + icon.location + t3 );
            }
        }
        else {
            String usage = "Usage: ResourceIcon [-map|-files|-entities]";
            System.err.println( usage );
            System.exit( 1 );
        }
    }

}
