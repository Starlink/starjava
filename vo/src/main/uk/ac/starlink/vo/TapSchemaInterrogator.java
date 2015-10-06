package uk.ac.starlink.vo;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.ContentCoding;

/**
 * Interrogates the TAP_SCHEMA tables from a TAP service to acquire
 * table metadata information.
 * In the current implementation, synchronous queries are used.
 *
 * @author   Mark Taylor
 * @since    6 Jun 2011
 * @see      <a href="http://www.ivoa.net/Documents/TAP/">TAP standard</a>
 */
public class TapSchemaInterrogator {

    private final URL serviceUrl_;
    private final Map<String,String> extraParams_;
    private final int maxrec_;
    private final ContentCoding coding_;

    /**
     * Acquires ForeignMeta.Link objects from TAP_SCHEMA.key_columns.
     * When reading a map, it is keyed by key_id.
     */
    public static final MetaQuerier<ForeignMeta.Link> LINK_QUERIER =
        createLinkQuerier();

    /**
     * Acquires ForeignMeta objects from TAP_SCHEMA.keys.
     * When reading a map, it is keyed by from_table.
     */
    public static final MetaQuerier<ForeignMeta> FKEY_QUERIER =
        createForeignKeyQuerier();

    /**
     * Acquires ColumnMeta objects from TAP_SCHEMA.columns.
     * When reading a map, it is keyed by table_name.
     */
    public static final MetaQuerier<ColumnMeta> COLUMN_QUERIER =
        createColumnQuerier();

    /**
     * Acquires TableMeta objects from TAP_SCHEMA.tables.
     * When reading a map, it is keyed by schema_name.
     */
    public static final MetaQuerier<TableMeta> TABLE_QUERIER =
        createTableQuerier();

    /**
     * Acquires SchemaMeta objects from TAP_SCHEMA.schemas.
     * It doesn't read maps.
     */
    public static final MetaQuerier<SchemaMeta> SCHEMA_QUERIER =
        createSchemaQuerier();

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructs an interrogator with explicit configuration.
     *
     * @param  serviceUrl  TAP base service URL
     * @param  maxrec  maximum number of records to retrieve per query
     * @param  coding  configures HTTP compression
     */
    public TapSchemaInterrogator( URL serviceUrl, int maxrec,
                                  ContentCoding coding ) {
        serviceUrl_ = serviceUrl;
        maxrec_ = maxrec;
        coding_ = coding;
        extraParams_ = new LinkedHashMap<String,String>();
        if ( maxrec > 0 ) {
            extraParams_.put( "MAXREC", Integer.toString( maxrec_ ) );
        }
    }

    /**
     * Returns the TAP service URL used by this interrogator.
     *
     * @return  service URL
     */
    public URL getServiceUrl() {
        return serviceUrl_;
    }

    /**
     * This convenience method returns an array of fully filled in
     * SchemaMeta objects describing the tables available from the service.
     *
     * @return  fully populated array of known schemas
     */
    public SchemaMeta[] queryMetadata() throws IOException {
        return readSchemas( true, true, true );
    }

    /**
     * Reads all schemas.
     * According to the options, the schemas may or may not have their
     * tables filled in, and those tables may or may not have their
     * columns and foreign key information filled in.
     *
     * @param  populateSchemas  if true, schemas will contain non-null
     *                          table lists
     * @param  populateTables   if true, tables will contain non-null
     *                          column/key lists
     *                          (ignored if populateSchemas is false)
     * @param  addOrphanTables  if true include tables whose schemas are not
     *                          explicitly declared
     * @return  schema list
     */
    public SchemaMeta[] readSchemas( boolean populateSchemas,
                                     boolean populateTables,
                                     boolean addOrphanTables )
            throws IOException {
        List<SchemaMeta> sList = SCHEMA_QUERIER.readList( this, null );
        if ( populateSchemas ) {
            Map<String,List<TableMeta>> tMap =
                TABLE_QUERIER.readMap( this, null );
            if ( populateTables ) {
                Map<String,List<ForeignMeta.Link>> lMap =
                    LINK_QUERIER.readMap( this, null );
                Map<String,List<ForeignMeta>> fMap =
                    FKEY_QUERIER.readMap( this, null );
                Map<String,List<ColumnMeta>> cMap =
                    COLUMN_QUERIER.readMap( this, null );
                for ( List<ForeignMeta> flist : fMap.values() ) {
                    for ( ForeignMeta fmeta : flist ) {
                        populateForeignKey( fmeta, lMap );
                    }
                }
                checkEmpty( lMap, "Links" );
                for ( List<TableMeta> tlist : tMap.values() ) {
                    for ( TableMeta tmeta : tlist ) {
                        populateTable( tmeta, fMap, cMap );
                    }
                }
                checkEmpty( fMap, "Foreign Keys" );
                checkEmpty( cMap, "Columns" );
            }
            for ( SchemaMeta smeta : sList ) {
                populateSchema( smeta, tMap );
            }

            /* It is possible that some tables are present with schema names
             * that do not appear in the schemas table.  If requested, add
             * these anyway. */
            if ( ! tMap.isEmpty() && addOrphanTables ) {
                logger_.warning( "Adding entries from phantom schemas: "
                               + tMap.keySet() );
                for ( Iterator<Map.Entry<String,List<TableMeta>>> entryIt =
                          tMap.entrySet().iterator(); entryIt.hasNext(); ) {
                    Map.Entry<String,List<TableMeta>> entry = entryIt.next();
                    entryIt.remove();
                    String sname = entry.getKey();
                    List<TableMeta> tlist = entry.getValue();
                    assert tlist != null;
                    SchemaMeta smeta = new SchemaMeta();
                    smeta.name_ = sname;
                    smeta.setTables( tlist.toArray( new TableMeta[ 0 ] ) );
                    sList.add( smeta );
                }
                assert tMap.isEmpty();
            }
            checkEmpty( tMap, "Tables" );
        }
        return sList.toArray( new SchemaMeta[ 0 ] );
    }

    /**
     * Reads a map of metadata items using a given MetaQuerier object.
     * The key of the map is the name of the parent object of the
     * map value list type.  See the documentation of the specific
     * querier object for details.
     *
     * <p>The form of the basic SELECT statement generated by this
     * call is "SELECT &lt;columns&Gt; FROM &lt;table&gt;".
     * If non-null the text of the <code>moreAdql</code> parameter
     * is appended (after a space), so it may be used to qualify the
     * query further.
     *
     * @param  mq   type-specific querier
     * @param  moreAdql   additional ADQL text to append after the
     *                    FROM clause (for example a WHERE clause);
     *                    may be null
     * @return  map from parent metadata item name to list of metadata items
     */
    public <T> Map<String,List<T>> readMap( MetaQuerier<T> mq, String moreAdql )
            throws IOException {
        return mq.readMap( this, moreAdql );
    }

    /**
     * Reads a list of metadata items using a given MetaQuerier object.
     *
     * <p>The form of the basic SELECT statement generated by this
     * call is "SELECT &lt;columns&Gt; FROM &lt;table&gt;".
     * If non-null the text of the <code>moreAdql</code> parameter
     * is appended (after a space), so it may be used to qualify the
     * query further.
     *
     * @param  mq   type-specific querier
     * @param  moreAdql   additional ADQL text to append after the
     *                    FROM clause (for example a WHERE clause);
     *                    may be null
     * @return  list of metadata items
     */
    public <T> List<T> readList( MetaQuerier<T> mq, String moreAdql )
            throws IOException {
        return mq.readList( this, moreAdql );
    }

    /**
     * Fills in link information for a ForeignMeta object.
     * Any relevant entries are removed from the supplied map.
     * If the map contains no relevant entries, an empty list is filled in.
     *
     * @param  fmeta  unpopulated foreign key item
     * @param  lMap  map acquired using {@link #LINK_QUERIER}
     */
    public void populateForeignKey( ForeignMeta fmeta,
                                    Map<String,List<ForeignMeta.Link>> lMap ) {
        List<ForeignMeta.Link> llist = lMap.remove( fmeta.getKeyId() );
        ForeignMeta.Link[] l0 = new ForeignMeta.Link[ 0 ];
        fmeta.setLinks( llist == null ? l0 : llist.toArray( l0 ) );
    }

    /**
     * Fills in foreign key and column information for a TableMeta object,
     * Any relevant entries are removed from the supplied maps.
     * Where the maps contain no relevant entries, an empty list is filled in.
     *
     * @param  tmeta  unpopulated table metadata item
     * @param  fMap  map acquired using {@link #FKEY_QUERIER}
     * @param  cMap  map acquired using {@link #COLUMN_QUERIER}
     */
    public void populateTable( TableMeta tmeta,
                               Map<String,List<ForeignMeta>> fMap,
                               Map<String,List<ColumnMeta>> cMap ) {
        String tname = tmeta.getName();
        List<ForeignMeta> flist = fMap.remove( tname );
        ForeignMeta[] f0 = new ForeignMeta[ 0 ];
        tmeta.setForeignKeys( flist == null ? f0 : flist.toArray( f0 ) );
        List<ColumnMeta> clist = cMap.remove( tname );
        ColumnMeta[] c0 = new ColumnMeta[ 0 ];
        tmeta.setColumns( clist == null ? c0 : clist.toArray( c0 ) );
    }

    /**
     * Fills in table information for a SchemaMeta object.
     * Any relevant entries are removed from the supplied map.
     * If the map contains no relevant entries, an empty list is filled in.
     *
     * @param  smeta  unpopulated schema metadata item
     * @param  tMap  map acquired using {@link #TABLE_QUERIER}
     */
    public void populateSchema( SchemaMeta smeta,
                                Map<String,List<TableMeta>> tMap ) {
        List<TableMeta> tlist = tMap.remove( smeta.getName() );
        TableMeta[] t0 = new TableMeta[ 0 ];
        smeta.setTables( tlist == null ? t0 : tlist.toArray( t0 ) );
    }

    /**
     * Constructs a TAP query for a given ADQL string.
     * May be overridden.
     *
     * @param  adql  query text
     * @return  query to execute
     */
    protected TapQuery createTapQuery( String adql ) {
        return new TapQuery( serviceUrl_, adql, extraParams_ );
    }

    /**
     * Performs an ADQL TAP query to this interrogator's service.
     * May be overridden.
     *
     * @param  tq   tap query
     * @return  output table
     */
    protected StarTable executeQuery( TapQuery tq ) throws IOException {
        return tq.executeSync( StoragePolicy.getDefaultPolicy(), coding_ );
    }

    /**
     * Checks that a metadata map (which should have been divested of
     * all its entries) is empty, and logs a warning if not.
     *
     * @param   map   map to test
     * @param   objType  name of the type of obect the map contains
     */
    private <T> void checkEmpty( Map<String,List<T>> map, String objType ) {
        int nEntry = map.size();
        if ( nEntry > 0 ) {
            logger_.warning( "Schema interrogation: " + nEntry + " orphaned "
                           + objType + " entries" );
            logger_.info( "Orphaned " + objType + "s: " + map.keySet() );
        }
    }

    /**
     * Indicates whether an TAP output value represents a boolean True.
     *
     * @param   entry   table entry
     * @return  true iff entry is a Number with a non-zero value
     */
    private static boolean isTrue( Object entry ) {
        return entry instanceof Number
            && ((Number) entry).intValue() != 0;
    }

    /**
     * Returns a MetaQuerier for reading ForeignMeta.Link objects.
     *
     * @return   link querier
     */
    private static MetaQuerier<ForeignMeta.Link> createLinkQuerier() {
        ColList lcList = new ColList();
        final int ilcFrom = lcList.addStringCol( "from_column" );
        final int ilcTarget = lcList.addStringCol( "target_column" );
        return new MetaQuerier<ForeignMeta.Link>( "TAP_SCHEMA.key_columns",
                                                  lcList, "key_id" ) {
            public ForeignMeta.Link createMeta( Object[] row ) {
                ForeignMeta.Link link = new ForeignMeta.Link();
                link.from_ = (String) row[ ilcFrom ];
                link.target_ = (String) row[ ilcTarget ];
                return link;
            }
        };
    }

    /**
     * Returns a MetaQuerier for reading ForeignMeta objects.
     *
     * @return  foreign key querier
     */
    private static MetaQuerier<ForeignMeta> createForeignKeyQuerier() {
        ColList fcList = new ColList();
        final int ifcId = fcList.addStringCol( "key_id" );
        final int ifcTarget = fcList.addStringCol( "target_table" );
        final int ifcDesc = fcList.addStringCol( "description" );
        final int ifcUtype = fcList.addStringCol( "utype" );
        return new MetaQuerier<ForeignMeta>( "TAP_SCHEMA.keys", fcList,
                                             "from_table" ) {
            public ForeignMeta createMeta( Object[] row ) {
                ForeignMeta fmeta = new ForeignMeta();
                fmeta.keyId_ = (String) row[ ifcId ];
                fmeta.targetTable_ = (String) row[ ifcTarget ];
                fmeta.description_ = (String) row[ ifcDesc ];
                fmeta.utype_ = (String) row[ ifcUtype ];
                return fmeta;
            }
        };
    }

    /**
     * Returns a MetaQuerier for reading ColumnMeta objects.
     *
     * @return   column querier
     */
    private static MetaQuerier<ColumnMeta> createColumnQuerier() {
        ColList ccList = new ColList();
        final int iccName = ccList.addStringCol( "column_name" );
        final int iccDesc = ccList.addStringCol( "description" );
        final int iccUnit = ccList.addStringCol( "unit" );
        final int iccUcd = ccList.addStringCol( "ucd" );
        final int iccUtype = ccList.addStringCol( "utype" );
        final int iccDatatype = ccList.addStringCol( "datatype" );
        final int iccIndexed = ccList.addOtherCol( "indexed" );
        final int iccPrincipal = ccList.addOtherCol( "principal" );
        final int iccStd = ccList.addOtherCol( "std" );
        return new MetaQuerier<ColumnMeta>( "TAP_SCHEMA.columns", ccList,
                                            "table_name" ) {
            public ColumnMeta createMeta( Object[] row ) {
                ColumnMeta cmeta = new ColumnMeta();
                cmeta.name_ = (String) row[ iccName ];
                cmeta.description_ = (String) row[ iccDesc ];
                cmeta.unit_ = (String) row[ iccUnit ];
                cmeta.ucd_ = (String) row[ iccUcd ];
                cmeta.utype_ = (String) row[ iccUtype ];
                cmeta.dataType_ = (String) row[ iccDatatype ];
                List<String> flagList = new ArrayList<String>();
                if ( isTrue( row[ iccIndexed ] ) ) {
                    flagList.add( "indexed" );
                }
                if ( isTrue( row[ iccPrincipal ] ) ) {
                    flagList.add( "principal" );
                }
                if ( isTrue( row[ iccStd ] ) ) {
                    flagList.add( "std" );
                }
                cmeta.flags_ = flagList.toArray( new String[ 0 ] );
                return cmeta;
            }
        };
    }

    /**
     * Returns a MetaQuerier for reading TableMeta objects.
     *
     * @return  table querier
     */
    private static MetaQuerier<TableMeta> createTableQuerier() {
        ColList tcList = new ColList();
        final int itcName = tcList.addStringCol( "table_name" );
        final int itcType = tcList.addStringCol( "table_type" );
        final int itcDesc = tcList.addStringCol( "description" );
        final int itcUtype = tcList.addStringCol( "utype" );
        return new MetaQuerier<TableMeta>( "TAP_SCHEMA.tables", tcList,
                                           "schema_name" ) {
            public TableMeta createMeta( Object[] row ) {
                TableMeta tmeta = new TableMeta();
                tmeta.name_ = (String) row[ itcName ];
                tmeta.type_ = (String) row[ itcType ];
                tmeta.description_ = (String) row[ itcDesc ];
                tmeta.utype_ = (String) row[ itcUtype ];
                return tmeta;
            }
        };
    }

    /**
     * Returns a MetaQuerier for reading SchemaMeta objects.
     *
     * @return   schema querier
     */
    private static MetaQuerier<SchemaMeta> createSchemaQuerier() {
        ColList scList = new ColList();
        final int iscName = scList.addStringCol( "schema_name" );
        final int iscDesc = scList.addStringCol( "description" );
        final int iscUtype = scList.addStringCol( "utype" );
        return new MetaQuerier<SchemaMeta>( "TAP_SCHEMA.schemas", scList,
                                            null ) {
            public SchemaMeta createMeta( Object[] row ) {
                SchemaMeta smeta = new SchemaMeta();
                smeta.name_ = (String) row[ iscName ];
                smeta.description_ = (String) row[ iscDesc ];
                smeta.utype_ = (String) row[ iscUtype ];
                return smeta;
            }
        };
    }

    /**
     * Object that can read a certain type T of TAP metadata object from
     * a table of a TAP_SCHEMA database table.
     * Instances are provided as static members of the enclosing
     * {@link TapSchemaInterrogator} class.
     */
    public static abstract class MetaQuerier<T> {

        final String tableName_;
        final CSpec[] atts_;
        final String parentColName_;

        /**
         * Constructor.
         *
         * @param  tableName  name of the TAP database table from the rows
         *                    of which each metadata item can be read
         * @param  attList   sequence of columns required when querying
         *                   the database table; each represents an attribute
         *                   of the constructed metadata item
         * @param  parentColName  name of the string-typed database column
         *                        that refers to the 'parent' object
         *                        of the constructed metadata items;
         *                        may be null
         */
        private MetaQuerier( String tableName, ColList attList,
                             String parentColName ) {
            tableName_ = tableName;
            atts_ = attList.getCols();
            parentColName_ = parentColName;
        }

        /**
         * Returns the name of the TAP database table from the rows of which
         * each metadata item can be read.
         *
         * @return  TAP_SCHEMA table name
         */
        public String getTableName() {
            return tableName_;
        }

        /**
         * Constructs a metadata item from a database row.
         *
         * @param  row  database query response row, for a query containing
         *              this querier's attribute list columns as at least
         *              the initial elements (there may be more after)
         * @return  metadata item constructed from <code>row</code> elements
         */
        abstract T createMeta( Object[] row );

        /**
         * Queries the database to retrieve a map of parent-name to
         * metadata object lists.
         *
         * @param  tsi  interrogator
         * @return  map of parent object name to lists of metadata items
         */
        Map<String,List<T>> readMap( TapSchemaInterrogator tsi,
                                     String moreAdql )
                throws IOException {
            List<CSpec> colList = new ArrayList<CSpec>();
            colList.addAll( Arrays.asList( atts_ ) );
            colList.add( new CSpec( parentColName_, true ) );
            int icParent = colList.size() - 1;
            StarTable table = query( tsi, colList, moreAdql );
            Map<String,List<T>> map = new LinkedHashMap<String,List<T>>();
            RowSequence rseq = table.getRowSequence();
            try {
                while ( rseq.next() ) {
                    Object[] row = rseq.getRow();
                    Object parentValue = row[ icParent ];
                    String key = parentValue == null ? null
                                                     : parentValue.toString();
                    T value = createMeta( row );
                    if ( ! map.containsKey( key ) ) {
                        map.put( key, new ArrayList<T>() );
                    }
                    map.get( key ).add( value );
                }
            }
            finally {
                rseq.close();
            }
            return map;
        }

        /**
         * Queries the database to retrieve a list of metadata objects.
         * This may optionally be further restricted to those with a given
         * value of parent item.
         *
         * @param  tsi  interrogator
         * @param  moreAdql   additional ADQL text to append after the
         *                    FROM clause (for example a WHERE clause);
         *                    may be null
         * @return  list of metadata items
         */
        List<T> readList( TapSchemaInterrogator tsi, String moreAdql )
                throws IOException {
            List<CSpec> colList = Arrays.asList( atts_ );
            StarTable table = query( tsi, colList, moreAdql );
            List<T> list = new ArrayList<T>();
            RowSequence rseq = table.getRowSequence();
            try {
                while ( rseq.next() ) {
                    list.add( createMeta( rseq.getRow() ) );
                }
            }
            finally {
                rseq.close();
            }
            return list;
        }

        /**
         * Executes a query for the named columns and returns the result
         * as a StarTable.
         *
         * @param  tsi    interrogator for which query will be done
         * @param  moreAdql   additional ADQL text to append after the
         *                    FROM clause (for example a WHERE clause);
         *                    may be null
         * @return   result
         */
        private StarTable query( TapSchemaInterrogator tsi, List<CSpec> colList,
                                 String moreAdql )
                throws IOException {
            StringBuffer sbuf = new StringBuffer();
            sbuf.append( "SELECT " );
            for ( Iterator<CSpec> it = colList.iterator(); it.hasNext(); ) {
                sbuf.append( it.next().name_ );
                if ( it.hasNext() ) {
                    sbuf.append( ", " );
                }
            }
            sbuf.append( " FROM " )
                .append( tableName_ );
            if ( moreAdql != null ) {
                sbuf.append( " " )
                    .append( moreAdql );
            }
            StarTable result =
                tsi.executeQuery( tsi.createTapQuery( sbuf.toString() ) );
            checkResultTable( result, colList );
            return result;
        }

        /**
         * Performs some checks on the resulting table.
         * If it is sufficiently unlike what's expected, an informative
         * IOException will be thrown.  Otherwise no action is taken.
         *
         * @param   result table  table to check
         */
        private void checkResultTable( StarTable table, List<CSpec> colList )
                throws IOException {
            int ncol = table.getColumnCount();
            if ( ncol != colList.size() ) {
                throw new IOException( "Schema query column count mismatch ("
                                     + ncol + " != " + colList.size() + " )" );
            }
            for ( int ic = 0; ic < ncol; ic++ ) {
                ColumnInfo info = table.getColumnInfo( ic );
                boolean isString = String.class
                                  .isAssignableFrom( info.getContentClass() );
                CSpec cspec = colList.get( ic );
                boolean mustBeString = cspec.isString_;
                if ( mustBeString && ! isString ) {
                    throw new IOException( "Schema query column type mismatch: "
                                         + info + " is not string type" );
                }
            }
        }
    }

    /**
     * Represents a list of columns in a TAP table which will be queried.
     */
    private static class ColList {

        private final List<CSpec> clist_;

        /**
         * Constructor.
         */
        ColList() {
            clist_ = new ArrayList<CSpec>();
        }

        /**
         * Returns the content of this list as an array of column specification
         * objects.
         *
         * @return   column specification objects
         */
        CSpec[] getCols() {
            return clist_.toArray( new CSpec[ 0 ] );
        }

        /**
         * Adds a new string-valued column to the list.
         * If the corresponding column in the result is not string-valued
         * an error will result.
         *
         * @param  name  column name in remote table
         * @return  index of column which has been added
         */
        int addStringCol( String name ) {
            return addCol( new CSpec( name, true ) );
        }

        /**
         * Adds a new non-string-valued column to the list.
         *
         * @param  name  column name in remote table
         * @return  index of column which has been added
         */
        int addOtherCol( String name ) {
            return addCol( new CSpec( name, false ) );
        }

        /**
         * Adds a new column specifier to the list.
         *
         * @param  cspec  column specifier
         * @return  index of column which has been added
         */
        private int addCol( CSpec cspec ) {
            clist_.add( cspec );
            return clist_.size() - 1;
        }
    }

    /**
     * Column specifier.
     * Aggregates name and rudimentary type information.
     */
    private static class CSpec {
        final String name_;
        final boolean isString_;

        /**
         * Constructor.
         *
         * @param  name  column name
         * @param  isString  true iff the column must be string valued
         */
        CSpec( String name, boolean isString ) {
            name_ = name;
            isString_ = isString;
        }
    }

    /**
     * Prints out tmetadata content of a given TAP service.
     *
     * @param  args  first element is TAP service URL
     */
    public static void main( String[] args ) throws IOException {
        String url = args[ 0 ];
        int maxrec = 100000;
        ContentCoding coding = ContentCoding.GZIP;
        SchemaMeta[] smetas =
            new TapSchemaInterrogator( new URL( url ), maxrec, coding )
           .readSchemas( true, true, true );
        for ( int is = 0; is < smetas.length; is++ ) {
            SchemaMeta smeta = smetas[ is ];
            System.out.println( "S " + is + ": " + smeta );
            TableMeta[] tmetas = smeta.getTables();
            if ( tmetas != null ) {
                for ( int it = 0; it < tmetas.length; it++ ) {
                    TableMeta tmeta = tmetas[ it ];
                    System.out.println( "\tT " + it + ": " + tmeta );
                    ColumnMeta[] cmetas = tmeta.getColumns();
                    if ( cmetas != null ) {
                        for ( int ic = 0; ic < cmetas.length; ic++ ) {
                            System.out.println( "\t\tC " + ic + ": "
                                              + cmetas[ ic ] );
                        }
                    }
                    ForeignMeta[] fmetas = tmeta.getForeignKeys();
                    if ( fmetas != null ) {
                        for ( int ik = 0; ik < fmetas.length; ik++ ) {
                            System.out.println( "\t\tF " + ik + ": "
                                              + fmetas[ ik ] );
                        }
                    }
                }
            }
        }
    }
}
