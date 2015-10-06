package uk.ac.starlink.vo;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.ContentCoding;

/**
 * TapMetaReader implementation that uses TAP queries on the TAP_SCHEMA
 * schema to acquire table metadata.
 *
 * @author   Mark Taylor
 * @since    18 Mar 2015
 */
public class TapSchemaTapMetaReader implements TapMetaReader {

    private final TapSchemaInterrogator tsi_;
    private final boolean populateSchemas_;
    private final boolean populateTables_;
    private final MetaNameFixer fixer_;
    private final boolean addOrphanTables_;
    private final Object fkeyReadLock_;
    private Map<String,List<ForeignMeta>> fkeyMap_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     *
     * @param  serviceUrl  TAP service URL
     * @param  maxrec   maximum number of records to be requested at once
     * @param  coding  configures HTTP compression
     * @param  populateSchemas   whether SchemaMeta objects will be
     *                           filled in with table lists when they are
     *                           acquired
     * @param  populateTables   whether TableMeta objects will be
     *                          filled in with column and foreign key lists
     *                          when they are acquired
     * @param  fixer  object that fixes up syntactically incorrect
     *                table/column names; if null no fixing is done;
     *                has no effect for compliant TAP_SCHEMA services
     * @param  preloadFkeys  if true, all foreign key info is loaded in one go,
     *                       if false it's read per-table as required
     */
    public TapSchemaTapMetaReader( String serviceUrl, int maxrec,
                                   ContentCoding coding,
                                   boolean populateSchemas,
                                   boolean populateTables,
                                   MetaNameFixer fixer,
                                   boolean preloadFkeys ) {
        final URL url;
        try {
            url = new URL( serviceUrl );
        }
        catch ( MalformedURLException e ) {
            throw (IllegalArgumentException)
                  new IllegalArgumentException( "Bad URL: " + serviceUrl )
                 .initCause( e );
        }
        tsi_ = new TapSchemaInterrogator( url, maxrec, coding ) {
            @Override
            protected StarTable executeQuery( TapQuery tq ) throws IOException {
                logger_.info( tq.getAdql() );
                return super.executeQuery( tq );
            }
        };
        populateSchemas_ = populateSchemas;
        populateTables_ = populateTables;
        fixer_ = fixer == null ? MetaNameFixer.NONE : fixer;
        fkeyReadLock_ = preloadFkeys ? new Object() : null;
        addOrphanTables_ = true;
    }

    public String getSource() {
        return tsi_.getServiceUrl().toString();
    }

    public String getMeans() {
        List<String> preList = new ArrayList<String>();
        preList.add( "schemas" );
        if ( populateSchemas_ ) {
            preList.add( "tables" );
            if ( populateTables_ ) {
                preList.add( "columns" );
                preList.add( "fkeys" );
            }
        }
        if ( ! preList.contains( "fkeys" ) && fkeyReadLock_ != null ) {
            preList.add( "fkeys" );
        }
        StringBuffer ibuf = new StringBuffer();
        for ( String item : preList ) {
            if ( ibuf.length() != 0 ) {
                ibuf.append( ", " );
            }
            ibuf.append( item );
        }
        return "TAP_SCHEMA queries; preload " + ibuf.toString();
    }

    public SchemaMeta[] readSchemas() throws IOException {
        SchemaMeta[] schemas = 
            tsi_.readSchemas( populateSchemas_, populateTables_,
                              addOrphanTables_ );
        fixer_.fixSchemas( schemas );
        sortSchemas( schemas );
        for ( SchemaMeta smeta : schemas ) {
            TableMeta[] tmetas = smeta.getTables();
            if ( tmetas != null ) {
                sortTables( tmetas );
            }
        }
        return schemas;
    }

    public TableMeta[] readTables( SchemaMeta schema ) throws IOException {
        String whereClause = "schema_name = '" + schema.getName() + "'";
        List<TableMeta> tableList =
            tsi_.readList( TapSchemaInterrogator.TABLE_QUERIER, whereClause );
        if ( populateTables_ ) {
            Map<String,List<ForeignMeta.Link>> lMap =
                tsi_.readMap( TapSchemaInterrogator.LINK_QUERIER,
                              "NATURAL JOIN TAP_SCHEMA.keys "
                            + "JOIN TAP_SCHEMA.tables "
                            + "ON from_table = table_name "
                            + whereClause );
            Map<String,List<ForeignMeta>> fMap =
                tsi_.readMap( TapSchemaInterrogator.FKEY_QUERIER,
                              "JOIN TAP_SCHEMA.tables "
                            + "ON from_table = table_name "
                            + whereClause );
            for ( List<ForeignMeta> flist : fMap.values() ) {
                for ( ForeignMeta fmeta : flist ) {
                    tsi_.populateForeignKey( fmeta, lMap );
                }
            }
            checkEmpty( lMap, "Foreign links" );
            Map<String,List<ColumnMeta>> cMap =
                tsi_.readMap( TapSchemaInterrogator.COLUMN_QUERIER,
                              "NATURAL JOIN TAP_SCHEMA.tables " + whereClause );
            for ( TableMeta tmeta : tableList ) {
                tsi_.populateTable( tmeta, fMap, cMap );
            }
            checkEmpty( fMap, "Foreign keys" );
            checkEmpty( cMap, "Columns" );
        }
        TableMeta[] tables = tableList.toArray( new TableMeta[ 0 ] );
        fixer_.fixTables( tables, schema );
        sortTables( tables );
        return tables;
    }

    public ColumnMeta[] readColumns( TableMeta table ) throws IOException {
        String whereClause =
            "WHERE table_name = '" + fixer_.getOriginalTableName( table ) + "'";
        ColumnMeta[] columns =
            tsi_.readList( TapSchemaInterrogator.COLUMN_QUERIER, whereClause )
           .toArray( new ColumnMeta[ 0 ] );
        fixer_.fixColumns( columns );
        return columns;
    }

    public ForeignMeta[] readForeignKeys( TableMeta table ) throws IOException {
        String tname = fixer_.getOriginalTableName( table );
        final List<ForeignMeta> fkeyList;

        /* If we are preloading the foreign keys, make sure all the
         * foreign key information is required, then pull out the
         * one we need. */
        if ( fkeyReadLock_ != null ) {
            synchronized ( fkeyReadLock_ ) {
                if ( fkeyMap_ == null ) {

                    /* Lazily load foreign key information if required. */
                    Map<String,List<ForeignMeta.Link>> lMap =
                        tsi_.readMap( TapSchemaInterrogator.LINK_QUERIER,
                                      null );
                    Map<String,List<ForeignMeta>> fMap =
                        tsi_.readMap( TapSchemaInterrogator.FKEY_QUERIER,
                                      null );
                    for ( List<ForeignMeta> flist : fMap.values() ) {
                        for ( ForeignMeta fmeta : flist ) {
                            tsi_.populateForeignKey( fmeta, lMap );
                        }
                    }
                    checkEmpty( lMap, "Links" );
                    fkeyMap_ = fMap;
                }
            }
            fkeyList = fkeyMap_.get( tname );
        }

        /* Otherwise, query the TAP service for the required information
         * directly. */
        else {
            String whereClause = "WHERE from_table = '" + tname + "'";
            Map<String,List<ForeignMeta.Link>> lMap =
                tsi_.readMap( TapSchemaInterrogator.LINK_QUERIER,
                              "NATURAL JOIN TAP_SCHEMA.keys "
                            + whereClause );
            fkeyList = tsi_.readList( TapSchemaInterrogator.FKEY_QUERIER,
                                      whereClause );
            for ( ForeignMeta fmeta : fkeyList ) {
                tsi_.populateForeignKey( fmeta, lMap );
            }
            checkEmpty( lMap, "Foreign links" );
        }

        /* Return the result. */
        return fkeyList == null ? new ForeignMeta[ 0 ]
                                : fkeyList.toArray( new ForeignMeta[ 0 ] );
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
     * Sorts an array of schemas in place by schema name.
     *
     * @param  smetas  schema array
     */
    static void sortSchemas( SchemaMeta[] smetas ) {
        Arrays.sort( smetas, new Comparator<SchemaMeta>() {
            public int compare( SchemaMeta s1, SchemaMeta s2 ) {
                return getSchemaName( s1 ).compareTo( getSchemaName( s2 ) );
            }
            private String getSchemaName( SchemaMeta smeta ) {
                String name = smeta.getName();
                return name == null ? "" : name;
            }
        } );
    }

    /**
     * Sorts an array of tables in place by table name.
     *
     * @param  tmetas  table array
     */
    static void sortTables( TableMeta[] tmetas ) {
        Arrays.sort( tmetas, new Comparator<TableMeta>() {
            public int compare( TableMeta t1, TableMeta t2 ) {
                return getTableName( t1 ).compareTo( getTableName( t2 ) );
            }
            private String getTableName( TableMeta tmeta ) {
                String name = tmeta.getName();
                return name == null ? "" : name;
            }
        } );
    }
}
