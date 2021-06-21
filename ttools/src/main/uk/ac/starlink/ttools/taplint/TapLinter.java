package uk.ac.starlink.ttools.taplint;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.util.URLUtils;
import uk.ac.starlink.vo.StdCapabilityInterface;
import uk.ac.starlink.vo.TapCapabilitiesDoc;
import uk.ac.starlink.vo.TapCapability;
import uk.ac.starlink.vo.TapService;
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
    private final ObsLocStage obslocStage_;
    private final ExampleStage exampleStage_;
    private final TapSchemaMetadataHolder tapSchemaMetadata_;
    private final CapabilitiesReader capabilitiesReader_;

    /** Name of the MDQ stage. */
    public static final String MDQ_NAME = "MDQ";

    /**
     * Constructor.
     */
    public TapLinter() {

        /* Create all known validation stages. */
        capabilitiesReader_ = new CapabilitiesReader();
        tmetaXsdStage_ = new XsdStage( IvoaSchemaResolver.VODATASERVICE_URI,
                                       "tableset", false, "table metadata" ) {
            public URL getDocumentUrl( TapService tapService ) {
                return tapService.getTablesEndpoint();
            }
        };
        tmetaStage_ = new TablesEndpointStage( capabilitiesReader_ );
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
            public URL getDocumentUrl( TapService tapService ) {
                return tapService.getCapabilitiesEndpoint();
            }
        };
        tcapStage_ = new CapabilityStage( capabilitiesReader_ );
        availXsdStage_ = new XsdStage( IvoaSchemaResolver.AVAILABILITY_URI,
                                       "availability", false, "availability" ) {
            public URL getDocumentUrl( TapService tapService ) {
                return tapService.getAvailabilityEndpoint();
            }
        };
        getQueryStage_ =
            new QueryStage( VotLintTapRunner.createGetSyncRunner( true ),
                            metaHolder, capabilitiesReader_ );
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
                             capabilitiesReader_ );
        obstapStage_ =
            new ObsTapStage( VotLintTapRunner.createGetSyncRunner( true ),
                             capabilitiesReader_,
                             new AnyMetadataHolder( new MetadataHolder[] {
                                 tapSchemaStage_, tmetaStage_,
                             } ) );
        obslocStage_ =
            new ObsLocStage( VotLintTapRunner.createGetSyncRunner( true ),
                             capabilitiesReader_, metaHolder );
        exampleStage_ =
            new ExampleStage( VotLintTapRunner.createGetSyncRunner( true ),
                              capabilitiesReader_,
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
        stageSet_.add( "LOC", obslocStage_, true );
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
     * @param  tapService   TAP service description
     * @param  stageCodeSet  unordered collection of code strings indicating
     *         which stages should be run
     * @param  maxTestTables  limit on the number of tables to test,
     *                        or &lt;=0 for no limit
     * @return   tap validator executable
     */
    public Executable createExecutable( final OutputReporter reporter,
                                        final TapService tapService,
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
        capabilitiesReader_.init( reporter, tapService );
        colMetaStage_.setMaxTestTables( maxTestTables );

        /* Create and return an executable which will run the
         * requested stages. */
        final String[] announcements = getAnnouncements();
        return new Executable() {
            public void execute() {
                String uaToken = UserAgentUtil.COMMENT_TEST;
                UserAgentUtil.pushUserAgentToken( uaToken );
                try {
                    reporter.start( announcements );
                    for ( int ic = 0; ic < codes.length; ic++ ) {
                        String code = codes[ ic ];
                        Stage stage = stageSet_.getStage( code );
                        assert stage != null;
                        reporter.startSection( code, stage.getDescription() );
                        stage.run( reporter, tapService );
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
            MetadataHolder h = getBestHolder();
            return h == null ? null : h.getTableMetadata();
        }

        public boolean hasDetail() {
            MetadataHolder h = getBestHolder();
            return h == null ? false : h.hasDetail();
        }

        /**
         * Returns from the list of available metadata suppliers
         * the one that has the most complete metadata.
         *
         * @return  best available metadata holder
         */
        private MetadataHolder getBestHolder() {

            /* Pick only populated holders. */
            List<MetadataHolder> popHolders = new ArrayList<>();
            for ( MetadataHolder h : holders_ ) {
                SchemaMeta[] smetas = h.getTableMetadata();
                if ( smetas != null && smetas.length > 0 ) {
                    popHolders.add( h );
                }
            }

            /* Return preferentially one with column/fkey detail. */
            for ( MetadataHolder h : popHolders ) {
                if ( h.hasDetail() ) {
                    return h;
                }
            }

            /* Otherwise, return any populated instance or null. */
            return popHolders.size() > 0 ? popHolders.get( 0 ) : null;
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

    /**
     * CapabilityHolder implementation.
     * It reads the capabilities document, once, when it is first requested.
     * The init method must be called before it is used.
     */
    private static class CapabilitiesReader implements CapabilityHolder {
        private Reporter reporter_;
        private TapService tapService_;
        private CapabilityHolder holder_;

        /**
         * Initialises for use.
         *
         * @param  reporter  reporter
         * @param  tapService   target service
         */
        public void init( Reporter reporter, TapService tapService ) {
            reporter_ = reporter;
            tapService_ = tapService;
        }

        public Element getElement() {
            return getCapabilityHolder().getElement();
        }

        public TapCapability getCapability() {
            return getCapabilityHolder().getCapability();
        }

        public StdCapabilityInterface[] getInterfaces() {
            return getCapabilityHolder().getInterfaces();
        }

        public String getServerHeader() {
            return getCapabilityHolder().getServerHeader();
        }

        /**
         * Returns a lazily-read capabilities document.
         * May be a dummy if reading fails, but will not be null.
         *
         * @return  capabilities document, not null
         */
        private CapabilityHolder getCapabilityHolder() {
            if ( holder_ == null ) {
                holder_ = readCapabilityHolder();
            }
            return holder_;
        }

        /**
         * Reads a capabilities holder from the TAP service.
         * May be a dummy if reading fails, but will not be null.
         *
         * @return  capabilities holder, not null
         */
        private CapabilityHolder readCapabilityHolder() {
            URL capUrl = tapService_.getCapabilitiesEndpoint();
            reporter_.report( FixedCode.I_CURL,
                              "Reading capability metadata from " + capUrl );
            InputStream in = null;
            Element el;
            String serverHdr;
            try {
                URLConnection conn =
                    URLUtils.followRedirects( capUrl.openConnection(), null );
                serverHdr = conn.getHeaderField( "server" );
                in = new BufferedInputStream( conn.getInputStream() );
                el = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse( in )
                    .getDocumentElement();
            }
            catch ( SAXException e ) {
                el = null;
                serverHdr = null;
                reporter_.report( FixedCode.E_CPSX,
                                  "Error parsing capabilities metadata", e );
            }
            catch ( ParserConfigurationException e ) {
                el = null;
                serverHdr = null;
                reporter_.report( FixedCode.F_CAPC,
                                  "Trouble setting up XML parse", e );
            }
            catch ( IOException e ) {
                el = null;
                serverHdr = null;
                reporter_.report( FixedCode.E_CPIO,
                                  "Error reading capabilities metadata", e );
            }
            finally {
                if ( in != null ) {
                    try {
                        in.close();
                    }
                    catch ( IOException e ) {
                    }
                }
            }
            final StdCapabilityInterface[] intfs =
                el == null ? null
                           : TapCapabilitiesDoc.getInterfaces( el );
            TapCapability tapcap;
            if ( el != null ) {
                try {
                    tapcap = TapCapabilitiesDoc.getTapCapability( el );
                }
                catch ( XPathExpressionException e ) {
                    tapcap = null;
                    reporter_.report( FixedCode.E_CPSX,
                                      "Error parsing capabilities metadata",
                                      e);
                }
            }
            else {
                tapcap = null;
            }
            final Element el0 = el;
            final TapCapability tapcap0 = tapcap;
            final String serverHdr0 = serverHdr;
            return new CapabilityHolder() {
                public Element getElement() {
                    return el0;
                }
                public TapCapability getCapability() {
                    return tapcap0;
                }
                public StdCapabilityInterface[] getInterfaces() {
                    return intfs;
                }
                public String getServerHeader() {
                    return serverHdr0;
                }
            };
        }
    }
}
