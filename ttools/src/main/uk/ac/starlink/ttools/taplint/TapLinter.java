package uk.ac.starlink.ttools.taplint;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.TaskException;

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
    private final XsdStage tcapXsdStage_;
    private final CapabilityStage tcapStage_;
    private final XsdStage availXsdStage_;

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
        tapSchemaStage_ = new TapSchemaStage();
        tcapXsdStage_ = XsdStage
                       .createXsdStage( CAPABILITIES_XSD, "/capabilities", true,
                                        "capabilities" );
        tcapStage_ = new CapabilityStage();
        availXsdStage_ = XsdStage
                        .createXsdStage( AVAILABILITY_XSD, "/availability",
                                         false, "availability" );

        /* Record them in order. */
        stageSet_ = new StageSet();
        stageSet_.add( "TMV", tmetaXsdStage_, true );
        stageSet_.add( "TME", tmetaStage_, true );
        stageSet_.add( "TMS", tapSchemaStage_, true );
        stageSet_.add( "CPV", tcapXsdStage_, false );
        stageSet_.add( "CPC", tcapStage_, true );
        stageSet_.add( "AVV", availXsdStage_, true );
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
     * @param  serviceUrl  TAP service URL
     * @param  reporter  validation message destination
     * @param  stageCodeSet  unordered collection of code strings indicating
     *         which stages should be run
     * @return   tap validator executable
     */
    public Executable createExecutable( final URL serviceUrl,
                                        final Reporter reporter,
                                        Set<String> stageCodeSet )
            throws TaskException {

        /* Prepare a checked and ordered sequence of codes determining
         * which stages will be executed.  Note the order is that defined
         * by the list of known codes, not that defined by the input set. */
        List<String> knownCodes = new ArrayList( stageSet_.stageMap_.keySet() );
        if ( ! knownCodes.containsAll( stageCodeSet ) ) {
            Set remainder = new HashSet( stageCodeSet );
            remainder.removeAll( knownCodes );
            throw new TaskException( "Unknown stage codes " + remainder );
        }
        knownCodes.retainAll( stageCodeSet );
        final String[] codes = knownCodes.toArray( new String[ 0 ] );

        /* Create and return an executable which will run the
         * requested stages. */
        return new Executable() {
            public void execute() {
                for ( int ic = 0; ic < codes.length; ic++ ) {
                    String code = codes[ ic ];
                    Stage stage = stageSet_.getStage( code );
                    assert stage != null;
                    reporter.startSection( code, stage.getDescription() );
                    stage.run( serviceUrl, reporter );
                    reporter.endSection();
                }
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
