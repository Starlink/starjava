package uk.ac.starlink.parquet;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.formats.DocumentedIOHandler;
import uk.ac.starlink.util.ConfigMethod;
import uk.ac.starlink.util.IOUtils;
import uk.ac.starlink.votable.VOTableVersion;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;

/**
 * TableWriter implementation for output to Parquet format.
 * As well as writing a basic Parquet file, metadata following the
 * <a href="https://www.ivoa.net/documents/Notes/VOParquet/"
 *    >VOParquet convention</a> (currently at v1.0) is optionally written.
 *
 * @author   Mark Taylor
 * @since    25 Feb 2021
 */
public class ParquetTableWriter
        implements StarTableWriter, DocumentedIOHandler {

    private boolean groupArray_;
    private CompressionCodecName codec_;
    private Boolean useDict_;
    private boolean votMeta_;
    private Map<String,String> kvItems_;
    private VOTableVersion votVersion_;

    public ParquetTableWriter() {
        groupArray_ = true;
        votMeta_ = true;
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
            readText( "parquet-format.xml" ),
            "</p>",
            "<p>The parquet file format itself defines only rather limited",
            "semantic metadata, so that there is no standard way to record",
            "column units, descriptions, UCDs etc.",
            "By default,",
            "additional metadata is written in the form of a DATA-less VOTable",
            "attached to the file footer, as described by the",
            "<webref url='https://www.ivoa.net/documents/Notes/VOParquet/'",
            ">VOParquet convention</webref>.",
            "This additional metadata can then be retrieved by other",
            "VOParquet-aware software.",
            "</p>",
            readText( "parquet-packaging.xml" ),
            ""
        );
    }

    public void writeStarTable( StarTable table, String location,
                                StarTableOutput sto )
            throws IOException {
        ParquetIO io = ParquetUtil.getIO();
        if ( "-".equals( location ) ) {
            io.writeParquet( table, this, System.out );
        }
        else {
            io.writeParquet( table, this, location );
        }
    }

    public void writeStarTable( StarTable table, OutputStream out )
            throws IOException {
        ParquetUtil.getIO().writeParquet( table, this, out );
    }

    /**
     * Configures how array-valued columns are written.
     * If false, it's a top-level <code>repeated</code> primitive,
     * if true, it's an <code>optional group</code> containing a
     * <code>repeated group</code> containing a <code>optional</code>
     * primitive.
     *
     * <p>True is the default, set it false with care, since that precludes
     * null array values or array elements.
     *
     * @param  groupArray   true for grouped arrays,
     *                      false for repeated primitives
     */
    @ConfigMethod(
        property = "groupArray",
        sequence = 5,
        usage = "true|false",
        example = "false",
        doc = "<p>Controls the low-level detail of how array-valued columns\n"
            + "are written.\n"
            + "For an array-valued int32 column named IVAL,\n"
            + "<code>groupArray=false</code> will write it as\n"
            + "\"<code>repeated int32 IVAL</code>\"\n"
            + "while <code>groupArray=true</code> will write it as\n"
            + "\"<code>optional group IVAL (LIST) {repeated group list\n"
            + "{optional int32 element}}</code>\".\n"
            + "</p>"
            + "<p>Although setting it <code>false</code> may be slightly more\n"
            + "efficient, the default is <code>true</code>,\n"
            + "since if any of the columns have array values that either\n"
            + "may be null or may have elements which are null,\n"
            + "groupArray-style declarations for all columns are required\n"
            + "by the <webref url='"
                     + "https://github.com/apache/parquet-format/blob/"
                     + "apache-parquet-format-2.10.0/"
                     + "LogicalTypes.md'>Parquet file format</webref>:\n"
            + "<blockquote><em>\n"
            + "\"A repeated field that is neither contained by a LIST- or\n"
            + "MAP-annotated group nor annotated by LIST or MAP should be\n"
            + "interpreted as a required list of required elements where\n"
            + "the element type is the type of the field.\n"
            + "Implementations should use either LIST and MAP annotations\n"
            + "or unannotated repeated fields, but not both. When using the\n"
            + "annotations, no unannotated repeated types are allowed.\"\n"
            + "</em></blockquote>\n"
            + "</p>"
            + "<p>If this option is set false and an attempt is made to write\n"
            + "null arrays or arrays with null values, writing will fail.\n"
            + "</p>"
    )
    public void setGroupArray( boolean groupArray ) {
        groupArray_ = groupArray;
    }

    /**
     * Indicates how array-valued columns are written.
     *
     * @return   true for grouped arrays,
     *           false for repeated primitives
     */
    public boolean isGroupArray() {
        return groupArray_;
    }

    /**
     * Sets the compression type for data output.
     * Supported options are currently
     * <code>uncompressed</code>, <code>snappy</code>,
     * <code>gzip</code>, <code>lz4_raw</code>.
     *
     * @param  codec  compression type
     */
    @ConfigMethod(
        property = "compression",
        sequence = 2,
        example = "gzip",
        usage = "uncompressed|snappy|zstd|gzip|lz4_raw",
        doc = "<p>Configures the type of compression used for output.\n"
            + "Supported values are probably\n"
            + "<code>uncompressed</code>, <code>snappy</code>,\n"
            + "<code>zstd</code>, <code>gzip</code> and <code>lz4_raw</code>.\n"
            + "Others may be available if the relevant codecs are on the\n"
            + "classpath at runtime.\n"
            + "If no value is specified, the parquet-mr library default\n"
            + "is used, which is probably <code>uncompressed</code>."
            + "</p>"
    )
    public void setCompressionCodec( CompressionCodecName codec ) {
        codec_ = codec;
    }

    /**
     * Returns the compression type used for data output.
     *
     * @return  compression type
     */
    public CompressionCodecName getCompressionCodec() {
        return codec_;
    }

    /**
     * Sets the dictionary encoding flag.
     * If null, the library default is used.
     *
     * @param  useDict  true to use dictionary encoding,
     *                  false to use other methods
     */
    @ConfigMethod(
        property = "usedict",
        example = "false",
        sequence = 4,
        doc = "<p>Determines whether dictionary encoding is used for output.\n"
            + "This will work well to compress the output\n"
            + "for columns with a small number of distinct values.\n"
            + "Even when this setting is true,\n"
            + "dictionary encoding is abandoned once many values\n"
            + "have been encountered (the dictionary gets too big).\n"
            + "If no value is specified, the parquet-mr library default\n"
            + "is used, which is probably <code>true</code>.\n"
            + "</p>"
    )
    public void setDictionaryEncoding( Boolean useDict ) {
        useDict_ = useDict;
    }

    /**
     * Returns the dictionary encoding flag.
     *
     * @return  true to use dictionary encoding,
     *          false for other methods
     */
    public Boolean isDictionaryEncoding() {
        return useDict_;
    }

    /**
     * Sets the flag to indicate storing metadata in a dummy VOTable.
     *
     * @param  votMeta  true to store rich metadata as VOTable text
     */
    @ConfigMethod(
        property = "votmeta",
        sequence = 1,
        example = "false",
        doc = "<p>If true, rich metadata for the table will be written out\n"
            + "in the form of a DATA-less VOTable that is stored in the\n"
            + "parquet extra metadata key-value list under the key\n"
            + "<code>" + ParquetStarTable.VOTMETA_KEY + "</code>,\n"
            + "according to the\n"
            + "<webref url='https://www.ivoa.net/documents/Notes/VOParquet/'"
            + ">VOParquet convention</webref> (version 1.0).\n"
            + "This enables items such as Units, UCDs and column descriptions, "
            + "that would otherwise be lost in the serialization,\n"
            + "to be stored in the output parquet file.\n"
            + "This information can then be recovered by parquet readers\n"
            + "that understand this convention.\n"
            + "</p>"
    )
    public void setVOTableMetadata( boolean votMeta ) {
        votMeta_ = votMeta;
    }

    /**
     * Returns the flag that indicates storing metadata in a dummy VOTable.
     * See the VOParquet convention.
     *
     * @return  if true, rich metadata will be stored as VOTable text
     */
    public boolean isVOTableMetadata() {
        return votMeta_;
    }

    /**
     * Sets additional items for the key-value map in the parquet file footer.
     * Any items here will override whatever was going to be written otherwise.
     * Entries with a null value will delete the corresponding key.
     *
     * @param  kvItems   map of key-value pairs to be written to parquet footer
     */
    public void setKeyValueItems( Map<String,String> kvItems ) {
        kvItems_ = kvItems;
    }

    /**
     * Returns a map of additional items for the key-value map in the
     * parquet file footer.
     *
     * @return   additional key-value metadata items
     */
    public Map<String,String> getKeyValueItems() {
        return kvItems_;
    }

    /**
     * Calls setKeyValueItems.
     * But the KVMap argument means that this can be configured from a
     * user-supplied string value.
     *
     * @param  kvItems   additional key-value metadata items
     */
    @ConfigMethod(
        property = "kvmap",
        sequence = 3,
        example = "author:Messier",
        usage = "key1:value1;key2:value2;...",
        doc = "<p>Can be used to doctor the map of key-value metadata\n"
            + "stored in the parquet footer.\n"
            + "Map items are specified with a colon, like\n"
            + "<code>&lt;key&gt;:&lt;value&gt;</code>\n"
            + "and separated with a semicolon,\n"
            + "so for instance you could write\n"
            + "\"<code>kvmap=author:Messier;year:1774</code>\".\n"
            + "This will overwrite any map entries that would otherwise\n"
            + "have been written.\n"
            + "If a value starts with the at sign (\"<code>@</code>\")\n"
            + "it is interpreted as giving the name of a file\n"
            + "whose contents will be used instead of the literal value.\n"
            + "Specifying an empty entry will ensure it is not written\n"
            + "into the key=value list."
            + "</p>"
    )
    public void setKVMap( KVMap kvItems ) {
        setKeyValueItems( kvItems );
    }

    /**
     * Sets the version of VOTable used to write metadata, if any.
     *
     * @param  votVersion  preferred VOTable version, or null for default
     */
    public void setVOTableVersion( VOTableVersion votVersion ) {
        votVersion_ = votVersion;
    }

    /**
     * Returns the version of VOTable used to write metadata, if any.
     *
     * @return  preferred VOTable version, or null for default
     */
    public VOTableVersion getVOTableVersion() {
        return votVersion_;
    }

    /**
     * Map of key-value pairs.
     * Instance behaviour is inherited from LinkedHashMap.
     * The reason to have this as a separate class is that it has its
     * own {@link #valueOf} method, which can be used reflectively
     * to configure the output handler using a config parameter,
     * via {@link uk.ac.starlink.util.BeanConfig}.
     */
    public static class KVMap extends LinkedHashMap<String,String> {

        /**
         * Deserializes a string to a KVMap instance.
         * Syntax is "key1:value1;key2:value2,...".
         *
         * @param  txt   textual representation of map
         * @return  map instance
         */
        public static KVMap valueOf( String txt ) {
            KVMap map = new KVMap();
            String[] words = txt.split( ";", 0 );
            for ( String word : words ) {
                String[] kv = word.split( ":", 2 );
                if ( kv.length == 2 ) {
                    String key = kv[ 0 ];
                    String val = kv[ 1 ];
                    final String value;
                    if ( val.length() == 0 ) {
                        value = null;
                    }
                    else if ( val.charAt( 0 ) == '@' ) {
                        String fname = val.substring( 1 );
                        try {
                            value = readFile( fname );
                        }
                        catch ( IOException e ) {
                            throw new RuntimeException( "Failed to read file "
                                                      + fname, e );
                        }
                    }
                    else {
                        value = val;
                    }
                    map.put( key, value );
                }
                else {
                    String msg = new StringBuffer()
                       .append( "Don't understand \"" )
                       .append( word )
                       .append( "\", should be <key>:[<value>]" )
                       .toString();
                    throw new IllegalArgumentException( msg );
                }
            }
            return map;
        }

        /**
         * Reads a file as UTF-8.
         *
         * @param  fname  filename
         */
        private static String readFile( String fname ) throws IOException {
            try ( InputStream in = new FileInputStream( fname ) ) {
                return new String( IOUtils.readBytes( in, 1024 * 1024 ),
                                   StandardCharsets.UTF_8 );
            }
        }
    }
}
