package uk.ac.starlink.ttools.taplint;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TapCapability;

/**
 * Performs TAP validation.
 *
 * @author   Mark Taylor
 * @since    3 Jun 2011
 */
public class TapLintExecutable implements Executable {

    private final URL serviceUrl_;
    private final Reporter reporter_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.taplint" );

    private static final String XSDS = "http://www.ivoa.net/xml";
    private static final URL VODATASERVICE_XSD =
        toUrl( XSDS + "/VODataService/VODataService-v1.1.xsd" );
    private static final URL CAPABILITIES_XSD =
        TapLintExecutable.class.getResource( "VOSICapabilities-v1.0.xsd" );
    private static final URL AVAILABILITY_XSD =
        TapLintExecutable.class.getResource( "VOSIAvailability-v1.0.xsd" );

    public TapLintExecutable( URL serviceUrl, Reporter reporter ) {
        serviceUrl_ = serviceUrl;
        reporter_ = reporter;
    }

    public void execute() {
        XsdStage tmXsdStage =
            XsdStage.createXsdStage( VODATASERVICE_XSD, "/tables", false,
                                     "table metadata" );
        runStage( tmXsdStage, "TMV" );
        TableMeta[] tmetas = null;
        if ( tmXsdStage.getResult() != XsdStage.Result.NOT_FOUND ) {
            TableMetadataStage tmStage = new TableMetadataStage();
            runStage( tmStage, "TMC" );
            tmetas = tmStage.getTableMetadata();
        }
        XsdStage capXsdStage =
            XsdStage.createXsdStage( CAPABILITIES_XSD, "/capabilities", true,
                                     "capabilities" );
    //  runStage( capXsdStage, "CPV" );
        CapabilityStage capStage = new CapabilityStage();
        runStage( capStage, "CPC" );
        TapCapability tcap = capStage.getCapability();
        XsdStage availXsdStage =
            XsdStage.createXsdStage( AVAILABILITY_XSD, "/availability", false,
                                     "availability" );
        runStage( availXsdStage, "AVV" );
    }

    private void runStage( Stage stage, String code ) {
        reporter_.startSection( code, stage.getDescription() );
        stage.run( serviceUrl_, reporter_ );
        reporter_.endSection();
    }

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

    public static void main( String[] args ) throws Exception {
      //new TapLintExecutable( null, System.out, true )
      //   .validateXml( new URL( args[ 0 ] ), new URL( args[ 1 ] ) );
        String surl = args.length > 0
                    ? args[ 0 ]
                    : "http://dc.zah.uni-heidelberg.de/__system__/tap/run/tap";
        Logger.getLogger( "uk.ac.starlink" ).setLevel( Level.WARNING );
        new TapLintExecutable( new URL( surl ),
                               new Reporter( System.out, 4, true ) )
           .execute();
    }
}
