package uk.ac.starlink.parquet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.parquet.column.ColumnReadStore;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.column.impl.ColumnReadStoreImpl;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.io.api.Converter;
import org.apache.parquet.io.api.GroupConverter;
import org.apache.parquet.io.api.PrimitiveConverter;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.metadata.FileMetaData;
import org.apache.parquet.schema.MessageType;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.Bi;
import uk.ac.starlink.util.IOSupplier;

/**
 * Partial StarTable implementation based on a Parquet file.
 * This class provides metadata handling and preparation for data reading,
 * but does not implement actual data access methods.
 *
 * @author   Mark Taylor
 * @since    25 Feb 2021
 */
public abstract class ParquetStarTable extends AbstractStarTable {

    private final IOSupplier<ParquetFileReader> pfrSupplier_;
    private final MessageType schema_;
    private final String createdBy_;
    private final String votmeta_;
    private final long nrow_;
    private final int ncol_;
    private final ColumnInfo[] cinfos_;
    private final InputColumn<?>[] incols_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.parquet" );

    /** Parameter metadata for parquet creation info. */
    public static final ValueInfo CREATEDBY_INFO =
        new DefaultValueInfo( "Parquet_Created_By", String.class,
                              "Parquet library source for file" );

    /** Column metadata for dummy/unsupported columns. */
    public static final ValueInfo UNSUPPORTED_INFO =
        new DefaultValueInfo( "PARQUET_UNSUPPORTED", Boolean.class,
                              "If set, STIL reader cannot retrieve data" );

    /** Extra metadata key for table name. */
    public static final String NAME_KEY = "name";

    /** Namespace used in metadata keys for VOParquet convention things. */
    public static final String VOTMETA_NAMESPACE = "IVOA.VOTable-Parquet.";

    /** Extra metadata key for skeleton VOTable text. */
    public static final String VOTMETA_KEY = VOTMETA_NAMESPACE + "content";

    /** Extra metadata key for VOTable metadata version. */
    public static final String VOTMETAVERSION_KEY =
        VOTMETA_NAMESPACE + "version";

    /** Required value for VOTable metadata version. */
    public static final String REQUIRED_VOTMETAVERSION = "1.0";

    /**
     * Constructor.
     *
     * @param  pfrSupplier  access to parquet data file
     * @param  config  table reading options
     */
    @SuppressWarnings("this-escape")
    public ParquetStarTable( IOSupplier<ParquetFileReader> pfrSupplier,
                             Config config )
            throws IOException {
        pfrSupplier_ = pfrSupplier;

        /* Acquire per-file metadata. */
        Map<String,String> kvmap;
        try ( ParquetFileReader pfr = pfrSupplier.get() ) {
            FileMetaData fmeta = pfr.getFileMetaData();
            kvmap = fmeta.getKeyValueMetaData();
            schema_ = fmeta.getSchema();
            createdBy_ = fmeta.getCreatedBy();
            nrow_ = pfr.getRecordCount();
        }
        Map<String,String> metaMap = kvmap == null
                                   ? new HashMap<String,String>()
                                   : new LinkedHashMap<String,String>( kvmap );

        /* Record table name if stored. */
        String tname = metaMap.remove( NAME_KEY );
        if ( tname != null ) {
            setName( tname );
        }

        /* Record VOTable metadata if present. */
        votmeta_ = metaMap.remove( VOTMETA_KEY );
        String votmetaVersion = metaMap.remove( VOTMETAVERSION_KEY );
  
        /* Record remaining per-file metadata as parameters. */
        List<DescribedValue> params = getParameters();
        if ( createdBy_ != null ) {
            params.add( new DescribedValue( CREATEDBY_INFO, createdBy_ ) );
        }
        for ( Map.Entry<String,String> meta : metaMap.entrySet() ) {
            String pvalue = meta.getValue();
            if ( pvalue != null && pvalue.trim().length() > 0 ) {
                ValueInfo pinfo =
                    new DefaultValueInfo( meta.getKey(), String.class, null );
                params.add( new DescribedValue( pinfo, pvalue ) );
            }
        }

        /* Prepare per-column metadata and readers. */
        List<ColumnInfo> cinfos = new ArrayList<>();
        List<InputColumn<?>> incols = new ArrayList<>();
        int ic = 0;
        for ( String[] path : schema_.getPaths() ) {
            String cname = path[ 0 ];
            InputColumn<?> incol =
                InputColumns.createInputColumn( schema_, path );
            ColumnDescriptor cdesc = schema_.getColumnDescription( path );
            final Bi<InputColumn<?>,ColumnInfo> col;
            if ( incol != null ) {
                ColumnInfo cinfo =
                    new ColumnInfo( cname, incol.getContentClass(), null );
                cinfo.setNullable( incol.isNullable() );
                col = new Bi<InputColumn<?>,ColumnInfo>( incol, cinfo );
            }
            else if ( config.includeUnsupportedColumns() ) {
                incol = InputColumns.createUnsupportedColumn( cdesc );
                ColumnInfo cinfo =
                    new ColumnInfo( cname, incol.getContentClass(), null );
                cinfo.setNullable( true );
                cinfo.setElementSize( 0 );
                cinfo.setAuxDatum( new DescribedValue( UNSUPPORTED_INFO,
                                                       Boolean.TRUE ) );
                col = new Bi<InputColumn<?>,ColumnInfo>( incol, cinfo );
            }
            else {
                logger_.warning( "Omitting unsupported Parquet column "
                               + cdesc );
                col = null;
            }
            if ( col != null ) {
                incols.add( col.getItem1() );
                cinfos.add( col.getItem2() );
            }
            ic++;
        }
        assert ic == schema_.getFieldCount();
        incols_ = incols.toArray( new InputColumn<?>[ 0 ] );
        cinfos_ = cinfos.toArray( new ColumnInfo[ 0 ] );
        ncol_ = cinfos_.length;
    }

    public int getColumnCount() {
        return ncol_;
    }

    public long getRowCount() {
        return nrow_;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return cinfos_[ icol ];
    }

    /**
     * Returns a reader for this table's underlying file.
     *
     * @return   parquet file reader
     */
    public ParquetFileReader getParquetFileReader() throws IOException {
        return pfrSupplier_.get();
    }

    /**
     * Returns the schema of the the parquet file.
     *
     * @return   parquet schema
     */
    public MessageType getSchema() {
        return schema_;
    }

    /**
     * Returns the text content of a VOTable intended to supply
     * metadata for this table.
     *
     * @return   metadata VOTable text, or null
     */
    public String getVOTableMetadataText() {
        return votmeta_;
    }

    /**
     * Gets a ColumnReadStore from a PageReadStore.
     *
     * @param  pageStore  page store
     * @param  schema    schema for required data; this may for instance
     *                   define only a subset of available columns
     * @return   column store
     */
    public ColumnReadStore getColumnReadStore( PageReadStore pageStore,
                                               MessageType schema ) {

        /* We won't be using the GroupConverter-related methods of the
         * ColumnReaders (writeCurrentValueToConverter), so we can
         * supply a no-op GroupConverter. */
        GroupConverter groupConverter = new DummyGroupConverter();
        return new ColumnReadStoreImpl( pageStore, groupConverter, schema,
                                        createdBy_ );
    }

    /**
     * Returns the input column that can be used to read a given column
     * of this table.
     *
     * @param  icol  column index
     * @return   input column
     */
    public InputColumn<?> getInputColumn( int icol ) {
        return incols_[ icol ];
    }

    /**
     * Defines configuration options for reading parquet tables.
     */
    public interface Config {

        /**
         * Whether columns from the parquet table whose datatype is
         * unsupported by STIL will be included (with null values)
         * in the created ParquetStarTable, or simply ignored.
         * If included, they will contain the {@link #UNSUPPORTED_INFO} 
         * aux metadata item with value TRUE.
         *
         * @return  true to include unsupported column types, false to ignore
         */
        boolean includeUnsupportedColumns();
    }

    /**
     * GroupConverter implementation that doesn't do anything.
     */
    private static class DummyGroupConverter extends GroupConverter {
        public void start() {
        }
        public void end() {
        }
        public Converter getConverter( int iField ) {
            return new DummyPrimitiveConverter();
        }
    }

    /**
     * PrimitiveConverter implementation that doesn't do anything.
     */
    private static class DummyPrimitiveConverter
            extends PrimitiveConverter {
        public GroupConverter asGroupConverter() {
            return new DummyGroupConverter();
        }
    }
}
