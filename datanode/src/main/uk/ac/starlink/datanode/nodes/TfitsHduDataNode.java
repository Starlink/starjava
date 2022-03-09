package uk.ac.starlink.datanode.nodes;

import java.util.Arrays;
import javax.swing.JComponent;
import uk.ac.starlink.datanode.viewers.TextViewer;
import uk.ac.starlink.fits.FitsHeader;
import uk.ac.starlink.fits.HeaderValueException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableFormatException;

/**
 * DataNode class for representing FITS HDUs which does not use nom.tam.fits.
 * It understands BINABLE and TABLE extensions, but not array-type HDUs.
 */
public class TfitsHduDataNode extends DefaultDataNode {

    private final FitsHeader hdr;
    private final int ihdu;
    private final StarTable table;

    /**
     * Constructor.
     *
     * @param  hdr  FITS header
     * @param  ihdu   HDU index
     * @param  table   table object, or null for a non-table HDU
     */
    public TfitsHduDataNode( FitsHeader hdr, int ihdu, StarTable table ) {
        this.hdr = hdr;
        this.ihdu = ihdu;
        this.table = table;
        setIconID( table == null ? IconFactory.HDU : IconFactory.TABLE );
        setLabel( ihdu == 0 ? "Primary HDU" : "HDU " + ihdu );
    }

    public boolean allowsChildren() {
        return false;
    }

    public void configureDetail( DetailViewer dv ) {
        long hdrSize = hdr.getHeaderBlockCount();
        dv.addKeyedItem( "Number of header cards", hdr.getCards().length );
        if ( hdrSize >= 0 ) {
            dv.addKeyedItem( "Blocks in header", hdrSize );
        }
        try {
            long datSize = hdr.getDataBlockCount();
            if ( datSize >= 0 ) {
                dv.addKeyedItem( "Blocks of data", datSize );
            }
        }
        catch ( HeaderValueException e ) {
            // no action
        }
        dv.addPane( "Header cards", new ComponentMaker() {
            public JComponent getComponent() {
                return new TextViewer( Arrays.asList( hdr.getCards() )
                                             .iterator() );
            }
        } );
    }

    public String getDescription() {
        return table == null ? "FITS HDU"
                             : "FITS Table HDU ("
                             + table.getColumnCount() + "x"
                             + table.getRowCount() + ")";
    }

    public String getNodeTLA() {
        return table == null ? "HDU" : "TBL";
    }

    public String getNodeType() {
        return table == null ? "FITS HDU" : "FITS Table HDU";
    }

    public String getPathSeparator() {
        return ".";
    }

    public String getPathElement() {
        return Integer.toString( ihdu );
    }

    public StarTable getStarTable() {
        return table;
    }

    public boolean isStarTable() {
        return table != null;
    }

    public boolean hasDataObject( DataType dtype ) {
        if ( dtype == DataType.TABLE ) {
            return table != null;
        }
        else {
            return super.hasDataObject( dtype );
        }
    }

    public Object getDataObject( DataType dtype ) throws DataObjectException {
        return dtype == DataType.TABLE
             ? table
             : super.getDataObject( dtype );
    }

    protected FitsHeader getHeader() {
        return hdr;
    }
}
