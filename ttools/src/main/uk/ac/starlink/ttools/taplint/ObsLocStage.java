package uk.ac.starlink.ttools.taplint;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.vo.ColumnMeta;
import uk.ac.starlink.vo.Ivoid;
import uk.ac.starlink.vo.SchemaMeta;
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TapCapability;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.vo.TapService;
import uk.ac.starlink.votable.VOStarTable;

/**
 * Validation stage for testing ObsLocTAP data model metadata and content.
 * This implementation corresponds to PR-ObsLocTAP-20210609.
 *
 * @author   Mark Taylor
 * @since    5 Feb 2021
 * @see    <a href="https://www.ivoa.net/documents/ObsLocTAP/"
 *            >Observation Locator Table Access Protocol</a>
 */
public class ObsLocStage implements Stage {

    private final TapRunner tapRunner_;
    private final CapabilityHolder capHolder_;
    private final MetadataHolder metaHolder_;

    /** Full required name of ObsPlan table. */
    public static final String OBSPLAN_TNAME = "ivoa.obsplan";

    /** Required registration UType for ObsPlan table. */
    public static final Ivoid OBSPLAN_UTYPE =
        new Ivoid( "ivo://ivoa.net/std/obsloctap#table-1.0" );

    /** Feature type for ADQL Geometry functions from TAPRegExt. */
    public static final Ivoid ADQLGEO_TYPE =
        new Ivoid( "ivo://ivoa.net/std/TAPRegExt#features-adqlgeo" );

    /** Required ADQL Geometry functions (ObsLocTAP sec 3.3). */
    public static final String[] ADQLGEO_FORMS = new String[] {
        "CIRCLE", "POLYGON", "POINT", "INTERSECTS", "CONTAINS",
    };

    /** Known DALI Xtypes indicating region data. */
    public static final String[] REGION_XTYPES = new String[] {
        "point", "circle", "polygon",
    };

    /**
     * Constructor.
     *
     * @param   tapRunner  runs TAP queries
     * @param   capHolder  provides capability metadata at runtime
     * @param   metaHolder provides table metadata at runtime
     */
    public ObsLocStage( TapRunner tapRunner, CapabilityHolder capHolder,
                        MetadataHolder metaHolder ) {
        tapRunner_ = tapRunner;
        capHolder_ = capHolder;
        metaHolder_ = metaHolder;
    }

    public String getDescription() {
        return "Test implementation of ObsLocTAP Data Model";
    }

    public void run( Reporter reporter, TapService tapService ) {

        /* Get ObsPlan table if present. */
        TableMeta planMeta = null;
        for ( SchemaMeta smeta : metaHolder_.getTableMetadata() ) {
            for ( TableMeta tmeta : smeta.getTables() ) {
                if ( OBSPLAN_TNAME.equalsIgnoreCase( tmeta.getName() ) ) {
                    planMeta = tmeta;
                }
            }
        }
        if ( planMeta == null ) {
            reporter.report( FixedCode.F_NOTP,
                             "No table with name " + OBSPLAN_TNAME );
            return;
        }

        /* Check service capabilities. */
        TapCapability tcap = capHolder_.getCapability();
        if ( tcap == null ) {
            reporter.report( FixedCode.F_CAP0,
                             "No capabilities for ADQLGEO declaration" );
        }
        else if ( tcap != null ) {
            Collection<String> adqlgeoFuncs =
                Arrays.asList( tcap.getLanguages() ).stream()
               .filter( l -> "adql".equalsIgnoreCase( l.getName() ) )
               .map( l -> l.getFeaturesMap().get( ADQLGEO_TYPE ) )
               .flatMap( tlfs -> tlfs == null ? Stream.empty()
                                              : Arrays.asList( tlfs ).stream() )
               .map( feature -> feature.getForm() )
               .map( String::toUpperCase )
               .collect( Collectors.toCollection( LinkedHashSet::new ) );
            Collection<String> missingGeo =
                new LinkedHashSet<String>( Arrays.asList( ADQLGEO_FORMS ) );
            missingGeo.removeAll( adqlgeoFuncs );
            int nMissing = missingGeo.size();
            if ( missingGeo.size() > 0 ) {
                String msg = new StringBuffer()
                   .append( nMissing == ADQLGEO_FORMS.length ? "No"
                                                             : "Not all" )
                   .append( " required ADQL geometry functions declared" )
                   .append( " (see TAPRegExt)." )
                   .append( " ObsLocTAP requires " )
                   .append( ADQLGEO_TYPE )
                   .append( " features " )
                   .append( Arrays.toString( ADQLGEO_FORMS ) )
                   .toString();

                /* Jesus Salgado (on ObsLocTAP10RFC page) recommends
                 * that missing the *declaration* of these UDFs is a Warning
                 * not an Error, though implementation of them is mandated
                 * by the standard (sec 3.3). */
                reporter.report( FixedCode.W_PGEO, msg );
            }
        }

        /* Run tests. */
        new ObsLocRunner( reporter, tapService, planMeta, tapRunner_ )
           .run();
    }

    /**
     * Does the work for running tests on an ObsLocTAP table.
     */
    private static class ObsLocRunner implements Runnable {
        private final Reporter reporter_;
        private final TapService tapService_;
        private final TableMeta planMeta_;
        private final TapRunner tapRunner_;
        private final Map<String,ColumnMeta> gotCols_;
        private final Map<String,PlanCol> reqCols_;

        /**
         * Constructor.
         *
         * @param  reporter  validation message destination
         * @param  tapService  TAP service description
         * @param  planMeta   table metadata for ivoa.obsplan table
         * @param  tapRunner  runs TAP queries
         */
        ObsLocRunner( Reporter reporter, TapService tapService,
                      TableMeta planMeta, TapRunner tapRunner ) {
            reporter_ = reporter;
            tapService_ = tapService;
            planMeta_ = planMeta;
            tapRunner_ = tapRunner;
            gotCols_ = ObsTapStage.toMap( planMeta.getColumns() );
            reqCols_ = createRequiredColumns();
        }

        /**
         * Runs the tests.
         */
        public void run() {

            /* Check table utype (ObsLocTAP 1.0 sec 5). */
            String utype = planMeta_.getUtype();
            if ( ! OBSPLAN_UTYPE.equalsIvoid( new Ivoid( utype ) ) ) {
                String msg = new StringBuffer()
                    .append( "Table " )
                    .append( OBSPLAN_TNAME )
                    .append( " utype (" )
                    .append( utype == null ? "null"
                                           : '"' + utype + '"' )
                    .append( ") != \"" )
                    .append( OBSPLAN_UTYPE )
                    .append( "\"" )
                    .toString();
                reporter_.report( FixedCode.E_PLUT, msg );
            }

            /* Check declared column metadata for required columns. */
            int nreq = 0;
            for ( String reqName : reqCols_.keySet() ) {
                PlanCol reqCol = reqCols_.get( reqName );
                ColumnMeta gotCol = gotCols_.get( reqName );
                if ( gotCol != null ) {
                    checkMetadata( gotCol, reqCol );
                    nreq++;
                }
                else {
                    String msg = new StringBuffer()
                       .append( "Required ObsLocTAP column " )
                       .append( reqName )
                       .append( " is missing" )
                       .toString();
                    reporter_.report( FixedCode.E_PCOL, msg );
                }
            }

            /* Check data types using a sample result. */
            String adql1 = "SELECT TOP 1 * FROM " + OBSPLAN_TNAME;
            TapQuery tq1 = new TapQuery( tapService_, adql1, null );
            StarTable table1 = tapRunner_.getResultTable( reporter_, tq1 );
            ColumnInfo[] cinfos = table1 == null
                                ? new ColumnInfo[ 0 ]
                                : Tables.getColumnInfos( table1 );
            for ( ColumnInfo cinfo : cinfos ) {
                String cname = ObsTapStage.nameKey( cinfo.getName() );
                PlanCol stdCol = reqCols_.get( cname );
                if ( stdCol != null ) {
                    stdCol.type_.checkInfo( reporter_, cinfo );
                }
            }

            /* Check column content. */
            for ( String cname : gotCols_.keySet() ) {
                PlanCol stdCol = reqCols_.get( cname );
                if ( stdCol != null ) {
                    checkContent( stdCol );
                }
            }

            /* Special checks for observation times. */
            checkObservationTimes();

            /* Summarise columns. */
            int nother = gotCols_.size() - nreq;
            String msg = new StringBuffer()
               .append( OBSPLAN_TNAME )
               .append( " columns: " )
               .append( nreq )
               .append( "/" )
               .append( reqCols_.size() )
               .append( " required, " )
               .append( nother )
               .append( " custom" )
               .toString();
            reporter_.report( FixedCode.S_PCLS, msg );
        }

        /**
         * Checks that a given column has the correct metadata.
         *
         * @param  gotCol  metadata actually present in a column
         * @param  stdCol  correct metadata for a column
         */
        private void checkMetadata( ColumnMeta gotCol, PlanCol stdCol ) {
            String cname = gotCol.getName();
            compareItem( cname, "Utype", FixedCode.E_CUTP,
                         stdCol.utype_, gotCol.getUtype(), false );
            compareItem( cname, "UCD", FixedCode.E_CUCD,
                         stdCol.ucd_, gotCol.getUcd(), false );
            compareItem( cname, "Unit", FixedCode.E_CUNI,
                         stdCol.unit_, gotCol.getUnit(), true );
        }

        /**
         * Performs tests as applicable on the content of an obsplan column,
         * known to be present in the service.
         *
         * @param  stdCol  specification of column from ObsLocTAP standard
         */
        private void checkContent( PlanCol stdCol ) {
            String optList = stdCol.adqlOptList_;
            boolean isNullable = stdCol.isNullable_;
            String cname = stdCol.name_;

            /* If the column content is restricted, check only restricted
             * values are present. */
            if ( optList != null ) {
                int maxWrong = 4;
                StringBuffer abuf = new StringBuffer()
                   .append( "SELECT " )
                   .append( "DISTINCT TOP " )
                   .append( maxWrong + 1 )
                   .append( " " )
                   .append( cname )
                   .append( " FROM " )
                   .append( OBSPLAN_TNAME )
                   .append( " WHERE " )
                   .append( cname )
                   .append( " NOT IN (" )
                   .append( optList )
                   .append( ")" );
                if ( isNullable ) {
                    abuf.append( " AND " )
                        .append( cname )
                        .append( " IS NOT NULL" );
                }
                else {
                    abuf.append( " OR " )
                        .append( cname )
                        .append( " IS NULL" );
                }
                String adql = abuf.toString();
                TapQuery tq = new TapQuery( tapService_, adql, null );
                StarTable table = tapRunner_.getResultTable( reporter_, tq );
                TableData tdata = TableData.createTableData( reporter_, table );
                if ( tdata == null ) {
                    return;
                }
                long nwrong = tdata.getRowCount();
                if ( nwrong > 0 ) {
                    String msg = new StringBuffer()
                       .append( "Illegal " )
                       .append( nwrong == 1 ? "value" : "values" )
                       .append( " in column " )
                       .append( cname )
                       .append( ": " )
                       .append( Arrays.asList( tdata.getColumn( 0 ) ).stream()
                               .map( String::valueOf )
                               .collect( Collectors.joining( ", " ) ) )
                       .append( nwrong > maxWrong ? ", ..." : "" )
                       .toString();
                    reporter_.report( FixedCode.E_PVAL, msg );
                }
            }

            /* If the column is not nullable, look for null values. */
            else if ( ! isNullable ) {
                String adql = new StringBuffer()
                   .append( "SELECT " )
                   .append( "TOP 1 " )
                   .append( cname )
                   .append( " FROM " )
                   .append( OBSPLAN_TNAME )
                   .append( " WHERE " )
                   .append( cname )
                   .append( " IS NULL" )
                   .toString();
                TapQuery tq = new TapQuery( tapService_, adql, null );
                StarTable table = tapRunner_.getResultTable( reporter_, tq );
                TableData tdata = TableData.createTableData( reporter_, table );
                if ( tdata != null && tdata.getRowCount() > 0 ) {
                    String msg = new StringBuffer()
                       .append( "NULL values in non-nullable column " )
                       .append( cname )
                       .toString();
                    reporter_.report( FixedCode.E_LNUL, msg );
                }
            }
        }

        /**
         * Check specific constraint on the t_min, t_max, t_exptime columns:
         * they may not be null for Scheduled or Performed status records.
         */
        private void checkObservationTimes() {
            String adql = new StringBuffer()
                .append( "SELECT " )
                .append( "TOP 1 " )
                .append( "execution_status, t_min, t_max, t_exptime " )
                .append( " FROM " )
                .append( OBSPLAN_TNAME )
                .append( " WHERE (" )
                .append( "t_min IS NULL OR " )
                .append( "t_max IS NULL OR " )
                .append( "t_exptime IS NULL" )
                .append( ") AND " )
                .append( "execution_status IN ('Scheduled', 'Performed')" )
                .toString();
            TapQuery tq = new TapQuery( tapService_, adql, null );
            StarTable table = tapRunner_.getResultTable( reporter_, tq );
            TableData tdata = TableData.createTableData( reporter_, table );
            if ( tdata != null && tdata.getRowCount() > 0 ) {
                String msg = new StringBuffer()
                   .append( "NULL t_min/t_max/t_exptime values for " )
                   .append( "execution_status Scheduled/Performed" )
                   .toString();
                reporter_.report( FixedCode.E_LNSP, msg );
            }
        }

        /**
         * Checks a metadata item against the value it should have and
         * reports an error if not.
         *
         * @param  colName   name of column
         * @param  itemName  name of metadata item
         * @param  code      code value for mismatch error reports
         * @param  obsValue  correct value for metadata item
         * @param  gotValue  actual value of metadata item
         * @param  isCaseSensitive  true iff value comparison is case-sensitive
         */
        private void compareItem( String colName, String itemName,
                                  ReportCode code,
                                  String obsValue, String gotValue,
                                  boolean isCaseSensitive ) {
            String vGot = gotValue == null || gotValue.trim().length() == 0
                        ? "null"
                        : gotValue;
            String vObs = obsValue == null ? "null" : obsValue;
            if ( isCaseSensitive ? ( ! vGot.equals( vObs ) )
                                 : ( ! vGot.equalsIgnoreCase( vObs ) ) ) {
                StringBuffer sbuf = new StringBuffer()
                    .append( "Wrong " )
                    .append( itemName )
                    .append( " for " )
                    .append( OBSPLAN_TNAME )
                    .append( " column " )
                    .append( colName )
                    .append( ": " )
                    .append( gotValue )
                    .append( " != " )
                    .append( obsValue );
                reporter_.report( code, sbuf.toString() );
            }
        }
    }

    /**
     * Returns an array of the specifications of the required ivoa.obsplan
     * columns, as specified by the the ObsLocTAP standard.
     *
     * @param  map from normalised column name to column specification object
     */
    static Map<String,PlanCol> createRequiredColumns() {

        /* Prepare list of columns. */
        PlanCol[] cols = new PlanCol[] {
            new PlanCol( "t_planning", Type.FLOAT, "d", null, null ),
            new PlanCol( "target_name", Type.STRING, null,
                         "Target.name", "meta.id;src" ),
            new PlanCol( "obs_id", Type.STRING, null,
                         "DataID.observationID", "meta.id" ),
            new PlanCol( "obs_collection", Type.STRING, null,
                         "DataID.collection", "meta.id" ),
            new PlanCol( "s_ra", Type.FLOAT, "deg",
                         "Char.SpatialAxis.Coverage.Location.Coord.Position2D."
                       + "Value2.C1", "pos.eq.ra" ),
            new PlanCol( "s_dec", Type.FLOAT, "deg",
                         "Char.SpatialAxis.Coverage.Location.Coord.Position2D."
                       + "Value2.C2", "pos.eq.dec" ),
            new PlanCol( "s_fov", Type.FLOAT, "deg",
                         "Char.SpatialAxis.Coverage.Bounds.Extent.diameter",
                         "phys.angSize;instr.fov" ),
            new PlanCol( "s_region", Type.REGION, null,
                         "Char.SpatialAxis.Coverage.Support.Area",
                         "pos.outline;obs.field" ),
            new PlanCol( "s_resolution", Type.FLOAT, "arcsec",
                         "Char.SpatialAxis.Resolution.Refval.value",
                         "pos.angResolution" ),
            new PlanCol( "t_min", Type.FLOAT, "d",
                         "Char.TimeAxis.Coverage.Bounds.Limits.StartTime",
                         "time.start;obs.exposure" ),
            new PlanCol( "t_max", Type.FLOAT, "d",
                         "Char.TimeAxis.Coverage.Bounds.Limits.StopTime",
                         "time.end;obs.exposure" ),
            new PlanCol( "t_exptime", Type.FLOAT, "s",
                         "Char.TimeAxis.Coverage.Support.Extent",
                         "time.duration;obs.exposure" ),
            new PlanCol( "t_resolution", Type.FLOAT, "s",
                         "Char.TimeAxis.Resolution.Refval.value",
                          "time.resolution" ),
            new PlanCol( "em_min", Type.FLOAT, "m",
                         "Char.SpectralAxis.Coverage.Bounds.Limits.LoLimit",
                         "em.wl;stat.min" ),
            new PlanCol( "em_max", Type.FLOAT, "m",
                         "Char.SpectralAxis.Coverage.Bounds.Limits.HiLimit",
                         "em.wl;stat.max" ),
            new PlanCol( "em_res_power", Type.FLOAT, null,
                         "Char.SpectralAxis.Resolution.ResolPower.refVal",
                         "spect.resolution" ),
            new PlanCol( "o_ucd", Type.STRING, null,
                         "Char.ObservableAxis.ucd", "meta.ucd" ),
            new PlanCol( "pol_states", Type.STRING, null,
                         "Char.PolarizationAxis.stateList",
                         "meta.code;phys.polarization" ), 
            new PlanCol( "pol_xel", Type.INTEGER, null,
                         "Char.PolarizationAxis.numBins", "meta.number" ),
            new PlanCol( "facility_name", Type.STRING, null,
                         "Provenance.ObsConfig.Facility.name",
                         "meta.id;instr.tel" ),
            new PlanCol( "instrument_name", Type.STRING, null,
                         "Provenance.ObsConfig.Instrument.name",
                         "meta.id;instr" ),
            new PlanCol( "t_plan_exptime", Type.FLOAT, "s",
                         "Char.TimeAxis.Coverage.Support.Extent",
                         "time.duration;obs.exposure" ),
            new PlanCol( "category", Type.STRING, null, null, null ),
            new PlanCol( "priority", Type.INTEGER, null, null, null ),
            new PlanCol( "execution_status", Type.STRING, null, null, null ),
            new PlanCol( "tracking_type", Type.STRING, null, null, null ),
        };

        /* Store them in a map. */
        Map<String,PlanCol> map = new LinkedHashMap<>();
        for ( PlanCol col : cols ) {
            map.put( ObsTapStage.nameKey( col.name_ ), col );
        }
        assert map.size() == 26;

        /* Add some additional constraints for various columns. */
        map.get( "t_planning" ).isNullable_ = false;
        map.get( "obs_id" ).isNullable_ = false;
        map.get( "facility_name" ).isNullable_ = false;
        map.get( "category" ).setStringOpts( new String[] {
            "Fixed", "Coordinated", "Window", "Other",
        } );
        map.get( "category" ).isNullable_ = false;
        map.get( "priority" ).setIntOpts( new int[] { 0, 1, 2 } );
        map.get( "priority" ).isNullable_ = false;
        map.get( "execution_status" ).setStringOpts( new String[] {
            "Planned", "Scheduled", "Unscheduled", "Performed", "Aborted",
        } );
        map.get( "execution_status" ).isNullable_ = false;
        map.get( "tracking_type" ).setStringOpts( new String[] {
            "Sidereal", "Solar-system-object-tracking", "Fixed-az-el-transit",
        } );
        map.get( "tracking_type" ).isNullable_ = false;
        return map;
    }

    /**
     * Indicates whether a column's content class matches one of a supplied
     * list of permitted classes.
     *
     * @param  info  column info
     * @param  clazzes  permitted class list
     * @return   true iff info.getContentClass() maches one of the clazzes
     */
    private static boolean matchesClass( ValueInfo info, Class<?>... clazzes ) {
        return Arrays.asList( clazzes ).contains( info.getContentClass() );
    }

    /**
     * Returns the human-readable name of the data type of a column
     * from a VOTable.
     *
     * @param  info  column info
     * @return   datatype name
     */
    public static String votype( ValueInfo info ) {

        /* This isn't really correct, since it gives the java name not the
         * reconstructed datatype,arraysize information that would have
         * come from the VOTable.  But it's good enough to get the idea. */
        return info.getContentClass().getSimpleName();
    }

    /**
     * Enum for column data types used by obsplan table.
     */
    private static enum Type {
        INTEGER() {
            void checkInfo( Reporter reporter, ValueInfo info ) {
                if ( ! matchesClass( info,
                                     Byte.class, Short.class, Integer.class,
                                     Long.class ) ) {
                    reportTypeMismatch( reporter, info,
                                        votype( info ) + " not integer" );
                }
            }
        },
        FLOAT() {
            void checkInfo( Reporter reporter, ValueInfo info ) {
                if ( ! matchesClass( info, Float.class, Double.class,
                                     Byte.class, Short.class, Integer.class,
                                     Long.class ) ) {
                    reportTypeMismatch( reporter, info,
                                        votype( info ) + " not numeric");
                }
            }
        },
        STRING() {
            void checkInfo( Reporter reporter, ValueInfo info ) {
                if ( ! String.class.equals( info.getContentClass() ) ) {
                    reportTypeMismatch( reporter, info,
                                        votype( info ) + " not string" );
                }
            }
        },
        REGION() {
            void checkInfo( Reporter reporter, ValueInfo info ) {
                if ( matchesClass( info, String.class ) ) {
                    // assumed STC-S
                }
                else if ( matchesClass( info,
                                        float[].class, double[].class ) ) {
                    String xtype = info.getXtype();
                    if ( xtype == null || xtype.trim().length() == 0 ) {
                        reportTypeMismatch( reporter, info,
                                            "has no DALI region xtype" );
                    }
                    else if ( Arrays.asList( REGION_XTYPES )
                                    .contains( xtype ) ) {
                        // good
                    }
                    else {
                        String msg = new StringBuffer()
                           .append( "Unrecognised region xtype='" )
                           .append( xtype )
                           .append( "' on column " )
                           .append( info.getName() )
                           .append( " (not in " )
                           .append( Arrays.toString( REGION_XTYPES ) )
                           .append( ")" )
                           .toString();
                        reporter.report( FixedCode.W_CRGN, msg );
                    }
                }
                else {
                    reportTypeMismatch( reporter, info,
                                        "not string or floating point array" );
                }
            }
        };

        /**
         * Checks a supplied column metadata object matches the requirements
         * of this data type, and reports any issues using a given reporter.
         *
         * @param  reporter  validation message destination
         * @param  info   column metadata to check
         */
        abstract void checkInfo( Reporter reporter, ValueInfo info );

        /**
         * Writes a type mismatch report.
         *
         * @param  reporter  validation message destination
         * @param  info   offending column
         * @param  txt   mismatch-specific text
         */
        private static void reportTypeMismatch( Reporter reporter,
                                                ValueInfo info,
                                                String txt ) {
            String msg = new StringBuffer()
               .append( "Data type mismatch for " )
               .append( OBSPLAN_TNAME )
               .append( " column " )
               .append( info.getName() )
               .append( ": " )
               .append( txt )
               .toString();
            reporter.report( FixedCode.E_PCMS, msg );
        }
    }

    /**
     * Represents metadata for an obsplan column defined by the ObsLocTAP
     * standard.
     */
    static class PlanCol {

        final String name_;
        final Type type_;
        final String unit_;
        final String utype_;
        final String ucd_;
        boolean isNullable_;
        String adqlOptList_;

        /**
         * Constructor.
         *
         * @param  name  column name
         * @param  type  column data type
         * @param  unit  unit string, or null
         * @param  utype  utype, or null 
         * @param  ucd    UCD, or null
         */
        PlanCol( String name, Type type, 
                 String unit, String utype, String ucd ) {
            name_ = name;
            type_ = type;
            unit_ = unit;
            utype_ = utype;
            ucd_ = ucd;
            isNullable_ = true;
        }

        /**
         * Sets the exclusive list of permitted non-NULL values
         * for this string-valued column.
         *
         * @param  opts  permitted values
         */
        void setStringOpts( String[] opts ) {
            adqlOptList_ = Arrays.asList( opts ).stream()
                          .map( s -> "'" + s + "'" )
                          .collect( Collectors.joining( ", " ) );
        }

        /**
         * Sets the exclusive list of permitted non-NULL values
         * for this integer-valued column.
         *
         * @param  opts  permitted values
         */
        void setIntOpts( int[] opts ) {
            adqlOptList_ = IntStream.of( opts )
                          .mapToObj( Integer::toString )
                          .collect( Collectors.joining( ", " ) );
        }
    }
}
