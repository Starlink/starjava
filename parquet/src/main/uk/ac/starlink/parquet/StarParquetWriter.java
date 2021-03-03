package uk.ac.starlink.parquet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Types;
import org.apache.parquet.io.OutputFile;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;

/**
 * ParquetWriter implementation for output of StarTables.
 *
 * @author   Mark Taylor
 * @since    25 Feb 2021
 */
public class StarParquetWriter extends ParquetWriter<Object[]> {

    // Written with reference to implementation classes
    // org.apache.parquet.cli.commands.ConvertCSVCommand,
    // org.apache.parquet.avro.AvroWriteSupport.

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.parquet" );

    // All superclass constructors are deprecated, but I don't see any other
    // way to construct one.  The Avro and Example ParquetWriter
    // implementations both use these deprecated constructors.
    @SuppressWarnings("deprecation")
    private StarParquetWriter( Path path, StarWriteSupport support )
            throws IOException {
        super( path, support );
    }

    /**
     * Used to configure and produce a ParquetWriter for StarTable output.
     */
    public static class StarBuilder
            extends ParquetWriter.Builder<Object[],StarBuilder> {
        private final StarTable table_;
        private boolean groupArray_;

        /**
         * Constructor based on a hadoop Path.
         *
         * @param  table  table to write
         * @param  path  destination path
         */
        public StarBuilder( StarTable table, Path path ) {
            super( path );
            table_ = table;
        }

        /**
         * Constructor based on a parquet OutputFile.
         *
         * @param  table  table to write
         * @param  ofile  destination file
         */
        public StarBuilder( StarTable table, OutputFile ofile ) {
            super( ofile );
            table_ = table;
        }

        /**
         * Returns the table that this writer will write.
         *
         * @return  table
         */
        public StarTable getTable() {
            return table_;
        }

        /**
         * Configures array-valued column writing style.
         *
         * @param   groupArray   true for group-style arrays,
         *                       false for repeated primitives
         */
        public StarBuilder withGroupArray( boolean groupArray ) {
            groupArray_ = groupArray;
            return self();
        }

        public StarBuilder self() {
            return this;
        }

        public WriteSupport<Object[]> getWriteSupport( Configuration config ) {
            if ( table_ == null ) {
                throw new IllegalStateException( "builder.withTable"
                                               + " not called" );
            }
            return new StarWriteSupport( table_, groupArray_ );
        }
    }

    /**
     * WriteSupport implementation for StarTable output.
     * The typed object written is a row of objects as supplied by
     * {@link uk.ac.starlink.table.RowSequence#getRow}.
     */
    private static class StarWriteSupport extends WriteSupport<Object[]> {

        private final int ncol_;
        private final OutCol[] outcols_;
        private final MessageType schema_;
        private final Map<String,String> metaMap_;
        private RecordConsumer consumer_;

        /**
         * Constructor.
         *
         * @param  table  table to write
         * @param   groupArray   true for group-style arrays,
         *                       false for repeated primitives
         */
        StarWriteSupport( StarTable table, boolean groupArray ) {
            List<OutCol> outcols = new ArrayList<>();
            metaMap_ = new HashMap<String,String>();
            Types.MessageTypeBuilder schemaBuilder = Types.buildMessage();
            int jc = 0;
            for ( int ic = 0; ic < table.getColumnCount(); ic++ ) {
                ColumnInfo cinfo = table.getColumnInfo( ic );
                Encoder encoder = Encoders.createEncoder( cinfo, groupArray );
                if ( encoder != null ) {
                    outcols.add( new OutCol( encoder, ic, jc++ ) );
                    schemaBuilder.addField( encoder.getColumnType() );
                }
                else {
                    logger_.warning( "Can't write column to parquet: "
                                   + cinfo );
                }
            }
            schema_ = schemaBuilder.named( "table" );
            outcols_ = outcols.toArray( new OutCol[ 0 ] );
            ncol_ = outcols_.length;
        }

        public String getName() {
            return "STIL";
        }

        public WriteContext init( Configuration config ) {
            return new WriteContext( schema_, metaMap_ );
        }

        public FinalizedWriteContext finalizeWrite() {
            return new FinalizedWriteContext( new HashMap<String,String>() );
        }

        public void prepareForWrite( RecordConsumer recordConsumer ) {
            consumer_ = recordConsumer;
        }

        public void write( Object[] record ) {
            consumer_.startMessage();
            for ( OutCol outcol : outcols_ ) {
                outcol.write( record[ outcol.icIn_ ], consumer_ );
            }
            consumer_.endMessage();
        }
    }

    /**
     * Aggregates information required about a table column to write.
     */
    private static class OutCol {

        final Encoder encoder_;
        final int icIn_;
        final int icOut_;
        final String cname_;

        /**
         * Constructor.
         *
         * @param  encoder  typed value encoder
         * @param  icIn   index of column in input table
         * @param  icOut  index of column in output table
         */
        OutCol( Encoder encoder, int icIn, int icOut ) {
            encoder_ = encoder;
            icIn_ = icIn;
            icOut_ = icOut;
            cname_ = encoder.getColumnName();
        }

        /**
         * Outputs a value from the StarTable column to the Parquet
         * record consumer.
         *
         * @param  value   typed value from input table column
         * @param  consumer   value destination
         */
        void write( Object value, RecordConsumer consumer ) {
            if ( value != null ) {
                consumer.startField( cname_, icOut_ );
                encoder_.addValue( value, consumer );
                consumer.endField( cname_, icOut_ );
            }
        }
    }
}
