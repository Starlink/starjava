package uk.ac.starlink.parquet;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.apache.parquet.column.statistics.Statistics;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.metadata.BlockMetaData;
import org.apache.parquet.hadoop.metadata.ColumnChunkMetaData;
import org.apache.parquet.hadoop.metadata.FileMetaData;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.schema.MessageType;
import uk.ac.starlink.table.BeanStarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.LogUtils;

/**
 * Simple utility that can display metadata information from a parquet file
 * to standard output.
 *
 * <p>This can be invoked using the {@link #main} method, which provides a
 * <code>-h</code> option for help.  Or its functionality can be wrapped
 * by a more capable utility.
 *
 * @author   Mark Taylor
 * @since    8 Jan 2024
 */
public class ParquetDump {

    private final ParquetStarTable starTable_;
    private final ParquetFileReader pfr_;
    private final FileMetaData fileMeta_;
    private final ParquetMetadata footer_;
    private final MessageType schema_;

    /**
     * Constructor.
     *
     * @param   starTable  parquet star table
     */
    public ParquetDump( ParquetStarTable starTable ) throws IOException {
        starTable_ = starTable;
        pfr_ = starTable.getParquetFileReader();
        fileMeta_ = pfr_.getFileMetaData();
        footer_ = pfr_.getFooter();
        schema_ = fileMeta_.getSchema();
    }

    /**
     * Returns the table on which this object can report.
     *
     * @return  parquet table
     */
    public ParquetStarTable getTable() {
        return starTable_;
    }

    /**
     * Returns a multi-line string showing the parquet schema.
     *
     * @return  schema string
     */
    public String formatSchema() {
        StringBuilder sb = new StringBuilder();
        String schemaIndent = "   ";  // this value seems to be ignored
        schema_.writeToStringBuilder( sb, schemaIndent );
        return sb.toString();
    }

    /**
     * Returns a multi-line string giving an abbreviated representation
     * of the content of the file-level key-value metadata in the
     * parquet footer.
     * If any of the values is longer than the supplied <code>maxChar</code>
     * threshold, or if it contains embedded newlines, it is just represented
     * by a summary of its length.
     *
     * @param  maxChar    maximum length of displayed value
     * @return  key-value string
     */
    public String formatKeyValuesCompact( int maxChar ) {
        StringBuffer sbuf = new StringBuffer();
        for ( Map.Entry<String,String> entry :
              starTable_.getExtraMetadataMap().entrySet() ) {
            String key = entry.getKey();
            String value = entry.getValue();
            sbuf.append( key )
                .append( ": " );
            if ( value.length() + key.length() < maxChar &&
                 value.indexOf( '\n' ) < 0 ) {
                sbuf.append( value );
            }
            else {
                sbuf.append( "<" + value.length() + " chars>" );
            }
            sbuf.append( '\n' );
        }
        return sbuf.toString();
    }

    /**
     * Returns a multi-line string showing information about the data
     * blocks in the parquet file.
     *
     * @return  block info string
     */
    public String formatBlocks() {
        StringBuffer sbuf = new StringBuffer();
        BeanStarTable blockTable;
        try {
            blockTable = new BeanStarTable( BlockMetaData.class );
        }
        catch ( IntrospectionException e ) {
            throw new RuntimeException( e );
        }

        /* Note these must match bean accessor methods from the
         * BlockMetaData class. */
        blockTable.setColumnProperties( new String[] {
            "ordinal", 
            "rowIndexOffset",
            "rowCount",
            "compressedSize",
            "totalByteSize",
        } );
        blockTable.setData( footer_.getBlocks()
                                   .toArray( new BlockMetaData[ 0 ] ) );
        return Tables.tableToString( blockTable, "ascii" );
    }

    /**
     * Returns a multi-line string showing information about the column chunks
     * in each data block.
     *
     * @return  column-chunk string
     */
    public String formatColumnChunks() {
        StringBuffer sbuf = new StringBuffer();
        int ib = 0;
        for ( BlockMetaData block : footer_.getBlocks() ) {
            if ( ib > 0 ) {
                sbuf.append( "\n" );
            }
            sbuf.append( "Block " )
                .append( block.getOrdinal() )
                .append( " (" )
                .append( block.getRowCount() )
                .append( " rows)" )
                .append( ":\n" )
                .append( "------------------------\n" );
            BeanStarTable chunkTable;
            try {
                chunkTable = new BeanStarTable( ChunkMeta.class );
            }
            catch ( IntrospectionException e ) {
                throw new RuntimeException( e );
            }
            chunkTable.setData( block.getColumns()
                               .stream()
                               .map( ChunkMeta::new )
                               .toArray( n -> new ChunkMeta[ n ] ) );

            /* Note these must match bean accessor methods from the
             * ChunkMeta class. */
            chunkTable.setColumnProperties( new String[] {
                "path",
                "type",
                "count",
                "nullCount",
                "uncompressedSize",
                "compressedSize",
                "codec",
                "encodings",
                "minMax",
            } );
            sbuf.append( Tables.tableToString( chunkTable, "ascii" ) );
        }
        return sbuf.toString();
    }

    /**
     * Formats a multi-line string with a supplied heading.
     *
     * @param  heading  heading text
     * @param  lines   multi-line string giving text
     */
    public static String formatLines( String heading, String lines ) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( heading )
            .append( ":\n" );
        if ( lines != null ) {
            for ( String line : lines.split( "[\n\r]+" ) ) {
                sbuf.append( "   " )
                    .append( line )
                    .append( '\n' );
            }
        }
        return sbuf.toString();
    }

    /**
     * Bean adaptor class that wraps a ColumnChunkMetaData object to
     * provide BeanStarTable-friendly methods
     * (string- or primitive-valued accessors) that report interesting
     * information about a ColumnChunkMetaData object.
     */
    public static class ChunkMeta {
        final ColumnChunkMetaData base_;

        /**
         * Constructor.
         *
         * @param  base  base object
         */
        ChunkMeta( ColumnChunkMetaData base ) {
            base_ = base;
        }
        public String getPath() {
            return base_.getPath().toDotString();
        }
        public String getType() {
            return base_.getPrimitiveType().getPrimitiveTypeName().toString();
        }
        public String getCodec() {
            return base_.getCodec().toString();
        }
        public boolean isDict() {
            return base_.hasDictionaryPage();
        }
        public long getCompressedSize() {
            return base_.getTotalSize();
        }
        public long getUncompressedSize() {
            return base_.getTotalUncompressedSize();
        }
        public long getCount() {
            return base_.getValueCount();
        }
        public long getNullCount() {
            return base_.getStatistics().getNumNulls();
        }
        public String getMinMax() {
            Statistics<?> stats = base_.getStatistics();
            if ( stats != null ) {
                Object min = stats.minAsString();
                Object max = stats.maxAsString();
                if ( min != null || max != null ) {
                    return String.valueOf(min)
                         + "/"
                         + String.valueOf(max);
                }
                else {
                    return null;
                }
            }
            else {
                return null;
            }
        }
        public String getEncodings() {
            return base_.getEncodings().stream()
                  .map( Object::toString )
                  .collect( Collectors.joining( "," ) );
        }
    }

    /**
     * Returns a map of the output functions offered by this utility.
     *
     * @return  map with keys that are function labels,
     *          and values that map a dump instance to a multiline output string
     */
    public static Map<String,Function<ParquetDump,String>>
            createDumpFunctionMap() {
        Map<String, Function<ParquetDump,String>> map = new LinkedHashMap<>();
        map.put( "schema",
                 dump -> formatLines( "Schema", dump.formatSchema() ) );
        map.put( "kv",
                 dump -> formatLines( "Key-Values",
                                      dump.formatKeyValuesCompact( 75 ) ) );
        map.put( "block",
                 dump -> formatLines( "Blocks", dump.formatBlocks() ) );
        map.put( "chunk",
                 dump -> formatLines( "Column Chunks",
                                      dump.formatColumnChunks() ) );
        map.put( "vot",
                 dump -> formatLines( "VOTable",
                                      dump.starTable_
                                          .getVOTableMetadataText() ) );
        return map;
    }

    /**
     * Reads a parquet table from a filename or URL, suitable for use
     * with this utility.
     * Columns unsupported by STIL are included.
     *
     * @param  location   filename or URL
     * @return   parquet table
     */
    public static ParquetStarTable readParquetTable( String location )
            throws IOException {
        ParquetIO io = ParquetUtil.getIO();
        DataSource datsrc = DataSource.makeDataSource( location );
        ParquetTableBuilder builder = new ParquetTableBuilder();
        boolean useCache = false;
        boolean tryUrl = false;
        ParquetStarTable.Config config = new ParquetStarTable.Config() {
            public boolean includeUnsupportedColumns() {
                return true;
            }
        };
        return io.readParquet( datsrc, builder, config, useCache, tryUrl );
    }

    /**
     * Main method.  Use <code>-help</code>.
     */
    public static void main( String[] args ) throws IOException {
        LogUtils.getLogger( "uk.ac.starlink" ).setLevel( Level.WARNING );
        ParquetIO io = ParquetUtil.getIO();

        /* Prepare display options. */
        Map<String, Function<ParquetDump,String>> funcMap =
            createDumpFunctionMap();

        /* Autogenerate usage string. */
        String usage = "\n   Usage: " + ParquetDump.class.getName()
                     + funcMap.keySet().stream().map( k -> " -" + k )
                                       .collect( Collectors.joining() )
                     + " <parquet-file>"
                     + "\n";

        /* Process command-line arguments. */
        List<String> items = new ArrayList<>();
        List<String> argList = new ArrayList<>( Arrays.asList( args ) );
        for ( Iterator<String> it = argList.iterator(); it.hasNext(); ) {
            String arg = it.next();
            if ( arg.startsWith( "-h" ) ) {
                it.remove();
                System.out.println( usage );
                return;
            }
            else if ( arg.startsWith( "-" ) ) {
                String itemName = null;
                for ( String name : funcMap.keySet() ) {
                    if ( arg.startsWith( "-" + name ) ) {
                        itemName = name;
                        it.remove();
                    }
                }
                if ( itemName == null ) {
                    System.err.println( usage );
                    System.exit( 1 );
                }
                else {
                    items.add( itemName );
                }
            }
        }
        if ( argList.size() != 1 ) {
            System.err.println( usage );
            System.exit( 1 );
        }
        String loc = argList.remove( 0 );
        if ( items.isEmpty() ) {
            items.addAll( funcMap.keySet() );
        }
        ParquetStarTable table = readParquetTable( loc );

        /* Perform requested actions. */
        ParquetDump dump = new ParquetDump( table );
        for ( String name : items ) {
            System.out.println( funcMap.get( name ).apply( dump ) );
        }
    }
}
