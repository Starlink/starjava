package uk.ac.starlink.treeview;

import java.io.IOException;
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
import uk.ac.starlink.fits.FitsConstants;
import uk.ac.starlink.fits.FitsStarTable;
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
    private String description;
    private JComponent fullview;
    private Header header;
    private FITSDataNode.ArrayDataMaker hdudata;

    /**
     * Initialises a <code>TableHDUDataNode</code> from an <code>Header</code>
     * object.  The stream is read to the end of the HDU.
     *
     * @param   header a FITS header object from which the node is to be created
     * @param   istrm  the stream from which the data can be read.
     */
    public TableHDUDataNode( Header header, 
                             FITSDataNode.ArrayDataMaker hdudata ) 
            throws NoSuchDataException {
        super( header, hdudata );
        this.header = header;
        this.hdudata = hdudata;
        hduType = getHduType();
        String type;
        try {
            if ( BinaryTableHDU.isHeader( header ) ) {
                tdata = new BinaryTable( header );
                type = "Binary";
            }
            else if ( AsciiTableHDU.isHeader( header ) ) {
                tdata = new AsciiTable( header );
                type = "ASCII";
            }
            else {
                throw new NoSuchDataException( "Not a table" );
            }

            /* Get the column information from the header. */
            int ncols = header.getIntValue( "TFIELDS" );
            int nrows = header.getIntValue( "NAXIS2" );

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
        return IconFactory.getIcon( IconFactory.TABLE );
    }

    public boolean hasFullView() {
        return true;
    }

    public JComponent getFullView() {
        if ( fullview == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullview = dv.getComponent();
            dv.addSeparator();
            dv.addKeyedItem( "Number of header cards",
                             header.getNumberOfCards() );
            dv.addKeyedItem( "Blocks in header", header.getSize() / 2880 );
            dv.addKeyedItem( "Blocks of data", 
                             FitsConstants.getDataSize( header ) / 2880 );
            dv.addSeparator();
            dv.addKeyedItem( "HDU type", hduType );

            /* Make the table. */
            try {
                ArrayDataInput istrm = hdudata.getArrayData();

                /* Skip the header. */
                Header hdr = new Header( istrm );

                /* Read the table data. */
                TableHDU thdu;
                if ( tdata instanceof BinaryTable ) {
                    ((BinaryTable) tdata).read( istrm );
                    thdu = new BinaryTableHDU( header, (Data) tdata );
                }
                else if ( tdata instanceof AsciiTable ) {
                    ((AsciiTable) tdata).read( istrm );
                    ((AsciiTable) tdata).getData();
                    thdu = new AsciiTableHDU( header, (Data) tdata );
                }
                else {
                    throw new AssertionError();
                }

                /* Show the header cards. */
                dv.addPane( "Header cards", new ComponentMaker() {
                    public JComponent getComponent() {
                        return new TextViewer( header.iterator() );
                    }
                } );

                /* Make a StarTable out of it, and do table-specific display. */
                final StarTable startable = new FitsStarTable( thdu );
                StarTableDataNode.addDataViews( dv, startable );
            }
            catch ( final FitsException e ) {
                dv.addPane( "Error reading table", new ComponentMaker() {
                     public JComponent getComponent() {
                         return new TextViewer( e );
                     }
                } );
            }
            catch ( final IOException e ) {
                dv.addPane( "Error reading table", new ComponentMaker() {
                     public JComponent getComponent() {
                         return new TextViewer( e );
                     }
                } );
            }
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
