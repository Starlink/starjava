package uk.ac.starlink.ttools.taplint;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.vo.ColumnMeta;
import uk.ac.starlink.vo.ForeignMeta;
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TapSchemaInterrogator;

/**
 * Validation stage for checking table metadata from the TAP_SCHEMA tables.
 *
 * @author   Mark Taylor
 * @since    6 Jun 2011
 */
public class TapSchemaStage extends TableMetadataStage {

    private TableMeta[] tmetas_;

    public TapSchemaStage() {
        super( "TAP_SCHEMA tables", false );
    }

    protected TableMeta[] readTableMetadata( URL serviceUrl,
                                             Reporter reporter ) {
        TapSchemaInterrogator tsi = new TapSchemaInterrogator( serviceUrl ); 
        Map<String,List<ColumnMeta>> cMap;
        try {
            cMap = tsi.readColumns();
        }
        catch ( IOException e ) {
            reporter.report( Reporter.Type.ERROR, "CLIO",
                             "Error reading TAP_SCHEMA.columns table", e );
            cMap = new HashMap<String,List<ColumnMeta>>();
        }

        Map<String,List<ForeignMeta.Link>> lMap;
        try {
            lMap = tsi.readForeignLinks();
        }
        catch ( IOException e ) {
            reporter.report( Reporter.Type.ERROR, "FLIO",
                             "Error reading TAP_SCHEMA.key_columns table", e );
            lMap = new HashMap<String,List<ForeignMeta.Link>>();
        }

        Map<String,List<ForeignMeta>> fMap;
        try {
            fMap = tsi.readForeignKeys( lMap );
        }
        catch ( IOException e ) {
            reporter.report( Reporter.Type.ERROR, "FKIO",
                             "Error reading TAP_SCHEMA.keys table", e );
            fMap = new HashMap<String,List<ForeignMeta>>();
        }

        List<TableMeta> tList;
        try {
            tList = tsi.readTables( cMap, fMap );
        }
        catch ( IOException e ) {
            tList = null;
            reporter.report( Reporter.Type.ERROR, "TBIO",
                             "Error reading TAP_SCHEMA.tables table", e );
        }

        checkEmpty( reporter, cMap, "CLUN", "columns" );
        checkEmpty( reporter, fMap, "FKUN", "keys" );
        checkEmpty( reporter, lMap, "FLUN", "key_columns" );
        return tList == null ? null
                             : tList.toArray( new TableMeta[ 0 ] );
    }

    /**
     * Check that a map is empty, and report on any entries that are present.
     * The maps that this checks should be empty since their entries are
     * removed as their content is used by the metadata reading routines
     * to populate higher classes in the metadata hierarchy.
     *
     * @param   reporter   destination for validation messages
     * @param   map   map to check
     * @param   code  reporting code for unused entries
     * @parma   stName   unqualified TAP_SCHEMA table name
     */
    private void checkEmpty( Reporter reporter, Map<?,?> map,
                             String code, String stName ) {
        for ( Object key : map.keySet() ) {
            reporter.report( Reporter.Type.WARNING, code,
                             "Unused entry in TAP_SCHEMA." + stName
                           + " table: " + key );
        }
    }
}
