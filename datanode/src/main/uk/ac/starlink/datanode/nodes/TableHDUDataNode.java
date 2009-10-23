package uk.ac.starlink.datanode.nodes;

import java.io.IOException;
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
import nom.tam.util.RandomAccess;
import uk.ac.starlink.fits.BintableStarTable;
import uk.ac.starlink.fits.FitsConstants;
import uk.ac.starlink.fits.FitsStarTable;
import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.IOUtils;

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
    private Header header;
    private FITSDataNode.ArrayDataMaker hdudata;
    private StarTable starTable;

    /**
     * Initialises a <code>TableHDUDataNode</code> from an <code>Header</code>
     * object.  The stream is read to the end of the HDU.
     *
     * @param   header a FITS header object from which the node is to be created
     * @param   hdudata  object which can supply the data stream where
     *          the data resides
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
        setIconID( IconFactory.TABLE );
    }

    /**
     * Returns the StarTable containing the data.  Its construction,
     * which involves reading from the stream, is deferred until 
     * necessary. 
     * 
     * @return   the StarTable object containing the data for this HDU
     */
    public synchronized StarTable getStarTable() throws IOException {

        /* If we haven't got it yet, read data and build a StarTable. */
        if ( starTable == null ) {
            try {
                ArrayDataInput istrm = hdudata.getArrayData();
                DataSource datsrc = hdudata.getDataSource();
                long offset = hdudata.getOffset();
                starTable = attemptReadTable( istrm, datsrc,
                                              new long[] { offset } );
            }
            catch ( FitsException e ) {
                throw (IOException) new IOException( e.getMessage() ) 
                                   .initCause( e );
            }
        }
        return starTable;
    }

    public boolean isStarTable() {
        return true;
    }

    public boolean allowsChildren() {
        return false;
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

    public boolean hasDataObject( DataType dtype ) {
        if ( dtype == DataType.TABLE ) {
            return true;
        }
        else {
            return super.hasDataObject( dtype );
        }
    }

    public Object getDataObject( DataType dtype ) throws DataObjectException {
        if ( dtype == DataType.TABLE ) {
            try {
                return getStarTable();
            }
            catch ( IOException e ) {
                throw new DataObjectException( e );
            }
        }
        else {
            return super.getDataObject( dtype );
        }
    }

    /**
     * Reads the next header, and if it represents a table HDU, makes a
     * StarTable out of it and returns.  If it is some other kind of HDU, 
     * <tt>null</tt> is returned.  In either case, the stream is advanced
     * the end of that HDU.
     *
     * NOTE: This code has been copied from a slightly earlier version of
     * uk.ac.starlink.fits.FitsTableBuilder.attemptReadTable.
     * That code was rationalised, and that stopped this class from working
     * properly (attempting to access table data usually resulted in 
     * MappedFile.seek() out of bounds errors).  
     * I haven't looked into it in full detail, but probably
     * this class was relying for its behaviour on undocumented misfeatures
     * of the implementation below.  Since the datanode package is 
     * unsupported at time of writing, a proper fix is not on the cards.
     * Copying the 'working' code here so that existing behaviour continues
     * correctly is therefore the best option.  - MBT 23 Oct 2009
     * 
     * @param  strm  stream to read from, positioned at the start of an HDU
     *         (before the header)
     * @param  datsrc  a DataSource which can supply the data 
     *         in <tt>strm</tt>
     * @param  pos  a 1-element array holding the position in <tt>datsrc</tt>
     *         at which <tt>strm</tt> is positioned -
     *         it's an array so it can be updated by this routine (sorry)
     * @return   a StarTable made from the HDU at the start of <tt>strm</tt>
     *           or null
     */
    private static StarTable attemptReadTable( ArrayDataInput strm,
                                               DataSource datsrc, long[] pos )
        throws FitsException, IOException {

        /* Read the header. */
        Header hdr = new Header();
        int headsize = FitsConstants.readHeader( hdr, strm );
        long datasize = FitsConstants.getDataSize( hdr );
        long datpos = pos[ 0 ] + headsize;
        pos[ 0 ] += headsize + datasize;
        String xtension = hdr.getStringValue( "XTENSION" );
          
        /* If it's a BINTABLE HDU, make a BintableStarTable out of it. */ 
        if ( "BINTABLE".equals( xtension ) ) {
            if ( strm instanceof RandomAccess ) {
                return BintableStarTable
                      .makeRandomStarTable( hdr, (RandomAccess) strm );
            }
            else {
                return BintableStarTable
                      .makeSequentialStarTable( hdr, datsrc, datpos );
            }

            // BinaryTable tdata = new BinaryTable( hdr );
            // tdata.read( strm );
            // TableHDU thdu = new BinaryTableHDU( hdr, (Data) tdata );
            // return new FitsStarTable( thdu );
        }

        /* If it's a TABLE HDU (ASCII table), make a FitsStarTable. */
        else if ( "TABLE".equals( xtension ) ) {
            AsciiTable tdata = new AsciiTable( hdr );
            tdata.read( strm );
            tdata.getData();
            TableHDU thdu = new AsciiTableHDU( hdr, (Data) tdata );
            return new FitsStarTable( thdu );
        }

        /* It's not a table HDU - just skip over it and return null. */
        else {
            IOUtils.skipBytes( strm, datasize );
            return null;
        }
    }
}
