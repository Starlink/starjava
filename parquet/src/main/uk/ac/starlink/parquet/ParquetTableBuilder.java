package uk.ac.starlink.parquet;

import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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

        /* For random access, it would be nice to read the table in parallel
         * here (multiple columns on multiple threads, using e.g.
         * uk.ac.starlink.table.storage.ColumnStore implementations).
         * It ought to be possible, but as far as I can see,
         * parquet-mr table read methods have only a single input stream,
         * so there's no chance to do concurrent reads.  You could do
         * totally separate reads of the input file on multiple threads,
         * but the PageReadStore objects (row groups) are slow to read
         * and very memory heavy for large files, and I can't see how to
         * narrow them down to read only for a subset of columns,
         * so the idea seems doomed, despite the fact that pyarrow.parquet
         * can do it fine. */
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
