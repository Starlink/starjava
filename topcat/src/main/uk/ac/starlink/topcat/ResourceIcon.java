package uk.ac.starlink.topcat;

import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.help.JHelp;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.plaf.metal.MetalCheckBoxIcon;
import uk.ac.starlink.hapi.HapiTableLoadDialog;
import uk.ac.starlink.table.gui.FileChooserTableLoadDialog;
import uk.ac.starlink.table.gui.FilestoreTableLoadDialog;
import uk.ac.starlink.table.gui.SQLTableLoadDialog;
import uk.ac.starlink.topcat.interop.TopcatServer;
import uk.ac.starlink.topcat.plot.ErrorModeSelectionModel;
import uk.ac.starlink.topcat.plot.SphereWindow;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.vo.ConeSearchDialog;
import uk.ac.starlink.vo.Ri1RegistryTableLoadDialog;
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

    /** Location of image resource files relative to this class. */
    public static final String PREFIX = "images/";

    /* All the class members are defined here. */
    public static final ImageIcon

        /* Special. */
        DO_WHAT = makeIcon( "burst.png" ),

        /* Adverts. */
        STARLINK = makeIcon( "starlinklogo3.png" ),
        TABLE = makeIcon( "browser1.png" ),
        TOPCAT_LOGO = makeIcon( "tc_sok.png" ),
        TOPCAT_LOGO_SMALL = makeIcon( "tc_sok_24.png" ),
        TOPCAT_LOGO_XM = makeIcon( "tc_sok_santa.png" ),
        TOPCAT_LOGO_XM_SMALL = makeIcon( "tc_sok_santa_24.png" ),
        STAR_LOGO = makeIcon( "starlink48.png" ),
        ASTROGRID_LOGO = makeIcon( "ag48.png" ),
        BRISTOL_LOGO = makeIcon( "bris48.png" ),
        VOTECH_LOGO = makeIcon( "votech48.png" ),
        STFC_LOGO = makeIcon( "stfc48.png" ),
        GAVO_LOGO = makeIcon( "gavo48.png" ),
        ESA_LOGO = makeIcon( "esa48.png" ),
        VIZIER_LOGO = makeIcon( "vizier_logo.png" ),
        EU_LOGO = makeIcon( "eu48.png" ),
        EUROPLANET_LOGO = makeIcon( "europlanet48.png" ),

        /* Generic actions. */
        CLOSE = makeIcon( "multiply4.png"),
        EXIT = makeIcon( "exit.png" ),
        LOAD = makeIcon( "Open24.png" ),
        SAVE = makeIcon( "Save24.png" ),
        IMPORT = makeIcon( "browser1.png" ),
        PRINT = makeIcon( "Print24.png" ),
        PRINT_ZIP = makeIcon( "Print24_compressed.png" ),
        IMAGE = makeIcon( "picture.png" ),
        FITS = makeIcon( "fits1.png" ),
        COPY = makeIcon( "Copy24.png" ),
        REDO = makeIcon( "Redo24.png" ),
        PAUSE = makeIcon( "Pause24.png" ),
        SCROLL = makeIcon( "History24.png" ),
        ADD = makeIcon( "Plus1.png" ),
        SUBTRACT = makeIcon( "Minus1.png" ),
        DELETE = makeIcon( "trash2.png" ),
        HELP = makeIcon( "Help3.png" ),
        HELP_BROWSER = makeIcon( "Help3b.png" ),
        DEMO = makeIcon( "demo.png" ),
        HIDE = makeIcon( "false.png" ),
        REVEAL = makeIcon( "true.png" ),
        HIDE_ALL = makeIcon( "falseAll2.png" ),
        REVEAL_ALL = makeIcon( "trueAll2.png" ),
        HIDE_ALL_TINY = makeIcon( "falseAllTiny.png" ),
        REVEAL_ALL_TINY = makeIcon( "trueAllTiny.png" ),
        MODIFY = makeIcon( "redo3.png" ),
        SEARCH = makeIcon( "search2.png" ),
        LOG = makeIcon( "book3.png" ),
        CLEAR = makeIcon( "newdoc1.png" ),
        HIDE_WINDOWS = makeIcon( "hide1.png" ),
        SCROLLER = makeIcon( "scroll2.png" ),
        PARALLEL = makeIcon( "parallel1.png" ),
        STILTS = makeIcon( "stilts.png" ),
        EDIT = makeIcon( "pencil.png" ),

        /* Windows. */
        CONTROL = makeIcon( "controlw.png" ),
        COLUMNS = makeIcon( "colmeta0.png" ),
        STATS = makeIcon( "sigma0.png" ),
        HISTOGRAM = makeIcon( "hist0.png" ),
        CUMULATIVE = makeIcon( "cum0.png" ),
        NORMALISE = makeIcon( "hnorm1.png" ),
        PLOT = makeIcon( "plot0.png" ),
        DENSITY = makeIcon( "2hist2.png" ),
        PLOT3D = makeIcon( "3dax6.png" ),
        SPHERE = makeIcon( "sphere2.png" ),
        STACK = makeIcon( "stack1.png" ),
        PARAMS = makeIcon( "tablemeta0.png" ),
        VIEWER = makeIcon( "browser1.png" ),
        SUBSETS = makeIcon( "venn2.png" ),
        FUNCTION = makeIcon( "fx4.png" ),
        MATCH1 = makeIcon( "matchOne2.png" ),
        MATCH2 = makeIcon( "matchTwo2.png" ),
        MATCHN = makeIcon( "matchN.png" ),
        CONCAT = makeIcon( "concat4.png" ),
        MULTICONE = makeIcon( "cones.png" ),
        MULTISIA = makeIcon( "sias.png" ),
        MULTISSA = makeIcon( "ssas.png" ),
        DATALINK = makeIcon( "datalink2.png" ),
        SAMP = makeIcon( "comms2.png" ),
        GAVO = makeIcon( "virgo3.png" ),
        VIZIER = makeIcon( "vizmini.png" ),
        BASTI = makeIcon( "BaSTI_icon_B.png" ),
        TREE_DIALOG = makeIcon( "browse.png" ),
        CLASSIFY = makeIcon( "classify3.png" ),
        ACTIVATE = makeIcon( "activate3.png" ),
        REACTIVATE = makeIcon( "flash-1.png" ),
        REACTIVATE_ALL = makeIcon( "flash-2d.png" ),
        ACTIVATE_SEQ = makeIcon( "flash1seq.png" ),
        ACTIVATE_SEQ_ALL = makeIcon( "flash2seq.png" ),
        PAUSE_SEQ = makeIcon( "pauseseq.png" ),
        CANCEL_SEQ = makeIcon( "stopseq.png" ),
        DELETE_INACTIVE = makeIcon( "trash2-box.png" ),

        /* Specific actions. */
        UNSORT = makeIcon( "arrow_level.png" ),
        DELETE_COLUMN = makeIcon( "ColumnDelete24.png" ),
        VISIBLE_SUBSET = makeIcon( "spoints5.png" ),
        JEL_VISIBLE_SUBSET = makeIcon( "jelvis.png" ),
        RANGE_SUBSET = makeIcon( "sbars0.png" ),
        XRANGE_SUBSET = makeIcon( "xrange1.png" ),
        BLOB_SUBSET = makeIcon( "blob2.png" ),
        BLOB_SUBSET_END = makeIcon( "ublob3b.png" ),
        POLY_SUBSET = makeIcon( "poly2.png" ),
        POLY_SUBSET_END = makeIcon( "unpoly2.png" ),
        RESIZE = makeIcon( "4way3.png" ),
        RESIZE_X = makeIcon( "ew_arrow.png" ),
        RESIZE_Y = makeIcon( "ns_arrow.png" ),
        GRID_ON = makeIcon( "gridon.png" ),
        GRID_OFF = makeIcon( "gridoff.png" ),
        Y_CURSOR = makeIcon( "vline0.png" ),
        Y0_LINE = makeIcon( "y0line1.png" ),
        TO_COLUMN = makeIcon( "Column.png" ),
        HIGHLIGHT = makeIcon( "somerows.png" ),
        APPLY_SUBSET = makeIcon( "subset1.png" ),
        COUNT = makeIcon( "ab3.png" ),
        RECOUNT = makeIcon( "re-ab3.png" ),
        INVERT = makeIcon( "invert3.png" ),
        HEAD = makeIcon( "head.png" ),
        TAIL = makeIcon( "tail.png" ),
        SAMPLE = makeIcon( "sample.png" ),
        INCLUDE_ROWS = makeIcon( "selrows3.png" ),
        EXCLUDE_ROWS = makeIcon( "exrows.png" ),
        UP = makeIcon( "arrow_n_pad.png" ),
        DOWN = makeIcon( "arrow_s_pad.png" ),
        UP_TRIM = makeIcon( "arrow_n.png" ),
        DOWN_TRIM = makeIcon( "arrow_s.png" ),
        MOVE_UP = makeIcon( "Up2.png" ),
        MOVE_DOWN = makeIcon( "Down2.png" ),
        EQUATION = makeIcon( "xeq.png" ),
        EXPLODE = makeIcon( "explode.png" ),
        COLLAPSE = makeIcon( "collapse.png" ),
        ADDSKY = makeIcon( "addsky1.png" ),
        COLOR_LOG = makeIcon( "logred2.png" ),
        XLOG = makeIcon( "xlog.png" ),
        YLOG = makeIcon( "ylog.png" ),
        XFLIP = makeIcon( "xflip.png" ),
        YFLIP = makeIcon( "yflip.png" ),
        XYZ = makeIcon( "xyz.png" ),
        FOG = makeIcon( "fog1.png" ),
        ANTIALIAS = makeIcon( "aaA4.png" ),
        COLOR = makeIcon( "rgb.png" ),
        FINE = makeIcon( "smallpix.png" ),
        ROUGH = makeIcon( "bigpix2.png" ),
        AXIS_EDIT = makeIcon( "axed3.png" ),
        AXIS_LOCK = makeIcon( "axlock.png" ),
        AUX_LOCK = makeIcon( "auxlock.png" ),
        BROADCAST = makeIcon( "tx3.png" ),
        SEND = makeIcon( "phone2.png" ),
        ADD_TAB = makeIcon( "atab3.png" ),
        REMOVE_TAB = makeIcon( "rtab3.png" ),
        COLORS = makeIcon( "colours2.png" ),
        ADD_COLORS = makeIcon( "acolour1.png" ),
        REMOVE_COLORS = makeIcon( "rcolour1.png" ),
        NORTH = makeIcon( "north2.png" ),
        WEIGHT = makeIcon( "weight6.png" ),
        JPEG = makeIcon( "jpeg1.png" ),
        SPLIT = makeIcon( "split4.png" ),
        FORWARD = makeIcon( "Forward24.png" ),
        BACKWARD = makeIcon( "Back24.png" ),
        PAGE_SETUP = makeIcon( "PageSetup24.png" ),
        MANUAL = makeIcon( "book1.png" ),
        MANUAL_BROWSER = makeIcon( "book1b.png" ),
        MANUAL1_BROWSER = makeIcon( "scroll1b.png" ),
        LEGEND = makeIcon( "legend3.png" ),
        LABEL = makeIcon( "label2.png" ),
        RADIAL = makeIcon( "clock1.png" ),
        CONNECT = makeIcon( "connected-24.png" ),
        DISCONNECT = makeIcon( "disconnected-24.png" ),
        NO_HUB = makeIcon( "nohub.png" ),
        PDF = makeIcon( "pdf3.png" ),
        TUNING = makeIcon( "TuningFork.png" ),
        PROFILE = makeIcon( "vu-meter.png" ),
        SYSTEM = makeIcon( "sysbrowser.png" ),
        KEEP_OPEN = makeIcon( "tack2.png" ),
        LISTEN = makeIcon( "tcear_sok.png" ),
        TO_BROWSER = makeIcon( "toBrowser.png" ),
        SYNTAX = makeIcon( "syntax.png" ),
        FOOTPRINT = makeIcon( "footprint.png" ),
        ZOOM_IN = makeIcon( "mag-plus.png" ),
        ZOOM_OUT = makeIcon( "mag-minus.png" ),
        SKETCH = makeIcon( "sketch1.png" ),
        PROGRESS = makeIcon( "plot_prog.png" ),
        MATCHPLOT = makeIcon( "matchplot2.png" ),
        CDSXMATCH = makeIcon( "xm3.png" ),
        HISTO_SAVE = makeIcon( "histo-save.png" ),
        HISTO_IMPORT = makeIcon( "histo-import.png" ),
        WARNING = makeIcon( "warning_triangle.png" ),
        APPROVE_ALL = makeIcon( "approve_all.png" ),
        MEASURE = makeIcon( "measure.png" ),
        LOCK = makeIcon( "lock2.png" ),
        QAPPROX = makeIcon( "qapprox.png" ),
        RESET_AUTH = makeIcon( "rublock.png" ),

        /* Plot2 icons. */
        UP_DOWN = makeIcon( "updown8.png" ),
        FLOAT = makeIcon( "float1.png" ),
        PLOT_DATA = makeIcon( "dataplot.png" ),
        PLOT_PAIR = makeIcon( "pairplot.png" ),
        PLOT_QUAD = makeIcon( "quadplot.png" ),
        PLOT_HISTO = makeIcon( "histoplot.png" ),
        PLOT_AREA = makeIcon( "areaplot.png" ),
        PLOT_VECTOR = makeIcon( "vectorplot.png" ),
        PLOT_MATRIX = makeIcon( "pm1.png" ),
        PLOT2_PLANE = makeIcon( "plot2plane.png" ),
        PLOT2_SKY = makeIcon( "plot2sky.png" ),
        PLOT2_CUBE = makeIcon( "plot2cube.png" ),
        PLOT2_SPHERE = makeIcon( "plot2sphere.png" ),
        PLOT2_MATRIX = makeIcon( "plot2matrix.png" ),
        PLOT2_TIME = makeIcon( "plot2time.png" ),
        PLOT2_HISTOGRAM = makeIcon( "plot2hist.png" ),
        HISTOBARS = makeIcon( "histobars.png" ),
        NAV_HELP = makeIcon( "navhelp.png" ),
        NAV_DEC = makeIcon( "navdec1.png" ),
        SMALL_CLOSE = makeIcon( "x9.png" ),
        SMALL_HELP = makeIcon( "q9.png" ),
        ADD_CONTROL = makeIcon( "addlayer4.png" ),
        AXIS_CONFIG = makeIcon( "axed5.png" ),
        FRAME_CONFIG = makeIcon( "axframe.png" ),
        SAVE_OVERLAY = makeIcon( "save_over.png" ),
        IMPORT_OVERLAY = makeIcon( "table_over.png" ),

        /* Datanode (hierarchy browser) icons. */
        COLLAPSED = makeIcon( "handle1.png" ),
        EXPANDED = makeIcon( "handle2.png" ),
        HOME = makeIcon( "Home24.png" ),
        TV_UP = makeIcon( "Up.png" ),
        TV_DOWN = makeIcon( "Down.png" ),

        /* Other JTree icons. */
        FOLDER_NODE = makeIcon( "folder_node.png" ),
        LIBRARY_NODE = makeIcon( "book_leaf.png" ),
        FUNCTION_NODE = makeIcon( "fx_leaf.png" ),
        CONSTANT_NODE = makeIcon( "c_leaf.png" ),

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

    private static Component dummyComponent_;

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
     * Returns an Image for this icon if it can, or <code>null</code> if it
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
        return server == null ? null
                              : server.getRelativeUrl( PREFIX + location );
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
     * Paints a stack of icons on top of each other and returns the result.
     * Later elements obscure earlier ones
     *
     * @param  icons  icon array
     * @return  combination
     */
    public static ImageIcon combineIcons( Icon[] icons ) {
        int width = 0;
        int height = 0;
        for ( int i = 0; i < icons.length; i++ ) {
            width = Math.max( width, icons[ i ].getIconWidth() );
            height = Math.max( height, icons[ i ].getIconHeight() );
        }
        BufferedImage image =
            new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB );
        Component c = getDummyComponent();
        Graphics2D g2 = image.createGraphics();
        Composite compos0 = g2.getComposite();
        g2.setComposite( AlphaComposite.Src );
        for ( int i = 0; i < icons.length; i++ ) {
            icons[ i ].paintIcon( c, g2, 0, 0 );
            g2.setComposite( compos0 );
        }
        g2.dispose();
        return new ImageIcon( image );
    }

    /**
     * Doctors an icon representing a control to look like it means adding
     * that control.  Currently, it puts a little plus sign in the corner.
     *
     * @param  baseIcon  standard sized (24x24) base icon
     * @return  doctored icon
     */
    public static ImageIcon toAddIcon( Icon baseIcon ) {
        return combineIcons( new Icon[] { baseIcon, ADD_CONTROL } );
    }

    /**
     * Doctors an icon look like it means saving data associated with that
     * icon.
     * Currently, it puts a little floppy disk icon in the corner.
     *
     * @param  baseIcon  standard sized (24x24) base icon
     * @return  doctored icon
     */
    public static ImageIcon toSaveIcon( Icon baseIcon ) {
        return combineIcons( new Icon[] { baseIcon, SAVE_OVERLAY } );
    }

    /**
     * Doctors an icon look like it means importing a table
     * associated with that icon.
     * Currently, it puts a little table icon in the corner.
     *
     * @param  baseIcon  standard sized (24x24) base icon
     * @return  doctored icon
     */
    public static ImageIcon toImportIcon( Icon baseIcon ) {
        return combineIcons( new Icon[] { baseIcon, IMPORT_OVERLAY } );
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
     * Checks that all the required resource files are present for 
     * this class.  If any of the image files are not present, it will
     * throw an informative FileNotFoundException.
     *
     * @throws  FileNotFoundException   if any of the graphics 
     *          files are missing
     */
    public static void checkResourcesPresent() throws FileNotFoundException {
        List<String> notFound = new ArrayList<String>();
        for ( ResourceIcon icon : getMemberNameMap().values() ) {
            icon.readBaseIcon();
            if ( ! icon.resourceFound.booleanValue() ) {
                notFound.add( icon.location );
            }
        }
        if ( notFound.size() > 0 ) {
            StringBuffer msg = new StringBuffer()
                              .append( "Resource files not found:" );
            for ( String loc : notFound ) {
                msg.append( ' ' )
                   .append( loc );
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
        Map<String,ResourceIcon> iconMap = getMemberNameMap();
        List<String> iconList = new ArrayList<String>( iconMap.keySet() );
        Collections.sort( iconList );
        for ( String name : iconList ) {
            ResourceIcon icon = iconMap.get( name );
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
    private static Map<String,ResourceIcon> getMemberNameMap() {
        Map<String,ResourceIcon> nameMap =
            new LinkedHashMap<String,ResourceIcon>();
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
    @SuppressWarnings("static")
    private static Map<String,Icon> getDiyIconMap() {
        Map<String,Icon> nameMap = new LinkedHashMap<String,Icon>();
        ErrorModeSelectionModel errX = new ErrorModeSelectionModel( 0, "X" );
        ErrorModeSelectionModel errY = new ErrorModeSelectionModel( 1, "Y" );
        ErrorModeSelectionModel errZ = new ErrorModeSelectionModel( 2, "Z" );
        putMap( nameMap, "ERROR_X", errX.createOnOffButton().getIcon() );
        putMap( nameMap, "ERROR_Y", errY.createOnOffButton().getIcon() );
        putMap( nameMap, "ERROR_Z", errZ.createOnOffButton().getIcon() );
        putMap( nameMap, "ERROR_TANGENT",
                         SphereWindow.createTangentErrorIcon() );
        putMap( nameMap, "ERROR_NONE",
                         errX.getIcon( ErrorMode.NONE, 24, 24, 1, 1 ) );
        putMap( nameMap, "ERROR_SYMMETRIC",
                         errX.getIcon( ErrorMode.SYMMETRIC, 24, 24, 1, 1 ) );
        putMap( nameMap, "ERROR_LOWER",
                         errX.getIcon( ErrorMode.LOWER, 24, 24, 1, 1 ) );
        putMap( nameMap, "ERROR_UPPER",
                         errX.getIcon( ErrorMode.UPPER, 24, 24, 1, 1 ) );
        putMap( nameMap, "ERROR_BOTH",
                         errX.getIcon( ErrorMode.BOTH, 24, 24, 1, 1 ) );
        putMap( nameMap, "FILESTORE_DIALOG",
                         new FilestoreTableLoadDialog().getIcon() );
        putMap( nameMap, "FILECHOOSER_DIALOG",
                         new FileChooserTableLoadDialog().getIcon() );
        putMap( nameMap, "SQL_DIALOG",
                         new SQLTableLoadDialog().getIcon() );
        putMap( nameMap, "CONE_DIALOG",
                         new ConeSearchDialog().getIcon() );
        putMap( nameMap, "SIAP_DIALOG",
                         new SiapTableLoadDialog().getIcon() );
        putMap( nameMap, "SSAP_DIALOG",
                         new SsapTableLoadDialog().getIcon() );
        putMap( nameMap, "TAP_DIALOG",
                         new TapTableLoadDialog().getIcon() );
        putMap( nameMap, "HAPI",
                         new HapiTableLoadDialog().getIcon() );
        putMap( nameMap, "REGISTRY_DIALOG",
                         new Ri1RegistryTableLoadDialog().getIcon() );
        putMap( nameMap, "HELP_TOC",
                         new ImageIcon( JHelp.class
                            .getResource( "plaf/basic/images/TOCNav.gif" ) ) );
        putMap( nameMap, "HELP_SEARCH",
                         new ImageIcon( JHelp.class
                        .getResource( "plaf/basic/images/SearchNav.gif" ) ) );

        /* Pull in icons from external packages. */
        Class<?>[] riClazzes = new Class<?>[] {
            uk.ac.starlink.ttools.gui.ResourceIcon.class,
            uk.ac.starlink.vo.ResourceIcon.class,
        };
        for ( Class<?> riClazz : riClazzes ) {
            Field[] fields = riClazz.getDeclaredFields();
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
                     putMap( nameMap, name, icon );
                }
            }
        }

        /* Icons which are the result of calling toAddIcon on existing ones. */
        uk.ac.starlink.ttools.gui.ResourceIcon TTRI =
            uk.ac.starlink.ttools.gui.ResourceIcon.REF;
        putMap( nameMap, "ADD_PLOT_DATA", toAddIcon( PLOT_DATA ) );
        putMap( nameMap, "ADD_PLOT_PAIR", toAddIcon( PLOT_PAIR ) );
        putMap( nameMap, "ADD_PLOT_QUAD", toAddIcon( PLOT_QUAD ) );
        putMap( nameMap, "ADD_PLOT_MATRIX", toAddIcon( PLOT_MATRIX ) );
        putMap( nameMap, "ADD_PLOT_AREA", toAddIcon( PLOT_AREA ) );
        putMap( nameMap, "ADD_PLOT_HISTO", toAddIcon( PLOT_HISTO ) );
        putMap( nameMap, "ADD_PLOT_HEALPIX", toAddIcon( TTRI.PLOT_HEALPIX ) );
        putMap( nameMap, "ADD_PLOT_SPECTRO", toAddIcon( TTRI.PLOT_SPECTRO ) );
        putMap( nameMap, "ADD_PLOT_XYARRAY", toAddIcon( PLOT_VECTOR ) );
        putMap( nameMap, "ADD_PLOT_FUNCTION", toAddIcon( TTRI.PLOT_FUNCTION ) );
        putMap( nameMap, "ADD_PLOT_SKYGRID", toAddIcon( TTRI.PLOT_SKYGRID ) );
        putMap( nameMap, "ADD_FORM_MARK", toAddIcon( TTRI.FORM_MARK ) );
        putMap( nameMap, "ADD_FORM_SIZE", toAddIcon( TTRI.FORM_SIZE ) );
        putMap( nameMap, "ADD_FORM_SIZEXY", toAddIcon( TTRI.FORM_SIZEXY ) );
        putMap( nameMap, "ADD_FORM_VECTOR", toAddIcon( TTRI.FORM_VECTOR ) );
        putMap( nameMap, "ADD_FORM_ERROR", toAddIcon( TTRI.FORM_ERROR ) );
        putMap( nameMap, "ADD_FORM_XYELLIPSE",
                         toAddIcon( TTRI.FORM_XYELLIPSE ) );
        putMap( nameMap, "ADD_FORM_SKYELLIPSE",
                         toAddIcon( TTRI.FORM_SKYELLIPSE ) );
        putMap( nameMap, "ADD_FORM_ELLIPSE_CORR",
                         toAddIcon( TTRI.FORM_ELLIPSE_CORR ) );
        putMap( nameMap, "ADD_PLOT_LINE", toAddIcon( TTRI.PLOT_LINE ) );
        putMap( nameMap, "ADD_FORM_LINEARFIT",
                         toAddIcon( TTRI.FORM_LINEARFIT ) );
        putMap( nameMap, "ADD_FORM_QUANTILE", toAddIcon( TTRI.FORM_QUANTILE ) );
        putMap( nameMap, "ADD_PLOT_LABEL", toAddIcon( TTRI.PLOT_LABEL ) );
        putMap( nameMap, "ADD_PLOT_AREALABEL",
                         toAddIcon( TTRI.PLOT_AREALABEL ) );
        putMap( nameMap, "ADD_PLOT_CONTOUR", toAddIcon( TTRI.PLOT_CONTOUR ) );
        putMap( nameMap, "ADD_FORM_GRID", toAddIcon( TTRI.FORM_GRID ) );
        putMap( nameMap, "ADD_FORM_SKYDENSITY",
                         toAddIcon( TTRI.FORM_SKYDENSITY ) );
        putMap( nameMap, "ADD_FORM_FILL", toAddIcon( TTRI.FORM_FILL ) );
        putMap( nameMap, "ADD_FORM_HISTOGRAM",
                         toAddIcon( TTRI.FORM_HISTOGRAM ) );
        putMap( nameMap, "ADD_FORM_KDE", toAddIcon( TTRI.FORM_KDE ) );
        putMap( nameMap, "ADD_FORM_KNN", toAddIcon( TTRI.FORM_KNN ) );
        putMap( nameMap, "ADD_FORM_DENSOGRAM",
                         toAddIcon( TTRI.FORM_DENSOGRAM ) );
        putMap( nameMap, "ADD_FORM_GAUSSIAN",
                         toAddIcon( TTRI.FORM_GAUSSIAN ) );
        putMap( nameMap, "ADD_FORM_MARKS2", toAddIcon( TTRI.FORM_MARKS2 ) );
        putMap( nameMap, "ADD_FORM_LINK2", toAddIcon( TTRI.FORM_LINK2 ) );

        /* Icons which are the result of calling toSaveIcon/toImportIcon
         * on existing ones. */
        putMap( nameMap, "SAVE_FORM_SKYDENSITY",
                         toSaveIcon( TTRI.FORM_SKYDENSITY ) );
        putMap( nameMap, "IMPORT_FORM_SKYDENSITY",
                         toImportIcon( TTRI.FORM_SKYDENSITY ) );
        putMap( nameMap, "SAVE_FORM_HISTOGRAM",
                         toSaveIcon( TTRI.FORM_HISTOGRAM ) );
        putMap( nameMap, "IMPORT_FORM_HISTOGRAM",
                         toImportIcon( TTRI.FORM_HISTOGRAM ) );
        putMap( nameMap, "SAVE_FORM_GRID",
                         toSaveIcon( TTRI.FORM_GRID ) );
        putMap( nameMap, "IMPORT_FORM_GRID",
                         toImportIcon( TTRI.FORM_GRID ) );

        return nameMap;
    }

    /**
     * Adds an entry to a map, making sure that it doesn't overwrite
     * any previous entry.
     *
     * @param    map  map to update
     * @param   key   new key
     * @param   value  new value
     * @throws   IllegalArgumentException  iff map already contains key
     */
    private static void putMap( Map<String,Icon> map, String key, Icon value ) {
        if ( map.containsKey( key ) ) {
            throw new IllegalArgumentException( "Map already contains key "
                                              + key );
        }
        map.put( key, value );
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
     * Writes an icon as a PNG to a given filename.
     *
     * @param   icon  icon to draw
     * @param   file  destination file
     */
    private static void writePng( Icon icon, File file ) throws IOException {
        String format = "PNG";
        boolean fmtok = ImageIO.write( toImage( icon ), format, file );
        if ( ! fmtok ) {
            throw new IOException( "Unknown format " + format );
        }
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
     * Converts an Icon to an BufferedImage.
     *
     * @param  input icon
     * @return  image with icon painted on it
     */
    private static BufferedImage toImage( Icon icon ) {
        int w = icon.getIconWidth();
        int h = icon.getIconHeight();

        /* Create an image to draw on. */
        BufferedImage image =
            new BufferedImage( w, h, BufferedImage.TYPE_INT_ARGB );
        Graphics2D g2 = image.createGraphics();

        /* This clears the image.  It may not be necessary.
         * What I really want to do is to clear it to transparent white
         * (0x00ffffff) but this seems incredibly hard to do,
         * it's likely to be transparent black instead (0x00000000).
         * It shouldn't make a difference, but in some cases writing a
         * PNG with the wrong coloured transparent background shows up
         * coloured in a PDF. */
        g2.setComposite( AlphaComposite.Clear );
        g2.fillRect( 0, 0, w, h );

        /* Copy the icon completely; this will overwrite the background
         * if the icon is an image or otherwise fills the area,
         * so meaning failure of the previous step doesn't matter. */
        g2.setComposite( AlphaComposite.Src );

        /* Paint the icon. */
        icon.paintIcon( getDummyComponent(), g2, 0, 0 );

        /* Clear up and return the icon. */
        g2.dispose();
        return image;
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
            for ( ResourceIcon icon : getMemberNameMap().values() ) {
                System.out.println( icon.location );
            }
        }
        else if ( "-entities".equals( mode ) ) {
            Map<String,ResourceIcon> iconMap = getMemberNameMap();
            String t1 = "  <!ENTITY IMG.";
            String t2 = " '<img src=\"../" + PREFIX;
            String t3 = "\"/>'>";
            for ( Map.Entry<String,ResourceIcon> entry :
                  getMemberNameMap().entrySet() ) {
                System.out.println( new StringBuffer()
                                   .append( t1 )
                                   .append( entry.getKey() )
                                   .append( t2 )
                                   .append( entry.getValue().location )
                                   .append( t3 ) );
            }
            for ( String name : getDiyIconMap().keySet() ) {
                System.out.println( new StringBuffer()
                                   .append( t1 )
                                   .append( name )
                                   .append( t2 )
                                   .append( name )
                                   .append( ".png" )
                                   .append( t3 ) );
            }
        }
        else if ( "-writepngs".equals( mode ) ) {
            for ( Map.Entry<String,Icon> entry : getDiyIconMap().entrySet() ) {
                writePng( entry.getValue(),
                          new File( entry.getKey() + ".png" ) );
            }
        }
        else {
            String usage =
                "Usage: ResourceIcon [-map|-files|-entities|-writepngs]";
            System.err.println( usage );
            System.exit( 1 );
        }
    }
}
