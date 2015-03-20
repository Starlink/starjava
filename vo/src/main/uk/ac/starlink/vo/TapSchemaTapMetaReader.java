package uk.ac.starlink.vo;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import uk.ac.starlink.table.StarTable;

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
    private final boolean addOrphanTables_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     *
     * @param  serviceUrl  TAP service URL
     * @param  maxrec   maximum number of records to be requested at once
     * @param  populateSchemas   whether SchemaMeta objects will be
     *                           filled in with table lists when they are
     *                           acquired
     * @param  populateTables   whether TableMeta objects will be
     *                          filled in with column and foreign key lists
     *                          when they are acquired
     */
    public TapSchemaTapMetaReader( String serviceUrl, int maxrec,
                                   boolean populateSchemas,
                                   boolean populateTables ) {
        final URL url;
        try {
            url = new URL( serviceUrl );
        }
        catch ( MalformedURLException e ) {
            throw (IllegalArgumentException)
                  new IllegalArgumentException( "Bad URL: " + serviceUrl )
                 .initCause( e );
        }
        tsi_ = new TapSchemaInterrogator( url, maxrec ) {
            @Override
            protected StarTable executeQuery( TapQuery tq ) throws IOException {
                logger_.info( tq.getAdql() );
                return super.executeQuery( tq );
            }
        };
        populateSchemas_ = populateSchemas;
        populateTables_ = populateTables;
        addOrphanTables_ = true;
    }

    public String getSource() {
        return "TAP_SCHEMA(" + tsi_.getServiceUrl() + ")";
    }

    public SchemaMeta[] readSchemas() throws IOException {
        return tsi_.readSchemas( populateSchemas_, populateTables_,
                                 addOrphanTables_ );
    }

    public TableMeta[] readTables( SchemaMeta schema ) throws IOException {
        String whereClause = "schema_name = '" + schema.getName() + "'";
        List<TableMeta> tables =
            tsi_.readList( TapSchemaInterrogator.TABLE_QUERIER, whereClause );
        if ( populateTables_ ) {
            Map<String,List<ForeignMeta.Link>> lMap =
               tsi_.readMap( TapSchemaInterrogator.LINK_QUERIER,
                             "NATURAL JOIN TAP_SCHEMA.keys "
                           + "JOIN TAP_SCHEMA.tables ON from_table = table_name"
                           + " " + whereClause );
            Map<String,List<ForeignMeta>> fMap =
               tsi_.readMap( TapSchemaInterrogator.FKEY_QUERIER,
                             "JOIN TAP_SCHEMA.tables ON from_table = table_name"
                           + " " + whereClause );
            for ( List<ForeignMeta> flist : fMap.values() ) {
                for ( ForeignMeta fmeta : flist ) {
                    tsi_.populateForeignKey( fmeta, lMap );
                }
            }
            checkEmpty( lMap, "Foreign links" );
            Map<String,List<ColumnMeta>> cMap =
               tsi_.readMap( TapSchemaInterrogator.COLUMN_QUERIER,
                             "NATURAL JOIN TAP_SCHEMA.tables " + whereClause );
            for ( TableMeta tmeta : tables ) {
                tsi_.populateTable( tmeta, fMap, cMap );
            }
            checkEmpty( fMap, "Foreign keys" );
            checkEmpty( cMap, "Columns" );
        }
        return tables.toArray( new TableMeta[ 0 ] );
    }

    public ColumnMeta[] readColumns( TableMeta table ) throws IOException {
        return tsi_.readList( TapSchemaInterrogator.COLUMN_QUERIER,
                              "WHERE table_name = '" + table.getName() + "'" )
              .toArray( new ColumnMeta[ 0 ] );
    }

    public ForeignMeta[] readForeignKeys( TableMeta table ) throws IOException {
        String whereClause = "WHERE from_table = '" + table.getName() + "'";
        Map<String,List<ForeignMeta.Link>> lMap =
            tsi_.readMap( TapSchemaInterrogator.LINK_QUERIER,
                          "NATURAL JOIN TAP_SCHEMA.keys "
                        + whereClause );
        List<ForeignMeta> fList =
            tsi_.readList( TapSchemaInterrogator.FKEY_QUERIER,
                           whereClause );
        for ( ForeignMeta fmeta : fList ) {
            tsi_.populateForeignKey( fmeta, lMap );
        }
        checkEmpty( lMap, "Foreign links" );
        return fList.toArray( new ForeignMeta[ 0 ] );
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
}
