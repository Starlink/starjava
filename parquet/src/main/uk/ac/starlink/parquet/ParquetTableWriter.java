package uk.ac.starlink.parquet;

import java.io.IOException;
import java.io.OutputStream;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.formats.DocumentedIOHandler;
import uk.ac.starlink.util.ConfigMethod;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;

/**
 * TableWriter implementation for output to Parquet format.
 *
 * @author   Mark Taylor
 * @since    25 Feb 2021
 */
public class ParquetTableWriter
        implements StarTableWriter, DocumentedIOHandler {

    private boolean groupArray_;
    private CompressionCodecName codec_;
    private Boolean useDict_;

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
     * primitive.  The latter way seems unnecessarily complicated to me,
     * but it seems to be what python writes.
     *
     * @param  groupArray   true for grouped arrays,
     *                      false for repeated primitives
     */
    @ConfigMethod(
        property = "groupArray",
        usage = "true|false",
        example = "false",
        doc = "<p>Controls the low-level detail of how array-valued columns\n"
            + "are written.\n"
            + "For an array-valued int32 column named IVAL,\n"
            + "<code>groupArray=false</code> will write it as\n"
            + "\"<code>repeated int32 IVAL</code>\"\n"
            + "while <code>groupArray=true</code> will write it as\n"
            + "\"<code>optional group IVAL (LIST) { repeated group list\n"
            + "{ optional int32 item} }</code>\".\n"
            + "I don't know why you'd want to do it the latter way,\n"
            + "but some other parquet writers seem to do that by default,\n"
            + "so there must be some good reason.\n"
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
        example = "gzip",
        usage = "uncompressed|snappy|gzip|lz4_raw",
        doc = "<p>Configures the type of compression used for output.\n"
            + "Supported values are probably\n"
            + "<code>uncompressed</code>, <code>snappy</code>,\n"
            + "<code>gzip</code> and <code>lz4_raw</code>.\n"
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
}
