package uk.ac.starlink.fits;

import java.awt.datatransfer.DataFlavor;
import java.io.EOFException;
import java.io.IOException;
import nom.tam.fits.AsciiTable;
import nom.tam.fits.AsciiTableHDU;
import nom.tam.fits.BinaryTable;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Data;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.TableHDU;
import nom.tam.util.ArrayDataInput;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;

/**
 * Implementation of the <tt>TableBuilder</tt> interface which 
 * gets <tt>StarTable</tt>s from FITS files.
 * The position attribute of the <tt>DataSource</tt> can be used to indicate
 * the HDU at which the table is located in the FITS file described
 * by 
 *
 * @author   Mark Taylor (Starlink)
 */
public class FitsTableBuilder implements TableBuilder {

    /**
     * Creates a StarTable from a DataSource which refers to a FITS
     * file or stream.  If the source has a position attribute, it
     * will be interpreted as an integer indicating which HDU the
     * table is in.  The first HDU is number 0 (though being a primary
     * HDU this one can't hold a table).  If there is no position,
     * the first HDU which does hold a table is used.
     *
     * @param  datsrc  the source of the fits table data
     * @return  a new StarTable based on <tt>datsrc</tt>
     */
    public StarTable makeStarTable( DataSource datsrc ) throws IOException {

        /* Check if this looks like a FITS file. */
        if ( ! FitsConstants.isMagic( datsrc.getIntro() ) ) {
            return null;
        }

        ArrayDataInput strm = null;
        try {

            /* Get a FITS data stream. */
            strm = FitsConstants.getInputStream( datsrc );

            /* If an HDU was specified explicitly, try to pick up that one
             * as a table. */
            if ( datsrc.getPosition() != null ) {
                TableHDU thdu;
                try {
                    thdu = attemptGetTableHDU( strm );
                }
                catch ( EOFException e ) {
                    throw new IOException( "Fell off end of file looking for "
                                         + datsrc );
                }
                if ( thdu != null ) {
                    return makeTable( thdu );
                }
                else {
                    throw new IOException( datsrc + " not a Table HDU" );
                }
            }

            /* Otherwise starting from where we are, find the first
             * table HDU. */
            else {
                try {
                    while ( true ) {
                        TableHDU thdu = attemptGetTableHDU( strm );
                        if ( thdu != null ) {
                            return makeTable( thdu );
                        }
                    }
                }
                catch ( EOFException e ) {
                    throw new IOException( "No table HDUs in " + datsrc );
                }
            }
        }
        catch ( FitsException e ) {
            throw (IOException) new IOException().initCause( e );
        }
        finally {
            if ( strm != null ) {
                strm.close();
            }
        }
    }

    /**
     * Returns <tt>true</tt> for a flavor with the MIME type "application/fits".
     */
    public boolean canImport( DataFlavor flavor ) {
        if ( flavor.getPrimaryType().equals( "application" ) &&
             flavor.getSubType().equals( "fits" ) ) {
            return true;
        }
        return false;
    }

    /**
     * Reads the next header, and if it represents a Table HDU, returns
     * the HDU.  If it is some other kind of HDU null is returned.
     * In either case, the stream is advanced to the end of that HDU.
     */
    private static TableHDU attemptGetTableHDU( ArrayDataInput strm )
            throws FitsException, IOException {

        /* Read the header. */
        Header hdr = new Header();
        FitsConstants.readHeader( hdr, strm ); 

        /* If it's an ascii or binary table, read the data and return. */
        if ( hdr.containsKey( "XTENSION" ) ) {
            if ( AsciiTableHDU.isHeader( hdr ) ) {
                AsciiTable tdata = new AsciiTable( hdr );
                tdata.read( strm );
                tdata.getData();
                return new AsciiTableHDU( hdr, (Data) tdata );
            }
            else if ( BinaryTableHDU.isHeader( hdr ) ) {
                BinaryTable tdata = new BinaryTable( hdr );
                tdata.read( strm );
                return new BinaryTableHDU( hdr, (Data) tdata );
            }
        }

        /* Not a table - just skip ahead and return null. */
        for ( long skipBytes = FitsConstants.getDataSize( hdr );
              skipBytes > 0; ) {
             skipBytes -= strm.skip( skipBytes );
        }
        return null;
    }

    /**
     * Makes a StarTable from a TableHDU.
     */
    private static StarTable makeTable( TableHDU tabhdu )
            throws FitsException, IOException {

        /* Make a table out of it. */
        return new FitsStarTable( (TableHDU) tabhdu );
    }
}
