package uk.ac.starlink.ttools.taplint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.vo.ColumnMeta;
import uk.ac.starlink.vo.Ivoid;
import uk.ac.starlink.vo.SchemaMeta;
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TapCapability;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.vo.TapService;

/**
 * Validation stage for testing ObsCore data model metadata and content.
 *
 * @author   Mark Taylor
 * @since    24 Nov 2011
 * @see <a href="http://www.ivoa.net/Documents/ObsCore/index.html"
 *         >IVOA Observation Data Model</a>
 */
public class ObsTapStage implements Stage {

    private final TapRunner tapRunner_;
    private final CapabilityHolder capHolder_;
    private final MetadataHolder metaHolder_;

    private static final Ivoid OBSCORE10_ID_WRONG =
        new Ivoid( "ivo://ivoa.net/std/ObsCore-1.0" );
    private static final Ivoid OBSCORE11_ID_WRONG =
        new Ivoid( "ivo://ivoa.net/std/ObsCore/v1.1" );
    private static final String OBSCORE_TNAME = "ivoa.ObsCore";

    /**
     * Constructor.
     *
     * @param   tapRunner  runs TAP queries
     * @param   capHolder  provides capability metadata at runtime
     * @param   metaHolder provides table metadata at runtime
     */
    public ObsTapStage( TapRunner tapRunner, CapabilityHolder capHolder,
                        MetadataHolder metaHolder ) {
        tapRunner_ = tapRunner;
        capHolder_ = capHolder;
        metaHolder_ = metaHolder;
    }

    public String getDescription() {
        return "Test implementation of ObsCore Data Model";
    }

    public void run( Reporter reporter, TapService tapService ) {

        /* Check prerequisites. */
        boolean obsDeclared;
        TapCapability tcap = capHolder_.getCapability();
        final ObscoreVersion obscoreVersion;
        if ( tcap != null ) {
            obscoreVersion = getObscoreDm( reporter, tcap );
            if ( obscoreVersion == null ) {
                reporter.report( FixedCode.I_NODM,
                                 "Table capabilities lists no ObsCore DataModel"
                               + " - no ObsCore tests" );
                return;
            }
        }
        else {
            obscoreVersion = null;
        }
        SchemaMeta[] smetas = metaHolder_.getTableMetadata();
        if ( smetas == null ) {
            reporter.report( FixedCode.F_NOTM,
                             "No table metadata"
                           + " (earlier stages failed/skipped?)" );
            return;
        }

        /* Get ObsCore table if present. */
        TableMeta obsMeta = null;
        for ( SchemaMeta smeta : smetas ) {
            for ( TableMeta tmeta : smeta.getTables() ) {
                if ( OBSCORE_TNAME.equalsIgnoreCase( tmeta.getName() ) ) {
                    obsMeta = tmeta;
                }
            }
        }
        if ( obsMeta == null ) {
            String missingMsg = "No table with name " + OBSCORE_TNAME;
            if ( obscoreVersion != null ) {
                reporter.report( FixedCode.F_NOTB, missingMsg );
            }
            else {
                reporter.report( FixedCode.I_OCCP,
                                 missingMsg 
                               + "; probably just means no ObsCore intended"
                               + " but can't tell for sure"
                               + " because no capabilities present"
                               + " (earlier stages failed/skipped?)" );
            }
            return;
        }

        /* Determine effective DM version. */
        final boolean is11;
        if ( obscoreVersion != null ) {
            is11 = obscoreVersion.is11_;
        }
        else {
            String msg = new StringBuffer()
               .append( OBSCORE_TNAME )
               .append( " table present but no ObsCore DM" )
               .append( " declaration available" )
               .append( "; assume ObsCore 1.0" )
               .toString();
            reporter.report( FixedCode.W_DMDC, msg );
            is11 = false;
        }
        reporter.report( FixedCode.I_DMID,
                         "Checking against ObsCore DM "
                       + ( is11 ? "1.1" : "1.0" ) );

        /* Run tests. */
        new ObsTapRunner( reporter, tapService, obsMeta, is11, tapRunner_ )
           .run();
    }

    /**
     * Determines whether a table capability reports conformance to the
     * ObsCore data model.  If not, an appropriate report is made.
     *
     * @param  reporter   reporter
     * @param  tcap    tap capability object
     * @return   version of ObsCore model indicated, or null if no ObsCore
     */
    private ObscoreVersion getObscoreDm( Reporter reporter,
                                         TapCapability tcap ) {
        List<Ivoid> dmList = tcap.getDataModels() == null
                           ? Collections.emptyList()
                           : Arrays.asList( tcap.getDataModels() );

        /* Match for known data model declarations corresponding to ObsCore. */
        boolean has10 = dmList.contains( ObscoreVersion.V10.ivoid_ );
        boolean has11 = dmList.contains( ObscoreVersion.V11.ivoid_ );
        boolean has10wrong = dmList.contains( OBSCORE10_ID_WRONG );
        boolean has11wrong = dmList.contains( OBSCORE11_ID_WRONG );

        /* Check for presence of one of the known ObsCore data models,
         * and return values accordingly. */
        if ( has11 ) {
            if ( has10 ) {
                String msg = new StringBuffer()
                   .append( "Declared both v1.0 and v1.1 ObsCore DMs (" )
                   .append( ObscoreVersion.V10.ivoid_ )
                   .append( " and " )
                   .append( ObscoreVersion.V11.ivoid_ )
                   .append( "); can't simultaneously satisfy both" )
                   .toString();
                reporter.report( FixedCode.W_DMSS, msg );
            }
            return ObscoreVersion.V11;
        }
        else if ( has10 ) {
            return ObscoreVersion.V10;
        }

        /* Failing that, check for OBSCORE10_ID_WRONG, which is a string
         * erroneously used in examples in TAPRegExt 1.0.  This confusion
         * reported on {dal,dm}@ivoa.net mailing lists on 4 Dec 2013. */
        else if ( has10wrong ) {
            String msg = new StringBuffer()
               .append( "Wrong ObsCore identifier " )
               .append( OBSCORE10_ID_WRONG )
               .append( " reported, should be " )
               .append( ObscoreVersion.V10.ivoid_ )
               .append( " (known error in TAPRegExt 1.0 document)" )
               .toString();
            reporter.report( FixedCode.W_WODM, msg );
            return ObscoreVersion.V10;
        }

        /* Or for incorrect v1.1 id, present in PR-ObsCore-20160330. */
        else if ( has11wrong ) {
            String msg = new StringBuffer()
               .append( "Wrong ObsCore identifier " )
               .append( OBSCORE11_ID_WRONG )
               .append( " reported, should be " )
               .append( ObscoreVersion.V11.ivoid_ )
               .append( " (corrected from PR-ObsCore-20160330)" )
               .toString();
            reporter.report( FixedCode.W_WODM, msg );
            return ObscoreVersion.V11;
        }

        /* Failing that, if it says ObsCore, that's probably what it means. */
        else {
            for ( Ivoid dm : dmList ) {
                if ( dm.toString().toLowerCase().indexOf( "obscore" ) >= 0 ) {
                    String msg = new StringBuffer()
                       .append( "Mis-spelt ObsCore identifier? " )
                       .append( dm )
                       .append( " reported, should be " )
                       .append( ObscoreVersion.V10.ivoid_ )
                       .append( " or " )
                       .append( ObscoreVersion.V11.ivoid_ )
                       .append( "; assuming ObsCore 1.0" )
                       .toString();
                    reporter.report( FixedCode.W_IODM, msg );
                    return ObscoreVersion.V10;
                }
            }
        }

        /* Otherwise: no ObsCore data model declared. */
        return null;
    }

    /**
     * Does the work for running tests on ObsCore table.
     */
    private static class ObsTapRunner implements Runnable {
        private final Reporter reporter_;
        private final TapService tapService_;
        private final TapRunner tRunner_;
        private final Map<String,ColumnMeta> gotColMap_;
        private final Map<String,ObsCol> reqColMap_;
        private final Map<String,ObsCol> optColMap_;

        /**
         * Constructor.
         *
         * @param  reporter  validation message destination
         * @param  tapService  TAP service description
         * @param  obsMeta   table metadata for ivoa.ObsCore table
         * @param  is11   true for ObsCore-1.1, false for ObsCore-1.0
         * @param  tapRunner  runs TAP queries
         */
        ObsTapRunner( Reporter reporter, TapService tapService,
                      TableMeta obsMeta, boolean is11, TapRunner tapRunner ) {
            reporter_ = reporter;
            tapService_ = tapService;
            gotColMap_ = toMap( obsMeta.getColumns() );
            tRunner_ = tapRunner;
            reqColMap_ = createMandatoryColumns( is11 );
            optColMap_ = createOptionalColumns( is11 );
        }

        /**
         * Runs the test.
         */
        public void run() {

            /* Check column metadata for required columns. */
            int nreq = 0;
            for ( String reqName : reqColMap_.keySet() ) {
                ObsCol reqCol = reqColMap_.get( reqName );
                ColumnMeta gotCol = gotColMap_.get( reqName );
                if ( gotCol != null ) {
                    checkMetadata( gotCol, reqCol );
                    nreq++;
                }
                else {
                    String msg = new StringBuffer()
                       .append( "Required ObsCore column " )
                       .append( reqName )
                       .append( " is missing" )
                       .toString();
                    reporter_.report( FixedCode.E_OCOL, msg );
                }
            }

            /* Check column metadata for optional columns. */
            int nopt = 0;
            for ( String optName : optColMap_.keySet() ) {
                ObsCol optCol = optColMap_.get( optName );
                ColumnMeta gotCol = gotColMap_.get( optName );
                if ( gotCol != null ) {
                    checkMetadata( gotCol, optCol );
                    nopt++;
                }
            }

            /* Check column content. */
            for ( String cname : gotColMap_.keySet() ) {
                ObsCol stdCol = null;
                if ( stdCol == null ) {
                    stdCol = reqColMap_.get( cname );
                }
                if ( stdCol == null ) {
                    stdCol = optColMap_.get( cname );
                }
                if ( stdCol != null ) {
                    checkContent( gotColMap_.get( cname ), stdCol );
                }
            }

            /* Summarise columns in each category. */
            int nother = gotColMap_.size() - nreq - nopt;
            String msg = new StringBuffer()
               .append( "ivoa.ObsCore columns: " )
               .append( nreq )
               .append( "/" )
               .append( reqColMap_.size() )
               .append( " required, " )
               .append( nopt )
               .append( "/" )
               .append( optColMap_.size() )
               .append( " optional, " )
               .append( nother )
               .append( " custom" )
               .toString();
            reporter_.report( FixedCode.S_COLS, msg );
            tRunner_.reportSummary( reporter_ );
        }

        /**
         * Checks that a given column has the correct metadata.
         *
         * @param  gotCol  metadata actually present in a column
         * @param  stdCol  correct metadata for a column
         */
        private void checkMetadata( ColumnMeta gotCol, ObsCol stdCol ) {
            String cname = gotCol.getName();
            compareItem( cname, "Utype", FixedCode.E_CUTP,
                         stdCol.utype_, gotCol.getUtype(), false, null );
            compareItem( cname, "UCD", FixedCode.E_CUCD,
                         stdCol.ucd_, gotCol.getUcd(), false, stdCol.ucdOnce_ );
            compareItem( cname, "Unit", FixedCode.E_CUNI,
                         stdCol.unit_, gotCol.getUnit(), true, null );
            checkType( gotCol, stdCol );
        }

        /**
         * Checks that a given column has the correct data type.
         *
         * @param  gotCol  metadata actually present in a column
         * @param  stdCol  correct metadata for a column
         */
        private void checkType( ColumnMeta gotCol, ObsCol stdCol ) {
            String cname = gotCol.getName();
            String gotType = gotCol.getDataType();
            Type stdType = stdCol.type_;
            if ( stdType.isEqual( gotType ) ) {
                // no comment
            }
            else if ( stdType.isCompatible( gotType ) ) {
                String msg = new StringBuffer()
                   .append( "Imperfect datatype match for ObsCore column " )
                   .append( cname )
                   .append( ": " )
                   .append( gotType )
                   .append( " != " )
                   .append( stdType )
                   .toString();
                reporter_.report( FixedCode.W_TYPI, msg );
            }
            else {
                String msg = new StringBuffer()
                   .append( "Wrong datatype for ObsCore column " )
                   .append( cname )
                   .append( ": " )
                   .append( gotType )
                   .append( " != " )
                   .append( stdType )
                   .toString();
                reporter_.report( FixedCode.E_TYPX, msg );
            }
        }

        /**
         * Runs checks on column content as appropriate.
         *
         * @param  gotCol  metadata actually present in a column
         * @param  stdCol  correct metadata for a column
         */
        private void checkContent( ColumnMeta gotCol, ObsCol stdCol ) {
            String cname = gotCol.getName();
            if ( stdCol.nullForbidden_ ) {
                checkNoNulls( cname );
            }
            if ( stdCol.range_ != null ) {
                checkRange( cname, stdCol.range_ );
            }
            if ( stdCol.hardOptions_ != null ) {
                checkStringOptions( cname, stdCol.hardOptions_, 
                                    ! stdCol.nullForbidden_, true );
            }
            else if ( stdCol.softOptions_ != null ) {
                checkStringOptions( cname, stdCol.softOptions_,
                                    ! stdCol.nullForbidden_, false );
            }
        }

        /**
         * Checks that a given ObsCore column contains no NULL values.
         *
         * @param  cname  column name
         */
        private void checkNoNulls( String cname ) {
            String adql = new StringBuffer()
               .append( "SELECT TOP 1 " )
               .append( cname )
               .append( " FROM " )
               .append( OBSCORE_TNAME )
               .append( " WHERE " )
               .append( cname )
               .append( " IS NULL" )
               .toString();
            TableData result = runQuery( adql );
            if ( result != null && result.getRowCount() > 0 ) {
                String msg = new StringBuffer()
                   .append( "Illegal NULL(s) in ObsCore column " )
                   .append( cname )
                   .toString();
                reporter_.report( FixedCode.E_HNUL, msg );
            }
        }

        /**
         * Checks that all the values in a given ObsCore column are within
         * a given numerical range.
         *
         * @param   cname  column name
         * @param   [low,high] inclusive range permitted
         */
        private void checkRange( String cname, Comparable<?>[] range ) {
            String adql = new StringBuffer()
               .append( "SELECT TOP 1 " )
               .append( cname )
               .append( " FROM " )
               .append( OBSCORE_TNAME )
               .append( " WHERE " )
               .append( cname )
               .append( " NOT BETWEEN " )
               .append( range[ 0 ] )
               .append( " AND " )
               .append( range[ 1 ] )
               .toString();
            TableData result = runQuery( adql );
            if ( result != null && result.getRowCount() > 0 ) {
                String msg = new StringBuffer()
                   .append( "Value(s) out of range in ObsCore column " )
                   .append( cname )
                   .append( ": " )
                   .append( result.getCell( 0, 0 ) )
                   .append( " not in [" )
                   .append( range[ 0 ] )
                   .append( "," )
                   .append( range[ 1 ] )
                   .append( "]" )
                   .toString();
                reporter_.report( FixedCode.E_RANG, msg );
            }
        }

        /**
         * Checks that all the values in a given ObsCore column have one
         * of a fixed set of string values.  NULLs are not reported on.
         *
         * @param   cname  column name
         * @param  opts   permitted values
         * @param   nullPermitted  true iff NULL is permissible
         * @param  hard  true for required lists, false for suggested
         */
        private void checkStringOptions( String cname, String[] opts,
                                         boolean nullPermitted, boolean hard ) {
            int maxWrong = 4;
            StringBuffer abuf = new StringBuffer()
               .append( "SELECT " )
               .append( "DISTINCT TOP " )
               .append( maxWrong )
               .append( " " )
               .append( cname )
               .append( " FROM " )
               .append( OBSCORE_TNAME )
               .append( " WHERE " )
               .append( cname )
               .append( " NOT IN (" );
            for ( int io = 0; io < opts.length; io++ ) {
                if ( io > 0 ) {
                    abuf.append( ", " );
                }
                abuf.append( "'" )
                    .append( opts[ io ] )
                    .append( "'" );
            }
            abuf.append( ")" );
            if ( nullPermitted ) {
                abuf.append( " AND " )
                    .append( cname )
                    .append( " IS NOT NULL" );
            }
            TableData result = runQuery( abuf.toString() );
            if ( result == null ) {
                return;
            }
            long nwrong = result.getRowCount();
            if ( nwrong > 0 ) {
                StringBuffer mbuf = new StringBuffer()
                   .append( hard ? "Illegal" : "Non-standard" )
                   .append( " " )
                   .append( nwrong == 1 ? "value" : "values" )
                   .append( " in column " )
                   .append( cname )
                   .append( ": " );
                for ( int irow = 0; irow < nwrong; irow++ ) {
                    if ( irow > 0 ) {
                        mbuf.append( ", " );
                    }
                    mbuf.append( '"' )
                        .append( result.getCell( irow, 0 ) )
                        .append( '"' );
                }
                if ( nwrong >= maxWrong ) {
                    mbuf.append( ", ..." );
                }
                mbuf.append( "; " )
                    .append( hard ? "legal" : "standard" )
                    .append( " values are: " );
                for ( int io = 0; io < opts.length; io++ ) {
                    if ( io > 0 ) {
                        mbuf.append( ", " );
                    }
                    mbuf.append( '"' )
                        .append( opts[ io ] )
                        .append( '"' );
                }
                reporter_.report( hard ? FixedCode.E_ILOP : FixedCode.W_NSOP,
                                  mbuf.toString() );
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
         * @param  obsValueOnce   if not null, indicates a value that used
         *                        to be OK, but has been changed by an
         *                        ObsCore Erratum
         */
        private void compareItem( String colName, String itemName,
                                  ReportCode code,
                                  String obsValue, String gotValue,
                                  boolean isCaseSensitive,
                                  String obsValueOnce ) {
            String vGot = gotValue == null ? "null" : gotValue;
            String vObs = obsValue == null ? "null" : obsValue;
            if ( isCaseSensitive ? ( ! vGot.equals( vObs ) )
                                 : ( ! vGot.equalsIgnoreCase( vObs ) ) ) {
                StringBuffer sbuf = new StringBuffer()
                    .append( "Wrong " )
                    .append( itemName )
                    .append( " in ObsCore column " )
                    .append( colName )
                    .append( ": " )
                    .append( gotValue )
                    .append( " != " )
                    .append( obsValue );
                if ( obsValueOnce != null &&
                     isCaseSensitive ? vGot.equals( obsValueOnce)
                                     : vGot.equalsIgnoreCase( obsValueOnce ) ) {
                    sbuf.append( " (used to be correct," )
                        .append( " but changed by ObsCore Erratum)" );
                }
                reporter_.report( code, sbuf.toString() );
            }
        }

        /**
         * Executes an ADQL query and returns the result as a TableData object.
         * If there is some error, it is reported through the reporting
         * system, and null is returned.
         *
         * @param   adql  query string
         * @return   table result, or null
         */
        private TableData runQuery( String adql ) {
            TapQuery tq = new TapQuery( tapService_, adql, null );
            StarTable table = tRunner_.getResultTable( reporter_, tq );
            return TableData.createTableData( reporter_, table );
        }
    }

    /**
     * Creates a description of the standard required ObsCore columns.
     *
     * @param   is11  true for ObsCore-1.1, false for ObsCore-1.0
     * @return  new name-&gt;metadata map
     */
    static Map<String,ObsCol> createMandatoryColumns( boolean is11 ) {
        List<ObsCol> list = new ArrayList<ObsCol>();
        list.addAll( Arrays.asList( new ObsCol[] {
            new ObsCol( "dataproduct_type", Type.VARCHAR,
                        is11 ? "ObsDataset.dataProductType"
                             : "Obs.dataProductType",
                        "meta.code.class" ),
            new ObsCol( "calib_level", Type.INTEGER,
                        is11 ? "ObsDataset.calibLevel"
                             : "Obs.calibLevel",
                        "meta.code;obs.calib" ),
            new ObsCol( "obs_collection", Type.VARCHAR,
                        "DataID.Collection", "meta.id" ),
            new ObsCol( "obs_id", Type.VARCHAR,
                        "DataID.observationID", "meta.id" ),
            new ObsCol( "obs_publisher_did", Type.VARCHAR,
                        "Curation.PublisherDID", "meta.ref.ivoid" ),
            new ObsCol( "access_url", Type.CLOB,
                        "Access.Reference", "meta.ref.url" ),
            new ObsCol( "access_format", Type.VARCHAR,
                        "Access.Format", "meta.code.mime" ),
            new ObsCol( "access_estsize", Type.BIGINT,
                        "Access.Size", "phys.size;meta.file", "kbyte" ),
            new ObsCol( "target_name", Type.VARCHAR,
                        "Target.Name", "meta.id;src" ),
            new ObsCol( "s_ra", Type.DOUBLE,
                        "Char.SpatialAxis.Coverage.Location"
                        + ".Coord.Position2D.Value2.C1", "pos.eq.ra", "deg" ),
            new ObsCol( "s_dec", Type.DOUBLE,
                        "Char.SpatialAxis.Coverage.Location"
                        + ".Coord.Position2D.Value2.C2", "pos.eq.dec", "deg" ),
            new ObsCol( "s_fov", Type.DOUBLE,
                        "Char.SpatialAxis.Coverage.Bounds.Extent.diameter",
                        "phys.angSize;instr.fov", "deg" ),
            new ObsCol( "s_region", Type.REGION,
                        "Char.SpatialAxis.Coverage.Support.Area",
                        is11 ? "pos.outline;obs.field"
                             : "phys.angArea;obs",
                        null ), // from ObsTAP 1.0 Table 6 but not Table 1
            new ObsCol( "s_resolution", Type.DOUBLE,
                        is11 ? "Char.SpatialAxis.Resolution.Refval.value"
                             : "Char.SpatialAxis.Resolution.refval",
                        "pos.angResolution", "arcsec" ),
            new ObsCol( "t_min", Type.DOUBLE,
                        is11 ? "Char.TimeAxis.Coverage.Bounds.Limits"
                                                   + ".StartTime"
                             : "Char.TimeAxis.Coverage.Bounds.Limits"
                                                   + ".Interval.StartTime",
                        "time.start;obs.exposure", "d" ),
            new ObsCol( "t_max", Type.DOUBLE,
                        is11 ? "Char.TimeAxis.Coverage.Bounds.Limits"
                                                   + ".StopTime"
                             : "Char.TimeAxis.Coverage.Bounds.Limits"
                                                   + ".Interval.StopTime",
                        "time.end;obs.exposure", "d" ),
            new ObsCol( "t_exptime", Type.DOUBLE,
                        "Char.TimeAxis.Coverage.Support.Extent",
                        "time.duration;obs.exposure", "s" ),
            new ObsCol( "t_resolution", Type.DOUBLE,
                        is11 ? "Char.TimeAxis.Resolution.Refval.value"
                             : "Char.TimeAxis.Resolution.refval",
                        "time.resolution", "s" ),
            new ObsCol( "em_min", Type.DOUBLE,
                        is11 ? "Char.SpectralAxis.Coverage.Bounds.Limits"
                                                       + ".LoLimit"
                             : "Char.SpectralAxis.Coverage.Bounds.Limits"
                                                       + ".Interval.LoLim",
                        "em.wl;stat.min", "m" ),
            new ObsCol( "em_max", Type.DOUBLE,
                        is11 ? "Char.SpectralAxis.Coverage.Bounds.Limits"
                                                       + ".HiLimit"
                             : "Char.SpectralAxis.Coverage.Bounds.Limits"
                                                       + ".Interval.HiLim",
                        "em.wl;stat.max", "m" ),
            new ObsCol( "em_res_power", Type.DOUBLE,
                        "Char.SpectralAxis.Resolution.ResolPower.refVal",
                        "spect.resolution" ),
            new ObsCol( "o_ucd", Type.VARCHAR,
                        "Char.ObservableAxis.ucd", "meta.ucd" ),

            // Note ObsCore 1.1 inconsistent on mandatoryness of pol_states.
            // Clarified in ObsCore-1.1 Erratum #2: it is mandatory.
            new ObsCol( "pol_states", Type.VARCHAR,
                        "Char.PolarizationAxis.stateList",
                        "meta.code;phys.polarization" ),
            new ObsCol( "facility_name", Type.VARCHAR,
                        "Provenance.ObsConfig.facility.name",
                        "meta.id;instr.tel" ),
            new ObsCol( "instrument_name", Type.VARCHAR,
                        "Provenance.ObsConfig.instrument.name",
                        "meta.id;instr" ),
        } ) );
        assert list.size() == 25;

        /* Add columns introduced in ObsCore 1.1. */
        if ( is11 ) {
            list.addAll( Arrays.asList( new ObsCol[] {
                new ObsCol( "s_xel1", Type.BIGINT,
                            "Char.SpatialAxis.numBins1", "meta.number" ),
                new ObsCol( "s_xel2", Type.BIGINT,
                            "Char.SpatialAxis.numBins2", "meta.number" ),
                new ObsCol( "t_xel", Type.BIGINT,
                            "Char.TimeAxis.numBins", "meta.number" ),
                new ObsCol( "em_xel", Type.BIGINT,
                            "Char.SpectralAxis.numBins", "meta.number" ),
                new ObsCol( "pol_xel", Type.BIGINT,
                            "Char.PolarizationAxis.numBins", "meta.number" ),
            } ) );
            assert list.size() == 30;
        }

        Map<String,ObsCol> map = toMap( list );

        /* Note some additional constraints. */

        /* ObsTAP 1.0 Sec 4.1, ObsTAP 1.1 Sec 3.3.1. */
        List<String> dpopts =
                new ArrayList<String>( Arrays.asList( new String[] {
            "image", "cube", "spectrum", "sed", "timeseries", "visibility",
            "event",
        } ) );
        if ( is11 ) {
            dpopts.add( "measurements" );
        }
        map.get( "dataproduct_type" ).hardOptions_ =
            dpopts.toArray( new String[ 0 ] );

        /* ObsTAP 1.0 Sec 4.2, ObsTAP 1.1 sec 3.3.2. */
        map.get( "calib_level" ).range_ =
            new Integer[] { Integer.valueOf( 0 ),
                            Integer.valueOf( is11 ? 4 : 3 ) };

        /* ObsTAP 1.0 Table 4. */
        map.get( "calib_level" ).nullForbidden_ = true;
        map.get( "obs_collection" ).nullForbidden_ = true;
        map.get( "obs_id" ).nullForbidden_ = true;
        map.get( "obs_publisher_did" ).nullForbidden_ = true;

        /* ObsCore 1.1 Erratum #1. */
        map.get( "obs_publisher_did" ).ucdOnce_ = "meta.ref.uri;meta.curation";
        map.get( "dataproduct_type" ).ucdOnce_ = "meta.id";

        return map;
    }

    /**
     * Creates a description of the standard optional ObsCore columns.
     *
     * @param   is11  true for ObsCore-1.1, false for ObsCore-1.0
     * @return  new name-&gt;metadata map
     */
    static Map<String,ObsCol> createOptionalColumns( boolean is11 ) {
        List<ObsCol> list = new ArrayList<ObsCol>();
        list.addAll( Arrays.asList( new ObsCol[] {
            new ObsCol( "dataproduct_subtype", Type.VARCHAR,
                        is11 ? "ObsDataset.dataProductSubtype"
                             : "Obs.dataProductSubtype", "meta.code.class" ),
            new ObsCol( "target_class", Type.VARCHAR,
                        "Target.Class", "src.class" ),
            new ObsCol( "obs_creation_date", Type.TIMESTAMP,
                        "DataID.Date", "time;meta.dataset" ),
            new ObsCol( "obs_creator_name", Type.VARCHAR,
                        "DataID.Creator", "meta.id" ),
            new ObsCol( "obs_creator_did", Type.VARCHAR,
                        "DataID.CreatorDID", "meta.id" ),
            new ObsCol( "obs_title", Type.VARCHAR,
                        "DataID.Title", "meta.title;obs" ),
            new ObsCol( "publisher_id", Type.VARCHAR,
                        "Curation.PublisherID", "meta.ref.ivoid" ),
            new ObsCol( "bib_reference", Type.VARCHAR,
                        "Curation.Reference", "meta.bib.bibcode" ),
            new ObsCol( "data_rights", Type.VARCHAR,
                        "Curation.Rights", "meta.code" ),
            new ObsCol( "obs_release_date", Type.TIMESTAMP,
                        "Curation.releaseDate", "time.release" ),
            new ObsCol( "s_ucd", Type.VARCHAR,
                        "Char.SpatialAxis.ucd", "meta.ucd" ),
            new ObsCol( "s_unit", Type.VARCHAR,
                        "Char.SpatialAxis.unit", "meta.unit" ),
            new ObsCol( "s_resolution_min", Type.DOUBLE,
                        is11 ? "Char.SpatialAxis.Resolution.Bounds.Limits"
                                                        + ".LoLimit"
                             : "Char.SpatialAxis.Resolution.Bounds.Limits"
                                                        + ".Interval.LoLim",
                        "pos.angResolution;stat.min", "arcsec" ),
            new ObsCol( "s_resolution_max", Type.DOUBLE,
                        is11 ? "Char.SpatialAxis.Resolution.Bounds.Limits"
                                                        + ".HiLimit"
                             : "Char.SpatialAxis.Resolution.Bounds.Limits"
                                                        + ".Interval.HiLim",
                        "pos.angResolution;stat.max", "arcsec" ),
            new ObsCol( "s_calib_status", Type.VARCHAR,
                        is11 ? "Char.SpatialAxis.calibrationStatus"
                             : "Char.SpatialAxis.calibStatus",
                        "meta.code.qual" ),
            new ObsCol( "s_stat_error", Type.DOUBLE,
                        "Char.SpatialAxis.Accuracy.statError.refval.value",
                        "stat.error;pos.eq", "arcsec" ),
            new ObsCol( "t_calib_status", Type.VARCHAR,
                        is11 ? "Char.TimeAxis.calibrationStatus"
                             : "Char.TimeAxis.calibStatus",
                        "meta.code.qual" ),
            new ObsCol( "t_stat_error", Type.DOUBLE,
                        "Char.TimeAxis.Accuracy.StatError.refval.value",
                        "stat.error;time", "s" ),
            new ObsCol( "em_ucd", Type.VARCHAR,
                        "Char.SpectralAxis.ucd", "meta.ucd" ),
            new ObsCol( "em_unit", Type.VARCHAR,
                        "Char.SpectralAxis.unit", "meta.unit" ),
            new ObsCol( "em_calib_status", Type.VARCHAR,
                        is11 ? "Char.SpectralAxis.calibrationStatus"
                             : "Char.SpectralAxis.calibStatus",
                        "meta.code.qual" ),
            new ObsCol( "em_res_power_min", Type.DOUBLE,
                        is11 ? "Char.SpectralAxis.Resolution.ResolPower.LoLimit"
                             : "Char.SpectralAxis.Resolution.ResolPower.LoLim",
                        "spect.resolution;stat.min" ),
            new ObsCol( "em_res_power_max", Type.DOUBLE,
                        is11 ? "Char.SpectralAxis.Resolution.ResolPower.HiLimit"
                             : "Char.SpectralAxis.Resolution.ResolPower.HiLim",
                        "spect.resolution;stat.max" ),
            new ObsCol( "em_resolution", Type.DOUBLE,
                        "Char.SpectralAxis.Resolution.refval.value",
                        "spect.resolution;stat.mean", "m" ),
            new ObsCol( "em_stat_error", Type.DOUBLE,
                        "Char.SpectralAxis.Accuracy.StatError.refval.value",
                        "stat.error;em", "m" ),
            new ObsCol( "o_unit", Type.VARCHAR,
                        "Char.ObservableAxis.unit", "meta.unit" ),
            new ObsCol( "o_calib_status", Type.VARCHAR,
                        is11 ? "Char.ObservableAxis.calibrationStatus"
                             : "Char.ObservableAxis.calibStatus",
                        "meta.code.qual" ),
            new ObsCol( "o_stat_error", Type.DOUBLE,
                        "Char.ObservableAxis.Accuracy.StatError.refval.value",
                        "stat.error" ),
            new ObsCol( "proposal_id", Type.VARCHAR,
                        "Provenance.Proposal.identifier",
                        "meta.id;obs.proposal" ),
        } ) );
        assert list.size() == 29;

        /* Add columns introduced in ObsCore 1.1. */
        if ( is11 ) {
            list.addAll( Arrays.asList( new ObsCol[] {
                new ObsCol( "s_pixel_scale", Type.DOUBLE,
                            "Char.SpatialAxis.Sampling.RefVal.SamplingPeriod",
                            "phys.angSize;instr.pixel", "arcsec" ),
            } ) );
            assert list.size() == 30;
        }

        Map<String,ObsCol> map = toMap( list );

        /* Note some additional constraints. */

        /* ObsTAP B.6.1.4. */
        map.get( "s_calib_status" ).softOptions_ = new String[] {
            "uncalibrated", "raw", "calibrated",
        };

        /* ObsTAP B.6.2.1. */
        map.get( "em_calib_status" ).softOptions_ = new String[] {
            "calibrated", "uncalibrated", "relative", "absolute",
        };

        /* ObsTAP B.6.3.3. */
        map.get( "t_calib_status" ).softOptions_ = new String[] {
            "calibrated", "uncalibrated", "relative", "raw",
        };

        /* ObsTAP B.6.5.2. */
        map.get( "o_calib_status" ).softOptions_ = new String[] {
            "absolute", "relative", "normalized", "any",
        };

        /* ObsCore 1.1 Erratum #1. */
        map.get( "publisher_id" ).ucdOnce_ = "meta.ref.uri;meta.curation";
        map.get( "o_stat_error" ).ucdOnce_ = "stat.error;phot.flux";
        map.get( "dataproduct_subtype" ).ucdOnce_ = "meta.id";

        return map;
    }

    /**
     * Enumeration of known data types for standard columns.
     */
    private enum Type {
        INTEGER( new String[] { "SMALLINT", "BIGINT" },
                 new String[] { "short", "int", "long" } ),
        BIGINT( new String[] { "SMALLINT", "INTEGER" },
                new String[] { "short", "int", "long" } ),
        DOUBLE( new String[] { "REAL" },
                new String[] { "float", "double" } ),
        VARCHAR( new String[] { "CHAR" },
                 new String[] { "char", "unicodeChar" } ),
        TIMESTAMP( new String[] {},
                   new String[] { "char", "unicodeChar" } ),
        REGION( new String[] {},
                new String[] { "char", "unicodeChar" } ),
        CLOB( new String[] { "VARCHAR", "CHAR", },
              new String[] { "char", "unicodeChar" } );

        private final Set<String> adqlTypeSet_;
        private final Set<String> votableTypeSet_;

        /**
         * Constructor.
         *
         * @param  adqlTypes  base type names for ADQL types which roughly
         *         correspond to this type
         * @param  votableTypes  VOTable type names which roughly
         *         correspond to this type
         */
        Type( String[] adqlTypes, String[] votableTypes ) {
            adqlTypeSet_ = new HashSet<String>( Arrays.asList( adqlTypes ) );
            votableTypeSet_ =
                new HashSet<String>( Arrays.asList( votableTypes ) );
        }

        /**
         * Indicates whether a given data type is, or at least may be,
         * exactly equal to this one.
         *
         * @param   dtype  type string for comparison
         * @return  true iff dtype may be an exact match
         */
        boolean isEqual( String dtype ) {
            String baseDtype = CompareMetadataStage.stripAdqlType( dtype );
            return name().equals( baseDtype )
   
                /* If the submitted type is a VOTable-style type, we're not
                 * comparing like with like, so just check it's roughly
                 * similar. */
                || votableTypeSet_.contains( baseDtype );
        }

        /**
         * Indicates whether a given data type at least roughly corresponds
         * to this one.
         *
         * @param   dtype  type string for comparison
         * @return  true iff dtype is a rough match
         */
        boolean isCompatible( String dtype ) {
            String baseDtype = CompareMetadataStage.stripAdqlType( dtype );
            return name().equals( baseDtype )
                || adqlTypeSet_.contains( baseDtype )
                || votableTypeSet_.contains( baseDtype );
        }
    }

    /**
     * Converts an array of ObsCol objects into a name->value map.
     * The {@link #nameKey} method is used to normalise the column name.
     *
     * @param  cols  column metadata list
     * @return  map
     */
    private static Map<String,ObsCol> toMap( List<ObsCol> cols ) {
        Map<String,ObsCol> map = new LinkedHashMap<String,ObsCol>();
        for ( ObsCol col : cols ) {
            map.put( nameKey( col.name_ ), col );
        }
        assert cols.size() == map.size();
        return map;
    }

    /**
     * Converts an array of ColumnMeta objects into a name-&gt;value map.
     * The {@link #nameKey} method is used to normalise the column name.
     *
     * @param  cols  column metadata list
     * @return  map of normalised column name to column metadata object
     */
    public static Map<String,ColumnMeta> toMap( ColumnMeta[] cols ) {
        Map<String,ColumnMeta> map = new LinkedHashMap<String,ColumnMeta>();
        for ( int i = 0; i < cols.length; i++ ) {
            map.put( nameKey( cols[ i ].getName() ), cols[ i ] );
        }
        return map;
    }

    /**
     * Normalises a column name to produce a value suitable for use as a
     * Map key.
     *
     * @param   name   column name
     * @return   map-friendly value identifying <code>name</code>
     */
    public static String nameKey( String name ) {
        return name.toLowerCase();
    }

    /**
     * Enumeration for known versions of the ObsCore data model.
     */
    private enum ObscoreVersion {

        /* ObsCore 1.0. */
        V10( "ivo://ivoa.net/std/ObsCore/v1.0", false ),

        /* ObsCore 1.1. */
        V11( "ivo://ivoa.net/std/ObsCore#core-1.1", true );

        final Ivoid ivoid_;
        final boolean is11_;

        /**
         * Constructor.
         *
         * @param   ivoid  datamodel VO identifier, from ObsCore document
         * @param   is11   true for ObsCore 1.1, false for ObsCore 1.0
         */
        ObscoreVersion( String ivoid, boolean is11 ) {
            ivoid_ = new Ivoid( ivoid );
            is11_ = is11;
        }
    }

    /**
     * Represents metadata for a standard ObsCore column.
     */
    static class ObsCol {
        final String name_;
        final Type type_;
        final String utype_;
        final String ucd_;
        final String unit_;
        String ucdOnce_;
        boolean nullForbidden_;
        String[] hardOptions_;
        String[] softOptions_;
        Comparable<?>[] range_;   // [low, high] inclusive

        /**
         * Constructor including units.
         *
         * @param  name  column name
         * @param  type  data type
         * @param  utype  Utype, omitting "obscore:" prefix
         * @param  ucd    UCD
         * @param  unit   unit
         */
        ObsCol( String name, Type type, String utype, String ucd,
                String unit ) {
            name_ = name;
            type_ = type;
            utype_ = "obscore:" + utype;
            ucd_ = ucd;
            unit_ = unit;
        }

        /**
         * Constructor excluding units.
         *
         * @param  name  column name
         * @param  type  data type
         * @param  utype  Utype, omitting "obscore:" prefix
         * @param  ucd    UCD
         */
        ObsCol( String name, Type type, String utype, String ucd ) {
            this( name, type, utype, ucd, null );
        }
    }
}
