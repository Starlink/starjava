package uk.ac.starlink.treeview;

import java.util.*;
import java.io.*;
import javax.swing.*;
import javax.swing.tree.*;

/**
 * Returns {@link javax.swing.Icon}s for use in identifying objects.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class IconFactory {

    public static final String IMAGE_PATH = "uk/ac/starlink/treeview/images/";

    public static final short STOP = 1;
    public static final short EXIT = 2;
    public static final short HELP = 3;
    public static final short SPLIT_NONE = 11;
    public static final short SPLIT_BELOW = 12;
    public static final short SPLIT_BESIDE = 13;
    public static final short CASCADE = 21;
    public static final short EXCISE = 22;
    public static final short OPEN = 23;
    public static final short CLOSE = 24;
    public static final short ARY0 = 91;
    public static final short ARY1 = 92;
    public static final short ARY2 = 93;
    public static final short ARY3 = 94;
    public static final short LEAF = 101;
    public static final short PARENT = 102;
    public static final short SCALAR = 103;
    public static final short STRUCTURE = 104;
    public static final short FILE = 105;
    public static final short DIRECTORY = 106;
    public static final short NDF = 107;
    public static final short ZIPFILE = 108;
    public static final short ZIPENTRY = 109;
    public static final short WCS = 110;
    public static final short FITS = 111;
    public static final short HDU = 112;
    public static final short FRAME = 113;
    public static final short SKYFRAME = 114;
    public static final short HISTORY = 115;
    public static final short HISTORY_RECORD = 116;
    public static final short TABLE = 117;
    public static final short XML_DOCUMENT = 120;
    public static final short XML_ELEMENT = 121;
    public static final short XML_COMMENT = 122;
    public static final short XML_PI = 123;
    public static final short XML_CDATA = 124;
    public static final short XML_EREF = 125;
    public static final short XML_STRING = 126;
    public static final short NDX = 130;
    public static final short HDX_CONTAINER = 131;
    public static final short VOTABLE = 140;
    public static final short VOCOMPONENT = 141;
    public static final short SPLAT = 200;
    public static final short SOG = 201;
   
    private Map iconMap;
    private static IconFactory soleInstance = new IconFactory();

    private IconFactory() {
        iconMap = new HashMap();
    }

    public static IconFactory getInstance() {
        return soleInstance;
    }

    public Icon getIcon( short id ) {
        String iname;
        switch ( id ) {
            case SPLIT_BELOW:    iname = "SplitHorizontal.gif";      break;
            case SPLIT_BESIDE:   iname = "SplitVertical.gif";        break;
            case SPLIT_NONE:     iname = "Frame.gif";                break;
            case CASCADE:        iname = "PlusPlus.gif";             break;
            case EXCISE:         iname = "MinusMinus.gif";           break;
            case OPEN:           iname = "Plus.gif";                 break;
            case CLOSE:          iname = "Minus.gif";                break;
            case STOP:           iname = "Stop.gif";                 break;
            case EXIT:           iname = "exit1.gif";                break;
            case HELP:           iname = "Help2.gif";                break;
            case ARY0:           iname = "cell.gif";                 break;
            case ARY1:           iname = "row.gif";                  break;
            case ARY2:           iname = "Sheet.gif";                break;
            case ARY3:           iname = "Sheets.gif";               break;
            case LEAF:           iname = "defaultLeafIcon";          break;
            case PARENT:         iname = "defaultClosedIcon";        break;
            case SCALAR:         iname = "ball.black.gif";           break;
            case STRUCTURE:      iname = "closed.gif";               break;
            case FILE:           iname = "defaultLeafIcon";          break;
            case DIRECTORY:      iname = "defaultClosedIcon";        break;
            case NDF:            iname = "star_gold.gif";            break;
            case ZIPFILE:        iname = "squishdir2.gif";           break;
            case ZIPENTRY:       iname = "squishfile2.gif";          break;
            case WCS:            iname = "world1.gif";               break;
            case FITS:           iname = "star_bul.gif";             break;
            case HDU:            iname = "TileCascade.gif";          break;
            case FRAME:          iname = "axes42.gif";               break;
            case SKYFRAME:       iname = "axes62.gif";               break;
            case HISTORY:        iname = "book2.gif";                break;
            case HISTORY_RECORD: iname = "mini-doc.gif";             break;
            case TABLE:          iname = "table10.gif";              break;
            case XML_DOCUMENT:   iname = "xml_doc.gif";              break;
            case XML_ELEMENT:    iname = "xml_el.gif";               break;
            case XML_COMMENT:    iname = "xml_comm.gif";             break;
            case XML_PI:         iname = "xml_pi.gif";               break;
            case XML_CDATA:      iname = "xml_txt.gif";              break;
            case XML_EREF:       iname = "xml_eref.gif";             break;
            case XML_STRING:     iname = "xml_txt.gif";              break;
            case NDX:            iname = "jsky2.gif";                break;
            case HDX_CONTAINER:  iname = "box7.gif";                 break;
            case VOTABLE:        iname = "telescope2.gif";           break;
            case VOCOMPONENT:    iname = "smallbox1.gif";            break;
            case SPLAT:          iname = "multidisplay.gif";         break;
            case SOG:            iname = "sogimage.gif";             break;
            default:
                throw new Error( 
                    "Unknown icon identifier (programming error)" );
        }
        return getIconByName( iname );
    }

    public Icon getArrayIcon( int ndim ) {
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
        return getIcon( id );
    }

    private Icon getIconByName( String name ) {
        if ( ! iconMap.containsKey( name ) ) {
            Icon icon = null;
            if ( name.equals( "defaultLeafIcon" ) ) {
                icon = new DefaultTreeCellRenderer().getDefaultLeafIcon();
            }
            else if ( name.equals( "defaultClosedIcon" ) ) {
                icon = new DefaultTreeCellRenderer().getDefaultClosedIcon();
            }
            else if ( name.equals( "defaultOpenIcon" ) ) {
                icon = new DefaultTreeCellRenderer().getDefaultOpenIcon();
            }
            else {
                String iname = IMAGE_PATH + name;
                InputStream istrm = this.getClass().getClassLoader()
                                   .getResourceAsStream( iname );
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

            /* If we haven't succeeded so far, use a default icon. */
            if ( icon == null ) {
                icon = getIconByName( "defaultLeafIcon" );
            }
            iconMap.put( name, icon );
        }
        return (Icon) iconMap.get( name );
    }
}
