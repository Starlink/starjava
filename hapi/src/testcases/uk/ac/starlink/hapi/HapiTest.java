package uk.ac.starlink.hapi;

import junit.framework.TestCase;

public class HapiTest extends TestCase {

    public void testVersion() {
        assertEquals( "id",
                      HapiVersion.fromText( null ).getDatasetRequestParam() );
        assertEquals( "stop",
                      HapiVersion.fromText( "3.1" ).getStopRequestParam() );
        assertEquals( "time.max",
                      HapiVersion.fromText( "2.0" ).getStopRequestParam() );
    }
}
