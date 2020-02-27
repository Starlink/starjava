package uk.ac.starlink.feather;

import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import uk.ac.bristol.star.feather.FeatherTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.util.URLUtils;

/**
 * TableBuilder implementation for Feather format.
 *
 * @author   Mark Taylor
 * @since    26 Feb 2020
 */
public class FeatherTableBuilder implements TableBuilder {

    public FeatherTableBuilder() {
    }

    public String getFormatName() {
        return "feather";
    }

    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy storagePolicy )
            throws IOException {
        if ( ! FeatherTable.isMagic( datsrc.getIntro() ) ) {
            throw new TableFormatException( "No FEA1 magic number" );
        }
        File ffile = getFile( datsrc );
        if ( ffile != null && datsrc.getCompression() == Compression.NONE ) {
            return new FeatherStarTable( FeatherTable.fromFile( ffile ) );
        }
        else {
            throw new TableFormatException( "Only uncompressed files supported"
                                          + " for Feather" );
        }
    }

    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    public void streamStarTable( InputStream in, TableSink sink, String pos )
            throws IOException {
        throw new TableFormatException( "Can't stream from Feather format" );
    }

    /**
     * Returns a file corresponding to a DataSource, if possible.
     *
     * @param  datsrc  data source
     * @return   corresponding file, or null if it's not a file
     */
    private static File getFile( DataSource datsrc ) {
        if ( datsrc instanceof FileDataSource ) {
            return ((FileDataSource) datsrc).getFile();
        }
        else if ( datsrc instanceof URLDataSource ) {
            return URLUtils
                  .urlToFile( ((URLDataSource) datsrc).getURL().toString() );
        }
        else {
            return null;
        }
    }
}
