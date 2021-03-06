package uk.ac.starlink.feather;

import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import uk.ac.bristol.star.feather.FeatherTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.formats.DocumentedIOHandler;
import uk.ac.starlink.table.formats.DocumentedTableBuilder;
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
public class FeatherTableBuilder extends DocumentedTableBuilder {

    public FeatherTableBuilder() {
        super( new String[] { "fea", "feather" } );
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

    public String getXmlDescription() {
        return String.join( "\n",
            "<p>The Feather file format is a column-oriented binary",
            "disk-based format based on Apache Arrow",
            "and supported by (at least) Python, R and Julia.",
            "Some description of it is available at",
            DocumentedIOHandler.toLink( "https://github.com/wesm/feather" ),
            "and",
            DocumentedIOHandler
           .toLink( "https://blog.rstudio.com/2016/03/29/feather/" ) + ".",
            "It can be used for large datasets, but it does not support",
            "array-valued columns.",
            "It can be a useful format to use for exchanging data with R,",
            "for which FITS I/O is reported to be slow.",
            "</p>",
            "<p>At present CATEGORY type columns are not supported,",
            "and metadata associated with TIME, DATE and TIMESTAMP",
            "columns is not retrieved.",
            "</p>",
        "" );
    }

    public boolean canStream() {
        return false;
    }

    public boolean docIncludesExample() {
        return false;
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
