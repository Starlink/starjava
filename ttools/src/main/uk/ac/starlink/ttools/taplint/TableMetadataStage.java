package uk.ac.starlink.ttools.taplint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.util.CountMap;
import uk.ac.starlink.vo.AdqlSyntax;
import uk.ac.starlink.vo.ColumnMeta;
import uk.ac.starlink.vo.EndpointSet;
import uk.ac.starlink.vo.ForeignMeta;
import uk.ac.starlink.vo.SchemaMeta;
import uk.ac.starlink.vo.TableMeta;

/**
 * Validation stage for checking the content of parsed Table metadata.
 * Concrete subclasses must provide a method to acquire the metadata
 * as an array of TableMeta objects.
 *
 * @author   Mark Taylor
 * @since    3 Jun 2011
 */
public abstract class TableMetadataStage implements Stage, MetadataHolder {

    private final String srcDescription_;
    private final String[] knownColFlags_;
    private final boolean reportOtherFlags_;
    private SchemaMeta[] smetas_;
    private static final AdqlSyntax syntax_ = AdqlSyntax.getInstance();
    private static final String[] KNOWN_COL_FLAGS =
        new String[] { "indexed", "primary", "nullable" };

    /**
     * Constructor.
     *
     * @param   srcDescription  short text description of table metadata source
     * @param   knownColFlags  standard column flag values to report counts for
     * @param   reportOtherFlags  whether to report counts for non-standard
     *                            column flag values
     */
    public TableMetadataStage( String srcDescription, String[] knownColFlags,
                               boolean reportOtherFlags ) {
        srcDescription_ = srcDescription;
        knownColFlags_ = knownColFlags;
        reportOtherFlags_ = reportOtherFlags;
    }

    public String getDescription() {
        return "Check content of tables metadata from " + srcDescription_;
    }

    /**
     * Returns a short text description of table metadata source.
     * 
     * @return  metadata source description
     */
    public String getSourceDescription() {
        return srcDescription_;
    }

    /**
     * Returns the table metadata obtained by the last run of this stage.
     *
     * @return  table metadata array
     */
    public SchemaMeta[] getTableMetadata() {
        return smetas_;
    }
   
    /**
     * Returns an array providing table metadata to check.
     *
     * @param  reporter   destination for validation messages
     * @param  endpointSet  TAP service endpoints
     * @return   list of fully populated schema metadata elements
     */
    protected abstract SchemaMeta[]
            readTableMetadata( Reporter reporter, EndpointSet endpointSet );

    public void run( Reporter reporter, EndpointSet endpointSet ) {
        SchemaMeta[] smetas = readTableMetadata( reporter, endpointSet );
        checkSchemas( reporter, smetas );
        smetas_ = smetas;
    }

    /**
     * Performs the checking and reporting for a given table metadata set.
     *
     * @param  reporter  destination for validation messages
     * @param  smetas   table metadata to check
     */
    private void checkSchemas( Reporter reporter, SchemaMeta[] smetas ) { 
        if ( smetas == null ) {
            reporter.report( FixedCode.F_GONE, "Table metadata absent" );
            return;
        }

        /* Table names must be unique globally, not just within each schema. */
        int nSchema = smetas.length;
        Map<String,SchemaMeta> schemaMap =
            createNameMap( reporter, "schema", 'S', smetas );
        for ( String sname : schemaMap.keySet() ) {
            if ( sname != null ) {
                checkSchemaName( reporter, sname );
            }
        }
        List<TableMeta> tmList = new ArrayList<TableMeta>();
        for ( SchemaMeta smeta : smetas ) {
            tmList.addAll( Arrays.asList( smeta.getTables() ) );
        }
        TableMeta[] tmetas = tmList.toArray( new TableMeta[ 0 ] );
        int nTable = tmetas.length;
        int nCol = 0;
        Map<String,TableMeta> tableMap = 
            createNameMap( reporter, "table", 'T', tmetas );
        for ( String tname : tableMap.keySet() ) {
            if ( tname != null ) {
                checkTableName( reporter, tname );
            }
        }

        Map<String,Map<String,ColumnMeta>> colsMap =
            new HashMap<String,Map<String,ColumnMeta>>();
        for ( String tname : tableMap.keySet() ) {
            TableMeta tmeta = tableMap.get( tname );
            ColumnMeta[] cols = tmeta.getColumns();
            nCol += cols.length; 
            Map<String,ColumnMeta> cmap =
                createNameMap( reporter, "column", 'C', cols );
            for ( String cname : cmap.keySet() ) {
                if ( cname != null ) {
                    checkColumnName( reporter, cname, tmeta );
                }
            }
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
                    String msg = new StringBuffer()
                       .append( "Non-existent target table " )
                       .append( fkey.getTargetTable() )
                       .append( " for foreign key in table " )
                       .append( tmeta )
                       .toString();
                    reporter.report( FixedCode.E_FKNT, msg );
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
                            StringBuilder mbuf = new StringBuilder()
                               .append( "Broken link " )
                               .append( link )
                               .append( " in foreign key " )
                               .append( tname )
                               .append( fkey );
                            if ( fromCol == null ) {
                                mbuf.append( " (no column " )
                                    .append( tname )
                                    .append( '.' )
                                    .append( link.getFrom() )
                                    .append( ')' );
                            }
                            if ( toCol == null ) {
                                mbuf.append( " (no column " )
                                    .append( targetTableName )
                                    .append( '.' )
                                    .append( link.getTarget() )
                                    .append( ')' );
                            }
                            reporter.report( FixedCode.E_FKLK,
                                             mbuf.toString() );
                        }
                        else {
                            String fromType = fromCol.getDataType();
                            String toType = toCol.getDataType();
                            if ( fromType == null ||
                                 ! fromType.equals( toType ) ) {
                                String msg = new StringBuffer()
                                    .append( "Type mismatch for link " )
                                    .append( link )
                                    .append( " in foreign key " )
                                    .append( fkey )
                                    .append( "; " )
                                    .append( fromType )
                                    .append( " vs. " )
                                    .append( toType )
                                    .toString();
                                reporter.report( FixedCode.W_FTYP, msg );
                            }
                        }
                    }
                }
            }
        }
        Collection<String> otherFlagList =
            new HashSet<String>( flagMap.keySet() );
        otherFlagList.removeAll( Arrays.asList( knownColFlags_ ) );
        String[] otherFlags = otherFlagList.toArray( new String[ 0 ] );
        reporter.report( FixedCode.S_SUMM,
                         "Schemas: " + nSchema + ", "
                       + "Tables: " + nTable + ", "
                       + "Columns: " + nCol + ", "
                       + "Foreign Keys: " + nForeign );
        reporter.report( FixedCode.S_FLGS,
                         "Standard column flags: "
                       + summariseCounts( flagMap, knownColFlags_ ) );
        if ( reportOtherFlags_ ) {
            reporter.report( FixedCode.S_FLGO,
                             "Other column flags: "
                           + summariseCounts( flagMap, otherFlags ) );
        }
    }

    /**
     * Checks legality of a metadata Schema name.
     *
     * @param  reporter   destination for validation messages
     * @param  sname   schema name
     */
    private void checkSchemaName( Reporter reporter, String sname ) {
        // no constraints?
    }

    /**
     * Checks legality of a metadata Table name.
     *
     * @param  reporter   destination for validation messages
     * @param  tname    table name
     */
    private void checkTableName( Reporter reporter, String tname ) {
        if ( ! syntax_.isAdqlTableName( tname ) ) {
            reporter.report( FixedCode.E_TNTN,
                             "Bad ADQL table name '" + tname + "'" );
        }
        if ( syntax_.isReserved( tname ) ) {
            reporter.report( FixedCode.E_TRSV,
                             "Table name is ADQL reserved word '"
                           + tname + "'" );
        }
    }

    /**
     * Checks legality of a metadata Column name.
     *
     * @param  reporter   destination for validation messages
     * @param  cname    column name
     * @param  tmeta    table containing the column
     */
    private void checkColumnName( Reporter reporter, String cname,
                                  TableMeta tmeta ) {
        String detailTxt =
              " '" + cname + "' in table " + tmeta.getName()
            + " - should delimit like '" + syntax_.quote( cname ) + "'";
        if ( ! syntax_.isAdqlColumnName( cname ) ) {
            reporter.report( FixedCode.E_CNID,
                             "Column name is not ADQL identifier" + detailTxt );
        }
        if ( syntax_.isReserved( cname ) ) {
            reporter.report( FixedCode.E_CRSV,
                             "Column name is ADQL reserved word" + detailTxt );
        }
    }

    /**
     * Summarises how many entries in a given map there are for each of
     * a given set of keys.
     *
     * @param  countMap   map containing counts
     * @param  keys   keys to be summarised
     * @return   summary string
     */
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

    /**
     * Creates a map from an array of objects.
     * The map keys are the results of calling <code>toString</code> on the
     * objects.  Any duplicates will be reported through the supplied
     * reporter or blank values.  Note that some keys of the returned map
     * may be null or empty strings (in that case a report will already
     * have been made).
     *
     * @param   reporter   destination for validation messages
     * @param   dName  descriptive name of type of thing in map
     * @param   dChr   single character labelling type of thing
     * @param   values  objects to populate map
     * @return   name -> value map
     */
    private <V> Map<String,V> createNameMap( Reporter reporter, String dName,
                                             char dChr, V[] values ) {
        Map<String,V> map = new LinkedHashMap<String,V>();
        for ( int iv = 0; iv < values.length; iv++ ) {
            V value = values[ iv ];
            String name = value.toString();
            if ( name == null || name.trim().length() == 0 ) {
                reporter.report( new AdhocCode( ReportType.ERROR,
                                                "" + dChr + "BLA" ),
                                 "Blank name for " + dName + " #" + iv );
            }
            if ( ! map.containsKey( name ) ) {
                map.put( name, value );
            }
            else {
                reporter.report( new AdhocCode( ReportType.WARNING,
                                                "" + dChr + "DUP" ),
                                 "Duplicate " + dName + " \"" + name + "\"" );
            }
        }
        return map;
    }
}
