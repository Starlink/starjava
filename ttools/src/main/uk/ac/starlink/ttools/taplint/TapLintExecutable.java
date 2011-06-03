package uk.ac.starlink.ttools.taplint;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.task.Executable;

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

    public TapLintExecutable( URL serviceUrl, Reporter reporter ) {
        serviceUrl_ = serviceUrl;
        reporter_ = reporter;
    }

    public void execute() {
        XsdStage tmXsdStage = new TableMetadataXsdStage();
        runStage( tmXsdStage, "TMV" );
        if ( tmXsdStage.getResult() != XsdStage.Result.NOT_FOUND ) {
            runStage( new TableMetadataStage(), "TMC" );
        }
    }

    private void runStage( Stage stage, String code ) {
        reporter_.startSection( code, stage.getDescription() );
        stage.run( serviceUrl_, reporter_ );
        reporter_.endSection();
    }

    public static void main( String[] args ) throws Exception {
      //new TapLintExecutable( null, System.out, true )
      //   .validateXml( new URL( args[ 0 ] ), new URL( args[ 1 ] ) );
        String surl = args.length > 0
                    ? args[ 0 ]
                    : "http://dc.zah.uni-heidelberg.de/__system__/tap/run/tap";
        Logger.getLogger( "uk.ac.starlink" ).setLevel( Level.WARNING );
        new TapLintExecutable( new URL( surl ), new Reporter( System.out, 4 ) )
           .execute();
    }
}
