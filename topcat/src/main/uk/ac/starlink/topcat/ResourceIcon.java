package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
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
import javax.help.HelpSet;
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

    /** Location of image resource files relative to this class. */
    public static final String PREFIX = "images/";

    /** List of all class members. */
    private static List allMembers = new ArrayList();

    /* All the class members are defined here. */
    public static final ResourceIcon

        /* Special. */
        DO_WHAT = new ResourceIcon( "burst.gif" ),
        BLANK = new ResourceIcon( "blank24.gif" ),

        /* Adverts. */
        TOPCAT = new ResourceIcon( "TopCat2.gif" ),
        TOPCAT_LOGO = new ResourceIcon( "tc3.gif" ),
        STARLINK = new ResourceIcon( "starlinklogo3.gif" ),

        /* Generic actions. */
        CLOSE = new ResourceIcon( "multiply4.gif"),
        LOAD = new ResourceIcon( "Open24.gif" ),
        SAVE = new ResourceIcon( "Save24.gif" ),
        PRINT = new ResourceIcon( "Print24.gif" ),
        COPY = new ResourceIcon( "Copy24.gif" ),
        REDO = new ResourceIcon( "Redo24.gif" ),
        ADD = new ResourceIcon( "Plus1.gif" ),
        REMOVE = new ResourceIcon( "Minus1.gif" ),
        HELP = new ResourceIcon( "Help3.gif" ),

        /* Windows. */
        COLUMNS = new ResourceIcon( "colmeta0.gif" ),
        STATS = new ResourceIcon( "sigma0.gif" ),
        PLOT = new ResourceIcon( "plot0.gif" ),
        PARAMS = new ResourceIcon( "tablemeta0.gif" ),
        VIEWER = new ResourceIcon( "browser1.gif" ),
        SUBSETS = new ResourceIcon( "venn2.gif" ),

        /* Specific actions. */
        UNSORT = DO_WHAT,
        DELETE_COLUMN = new ResourceIcon( "ColumnDelete24.gif" ),
        VISIBLE_SUBSET = new ResourceIcon( "spoints5.gif" ),
        RESIZE = new ResourceIcon( "4way3.gif" ),
        GRID_ON = new ResourceIcon( "gridon.gif" ),
        GRID_OFF = new ResourceIcon( "gridoff.gif" ),
        TO_COLUMN = new ResourceIcon( "Column.gif" ),

        FORWARD = new ResourceIcon( "Forward24.gif" ),
        BACKWARD = new ResourceIcon( "Back24.gif" ),
        PAGE_SETUP = new ResourceIcon( "PageSetup24.gif" ),

        /* Metal. */
        QUERY = new ResourceIcon( "query_message.gif" ),

        /* Dummy terminator. */
        dummy = DO_WHAT;

    private String location;
    private Icon baseIcon;
    private Boolean resourceFound;

    private ResourceIcon( String location ) {
        this.location = location;
        allMembers.add( this );
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
        for ( Iterator it = allMembers.iterator(); it.hasNext(); ) {
            ResourceIcon icon = (ResourceIcon) it.next();
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
     * Returns a {@link javax.help.Map} of the ResourceIcon objects declared
     * as public static final members of this class.  Thus you can use
     * a target ID which is the name of the member of this class, and
     * it will be understood to refer to the URL where the corresponding
     * icon can be found.
     * <p>
     * <b>This method doesn't work</b>
     *
     * @param  hs  the HelpSet for which the Map is required.  This is
     *             pretty much ignored, but needs to be used to construct
     *             the Map.ID instances
     * @return   a Map which understands where resource icons can be found
     */
    public static javax.help.Map getHelpMap( HelpSet hs ) {
        return new ResourceIconHelpMap( hs );
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
        java.util.Map iconMap = getMemberNameMap();
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
    private static java.util.Map getMemberNameMap() {
        java.util.Map nameMap = new HashMap();
        Field[] fields = ResourceIcon.class.getDeclaredFields();
        for ( int i = 0; i < fields.length; i++ ) {
            Field field = fields[ i ];
            int mods = field.getModifiers();
            String name = field.getName();
            if ( field.getType() == ResourceIcon.class &&
                 Modifier.isPublic( mods ) &&
                 Modifier.isStatic( mods ) &&
                 Modifier.isFinal( mods ) &&
                 name.equals( name.toUpperCase() ) ) {
                try {
                    nameMap.put( name, field.get( null ) );
                }
                catch ( IllegalAccessException e ) {
                    throw new AssertionError( e );
                }
            }
        }
        return nameMap;
    }

    /**
     * Invokes the {@link #writeHelpMapXML} method to standard output.
     */
    public static void main( String[] args ) {
        String mode = args.length == 1 ? args[ 0 ] : null;
        if ( mode == null ) {
            System.err.println( "Usage: ResourceIcon [-map|-files]" );
        }
        else if ( mode.equals( "-map" ) ) {
            writeHelpMapXML( System.out, "../" );
        }
        else if ( mode.equals( "-files" ) ) {
            java.util.Map iconMap = getMemberNameMap();
            for ( Iterator it = iconMap.keySet().iterator(); it.hasNext(); ) {
                ResourceIcon icon = (ResourceIcon) iconMap.get( it.next() );
                System.out.println( icon.location );
            }
        }
    }

    /**
     * This class implements a javax.help.Map which can use targets
     * which have the name given by the names of the declared static
     * ResourceIcon members of this class and turn them into URLs.
     * <p>
     * For an interface so small, javax.help.Map is incredibly confusingly
     * written.
     */
    static class ResourceIconHelpMap implements javax.help.Map {

        java.util.Map url2mapidMap = new HashMap();
        java.util.Map idstring2urlMap = new HashMap();
        List mapidList = new ArrayList();

        /**
         * Constructs a new javax.help.Map based on the resources defined
         * by the ResourceIcon class.
         * 
         * @param  hs  the HelpSet for which the Map is required.  This is
         *             pretty much ignored, but needs to be used to construct
         *             the Map.ID instances
         */
        public ResourceIconHelpMap( HelpSet hs ) {
            java.util.Map nameMap = getMemberNameMap();
            for ( Iterator it = nameMap.entrySet().iterator(); it.hasNext(); ) {
                java.util.Map.Entry entry = (java.util.Map.Entry) it.next();
                String idstring = (String) entry.getKey();
                URL url = ((ResourceIcon) entry.getValue()).getURL();
                if ( idstring != null && url != null ) {
                    javax.help.Map.ID mapid = javax.help.Map.ID
                                             .create( idstring, hs );
                    idstring2urlMap.put( idstring, url );
                    url2mapidMap.put( url, mapid );
                    mapidList.add( mapid );
                }
            }
        }

        public boolean isValidID( String idstring, HelpSet hs ) {
            return idstring2urlMap.containsKey( idstring );
        }

        public Enumeration getAllIDs() {
            return Collections.enumeration( mapidList );
        }

        public URL getURLFromID( javax.help.Map.ID mapid ) {
            String idstring = mapid.id;
            return (URL) idstring2urlMap.get( idstring );
        }

        public boolean isID( URL url ) {
            return url2mapidMap.containsKey( url );
        }

        public javax.help.Map.ID getIDFromURL( URL url ) {
            return (javax.help.Map.ID) url2mapidMap.get( url );
        }

        public javax.help.Map.ID getClosestID( URL url ) {
            return getIDFromURL( url );
        }

        public Enumeration getIDs( URL url ) {
            javax.help.Map.ID mapid = 
                (javax.help.Map.ID) url2mapidMap.get( url );
            Collection answer = ( mapid == null )
                              ? (Collection) Collections.nCopies( 0, null )
                              : (Collection) Collections.singleton( mapid );
            return Collections.enumeration( answer );
        }

    }
}
