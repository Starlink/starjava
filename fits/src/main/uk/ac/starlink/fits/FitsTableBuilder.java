package uk.ac.starlink.fits;

import java.io.IOException;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.TableHDU;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;

/**
 * Implementation of the <tt>TableBuilder</tt> interface which 
 * gets <tt>StarTable</tt>s from FITS files.
 *
 * @author   Mark Taylor (Starlink)
 */
public class FitsTableBuilder implements TableBuilder {

    public StarTable makeStarTable( DataSource datsrc ) throws IOException {

        /* Check if this looks like a FITS file. */
        byte[] buffer = new byte[ 80 ]; 
        datsrc.getMagic( buffer );        
        if ( ! FitsConstants.isMagic( buffer ) ) {
            return null;
        }

        try {

            /* Make a Fits object. */
            Fits fits;
            if ( datsrc instanceof FileDataSource &&
                 datsrc.getCompression() == Compression.NONE ) {
                fits = new Fits( ((FileDataSource) datsrc).getFile() );
            }
            else {
                fits = new Fits( datsrc.getInputStream() );
            }
    
            /* Go through the headers until we find a table HDU. */
            for ( BasicHDU hdu; ( hdu = fits.readHDU() ) != null; ) {
                if ( hdu instanceof TableHDU ) {

                    /* This step appears to be necessary, at least for
                     * AsciiTables, otherwise you get errors later. 
                     * I don't really understand the nom.tam.fits model
                     * of deferred data reading. */
                    hdu.getData().getData();

                    /* Make a table out of it. */
                    return new FitsStarTable( (TableHDU) hdu );
                }
            }
        }
        catch ( FitsException e ) {
            throw (IOException) new IOException().initCause( e );
        }

        /* No table HDU found. */
        return null;
    }
}
