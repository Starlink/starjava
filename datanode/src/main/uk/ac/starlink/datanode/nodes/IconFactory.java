package uk.ac.starlink.datanode.nodes;

import java.util.*;
import java.io.*;
import java.net.URL;
import javax.swing.*;
import javax.swing.tree.*;

/**
 * Returns {@link javax.swing.Icon}s for use in identifying objects.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class IconFactory {

    public static final String IMAGE_PATH = "/uk/ac/starlink/datanode/images/";

    public static final short NO_ICON = - Short.MAX_VALUE;
    public static final short STOP = 1;
    public static final short EXIT = 2;
    public static final short HELP = 3;
    public static final short DEMO = 4;
    public static final short LOAD = 5;
    public static final short SPLIT_NONE = 11;
    public static final short SPLIT_BELOW = 12;
    public static final short SPLIT_BESIDE = 13;
    public static final short CASCADE = 21;
    public static final short EXCISE = 22;
    public static final short OPEN = 23;
    public static final short CLOSE = 24;
    public static final short RELOAD = 30;
    public static final short DELETE = 31;
    public static final short UP = 32;
    public static final short DOWN = 33;
    public static final short HOME = 34;
    public static final short ARY0 = 91;
    public static final short ARY1 = 92;
    public static final short ARY2 = 93;
    public static final short ARY3 = 94;
    public static final short WHAT = 99;
    public static final short LEAF = 101;
    public static final short PARENT = 102;
    public static final short SCALAR = 103;
    public static final short STRUCTURE = 104;
    public static final short FILE = 105;
    public static final short DIRECTORY = 106;
    public static final short NDF = 107;
    public static final short ZIPFILE = 108;
    public static final short ZIPBRANCH = 109;
    public static final short WCS = 110;
    public static final short FITS = 111;
    public static final short HDU = 112;
    public static final short FRAME = 113;
    public static final short SKYFRAME = 114;
    public static final short HISTORY = 115;
    public static final short HISTORY_RECORD = 116;
    public static final short TABLE = 117;
    public static final short TREE = 118;
    public static final short ERROR = 119;
    public static final short XML_DOCUMENT = 120;
    public static final short XML_ELEMENT = 121;
    public static final short XML_COMMENT = 122;
    public static final short XML_PI = 123;
    public static final short XML_CDATA = 124;
    public static final short XML_EREF = 125;
    public static final short XML_STRING = 126;
    public static final short XML_DTD = 127;
    public static final short NDX = 130;
    public static final short HDX_CONTAINER = 131;
    public static final short TARFILE = 132;
    public static final short TARBRANCH = 133;
    public static final short COMPRESSED = 134;
    public static final short DATA = 135;
    public static final short VOTABLE = 140;
    public static final short VOCOMPONENT = 141;
    public static final short SPECFRAME = 142;
    public static final short MYSPACE = 143;
    public static final short HIERARCH = 144;
    public static final short SPLAT = 200;
    public static final short SOG = 201;
    public static final short TOPCAT = 202;
    public static final short HANDLE_EXPAND = 301;
    public static final short HANDLE_COLLAPSE = 302;
    public static final short TREE_LOGO = 401;
    public static final short STARLINK_LOGO = 402;
    public static final short HANDLE1 = 403;
    public static final short HANDLE2 = 404;
    public static final short STAR_LOGO = 405;
   
    private static boolean festive = isFestive();
    private static Map iconMap = new HashMap();

    private IconFactory() {
    }

    /**
     * Gets the icon with the given ID.
     *
     * @throws  IllegalArgumentException  if <code>id</code> is not one of the
     *          known icon identifiers
     */
    public static Icon getIcon( short id ) {
        return getIconByName( getIconName( id ) );
    }

    /**
     * Gets a URL for the icon with the given ID.  Note that the return
     * may be null if the icon does not have a gif (because it is acquired
     * directly from the UIManager for instance).
     *
     * @param  id  icon identifier
     * @return  the URL of the icon, or null
     * @throws  IllegalArgumentException  if <code>id</code> is not one of the
     *          known icon identifiers
     */
    public static URL getIconURL( short id ) {
        return IconFactory.class.getResource( IMAGE_PATH + getIconName( id ) );
    }

    /**
     * Returns the name by which the icon with a given id is known internally.
     * This may refer to a resource in the IMAGE_PATH directory, or may not.
     * It is used as the key to the <code>iconMap</code> map.
     *
     * @param   id  the icon identifier
     * @return  the internal name of the icon
     * @throws  IllegalArgumentException  if <code>id</code> is not one of the
     *          known icon identifiers
     */
    private static String getIconName( short id ) {
        String iname;
        switch ( id ) {
            case SPLIT_BELOW:    iname = "SplitHorizontal.gif";      break;
            case SPLIT_BESIDE:   iname = "SplitVertical.gif";        break;
            case SPLIT_NONE:     iname = "Frame.gif";                break;
            case CASCADE:        iname = "PlusPlus.gif";             break;
            case EXCISE:         iname = "MinusMinus.gif";           break;
            case OPEN:           iname = "Plus.gif";                 break;
            case CLOSE:          iname = "Minus.gif";                break;
            case RELOAD:         iname = "redoy2.gif";               break;
            case DELETE:         iname = "trasha2.gif";              break;
            case UP:             iname = "Up.gif";                   break;
            case DOWN:           iname = "Down.gif";                 break;
            case HOME:           iname = "Home24.gif";               break;
            case STOP:           iname = "Stop.gif";                 break;
            case EXIT:           iname = "multiply3.gif";            break;
            case HELP:           iname = "Help2.gif";                break;
            case DEMO:           iname = "multi8.gif";               break;
            case LOAD:           iname = "open3.gif";                break;
            case ARY0:           iname = "cell.gif";                 break;
            case ARY1:           iname = "row.gif";                  break;
            case ARY2:           iname = "Sheet.gif";                break;
            case ARY3:           iname = "Sheets.gif";               break;
            case WHAT:           iname = "burst.gif";                break;
            case LEAF:           iname = "Tree.leafIcon";            break;
            case PARENT:         iname = "Tree.closedIcon";          break;
            case SCALAR:         iname = "ball.black.gif";           break;
            case STRUCTURE:      iname = "closed.gif";               break;
            case FILE:           iname = "file1.gif";                break;
            case DIRECTORY:      iname = "dir4.gif";                 break;
            case NDF:            iname = "star_gold.gif";            break;
            case ZIPFILE:        iname = "archgreen.gif";            break;
            case ZIPBRANCH:      iname = "branchgreen.gif";          break;
            case WCS:            iname = "world1.gif";               break;
            case FITS:           iname = "gstar2.gif";               break;
            case HDU:            iname = "TileCascade.gif";          break;
            case FRAME:          iname = "axes42.gif";               break;
            case SKYFRAME:       iname = "axes62.gif";               break;
            case HISTORY:        iname = "book2.gif";                break;
            case HISTORY_RECORD: iname = "mini-doc.gif";             break;
            case TABLE:          iname = "table10.gif";              break;
            case TREE:           iname = !festive ? "TREE28.gif" 
                                                  : "sxtree6.gif";   break;
            case ERROR:          iname = "madsmiley.gif";            break;
            case XML_DOCUMENT:   iname = "xml_doc.gif";              break;
            case XML_ELEMENT:    iname = "xml_el.gif";               break;
            case XML_COMMENT:    iname = "xml_comm.gif";             break;
            case XML_PI:         iname = "xml_pi.gif";               break;
            case XML_CDATA:      iname = "xml_txt.gif";              break;
            case XML_EREF:       iname = "xml_eref.gif";             break;
            case XML_STRING:     iname = "xml_txt.gif";              break;
            case XML_DTD:        iname = "xml_dtd.gif";              break;
            case NDX:            iname = "fuzz3.gif";                break;
            case HDX_CONTAINER:  iname = "box7.gif";                 break;
            case TARFILE:        iname = "archyellow.gif";           break;
            case TARBRANCH:      iname = "branchyellow.gif";         break;
            case COMPRESSED:     iname = "squishdat.gif";            break;
            case DATA:           iname = "plaindat.gif";             break;
            case VOTABLE:        iname = "telescope2.gif";           break;
            case VOCOMPONENT:    iname = "smallbox1.gif";            break;
            case SPECFRAME:      iname = "axes81.gif";               break;
            case MYSPACE:        iname = "AGlogo.gif";               break;
            case HIERARCH:       iname = "browse.gif";               break;
            case SPLAT:          iname = "multidisplay.gif";         break;
            case SOG:            iname = "sogimage.gif";             break;
            case TOPCAT:         iname = "TopCat2.gif";              break;
            case HANDLE_EXPAND:  iname = "Tree.expandedIcon";        break;
            case HANDLE_COLLAPSE:iname = "Tree.collapsedIcon";       break;
            case TREE_LOGO:      iname = !festive ? "TREE27.gif" 
                                                  : "xtree5.gif";    break;
            case STARLINK_LOGO:  iname = "starlinklogo3.gif";        break;
            case HANDLE1:        iname = "handle1.gif";              break;
            case HANDLE2:        iname = "handle2.gif";              break;
            case STAR_LOGO:      iname = "starlinklogo1b.gif";       break;
            default:
                throw new IllegalArgumentException( 
                    "Unknown icon identifier (programming error)" );
        }
        return iname;
    }


    public static short getArrayIconID( int ndim ) {
        String iname;
        short id;
        switch ( ndim ) {
            case 0:
                id = ARY0;
                break;
            case 1:
                id = ARY1;
                break;
            case 2:
                id = ARY2;
                break;
            default:
                id = ARY3;
                break;
        }
        return id;
    }

    /**
     * Returns an icon given a name which can be either the name of a gif
     * in the icons directory or an attribute name known to the UIManager
     * (see javax.swing.plaf.basic.BasicLookAndFeel source code).
     */
    private static Icon getIconByName( String name ) {
        if ( ! iconMap.containsKey( name ) ) {
            Icon icon = null;
            if ( name.endsWith( ".gif" ) ) {
                InputStream istrm = IconFactory.class
                                   .getResourceAsStream( IMAGE_PATH + name );
                if ( istrm != null ) {
                    try {
                        istrm = new BufferedInputStream( istrm ); 
                        ByteArrayOutputStream ostrm = 
                            new ByteArrayOutputStream( 1024 );
                        int datum;
                        while ( ( datum = istrm.read() ) > -1 ) {
                            ostrm.write( datum );
                        }
                        icon = new ImageIcon( ostrm.toByteArray() );
                    }
                    catch ( IOException e ) {}
                }
            }
            else {
                icon = UIManager.getIcon( name );
            }

            /* If we haven't succeeded so far, use a default icon. */
            if ( icon == null ) {
                icon = getIconByName( "Tree.leafIcon" );
            }
            iconMap.put( name, icon );
        }
        return (Icon) iconMap.get( name );
    }

    /**
     * This entirely frivolous method determines whether the date is close
     * enough to Christmas to change tree icons into christmas tree icons.
     * The corresponding functionality is largely undocumented.
     */
    private static boolean isFestive() {
        Calendar now = Calendar.getInstance();
        int month = now.get( Calendar.MONTH );
        int day = now.get( Calendar.DAY_OF_MONTH );
        return ( month == Calendar.DECEMBER && day >= 21 )
            || ( month == Calendar.JANUARY && day <= 6 );
    }
}
