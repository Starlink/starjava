package uk.ac.starlink.ttools.taplint;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.vo.EndpointSet;
import uk.ac.starlink.vo.SchemaMeta;
import uk.ac.starlink.vo.UserAgentUtil;

/**
 * Organises validation stages for TAP validator.
 *
 * @author   Mark Taylor
 * @since    6 Jun 2011
 */
public class TapLinter {

    private final StageSet stageSet_;
    private final XsdStage tmetaXsdStage_;
    private final TablesEndpointStage tmetaStage_;
    private final TapSchemaStage tapSchemaStage_;
    private final CompareMetadataStage cfTmetaStage_;
    private final XsdStage tcapXsdStage_;
    private final CapabilityStage tcapStage_;
    private final XsdStage availXsdStage_;
    private final QueryStage getQueryStage_;
    private final QueryStage postQueryStage_;
    private final QueryStage asyncQueryStage_;
    private final JobStage jobStage_;
    private final ColumnMetadataStage colMetaStage_;
    private final UploadStage uploadStage_;
    private final ObsTapStage obstapStage_;
    private final ExampleStage exampleStage_;
    private final TapSchemaMetadataHolder tapSchemaMetadata_;

    /** Name of the MDQ stage. */
    public static final String MDQ_NAME = "MDQ";

    /**
     * Constructor.
     */
    public TapLinter() {

        /* Create all known validation stages. */
        tmetaXsdStage_ = new XsdStage( IvoaSchemaResolver.VODATASERVICE_URI,
                                       "tableset", false, "table metadata" ) {
            public URL getDocumentUrl( EndpointSet endpointSet ) {
                return endpointSet.getTablesEndpoint();
            }
        };
        tmetaStage_ = new TablesEndpointStage();
        tapSchemaStage_ =
            new TapSchemaStage( VotLintTapRunner.createGetSyncRunner( true ) );
        tapSchemaMetadata_ = new TapSchemaMetadataHolder();
        MetadataHolder metaHolder =
                new AnyMetadataHolder( new MetadataHolder[] {
            tapSchemaStage_,
            tmetaStage_,
            tapSchemaMetadata_,
        } );
        MetadataHolder declaredMetaHolder =
                new AnyMetadataHolder( new MetadataHolder[] {
            tapSchemaStage_,
            tmetaStage_,
        } );
        cfTmetaStage_ = CompareMetadataStage
                       .createStage( tmetaStage_, tapSchemaStage_ );
        tcapXsdStage_ = new XsdStage( IvoaSchemaResolver.CAPABILITIES_URI,
                                      "capabilities", true, "capabilities" ) {
            public URL getDocumentUrl( EndpointSet endpointSet ) {
                return endpointSet.getCapabilitiesEndpoint();
            }
        };
        tcapStage_ = new CapabilityStage();
        availXsdStage_ = new XsdStage( IvoaSchemaResolver.AVAILABILITY_URI,
                                       "availability", false, "availability" ) {
            public URL getDocumentUrl( EndpointSet endpointSet ) {
                return endpointSet.getAvailabilityEndpoint();
            }
        };
        getQueryStage_ =
            new QueryStage( VotLintTapRunner.createGetSyncRunner( true ),
                            metaHolder, tcapStage_ );
        postQueryStage_ =
            new QueryStage( VotLintTapRunner.createPostSyncRunner( true ),
                            metaHolder, null );
        asyncQueryStage_ =
            new QueryStage( VotLintTapRunner.createAsyncRunner( 500, true ),
                            metaHolder, null );
        jobStage_ = new JobStage( metaHolder, 500 );
        colMetaStage_ =
            new ColumnMetadataStage( VotLintTapRunner
                                    .createGetSyncRunner( false ),
                                     declaredMetaHolder, -1 );
        uploadStage_ =
            new UploadStage( VotLintTapRunner.createAsyncRunner( 500, true ),
                             tcapStage_ );
        obstapStage_ =
            new ObsTapStage( VotLintTapRunner.createGetSyncRunner( true ),
                             tcapStage_,
                             new AnyMetadataHolder( new MetadataHolder[] {
                                 tapSchemaStage_, tmetaStage_,
                             } ) );
        exampleStage_ =
            new ExampleStage( VotLintTapRunner.createGetSyncRunner( true ),
                              tcapStage_,
                              new AnyMetadataHolder( new MetadataHolder[] {
                                  tapSchemaStage_, tmetaStage_,
                              } ) );

        /* Record them in order. */
        stageSet_ = new StageSet();
        stageSet_.add( "TMV", tmetaXsdStage_, true );
        stageSet_.add( "TME", tmetaStage_, true );
        stageSet_.add( "TMS", tapSchemaStage_, true );
        stageSet_.add( "TMC", cfTmetaStage_, true );
        stageSet_.add( "CPV", tcapXsdStage_, true );
        stageSet_.add( "CAP", tcapStage_, true );
        stageSet_.add( "AVV", availXsdStage_, true );
        stageSet_.add( "QGE", getQueryStage_, true );
        stageSet_.add( "QPO", postQueryStage_, true );
        stageSet_.add( "QAS", asyncQueryStage_, true );
        stageSet_.add( "UWS", jobStage_, true );
        stageSet_.add( MDQ_NAME, colMetaStage_, true );
        stageSet_.add( "OBS", obstapStage_, true );
        stageSet_.add( "UPL", uploadStage_, true );
        stageSet_.add( "EXA", exampleStage_, true );
    }

    /**
     * Returns an ordered map of the validation stages defined by this class.
     *
     * @return  ordered code-&gt;stage map
     */
    public Map<String,Stage> getKnownStages() {
        return Collections
              .unmodifiableMap( new LinkedHashMap<String,Stage>( stageSet_
                                                                .stageMap_ ) );
    }

    /**
     * Indicates whether the stage with a given code is run by default or not.
     *
     * @param   code  stage code
     * @return  true iff the corresponding stage is run by default 
     */
    public boolean isDefault( String code ) {
        return stageSet_.dflts_.contains( code );
    }

    /**
     * Creates and returns an executable for TAP validation.
     *
     * @param  reporter  validation message destination
     * @param  endpointSet  locations of TAP services
     * @param  stageCodeSet  unordered collection of code strings indicating
     *         which stages should be run
     * @param  maxTestTables  limit on the number of tables to test,
     *                        or &lt;=0 for no limit
     * @return   tap validator executable
     */
    public Executable createExecutable( final OutputReporter reporter,
                                        final EndpointSet endpointSet,
                                        Set<String> stageCodeSet,
                                        int maxTestTables )
            throws TaskException {

        /* Prepare a checked and ordered sequence of codes determining
         * which stages will be executed.  Note the order is that defined
         * by the list of known codes, not that defined by the input set. */
        List<String> selectedCodeList = new ArrayList<String>();
        for ( String knownCode : stageSet_.stageMap_.keySet() ) {
            boolean selected = false;
            for ( Iterator<String> sit = stageCodeSet.iterator();
                  ! selected && sit.hasNext(); ) {
                if ( knownCode.equalsIgnoreCase( sit.next() ) ) {
                    sit.remove();
                    selected = true;
                }
            }
            if ( selected ) {
                selectedCodeList.add( knownCode );
            }
        }
        if ( ! stageCodeSet.isEmpty() ) {
            throw new TaskException( "Unknown stage codes " + stageCodeSet );
        }
        final String[] codes = selectedCodeList.toArray( new String[ 0 ] );

        /* Other initialisation. */
        tapSchemaMetadata_.setReporter( reporter );
        colMetaStage_.setMaxTestTables( maxTestTables );

        /* Create and return an executable which will run the
         * requested stages. */
        final String[] announcements = getAnnouncements();
        return new Executable() {
            public void execute() {
                String uaToken = UserAgentUtil.COMMENT_VALIDATE;
                UserAgentUtil.pushUserAgentToken( uaToken );
                try {
                    reporter.start( announcements );
                    for ( int ic = 0; ic < codes.length; ic++ ) {
                        String code = codes[ ic ];
                        Stage stage = stageSet_.getStage( code );
                        assert stage != null;
                        reporter.startSection( code, stage.getDescription() );
                        stage.run( reporter, endpointSet );
                        reporter.summariseUnreportedMessages( code );
                        reporter.endSection();
                    }
                    reporter.end();
                }
                finally {
                    UserAgentUtil.popUserAgentToken( uaToken );
                }
            }
        };
    }

    /**
     * Returns a list of startup announcements with which the taplint
     * application introduces itself.
     *
     * @return   announcement lines
     */
    private static String[] getAnnouncements() {

        /* Version report. */
        String versionLine = new StringBuffer()
            .append( "This is STILTS taplint, " )
            .append( Stilts.getVersion() )
            .append( "/" )
            .append( Stilts.getStarjavaRevision() )
            .toString();

        /* Count by report type of known FixedCode instances. */
        Map<ReportType,int[]> codeMap = new LinkedHashMap<ReportType,int[]>();
        for ( ReportType type : ReportType.values() ) {
            codeMap.put( type, new int[ 1 ] );
        }
        for ( FixedCode code : FixedCode.values() ) {
            codeMap.get( code.getType() )[ 0 ]++;
        }
        StringBuffer cbuf = new StringBuffer()
            .append( "Static report types: " );
        for ( Map.Entry<ReportType,int[]> entry : codeMap.entrySet() ) {
            cbuf.append( entry.getKey() )
                .append( "(" )
                .append( entry.getValue()[ 0 ] )
                .append( ")" )
                .append( ", " );
        }
        cbuf.setLength( cbuf.length() - 2 );
        String codesLine = cbuf.toString();

        /* Return lines. */
        return new String[] { versionLine, codesLine, };
    }

    /**
     * MetadataHolder implementation which delegates operations to any
     * of those it owns.
     */
    private static class AnyMetadataHolder implements MetadataHolder {
        private final MetadataHolder[] holders_;

        /**
         * Constructor.
         *
         * @param  holders  subordinate candidate metadata suppliers
         */
        AnyMetadataHolder( MetadataHolder[] holders ) {
            holders_ = holders;
        }

        public SchemaMeta[] getTableMetadata() {
            for ( int ih = 0; ih < holders_.length; ih++ ) {
                SchemaMeta[] smetas = holders_[ ih ].getTableMetadata();
                if ( smetas != null && smetas.length > 0 ) {
                    return smetas;
                }
            }
            return null;
        }
    }

    /**
     * Data structure which organises the known stages.
     */
    private static class StageSet {
        Map<String,Stage> stageMap_;
        Set<String> dflts_;

        /**
         * Constructor.
         */
        StageSet() {
            stageMap_ = new LinkedHashMap<String,Stage>();
            dflts_ = new HashSet<String>();
        }

        /**
         * Adds a stage to the list.
         *
         * @param  code  code string
         * @param  stage  stage
         * @param  dflt  true iff stage is run by default
         */
        void add( String code, Stage stage, boolean dflt ) {
            stageMap_.put( code, stage );
            if ( dflt ) {
                dflts_.add( code );
            }
        }

        /**
         * Returns the stage for a given code.
         *
         * @param  code  code
         * @return  stage
         */
        Stage getStage( String code ) {
            return stageMap_.get( code );
        }
    }
}
