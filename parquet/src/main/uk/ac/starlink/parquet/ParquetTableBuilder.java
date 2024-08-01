package uk.ac.starlink.parquet;

import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.io.InputStream;
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

/**
 * TableBuilder for parquet files.
 *
 * @author   Mark Taylor
 * @since    25 Feb 2021
 */
public class ParquetTableBuilder extends DocumentedTableBuilder {

    private Boolean cacheCols_;
    private int nThread_;
    private boolean tryUrl_;

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
            "<p>Depending on the way that the table is accessed,",
            "the reader tries to take advantage of the column and",
            "row block structure of parquet files to read the data",
            "in parallel where possible.",
            "</p>",
            readText( "parquet-packaging.xml" ),
            ""
        );
    }

    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy storage ) throws IOException {
        if ( ! ParquetUtil.isMagic( datsrc.getIntro() ) ) {
            throw new TableFormatException( "Not parquet format"
                                          + " (no leading magic number)" );
        }
        else {
            ParquetIO io = ParquetUtil.getIO();
            boolean useCache = useCache( datsrc, wantRandom, storage );
            return io.readParquet( datsrc, this, useCache, tryUrl_ );
        }
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
     * Returns policy for table construction.
     *
     * @return   true for caching, false for read as required,
     *           null for adaptive
     */
    public Boolean getCacheCols() {
        return cacheCols_;
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
     * Configures whether an attempt is made to open parquet files from
     * non-file URLs.
     *
     * @param  tryUrl  true to attempt opening non-file URLs
     */
    @ConfigMethod(
        property = "tryUrl",
        doc = "<p>Whether to attempt to open non-file URLs as parquet files.\n"
            + "This usually seems to fail with a cryptic error message,\n"
            + "so it is not attempted by default, but it's possible that with\n"
            + "suitable library support on the classpath it might work,\n"
            + "so this option exists to make the attempt.\n"
            + "</p>",
        example = "true"
    ) 
    public void setTryUrl( boolean tryUrl ) {
        tryUrl_ = tryUrl;
    }

    /**
     * Indicates whether an attempt is made to open parquet files from
     * non-file URLs.
     *
     * @return  true to attempt non-file URLs, false to just give up
     */
    public boolean getTryUrl() {
        return tryUrl_;
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
}
