package uk.ac.starlink.parquet;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.io.OutputFile;
import org.apache.parquet.io.PositionOutputStream;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.formats.DocumentedIOHandler;

/**
 * TableWriter implementation for output to Parquet format.
 *
 * @author   Mark Taylor
 * @since    25 Feb 2021
 */
public class ParquetTableWriter
        implements StarTableWriter, DocumentedIOHandler {

    static {
        ParquetUtil.silenceLog4j();
    }

    public ParquetTableWriter() {
    }

    public String getFormatName() {
        return "parquet";
    }

    public String[] getExtensions() {
        return new String[] { "parquet", "parq" };
    }

    public boolean looksLikeFile( String location ) {
        return DocumentedIOHandler.matchesExtension( this, location );
    }

    public String getMimeType() {
        return "application/octet-stream";
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
            "<p>At present, only very limited metadata is written.",
            "Parquet does not seem(?) to have any standard format for",
            "per-column metadata, so the only information written about",
            "each column apart from its datatype is its name.",
            "</p>",
            ""
        );
    }

    public void writeStarTable( StarTable table, String location,
                                StarTableOutput sto )
            throws IOException {
        if ( "-".equals( location ) ) {
            writeStarTable( table, System.out );
        }
        else {
            Path path = new Path( location );
            StarParquetWriter.StarBuilder builder =
                new StarParquetWriter.StarBuilder( table, path );
            configureBuilder( builder );
            writeParquetTable( builder );
        }
    }

    public void writeStarTable( StarTable table, OutputStream out )
            throws IOException {
        StarParquetWriter.StarBuilder builder =
            new StarParquetWriter.StarBuilder( table, createOutputFile( out ) );
        configureBuilder( builder );
        writeParquetTable( builder );
    }

    /**
     * Performs all required post-construction configuration
     * of the ParquetWriter.Builder in accordance with the requirements
     * of this ParquetTableWriter.
     *
     * @param  builder  builder to configure
     */
    private void configureBuilder( StarParquetWriter.StarBuilder builder ) {
        builder.withWriteMode( ParquetFileWriter.Mode.OVERWRITE )
               .withValidation( true )
               .withPageWriteChecksumEnabled( false ) // doesn't seem to help
               .withDictionaryEncoding( true );
    }

    /**
     * Performs the actual writing.
     *
     * @param  builder  configuration object
     */
    private void writeParquetTable( StarParquetWriter.StarBuilder builder )
            throws IOException {
        try ( ParquetWriter<Object[]> pwriter = builder.build() ) {
            try ( RowSequence rseq = builder.getTable().getRowSequence() ) {
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
