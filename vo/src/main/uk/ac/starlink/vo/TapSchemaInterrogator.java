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
    private final Map<String,StarTable> uploadMap_;
    private final int maxrec_;

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
        uploadMap_ = new HashMap<String,StarTable>();
        extraParams_ = new HashMap<String,String>();
        if ( maxrec > 0 ) {
            extraParams_.put( "MAXREC", Integer.toString( maxrec_ ) );
        }
    }

    /**
     * Returns an array of TableMeta objects describing the tables available
     * from the service.  The column and foreign key information is filled in.
     *
     * @return  array of known tables
     */
    public TableMeta[] queryMetadata() throws IOException {
        Map<String,List<ForeignMeta.Link>> lMap = readForeignLinks();
        Map<String,List<ForeignMeta>> fMap = readForeignKeys( lMap );
        Map<String,List<ColumnMeta>> cMap = readColumns();
        List<TableMeta> tList = readTables( cMap, fMap );
        return tList.toArray( new TableMeta[ 0 ] );
    }

    /**
     * Queries the TAP_SCHEMA.key_columns table to get a list of all foreign
     * key links.  The returned map associates key_id values with lists of
     * from->to column links.
     *
     * @return   map from key_id to link list
     */
    public Map<String,List<ForeignMeta.Link>> readForeignLinks()
            throws IOException {
        ColList lcList = new ColList();
        int ilcId = lcList.add( "key_id" );
        int ilcFrom = lcList.add( "from_column" );
        int ilcTarget = lcList.add( "target_column" );
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
     *               entries are removed as used
     * @return  map from table name to foreign key list
     */
    public Map<String,List<ForeignMeta>>
           readForeignKeys( Map<String,List<ForeignMeta.Link>> lMap )
            throws IOException {
        ColList fcList = new ColList();
        int ifcId = fcList.add( "key_id" );
        int ifcFrom = fcList.add( "from_table" );
        int ifcTarget = fcList.add( "target_table" );
        int ifcDesc = fcList.add( "description" );
        int ifcUtype = fcList.add( "utype" );
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
                if ( ! lMap.containsKey( kid ) ) {
                    lMap.put( kid, new ArrayList<ForeignMeta.Link>() );
                }
                fmeta.links_ =
                    lMap.remove( kid ).toArray( new ForeignMeta.Link[ 0 ] );
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
     * The returned map associates table names lists of columns.
     *
     * @return  map from table name to column list
     */
    public Map<String,List<ColumnMeta>> readColumns() throws IOException {
        ColList ccList = new ColList();
        int iccTable = ccList.add( "table_name" );
        int iccName = ccList.add( "column_name" );
        int iccDesc = ccList.add( "description" );
        int iccUnit = ccList.add( "unit" );
        int iccUcd = ccList.add( "ucd" );
        int iccUtype = ccList.add( "utype" );
        int iccDatatype = ccList.add( "datatype" );
        int iccIndexed = ccList.add( "indexed" );
        StarTable cTable = ccList.query( "TAP_SCHEMA.columns" );

        Map<String,List<ColumnMeta>> cMap =
            new LinkedHashMap<String,List<ColumnMeta>>();
        RowSequence cSeq = cTable.getRowSequence();
        String[] indexedFlags = new String[] { "indexed" };
        String[] notIndexedFlags = new String[ 0 ];
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
                cmeta.flags_ = (((Number) row[ iccIndexed ]).intValue() > 0)
                             ? indexedFlags : notIndexedFlags;
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
     *
     * @param  cMap  map of known columns keyed by table name,
     *               as returned by {@link #readColumns};
     *               entries are removed as used
     * @param  fMap  map of known foreign keys keyed by table name,
     *               as returned by {@link #readForeignKeys readForeignKeys};
     *               entries are removed as used
     * @return  list of tables
     */
    public List<TableMeta> readTables( Map<String,List<ColumnMeta>> cMap,
                                       Map<String,List<ForeignMeta>> fMap )
            throws IOException {
        ColList tcList = new ColList();
        int itcName = tcList.add( "table_name" );
        int itcType = tcList.add( "table_type" );
        int itcDesc = tcList.add( "description" );
        int itcUtype = tcList.add( "utype" );
        StarTable tTable = tcList.query( "TAP_SCHEMA.tables" );

        List<TableMeta> tList = new ArrayList<TableMeta>();
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
                if ( ! cMap.containsKey( tname ) ) {
                    cMap.put( tname, new ArrayList<ColumnMeta>() );
                }
                tmeta.columns_ =
                    cMap.remove( tname ).toArray( new ColumnMeta[ 0 ] );
                if ( ! fMap.containsKey( tname ) ) {
                    fMap.put( tname, new ArrayList<ForeignMeta>() );
                }
                tmeta.foreignKeys_ =
                    fMap.remove( tname ).toArray( new ForeignMeta[ 0 ] );
                tList.add( tmeta );
            }
        }
        finally {
            tSeq.close();
        }
        return tList;
    }

    /**
     * Performs an ADQL TAP query to this interrogator's service.
     *
     * @param  adql  ADQL query text
     * @return  output table
     */
    protected StarTable query( String adql ) throws IOException {
        TapQuery tq =
            new TapQuery( serviceUrl_, adql, extraParams_, uploadMap_, 0 );
        return tq.executeSync( StoragePolicy.getDefaultPolicy() );
    }

    /**
     * Represents a list of columns in a TAP table which will be queried.
     */
    private class ColList {
        private final List<String> list_;

        /**
         * Constructor.
         */
        ColList() {
            list_ = new ArrayList<String>();
        }

        /**
         * Adds a new column to the list.
         *
         * @param  name  column name in remote table
         * @return  index of column which has been added
         */
        int add( String name ) {
            list_.add( name );
            return list_.size() - 1;
        }

        /**
         * Executes a query for the named columns and returns the result
         * as a StarTable.
         *
         * @param  table  table name to query
         * @return   result
         */
        StarTable query( String table ) throws IOException {
            StringBuffer sbuf = new StringBuffer();
            sbuf.append( "SELECT " );
            for ( Iterator<String> it = list_.iterator(); it.hasNext(); ) {
                sbuf.append( it.next() );
                if ( it.hasNext() ) {
                    sbuf.append( ", " );
                }
            }
            sbuf.append( " FROM " )
                .append( table );
            return TapSchemaInterrogator.this.query( sbuf.toString() );
        }
    }

    public static void main( String[] args ) throws IOException {
        String url = args[ 0 ];
        TableMeta[] tmetas =
            new TapSchemaInterrogator( new URL( args[ 0 ] ), 100000 )
           .queryMetadata();
        for ( int it = 0; it < tmetas.length; it++ ) {
            TableMeta tmeta = tmetas[ it ];
            System.out.println( "T " + it + ":\t" + tmeta );
            ColumnMeta[] cmetas = tmeta.getColumns();
            for ( int ic = 0; ic < cmetas.length; ic++ ) {
                System.out.println( "\tC " + ic + ":\t" + cmetas[ ic ] );
            }
            ForeignMeta[] fmetas = tmeta.getForeignKeys();
            for ( int ik = 0; ik < fmetas.length; ik++ ) {
                System.out.println( "\tF " + ik + ":\t" + fmetas[ ik ] );
            }
        }
    }

}
