package uk.ac.starlink.treeview;

import javax.swing.Icon;
import javax.swing.JComponent;
import nom.tam.fits.Header;
import nom.tam.fits.AsciiTableHDU;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.RandomGroupsHDU;

/**
 * An implementation of the {@link DataNode} interface for 
 * representing a general Header and Data Unit (HDU) in FITS files.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class HDUDataNode extends DefaultDataNode {

    private Icon icon;
    private JComponent fullview;
    private String description;
    private Header header;
    private String hduType;

    /**
     * Initialises a <code>HDUDataNode</code> from a <code>Header</code>
     * object.
     *
     * @param   hdr  a FITS header object
     *               from which the node is to be created.
     */
    public HDUDataNode( Header hdr ) throws NoSuchDataException {
        this.header = hdr;
        hduType = null;
        if ( hduType == null ) {
            try {
                if ( AsciiTableHDU.isHeader( hdr ) ) {
                    hduType = "ASCII table";
                }
            }
            catch ( Exception e ) {
                // known NullPointerException crops up here
            }
        }
        if ( hduType == null ) {
            if ( BinaryTableHDU.isHeader( hdr ) ) {
                hduType = "Binary table";
            }
        }
        if ( hduType == null ) {
            if ( RandomGroupsHDU.isHeader( hdr ) ) {
                hduType = "Random Groups";
            }
        }   
        if ( hduType == null ) {
            if ( ImageHDU.isHeader( hdr ) ) {
                if ( hdr.findKey( "NAXIS" ) != null ) {
                    hduType = "Image";
                }
                else {
                    hduType = "primary";
                }
            }
        }       
        if ( hduType == null ) {
            if ( BasicHDU.isHeader( hdr ) ) {
                hduType = "Basic HDU";
            }
        }
        if ( hduType == null ) {
            hduType = "???";
        }

        description = "(" + hduType + ")";
    }

    public boolean allowsChildren() {
        return false;
    }

    public Icon getIcon() {
        if ( icon == null ) {
            icon = IconFactory.getInstance().getIcon( IconFactory.HDU );
        }
        return icon;
    }

    public boolean hasFullView() {
        return true;
    }

    public JComponent getFullView() {
        if ( fullview == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullview = dv.getComponent();
            dv.addSeparator();
            dv.addKeyedItem( "HDU type", hduType );
            dv.addKeyedItem( "Number of header cards",
                             header.getNumberOfCards() );
            dv.addPane( "Header cards", new ComponentMaker() {
                public JComponent getComponent() {
                    return new TextViewer( header.iterator() );
                }
            } );
        }
        return fullview;
    }

    public String getDescription() {
        return description;
    }

    public String getNodeTLA() {
        return "HDU";
    }

    public String getNodeType() {
        return "FITS header+data unit";
    }

    protected String getHduType() {
        return hduType;
    }

    protected Header getHeader() {
        return header;
    }

}
