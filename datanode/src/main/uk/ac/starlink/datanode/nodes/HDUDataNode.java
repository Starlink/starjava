package uk.ac.starlink.datanode.nodes;

import javax.swing.JComponent;
import nom.tam.fits.Header;
import nom.tam.fits.AsciiTableHDU;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.RandomGroupsHDU;
import uk.ac.starlink.oldfits.FitsConstants;
import uk.ac.starlink.datanode.viewers.TextViewer;

/**
 * An implementation of the {@link DataNode} interface for 
 * representing a general Header and Data Unit (HDU) in FITS files.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class HDUDataNode extends DefaultDataNode {

    private String description;
    private Header header;
    private String hduType;
    private FITSDataNode.ArrayDataMaker hdudata;
    private int hduIndex;

    /**
     * Initialises a <code>HDUDataNode</code> from a <code>Header</code>
     * object.
     *
     * @param   hdr  a FITS header object from which the node is to be created
     * @param   hdudata  an object capable of returning the HDU data
     */
    public HDUDataNode( Header hdr, FITSDataNode.ArrayDataMaker hdudata ) 
            throws NoSuchDataException {
        this.header = hdr;
        this.hdudata = hdudata;
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
                if ( hdr.findKey( "NAXIS" ) != null &&
                     hdr.getIntValue( "NAXIS" ) > 0 ) {
                    hduType = "Image";
                }
                else {
                    hduType = "Header";
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
        setIconID( IconFactory.HDU );
    }

    public boolean allowsChildren() {
        return false;
    }

    public void configureDetail( DetailViewer dv ) {
        dv.addKeyedItem( "Number of header cards",
                         header.getNumberOfCards() );
        dv.addKeyedItem( "Blocks in header", header.getSize() / 2880 );
        dv.addKeyedItem( "Blocks of data", 
                         FitsConstants.getDataSize( header ) / 2880 );
        dv.addSeparator();
        dv.addKeyedItem( "HDU type", hduType );
        dv.addPane( "Header cards", new ComponentMaker() {
            public JComponent getComponent() {
                return new TextViewer( header.iterator() );
            }
        } );
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

    public String getPathSeparator() {
        return ".";
    }

    public String getPathElement() {
        return Integer.toString( hduIndex );
    }

    void setHDUIndex( int hduIndex ) {
        this.hduIndex = hduIndex;
    }

    public int getHDUIndex() {
        return hduIndex;
    }

    protected String getHduType() {
        return hduType;
    }

    protected Header getHeader() {
        return header;
    }

}
