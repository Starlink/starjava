package uk.ac.starlink.treeview;

import javax.swing.Icon;
import javax.swing.JComponent;
import nom.tam.fits.AsciiTable;
import nom.tam.fits.AsciiTableHDU;
import nom.tam.fits.BinaryTable;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Data;
import nom.tam.fits.FitsException;
import nom.tam.fits.TableData;
import nom.tam.fits.TableHDU;
import nom.tam.fits.Header;
import nom.tam.util.ArrayDataInput;
import uk.ac.starlink.fits.FitsStarTable;
import uk.ac.starlink.table.ColumnHeader;
import uk.ac.starlink.table.StarTable;

/**
 * An implementation of the {@link DataNode} interface for 
 * representing binary or ASCII tables in FITS HDUs.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class TableHDUDataNode extends HDUDataNode {

    private String hduType;
    private TableData tdata;
    private TableHDU thdu;
    private String description;
    private Icon icon;
    private JComponent fullview;
    private Header header;

    /**
     * Initialises a <code>TableHDUDataNode</code> from an <code>Header</code>
     * object.  The stream is read to the end of the HDU.
     *
     * @param   hdr  a FITS header object from which the node is to be created
     * @param   istrm  the stream from which the data can be read.
     */
    public TableHDUDataNode( Header hdr, ArrayDataInput istrm ) 
            throws NoSuchDataException {
        super( hdr );
        this.header = hdr;
        String type;
        try {
            if ( BinaryTableHDU.isHeader( hdr ) ) {
                tdata = new BinaryTable( hdr );
                ((BinaryTable) tdata).read( istrm );
                thdu = new BinaryTableHDU( hdr, ((Data) tdata) );
                type = "Binary";
            }
            else if ( AsciiTableHDU.isHeader( hdr ) ) {
                tdata = new AsciiTable( hdr );
                ((AsciiTable) tdata).read( istrm );
                ((AsciiTable) tdata).getData();
                thdu = new AsciiTableHDU( hdr, ((Data) tdata) );
                type = "ASCII";
            }
            else {
                throw new NoSuchDataException( "Not a table" );
            }

            /* Get the column information from the header. */
            int ncols = hdr.getIntValue( "TFIELDS" );
            int nrows = hdr.getIntValue( "NAXIS2" );

            description = type + " table (" + ncols + "x" + nrows + ")";
        }
        catch ( FitsException e ) {
            throw new NoSuchDataException( e );
        }
    }

    public boolean allowsChildren() {
        return false;
    }

    public Icon getIcon() {
        if ( icon == null ) {
            icon = IconFactory.getInstance().getIcon( IconFactory.TABLE );
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

            final StarTable startable = new FitsStarTable( thdu );
            int nrows = startable.getNumRows();
            int ncols = startable.getNumColumns();
            dv.addKeyedItem( "Columns", ncols );
            dv.addKeyedItem( "Rows", nrows );
            dv.addSubHead( "Columns" );
            for ( int i = 0; i < ncols; i++ ) {
                ColumnHeader head = startable.getHeader( i );
                dv.addKeyedItem( "Column " + ( i + 1 ), head.getName() );
            }
            dv.addPane( "Header cards", new ComponentMaker() {
                public JComponent getComponent() {
                    return new TextViewer( header.iterator() );
                }
            } );
            dv.addPane( "Column details", new ComponentMaker() {
                public JComponent getComponent() {
                    MetamapGroup metagroup =
                        new StarTableMetamapGroup( startable );
                    return new MetaTable( metagroup );
                }
            } );
            dv.addPane( "Table data", new ComponentMaker() {
                public JComponent getComponent() {
                    return new TreeviewJTable( startable );
                }
            } );
        }
        return fullview;
    }

    public String getDescription() {
        return description;
    }

    public String getNodeTLA() {
        return "TBL";
    }

    public String getNodeType() {
        return "FITS Table HDU";
    }

}
