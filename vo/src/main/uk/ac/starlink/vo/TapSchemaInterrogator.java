package uk.ac.starlink.vo;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
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

    private final TapService service_;
    private final Map<String,String> extraParams_;
    private final int maxrec_;
    private final ContentCoding coding_;
    private final Map<String,String[]> colsMap_;

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

    private static final AdqlSyntax syntax_ = AdqlSyntax.getInstance();

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructs an interrogator with explicit configuration.
     *
     * @param  service  TAP service description
     * @param  maxrec  maximum number of records to retrieve per query
     * @param  coding  configures HTTP compression
     */
    public TapSchemaInterrogator( TapService service, int maxrec,
                                  ContentCoding coding ) {
        service_ = service;
        maxrec_ = maxrec;
        coding_ = coding;
        extraParams_ = new LinkedHashMap<String,String>();
        if ( maxrec > 0 ) {
            extraParams_.put( "MAXREC", Integer.toString( maxrec_ ) );
        }
        colsMap_ = new HashMap<String,String[]>();
    }

    /**
     * Returns the TAP service used by this interrogator.
     *
     * @return  TAP service description
     */
    public TapService getTapService() {
        return service_;
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
                    SchemaMeta smeta = SchemaMeta.createDummySchema( sname );
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
     * call is "SELECT &lt;columns&gt; FROM &lt;table&gt;".
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
     * call is "SELECT &lt;columns&gt; FROM &lt;table&gt;".
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
        return new TapQuery( service_, adql, extraParams_ );
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
     * Returns the columns available in the TAP service for a given table.
     * The result may be cached from a previous invocation.
     *
     * @param  tableName  table name
     * @return   list of column names 
     */
    private String[] getAvailableColumns( String tableName )
            throws IOException {
        synchronized ( colsMap_ ) {
            if ( ! colsMap_.containsKey( tableName ) ) {
                colsMap_.put( tableName, readAvailableColumns( tableName ) );
            }
        }
        return colsMap_.get( tableName );
    }

    /**
     * Reads the columns available in the TAP service for a given table.
     *
     * @param  tableName  table name
     * @return   list of column names 
     */
    private String[] readAvailableColumns( String tableName )
            throws IOException {
        String adql = new StringBuffer()
            .append( "SELECT TOP 1 * FROM " )
            .append( tableName )
            .toString();
        Map<String,String> extraParams = new HashMap<String,String>();
        extraParams.put( "MAXREC", "1" );
        StarTable table =
            executeQuery( new TapQuery( service_, adql, extraParams ) );
        List<String> list = new ArrayList<String>();
        int ncol = table.getColumnCount();
        for ( int icol = 0; icol < ncol; icol++ ) {
            String name = table.getColumnInfo( icol ).getName();

            /* Avoid the 'size' or '"size"' column here.  This is mandated
             * for TAP_SCHEMA.columns in TAP 1.0, but it's a lot of trouble
             * to deal with because it's an ADQL reserved word.
             * In practice there is confusion about whether to delimit it
             * or not.  Moreover, at least since ADQL 1.1 it is deprecated
             * in favour of arraysize which contains more useful information. */
            if ( ! "size".equals( syntax_.unquote( name ).toLowerCase() ) ) {
                list.add( name );
            }
        }
        return list.toArray( new String[ 0 ] );
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
     * Returns an untyped object cast to String, or null if it has
     * some other run-time type.
     * 
     * @param  obj  object
     * @return  <code>((String) obj)</code> or null
     */
    private static String stringOrNull( Object obj ) {
        return obj instanceof String ? (String) obj : null;
    }

    /**
     * Returns a MetaQuerier for reading ForeignMeta.Link objects.
     *
     * @return   link querier
     */
    private static MetaQuerier<ForeignMeta.Link> createLinkQuerier() {
        final String cFromColumn;
        final String cTargetColumn;
        String[] atts = {
            cFromColumn = "from_column",
            cTargetColumn = "target_column",
        };
        return new MetaQuerier<ForeignMeta.Link>( "TAP_SCHEMA.key_columns",
                                                  atts, true, "key_id",
                                                  null, null ) {
            public ForeignMeta.Link createMeta( ColSet colset, Object[] row ) {
                ForeignMeta.Link link = new ForeignMeta.Link();
                link.from_ = colset.getCellString( cFromColumn, row );
                link.target_ = colset.getCellString( cTargetColumn, row );
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
        final String cKeyId;
        final String cTargetTable;
        final String cDescription;
        final String cUtype;
        String[] atts = {
            cKeyId = "key_id",
            cTargetTable = "target_table",
            cDescription = "description",
            cUtype = "utype",
        };
        return new MetaQuerier<ForeignMeta>( "TAP_SCHEMA.keys", atts, true,
                                             "from_table", null, null ) {
            public ForeignMeta createMeta( ColSet colset, Object[] row ) {
                ForeignMeta fmeta = new ForeignMeta();
                fmeta.keyId_ = colset.getCellString( cKeyId, row );
                fmeta.targetTable_ = colset.getCellString( cTargetTable, row );
                fmeta.description_ = colset.getCellString( cDescription, row );
                fmeta.utype_ = colset.getCellString( cUtype, row );
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
        final String cColumnName;
        final String cDescription;
        final String cUnit;
        final String cUcd;
        final String cUtype;
        final String cDatatype;
        final String cIndexed;
        final String cPrincipal;
        final String cStd;
        String[] attCols = {
            cColumnName = "column_name",
            cDescription = "description",
            cUnit = "unit",
            cUcd = "ucd",
            cUtype = "utype",
            cDatatype = "datatype",
            cIndexed = "indexed",
            cPrincipal = "principal",
            cStd = "std",
        };

        /* The following columns appear in TAP 1.1 but not 1.0, so it's
         * not a good idea to specify them as mandatory. */
        final String cColumnIndex = "column_index";
        final String cArraysize = "arraysize";
        final String cXtype = "xtype";
        final String[] flagAtts = { cIndexed, cPrincipal, cStd };
        return new MetaQuerier<ColumnMeta>( "TAP_SCHEMA.columns", attCols,
                                            false, "table_name",
                                            cColumnIndex, null ) {
            public ColumnMeta createMeta( ColSet colset, Object[] row ) {
                ColumnMeta cmeta = new ColumnMeta();
                cmeta.name_ = colset.getCellString( cColumnName, row );
                cmeta.description_ = colset.getCellString( cDescription, row );
                cmeta.unit_ = colset.getCellString( cUnit, row );
                cmeta.ucd_ = colset.getCellString( cUcd, row );
                cmeta.utype_ = colset.getCellString( cUtype, row );
                cmeta.dataType_ = colset.getCellString( cDatatype, row );
                List<String> flagList = new ArrayList<String>();
                for ( String flagAtt : flagAtts ) {
                    if ( colset.getCellBoolean( flagAtt, row ) ) {
                        flagList.add( flagAtt );
                    }
                }
                cmeta.flags_ = flagList.toArray( new String[ 0 ] );
                Map<String,Object> extras = colset.getExtras( row );
                extras.remove( cColumnIndex );
                cmeta.arraysize_ = stringOrNull( extras.remove( cArraysize ) );
                cmeta.xtype_ = stringOrNull( extras.remove( cXtype ) );
                cmeta.extras_ = extras;
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
        final String cTableName;
        final String cTableType;
        final String cDescription;
        final String cUtype;
        String[] attCols = {
            cTableName = "table_name",
            cTableType = "table_type",
            cDescription = "description",
            cUtype = "utype",
        };
        final String cNrows = "nrows";

        /* TAP 1.1 defines "table_index" as an optional display ordering
         * column, so maybe should use that.  But I'm inclined to stick
         * with alphabetic ordering here, since that's what you'll see
         * for other metadata read policies, and it's probably what
         * users expect. */
        final String cTableIndex = "table_index";
        String rankColName = null;
        return new MetaQuerier<TableMeta>( "TAP_SCHEMA.tables", attCols,
                                           false, "schema_name",
                                           rankColName, cTableName ) {
            public TableMeta createMeta( ColSet colset, Object[] row ) {
                TableMeta tmeta = new TableMeta();
                tmeta.name_ = colset.getCellString( cTableName, row );
                tmeta.type_ = colset.getCellString( cTableType, row );
                tmeta.description_ = colset.getCellString( cDescription, row );
                tmeta.utype_ = colset.getCellString( cUtype, row );
                tmeta.extras_ = colset.getExtras( row );
                Object nrows = tmeta.extras_.remove( cNrows );
                if ( nrows instanceof String || nrows instanceof Number ) {
                    tmeta.nrows_ = nrows.toString();
                }
                for ( Iterator<String> it = tmeta.extras_.keySet().iterator();
                      it.hasNext(); ) {
                    if ( cTableIndex.equalsIgnoreCase( it.next() ) ) {
                        it.remove();
                    }
                }
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
        final String cSchemaName;
        final String cDescription;
        final String cUtype;
        String[] attCols = {
            cSchemaName = "schema_name",
            cDescription = "description",
            cUtype = "utype",
        };
        return new MetaQuerier<SchemaMeta>( "TAP_SCHEMA.schemas", attCols,
                                            true, null, null, cSchemaName ) {
            public SchemaMeta createMeta( ColSet colset, Object[] row ) {
                SchemaMeta smeta = new SchemaMeta();
                smeta.name_ = colset.getCellString( cSchemaName, row );
                smeta.description_ = colset.getCellString( cDescription, row );
                smeta.utype_ = colset.getCellString( cUtype, row );
                return smeta;
            }
        };
    }

    /**
     * Object that can read a certain type of TAP metadata object from
     * a table of a TAP_SCHEMA database table.
     * Instances are provided as static members of the enclosing
     * {@link TapSchemaInterrogator} class.
     *
     * @param  <T>  metadata object type that this class can obtain
     */
    public static abstract class MetaQuerier<T> {

        final String tableName_;
        final String[] attCols_;
        final boolean queryStdOnly_;
        final String parentColName_;
        final String rankColName_;
        final String alphaColName_;
        final String[] attPlusParentCols_;

        /**
         * Constructor.
         *
         * @param  tableName  name of the TAP database table from the rows
         *                    of which each metadata item can be read
         * @param  attCols   standard columns representing metadata item
         *                   attributes with known semantics in the
         *                   TAP_SCHEMA table representing this object type;
         *                   ones not in this list and not the parentColName
         *                   count as "extra" metadata items
         * @param  queryStdOnly  if true, only the stdCols are queried,
         *                       and all must be present otherwise an error
         *                       will result (so they better be mandatory);
         *                       if false, all available columns in the
         *                       table are queried
         * @param  parentColName  name of the string-typed database column
         *                        that refers to the 'parent' object
         *                        of the constructed metadata items;
         *                        may be null
         * @param  rankColName  name of a column that, if it exists and is
         *                      numeric, gives a preferred display ordering
         *                      for this querier's metadata items; may be null
         * @param  alphaColName  name of a column that, if it exists and is
         *                       string-valued, provides an alphabetic display
         *                       ordering for this querier's metadata items
         *                       when rank is not available; may be null
         */
        private MetaQuerier( String tableName, String[] attCols,
                             boolean queryStdOnly, String parentColName,
                             String rankColName, String alphaColName ) {
            tableName_ = tableName;
            attCols_ = attCols;
            queryStdOnly_ = queryStdOnly;
            parentColName_ = parentColName;
            rankColName_ = rankColName;
            alphaColName_ = alphaColName;
            int natt = attCols_.length;
            List<String> appList = new ArrayList<String>();
            appList.addAll( Arrays.asList( attCols ) );
            if ( parentColName != null ) {
                appList.add( parentColName );
            }
            attPlusParentCols_ = appList.toArray( new String[ 0 ] );
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
         * @param  colset  describes the columns in the query
         * @param  row  database query response row
         * @return  metadata item constructed from <code>row</code> elements
         */
        abstract T createMeta( ColSet colset, Object[] row );

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
            String[] queryCols = queryStdOnly_
                               ? attPlusParentCols_
                               : tsi.getAvailableColumns( tableName_ );
            ColSet colset = new ColSet( queryCols, attPlusParentCols_ );
            StarTable table = query( tsi, colset, moreAdql );
            Map<String,List<RankedMeta>> rmap =
                new LinkedHashMap<String,List<RankedMeta>>();
            RowSequence rseq = table.getRowSequence();
            try {
                int iseq = 0;
                while ( rseq.next() ) {
                    Object[] row = rseq.getRow();
                    String key = colset.getCellString( parentColName_, row );
                    RankedMeta value = createRankedMeta( colset, row, iseq++ );
                    if ( ! rmap.containsKey( key ) ) {
                        rmap.put( key, new ArrayList<RankedMeta>() );
                    }
                    rmap.get( key ).add( value );
                }
            }
            finally {
                rseq.close();
            }
            Map<String,List<T>> map = new LinkedHashMap<String,List<T>>();
            for ( Map.Entry<String,List<RankedMeta>> entry : rmap.entrySet() ) {
                map.put( entry.getKey(), extractMetas( entry.getValue() ) );
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
            String[] queryCols = queryStdOnly_
                               ? attCols_
                               : tsi.getAvailableColumns( tableName_ );
            ColSet colset = new ColSet( queryCols, attPlusParentCols_ );
            StarTable table = query( tsi, colset, moreAdql );
            List<RankedMeta> rlist = new ArrayList<RankedMeta>();
            RowSequence rseq = table.getRowSequence();
            try {
                int iseq = 0;
                while ( rseq.next() ) {
                    rlist.add( createRankedMeta( colset, rseq.getRow(),
                                                 iseq++ ) );
                }
            }
            finally {
                rseq.close();
            }
            return extractMetas( rlist );
        }

        /**
         * Executes a query for the named columns and returns the result
         * as a StarTable.
         *
         * @param  tsi    interrogator for which query will be done
         * @param  colSet   describes columns to be queried
         * @param  moreAdql   additional ADQL text to append after the
         *                    FROM clause (for example a WHERE clause);
         *                    may be null
         * @return   result
         */
        private StarTable query( TapSchemaInterrogator tsi, ColSet colSet,
                                 String moreAdql )
                throws IOException {
            StringBuffer sbuf = new StringBuffer();
            sbuf.append( "SELECT " );
            String[] queryCols = colSet.querycols_;
            for ( int ic = 0; ic < queryCols.length; ic++ ) {
                if ( ic > 0 ) {
                    sbuf.append( ", " );
                }
                sbuf.append( queryCols[ ic ] );
            }
            sbuf.append( " FROM " )
                .append( tableName_ );
            if ( moreAdql != null ) {
                sbuf.append( " " )
                    .append( moreAdql );
            }
            StarTable result =
                tsi.executeQuery( tsi.createTapQuery( sbuf.toString() ) );
            checkResultTable( result, queryCols );
            return result;
        }

        /**
         * Performs some checks on the resulting table.
         * If it is sufficiently unlike what's expected, an informative
         * IOException will be thrown.  Otherwise no action is taken.
         *
         * @param   result table  table to check
         * @param   cols   columns expected to be present in table
         */
        private void checkResultTable( StarTable table, String[] cols )
                throws IOException {
            int ncol = table.getColumnCount();
            if ( ncol != cols.length ) {
                throw new IOException( "Schema query column count mismatch ("
                                     + ncol + " != " + cols.length + " )" );
            }
        }

        /**
         * Constructs a RankedMeta object from a database row.
         *
         * @param  colset  describes the columns in the query
         * @param  row  database query response row
         * @param  iseq   running index of object in this set
         * @return  metadata item constructed from <code>row</code> elements
         */
        private RankedMeta createRankedMeta( ColSet colset, Object[] row,
                                             int iseq ) {
            double rank = rankColName_ == null
                        ? Double.NaN
                        : colset.getCellDouble( rankColName_, row );
            String alpha = alphaColName_ == null
                         ? null
                         : colset.getCellString( alphaColName_, row );
            return new RankedMeta( createMeta( colset, row ),
                                   rank, alpha, iseq );
        }

        /**
         * Returns an (if possible sorted) list of metadata objects
         * from an unsorted collection of RankedMeta objects.
         *
         * @param  rlist  unsorted collection of ranked metadata objects
         * @return  sorted list of metadata objects
         */
        private List<T> extractMetas( List<RankedMeta> rlist ) {

            /* Sort the list as far as possible. */
            if ( rankColName_ != null || alphaColName_ != null ) {
                boolean hasRanks = false;
                boolean hasAlphas = false;
                for ( RankedMeta m : rlist ) {
                    hasRanks = hasRanks || ! Double.isNaN( m.rank_ );
                    hasAlphas = hasAlphas || m.alpha_ != null;
                }
                if ( hasRanks || hasAlphas ) {
                    Collections.sort( rlist );
                }
            }

            /* Extract the contents to an unranked list and return. */
            List<T> mlist = new ArrayList<T>( rlist.size() );
            for ( RankedMeta r : rlist ) {
                mlist.add( r.meta_ );
            }
            return mlist;
        }

        /**
         * Packages instances of this querier's metadata items along with
         * ordering information; a possible rank value and a possible
         * alphanumeric label.  Either or both may be blank.
         */
        private class RankedMeta implements Comparable<RankedMeta> {
            final T meta_;
            final double rank_;
            final String alpha_;
            final int iseq_;

            /**
             * Constructor.
             *
             * @param  meta  metadata item
             * @param  rank  numeric indication of ordering; NaN if unknown
             * @param  alpha  alphabetic indication of ordering; null if unknown
             * @param  iseq  running sequence number; used to resolve conflicts
             *               if rank and alpha are tied
             */
            RankedMeta( T meta, double rank, String alpha, int iseq ) {
                meta_ = meta;
                rank_ = rank;
                alpha_ = alpha;
                iseq_ = iseq;
            }

            /**
             * Implements Comparable interface.
             * Rank is considered before alpha.
             */
            public int compareTo( RankedMeta other ) {
                int rc = (int) Math.signum( toComparable( this.rank_ )
                                          - toComparable( other.rank_ ) );
                if ( rc != 0 ) {
                    return rc;
                }
                if ( this.alpha_ != null || other.alpha_ != null ) {
                    if ( this.alpha_ == null ) {
                        return +1;
                    }
                    else if ( other.alpha_ == null ) {
                        return -1;
                    }
                    else {
                        return this.alpha_.compareTo( other.alpha_ );
                    }
                }
                int sc = (int) Math.signum( this.iseq_ - other.iseq_ );
                if ( sc != 0 ) {
                    return sc;
                }
                return (int) Math.signum( this.hashCode() - other.hashCode() );
            }

            /**
             * Makes sure a numeric value is usable for numeric comparison.
             * Maps NaN to a large number.
             *
             * @param  rank  raw value
             * @return   sanitised value
             */
            private double toComparable( double rank ) {
                return Double.isNaN( rank ) ? 0.25 * Double.MAX_VALUE : rank;
            }
        }
    }

    /**
     * Describes a set of columns to be queried in an ADQL query.
     */
    private static class ColSet {
        final Map<String,Integer> icolMap_;
        final String[] querycols_;
        final String[] extraCols_;

        /**
         * Constructor.
         *
         * @param  queryCols  names of all columns queried
         * @param  stdCols  names of columns with known semantics;
         *                  any others will be made available as "extras"
         */
        ColSet( String[] queryCols, String[] stdCols ) {
            querycols_ = toLowers( queryCols );
            Collection<String> stdcols =
                new HashSet<String>( Arrays.asList( toLowers( stdCols ) ) );
            icolMap_ = new HashMap<String,Integer>();
            List<String> extrasList = new ArrayList<String>();
            int nc = querycols_.length;
            for ( int ic = 0; ic < nc; ic++ ) {
                String col = querycols_[ ic ];
                icolMap_.put( col, new Integer( ic ) );
                if ( ! stdcols.contains( col ) ) {
                    extrasList.add( queryCols[ ic ] );
                }
            }
            extraCols_ = extrasList.toArray( new String[ 0 ] );
        }

        /**
         * Returns a map of any non-standard items recovered from the query.
         * This is all the columns actually present except those named as
         * standard in the constructor.  Only non-blank items are included
         * in the returned map.
         *
         * @param  row   row from query result
         * @return   map from column name to value in row
         */
        Map<String,Object> getExtras( Object[] row ) {
            Map<String,Object> map = new LinkedHashMap<String,Object>();
            for ( String col : extraCols_ ) {
                Object obj = getCellObject( col, row );
                if ( ! Tables.isBlank( obj ) ) {
                    map.put( col, obj );
                }
            }
            return map;
        }

        /**
         * Returns the value of the cell in a given row of a named column.
         *
         * @param  colname   column name (not case sensitive)
         * @param  row   row from query result
         * @return   value of named column in row, null if can't do it
         */
        Object getCellObject( String colname, Object[] row ) {
            Integer icol = icolMap_.get( colname.toLowerCase() );
            return icol == null ? null : row[ icol.intValue() ];
        }

        /**
         * Returns the string value of the cell in a given row of
         * a named column.  In general the column name should refer to
         * a string-valued column, but no error will result if it doesn't.
         *
         * @param  colname   column name (not case sensitive)
         * @param  row   row from query result
         * @return   string value of named column in row, null if can't do it
         */
        String getCellString( String colname, Object[] row ) {
            Object obj = getCellObject( colname, row );
            return obj == null ? null : obj.toString();
        }

        /**
         * Returns the value of the cell in a given row of a named
         * column interpreted as a boolean.  In TAP terms this means
         * a non-zero integer value.  So the column should be numeric,
         * but no error will result if it isn't.
         *
         * @param  colname   column name (not case sensitive)
         * @param  row   row from query result
         * @return   boolean value of named column in row, false if can't do it
         */
        boolean getCellBoolean( String colname, Object[] row ) {
            Object obj = getCellObject( colname, row );
            return obj instanceof Number
                && ((Number) obj).intValue() != 0;
        }

        /**
         * Returns the value of the cell in a given row of a named column
         * interpreted as a double.  If the column is not numeric or
         * doesn't exist, NaN is returned.
         *
         * @param  colname   column name (not case sensitive)
         * @param  row   row from query result
         * @return   numeric value of named column in row, NaN if can't do it
         */
        double getCellDouble( String colname, Object[] row ) {
            Object obj = getCellObject( colname, row );
            return obj instanceof Number
                 ? ((Number) obj).doubleValue() : Double.NaN;
        }

        /**
         * Utility method to convert an array of strings to its
         * lower-cased equivalent.
         *
         * @param  ins  input list
         * @return   output list (same length)
         */
        private static String[] toLowers( String[] ins ) {
            int n = ins.length;
            String[] outs = new String[ n ];
            for ( int i = 0; i < n; i++ ) {
                outs[ i ] = ins[ i ].toLowerCase();
            }
            return outs;
        }
    }

    /**
     * Prints out metadata content of a given TAP service.
     *
     * @param  args  first element is TAP service URL
     */
    public static void main( String[] args ) throws IOException {
        String url = args[ 0 ];
        TapService service =
            TapServices.createDefaultTapService( new URL( url ) );
        int maxrec = 100000;
        ContentCoding coding = ContentCoding.GZIP;
        SchemaMeta[] smetas =
            new TapSchemaInterrogator( service, maxrec, coding )
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
