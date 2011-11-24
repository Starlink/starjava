package uk.ac.starlink.ttools.taplint;

import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TapCapability;

public class ObsTapStage implements Stage {

    private final TapRunner tapRunner_;
    private final CapabilityHolder capHolder_;
    private final MetadataHolder metaHolder_;

    private static final String OBSCORE_ID = "ivo://ivoa.net/std/ObsCore-1.0";
    private static final String OBSCORE_TNAME = "ivoa.ObsCore";

    private static Map<String,ObsCol> mandatoryColumnMap_;
    private static Map<String,ObsCol> optionalColumnMap_;

    public ObsTapStage( TapRunner tapRunner, CapabilityHolder capHolder,
                        MetadataHolder metaHolder ) {
        tapRunner_ = tapRunner;
        capHolder_ = capHolder;
        metaHolder_ = metaHolder;
    }

    public String getDescription() {
        return "Tests implementation of ObsCore DM";
    }

    public void run( Reporter reporter, URL serviceUrl ) {
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
        new ObsTapRunner( reporter, serviceUrl, obsMeta, tapRunner_ ).run();
    }

    private static class ObsTapRunner implements Runnable {
        private final Reporter reporter_;
        private final URL serviceUrl_;
        private final TableMeta obsMeta_;
        private final TapRunner tRunner_;
        private final Map<String,ObsCol> reqColMap_;
        private final Map<String,ObsCol> optColMap_;

        /**
         * Constructor.
         *
         * @param  reporter  validation message destination
         * @param  serviceUrl  TAP base URL
         * @param  tapRunner  runs TAP queries
         */
        ObsTapRunner( Reporter reporter, URL serviceUrl, TableMeta obsMeta,
                      TapRunner tapRunner ) {
            reporter_ = reporter;
            serviceUrl_ = serviceUrl;
            obsMeta_ = obsMeta;
            tRunner_ = tapRunner;
            reqColMap_ = getMandatoryColumns();
            optColMap_ = getOptionalColumns();
        }

        /**
         * Runs the test.
         */
        public void run() {
            for ( Iterator<String> it = reqColMap_.keySet().iterator();
                 it.hasNext(); ) {
            }
        }
    }

    private static Map<String,ObsCol> getMandatoryColumns() {
        if ( mandatoryColumnMap_ == null ) {
            mandatoryColumnMap_ = createMandatoryColumns();
        }
        return mandatoryColumnMap_;
    }

    private static Map<String,ObsCol> getOptionalColumns() {
        if ( optionalColumnMap_ == null ) {
            optionalColumnMap_ = createOptionalColumns();
        }
        return optionalColumnMap_;
    }

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

    private enum Type {
        INTEGER, BIGINT, DOUBLE, VARCHAR, TIMESTAMP, REGION, CLOB;
    }

    private static Map<String,ObsCol> toMap( ObsCol[] cols ) {
        Map<String,ObsCol> map = new LinkedHashMap<String,ObsCol>();
        for ( int i = 0; i < cols.length; i++ ) {
            map.put( cols[ i ].name_, cols[ i ] );
        }
        assert cols.length == map.size();
        return map;
    }

    private static class ObsCol {
        final String name_;
        final Type type_;
        final String utype_;
        final String ucd_;
        final String unit_;
        boolean nullForbidden_;
        Object[] options_;

        ObsCol( String name, Type type, String utype, String ucd ) {
            this( name, type, utype, ucd, null );
        }

        ObsCol( String name, Type type, String utype, String ucd,
                String unit ) {
            name_ = name;
            type_ = type;
            utype_ = utype;
            ucd_ = ucd;
            unit_ = unit;
        }
    }
}
