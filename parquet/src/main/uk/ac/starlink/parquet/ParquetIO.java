package uk.ac.starlink.parquet;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.OutputFile;
import org.apache.parquet.io.PositionOutputStream;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.IOSupplier;
import uk.ac.starlink.util.URLUtils;

/**
 * Handles all the interactions with the parquet-mr libraries required
 * by the high-level starjava parquet table I/O classes.
 * If the parquet-mr libraries are not present, it may not be possible
 * to instantiate this class.
 *
 * @author   Mark Taylor
 * @since    9 Apr 2021
 */
class ParquetIO {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.parquet" );

    /**
     * Reads a parquet table on behalf of a ParquetTableBuilder.
     *
     * @param  datsrc  data source
     * @param  builder   handler providing configuration
     * @param  useCache  true for column caching, false for sequential
     * @return  loaded table
     */
    public StarTable readParquet( DataSource datsrc,
                                  ParquetTableBuilder builder,
                                  boolean useCache )
            throws IOException {
        IOSupplier<ParquetFileReader> pfrSupplier =
            readerSupplier( createInputFile( datsrc ), datsrc.getName() );
        if ( useCache ) {
            int nThread = builder.getReadThreadCount();
            if ( nThread <= 0 ) {
                nThread = CachedParquetStarTable.getDefaultThreadCount();
            }
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

    /**
     * Writes a parqet table to a named location on behalf of a
     * ParquetTableWriter.
     *
     * @param   table  table to write
     * @param   writer  handler providing configuration
     * @param   location  destination
     */
    public void writeParquet( StarTable table, ParquetTableWriter writer,
                              String location )
            throws IOException {
        Path path = new Path( location );
        StarParquetWriter.StarBuilder builder =
            new StarParquetWriter.StarBuilder( table, path );
        configureBuilder( builder, writer );
        writeParquetTable( builder );
    }

    /**
     * Writes a parqet table to an output stream on behalf of a
     * ParquetTableWriter.
     *
     * @param   table  table to write
     * @param   writer  handler providing configuration
     * @param   out  destination stream
     */
    public void writeParquet( StarTable table, ParquetTableWriter writer,
                              OutputStream out )
            throws IOException {
        StarParquetWriter.StarBuilder builder =
            new StarParquetWriter.StarBuilder( table, createOutputFile( out ) );
        configureBuilder( builder, writer );
        writeParquetTable( builder );
    }

    /**
     * Returns an IOSupplier for an input file.
     *
     * @param  inFile  input file
     * @param  name   filename for reporting
     * @return   supplier for ParquetFileReader
     */
    private static IOSupplier<ParquetFileReader>
            readerSupplier( final InputFile inFile, final String name ) {
        return () -> {
            try {
                return ParquetFileReader.open( inFile );
            }

            /* The ParquetFileReader sometimes generates RuntimeExceptions
             * (e.g. for absent trailing magic number), so catch and rethrow
             * here. */
            catch ( RuntimeException e ) {
                throw new TableFormatException( "Trouble opening "
                                              + name + " as parquet", e );
            }
        };
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

    /**
     * Performs all required post-construction configuration
     * of the ParquetWriter.Builder.
     *
     * @param  builder  builder to configure
     * @param  writer   handler to specify configuration details
     */
    private static void configureBuilder( StarParquetWriter.StarBuilder builder,
                                          ParquetTableWriter writer ) {
        builder.withWriteMode( ParquetFileWriter.Mode.OVERWRITE )
               .withGroupArray( writer.isGroupArray() )
               .withValidation( true )
               .withPageWriteChecksumEnabled( false ) // doesn't seem to help
               .withDictionaryEncoding( true );
        CompressionCodecName codec = writer.getCompressionCodec();
        if ( codec != null ) {
            builder.withCompressionCodec( codec );
        }
        Boolean useDict = writer.isDictionaryEncoding();
        if ( useDict != null ) {
            builder.withDictionaryEncoding( useDict.booleanValue() );
        }
    }

    /**
     * Performs the actual writing.
     *
     * @param  builder  configuration object
     */
    private static void writeParquetTable( StarParquetWriter.StarBuilder bldr )
            throws IOException {
        try ( ParquetWriter<Object[]> pwriter = bldr.build() ) {
            try ( RowSequence rseq = bldr.getTable().getRowSequence() ) {
                while ( rseq.next() ) {
                    pwriter.write( rseq.getRow() );
                }
            }
        }
    }

    /**
     * Adapts an OutputStream to an parquet.io.OutputFile.
     * Of course this is not suitable for multiple calls to the stream
     * creation methods, so it probably breaks the implicit contract of
     * the OutputFile class, but it seems to be good enough for streaming
     * writes in this class.
     *
     * @param  out  destination stream
     * @return   output file that writes to the stream
     */
    private static OutputFile createOutputFile( final OutputStream out ) {
        return new OutputFile() {
            public PositionOutputStream create( long blockSizeHint )
                    throws IOException {
                return createPositionOutputStream( out );
            }
            public PositionOutputStream createOrOverwrite( long blockSizeHint )
                    throws IOException {
                return createPositionOutputStream( out );
            }
            public boolean supportsBlockSize() {
                return false;
            }
            public long defaultBlockSize() {
                return -1L;  // I think this is correct, but I can't see docs
            }
        };
    }

    /**
     * Adapts an OutputStream to a PositionOutputStream.
     *
     * @param  out  base output stream
     * @return   position-aware output stream
     */
    private static PositionOutputStream
            createPositionOutputStream( final OutputStream out ) {
        return new PositionOutputStream() {
            private long pos_;
            public long getPos() {
                return pos_;
            }
            @Override
            public void write( int b ) throws IOException {
                out.write( b );
                pos_++;
            }
            @Override
            public void write( byte[] b ) throws IOException {
                out.write( b );
                pos_ += b.length;
            }
            @Override
            public void write( byte[] b, int off, int len ) throws IOException {
                out.write( b, off, len );
                pos_ += len;
            }
            @Override
            public void flush() throws IOException {
                out.flush();
            }
            @Override
            public void close() throws IOException {
                out.close();
            }
        };
    }
}
