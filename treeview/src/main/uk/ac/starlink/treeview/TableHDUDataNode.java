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
    private FitsTableColumn[] columns;

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

            columns = new FitsTableColumn[ ncols ];
            for ( int i = 1; i <= ncols; i++ ) {
                FitsTableColumn col = new FitsTableColumn();
                columns[ i - 1 ] = col;
                col.setFormat( hdr.getStringValue( "TFORM" + i ) );
                col.setType( hdr.getStringValue( "TTYPE" + i ) );
                col.setUnit( hdr.getStringValue( "TUNIT" + i ) );
                if ( hdr.containsKey( "TNULL" + i ) ) {
                    col.setBlank( 
                        new Integer( hdr.getIntValue( "TNULL" + i ) ) );
                }
                col.setScale( hdr.getDoubleValue( "TSCALE" + i, 1.0 ) );
                col.setZero( hdr.getDoubleValue( "TZERO" + i, 0.0 ) );
                col.setDisp( hdr.getStringValue( "TDISP" + i ) );
            }
            
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
            int nrows = tdata.getNRows();
            int ncols = tdata.getNCols();
            dv.addKeyedItem( "Columns", ncols );
            dv.addKeyedItem( "Rows", nrows );
            dv.addSubHead( "Columns" );
            for ( int i = 0; i < ncols; i++ ) {
                String fmt;
                try {
                    fmt = thdu.getColumnFormat( i );
                }
                catch ( FitsException e ) {
                    fmt = e.getMessage();
                }
                dv.addKeyedItem( "Column " + ( i + 1 ), columns[ i ] );
            }
            dv.addPane( "Header cards", new ComponentMaker() {
                public JComponent getComponent() {
                    return new TextViewer( header.iterator() );
                }
            } );
            dv.addPane( "Table view", new ComponentMaker() {
                public JComponent getComponent() {
                    return new TableBrowser( thdu, columns );
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
