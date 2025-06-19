package uk.ac.starlink.parquet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
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
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.IntList;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.StringElementSizer;
import uk.ac.starlink.votable.VOSerializer;
import uk.ac.starlink.votable.VOSerializerConfig;
import uk.ac.starlink.votable.VOTableVersion;
import uk.ac.starlink.votable.VOTableWriter;

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
        private VOTableVersion votmetaVersion_;
        private Map<String,String> kvItems_;

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

        /**
         * Configures output of metadata in VOTable format.
         * If a non-null value is supplied, metadata will be written into
         * the output file key-value extra metadata list under the key
         * {@link ParquetStarTable#VOTMETA_KEY}.
         *
         * @param  version  VOTable version for dummy metadata table,
         *                  or null if not required
         */
        public StarBuilder withVOTableMetadata( VOTableVersion version ) {
            votmetaVersion_ = version;
            return self();
        }

        /**
         * Configures additional content for the key-value metadata in
         * the parquet footer.  Any items supplied here will override
         * values supplied in other ways.  An entry with a null value
         * will have the effect of removing the corresponding key.
         *
         * @param  kvItems  additional key-value metadata for parquet footer
         */
        public StarBuilder withKeyValueItems( Map<String,String> kvItems ) {
            kvItems_ = kvItems;
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
            return new StarWriteSupport( table_, groupArray_, votmetaVersion_,
                                         kvItems_ );
        }
    }

    /**
     * WriteSupport implementation for StarTable output.
     * The typed object written is a row of objects as supplied by
     * {@link uk.ac.starlink.table.RowSequence#getRow}.
     */
    private static class StarWriteSupport extends WriteSupport<Object[]> {

        private final int ncol_;
        private final OutCol<?>[] outcols_;
        private final MessageType schema_;
        private final Map<String,String> metaMap_;
        private RecordConsumer consumer_;

        /**
         * Constructor.
         *
         * @param  table  table to write
         * @param   groupArray   true for group-style arrays,
         *                       false for repeated primitives
         * @param   votmetaVersion  version of dummy metadata VOTable to write,
         *                          or null if not required
         * @param   kvItems  additional items for the key-value
         *                   extended metadata map; these will overwrite
         *                   anything otherwise put there, and keys with
         *                   a null value will remove the entry
         */
        StarWriteSupport( StarTable table, boolean groupArray,
                          VOTableVersion votmetaVersion,
                          Map<String,String> kvItems ) {
            List<OutCol<?>> outcols = new ArrayList<>();
            Types.MessageTypeBuilder schemaBuilder = Types.buildMessage();
            int jc = 0;
            int nc = table.getColumnCount();
            IntList icList = new IntList( nc );
            for ( int ic = 0; ic < nc; ic++ ) {
                ColumnInfo cinfo = table.getColumnInfo( ic );
                Encoder<?> encoder =
                    Encoders.createEncoder( cinfo, groupArray );
                if ( encoder != null ) {
                    outcols.add( OutCol.create( encoder, ic, jc++ ) );
                    schemaBuilder.addField( encoder.getColumnType() );
                    icList.add( ic );
                }
                else {
                    logger_.warning( "Can't write column to parquet: "
                                   + cinfo );
                }
            }
            schema_ = schemaBuilder.named( "table" );
            outcols_ = outcols.toArray( new OutCol<?>[ 0 ] );
            ncol_ = outcols_.length;

            StarTable outTable =
                new ColumnPermutedStarTable( table, icList.toIntArray() );
            assert outTable.getColumnCount() == ncol_;
            metaMap_ = getParquetMetadata( outTable );
            if ( votmetaVersion != null ) {
                String votmeta =
                    createMetadataVOTable( outTable, votmetaVersion );
                if ( votmeta != null ) {
                    metaMap_.put( ParquetStarTable.VOTMETA_KEY, votmeta );
                    metaMap_.put( ParquetStarTable.VOTMETAVERSION_KEY,
                                  ParquetStarTable.REQUIRED_VOTMETAVERSION );
                }
            }
            if ( kvItems != null ) {
                for ( Map.Entry<String,String> entry : kvItems.entrySet() ) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if ( value == null ) {
                        metaMap_.remove( key );
                    }
                    else {
                        metaMap_.put( key, value );
                    }
                }
            }
        }

        public String getName() {
            return "STIL";
        }

        public WriteContext init( Configuration config ) {
            return new WriteContext( schema_, metaMap_ );
        }

        public FinalizedWriteContext finalizeWrite() {
            return new FinalizedWriteContext( metaMap_ );
        }

        public void prepareForWrite( RecordConsumer recordConsumer ) {
            consumer_ = recordConsumer;
        }

        public void write( Object[] record ) {
            consumer_.startMessage();
            for ( OutCol<?> outcol : outcols_ ) {
                outcol.write( record[ outcol.icIn_ ], consumer_ );
            }
            consumer_.endMessage();
        }
    }

    /**
     * Returns a key-value map of extra file-level metadata that can
     * be associated in the output parquet file with a given StarTable.
     *
     * @param  table  table
     * @return  extra metadata map
     */
    private static Map<String,String> getParquetMetadata( StarTable table ) {
        Map<String,String> map = new LinkedHashMap<>();
        String name = table.getName();
        if ( name != null && name.trim().length() > 0 ) {
            map.put( ParquetStarTable.NAME_KEY, name );
        }
        int maxleng = 160;
        for ( DescribedValue dval : table.getParameters() ) {
            ValueInfo pinfo = dval.getInfo();
            String pname = pinfo.getName();
            Object pvalue = dval.getValue();
            if ( pvalue != null ) {
                String ptxt = pinfo.formatValue( pvalue, maxleng );
                if ( ptxt != null && ptxt.trim().length() > 0 &&
                     ptxt.equals( pinfo.formatValue( pvalue,
                                                     ( maxleng + 1 ) ) ) &&
                     ! map.containsKey( pname ) ) {
                    map.put( pname, ptxt );
                }
            }
        }
        return map;
    }

    /**
     * Returns the text of a DATA-less VOTable describing the metadata
     * of a supplied table.
     *
     * @param  table  table to document
     * @param  version   version of VOTable for serialization
     * @return   DATA-less VOTable content, or null if there was a problem
     */
    private static String createMetadataVOTable( StarTable table,
                                                 VOTableVersion version ) {
        if ( version == null ) {
            return null;
        }
        StringWriter textWriter = new StringWriter();
        BufferedWriter writer = new BufferedWriter( textWriter );
        VOTableWriter votWriter =
            new VOTableWriter( (DataFormat) null, false, version ) {};
        votWriter.setWriteDate( false );

        /* We use a fixed-value StringElementSizer here since it at least
         * gives the right dimensionality which is enough to keep
         * validators happy.  The actual string element sizes will be
         * variable, since that's handled by the parquet not VOTable output. */
        VOSerializerConfig config =
            new VOSerializerConfig( DataFormat.TABLEDATA, version,
                                    StringElementSizer.FIXED2 );
        try {
            VOSerializer voser = VOSerializer.makeSerializer( config, table );
            votWriter.writePreTableXML( writer );
            voser.writePreDataXML( writer );
            writer.write( "<!-- Dummy VOTable - no DATA element -->" );
            writer.newLine();
            voser.writePostDataXML( writer );
            votWriter.writePostTableXML( writer );
            writer.flush();
            return textWriter.getBuffer().toString();
        }
        catch ( IOException e ) {
            logger_.log( Level.WARNING,
                         "Failed to serialize VOTable metadata: " + e, e );
            return null;
        }
    }

    /**
     * Aggregates information required about a table column to write.
     */
    private static class OutCol<T> {

        final Encoder<T> encoder_;
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
        OutCol( Encoder<T> encoder, int icIn, int icOut ) {
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
            T typedValue = encoder_.typedValue( value );
            if ( typedValue != null ) {
                consumer.startField( cname_, icOut_ );
                encoder_.addValue( typedValue, consumer );
                consumer.endField( cname_, icOut_ );
            }
            else {
                encoder_.checkNull();
            }
        }

        /**
         * Creates a new Encoder instance.
         *
         * <p>Does the same thing as the constructor, but can be called
         * without knowledge of parameterised type.
         *
         * @param  encoder  typed value encoder
         * @param  icIn   index of column in input table
         * @param  icOut  index of column in output table
         */
        static <T> OutCol<T> create( Encoder<T> encoder, int icIn, int icOut ) {
            return new OutCol<T>( encoder, icIn, icOut );
        }
    }
}
