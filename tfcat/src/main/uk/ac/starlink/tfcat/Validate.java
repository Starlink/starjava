package uk.ac.starlink.tfcat;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Provides a main method which validates TFCat texts.
 *
 * @author   Mark Taylor
 * @since    10 Feb 2022
 */
public class Validate {
    public static void main( String[] args ) throws IOException {
        String usage = "\n   "
                     + Validate.class.getSimpleName() + ":"
                     + " [-help]"
                     + " [-debug]"
                     + " <file>|-"
                     + "\n";
        List<String> argList = new ArrayList<>( Arrays.asList( args ) );
        boolean isDebug = false;
        for ( Iterator<String> argIt = argList.iterator(); argIt.hasNext(); ) {
            String arg = argIt.next();
            if ( arg.startsWith( "-h" ) ) {
                argIt.remove();
                System.out.println( usage );
                return;
            }
            else if ( "-debug".equals( arg ) ) {
                argIt.remove();
                isDebug = true;
            }
        }
        if ( argList.size() != 1 ) {
            System.err.println( usage );
            System.exit( 1 );
        }
        String inFile = argList.get( 0 );
        try ( InputStream in = "-".equals( inFile )
                             ? System.in
                             : new FileInputStream( inFile ) ) {
            BasicReporter reporter =
                new BasicReporter( isDebug, TfcatUtil.getUcdChecker(),
                                   TfcatUtil.getUnitChecker() );
            try {
                JSONObject json = new JSONObject( new JSONTokener( in ) );
                TfcatObject tfcat =
                    Decoders.TFCAT.decode( reporter, json, null );
                if ( tfcat != null ) {
                    tfcat.purgeJson();
                    TfcatUtil.checkBoundingBoxes( reporter, tfcat );
                    TfcatUtil.checkCrs( reporter, tfcat );
                }
                else {
                    reporter.report( "No TFCat object found at top level" );
                }
            }
            catch ( JSONException e ) {
                reporter.report( "Bad JSON: " + e.getMessage() );
            }
            List<String> msgs = reporter.getMessages();
            if ( msgs.size() == 0 ) {
                return;
            }
            else {
                System.out.println( "FAIL (" + msgs.size() + ")" );
                for ( String msg : msgs ) {
                    System.out.println( "    " + msg );
                }
                System.exit( 1 );
            }
        }
    }
}
