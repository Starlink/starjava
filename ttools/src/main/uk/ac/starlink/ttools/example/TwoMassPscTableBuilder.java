package uk.ac.starlink.ttools.example;

import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.DataSource;

/**
 * TableBuilder implementation for the ASCII files distributed on the
 * 2MASS catalogue DVD set.  These files are also available from
 * <a href="ftp://ftp.ipac.caltech.edu/pub/2mass/allsky/"
 *         >ftp://ftp.ipac.caltech.edu/pub/2mass/allsky/</a>.
 *
 * @author   Mark Taylor
 * @since    12 Sep 2006
 */
public class TwoMassPscTableBuilder implements TableBuilder {

    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    public String getFormatName() {
        return "2mass-psc";
    }

    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy storagePolicy )
            throws IOException {
        URL schema = getClass().getResource( "twomass_psc_schema" );
        if ( schema == null ) {
            throw new IOException( "No schema found" );
        }
        else {
            return new PostgresAsciiStarTable( datsrc, schema );
        }
    }

    public void streamStarTable( InputStream in, TableSink sink,
                                 String pos ) {
        throw new UnsupportedOperationException();
    }
}
