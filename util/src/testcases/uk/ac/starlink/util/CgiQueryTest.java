package uk.ac.starlink.util;

import java.net.MalformedURLException;
import java.net.URL;
import junit.framework.TestCase;

public class CgiQueryTest extends TestCase {

    public CgiQueryTest( String name ) {
        super( name );
    }

    public void testQuery() throws MalformedURLException {
        CgiQuery cq = new CgiQuery( "http://jamms.org/query" )
                     .addArgument( "height", 100 )
                     .addArgument( "width", 101.5 )
                     .addArgument( "message", "hello sailor" );
        String cs = "http://jamms.org/query" 
                  + "?height=100" 
                  + "&width=101.5"
                  + "&message=hello%20sailor";
        assertEquals( cs, cq.toString() );
        assertEquals( new URL( cs ), cq.toURL() );
    }

    public void testBroken() {
        boolean worked;
        try {
            new CgiQuery( "not a url" );
            worked = false;
        }
        catch ( IllegalArgumentException e ) {
            worked = true;
        }
        assertTrue( worked );
    }
}
