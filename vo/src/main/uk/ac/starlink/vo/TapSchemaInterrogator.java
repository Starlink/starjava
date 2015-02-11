package uk.ac.starlink.vo;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
        extraParams_ = new HashMap<String,String>();
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
        ColList lcList = new ColList();
        int ilcId = lcList.addStringCol( "key_id" );
        int ilcFrom = lcList.addStringCol( "from_column" );
        int ilcTarget = lcList.addStringCol( "target_column" );
        StarTable lTable = lcList.query( "TAP_SCHEMA.key_columns" );

        Map<String,List<ForeignMeta.Link>> lMap =
            new LinkedHashMap<String,List<ForeignMeta.Link>>();
        RowSequence lSeq = lTable.getRowSequence();
        try {
            while ( lSeq.next() ) {
                Object[] row = lSeq.getRow();
                String kid = (String) row[ ilcId ];
                ForeignMeta.Link link = new ForeignMeta.Link();
                link.from_ = (String) row[ ilcFrom ];
                link.target_ = (String) row[ ilcTarget ];
                if ( ! lMap.containsKey( kid ) ) {
                    lMap.put( kid, new ArrayList<ForeignMeta.Link>() );
                }
                lMap.get( kid ).add( link );
            }
        }
        finally {
            lSeq.close();
        }
        return lMap;
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
        ColList fcList = new ColList();
        int ifcId = fcList.addStringCol( "key_id" );
        int ifcFrom = fcList.addStringCol( "from_table" );
        int ifcTarget = fcList.addStringCol( "target_table" );
        int ifcDesc = fcList.addStringCol( "description" );
        int ifcUtype = fcList.addStringCol( "utype" );
        StarTable fTable = fcList.query( "TAP_SCHEMA.keys" );

        Map<String,List<ForeignMeta>> fMap =
            new LinkedHashMap<String,List<ForeignMeta>>();
        RowSequence fSeq = fTable.getRowSequence();
        try {
            while ( fSeq.next() ) {
                Object[] row = fSeq.getRow();
                String tFromName = (String) row[ ifcFrom ];
                String kid = (String) row[ ifcId ];
                ForeignMeta fmeta = new ForeignMeta();
                fmeta.targetTable_ = (String) row[ ifcTarget ];
                fmeta.description_ = (String) row[ ifcDesc ];
                fmeta.utype_ = (String) row[ ifcUtype ];
                if ( lMap != null ) {
                    if ( ! lMap.containsKey( kid ) ) {
                        lMap.put( kid, new ArrayList<ForeignMeta.Link>() );
                    }
                    fmeta.links_ =
                        lMap.remove( kid ).toArray( new ForeignMeta.Link[ 0 ] );
                }
                if ( ! fMap.containsKey( tFromName ) ) {
                    fMap.put( tFromName, new ArrayList<ForeignMeta>() );
                }
                fMap.get( tFromName ).add( fmeta );
            }
        }
        finally {
            fSeq.close();
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
        ColList ccList = new ColList();
        int iccTable = ccList.addStringCol( "table_name" );
        int iccName = ccList.addStringCol( "column_name" );
        int iccDesc = ccList.addStringCol( "description" );
        int iccUnit = ccList.addStringCol( "unit" );
        int iccUcd = ccList.addStringCol( "ucd" );
        int iccUtype = ccList.addStringCol( "utype" );
        int iccDatatype = ccList.addStringCol( "datatype" );
        int iccIndexed = ccList.addOtherCol( "indexed" );
        int iccPrincipal = ccList.addOtherCol( "principal" );
        int iccStd = ccList.addOtherCol( "std" );
        StarTable cTable = ccList.query( "TAP_SCHEMA.columns" );

        Map<String,List<ColumnMeta>> cMap =
            new LinkedHashMap<String,List<ColumnMeta>>();
        RowSequence cSeq = cTable.getRowSequence();
        try {
            while ( cSeq.next() ) {
                Object[] row = cSeq.getRow();
                String tname = (String) row[ iccTable ];
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
                if ( ! cMap.containsKey( tname ) ) {
                    cMap.put( tname, new ArrayList<ColumnMeta>() );
                }
                cMap.get( tname ).add( cmeta );
            }
        }
        finally {
            cSeq.close();
        }
        return cMap;
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
        ColList tcList = new ColList();
        int itcSchema = tcList.addStringCol( "schema_name" );
        int itcName = tcList.addStringCol( "table_name" );
        int itcType = tcList.addStringCol( "table_type" );
        int itcDesc = tcList.addStringCol( "description" );
        int itcUtype = tcList.addStringCol( "utype" );
        StarTable tTable = tcList.query( "TAP_SCHEMA.tables" );

        Map<String,List<TableMeta>> tMap =
            new LinkedHashMap<String,List<TableMeta>>();
        RowSequence tSeq = tTable.getRowSequence();
        try {
            while ( tSeq.next() ) {
                Object[] row = tSeq.getRow();
                TableMeta tmeta = new TableMeta();
                String tname = (String) row[ itcName ];
                tmeta.name_ = tname;
                tmeta.type_ = (String) row[ itcType ];
                tmeta.description_ = (String) row[ itcDesc ];
                tmeta.utype_ = (String) row[ itcUtype ];
                if ( cMap != null ) {
                    if ( ! cMap.containsKey( tname ) ) {
                        cMap.put( tname, new ArrayList<ColumnMeta>() );
                    }
                    tmeta.setColumns( cMap.remove( tname )
                                     .toArray( new ColumnMeta[ 0 ] ) );
                }
                if ( fMap != null ) {
                    if ( ! fMap.containsKey( tname ) ) {
                        fMap.put( tname, new ArrayList<ForeignMeta>() );
                    }
                    tmeta.setForeignKeys( fMap.remove( tname )
                                         .toArray( new ForeignMeta[ 0 ] ) );
                }
                String schema = (String) row[ itcSchema ];
                if ( ! tMap.containsKey( schema ) ) {
                    tMap.put( schema, new ArrayList<TableMeta>() );
                }
                tMap.get( schema ).add( tmeta );
            }
        }
        finally {
            tSeq.close();
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

        /* Query the database tables for schema information. */
        ColList scList = new ColList();
        int iscName = scList.addStringCol( "schema_name" );
        int iscDesc = scList.addStringCol( "description" );
        int iscUtype = scList.addStringCol( "utype" );
        StarTable sTable = scList.query( "TAP_SCHEMA.schemas" );

        /* Prepare a list of SchemaMeta object based on the results. */
        List<SchemaMeta> sList = new ArrayList<SchemaMeta>();
        RowSequence sSeq = sTable.getRowSequence();
        try {
            while ( sSeq.next() ) {
                Object[] row = sSeq.getRow();
                SchemaMeta smeta = new SchemaMeta();
                String sname = (String) row[ iscName ];
                smeta.name_ = sname;
                smeta.description_ = (String) row[ iscDesc ];
                smeta.utype_ = (String) row[ iscUtype ];
                if ( tMap != null ) {
                    if ( ! tMap.containsKey( sname ) ) {
                        tMap.put( sname, new ArrayList<TableMeta>() );
                    }
                    smeta.setTables( tMap.remove( sname )
                                         .toArray( new TableMeta[ 0 ] ) );
                }
                sList.add( smeta );
             }
         }
         finally {
             sSeq.close();
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
    private boolean isTrue( Object entry ) {
        return entry instanceof Number
            && ((Number) entry).intValue() != 0;
    }

    /**
     * Represents a list of columns in a TAP table which will be queried.
     */
    private class ColList {
        private final List<CSpec> list_;

        /**
         * Constructor.
         */
        ColList() {
            list_ = new ArrayList<CSpec>();
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
            list_.add( cspec );
            return list_.size() - 1;
        }

        /**
         * Executes a query for the named columns and returns the result
         * as a StarTable.
         *
         * @param  tname  table name to query
         * @return   result
         */
        StarTable query( String tname ) throws IOException {
            StringBuffer sbuf = new StringBuffer();
            sbuf.append( "SELECT " );
            for ( Iterator<CSpec> it = list_.iterator(); it.hasNext(); ) {
                sbuf.append( it.next().name_ );
                if ( it.hasNext() ) {
                    sbuf.append( ", " );
                }
            }
            sbuf.append( " FROM " )
                .append( tname );
            StarTable result =
                executeQuery( createTapQuery( sbuf.toString() ) );
            checkResultTable( result );
            return result;
        }

        /**
         * Performs some checks on the resulting table.
         * If it is sufficiently unlike what's expected, an informative
         * IOException will be thrown.  Otherwise no action is taken.
         *
         * @param   result table  table to check
         */
        private void checkResultTable( StarTable table ) throws IOException {
            int ncol = table.getColumnCount();
            if ( ncol != list_.size() ) {
                throw new IOException( "Schema query column count mismatch ("
                                     + ncol + " != " + list_.size() + " )" );
            }
            for ( int ic = 0; ic < ncol; ic++ ) {
                ColumnInfo info = table.getColumnInfo( ic );
                boolean isString = String.class
                                  .isAssignableFrom( info.getContentClass() );
                CSpec cspec = list_.get( ic );
                boolean mustBeString = cspec.isString_;
                if ( mustBeString && ! isString ) {
                    throw new IOException( "Schema query column type mismatch: "
                                         + info + " is not string type" );
                }
            }
        }

        /**
         * Column specifier.
         * Aggregates name and rudimentary type information.
         */
        private class CSpec {
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
    }

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
