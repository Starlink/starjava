package uk.ac.starlink.parquet;

import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.apache.parquet.io.InputFile;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.formats.DocumentedTableBuilder;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.IOSupplier;
import uk.ac.starlink.util.URLUtils;

/**
 * TableBuilder for parquet files.
 *
 * @author   Mark Taylor
 * @since    25 Feb 2021
 */
public class ParquetTableBuilder extends DocumentedTableBuilder {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.parquet" );

    /* Log4j is annoying. */
    static {
        ParquetUtil.silenceLog4j();
    }

    /**
     * Constructor.
     */
    public ParquetTableBuilder() {
        super( new String[] { "parquet", "parq" } );
    }

    public String getFormatName() {
        return "parquet";
    }

    /**
     * Returns false; parquet metadata is in the footer.
     */
    public boolean canStream() {
        return false;
    }

    public boolean docIncludesExample() {
        return false;
    }

    public String getXmlDescription() {
        return String.join( "\n",
            "<p>Parquet is a columnar format developed within the Apache",
            "project.",
            "Data is compressed on disk and read into memory before use.",
            "</p>",
            "<p>This input handler will read columns representing",
            "scalars, strings and one-dimensional arrays of the same.",
            "It is not capable of reading multi-dimensional arrays,",
            "more complex nested data structures,",
            "or some more exotic data types like 96-bit integers.",
            "If such columns are encountered in an input file,",
            "a warning will be emitted through the logging system",
            "and the column will not appear in the read table.",
            "Support may be introduced for some additional types",
            "if there is demand.",
            "</p>",
            "<p>At present, only very limited metadata is read.",
            "Parquet does not seem(?) to have any standard format for",
            "per-column metadata, so the only information read about",
            "each column apart from its datatype is its name.",
            "</p>",
            ""
        );
    }

    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy storage ) throws IOException {
        if ( ! ParquetUtil.isMagic( datsrc.getIntro() ) ) {
            throw new TableFormatException( "Not parquet format"
                                          + " (no leading magic number)" );
        }
        IOSupplier<ParquetFileReader> pfrSupplier = () -> {
            try {
                return ParquetFileReader.open( createInputFile( datsrc ) );
            }

            /* The ParquetFileReader sometimes generates RuntimeExceptions
             * (e.g. for absent trailing magic number), so catch and rethrow
             * here. */
            catch ( RuntimeException e ) {
                throw new TableFormatException( "Trouble opening "
                                              + datsrc.getName() 
                                              + " as parquet", e );
            }
        };
        if ( wantRandom ) {
            try {
                return new CachedParquetStarTable( pfrSupplier, -1 );
            }
            catch ( IOException e ) {
                logger_.log( Level.WARNING,
                             "Cached read failed for " + datsrc, e );
            }
        }
        return new SequentialParquetStarTable( pfrSupplier );
    }

    public void streamStarTable( InputStream istrm, TableSink sink,
                                 String pos ) throws TableFormatException {
        throw new TableFormatException( "Can't stream parquet" );
    }

    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    /**
     * Tries to turn a datasource into a Hadoop input file.
     *
     * @return   input file, not null
     * @throws   IOException  if it can't be done
     */
    private static InputFile createInputFile( DataSource datsrc )
            throws IOException {
        Path path = null;

        /* A file will work if we can get one. */
        File file = getFile( datsrc );
        URL url = datsrc.getURL();
        if ( file != null ) {
            path = new Path( file.getPath() );
        }

        /* A URL might work, who knows? */
        else if ( url != null ) {
            path = new Path( url.toString() );
        }
        else {
            throw new IOException( "Can't turn " + datsrc.getClass().getName()
                                 + " " + datsrc + " into input file" );
        }
        return HadoopInputFile.fromPath( path, new Configuration() );
    }

    /**
     * Try to turn a data source into a file.
     * Since non-file URLs are not in general permissible for parquet,
     * we try to extract the file if at all possible.
     *
     * @param  datsrc  datasource
     * @return   file, or null
     */
    private static File getFile( DataSource datsrc ) {
        if ( datsrc instanceof FileDataSource ) {
            return ((FileDataSource) datsrc).getFile();
        }
        URL url = datsrc.getURL();
        return url == null ? null : URLUtils.urlToFile( url.toString() );
    }
}
