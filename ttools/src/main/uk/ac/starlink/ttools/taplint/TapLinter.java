package uk.ac.starlink.ttools.taplint;

import java.net.MalformedURLException;
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
import uk.ac.starlink.vo.TableMeta;

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
    private final TapSchemaMetadataHolder tapSchemaMetadata_;

    private static final String XSDS = "http://www.ivoa.net/xml";
    private static final URL VODATASERVICE_XSD =
        toUrl( XSDS + "/VODataService/VODataService-v1.1.xsd" );
    private static final URL CAPABILITIES_XSD =
        TapLinter.class.getResource( "VOSICapabilities-v1.0.xsd" );
    private static final URL AVAILABILITY_XSD =
        TapLinter.class.getResource( "VOSIAvailability-v1.0.xsd" );

    /**
     * Constructor.
     */
    public TapLinter() {

        /* Create all known validation stages. */
        tmetaXsdStage_ = XsdStage
                        .createXsdStage( VODATASERVICE_XSD, "/tables", false,
                                         "table metadata" );
        tmetaStage_ = new TablesEndpointStage();
        tapSchemaStage_ =
            new TapSchemaStage( VotLintTapRunner.createGetSyncRunner( true ) );
        tapSchemaMetadata_ = new TapSchemaMetadataHolder();
        MetadataHolder metaHolder =
                new AnyMetadataHolder( new MetadataHolder[] {
            tmetaStage_,
            tapSchemaStage_,
            tapSchemaMetadata_,
        } );
        MetadataHolder declaredMetaHolder =
                new AnyMetadataHolder( new MetadataHolder[] {
            tmetaStage_,
            tapSchemaStage_,
        } );
        cfTmetaStage_ = CompareMetadataStage
                       .createStage( tmetaStage_, tapSchemaStage_ );
        tcapXsdStage_ = XsdStage
                       .createXsdStage( CAPABILITIES_XSD, "/capabilities", true,
                                        "capabilities" );
        tcapStage_ = new CapabilityStage();
        availXsdStage_ = XsdStage
                        .createXsdStage( AVAILABILITY_XSD, "/availability",
                                         false, "availability" );
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
                                     declaredMetaHolder, 0 );
        uploadStage_ =
            new UploadStage( VotLintTapRunner.createAsyncRunner( 500, true ),
                             tcapStage_ );

        /* Record them in order. */
        stageSet_ = new StageSet();
        stageSet_.add( "TMV", tmetaXsdStage_, true );
        stageSet_.add( "TME", tmetaStage_, true );
        stageSet_.add( "TMS", tapSchemaStage_, true );
        stageSet_.add( "TMC", cfTmetaStage_, true );
        stageSet_.add( "CPV", tcapXsdStage_, false );
        stageSet_.add( "CAP", tcapStage_, true );
        stageSet_.add( "AVV", availXsdStage_, true );
        stageSet_.add( "QGE", getQueryStage_, true );
        stageSet_.add( "QPO", postQueryStage_, true );
        stageSet_.add( "QAS", asyncQueryStage_, true );
        stageSet_.add( "UWS", jobStage_, true );
        stageSet_.add( "MDQ", colMetaStage_, true );
        stageSet_.add( "UPL", uploadStage_, true );
    }

    /**
     * Returns an ordered map of the validation stages defined by this class.
     *
     * @return  ordered code->stage map
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
     * @param  serviceUrl  TAP service URL
     * @param  stageCodeSet  unordered collection of code strings indicating
     *         which stages should be run
     * @return   tap validator executable
     */
    public Executable createExecutable( final Reporter reporter,
                                        final URL serviceUrl,
                                        Set<String> stageCodeSet )
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

        /* Create and return an executable which will run the
         * requested stages. */
        return new Executable() {
            public void execute() {
                reporter.start();
                for ( int ic = 0; ic < codes.length; ic++ ) {
                    String code = codes[ ic ];
                    Stage stage = stageSet_.getStage( code );
                    assert stage != null;
                    reporter.startSection( code, stage.getDescription() );
                    stage.run( reporter, serviceUrl );
                    reporter.summariseUnreportedMessages( code );
                    reporter.endSection();
                }
                reporter.end();
            }
        };
    }

    /**
     * Utility method to turn a string into a URL without worrying about
     * pesky MalformedURLExceptions.  Any MUE is rethrown as an
     * IllegalArgumentException instead.
     *
     * @param  url  string
     * @return  URL
     */
    private static URL toUrl( String url ) {
        try {
            return new URL( url );
        }
        catch ( MalformedURLException e ) {
            throw (IllegalArgumentException)
                  new IllegalArgumentException( "Bad URL " + url )
                 .initCause( e );
        }
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

        public TableMeta[] getTableMetadata() {
            for ( int ih = 0; ih < holders_.length; ih++ ) {
                TableMeta[] tmetas = holders_[ ih ].getTableMetadata();
                if ( tmetas != null && tmetas.length > 0 ) {
                    return tmetas;
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
