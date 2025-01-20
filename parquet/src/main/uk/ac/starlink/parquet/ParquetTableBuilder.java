package uk.ac.starlink.parquet;

import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.table.formats.DocumentedTableBuilder;
import uk.ac.starlink.table.storage.AdaptiveByteStore;
import uk.ac.starlink.table.storage.MonitorStoragePolicy;
import uk.ac.starlink.util.ConfigMethod;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.IntList;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VODocument;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;
import uk.ac.starlink.votable.VOStarTable;

/**
 * TableBuilder for parquet files.
 * As well as reading basic Parquet data and metadata,
 * embedded metadata following the
 * <a href="https://www.ivoa.net/documents/Notes/VOParquet/"
 *    >VOParquet convention</a> is by default read and applied.
 *
 * @author   Mark Taylor
 * @since    25 Feb 2021
 */
public class ParquetTableBuilder extends DocumentedTableBuilder {

    private Boolean cacheCols_;
    private Boolean votMeta_;
    private int nThread_;
    private boolean tryUrl_;
    private String votableLoc_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.parquet" );

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
            readText( "parquet-format.xml" ),
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
            "<p>Parquet files typically do not contain rich metadata",
            "such as column units, descriptions, UCDs etc.",
            "To remedy that, this reader supports the",
            "<webref url='https://www.ivoa.net/documents/Notes/VOParquet/'",
            ">VOParquet convention</webref> (version 1.0),",
            "in which metadata is recorded in a DATA-less VOTable",
            "stored in the parquet file header.",
            "If such metadata is present it will by default be used,",
            "though this can be controlled using the <code>votmeta</code>",
            "configuration option below.",
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
        ParquetIO io = ParquetUtil.getIO();
        boolean useCache = useCache( datsrc, wantRandom, storage );

        /* We have to include unsupported columns in the first instance
         * so we can compare parquet and VOTable columns when (maybe)
         * trying to apply VOTable metadata.  Once that's done,
         * eliminate the unsupported columns before returning the result. */
        ParquetStarTable.Config config = new ParquetStarTable.Config() {
            public boolean includeUnsupportedColumns() {
                return true;
            }
        };
        ParquetStarTable parquetTable =
            io.readParquet( datsrc, this, config, useCache, tryUrl_ );
        if ( votableLoc_ != null ) {
            String votTxt = readUtf8FromLocation( votableLoc_ );
            parquetTable.setVOTableMetadataText( votTxt );
        }
        return hideUnsupportedColumns( applyVOTableMetadata( parquetTable ) );
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
     * Determines whether a DATA-less VOTable stored in input parquet file
     * will be used to supply metadata.
     * A null value (the default) uses such metadata if it is present.
     *
     * @param  votMeta  whether to read metadata from VOTable text
     */
    @ConfigMethod(
        property = "votmeta",
        example = "false",
        doc = "<p>If true, the content of the parquet extra metadata\n"
            + "key-value list item with key\n"
            + "<code>" + ParquetStarTable.VOTMETA_KEY + "</code>\n"
            + "will be read to supply the metadata for the input table,\n"
            + "following the\n"
            + "<webref url='https://www.ivoa.net/documents/Notes/VOParquet/'"
            + ">VOParquet convention</webref>.\n"
            + "If false, any such VOTable metadata is ignored.\n"
            + "If set null, the default, then such VOTable metadata\n"
            + "will be used only if it is present and apparently consistent\n"
            + "with the parquet data and metadata.\n"
            + "</p>"
    )
    public void setVOTableMetadata( Boolean votMeta ) {
        votMeta_ = votMeta;
    }

    /**
     * Indicates whether a DATA-less VOTable stored in input parquet file
     * will be used to supply metadata.
     * A null value (the default) uses such metadata if it is present.
     *
     * @return  whether to read metadata from VOTable text
     */
    public Boolean getVOTableMetadata() {
        return votMeta_;
    }

    /**
     * Sets the location of a data-less VOTable document that will be used
     * to supply additional metadata for a parquet table being read.
     * Normally not required, but if present it overrides any such metadata
     * VOTable embedded within the parquet file.
     * This value will only be used if {@link #getVOTableMetadata} returns
     * a non-FALSE value.
     *
     * @param  votableLoc  filename or URL of UTF-8-encoded data-less VOTable
     */
    @ConfigMethod(
        property = "votable",
        doc = "<p>Location of a UTF-8-encoded data-less VOTable\n"
            + "that will supply additional metadata for a parquet table\n"
            + "being read, according to the\n"
            + "<webref url='https://www.ivoa.net/documents/Notes/VOParquet/'"
            + ">VOParquet convention</webref>.\n"
            + "This is normally not required, but if present it overrides\n"
            + "any such metadata VOTable embedded within the parquet file.\n"
            + "This value will only be used if the <code>votmeta</code>\n"
            + "configuration is not false.\n"
            + "</p>",
        usage = "<filename-or-url>",
        example = "./metadata.vot"
    )
    public void setVOTableLocation( String votableLoc ) {
        votableLoc_ = votableLoc;
    }

    /**
     * Returns the location of a data-less VOTable document that will be used
     * to supply additional metadata for a parquet table being read.
     *
     * @return  override location for a UTF-8-encoded data-less VOTable
     */
    public String getVOTableLocation() {
        return votableLoc_;
    }

    /**
     * Reads all the characters from a given location into a string.
     * UTF-8 encoding is assumed.
     *
     * @param   loc  filname or URL
     * @return   content of stream
     */
    public static String readUtf8FromLocation( String loc ) throws IOException {
        try ( InputStream in = DataSource.getInputStream( loc, false ) ) {
            Reader rdr = new InputStreamReader( in, StandardCharsets.UTF_8 );
            int bufsiz = 10240;
            char[] buf = new char[ bufsiz ];
            StringBuffer sbuf = new StringBuffer();
            for ( int nc; ( nc = rdr.read( buf, 0, buf.length ) ) > 0; ) {
                sbuf.append( buf, 0, nc );
            }
            return sbuf.toString();
        }
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

    /**
     * Examines a ParquetStarTable and according to configuration
     * decorates it with metadata from an attached VOTable file.
     * See the VOParquet convention.
     *
     * @param  parquetTable  input table
     * @return   table possibly decoraed with VOTable metadata
     */
    private StarTable applyVOTableMetadata( ParquetStarTable parquetTable )
            throws TableFormatException {
        String votmetaTxt = parquetTable.getVOTableMetadataText();

        /* Return bare parquet table if appropriate. */
        if ( votmetaTxt == null || Boolean.FALSE.equals( votMeta_ ) ) {
            if ( Boolean.TRUE.equals( votMeta_ ) ) {
                throw new TableFormatException( "VOParquet: "
                                              + "No VOTable metadata found" );
            }
            else {
                return parquetTable;
            }
        }

        /* Otherwise try to decorate it with VOTable metadata,
         * according to VOParquet convention. */
        else {
            String failMsg;
            try {
                TableElement tabEl = readVOParquetTableElement( votmetaTxt );
                if ( tabEl == null ) {
                    failMsg = "No suitable TABLE element found";
                }
                else {
                    try {
                        VOStarTable votTable =
                            VOStarTable
                           .createDecoratedTable( parquetTable, tabEl );
                        return fixColumnTypes( parquetTable,  votTable );
                    }
                    catch ( IOException e ) {
                        failMsg = "Cannot reconcile VOTable metadata "
                                + "with parquet: " + e;
                    }
                }
            }
            catch ( IOException e ) {
                failMsg = "Failed to read VOTable metadata: " + e;
            }

            /* Metadata decoration didn't work: depending on configuration,
             * either fail, or log a warning and fall back to the
             * bare parquet table. */
            if ( Boolean.TRUE.equals( votMeta_ ) ) {
                throw new TableFormatException( "VOParquet: " + failMsg );
            }
            else {
                assert votMeta_ == null;
                logger_.warning( "VOParquet: " + failMsg );
                return parquetTable;
            }
        }
    }
    
    /**
     * Take an input table and remove any columns which have been marked
     * by the parquet reader as unsupported.
     *
     * @param  table  input table
     * @return   same or similar table, but with no unsupported columns
     */
    private StarTable hideUnsupportedColumns( StarTable table ) {
        IntList icolList = new IntList();
        for ( int icol = 0; icol < table.getColumnCount(); icol++ ) {
            ColumnInfo cinfo = table.getColumnInfo( icol );
            if ( Boolean.TRUE
                .equals( cinfo
                        .getAuxDatumValue( ParquetStarTable.UNSUPPORTED_INFO,
                                           Boolean.class ) ) ) {
                logger_.warning( "Ignoring parquet column with unsupported type"
                               + ": " + cinfo.getName() );
            }
            else {
                icolList.add( icol );
            }
        }
        if ( icolList.size() == table.getColumnCount() ) {
            return table;
        }
        else {
            return new ColumnPermutedStarTable( table, icolList.toIntArray(),
                                                true ) {
                public URL getURL() {
                    return table.getURL();
                }
            };
        }
    }

    /**
     * Locates the first TABLE element in a string
     * containing VOTable XML content.
     * It is supposed to be DATA-less; if it has a DATA child a warning
     * is logged but it gets returned anyway.
     *
     * @param  votTxt  VOTable document content
     * @return   the first TABLE element encountered
     * @throws  IOException  in case of trouble or if it can't be done
     */
    private static TableElement readVOParquetTableElement( String votTxt )
            throws IOException {
        VOElementFactory vofact = new VOElementFactory();
        final DOMSource domsrc;
        try {
            domsrc = vofact.transformToDOM( new StreamSource(
                                                new StringReader( votTxt ) ),
                                            false );
        }
        catch ( SAXException e ) {
            throw new TableFormatException( "VOTable parse failed", e );
        }
        VODocument doc = (VODocument) domsrc.getNode();
        VOElement topel = (VOElement) doc.getDocumentElement();
        NodeList tlist = topel.getElementsByVOTagName( "TABLE" );
        if ( tlist.getLength() > 0 ) {
            TableElement tableEl = (TableElement) tlist.item( 0 );
            if ( DOMUtils.getChildElementByName( tableEl, "DATA" ) != null ) {
                String msg = "VOParquet: first TABLE element has illegal "
                           + "DATA child (using it anyway)";
                logger_.warning( msg );
            }
            return tableEl;
        }
        else {
            throw new TableFormatException( "VOParquet: "
                                          + "no suitable TABLE element found" );
        }
    }

    /**
     * Attempts to return a table that resembles the supplied VOTable,
     * but corrected for consistency with the supplied parquet table.
     * The idea is that the VOTable has metadata which is rich and
     * should on the whole be preserved, but in case of inconsistency
     * between the two, the parquet metadata should override.
     * This is necessary since parquet metadata may correspond to
     * types that cannot be faithfully represented by the VOTable format
     * (for instance things related to signed/unsigned integers).
     *
     * @param  pt  parquet table, basic but reliable metadata
     * @param  vt  VOTable table, rich but possibly questionable metadata
     * @return  table with rich but reliable metadata
     * @throws  TableFormatException  if the two tables cannot be reconciled
     */
    private static StarTable fixColumnTypes( ParquetStarTable pt,
                                             VOStarTable vt )
            throws TableFormatException {

        /* Check basic consistency of columns. */
        int ncol = pt.getColumnCount();
        if ( vt.getColumnCount() != ncol ) {
            throw new TableFormatException( "Column count mismatch" );
        }

        /* Looks OK, assemble ColumnInfos that pick the rich metadata from
         * the VOTable but content classes from parquet. */
        final ColumnInfo[] cinfos = new ColumnInfo[ ncol ];
        int nMeta = 0;
        for ( int ic = 0; ic < ncol; ic++ ) {
            ColumnInfo vinfo = vt.getColumnInfo( ic );
            ColumnInfo pinfo = pt.getColumnInfo( ic );
            if ( vinfo.getName().equals( pinfo.getName() ) ) {
                ColumnInfo info = new ColumnInfo( vinfo );
                info.setContentClass( pinfo.getContentClass() );
                info.getAuxData().addAll( pinfo.getAuxData() );
                List<DomainMapper> dmappers = new ArrayList<>();
                dmappers.addAll( Arrays.asList( pinfo.getDomainMappers() ) );
                dmappers.addAll( Arrays.asList( vinfo.getDomainMappers() ) );
                info.setDomainMappers( dmappers.toArray( new DomainMapper[0]) );
                cinfos[ ic ] = info;
                nMeta++;
            }
            else {
                cinfos[ ic ] = pinfo;
                logger_.warning( "Name mismatch for column " + ( ic + 1 ) + ": "
                               + "\"" + pinfo.getName() + "\" != "
                               + "\"" + vinfo.getName() + "\", "
                               + "ignoring VOTable metadata" );
            }
        }

        /* Return a table with adjusted metadata.  URL is from original
         * table as read. */
        if ( nMeta > 0 ) {
            return new WrapperStarTable( vt ) {
                public URL getURL() {
                    return pt.getURL();
                }
                public ColumnInfo getColumnInfo( int ic ) {
                    return cinfos[ ic ];
                }
            };
        }
        else {
            return pt;
        }
    }
}
