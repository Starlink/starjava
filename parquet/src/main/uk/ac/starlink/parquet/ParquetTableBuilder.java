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
import uk.ac.starlink.table.storage.AdaptiveByteStore;
import uk.ac.starlink.table.storage.MonitorStoragePolicy;
import uk.ac.starlink.util.ConfigMethod;
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

    private Boolean cacheCols_;
    private int nThread_;

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
        if ( useCache( datsrc, wantRandom, storage ) ) {
            int nThread = nThread_ > 0
                        ? nThread_
                        : CachedParquetStarTable.getDefaultThreadCount();
            logger_.info( "Caching parquet column data for " + datsrc
                        + " with " + nThread + " threads" );
            try {
                return new CachedParquetStarTable( pfrSupplier, nThread );
            }
            catch ( IOException e ) {
                logger_.log( Level.WARNING,
                             "Cached read failed for " + datsrc, e );
            }
        }
        logger_.info( "No parquet column caching for " + datsrc );
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
     * Determines policy for table construction.
     * If true, the {@link #makeStarTable makeStarTable} method returns a
     * {@link CachedParquetStarTable} and if false a
     * {@link SequentialParquetStarTable}.
     * If null, the decision is made automatically on the basis of
     * whether it looks like random access is required and file size etc.
     * 
     * @param   cacheCols  column data read policy
     */
    @ConfigMethod(
        property = "cachecols",
        usage = "true|false|null",
        example = "true",
        doc = "<p>Forces whether to read all the column data at table load\n"
            + "time.  If <code>true</code>, then when the table is loaded,\n"
            + "all data is read by column into local scratch disk files,\n"
            + "which is generally the fastest way to ingest all the data.\n"
            + "If <code>false</code>, the table rows are read as required,\n"
            + "and possibly cached using the normal STIL mechanisms.\n"
            + "If <code>null</code> (the default), the decision is taken\n"
            + "automatically based on available information.\n"
            + "</p>"
    )
    public void setCacheCols( Boolean cacheCols ) {
        cacheCols_ = cacheCols;
    }

    /**
     * Sets the number of read threads to use when caching column data.
     * This is the value passed to the {@link CachedParquetStarTable}
     * constructor, and ignored when constructing a
     * {@link SequentialParquetStarTable}.
     *
     * @param  nThread  read thread count, or &lt;=0 for auto
     */
    @ConfigMethod(
        property = "nThread",
        usage = "<int>",
        example = "4",
        doc = "<p>Sets the number of read threads used for concurrently\n"
            + "reading table columns if the columns are cached at load time\n"
            + "- see the <code>cachecols</code> option.\n"
            + "If the value is &lt;=0 (the default), a value is chosen\n"
            + "based on the number of apparently available processors.\n"
            + "</p>"
    )
    public void setReadThreadCount( int nThread ) {
        nThread_ = nThread;
    }

    /**
     * Returns the number of read threads to use when caching column data.
     *
     * @return   read thread count, or &lt;=0 for auto
     */
    public int getReadThreadCount() {
        return nThread_;
    }

    /**
     * Determines whether to cache column data on table read.
     * If the {@link #setCacheCols} has been called that determines the result,
     * otherwise some heuristics based on available information are used.
     *
     * @param  datsrc  table data source
     * @param  wantRandom   whether a random-access table is requested
     * @param  storage  storage policy
     * @return   true for cached table, false for sequential table
     */
    private boolean useCache( DataSource datsrc, boolean wantRandom,
                              StoragePolicy storage ) {
        if ( cacheCols_ != null ) {
            return cacheCols_.booleanValue();
        }
        else if ( wantRandom ) {

            /* Testing identity of storage policy like this is hacky
             * and not robust. */
            while ( storage instanceof MonitorStoragePolicy ) {
                storage = ((MonitorStoragePolicy) storage).getBasePolicy();
            }
            if ( StoragePolicy.PREFER_MEMORY.equals( storage ) ) {
                return false;
            }
            if ( StoragePolicy.PREFER_DISK.equals( storage ) ) {
                return true;
            }
            if ( StoragePolicy.ADAPTIVE.equals( storage ) ) {
                if ( datsrc instanceof FileDataSource ) {
                    long len = ((FileDataSource) datsrc).getFile().length();
                    return len > 0.5 * AdaptiveByteStore.getDefaultLimit();
                }
            }
            return false;
        }
        else {
            return false;
        }
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
