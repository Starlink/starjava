package uk.ac.starlink.ttools.taplint;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import uk.ac.starlink.vo.ColumnMeta;
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TapCapability;

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

    private static final String OBSCORE_ID = "ivo://ivoa.net/std/ObsCore-1.0";
    private static final String OBSCORE_TNAME = "ivoa.ObsCore";

    private static Map<String,ObsCol> mandatoryColumnMap_;
    private static Map<String,ObsCol> optionalColumnMap_;

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
        return "Tests implementation of ObsCore Data Model";
    }

    public void run( Reporter reporter, URL serviceUrl ) {

        /* Check prerequisites. */
        TapCapability tcap = capHolder_.getCapability();
        if ( tcap != null ) {
            String[] dms = tcap.getDataModels();
            if ( dms == null ||
                 ! Arrays.asList( dms ).contains( OBSCORE_ID ) ) {
                reporter.report( ReportType.FAILURE, "NODM",
                                 "Table capabilities lists no DataModel "
                               + OBSCORE_ID );
                return;
            }
        }
        TableMeta[] tmetas = metaHolder_.getTableMetadata();
        if ( tmetas == null ) {
            reporter.report( ReportType.FAILURE, "NOTM",
                             "No table metadata"
                           + " (earlier stages failed/skipped?)" );
            return;
        }

        /* Get ObsCore table if present. */
        TableMeta obsMeta = null;
        for ( int im = 0; im < tmetas.length; im++ ) {
            TableMeta tmeta = tmetas[ im ];
            if ( OBSCORE_TNAME.equalsIgnoreCase( tmeta.getName() ) ) {
                obsMeta = tmeta;
            }
        }
        if ( obsMeta == null ) {
            reporter.report( ReportType.FAILURE, "NOTB",
                             "No table with name " + OBSCORE_TNAME );
            return;
        }

        /* Run tests. */
        new ObsTapRunner( reporter, serviceUrl, obsMeta, tapRunner_ ).run();
    }

    /**
     * Does the work for running tests on ObsCore table.
     */
    private static class ObsTapRunner implements Runnable {
        private final Reporter reporter_;
        private final URL serviceUrl_;
        private final TapRunner tRunner_;
        private final Map<String,ColumnMeta> gotColMap_;
        private final Map<String,ObsCol> reqColMap_;
        private final Map<String,ObsCol> optColMap_;

        /**
         * Constructor.
         *
         * @param  reporter  validation message destination
         * @param  serviceUrl  TAP base URL
         * @param  obsMeta   table metadata for ivoa.ObsCore table
         * @param  tapRunner  runs TAP queries
         */
        ObsTapRunner( Reporter reporter, URL serviceUrl, TableMeta obsMeta,
                      TapRunner tapRunner ) {
            reporter_ = reporter;
            serviceUrl_ = serviceUrl;
            gotColMap_ = toMap( obsMeta.getColumns() );
            tRunner_ = tapRunner;
            reqColMap_ = getMandatoryColumns();
            optColMap_ = getOptionalColumns();
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
                    compareCols( reqCol, gotCol );
                    nreq++;
                }
                else {
                    String msg = new StringBuffer()
                       .append( "Required ObsCore column " )
                       .append( reqName )
                       .append( " is missing" )
                       .toString();
                    reporter_.report( ReportType.ERROR, "MCOL", msg );
                }
            }

            /* Check column metadata for optional columns. */
            int nopt = 0;
            for ( String optName : optColMap_.keySet() ) {
                ObsCol optCol = optColMap_.get( optName );
                ColumnMeta gotCol = gotColMap_.get( optName );
                if ( gotCol != null ) {
                    compareCols( optCol, gotCol );
                    nopt++;
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
            reporter_.report( ReportType.SUMMARY, "COLS", msg );
        }

        /**
         * Checks that the metadata for a given column has the correct
         * metadata.
         *
         * @param  obsCol  correct metadata for a column
         * @param  gotCol  metadata actually present in a column
         */
        private void compareCols( ObsCol obsCol, ColumnMeta gotCol ) {
            String cname = gotCol.getName();
            compareItem( cname, "Utype", "CUTP",
                         obsCol.utype_, gotCol.getUtype(), false );
            compareItem( cname, "UCD", "CUCD",
                         obsCol.ucd_, gotCol.getUcd(), false );
            compareItem( cname, "Unit", "CUNI",
                         obsCol.unit_, gotCol.getUnit(), true );
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
        private void compareItem( String colName, String itemName, String code,
                                  String obsValue, String gotValue,
                                  boolean isCaseSensitive ) {
            String vGot = String.valueOf( gotValue );
            String vObs = String.valueOf( obsValue );
            if ( isCaseSensitive ? ( ! vGot.equals( vObs ) )
                                 : ( ! vGot.equalsIgnoreCase( vObs ) ) ) {
                String msg = new StringBuffer()
                    .append( "ObsCore column " )
                    .append( colName )
                    .append( " has wrong " )
                    .append( itemName )
                    .append( ": " )
                    .append( gotValue )
                    .append( " != " )
                    .append( obsValue )
                    .toString();
                reporter_.report( ReportType.ERROR, code, msg );
            }
        }
    }

    /**
     * Returns metadata objects for the standard required ObsCore columns.
     *
     * @return   lazily created name->metadata map
     */
    private static Map<String,ObsCol> getMandatoryColumns() {
        if ( mandatoryColumnMap_ == null ) {
            mandatoryColumnMap_ = createMandatoryColumns();
        }
        return mandatoryColumnMap_;
    }

    /**
     * Returns metadata objects for the standard optional ObsCore columns.
     *
     * @return  lazily created name->metadata map
     */
    private static Map<String,ObsCol> getOptionalColumns() {
        if ( optionalColumnMap_ == null ) {
            optionalColumnMap_ = createOptionalColumns();
        }
        return optionalColumnMap_;
    }

    /**
     * Creates a description of the standard required ObsCore columns.
     *
     * @return  new name->metadata map
     */
    private static Map<String,ObsCol> createMandatoryColumns() {
        Map<String,ObsCol> map = toMap( new ObsCol[] {
            new ObsCol( "dataproduct_type", Type.VARCHAR,
                        "Obs.dataProductType", "meta.id" ),
            new ObsCol( "calib_level", Type.INTEGER,
                        "Obs.calibLevel", "meta.code;obs.calib" ),
            new ObsCol( "obs_collection", Type.VARCHAR,
                        "DataID.Collection", "meta.id" ),
            new ObsCol( "obs_id", Type.VARCHAR,
                        "DataID.observationID", "meta.id" ),
            new ObsCol( "obs_publisher_did", Type.VARCHAR,
                        "Curation.PublisherDID", "meta.ref.url;meta.curation" ),
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
                        "phys.angArea;obs",
                        null ), // from ObsTAP Table 6 but not Table 1
            new ObsCol( "s_resolution", Type.DOUBLE,
                        "Char.SpatialAxis.Resolution.refval",
                        "pos.angResolution", "arcsec" ),
            new ObsCol( "t_min", Type.DOUBLE,
                        "Char.TimeAxis.Coverage.Bounds.Limits.Interval"
                        + ".StartTime", "time.start;obs.exposure", "d" ),
            new ObsCol( "t_max", Type.DOUBLE,
                        "Char.TimeAxis.Coverage.Bounds.Limits.Interval"
                        + ".StopTime", "time.end;obs.exposure", "d" ),
            new ObsCol( "t_exptime", Type.DOUBLE,
                        "Char.TimeAxis.Coverage.Support.Extent",
                        "time.duration;obs.exposure", "s" ),
            new ObsCol( "t_resolution", Type.DOUBLE,
                        "Char.TimeAxis.Resolution.refval", "time.resolution",
                        "s" ),
            new ObsCol( "em_min", Type.DOUBLE,
                        "Char.SpectralAxis.Coverage.Bounds.Limits.Interval"
                        + ".LoLim", "em.wl;stat.min", "m" ),
            new ObsCol( "em_max", Type.DOUBLE,
                        "Char.SpectralAxis.Coverate.Bounds.Limits.Interval"
                        + ".HiLim", "em.wl;stat.max", "m" ),
            new ObsCol( "em_res_power", Type.DOUBLE,
                        "Char.SpectralAxis.Resolution.ResolPower.refVal",
                        "spect.resolution" ),
            new ObsCol( "o_ucd", Type.VARCHAR,
                        "Char.ObservableAxis.ucd", "meta.ucd" ),
            new ObsCol( "pol_states", Type.VARCHAR,
                        "Char.PolarizationAxis.stateList",
                        "meta.code;phys.polarization" ),
            new ObsCol( "facility_name", Type.VARCHAR,
                        "Provenance.ObsConfig.facility.name",
                        "meta.id;instr.tel" ),
            new ObsCol( "instrument_name", Type.VARCHAR,
                        "Provenance.ObsConfig.instrument.name",
                        "meta.id;instr" ),
        } );
        assert map.size() == 25;

        /* Note some additional constraints. */
        map.get( "dataproduct_type" ).options_ = new String[] {
            "image", "cube", "spectrum", "sed", "timeseries", "visibility",
            "event", null,
        };
        map.get( "calib_level" ).options_ = new Integer[] {
           new Integer( 0 ), new Integer( 1 ),
           new Integer( 2 ), new Integer( 3 ),
        };
        map.get( "obs_collection" ).nullForbidden_ = true;
        map.get( "obs_id" ).nullForbidden_ = true;

        return map;
    }

    /**
     * Creates a description of the standard optional ObsCore columns.
     *
     * @return  new name->metadata map
     */
    private static Map<String,ObsCol> createOptionalColumns() {
        Map<String,ObsCol> map = toMap( new ObsCol[] {
            new ObsCol( "dataproduct_subtype", Type.VARCHAR,
                        "Obs.dataProductSubtype", "meta.id" ),
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
                        "Curation.PublisherID", "meta.ref.url;meta.curation" ),
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
                        "Char.SpatialAxis.Resolution.Bounds.Limits.Interval"
                        + ".LoLim", "pos.angResolution;stat.min", "arcsec" ),
            new ObsCol( "s_resolution_max", Type.DOUBLE,
                        "Char.SpatialAxis.Resolution.Bounds.Limits.Interval"
                        + ".HiLim", "pos.angResolution;stat.max", "arcsec" ),
            new ObsCol( "s_calib_status", Type.VARCHAR,
                        "Char.SpatialAxis.calibStatus", "meta.code.qual" ),
            new ObsCol( "s_stat_error", Type.DOUBLE,
                        "Char.SpatialAxis.Accuracy.statError.refval.value",
                        "stat.error;pos.eq", "arcsec" ),
            new ObsCol( "t_calib_status", Type.VARCHAR,
                        "Char.TimeAxis.calibStatus", "meta.code.qual" ),
            new ObsCol( "t_stat_error", Type.DOUBLE,
                        "Char.TimeAxis.Accuracy.StatError.refval.value",
                        "stat.error;time", "s" ),
            new ObsCol( "em_ucd", Type.VARCHAR,
                        "Char.SpectralAxis.ucd", "meta.ucd" ),
            new ObsCol( "em_unit", Type.VARCHAR,
                        "Char.SpectralAxis.unit", "meta.unit" ),
            new ObsCol( "em_calib_status", Type.VARCHAR,
                        "Char.SpectralAxis.calibStatus", "meta.code.qual" ),
            new ObsCol( "em_res_power_min", Type.DOUBLE,
                        "Char.SpectralAxis.Resolution.ResolPower.LoLim",
                        "spect.resolution;stat.min" ),
            new ObsCol( "em_res_power_max", Type.DOUBLE,
                        "Char.SpectralAxis.Resolution.ResolPower.HiLim",
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
                        "Char.ObservableAxis.calibStatus", "meta.code.qual" ),
            new ObsCol( "o_stat_error", Type.DOUBLE,
                        "Char.ObservableAxis.Accuracy.StatError.refval.value",
                        "stat.error;phot.flux" ),
            new ObsCol( "proposal_id", Type.VARCHAR,
                        "Provenance.Proposal.identifier",
                        "meta.id;obs.proposal" ),
        } );
        assert map.size() == 29;
        return map;
    }

    /**
     * Enumeration of known data types for standard columns.
     */
    private enum Type {
        INTEGER, BIGINT, DOUBLE, VARCHAR, TIMESTAMP, REGION, CLOB;
    }

    /**
     * Converts an array of ObsCol objects into a name->value map.
     *
     * @param  cols  column metadata list
     * @return  map
     */
    private static Map<String,ObsCol> toMap( ObsCol[] cols ) {
        Map<String,ObsCol> map = new LinkedHashMap<String,ObsCol>();
        for ( int i = 0; i < cols.length; i++ ) {
            map.put( nameKey( cols[ i ].name_ ), cols[ i ] );
        }
        assert cols.length == map.size();
        return map;
    }

    /**
     * Converts an array of ColumnMeta objects into a name->value map.
     *
     * @param  cols  column metadata list
     * @return  map
     */
    private static Map<String,ColumnMeta> toMap( ColumnMeta[] cols ) {
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
    private static String nameKey( String name ) {
        return name.toLowerCase();
    }

    /**
     * Represents metadata for a standard ObsCore column.
     */
    private static class ObsCol {
        final String name_;
        final Type type_;
        final String utype_;
        final String ucd_;
        final String unit_;
        boolean nullForbidden_;
        Object[] options_;

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
