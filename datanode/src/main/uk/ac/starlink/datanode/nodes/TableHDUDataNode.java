package uk.ac.starlink.datanode.nodes;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import nom.tam.fits.AsciiTableHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.util.ArrayDataInput;
import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.fits.WideFits;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.DataSource;

/**
 * An implementation of the {@link DataNode} interface for 
 * representing binary or ASCII tables in FITS HDUs.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class TableHDUDataNode extends HDUDataNode {

    private String hduType;
    private String description;
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
        this.hdudata = hdudata;
        hduType = getHduType();
        final String type;
        if ( BinaryTableHDU.isHeader( header ) ) {
            type = "Binary";
        }
        else if ( AsciiTableHDU.isHeader( header ) ) {
            type = "ASCII";
        }
        else {
            throw new NoSuchDataException( "Not a table" );
        }

        /* Get the column information from the header. */
        int ncols = header.getIntValue( "TFIELDS" );
        int nrows = header.getIntValue( "NAXIS2" );
        description = type + " table (" + ncols + "x" + nrows + ")";
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
     * <code>null</code> is returned.  In either case, the stream is advanced
     * the end of that HDU.
     * 
     * @param  strm  stream to read from, positioned at the start of an HDU
     *         (before the header)
     * @param  datsrc  a DataSource which can supply the data 
     *         in <code>strm</code>
     * @param  pos  a 1-element array holding the position in
     *         <code>datsrc</code> at which <code>strm</code> is positioned -
     *         it's an array so it can be updated by this routine (sorry)
     * @return   a StarTable made from the HDU at the start of <code>strm</code>
     *           or null
     */
    private static StarTable attemptReadTable( final ArrayDataInput strm,
                                               DataSource datsrc, long[] pos )
            throws FitsException, IOException {
        InputStream in = new InputStream() {
            public int read() throws IOException {
                try {
                    return strm.readUnsignedByte();
                }
                catch ( EOFException e ) {
                    return -1;
                }
            }
            @Override
            public int read( byte[] buf ) throws IOException {
                return strm.read( buf );
            }
            @Override
            public int read( byte[] buf, int offset, int size )
                    throws IOException {
                return strm.read( buf, offset, size );
            }
            @Override
            public long skip( long n ) throws IOException {
                return strm.skip( n );
            }
            @Override
            public void close() throws IOException {
                strm.close();
            }
        };
        return FitsTableBuilder
              .attemptReadTable( in, false, datsrc, WideFits.DEFAULT,
                                 pos, (StoragePolicy) null );
    }
}
