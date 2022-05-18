package uk.ac.starlink.tfcat;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import junit.framework.TestCase;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class ParseTest extends TestCase {

    public void testExampleReports() throws Exception {
        assertEquals( 0, getReports( "example1.tfcat" ).length );
        assertEquals( 1, getReports( "jupiter-obs.tfcat" ).length );
        assertEquals( 1, getReports( "doc-example.tfcat" ).length );
    }

    public void testExampleParse() throws Exception {
        TfcatObject tfcat = parseTfcat( "doc-example.tfcat" );
        FeatureCollection fcoll = (FeatureCollection) tfcat;
        Feature[] features = fcoll.getFeatures();
        assertEquals( 2, features.length );
        Feature f0 = features[ 0 ];
        Feature f1 = features[ 1 ];
        assertEquals( "0", f0.getId() );
        assertEquals( "1", f1.getId() );
        Position p0 = ((Geometry.Point) f0.getGeometry()).getShape();
        assertEquals( 1158051858., p0.getTime() );
        assertEquals( 24730., p0.getSpectral() );
        Position[] ls1 = ((Geometry.LineString) f1.getGeometry()).getShape();
        assertEquals( 2, ls1.length );
        assertEquals( new Position( 1158051882, 22770.0 ), ls1[ 1 ] );
    }

    private TfcatObject parseTfcat( String fileName )
            throws IOException, JSONException {
        URL resource = ParseTest.class.getResource( fileName );
        BasicReporter reporter = createReporter();
        try ( InputStream in = resource.openStream() ) {
            JSONObject json = new JSONObject( new JSONTokener( in ) );
            return Decoders.TFCAT.decode( reporter, json );
        }
    }

    private String[] getReports( String fileName )
            throws IOException, JSONException {
        URL resource = ParseTest.class.getResource( fileName );
        BasicReporter reporter = createReporter();
        try ( InputStream in = resource.openStream() ) {
            JSONObject json = new JSONObject( new JSONTokener( in ) );
            TfcatObject tfcat = Decoders.TFCAT.decode( reporter, json );
            tfcat.purgeJson();
            TfcatUtil.checkBoundingBoxes( reporter, tfcat );
        }
        return reporter.getMessages().toArray( new String[ 0 ] );
    }

    private static BasicReporter createReporter() {
        return new BasicReporter( false, TfcatUtil.getUcdChecker(),
                                  TfcatUtil.getUnitChecker() );
    }
}
