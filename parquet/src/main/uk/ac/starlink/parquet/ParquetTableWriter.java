package uk.ac.starlink.parquet;

import java.io.IOException;
import java.io.OutputStream;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.formats.DocumentedIOHandler;
import uk.ac.starlink.util.ConfigMethod;
import uk.ac.starlink.votable.VOTableVersion;
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
    private boolean votMeta_;
    private VOTableVersion votVersion_;

    public ParquetTableWriter() {
        groupArray_ = true;
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
            "<p>At present, by default only rather limited metadata is written",
            "so that column units, descriptions, UCDs etc are lost.",
            "Setting the <code>votmeta</code> option below provides a way",
            "to remedy that, but this is currently experimental",
            "and may not be supported by other parquet I/O libraries.",
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

    /**
     * Sets the flag to indicate storing metadata in a dummy VOTable.
     *
     * @param  votMeta  true to store rich metadata as VOTable text
     */
    @ConfigMethod(
        property = "votmeta",
        example = "true",
        doc = "<p>If true, rich metadata for the table will be written out\n"
            + "in the form of a DATA-less VOTable that is stored in the\n"
            + "parquet extra metadata key-value list under the key\n"
            + "<code>" + ParquetStarTable.VOTMETA_KEY + "</code>.\n"
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
     *
     * @return  if true, rich metadata will be stored as VOTable text
     */
    public boolean isVOTableMetadata() {
        return votMeta_;
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
}
