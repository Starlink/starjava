package uk.ac.starlink.ttools.taplint;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import org.xml.sax.SAXException;
import uk.ac.starlink.util.CountMap;
import uk.ac.starlink.vo.ColumnMeta;
import uk.ac.starlink.vo.ForeignMeta;
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TapQuery;

/**
 * Validation stage for checking the content of parsed Table metadata.
 *
 * @author   Mark Taylor
 * @since    3 Jun 2011
 */
public class TableMetadataStage implements Stage {

    private static final String[] KNOWN_COL_FLAGS =
        new String[] { "indexed", "primary", "nullable" };

    public TableMetadataStage() {
    }

    public String getDescription() {
        return "Check content of tables metadata";
    }

    public void run( URL serviceUrl, Reporter reporter ) {
        TableMeta[] tmetas;
        try {
            tmetas = TapQuery.readTableMetadata( serviceUrl );
        }
        catch ( SAXException e ) {
            reporter.report( Reporter.Type.ERROR, "FLSX",
                             "Can't parse table metadata well enough "
                            + "to check it", e );
            return;
        }
        catch ( IOException e ) {
            reporter.report( Reporter.Type.ERROR, "FLIO",
                             "Error reading table metadata", e );
            return;
        }
        checkTables( tmetas, reporter );
    }

    private void checkTables( TableMeta[] tmetas, Reporter reporter ) { 
        if ( tmetas == null ) {
            reporter.report( Reporter.Type.WARNING, "GONE",
                             "Table metadata absent" );
        }
        int nTable = tmetas.length;
        int nCol = 0;
        Map<String,TableMeta> tableMap = 
            createNameMap( "table", 'T', tmetas, reporter );
        Map<String,Map<String,ColumnMeta>> colsMap =
            new HashMap<String,Map<String,ColumnMeta>>();
        for ( String tname : tableMap.keySet() ) {
            TableMeta tmeta = tableMap.get( tname );
            ColumnMeta[] cols = tmeta.getColumns();
            nCol += cols.length; 
            Map<String,ColumnMeta> cmap =
                createNameMap( "column", 'C', cols, reporter );
            colsMap.put( tname, cmap );
        }
        int nForeign = 0;
        int nIndex = 0;
        CountMap<String> flagMap = new CountMap<String>();
        for ( String tname : tableMap.keySet() ) {
            TableMeta tmeta = tableMap.get( tname );
            Map<String,ColumnMeta> cmap = colsMap.get( tname );
            for ( String cname : cmap.keySet() ) {
                ColumnMeta col = cmap.get( cname );
                if ( col.isIndexed() ) {
                    nIndex++;
                }
                String[] flags = col.getFlags();
                for ( int ig = 0; ig < flags.length; ig++ ) {
                    flagMap.addItem( flags[ ig ] );
                }
            }
            ForeignMeta[] fkeys = tmeta.getForeignKeys();
            nForeign += fkeys.length;
            for ( int ik = 0; ik < fkeys.length; ik++ ) {
                ForeignMeta fkey = fkeys[ ik ];
                String targetTableName = fkey.getTargetTable();
                TableMeta targetTable = tableMap.get( targetTableName );
                if ( targetTable == null ) {
                    reporter.report( Reporter.Type.ERROR, "FKNT",
                                     "Foreign Key using non-existent "
                                   + "target table "
                                   + '"' + fkey.getTargetTable() + '"' );
                }
                else {
                    Map<String,ColumnMeta> targetCmap =
                        colsMap.get( targetTableName );
                    ForeignMeta.Link[] links = fkey.getLinks();
                    for ( int il = 0; il < links.length; il++ ) {
                        ForeignMeta.Link link = links[ il ];
                        ColumnMeta fromCol = cmap.get( link.getFrom() );
                        ColumnMeta toCol = targetCmap.get( link.getTarget() );
                        if ( fromCol == null || toCol == null ) {
                            reporter.report( Reporter.Type.ERROR, "FKLK",
                                             "Broken link " + link
                                           + " in foreign key " + fkey );
                        }
                    }
                }
            }
        }
        Collection<String> otherFlagList =
            new HashSet<String>( flagMap.keySet() );
        otherFlagList.removeAll( Arrays.asList( KNOWN_COL_FLAGS ) );
        String[] otherFlags = otherFlagList.toArray( new String[ 0 ] );
        reporter.report( Reporter.Type.SUMMARY, "SUMM",
                         "Tables: " + nTable + ", "
                       + "Columns: " + nCol + ", "
                       + "Foreign Keys: " + nForeign );
        reporter.report( Reporter.Type.SUMMARY, "FLGS",
                         "Standard column flags: "
                       + summariseCounts( flagMap, KNOWN_COL_FLAGS ) );
        reporter.report( Reporter.Type.SUMMARY, "FLGO",
                         "Other column flags: "
                       + summariseCounts( flagMap, otherFlags ) );
    }

    private String summariseCounts( CountMap<String> countMap, String[] keys ) {
        if ( keys.length == 0 ) {
            return "none";
        }
        StringBuffer sbuf = new StringBuffer();
        for ( int ik = 0; ik < keys.length; ik++ ) {
            String key = keys[ ik ];
            if ( ik > 0 ) {
                sbuf.append( ", " );
            }
            sbuf.append( key )
                .append( ": " )
                .append( countMap.getCount( key ) );
        }
        return sbuf.toString();
    }

    private <V> Map<String,V> createNameMap( String dName, char dChr,
                                             V[] values, Reporter reporter ) {
        Map<String,V> map = new LinkedHashMap<String,V>();
        for ( int iv = 0; iv < values.length; iv++ ) {
            V value = values[ iv ];
            String name = value.toString();
            if ( name == null || name.trim().length() == 0 ) {
                reporter.report( Reporter.Type.ERROR, "" + dChr + "BLA",
                                 "Blank name for " + dName + " #" + iv );
            }
            if ( ! map.containsKey( name ) ) {
                map.put( name, value );
            }
            else {
                reporter.report( Reporter.Type.WARNING, "" + dChr + "DUP",
                                 "Duplicate " + dName + " \"" + name + "\"" );
            }
        }
        return map;
    }
}
