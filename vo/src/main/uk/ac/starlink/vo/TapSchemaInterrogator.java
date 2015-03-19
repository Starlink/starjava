package uk.ac.starlink.vo;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;

/**
 * Interrogates the TAP_SCHEMA tables from a TAP service to acquire
 * table metadata information.
 * In the current implementation, synchronous queries are used.
 *
 * @see  <a href="http://www.ivoa.net/Documents/TAP/">TAP standard</a>
 */
public class TapSchemaInterrogator {

    private final URL serviceUrl_;
    private final Map<String,String> extraParams_;
    private final int maxrec_;

    private static final MetaQuerier<ForeignMeta.Link> LINK_QUERIER =
        createLinkQuerier();
    private static final MetaQuerier<ForeignMeta> FKEY_QUERIER =
        createForeignKeyQuerier();
    private static final MetaQuerier<ColumnMeta> COLUMN_QUERIER =
        createColumnQuerier();
    private static final MetaQuerier<TableMeta> TABLE_QUERIER =
        createTableQuerier();
    private static final MetaQuerier<SchemaMeta> SCHEMA_QUERIER =
        createSchemaQuerier();

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructs an interrogator with a default maxrec limit.
     *
     * @param  serviceUrl  TAP base service URL
     */
    public TapSchemaInterrogator( URL serviceUrl ) {
        this( serviceUrl, 100000 );
    }

    /**
     * Constructs an interrogator with a given maxrec limit.
     *
     * @param  serviceUrl  TAP base service URL
     * @param  maxrec  maximum number of records to retrieve per query
     */
    public TapSchemaInterrogator( URL serviceUrl, int maxrec ) {
        serviceUrl_ = serviceUrl;
        maxrec_ = maxrec;
        extraParams_ = new LinkedHashMap<String,String>();
        if ( maxrec > 0 ) {
            extraParams_.put( "MAXREC", Integer.toString( maxrec_ ) );
        }
    }

    /**
     * Returns an array of SchemaMeta objects describing the tables available
     * from the service.
     * The table, column and foreign key information is filled in.
     *
     * @return  fully populated array of known schemas
     */
    public SchemaMeta[] queryMetadata() throws IOException {
        Map<String,List<ForeignMeta.Link>> lMap = readForeignLinks();
        Map<String,List<ForeignMeta>> fMap = readForeignKeys( lMap );
        Map<String,List<ColumnMeta>> cMap = readColumns();
        Map<String,List<TableMeta>> tMap = readTables( cMap, fMap );
        List<SchemaMeta> sList = readSchemas( tMap, true );
        assert tMap.isEmpty();
        return sList.toArray( new SchemaMeta[ 0 ] );
    }

    /**
     * Queries the TAP_SCHEMA.key_columns table to get a list of all foreign
     * key links.  The returned map associates key_id values with lists of
     * from-&gt;to column links.
     *
     * @return   map from key_id to link list
     */
    public Map<String,List<ForeignMeta.Link>> readForeignLinks()
            throws IOException {
        return LINK_QUERIER.readMap( this );
    }

    /**
     * Queries the TAP_SCHEMA.keys table to get a list of all foreign keys.
     * The returned map associates from_table values with lists of foreign
     * keys.
     *
     * @param  lMap  map of known links keyed by key_id,
     *               as returned by {@link #readForeignLinks};
     *               entries are removed as used; may be null
     * @return  map from table name to foreign key list
     */
    public Map<String,List<ForeignMeta>>
           readForeignKeys( Map<String,List<ForeignMeta.Link>> lMap )
            throws IOException {
        Map<String,List<ForeignMeta>> fMap = FKEY_QUERIER.readMap( this );
        if ( lMap != null ) {
            ForeignMeta.Link[] links0 = new ForeignMeta.Link[ 0 ];
            for ( List<ForeignMeta> fmetaList : fMap.values() ) {
                for ( ForeignMeta fmeta : fmetaList ) {
                    String kid = fmeta.keyId_;
                    List<ForeignMeta.Link> lList = lMap.remove( kid );
                    fmeta.links_ = lList == null ? links0
                                                 : lList.toArray( links0 );
                }
            }
        }
        return fMap;
    }

    /**
     * Queries the TAP_SCHEMA.columns table to get a list of all columns.
     * The returned map associates table names with lists of columns.
     *
     * @return  map from table name to column list
     */
    public Map<String,List<ColumnMeta>> readColumns() throws IOException {
        return COLUMN_QUERIER.readMap( this );
    }

    /**
     * Queries the TAP_SCHEMA.tables table to get a list of all tables.
     * The returned map associates schema names with lists of tables.
     *
     * @param  cMap  map of known columns keyed by table name,
     *               as returned by {@link #readColumns};
     *               entries are removed as used; may be null
     * @param  fMap  map of known foreign keys keyed by table name,
     *               as returned by {@link #readForeignKeys readForeignKeys};
     *               entries are removed as used; may be null
     * @return  map from schema name to table list
     */
    public Map<String,List<TableMeta>>
            readTables( Map<String,List<ColumnMeta>> cMap,
                        Map<String,List<ForeignMeta>> fMap )
            throws IOException {
        Map<String,List<TableMeta>> tMap = TABLE_QUERIER.readMap( this );
        if ( cMap != null ) {
            ColumnMeta[] cols0 = new ColumnMeta[ 0 ];
            for ( List<TableMeta> tList : tMap.values() ) {
                for ( TableMeta tmeta : tList ) {
                    String tname = tmeta.getName();
                    List<ColumnMeta> cList = cMap.remove( tname );
                    tmeta.setColumns( cList == null ? cols0
                                                    : cList.toArray( cols0 ) );
                }
            }
        }
        if ( fMap != null ) {
            ForeignMeta[] fkeys0 = new ForeignMeta[ 0 ];
            for ( List<TableMeta> tList : tMap.values() ) {
                for ( TableMeta tmeta : tList ) {
                    String tname = tmeta.getName();
                    List<ForeignMeta> fList = fMap.remove( tname );
                    tmeta.setForeignKeys( fList == null
                                              ? fkeys0
                                              : fList.toArray( fkeys0 ) );
                }
            }
        }
        return tMap;
    }

    /**
     * Queries the TAP_SCHEMA.schemas table to get a list of all schemas.
     *
     * @param  tMap  map of known tables keyed by schema name,
     *               as returned by {@link #readTables};
     *               entries are removed as used; may be null
     * @param  addOrphans  if true, schema entries are faked for any schemas
     *                     referenced in the table map which are not read
     *                     from the database table; if false, those tables
     *                     will be retained in the table map on exit
     * @return   list of schemas
     */
    public List<SchemaMeta> readSchemas( Map<String,List<TableMeta>> tMap,
                                         boolean addOrphans )
            throws IOException {
        List<SchemaMeta> sList = SCHEMA_QUERIER.readList( this, null );
        if ( tMap != null ) {
            TableMeta[] tables0 = new TableMeta[ 0 ];
            for ( SchemaMeta smeta : sList ) {
                String sname = smeta.getName();
                List<TableMeta> tList = tMap.remove( sname );
                smeta.setTables( tList == null ? tables0 
                                               : tList.toArray( tables0 ) );
            }

            /* If the schemas referenced by some of the tables have not
             * been seen, fake schema entries if required. */
            if ( ! tMap.isEmpty() && addOrphans ) {
                logger_.warning( "Adding entries from phantom schemas: "
                               + tMap.keySet() );
                for ( String sname : tMap.keySet() ) {
                    SchemaMeta smeta = new SchemaMeta();
                    smeta.name_ = sname;
                    smeta.setTables( tMap.remove( sname )
                                         .toArray( new TableMeta[ 0 ] ) );
                    sList.add( smeta );
                }
            }
        }
        return sList;
    }

    /**
     * Constructs a TAP query for a given ADQL string.
     *
     * @param  adql  query text
     * @return  query to execute
     */
    protected TapQuery createTapQuery( String adql ) {
        return new TapQuery( serviceUrl_, adql, extraParams_ );
    }

    /**
     * Performs an ADQL TAP query to this interrogator's service.
     *
     * @param  tq   tap query
     * @return  output table
     */
    protected StarTable executeQuery( TapQuery tq ) throws IOException {
        return tq.executeSync( StoragePolicy.getDefaultPolicy() );
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
     */
    private static abstract class MetaQuerier<T> {

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
        MetaQuerier( String tableName, ColList attList, String parentColName ) {
            tableName_ = tableName;
            atts_ = attList.getCols();
            parentColName_ = parentColName;
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
        Map<String,List<T>> readMap( TapSchemaInterrogator tsi )
                throws IOException {
            List<CSpec> colList = new ArrayList<CSpec>();
            colList.addAll( Arrays.asList( atts_ ) );
            colList.add( new CSpec( parentColName_, true ) );
            int icParent = colList.size() - 1;
            StarTable table = query( tsi, colList, null );
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
         * @param  parentValue  if present, restricts query to only those
         *                      objects with a given parent;
         *                      if null, all objects are retrieved
         * @return  list of metadata items
         */
        List<T> readList( TapSchemaInterrogator tsi, String parentValue )
                throws IOException {
            List<CSpec> colList = Arrays.asList( atts_ );
            final String whereClause;
            if ( parentValue != null ) {
                whereClause = new StringBuffer()
                   .append( "WHERE " )
                   .append( parentColName_ )
                   .append( " = '" )
                   .append( parentValue )
                   .append( "'" )
                   .toString();
            }
            else {
                whereClause = null;
            }
            StarTable table = query( tsi, colList, whereClause );
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
        SchemaMeta[] smetas =
            new TapSchemaInterrogator( new URL( args[ 0 ] ), 100000 )
           .queryMetadata(); 
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
