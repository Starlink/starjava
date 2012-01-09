package uk.ac.starlink.topcat;

import Acme.JPM.Encoders.GifEncoder;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.help.JHelp;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.plaf.metal.MetalCheckBoxIcon;
import uk.ac.starlink.table.gui.FileChooserTableLoadDialog;
import uk.ac.starlink.table.gui.FilestoreTableLoadDialog;
import uk.ac.starlink.table.gui.SQLTableLoadDialog;
import uk.ac.starlink.topcat.interop.TopcatServer;
import uk.ac.starlink.topcat.plot.ErrorModeSelectionModel;
import uk.ac.starlink.topcat.plot.SphereWindow;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.vo.ConeSearchDialog;
import uk.ac.starlink.vo.RegistryTableLoadDialog;
import uk.ac.starlink.vo.SiapTableLoadDialog;
import uk.ac.starlink.vo.SsapTableLoadDialog;
import uk.ac.starlink.vo.TapTableLoadDialog;

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

    private static Component dummyComponent_;

    /** Location of image resource files relative to this class. */
    public static final String PREFIX = "images/";

    /* All the class members are defined here. */
    public static final ImageIcon

        /* Special. */
        DO_WHAT = makeIcon( "burst.gif" ),

        /* Adverts. */
        TOPCAT = makeIcon( "TopCat2.gif" ),
        STARLINK = makeIcon( "starlinklogo3.gif" ),
        TABLE = makeIcon( "browser1.gif" ),
        TOPCAT_LOGO = makeIcon( "tc3.gif" ),
        TOPCAT_LOGO_SMALL = makeIcon( "tc3_24.gif" ),
        TOPCAT_LOGO_XM = makeIcon( "tc_santa.gif" ),
        TOPCAT_LOGO_XM_SMALL = makeIcon( "tc_santa_24.gif" ),
        STAR_LOGO = makeIcon( "starlink48.gif" ),
        ASTROGRID_LOGO = makeIcon( "ag48.gif" ),
        BRISTOL_LOGO = makeIcon( "bris48.gif" ),
        VOTECH_LOGO = makeIcon( "votech48.gif" ),
        STFC_LOGO = makeIcon( "stfc48.gif" ),
        GAVO_LOGO = makeIcon( "gavo48.gif" ),
        VIZIER_LOGO = makeIcon( "vizier_logo.gif" ),

        /* Generic actions. */
        CLOSE = makeIcon( "multiply4.gif"),
        EXIT = makeIcon( "exit.gif" ),
        LOAD = makeIcon( "Open24.gif" ),
        SAVE = makeIcon( "Save24.gif" ),
        IMPORT = makeIcon( "browser1.gif" ),
        PRINT = makeIcon( "Print24.gif" ),
        PRINT_ZIP = makeIcon( "Print24_compressed.gif" ),
        IMAGE = makeIcon( "picture.gif" ),
        FITS = makeIcon( "fits1.gif" ),
        COPY = makeIcon( "Copy24.gif" ),
        REDO = makeIcon( "Redo24.gif" ),
        ADD = makeIcon( "Plus1.gif" ),
        SUBTRACT = makeIcon( "Minus1.gif" ),
        DELETE = makeIcon( "trash2.gif" ),
        HELP = makeIcon( "Help3.gif" ),
        HELP_BROWSER = makeIcon( "Help3b.gif" ),
        DEMO = makeIcon( "demo.gif" ),
        HIDE = makeIcon( "false.gif" ),
        REVEAL = makeIcon( "true.gif" ),
        HIDE_ALL = makeIcon( "falseAll2.gif" ),
        REVEAL_ALL = makeIcon( "trueAll2.gif" ),
        MODIFY = makeIcon( "redo3.gif" ),
        SEARCH = makeIcon( "search2.gif" ),
        LOG = makeIcon( "book3.gif" ),
        CLEAR = makeIcon( "newdoc1.gif" ),
        HIDE_WINDOWS = makeIcon( "hide1.gif" ),
        SCROLLER = makeIcon( "scroll2.gif" ),

        /* Windows. */
        CONTROL = makeIcon( "controlw.gif" ),
        COLUMNS = makeIcon( "colmeta0.gif" ),
        STATS = makeIcon( "sigma0.gif" ),
        HISTOGRAM = makeIcon( "hist0.gif" ),
        CUMULATIVE = makeIcon( "cum0.gif" ),
        NORMALISE = makeIcon( "hnorm1.gif" ),
        PLOT = makeIcon( "plot0.gif" ),
        DENSITY = makeIcon( "2hist2.gif" ),
        PLOT3D = makeIcon( "3dax6.gif" ),
        SPHERE = makeIcon( "sphere2.gif" ),
        STACK = makeIcon( "stack1.gif" ),
        PARAMS = makeIcon( "tablemeta0.gif" ),
        VIEWER = makeIcon( "browser1.gif" ),
        SUBSETS = makeIcon( "venn2.gif" ),
        FUNCTION = makeIcon( "fx2.gif" ),
        MATCH1 = makeIcon( "matchOne2.gif" ),
        MATCH2 = makeIcon( "matchTwo2.gif" ),
        MATCHN = makeIcon( "matchN.gif" ),
        CONCAT = makeIcon( "concat4.gif" ),
        MULTICONE = makeIcon( "cones.gif" ),
        MULTISIA = makeIcon( "sias.gif" ),
        MULTISSA = makeIcon( "ssas.gif" ),
        SAMP = makeIcon( "comms2.gif" ),
        GAVO = makeIcon( "virgo3.gif" ),
        VIZIER = makeIcon( "vizmini.gif" ),
        BASTI = makeIcon( "BaSTI_icon_B.gif" ),
        TREE_DIALOG = makeIcon( "browse.gif" ),

        /* Specific actions. */
        UNSORT = makeIcon( "arrow_level.gif" ),
        DELETE_COLUMN = makeIcon( "ColumnDelete24.gif" ),
        VISIBLE_SUBSET = makeIcon( "spoints5.gif" ),
        RANGE_SUBSET = makeIcon( "sbars0.gif" ),
        XRANGE_SUBSET = makeIcon( "xrange1.gif" ),
        BLOB_SUBSET = makeIcon( "blob2.gif" ),
        BLOB_SUBSET_END = makeIcon( "ublob3b.gif" ),
        RESIZE = makeIcon( "4way3.gif" ),
        RESIZE_X = makeIcon( "ew_arrow.gif" ),
        RESIZE_Y = makeIcon( "ns_arrow.gif" ),
        GRID_ON = makeIcon( "gridon.gif" ),
        GRID_OFF = makeIcon( "gridoff.gif" ),
        Y_CURSOR = makeIcon( "vline0.gif" ),
        Y0_LINE = makeIcon( "y0line1.gif" ),
        TO_COLUMN = makeIcon( "Column.gif" ),
        COUNT = makeIcon( "ab3.gif" ),
        RECOUNT = makeIcon( "re-ab3.gif" ),
        INVERT = makeIcon( "invert3.gif" ),
        HEAD = makeIcon( "head.gif" ),
        TAIL = makeIcon( "tail.gif" ),
        SAMPLE = makeIcon( "sample.gif" ),
        INCLUDE_ROWS = makeIcon( "selrows3.gif" ),
        EXCLUDE_ROWS = makeIcon( "exrows.gif" ),
        UP = makeIcon( "arrow_n_pad.gif" ),
        DOWN = makeIcon( "arrow_s_pad.gif" ),
        UP_TRIM = makeIcon( "arrow_n.gif" ),
        DOWN_TRIM = makeIcon( "arrow_s.gif" ),
        MOVE_UP = makeIcon( "Up2.gif" ),
        MOVE_DOWN = makeIcon( "Down2.gif" ),
        EQUATION = makeIcon( "xeq.gif" ),
        EXPLODE = makeIcon( "explode.gif" ),
        ADDSKY = makeIcon( "addsky1.gif" ),
        COLOR_LOG = makeIcon( "logred2.gif" ),
        XLOG = makeIcon( "xlog.gif" ),
        YLOG = makeIcon( "ylog.gif" ),
        XFLIP = makeIcon( "xflip.gif" ),
        YFLIP = makeIcon( "yflip.gif" ),
        XYZ = makeIcon( "xyz.gif" ),
        FOG = makeIcon( "fog1.gif" ),
        ANTIALIAS = makeIcon( "aaA4.gif" ),
        COLOR = makeIcon( "rgb.gif" ),
        FINE = makeIcon( "smallpix.gif" ),
        ROUGH = makeIcon( "bigpix2.gif" ),
        AXIS_EDIT = makeIcon( "axed3.gif" ),
        BROADCAST = makeIcon( "tx3.gif" ),
        SEND = makeIcon( "phone2.gif" ),
        ADD_TAB = makeIcon( "atab3.gif" ),
        REMOVE_TAB = makeIcon( "rtab3.gif" ),
        ADD_COLORS = makeIcon( "acolour1.gif" ),
        REMOVE_COLORS = makeIcon( "rcolour1.gif" ),
        NORTH = makeIcon( "north2.gif" ),
        WEIGHT = makeIcon( "weight6.gif" ),
        JPEG = makeIcon( "jpeg1.gif" ),
        SPLIT = makeIcon( "split4.gif" ),
        FORWARD = makeIcon( "Forward24.gif" ),
        BACKWARD = makeIcon( "Back24.gif" ),
        PAGE_SETUP = makeIcon( "PageSetup24.gif" ),
        MANUAL = makeIcon( "book1.gif" ),
        MANUAL_BROWSER = makeIcon( "book1b.gif" ),
        MANUAL1_BROWSER = makeIcon( "scroll1b.gif" ),
        LEGEND = makeIcon( "legend3.gif" ),
        LABEL = makeIcon( "label2.gif" ),
        RADIAL = makeIcon( "clock1.gif" ),
        CONNECT = makeIcon( "connected-24.gif" ),
        DISCONNECT = makeIcon( "disconnected-24.gif" ),
        NO_HUB = makeIcon( "nohub.gif" ),
        PDF = makeIcon( "pdf3.gif" ),
        TUNING = makeIcon( "TuningFork.gif" ),
        PROFILE = makeIcon( "vu-meter.gif" ),
        SYSTEM = makeIcon( "sysbrowser.gif" ),
        KEEP_OPEN = makeIcon( "tack2.gif" ),
        LISTEN = makeIcon( "tcear3.gif" ),
        TO_BROWSER = makeIcon( "toBrowser.gif" ),
        SYNTAX = makeIcon( "syntax.gif" ),
        FOOTPRINT = makeIcon( "footprint.gif" ),

        /* Non-standard sizes. */
        SMALL_DEC = makeIcon( "dec.gif" ),
        SMALL_INC = makeIcon( "inc.gif" ),

        /* Datanode (hierarchy browser) icons. */
        COLLAPSED = makeIcon( "handle1.gif" ),
        EXPANDED = makeIcon( "handle2.gif" ),
        HOME = makeIcon( "Home24.gif" ),
        TV_UP = makeIcon( "Up.gif" ),
        TV_DOWN = makeIcon( "Down.gif" ),

        /* Other JTree icons. */
        FOLDER_NODE = makeIcon( "folder_node.gif" ),
        LIBRARY_NODE = makeIcon( "book_leaf.gif" ),
        FUNCTION_NODE = makeIcon( "fx_leaf.gif" ),
        CONSTANT_NODE = makeIcon( "c_leaf.gif" ),

        /* Dummy terminator. */
        dummy = DO_WHAT;

    /** Blank icon. */
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
     * PREFIX + location relative to this class.  This will probably be
     * a jar: protocol URL and only useful to Java applications (possibly
     * only within this JVM).
     *
     * @return  the icon URL
     */
    public URL getURL() {
        return getClass().getResource( PREFIX + location );
    }

    /**
     * Returns a URL from which this icon can be retrieved by external
     * applications.  This is served from TOPCAT's internal HTTP server,
     * and so is only available as long as this instance of the program
     * is running.
     *
     * @return  url, or null if no server is running
     */
    public URL getExternalURL() throws IOException {
        TopcatServer server = TopcatServer.getInstance();
        return server == null
             ? null
             : new URL( server.getTopcatPackageUrl(), PREFIX + location );
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
     * Returns a full-size TOPCAT logo for display.
     *
     * @return  topcat logo
     */
    public static Icon getTopcatLogo() {
        return isFestive() ? ResourceIcon.TOPCAT_LOGO_XM
                           : ResourceIcon.TOPCAT_LOGO;
    }

    /**
     * Returns an icon-size TOPCAT logo.
     *
     * @return   24x24 pixel topcat logo
     */
    public static Icon getTopcatLogoSmall() {
        return isFestive() ? ResourceIcon.TOPCAT_LOGO_XM_SMALL
                           : ResourceIcon.TOPCAT_LOGO_SMALL;
    }

    /**
     * This entirely frivolous method determines whether the date is close
     * enough to Christmas to put a santa hat on the TOPCAT logo.
     * The corresponding functionality is largely undocumented.
     *
     * @return  true  iff it's festive time
     */
    static boolean isFestive() {
        Calendar now = Calendar.getInstance();
        int month = now.get( Calendar.MONTH );
        int day = now.get( Calendar.DAY_OF_MONTH );
        return ( month == Calendar.DECEMBER && day >= 15 )
            || ( month == Calendar.JANUARY && day <= 6 );
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
     * Provides an empty component.
     *
     * @return   lazily constructed component
     */
    private static Component getDummyComponent() {
        if ( dummyComponent_ == null ) {
            dummyComponent_ = new JPanel();
        }
        return dummyComponent_;
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
                ResourceIcon ricon = null;
                if ( icon instanceof ResourceIcon ) {
                    ricon = (ResourceIcon) icon;
                }
                else if ( icon instanceof ResourceImageIcon ) {
                    ricon = ((ResourceImageIcon) icon).getResourceIcon();
                }
                if ( ricon != null ) {
                    nameMap.put( name, ricon );
                }
            }
        }
        return nameMap;
    }

    /**
     * Returns a map of Icon objects which are <em>not</em> ResourceIcons.
     * Each (key,value) entry of the map is given by the (name,Icon) pair.
     * This method is only called at build time, not at runtime.
     *
     * @return  member name => member value mapping for hand-drawn icons
     */
    private static Map getDiyIconMap() {
        Map nameMap = new HashMap();
        ErrorModeSelectionModel errX = new ErrorModeSelectionModel( 0, "X" );
        ErrorModeSelectionModel errY = new ErrorModeSelectionModel( 1, "Y" );
        ErrorModeSelectionModel errZ = new ErrorModeSelectionModel( 2, "Z" );
        nameMap.put( "ERROR_X", errX.createOnOffButton().getIcon() );
        nameMap.put( "ERROR_Y", errY.createOnOffButton().getIcon() );
        nameMap.put( "ERROR_Z", errZ.createOnOffButton().getIcon() );
        nameMap.put( "ERROR_TANGENT", SphereWindow.createTangentErrorIcon() );
        nameMap.put( "ERROR_NONE",
                     errX.getIcon( ErrorMode.NONE, 24, 24, 1, 1 ) );
        nameMap.put( "ERROR_SYMMETRIC",
                     errX.getIcon( ErrorMode.SYMMETRIC, 24, 24, 1, 1 ) );
        nameMap.put( "ERROR_LOWER",
                     errX.getIcon( ErrorMode.LOWER, 24, 24, 1, 1 ) );
        nameMap.put( "ERROR_UPPER",
                     errX.getIcon( ErrorMode.UPPER, 24, 24, 1, 1 ) );
        nameMap.put( "ERROR_BOTH",
                     errX.getIcon( ErrorMode.BOTH, 24, 24, 1, 1 ) );
        nameMap.put( "FILESTORE_DIALOG",
                     new FilestoreTableLoadDialog().getIcon() );
        nameMap.put( "FILECHOOSER_DIALOG",
                     new FileChooserTableLoadDialog().getIcon() );
        nameMap.put( "SQL_DIALOG",
                     new SQLTableLoadDialog().getIcon() );
        nameMap.put( "CONE_DIALOG",
                     new ConeSearchDialog().getIcon() );
        nameMap.put( "SIAP_DIALOG",
                     new SiapTableLoadDialog().getIcon() );
        nameMap.put( "SSAP_DIALOG",
                     new SsapTableLoadDialog().getIcon() );
        nameMap.put( "TAP_DIALOG",
                     new TapTableLoadDialog().getIcon() );
        nameMap.put( "REGISTRY_DIALOG",
                     new RegistryTableLoadDialog().getIcon() );
        nameMap.put( "HELP_TOC",
                     new ImageIcon( JHelp.class
                             .getResource( "plaf/basic/images/toc.gif" ) ) );
        nameMap.put( "HELP_SEARCH",
                     new ImageIcon( JHelp.class
                             .getResource( "plaf/basic/images/search.gif" ) ) );
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
     * Returns an ImageIcon based on a given Icon object.  If the supplied
     * <code>icon</code> is already an ImageIcon, it is returned.  Otherwise,
     * it is painted to an Image and an ImageIcon is constructed from that.
     * The reason this is useful is that some Swing components will only
     * grey out disabled icons if they are ImageIcon subclasses (which is
     * naughty).
     *
     * @param  icon  input icon
     * @return   image icon
     */
    public static ImageIcon toImageIcon( Icon icon ) {
        if ( icon instanceof ImageIcon ) {
            return (ImageIcon) icon;
        }
        else {
            return new ImageIcon( createImage( icon ) );
        }
    }

    /**
     * Returns an image got by drawing an Icon.
     *
     * @param  icon 
     * @return  image
     */
    private static BufferedImage createImage( Icon icon ) {
        int w = icon.getIconWidth();
        int h = icon.getIconHeight();

        /* Create an image to draw on. */
        BufferedImage image =
            new BufferedImage( w, h, BufferedImage.TYPE_INT_ARGB );
        Graphics2D g2 = image.createGraphics();

        /* Clear it to transparent white. */
        Color color = g2.getColor();
        Composite compos = g2.getComposite();
        g2.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC ) );
        g2.setColor( new Color( 1f, 1f, 1f, 0f ) );
        g2.fillRect( 0, 0, w, h );
        g2.setColor( color );
        g2.setComposite( compos );

        /* Paint the icon. */
        icon.paintIcon( getDummyComponent(), g2, 0, 0 );

        /* Tidy up and return the image. */
        g2.dispose();
        return image;
    }

    /**
     * Writes an icon as a GIF to a given filename.
     *
     * @param   icon  icon to draw
     * @param   file  destination file
     */
    private static void writeGif( Icon icon, File file ) throws IOException {
        OutputStream out = new FileOutputStream( file );
        try {
            out = new BufferedOutputStream( out );
            new GifEncoder( createImage( icon ), out ).encode();
        }
        finally {
            out.close();
        }
    }

    /**
     * Invokes the {@link #writeHelpMapXML} method to standard output.
     */
    public static void main( String[] args ) throws IOException {
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
            Map diyMap = getDiyIconMap();
            for ( Iterator it = diyMap.keySet().iterator(); it.hasNext(); ) {
                String name = (String) it.next();
                System.out.println( t1 + name + t2 + name + ".gif" + t3 );
            }
        }
        else if ( "-writegifs".equals( mode ) ) {
            Map diyMap = getDiyIconMap();
            for ( Iterator it = diyMap.keySet().iterator(); it.hasNext(); ) {
                String name = (String) it.next();
                Icon icon = (Icon) diyMap.get( name );
                writeGif( icon, new File( name + ".gif" ) );
            }
        }
        else {
            String usage =
                "Usage: ResourceIcon [-map|-files|-entities|-writegifs]";
            System.err.println( usage );
            System.exit( 1 );
        }
    }
}
