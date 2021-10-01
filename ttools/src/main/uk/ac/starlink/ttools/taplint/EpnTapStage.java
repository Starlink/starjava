package uk.ac.starlink.ttools.taplint;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.func.Times;
import uk.ac.starlink.ttools.votlint.VocabChecker;
import uk.ac.starlink.vo.ColumnMeta;
import uk.ac.starlink.vo.SchemaMeta;
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.vo.TapService;
import uk.ac.starlink.vo.VocabTerm;

/**
 * Validation stage for testing EPN-TAP data model metadata and content.
 * This implementation corresponds to PR-EPNTAP-2.0-20210721.
 *
 * @author   Mark Taylor
 * @since    17 Jun 2021
 * @see  <a href="https://www.ivoa.net/documents/EPNTAP/">EPN-TAP</a>
 */
public class EpnTapStage implements Stage {

    private final TapRunner tapRunner_;
    private final MetadataHolder metaHolder_;

    /** Schema-less table name of EPN-TAP tables. */
    public static final String EPNCORE_TNAME = "epn_core";

    /** Required utype of EPN-TAP tables. */
    public static final String EPNCORE_UTYPE =
        "ivo://ivoa.net/std/epntap#table-2.0";

    /** Transitional utype of EPN-TAP tables. */
    public static final String EPNCORE_UTYPE2 =
        "ivo://vopdc.obspm/std/epncore#schema-2.0";

    // Julian Day times that bound believable values.
    // Times.mjdToDecYear(jd-2400000.5).
    private static final int JD_PLAUSIBLE_LO = 2086000; // approx 1000 AD
    private static final int JD_PLAUSIBLE_HI = 2817000; // approx 3000 AD

    // DALI 1.1 sec 3.3.3.
    static Pattern DALI_TIMESTAMP_REGEX = Pattern.compile(
          "[0-9]{4}-[01][0-9]-[0-3][0-9]"
        + "(?:T[0-2][0-9]:[0-5][0-9]:[0-5][0-9](?:[.][0-9]*)?Z?)?"
    );

    // VocabularyChecker for https://www.ivoa.net/rdf/messenger.
    // Note at time of writing these are all marked Preliminary.
    private static final VocabChecker MESSENGER_VOCAB =
        new VocabChecker( "http://www.ivoa.net/rdf/messenger",
                          new String[] {
                              "EUV", "Gamma-ray", "Infrared", "Millimeter",
                              "Neutrino", "Optical", "Photon", "UV", "X-ray",
                          } );

    /**
     * Constructor.
     *
     * @param   tapRunner  runs TAP queries
     * @param   metaHolder provides table metadata at runtime
     */
    public EpnTapStage( TapRunner tapRunner, MetadataHolder metaHolder ) {
        tapRunner_ = tapRunner;
        metaHolder_ = metaHolder;
    }

    public String getDescription() {
        return "Test implementation of EPN-TAP tables";
    }

    public void run( Reporter reporter, TapService tapService ) {

        /* Locate epn_core tables. */
        List<TableMeta> epnMetas = new ArrayList<>();
        for ( SchemaMeta smeta : metaHolder_.getTableMetadata() ) {
            for ( TableMeta tmeta : smeta.getTables() ) {
                String tname = tmeta.getName();
                String utype = tmeta.getUtype();
                String subname = tname.replaceAll( "^.*[.]", "" );
                boolean isEpnName = EPNCORE_TNAME.equalsIgnoreCase( subname );
                boolean isEpnUtype = EPNCORE_UTYPE.equalsIgnoreCase( utype );
                boolean isEpnUtype2 = EPNCORE_UTYPE2.equalsIgnoreCase( utype );
                if ( isEpnName && ! isEpnUtype ) {
                    final String msg;
                    final ReportCode code;
                    if ( isEpnUtype2 ) {
                        msg = new StringBuffer()
                           .append( "Table " )
                           .append( tname )
                           .append( " has transitional utype " )
                           .append( utype )
                           .append( " not EPN-TAP v2 utype " )
                           .append( EPNCORE_UTYPE )
                           .toString();
                        code = FixedCode.W_PNUB;
                    }
                    else {
                        msg = new StringBuffer()
                           .append( "Table " )
                           .append( tname )
                           .append( " has EPN-TAP name, but not utype (" ) 
                           .append( utype )
                           .append( " != " )
                           .append( EPNCORE_UTYPE )
                           .append( ")" )
                           .toString();
                        code = FixedCode.E_PNUT;
                    }
                    reporter.report( code, msg );
                }
                if ( isEpnUtype && ! isEpnName ) {
                    String msg = new StringBuffer()
                       .append( "Table " )
                       .append( tname )
                       .append( " has EPN-TAP utype, but not name" )
                       .append( " (not of form <schema-name>." )
                       .append( EPNCORE_TNAME )
                       .append( ")" )
                       .toString();
                    reporter.report( FixedCode.E_PNTN, msg );
                }
                if ( isEpnName || isEpnUtype ) {
                    epnMetas.add( tmeta );
                }
            }
        }

        /* Check each one. */
        int nEpn = epnMetas.size();
        for ( int i = 0; i < nEpn; i++ ) {
            TableMeta tmeta = epnMetas.get( i );
            String msg = new StringBuffer()
               .append( "Checking EPN-TAP table #" )
               .append( i + 1 )
               .append( "/" )
               .append( nEpn )
               .append( ": " )
               .append( tmeta.getName() )
               .toString();
            reporter.report( FixedCode.I_PNIX, msg );
            new EpncoreRunner( reporter, tapService, tmeta, tapRunner_ )
           .run();
        }

        /* Report on epn_core tables found. */
        if ( epnMetas.size() == 0 ) {
            reporter.report( FixedCode.F_NOPN,
                             "No " + EPNCORE_TNAME + " tables" );
        }
        else {
            String msg = new StringBuffer()
               .append( "Found " )
               .append( nEpn )
               .append( " " )
               .append( EPNCORE_TNAME )
               .append( " tables" )
               .toString();
            reporter.report( FixedCode.S_PNCN, msg );
        }
    }

    /**
     * Runs tests on a single epn_core table.
     */
    private static class EpncoreRunner implements Runnable {

        private final Reporter reporter_;
        private final TapService tapService_;
        private final TableMeta epnMeta_;
        private final TapRunner tapRunner_;
        private final String tname_;
        private final Map<String,ColumnMeta> gotCols_;

        /**
         * Constructor.
         *
         * @param  reporter  validation message destination
         * @param  tapService  TAP service description
         * @param  epnMeta   table metadata for epn_core table
         * @param  tapRunner  runs TAP queries
         */
        EpncoreRunner( Reporter reporter, TapService tapService,
                       TableMeta epnMeta, TapRunner tapRunner ) {
            reporter_ = reporter;
            tapService_ = tapService;
            epnMeta_ = epnMeta;
            tapRunner_ = tapRunner;
            tname_ = epnMeta.getName();
            gotCols_ = ObsTapStage.toMap( epnMeta.getColumns() );
        }

        /**
         * All the tests are performed from this method.
         */
        public void run() {
            List<SingleCol> presentCols = new ArrayList<>();
            List<ColumnMeta> customCols = new ArrayList<>( gotCols_.values() );

            /* Check declared column metadata for required columns. */
            int nreq = 0;
            SingleCol[] reqCols = toSingleCols( createMandatoryColumns() );
            for ( SingleCol reqCol : reqCols ) {
                String cname = reqCol.name_;
                ColumnMeta gotCol = gotCols_.get( cname );
                if ( gotCol != null ) {
                    nreq++;
                    presentCols.add( reqCol );
                    customCols.remove( gotCol );
                    checkMetadata( gotCol, reqCol );
                }
                else {
                    String msg = new StringBuffer()
                       .append( "Missing epn_core required column " )
                       .append( cname )
                       .append( " from table " )
                       .append( tname_ )
                       .toString();
                    reporter_.report( FixedCode.E_PN0C, msg );
                }
            }

            /* Set up a list of standard but optional columns. */
            Map<String,SingleCol> optMap = new LinkedHashMap<>();
            for ( SingleCol col : toSingleCols( getNonMandatoryColumns() ) ) {
                optMap.put( col.name_, col );
            }

            /* Identify columns in the presented table from the optional list
             * and check their metadata. */
            int nopt = 0;
            for ( ColumnMeta gotCol : epnMeta_.getColumns() ) {
                String cname = gotCol.getName();
                SingleCol stdCol = optMap.get( cname.toLowerCase() );
                if ( stdCol != null ) {
                    nopt++;
                    presentCols.add( stdCol );
                    customCols.remove( gotCol );
                    checkMetadata( gotCol, stdCol );
                }
            }

            /* Set up a map of all standard columns. */
            Map<String,SingleCol> stdColMap = new HashMap<>();
            stdColMap.putAll( optMap );
            for ( SingleCol reqCol : reqCols ) {
                stdColMap.put( reqCol.name_, reqCol );
            }

            /* Check actual datatypes using a sample result. */
            String adql1 = "SELECT TOP 1 * FROM " + tname_;
            TapQuery tq1 = new TapQuery( tapService_, adql1, null );
            StarTable table1 = tapRunner_.getResultTable( reporter_, tq1 );
            ColumnInfo[] cinfos = table1 == null
                                ? new ColumnInfo[ 0 ]
                                : Tables.getColumnInfos( table1 );
            for ( ColumnInfo cinfo : cinfos ) {
                String cname = cinfo.getName();
                SingleCol stdCol = stdColMap.get( cname.toLowerCase() );
                if ( stdCol != null ) {
                    stdCol.type_.checkInfo( reporter_, epnMeta_, cinfo );
                }
            }

            /* Report on non-standard columns; this can be useful when
             * checking by eye that column names aren't misspelt etc. */
            int nCustom = customCols.size();
            if ( nCustom > 0 ) {
                String msg = new StringBuffer()
                   .append( nCustom )
                   .append( nCustom == 1 ? " non-standard column in table "
                                         : " non-standard columns in table " )
                   .append( tname_ )
                   .append( " " )
                   .append( customCols )
                   .toString();
                reporter_.report( FixedCode.I_PNNS, msg );
            }

            /* Check min/max standard column pairs - see general rules
             * in section 1.2. */
            List<MinMaxCol> mmCols = new ArrayList<>();
            for ( EpnCol col : getAllColumns() ) {
                if ( col instanceof MinMaxCol ) {
                    mmCols.add( (MinMaxCol) col );
                }
            }

            /* Check min/max appear in pairs. */
            for ( MinMaxCol mmCol : mmCols ) {
                String minName = mmCol.names_.min_;
                String maxName = mmCol.names_.max_;
                boolean hasMin = gotCols_.get( minName ) != null;
                boolean hasMax = gotCols_.get( maxName ) != null;
                if ( hasMin != hasMax ) {
                    String msg = new StringBuffer()
                       .append( "Table " )
                       .append( tname_ )
                       .append( " has one but not both of " )
                       .append( minName )
                       .append( ", " )
                       .append( maxName )
                       .toString();
                    reporter_.report( FixedCode.E_PNMX, msg );
                }
                else if ( hasMin && hasMax ) {

                    /* Check they both (or neither) have values,
                     * and ordering constraints are observed. */
                    StringBuffer abuf = new StringBuffer()
                       .append( "SELECT TOP 1 " )
                       .append( gotCols_.containsKey( "granule_uid" )
                                ? "granule_uid, "
                                : "-1 AS dummy_uid, " )
                       .append( minName )
                       .append( ", " )
                       .append( maxName )
                       .append( " FROM " )
                       .append( tname_ )
                       .append( " WHERE (" )
                       .append( minName )
                       .append( " IS NULL AND " )
                       .append( maxName )
                       .append( " IS NOT NULL) OR (" )
                       .append( minName )
                       .append( " IS NOT NULL AND " )
                       .append( maxName )
                       .append( " IS NULL)" );

                    /* For longitude-like columns, min>max is OK since it's
                     * used to indicate a range crossing the anti-meridian. */
                    if ( ! mmCol.couldBeLongitude_ ) {
                        abuf.append( " OR (" )
                            .append( minName )
                            .append( " > " )
                            .append( maxName )
                            .append( ")" );
                    }
                    TableData tdata = runQuery( abuf.toString() );
                    if ( tdata != null && tdata.getRowCount() > 0 ) {
                        String msg = new StringBuffer()
                           .append( "Min/Max constraints violated for " )
                           .append( tname_ )
                           .append( " columns (" )
                           .append( minName )
                           .append( ", " )
                           .append( maxName )
                           .append( "): (" )
                           .append( tdata.getCell( 0, 1 ) )
                           .append( ", " )
                           .append( tdata.getCell( 0, 2 ) )
                           .append( ") at granule_id " )
                           .append( tdata.getCell( 0, 0 ) )
                           .toString();
                        reporter_.report( FixedCode.E_PNMM, msg );
                    }
                }
            }

            /* Attempt to check coordinate metadata.
             * The constraints on appropriate UCD and Unit values depend on
             * the spatial_frame_type, as explained in sec 2.1.4 and Table 2.
             * In principle, spatial_frame_type can vary over the table rows,
             * in which case obviously the column UCD and Unit metadata can't
             * represent them (since they'd be different for different rows).
             * But typically spatial_frame_type is fixed for all rows,
             * in which case there is a most-appropriate Unit/UCD value
             * for the c1 and c_resol_ columns, so check that here.
             * But given the above, anomalies can't really be reported
             * as errors, only warnings. */
            String ftypeAdql = new StringBuffer()
               .append( "SELECT DISTINCT TOP 2 spatial_frame_type FROM " )
               .append( tname_ )
               .append( " WHERE spatial_frame_type IS NOT NULL" )
               .toString();
            TableData ftypeData = runQuery( ftypeAdql );

            /* If we have a fixed and known spatial_frame_type, proceed. */
            if ( ftypeData != null && ftypeData.getRowCount() == 1 ) {
                Object ftypeName = ftypeData.getCell( 0, 0 );
                FrameType ftype =
                      ftypeName instanceof String
                    ? FrameType.valueOf( ((String) ftypeName).toUpperCase() )
                    : null;
                if ( ftype != null ) {
                    String msg = "Checking coordinate metadata for "
                               + "spatial_frame_type="
                               + ftype.toString().toLowerCase();
                    reporter_.report( FixedCode.I_PNSP, msg );

                    /* Do the actual checking here. */
                    checkSpatialMeta( ftype );
                }
            }

            /* Check column content using column-specific rules where
             * available. */
            for ( SingleCol stdCol : presentCols ) {
                ContentChecker checker = stdCol.checker_;
                if ( checker != null ) {
                    checker.checkContent( stdCol, this );
                }
            }

            /* Summarise columns. */
            int nother = gotCols_.size() - nreq - nopt;
            String msg = new StringBuffer()
               .append( tname_ )
               .append( " has " )
               .append( nreq )
               .append( "/" )
               .append( reqCols.length )
               .append( " EPN-TAP required, " )
               .append( nopt )
               .append( " EPN-TAP standard, and " )
               .append( nother )
               .append( " custom columns" )
               .toString();
            reporter_.report( FixedCode.S_PNCS, msg );
        }

        /**
         * Runs an ADQL query and returns the result, if any.
         * Only suitable for tables with small results.
         *
         * @param  adql   query string
         * @return   result table, or null
         */
        private TableData runQuery( CharSequence adql ) {
            TapQuery tq = new TapQuery( tapService_, adql.toString(), null );
            StarTable table = tapRunner_.getResultTable( reporter_, tq );
            return TableData.createTableData( reporter_, table );
        }

        /**
         * Checks that the actual column metadata matches standard values.
         * This method just checks UCD and Unit; datatype is checked elsewhere.
         *
         * @param  gotCol  column in test table
         * @param  stdCol  column declared by standard
         */
        private void checkMetadata( ColumnMeta gotCol, SingleCol stdCol ) {
            String cname = gotCol.getName();

            /* A null value in the standard metadata means
             * unconstrained; an empty string "" in the standard metadata
             * means that blank is required. */
            if ( stdCol.ucd_ != null ) {
                compareItem( cname, "UCD", FixedCode.E_CUCD,
                             stdCol.ucd_, gotCol.getUcd() );
            }
            if ( stdCol.unit_ != null ) {
                compareItem( cname, "Unit", FixedCode.E_CUNI,
                             stdCol.unit_, gotCol.getUnit() );
            }
        }

        /**
         * Compares a test metadata item with a standard value and
         * reports as required.
         *
         * @param  colName  column name
         * @param  itemName  label for type of metadata item
         * @param  code   report code for mismatch message
         * @param  stdValue   standard value for item
         * @param  gotValue   test value for item
         */
        private void compareItem( String colName,
                                  String itemName, ReportCode code,
                                  String stdValue, String gotValue ) {

            /* Null standard value means no constraint on test value. */
            if ( stdValue == null ) {
                return;
            }

            /* Empty standard value means the test value must also be
             * blank (empty or NULL). */
            if ( stdValue.length() == 0 &&
                 ( gotValue == null || gotValue.trim().length() == 0 ) ) {
                return;
            }

            /* Otherwise just test and report in case of a mismatch. */
            String vGot = gotValue == null || gotValue.trim().length() == 0
                        ? "null"
                        : gotValue;
            String vStd = stdValue == null ? "null" : stdValue;
            if ( ! vGot.equals( vStd ) ) {
                String msg = new StringBuffer()
                   .append( "Wrong " )
                   .append( itemName )
                   .append( " for " )
                   .append( tname_ )
                   .append( " column " )
                   .append( colName )
                   .append( ": \"" )
                   .append( gotValue )
                   .append( "\" != \"" )
                   .append( stdValue )
                   .append( "\"" )
                   .toString();
                reporter_.report( code, msg );
            }
        }

        /**
         * Checks the spatial coordinate column metadata for a given
         * spatial frame type.
         * As explained in Section 2.1.4 and Table 2, the utypes and
         * units of the c[123](min|max) and c_resol_[123](min|max)
         * columns depend on the value of the spatial_frame_type column.
         *
         * @param  ftype  represents the spatial_frame_type value
         */
        private void checkSpatialMeta( FrameType ftype ) {

            /* Go through each column of the test table looking for columns
             * to which this checking can apply. */
            Pattern ciRegex = Pattern.compile( "c([123])(_resol_)?(min|max)" );
            for ( ColumnMeta cmeta : epnMeta_.getColumns() ) {
                String cname = cmeta.getName();
                String gotUcd = cmeta.getUcd();
                String gotUnit = cmeta.getUnit();
                Matcher matcher = ciRegex.matcher( cname.toLowerCase() );
                if ( matcher.matches() ) {
                    int idim0 = Integer.parseInt( matcher.group( 1 ) ) - 1;
                    boolean isResol =
                        "_resol_".equalsIgnoreCase( matcher.group( 2 ) );
                    boolean isMax = "max".equalsIgnoreCase( matcher.group( 3 ));

                    /* Work out the units and UCDs that this column
                     * should have. */
                    String stdUnit = ftype.units_[ idim0 ];
                    String[] stdUcds = isResol
                                     ? new String[] { ftype.resolUcd( idim0 ) }
                                     : ftype.ucds_[ idim0 ];

                    /* Check equivalence and report. */
                    if ( stdUnit != null && ! stdUnit.equals( gotUnit ) ) {
                        String msg = new StringBuffer()
                           .append( "Coordinate unit mismatch: " )
                           .append( cname )
                           .append( " in table " )
                           .append( tname_ )
                           .append( " has unit \"" )
                           .append( gotUnit )
                           .append( "\"; for spatial_frame_type \"" )
                           .append( ftype.toString().toLowerCase() )
                           .append( "\" recommended unit is \"" )
                           .append( stdUnit )
                           .append( "\"" )
                           .toString();
                        reporter_.report( FixedCode.W_SPUN, msg );
                    }
                    if ( stdUcds != null ) {
                        List<String> mmStdUcds =
                            Arrays.stream( stdUcds )
                                  .map( u -> isMax ? toMaxUcd( u )
                                                   : toMinUcd( u ) )
                                  .map( String::toLowerCase )
                                  .collect( Collectors.toList() );
                        if ( ! mmStdUcds.contains( gotUcd.toLowerCase() ) ) {
                            StringBuffer mbuf = new StringBuffer()
                               .append( "Coordinate UCD mismatch: " )
                               .append( cname )
                               .append( " in table " )
                               .append( tname_ )
                               .append( " has UCD \"" )
                               .append( gotUcd )
                               .append( "\"; for spatial_frame_type \"" )
                               .append( ftype.toString().toLowerCase() )
                               .append( "\" recommended UCD" );
                            if ( mmStdUcds.size() == 1 ) {
                                mbuf.append( " is \"" )
                                    .append( mmStdUcds.get( 0 ) )
                                    .append( "\"" );
                            }
                            else {
                                mbuf.append( " are \"" )
                                    .append( mmStdUcds );
                            }
                            reporter_.report( FixedCode.W_SPUC,
                                              mbuf.toString() );
                        }
                    }
                }
            }
        }
    }

    /**
     * Converts an array of EpnCol values to an array of SingleCol values.
     * MinMaxCols may be expanded to pairs.
     *
     * @param  cols  input column list
     * @retur  single column list
     */
    static SingleCol[] toSingleCols( EpnCol[] cols ) {
        List<SingleCol> list = new ArrayList<>();
        for ( EpnCol col : cols ) {
            if ( col instanceof SingleCol ) {
                list.add( (SingleCol) col );
            }
            else if ( col instanceof MinMaxCol ) {
                MinMaxCol mcol = (MinMaxCol) col;
                list.add( mcol.minCol() );
                list.add( mcol.maxCol() );
            }
            else {
                throw new AssertionError();
            }
        }
        return list.toArray( new SingleCol[ 0 ] );
    }

    /**
     * Returns an array of all the standard columns known.
     * This includes both mandatory and optional entries.
     *
     * @return   all known columns
     */
    static EpnCol[] getAllColumns() {
        List<EpnCol> list = new ArrayList<>();
        list.addAll( Arrays.asList( createMandatoryColumns() ) );
        list.addAll( Arrays.asList( createOptionalColumns() ) );
        list.addAll( Arrays.asList( createExtensionColumns() ) );
        return list.toArray( new EpnCol[ 0 ] );
    }

    /**
     * Returns a list of all the non-mandatory columns.
     *
     * @return  columns
     */
    private static EpnCol[] getNonMandatoryColumns() {
        List<EpnCol> list = new ArrayList<>();
        list.addAll( Arrays.asList( createOptionalColumns() ) );
        list.addAll( Arrays.asList( createExtensionColumns() ) );
        return list.toArray( new EpnCol[ 0 ] );
    }

    /**
     * Creates a map from column ID to column from an array of EpnCols.
     *
     * @param  cols  columns
     * @return  id -&gt; column map
     */
    private static Map<String,EpnCol> toEpnColMap( EpnCol[] cols ) {
        Map<String,EpnCol> map = new LinkedHashMap<>();
        for ( EpnCol col : cols ) {
            map.put( col.id_, col );
        }
        return map;
    }

    /**
     * Creates a list of all the mandatory columns defined by the EPN-TAP
     * standard (section 2.1).
     *
     * @return   mandatory column list
     */
    private static EpnCol[] createMandatoryColumns() {
        Map<String,EpnCol> colMap = toEpnColMap( new EpnCol[] {
            textCol( "granule_uid", "meta.id" ),
            textCol( "granule_gid", "meta.id" ),
            textCol( "obs_id", "meta.id;obs" ),
            textCol( "dataproduct_type", "meta.code.class" ),
            textCol( "target_name", "meta.id;src" ),
            textCol( "target_class", "src.class" ),
            new MinMaxCol( "time_", Type.DOUBLE, "d",
                           new String2( "time.start;obs", "time.end;obs" ) ),
            new MinMaxCol( "time_sampling_step_", Type.DOUBLE, "s",
                           minMaxStats( "time.resolution" ) ),
            new MinMaxCol( "time_exp_", Type.DOUBLE, "s",
                           minMaxStats( "time.duration;obs.exposure" )),
            new MinMaxCol( "spectral_range_", Type.DOUBLE, "Hz",
                           minMaxStats( "em.freq" ) ),
            new MinMaxCol( "spectral_sampling_step_", Type.DOUBLE, "Hz",
                           minMaxStats( "em.freq;spect.binSize" ) ),
            new MinMaxCol( "spectral_resolution_", Type.DOUBLE, "",
                           minMaxStats( "spect.resolution" ) ),
            new MinMaxCol( "c1", Type.DOUBLE, null, null ),
            new MinMaxCol( "c2", Type.DOUBLE, null, null ),
            new MinMaxCol( "c3", Type.DOUBLE, null, null ),
            new SingleCol( "s_region", Type.SPOLY, null,
                           "pos.outline;obs.field" ),
            new MinMaxCol( "c1_resol_", Type.DOUBLE, null, null ),
            new MinMaxCol( "c2_resol_", Type.DOUBLE, null, null ),
            new MinMaxCol( "c3_resol_", Type.DOUBLE, null, null ),
            textCol( "spatial_frame_type", "meta.code.class;pos.frame" ),
            new MinMaxCol( "incidence_", Type.DOUBLE, "deg",
                           minMaxStats( "pos.incidenceAng" ) ),
            new MinMaxCol( "emergence_", Type.DOUBLE, "deg",
                           minMaxStats( "pos.emergenceAng" ) ),
            new MinMaxCol( "phase_", Type.DOUBLE, "deg",
                           minMaxStats( "pos.phaseAng" ) ),
            textCol( "instrument_host_name", "meta.id;instr.obsty" ),
            textCol( "instrument_name", "meta.id;instr" ),
            textCol( "measurement_type", "meta.ucd" ),
            new SingleCol( "processing_level", Type.INTEGER, null,
                           "meta.calibLevel" ),
            new SingleCol( "creation_date", Type.TIMESTAMP, null,
                           "time.creation" ),
            new SingleCol( "modification_date", Type.TIMESTAMP, null,
                           "time.processing" ),
            new SingleCol( "release_date", Type.TIMESTAMP, null,
                           "time.release" ),
            textCol( "service_title", "meta.title" ),
        } );

        /* First mark columns marked in the standard as "must be informed"
         * (bold in Table 1).  Note some of these checkers are superceded
         * by more specific tests in the following lines. */
        ContentChecker notNull = notNullChecker();
        colMap.get( "granule_uid" ).checker_ = notNull;
        colMap.get( "granule_gid" ).checker_ = notNull;
        colMap.get( "obs_id" ).checker_ = notNull;
        colMap.get( "dataproduct_type" ).checker_ = notNull;
        colMap.get( "creation_date" ).checker_ = notNull;
        colMap.get( "modification_date" ).checker_ = notNull;
        colMap.get( "release_date" ).checker_ = notNull;
        colMap.get( "service_title" ).checker_ = notNull;

        /* Note (potentially) longitude-like MinMaxCols. */
        for ( String cname : new String[] { "c1", "c2", "c3" } ) {
            ((MinMaxCol) colMap.get( cname )).couldBeLongitude_ = true;
        }

        /* Add specific content checkers. */
        colMap.get( "time_" ).checker_ = jdChecker();
        colMap.get( "spatial_frame_type" ).checker_ =
                optionsChecker( false, FixedCode.E_PNFT, new String[] {
            "celestial", "body", "cartesian", "spherical",
            "cylindrical", "none", "healpix",
        } );
        colMap.get( "s_region" ).checker_ = sregionChecker();
        colMap.get( "dataproduct_type" ).checker_ =
                hashlistOptionsChecker( false, FixedCode.E_PNDP, new String[] {
            "im", "ma", "sp", "ds", "sc", "pr", "pf", "vo", "mo",
            "cu", "ts", "ca", "ci", "sv", "ev",
        } );
        colMap.get( "processing_level" ).checker_ =
            rangeChecker( true, FixedCode.E_PNPL, "1", "6" );
        colMap.get( "target_class" ).checker_ =
                hashlistOptionsChecker( false, FixedCode.E_PNTG, new String[] {
            "asteroid", "dwarf_planet", "planet", "satellite", "comet",
            "exoplanet", "interplanetary_medium", "sample", "sky",
            "spacecraft", "spacejunk", "star", "calibration",
        } );

        colMap.get( "service_title" ).checker_ = serviceTitleChecker();
        ContentChecker timestampChecker = timestampChecker( false );
        colMap.get( "creation_date" ).checker_ = timestampChecker;
        colMap.get( "modification_date" ).checker_ = timestampChecker;
        colMap.get( "release_date" ).checker_ = timestampChecker;

        return colMap.values().toArray( new EpnCol[ 0 ] );
    }

    /**
     * Creates a list of the basic optional columns defined by the EPN-TAP
     * standard (section 2.2).
     *
     * @return   optional column list
     */
    private static EpnCol[] createOptionalColumns() {
        Map<String,EpnCol> colMap = toEpnColMap( new EpnCol[] {
            textCol( "access_url", "meta.ref.url;meta.file" ),
            textCol( "access_format", "meta.code.mime" ),
            new SingleCol( "access_estsize", Type.INTEGER, "kbyte",
                           "phys.size;meta.file" ),
            textCol( "access_md5", "meta.checksum;meta.file" ),
            textCol( "thumbnail_url", "meta.ref.url;meta.preview" ),
            textCol( "file_name", "meta.id;meta.file" ),
            textCol( "datalink_url", "meta.ref.url" ),
            textCol( "species", "meta.id;phys.atmol" ),
            textCol( "messenger", "instr.bandpass" ),
            textCol( "alt_target_name", "meta.id;src" ),
            textCol( "target_region", "meta.id;src;obs.field" ),
            textCol( "feature_name", "meta.id;src;obs.field" ),
            textCol( "publisher", "meta.curation" ),
            textCol( "bib_reference", "meta.bib" ),
            textCol( "internal_reference", "meta.id.cross" ),
            textCol( "external_link", "meta.ref.url" ),
            new SingleCol( "shape", Type.TEXT_MOC, null,
                           "pos.outline;obs.field" ),
            textCol( "spatial_coordinate_description",
                     "meta.code.class;pos.frame" ),
            textCol( "spatial_origin", "meta.ref;pos.frame" ),
            textCol( "time_refposition", "meta.ref;time.scale" ),
            textCol( "time_scale", "time.scale" ),
            new MinMaxCol( "subsolar_longitude_", Type.DOUBLE, "deg",
                           minMaxStats( "pos.bodyrc.lon" ) ),
            new MinMaxCol( "subsolar_latitude_", Type.DOUBLE, "deg",
                           minMaxStats( "pos.bodyrc.lat" ) ),
            new MinMaxCol( "subobserver_longitude_", Type.DOUBLE, "deg",
                           minMaxStats( "pos.bodyrc.lon" ) ),
            new MinMaxCol( "subobserver_latitude_", Type.DOUBLE, "deg",
                           minMaxStats( "pos.bodyrc.lat" ) ),
            new SingleCol( "ra", Type.DOUBLE, "deg", "pos.eq.ra;meta.main" ),
            new SingleCol( "dec", Type.DOUBLE, "deg", "pos.eq.dec;meta.main" ),
            new MinMaxCol( "radial_distance_", Type.DOUBLE, "km",
                           minMaxStats( "pos.distance;pos.bodyrc" ) ),
            new MinMaxCol( "altitude_fromshape_", Type.DOUBLE, "km",
                           minMaxStats( "pos.bodyrc.alt" ) ),
            new MinMaxCol( "solar_longitude_", Type.DOUBLE, "deg",
                           minMaxStats( "pos.ecliptic.lon;pos.heliocentric" ) ),
            new MinMaxCol( "local_time_", Type.DOUBLE, "h",
                           minMaxStats( "time.phase;time.period.rotation" ) ),
            new MinMaxCol( "target_distance_", Type.DOUBLE, "km",
                           minMaxStats( "pos.distance" ) ),
            new MinMaxCol( "target_time_", Type.TIMESTAMP, null,
                           new String2( "time.start;src", "time.end;src" ) ),
            new MinMaxCol( "earth_distance_", Type.DOUBLE, "AU",
                           minMaxStats( "pos.distance" ) ),
            new MinMaxCol( "sun_distance_", Type.DOUBLE, "AU",
                           minMaxStats( "pos.distance" ) ),
        } );

        for ( String cname : new String[] { "subsolar_longitude_",
                                            "subobserver_longitude_",
                                            "solar_longitude_", } ) {
            ((MinMaxCol) colMap.get( cname )).couldBeLongitude_ = true;
        }

        colMap.get( "local_time_" ).checker_ =
            rangeChecker( true, FixedCode.E_PNLT, "0", "24" );
        colMap.get( "target_time_" ).checker_ = timestampChecker( true );
        colMap.get( "messenger" ).checker_ =
            vocabChecker( true, MESSENGER_VOCAB,
                          FixedCode.E_PNMG, FixedCode.W_VCPD );
        colMap.get( "time_scale" ).checker_ =
            vocabChecker( true, VocabChecker.TIMESCALE,
                          FixedCode.W_PNTS, FixedCode.W_VCPD );

        // Currently not tested but could be: compliant MOC strings in
        // shape column.
         
        return colMap.values().toArray( new EpnCol[ 0 ] );
    }

    /**
     * Creates a list of the "extension" columns defined by the EPN-TAP
     * standard (section 2.3).
     *
     * @return   extension column list
     */
    private static EpnCol[] createExtensionColumns() {
        Map<String,EpnCol> colMap = toEpnColMap( new EpnCol[] {
            textCol( "obs_mode", "meta.code;instr.setup" ),
            textCol( "detector_name", "meta.id;instr.det" ),
            textCol( "opt_elem", "meta.id;instr.param" ),
            textCol( "filter", "meta.id;instr.filter" ),
            textCol( "instrument_type", "meta.id;instr" ),
            textCol( "acquisition_id", "meta.id" ),
            textCol( "proposal_id", "meta.id;obs.proposal" ),
            textCol( "proposal_pi", "meta.id.PI;obs.proposal" ),
            textCol( "proposal_title", "meta.title;obs.proposal" ),
            textCol( "campaign", "meta.id;obs.proposal" ),
            textCol( "target_description", "meta.note;src" ),
            textCol( "proposal_target_name", "meta.note;obs.proposal" ),
            new SingleCol( "target_apparent_radius", Type.DOUBLE, "arcsec",
                           "phys.angSize;src" ),
            new SingleCol( "north_pole_position", Type.DOUBLE, "deg",
                           "pos.posAng" ),
            textCol( "target_primary_hemisphere", "meta.id;obs.field" ),
            textCol( "target_secondary_hemisphere", "meta.id;obs.field" ),
            new SingleCol( "platesc", Type.DOUBLE, "arcsec/pix",
                           "instr.scale" ),
            new SingleCol( "orientation", Type.DOUBLE, "deg", "pos.posAng" ),
            textCol( "observer_name", "obs.observer;meta.main" ),
            textCol( "observer_institute", "meta.note" ),
            new SingleCol( "observer_id", Type.INTEGER, null, "meta.id.PI" ),
            textCol( "observer_code", "meta.id.PI" ),
            textCol( "observer_country", "meta.note;obs.observer" ),
            new SingleCol( "observer_lon", Type.DOUBLE, "deg",
                           "obs.observer;pos.earth.lon" ),
            new SingleCol( "observer_lat", Type.DOUBLE, "deg",
                           "obs.observer;pos.earth.lat" ),
            new SingleCol( "mass", Type.DOUBLE, "kg", "phys.mass" ),
            new SingleCol( "sidereal_rotation_period", Type.DOUBLE, "h",
                           "time.period.rotation" ),
            new SingleCol( "mean_radius", Type.DOUBLE, "km",
                           "phys.size.radius" ),
            new SingleCol( "equatorial_radius", Type.DOUBLE, "km",
                           "phys.size.radius" ),
            new SingleCol( "polar_radius", Type.DOUBLE, "km",
                           "phys.size.radius" ),
            new SingleCol( "diameter", Type.DOUBLE, "km",
                           "phys.size.diameter" ),
            new SingleCol( "semi_major_axis", Type.DOUBLE, "AU",
                           "phys.size.smajAxis" ),
            new SingleCol( "inclination", Type.DOUBLE, "deg",
                           "src.orbital.inclination" ),
            new SingleCol( "eccentricity", Type.DOUBLE, null,
                           "src.orbital.eccentricity" ),
            new SingleCol( "long_asc", Type.DOUBLE, "deg", "src.orbital.node" ),
            new SingleCol( "arg_perihel", Type.DOUBLE, "deg",
                           "src.orbital.periastron" ),
            new SingleCol( "mean_anomaly", Type.DOUBLE, "deg",
                           "src.orbital.meanAnomaly" ),
            textCol( "dynamical_class", "meta.code.class;src" ),
            textCol( "dynamical_type", "meta.code.class;src" ),
            textCol( "taxonomy_code", "src.class.color" ),
            new SingleCol( "magnitude", Type.DOUBLE, "mag", "phys.magAbs" ),
            new SingleCol( "flux", Type.DOUBLE, "mJy", "phot.flux.density" ),
            new SingleCol( "albedo", Type.DOUBLE, null, "phys.albedo" ),
            textCol( "map_projection", "pos.projection" ),
            new SingleCol( "map_height", Type.DOUBLE, "pix", "phys.size" ),
            new SingleCol( "map_width", Type.DOUBLE, "pix", "phys.size" ),
            textCol( "map_scale", "pos.wcs.scale" ),
            new MinMaxCol( "pixelscale_", Type.DOUBLE, "km/pix",
                           minMaxStats( "instr.scale" ) ),
            textCol( "particle_spectral_type", "meta.id;phys.particle" ),
            new MinMaxCol( "particle_spectral_range_", Type.DOUBLE, null,
                           null ),
            new MinMaxCol( "particle_spectral_sampling_step_", Type.DOUBLE,null,
                           minMaxStats( "spect.resolution;phys.particle" ) ),
            new MinMaxCol( "particle_spectral_resolution_", Type.DOUBLE, null,
                           minMaxStats( "spect.resolution;phys.particle" ) ),
            textCol( "original_publisher", "meta.note" ),
            textCol( "producer_name", "meta.note" ),
            textCol( "producer_institute", "meta.note" ),
            textCol( "sample_id", "meta.id;src" ),
            textCol( "sample_classification", "meta.note;phys.composition" ),
            textCol( "sample_desc", "meta.note" ),
            textCol( "species_inchikey", "meta.id;phys.atmol" ),
            textCol( "data_calibration_desc", "meta.note" ),
            textCol( "setup_desc", "meta.note" ),
            textCol( "geometry_type", "meta.note;instr.setup" ),
            textCol( "spectrum_type", "meta.note;instr.setup" ),
            new MinMaxCol( "grain_size_", Type.DOUBLE, "um",
                           minMaxStats( "phys.size" ) ), 
            new MinMaxCol( "azimuth_", Type.DOUBLE, "deg",
                           minMaxStats( "pos.azimuth" ) ),
            new SingleCol( "pressure", Type.DOUBLE, "bar", "phys.pressure" ),
            textCol( "measurement_atmosphere", "meta.note;phys.pressure" ),
            new SingleCol( "temperature", Type.DOUBLE, "K",
                           "phys.temperature" ),
            textCol( "event_type", "meta.code.class" ),
            textCol( "event_status", "meta.code.status" ),
            textCol( "event_cite", "meta.code.status" ),
        } );

        ((MinMaxCol) colMap.get( "azimuth_" )).couldBeLongitude_ = true;

        colMap.get( "geometry_type" ).checker_ =
                optionsChecker( true, FixedCode.E_PNGT, new String[] {
            "direct", "specular", "bidirectional", "directional-conical",
            "conical-directional", "biconical", "directional-hemispherical",
            "conical-hemispherical", "hemispherical-directional",
            "hemispherical-conical", "bihemispherical", "directional",
            "conical", "hemispherical", "other geometry", "unknown",
        } );
        colMap.get( "species_inchikey" ).checker_ = inchikeyListChecker();

        // Currently NOT tested but could be: particle_spectral_range,
        // which has UCDs and units dependent on particle_spectral_type value.
        // At time of writing, no services with the particle_spectral_type
        // column are registered.
        colMap.get( "particle_spectral_type" ).checker_ =
                optionsChecker( true, FixedCode.E_PPST, new String[] {
            "energy", "mass", "mass/charge",
        } );

        return colMap.values().toArray( new EpnCol[ 0 ] );
    }

    /**
     * Converts a basic UCD to one with ";stat.min" appended.
     *
     * @param   ucd  base UCD
     * @return   UCD for minimum
     */
    static String toMinUcd( String ucd ) {
        return ucd == null ? null : ucd + ";stat.min";
    }

    /**
     * Converts a basic UCD to one with ";stat.max" appended.
     *
     * @param   ucd  base UCD
     * @return   UCD for maximum
     */
    static String toMaxUcd( String ucd ) {
        return ucd == null ? null : ucd + ";stat.max";
    }

    /**
     * Utility method to create a SingleColumn with text type and no units.
     *
     * @param  name  column name
     * @param  ucd   UCD
     * @return  column
     */
    private static SingleCol textCol( String name, String ucd ) {
        return new SingleCol( name, Type.TEXT, null, ucd );
    }

    /**
     * Takes a base UCD and turns it into a min, max pair.
     *
     * @param  base  base UCD
     * @return  pair of min, max UCDs
     */
    private static String2 minMaxStats( String base ) {
        return new String2( toMinUcd( base ), toMaxUcd( base ) );
    }

    /**
     * Creates a check that there are no NULL values in a column.
     *
     * @return  new checker
     */
    private static ContentChecker notNullChecker() {
        return ( stdCol, runner ) -> {
            String cname = stdCol.name_;
            String tname = runner.tname_;
            String adql = new StringBuffer()
               .append( "SELECT " )
               .append( "TOP 1 " )
               .append( cname )
               .append( " FROM " )
               .append( tname )
               .append( " WHERE " )
               .append( cname )
               .append( " IS NULL" )
               .toString();
            TableData tdata = runner.runQuery( adql );
            if ( tdata != null && tdata.getRowCount() > 0 ) {
                String msg = new StringBuffer()
                   .append( "NULL values in " )
                   .append( tname )
                   .append( " non-nullable column " )
                   .append( cname )
                   .toString();
                runner.reporter_.report( FixedCode.E_PNUL, msg );
            }
        };
    }

    /**
     * Creates a check that a column contains only values from a
     * given fixed list of options.
     *
     * @param  isNullable  true iff null value is legal
     * @param  failCode  code with which to report an illegal value
     * @param  opts   legal values of column
     * @return   new checker;
     */
    private static ContentChecker optionsChecker( final boolean isNullable,
                                                  ReportCode failCode,
                                                  final String[] opts ) {
        return ( stdCol, runner ) -> {
            String cname = stdCol.name_;
            String tname = runner.tname_;
            StringBuffer abuf = new StringBuffer()
               .append( "SELECT TOP 1 " )
               .append( cname )
               .append( " FROM " )
               .append( tname )
               .append( " WHERE " )
               .append( cname )
               .append( " NOT IN (" )
               .append( Arrays.stream( opts )
                              .map( s -> "'" + s + "'" )
                              .collect( Collectors.joining( ", " ) ) )
               .append( ")" );
            if ( ! isNullable ) {
                abuf.append( " OR " )
                    .append( cname )
                    .append( " IS NULL" );
            }
            TableData tdata = runner.runQuery( abuf.toString() );
            if ( tdata != null && tdata.getRowCount() > 0 ) {
                Object badValue = tdata.getCell( 0, 0 );
                StringBuffer mbuf = new StringBuffer();
                if ( ! isNullable && badValue == null ) {
                    mbuf.append( "NULL value in " )
                        .append( tname )
                        .append( " non-nullable" );
                }
                else {
                    mbuf.append( "Illegal value \"" )
                        .append( badValue )
                        .append( "\" in " )
                        .append( tname );
                }
                String msg = 
                    mbuf.append( " column " )
                        .append( cname )
                        .append( "; legal values are " )
                        .append( Arrays.toString( opts ) )
                        .toString();
                runner.reporter_.report( failCode, mbuf.toString() );
            }
        };
    }

    /**
     * Creates a check that a column contains only hashlist values
     * (which may include single values) from a given fixed list of options.
     *
     * @param  isNullable  true iff null value is legal
     * @param  failCode  code with which to report an illegal value
     * @param  opts   legal values of column
     * @return   new checker;
     */
    private static ContentChecker
            hashlistOptionsChecker( final boolean isNullable,
                                    final ReportCode failCode,
                                    final String[] opts ) {
        return ( stdCol, runner ) -> {
            String cname = stdCol.name_;
            String tname = runner.tname_;
            String adql = new StringBuffer()
               .append( "SELECT DISTINCT TOP 30 " )
               .append( cname )
               .append( " FROM " )
               .append( tname )
               .toString();
            TableData tdata = runner.runQuery( adql );
            Set<String> gotSet = new LinkedHashSet<>();
            if ( tdata != null && tdata.getRowCount() > 0 ) {
                for ( Object val : tdata.getColumn( 0 ) ) {
                    if ( val == null || "".equals( val ) ) {
                        gotSet.add( null );
                    }
                    else if ( val instanceof String ) {
                        String sval = (String) val;
                        gotSet.addAll( Arrays.asList( sval.split( "#", -1 ) ) );
                    }
                }
            }
            boolean hasNull = gotSet.remove( "" ) | gotSet.remove( null );
            gotSet.removeAll( Arrays.asList( opts ) );
            if ( hasNull && ! isNullable ) {
                String msg = new StringBuffer()
                   .append( "NULL values in " )
                   .append( tname )
                   .append( " non-nullable column " )
                   .append( cname )
                   .toString();
                runner.reporter_.report( FixedCode.E_PNUL, msg );
            }
            if ( gotSet.size() > 0 ) {
                String msg = new StringBuffer()
                   .append( "Illegal items in " )
                   .append( tname )
                   .append( " hashlist column " )
                   .append( cname )
                   .append( " " )
                   .append( gotSet )
                   .append( "; legal values are " )
                   .append( Arrays.toString( opts ) )
                   .toString();
                runner.reporter_.report( failCode, msg );
            }
        };
    }

    /**
     * Creates a check that a column contains only values in a given range.
     *
     * @param  isNullable  true iff null value is legal
     * @param  failCode  code with which to report an illegal value
     * @param  loLimit  ADQL representation of lowest permitted value
     * @param  hiLimit  ADQL representation of highest permitted value
     * @return  new checker
     */
    private static ContentChecker rangeChecker( boolean isNullable,
                                                ReportCode failCode, 
                                                String loLimit,
                                                String hiLimit ) {
        return ( stdCol, runner ) -> {
            String cname = stdCol.name_;
            String tname = runner.tname_;
            StringBuffer abuf = new StringBuffer()
               .append( "SELECT TOP 1 " )
               .append( cname )
               .append( " FROM " )
               .append( tname )
               .append( " WHERE " )
               .append( "(NOT " )
               .append( cname )
               .append( " BETWEEN " )
               .append( loLimit )
               .append( " AND " )
               .append( hiLimit )
               .append( ")" );
            if ( ! isNullable ) {
                abuf.append( " OR (" )
                    .append( cname )
                    .append( " IS NULL)" );
            }
            TableData tdata = runner.runQuery( abuf.toString() );
            if ( tdata != null && tdata.getRowCount() > 0 ) {
                Object badValue = tdata.getCell( 0, 0 );
                StringBuffer mbuf = new StringBuffer();
                if ( ! isNullable && badValue == null ) {
                    mbuf.append( "NULL value for non-nullable " );
                }
                else {
                    mbuf.append( "Value " )
                        .append( badValue )
                        .append( " out of range " )
                        .append( loLimit )
                        .append( "..." )
                        .append( hiLimit )
                        .append( " in " );
                }
                mbuf.append( tname )
                    .append( " column " )
                    .append( cname );
                runner.reporter_.report( failCode, mbuf.toString() );
            }
        };
    }

    /**
     * Creates a check for column content within a given VO Vocabulary.
     *
     * @param  isNullable  true iff NULLs are permitted
     * @param  vChecker   vocabulary definition
     * @param  errorCode  code for unknown or disallowed NULL values
     * @param  flagCode  code for values flagged as deprecated or preliminary
     * @return   new checker
     */
    private static ContentChecker vocabChecker( boolean isNullable,
                                                VocabChecker vChecker,
                                                ReportCode errorCode,
                                                ReportCode flagCode ) {
        return ( stdCol, runner ) -> {
            String cname = stdCol.name_;
            String tname = runner.tname_;
            String adql = new StringBuffer()
               .append( "SELECT DISTINCT TOP 10 " )
               .append( cname )
               .append( " FROM " )
               .append( tname )
               .append( " WHERE " )
               .append( cname )
               .append( isNullable ? " IS NOT NULL AND "
                                   : " IS NULL OR " )
               .append( cname )
               .append( " NOT IN (" )
               .append( vChecker.getFixedTerms()
                                .stream()
                                .map( s -> "'" + s + "'" )
                                .collect( Collectors.joining( ", " ) ) )
               .append( ")" )
               .toString();
            TableData tdata = runner.runQuery( adql );
            if ( tdata != null && tdata.getRowCount() > 0 ) {
                Object[] values = tdata.getColumn( 0 );
                boolean hasNull = false;
                Set<String> prelims = new TreeSet<>();
                Set<String> deprecs = new TreeSet<>();
                Set<String> unknowns = new TreeSet<>();
                URL vocabUrl = vChecker.getVocabularyUrl();
                Map<String,VocabTerm> termMap = vChecker.getRetrievedTerms();
                for ( Object value : values ) {
                    VocabTerm term = termMap.get( value );
                    if ( value == null ) {
                        hasNull = true;
                    }
                    else if ( term == null ) {
                        unknowns.add( value.toString() );
                    }
                    else if ( term.isPreliminary() ) {
                        prelims.add( value.toString() );
                    }
                    else if ( term.isDeprecated() ) {
                        deprecs.add( value.toString() );
                    }
                }
                if ( ! isNullable && hasNull ) {
                    String msg = new StringBuffer()
                       .append( "NULL values in " )
                       .append( tname )
                       .append( " non-nullable column " )
                       .append( cname )
                       .toString();
                    runner.reporter_.report( errorCode, msg );
                }
                if ( unknowns.size() > 0 ) {
                    String msg = new StringBuffer()
                       .append( "Unknown values " )
                       .append( unknowns )
                       .append( " in " )
                       .append( tname )
                       .append( " column " )
                       .append( cname )
                       .append( " not in vocabulary " )
                       .append( vocabUrl )
                       .append( "; options are " )
                       .append( termMap.keySet() )
                       .toString();
                    runner.reporter_.report( errorCode, msg );
                }
                if ( deprecs.size() > 0 ) {
                    String msg = new StringBuffer()
                       .append( "Terms " )
                       .append( deprecs )
                       .append( " in " )
                       .append( tname )
                       .append( " column " )
                       .append( cname )
                       .append( " are deprecated in vocabulary " )
                       .append( vocabUrl )
                       .toString();
                    runner.reporter_.report( flagCode, msg );      
                }
                else if ( prelims.size() > 0 ) {
                    String msg = new StringBuffer()
                       .append( "Terms " )
                       .append( prelims )
                       .append( " in " )
                       .append( tname )
                       .append( " column " )
                       .append( cname )
                       .append( " are flagged preliminary in vocabulary " )
                       .append( vocabUrl )
                       .toString();
                    runner.reporter_.report( flagCode, msg );      
                }
            }
        };
    }

    /**
     * Creates a check that a column contains EPN-TAP-style ISO8601-style
     * timestamp strings.  The detailed form is defined in Note 4
     * to Table 1 (sec 3.1) and elsewhere in the standard.
     *
     * @param  isNullable  true iff null value is legal
     * @return  new checker
     */
    private static ContentChecker timestampChecker( boolean isNullable ) {
        return ( stdCol, runner ) -> {
            String cname = stdCol.name_;
            String tname = runner.tname_;
            StringBuffer abuf = new StringBuffer()
               .append( "SELECT DISTINCT TOP 10 " )
               .append( cname )
               .append( " FROM " )
               .append( tname );
            if ( isNullable ) {
                abuf.append( " WHERE " )
                    .append( cname )
                    .append( " IS NOT NULL" );
            }
            TableData tdata = runner.runQuery( abuf.toString() );
            if ( tdata != null && tdata.getRowCount() > 0 ) {
                Object[] values = tdata.getColumn( 0 );
                for ( Object val : values ) {
                    if ( val == null || "".equals( val ) ) {
                        if ( ! isNullable ) {
                            String msg = new StringBuffer()
                               .append( "NULL values in " )
                               .append( tname )
                               .append( " non-nullable column " )
                               .append( cname )
                               .toString();
                            runner.reporter_.report( FixedCode.E_PNUL, msg );
                        }
                    }
                    else {
                        String sval = (String) val;
                        if ( ! DALI_TIMESTAMP_REGEX.matcher( sval ).matches() ){
                            String msg = new StringBuffer()
                               .append( "Timestamp value \"" )
                               .append( sval )
                               .append( "\" in " )
                               .append( tname )
                               .append( " column " )
                               .append( cname )
                               .append( " does not match " )
                               .append( " does not match " )
                               .append( "YYYY-MM-DD['T'hh:mm:ss[.SSS]['Z']]" )
                               .toString();
                            runner.reporter_.report( FixedCode.E_PN86, msg );
                            return;
                        }
                    }
                }
            }
        };
    }

    /**
     * Creates a check that a column contains hashlists of inchikey strings.
     * Inchikeys are fixed-length (27-char) chemistry specifiers.
     *
     * @return  new checker
     */
    private static ContentChecker inchikeyListChecker() {
        return ( stdCol, runner ) -> {
            String cname = stdCol.name_;
            String tname = runner.tname_;
            StringBuffer abuf = new StringBuffer()
               .append( "SELECT DISTINCT TOP 30 " )
               .append( cname )
               .append( " FROM " )
               .append( tname )
               .append( " WHERE " )
               .append( cname )
               .append( " IS NOT NULL" );
            TableData tdata = runner.runQuery( abuf.toString() );
            if ( tdata != null && tdata.getRowCount() > 0 ) {
                Object[] values = tdata.getColumn( 0 );
                for ( Object val : values ) {
                    String sval = val instanceof String ? (String) val : null;
                    String badInchi = null;
                    String errmsg = null;
                    if ( sval != null && sval.trim().length() > 0 ) {
                        String[] items = sval.split( "#", -1 );
                        for ( String item : items ) {
                            String err = inchikeyError( item );
                            if ( err != null ) {
                                badInchi = item;
                                errmsg = err;
                            }
                        }
                    }
                    if ( badInchi != null ) {
                        String msg = new StringBuffer()
                           .append( "Bad InchiKey syntax (" )
                           .append( errmsg )
                           .append( ") in column " )
                           .append( cname )
                           .append( ": \"" )
                           .append( badInchi )
                           .append( '"' )
                           .toString();
                        runner.reporter_.report( FixedCode.E_PNIK, msg );
                    }
                }
            }
        };
    }

    /**
     * Check InchiKey syntax.
     *
     * @see  <a href="https://www.ncbi.nlm.nih.gov/pmc/articles/PMC4486400/"
     *          >Heller et al. 2015 (10.1186/s13321-015-0068-4)</a>
     * @param   txt  candidate inchikey
     * @return  null for correct syntax;
     *          if syntax is bad, a short explanation of what's wrong
     */
    private static String inchikeyError( String txt ) {
        final int inchikeyLeng = 27;
        if ( txt.length() != inchikeyLeng ) {
            return "not " + inchikeyLeng + " chars";
        }
        else if ( ! txt.matches( "^[-A-Z]*$" ) ) {
            return "chars not matching [-A-Z]";
        }
        else {
            return null;
        }
    }

    /**
     * Creates a check for the service_title column.
     * It has to be equal in every row to the table schema name.
     *
     * @return  new checker
     */
    private static ContentChecker serviceTitleChecker() {
        return ( stdCol, runner ) -> {
            String cname = stdCol.name_;
            String tname = runner.tname_;
            if ( tname.toLowerCase().endsWith( "." + EPNCORE_TNAME ) ) {
                String schema =
                    tname
                   .substring( 0, tname.length() - 1 - EPNCORE_TNAME.length() );
                String adql = new StringBuffer()
                   .append( "SELECT TOP 1 " )
                   .append( cname )
                   .append( " FROM " )
                   .append( tname )
                   .append( " WHERE " )
                   .append( cname )
                   .append( " != '" )
                   .append( schema )
                   .append( "'" )
                   .append( " OR " )
                   .append( cname )
                   .append( " IS NULL" )
                   .toString();
                TableData tdata = runner.runQuery( adql );
                if ( tdata != null && tdata.getRowCount() > 0 ) {
                    Object badval = tdata.getCell( 0, 0 );
                    String badTxt = badval == null
                                  ? "NULL"
                                  : ( "\"" + badval + "\"" );
                    String msg = new StringBuffer()
                       .append( "Column " )
                       .append( cname )
                       .append( " in " )
                       .append( tname )
                       .append( " is not consistently equal to \"" )
                       .append( schema )
                       .append( "\" (e.g. " )
                       .append( badTxt )
                       .append( ")" )
                       .toString();
                    runner.reporter_.report( FixedCode.E_PNST, msg );
                }
            }
        };
    }

    /**
     * Checks s_region content.  This is really a metadata check, to
     * see if the XType and datatype are consistent, but it's only
     * reasonable to raise an error if there are any non-NULL values
     * in the column, which often will not be the case.
     *
     * @return  new checker
     */
    private static ContentChecker sregionChecker() {
        return ( stdCol, runner ) -> {
            String cname = stdCol.name_;
            String tname = runner.tname_;
            String adql = new StringBuffer()
               .append( "SELECT TOP 1 " )
               .append( cname )
               .append( " FROM " )
               .append( tname )
               .append( " WHERE " )
               .append( cname )
               .append( " IS NOT NULL" )
               .toString();
            TableData tdata = runner.runQuery( adql );

            /* There are non-null values, so check the metadata. */
            if ( tdata != null && tdata.getRowCount() > 0 ) {
                ColumnInfo info = tdata.getTable().getColumnInfo( 0 );
                Class<?> clazz = info.getContentClass();
                String xtype = info.getXtype();
                String reqXtype = "adql:REGION";
                if ( String.class.equals( clazz ) ) {
                    if ( ! reqXtype.equals( xtype ) ) {
                        String msg = new StringBuffer()
                           .append( "Table " )
                           .append( tname )
                           .append( " column " )
                           .append( cname )
                           .append( " has wrong xtype for string datatype: " )
                           .append( xtype )
                           .append( " != " )
                           .append( reqXtype )
                           .toString();
                        runner.reporter_.report( FixedCode.E_SRXT, msg );
                    }
                }
                else if ( float[].class.equals( clazz ) ||
                          double[].class.equals( clazz ) ) {
                    Collection<String> arrayXtypes =
                            Arrays.asList( new String[] {
                        "point", "circle", "polygon",
                    } );
                    if ( ! arrayXtypes.contains( xtype ) ) {
                        String msg = new StringBuffer()
                           .append( "Table " ) 
                           .append( tname )
                           .append( " column " )
                           .append( cname )
                           .append( " has wrong datatype \"" )
                           .append( xtype )
                           .append( "\"; should be one of " )
                           .append( arrayXtypes )
                           .toString();
                        runner.reporter_.report( FixedCode.E_SRXT, msg );
                    }
                }
            }
        };
    }

    /**
     * Creates a check that a column contains a Julian Day value.
     * This is formally not restricted in value, but if a value corresponds
     * to some time in the distant past or far future there's a good chance
     * that it's been miscoded as an MJD or something, so issue a warning.
     *
     * @return  new checker
     */
    private static ContentChecker jdChecker() {
        return ( stdCol, runner ) -> {
            String cname = stdCol.name_;
            String tname = runner.tname_;
            String adql = new StringBuffer()
               .append( "SELECT TOP 1 " )
               .append( cname )
               .append( " FROM " )
               .append( tname )
               .append( " WHERE NOT " )
               .append( cname )
               .append( " BETWEEN " )
               .append( JD_PLAUSIBLE_LO )
               .append( " AND " )
               .append( JD_PLAUSIBLE_HI )
               .toString();
            TableData tdata = runner.runQuery( adql );
            if ( tdata != null && tdata.getRowCount() > 0 ) {
                Object badValue = tdata.getCell( 0, 0 );
                if ( badValue != null ) {
                    StringBuffer mbuf = new StringBuffer()
                       .append( "Value " )
                       .append( badValue )
                       .append( " in JD " )
                       .append( tname )
                       .append( " column " )
                       .append( cname )
                       .append( " is in distant past/future" );
                    double dval = badValue instanceof Number
                                ? ((Number) badValue).doubleValue()
                                : Double.NaN;
                    if ( ! Double.isInfinite( dval ) &&
                         ! Double.isNaN( dval ) ) {
                        mbuf.append( " (" )
                            .append( Times.mjdToIso( Times.jdToMjd( dval ) ) )
                            .append( ")" );
                    }
                    mbuf.append( "; are you sure it's in Julian Days?" )
                        .toString();
                    runner.reporter_.report( FixedCode.W_PNJD, mbuf.toString());
                }
            }
        };
    }

    /**
     * Column type enumeration.  Corresponds to the entries in the "Type"
     * column of Table 1.
     */
    private static enum Type {
        INTEGER() {
            void checkInfo( Reporter reporter, TableMeta tmeta,
                            ValueInfo info ) {
                Class<?> clazz = info.getContentClass();
                if ( clazz.equals( Byte.class ) ||
                     clazz.equals( Short.class ) ||
                     clazz.equals( Integer.class ) ||
                     clazz.equals( Long.class ) ) {
                    // OK
                }
                else {
                    reportTypeMismatch( reporter, tmeta, info,
                                        ObsLocStage.votype( info ) +
                                        " not integer" );
                }
            }
        },
        DOUBLE() {
            void checkInfo( Reporter reporter, TableMeta tmeta,
                            ValueInfo info ) {
                Class<?> clazz = info.getContentClass();
                if ( ! ( clazz.equals( Double.class ) ||
                         clazz.equals( Float.class ) ) ) {
                    reportTypeMismatch( reporter, tmeta, info,
                                        ObsLocStage.votype( info ) +
                                        " not floating point");
                }
            }
        },
        TEXT() {
            void checkInfo( Reporter reporter, TableMeta tmeta,
                            ValueInfo info ) {
                if ( ! String.class.equals( info.getContentClass() ) ) {
                    reportTypeMismatch( reporter, tmeta, info,
                                        ObsLocStage.votype( info ) +
                                        " not Text" );
                }
            }
        },
        TEXT_MOC() {
            void checkInfo( Reporter reporter, TableMeta tmeta,
                            ValueInfo info ) {
                if ( ! String.class.equals( info.getContentClass() ) ) {
                    reportTypeMismatch( reporter, tmeta, info,
                                        ObsLocStage.votype( info ) +
                                        " not Text" );
                }
                else {
                    checkXtype( reporter, tmeta, info, "MOC" );
                }
            }
        },
        TIMESTAMP() {
            void checkInfo( Reporter reporter, TableMeta tmeta,
                            ValueInfo info ) {
                if ( ! String.class.equals( info.getContentClass() ) ) {
                    reportTypeMismatch( reporter, tmeta, info,
                                        ObsLocStage.votype( info ) +
                                        " not String - not Timestamp?" );
                }
                else {
                    checkXtype( reporter, tmeta, info, "timestamp" );
                }
            }
        },
        SPOLY() {
            void checkInfo( Reporter reporter, TableMeta tmeta,
                            ValueInfo info ) {
                // metadata checking is done elsewhere (sregionChecker)
            }
        };

        /**
         * Checks whether a given column metadata item appears to
         * conform to this type, and reports accordingly.
         *
         * @param  reporter  reporter
         * @param  tmeta    metadata for epn_core table
         * @param  info    metadata for column to test
         */
        abstract void checkInfo( Reporter reporter, TableMeta tmeta,
                                 ValueInfo info );

        /**
         * Issues a report in case of a mismatch between declared and
         * submitted column metadata.
         *
         * @param  reporter  reporter
         * @param  tmeta    metadata for epn_core table
         * @param  info    metadata for column to test
         * @param  txt   user-directed detail message about mismatch
         */
        private static void reportTypeMismatch( Reporter reporter,
                                                TableMeta tmeta,
                                                ValueInfo info, String txt ) {
            String msg = new StringBuffer()
               .append( "Data type mismatch for " )
               .append( tmeta.getName() )
               .append( " column " )
               .append( info.getName() )
               .append( ": " )
               .append( txt )
               .toString();
            reporter.report( FixedCode.E_PNDE, msg );
        }

        /**
         * Checks that a retrieved column has a required xtype,
         * and reports if not.
         *
         * @param  reporter  reporter
         * @param  tmeta    metadata for epn_core table
         * @param  info    metadata for column to test
         * @param  reqValue  requird xtype
         */
        private static void checkXtype( Reporter reporter, TableMeta tmeta,
                                        ValueInfo info, String reqValue ) {
            String xtype = info.getXtype();
            if ( ! reqValue.equals( xtype ) ) {
                String msg = new StringBuffer()
                   .append( "Wrong xtype for " )
                   .append( tmeta.getName() )
                   .append( " column " )
                   .append( info.getName() )
                   .append( ": " )
                   .append( xtype )
                   .append( " != \"" )
                   .append( reqValue )
                   .append( "\"" )
                   .toString();
                reporter.report( FixedCode.E_PNXT, msg );
            }
        }
    }

    /**
     * Spatial frame type enumeration.
     * Corresponds to the permitted values of the spatial_frame_type column,
     * and includes information provided in Table 2.
     */
    enum FrameType {
        CELESTIAL( new String[] { "pos.eq.ra", "pos.eq.dec", "pos.distance" },
                   new String[] { "deg", "deg", "AU" },
                   new boolean[] { true, true, false } ),
        BODY( new String[][] { { "pos.bodyrc.lon" },
                               { "pos.bodyrc.lat" },
                               { "pos.bodyrc.alt", "pos.distance;pos.bodyrc" }},
              new String[] { "deg", "deg", "km" },
              new boolean[] { true, true, false } ),
        CARTESIAN( new String[] { "pos.cartesian.x",
                                  "pos.cartesian.y",
                                  "pos.cartesian.z" },
                   new String[] { "km", "km", "km" },
                   new boolean[] { false, false, false } ),
        SPHERICAL( new String[] { "pos.spherical.r",
                                  "pos.spherical.colat",
                                  "pos.spherical.azi" },
                   new String[] { "m", "deg", "deg" },
                   new boolean[] { false, true, true } ),
        CYLINDRICAL( new String[] { "pos.cylindrical.r",
                                    "pos.cylindrical.azi",
                                    "pos.cylindrical.z" },
                     new String[] { "km", "deg", "km" },
                     new boolean[] { false, true, false } );

        final String[][] ucds_;
        final String[] units_;
        final boolean[] isAngular_;

        /**
         * Constructor with multiple UCD options per axis.
         *
         * @param  ucds  3-element array by axis of arrays of permissible UCDs
         * @param  units 3-element array by axis of required units
         * @param  isAngular 3-element array by axis of angular/cartesian flags
         */
        FrameType( String[][] ucds, String[] units, boolean[] isAngular ) {
            ucds_ = ucds;
            units_ = units;
            isAngular_ = isAngular;
        }

        /**
         * Convenience constructor with single UCD option per axis.
         *
         * @param  ucds  3-element array by axis of required UCDs
         * @param  units 3-element array by axis of required units
         * @param  isAngular 3-element array by axis of angular/cartesian flags
         */
        FrameType( String[] ucds, String[] units, boolean[] isAngular ) {
            this( new String[][] { new String[] { ucds[ 0 ] },
                                   new String[] { ucds[ 1 ] },
                                   new String[] { ucds[ 2 ] } },
                  units, isAngular );
        }

        // Table 1 Note 2.
        /**
         * Returns the base UCD for the cN_resol_min/max column corresponding
         * to axis N.
         *
         * @param   idim0  zero-based axis index (0, 1, 2)
         * @return   base UCD (without ;stat.min/max)
         */
        String resolUcd( int idim0 ) {
            return isAngular_[ idim0 ] ? "pos.angResolution" : "pos.resolution";
        }
    }

    /**
     * Defines a check on the content of a given column.
     */
    @FunctionalInterface
    private interface ContentChecker {

        /**
         * Checks content corresponding to a given column.
         * This typically involves a TAP query with some tests made
         * on the result, and reports written as appropriate.
         *
         * @param  stdCol  column from the standard on behalf of which
         *                 this test is being executed;
         *                 this column is guaranteed to be present in
         *                 the runner's epnMeta_ table
         * @param  runner  object to use for executing queries and
         *                 reporting results
         */
        void checkContent( SingleCol stdCol, EpncoreRunner runner );
    }

    /**
     * Aggregates a pair of strings, relating to minimum and maximum values.
     */
    private static class String2 {
        final String min_;
        final String max_;

        /**
         * Constructor.
         *
         * @param  min  string relating to minimum
         * @param  max  string relating to maximum
         */
        String2( String min, String max ) {
            min_ = min;
            max_ = max;
        }
    }

    /**
     * Represents a column ("parameter") defined in the EPN-TAP standard.
     * This abstract superclass is instantiated differently according
     * to whether it represents a standalone column or a minimum/maximum pair.
     */
    static abstract class EpnCol {

        final String id_;
        final Type type_;
        final String unit_;
        ContentChecker checker_;

        /**
         * Constructor.
         *
         * @param  id   column identifier; base name of the column(s)
         * @param  type  column required data type
         * @param  unit  column required unit; null value means no restriction;
         *               empty string means test unit must be null/empty
         */
        EpnCol( String id, Type type, String unit ) {
            id_ = id;
            type_ = type;
            unit_ = unit;
        }
    }

    /**
     * Represents a single table column in an epn_core table.
     */
    static class SingleCol extends EpnCol {

        final String name_;
        final String ucd_;

        /**
         * Constructor.
         *
         * @param  name  column name
         * @param  type  column required data type
         * @param  unit  column required unit; null value means no restriction;
         *               empty string means test unit must be null/empty
         * @param  ucd   column required UCD; null value means no restriction;
         *               empty string means test UCD must be null/empty
         */
        SingleCol( String name, Type type, String unit, String ucd ) {
            super( name, type, unit );
            name_ = name;
            ucd_ = ucd;
        }
    }

    /**
     * Represents a matched pair of minimum/maximum columns in an
     * epn_core table.
     */
    private static class MinMaxCol extends EpnCol {

        final String2 names_;
        final String2 ucds_;
        boolean couldBeLongitude_;

        /**
         * Constructor.
         *
         * @param  baseName  column name without "min"/"max" suffix
         * @param  type  column required data type
         * @param  unit  column required unit;
         *               null value means no restriction,
         *               empty string means test unit must be null/empty
         * @param  ucds  pair of min/max required UCDs;
                         null value or null values mean no restriction
         */
        MinMaxCol( String baseName, Type type, String unit, String2 ucds ) {
            super( baseName, type, unit );
            names_ = new String2( baseName + "min", baseName + "max" );
            ucds_ = ucds == null ? new String2( null, null ) : ucds;
        }

        /**
         * Returns the minimum column associated with this pair.
         *
         * @return  minimum single column
         */
        SingleCol minCol() {
            SingleCol minCol =
                new SingleCol( names_.min_, type_, unit_, ucds_.min_ );
            minCol.checker_ = checker_;
            return minCol;
        }

        /**
         * Returns the maximum column associated with this pair.
         *
         * @return  maximum single column
         */
        SingleCol maxCol() {
            SingleCol maxCol =
                new SingleCol( names_.max_, type_, unit_, ucds_.max_ );
            maxCol.checker_ = checker_;
            return maxCol;
        }
    }

    /**
     * Writes a version of Table 1 to standard output.
     */
    public static void main( String[] args ) {
        System.out.println( "# name type unit ucd" );
        UnaryOperator<String> fmter =
            s -> s == null || s.length() == 0 ? "''" : s;
        for ( SingleCol col : toSingleCols( getAllColumns() ) ) {
            System.out.println( col.name_ + " "
                              + col.type_.toString() + " "
                              + fmter.apply( col.unit_ ) + " "
                              + fmter.apply( col.ucd_ ) );
        }
    }
}
