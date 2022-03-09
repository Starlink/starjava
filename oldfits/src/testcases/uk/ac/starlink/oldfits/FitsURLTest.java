package uk.ac.starlink.oldfits;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;

public class FitsURLTest extends TestCase {

    private String goodExt;
    private String badExt;
    private List allowed;
    private String prefix;

    public FitsURLTest( String name ) {
        super( name );
    }

    public void setUp() {
        goodExt = ".fits";
        badExt = ".not-fits-no-siree-bob";
        allowed = new ArrayList();
        allowed.add( goodExt );
        prefix = "http://donuts.rlyeh.mil/in/a/directory/file";
    }

    public void testValid() throws MalformedURLException {
        String base = prefix + goodExt;
        int hdu = 23;
        URL u0 = new URL( base );
        URL u1 = new URL( base + '#' + hdu );
        URL u2 = new URL( base + '[' + hdu + ']' );
        FitsURL fu0 = FitsURL.parseURL( u0, allowed );
        FitsURL fu1 = FitsURL.parseURL( u1, allowed );
        FitsURL fu2 = FitsURL.parseURL( u2, allowed );
        assertNotNull( fu0 );
        assertNotNull( fu1 );
        assertNotNull( fu2 );
        assertEquals( base, fu0.getContainer().toExternalForm() );
        assertEquals( base, fu1.getContainer().toExternalForm() );
        assertEquals( base, fu2.getContainer().toExternalForm() );
        assertEquals( hdu, fu1.getHDU() );
        assertEquals( hdu, fu2.getHDU() );
        assertTrue( hdu != fu0.getHDU() );
        assertEquals( 0, fu0.getHDU() );
    }

    public void testInvalid() throws MalformedURLException {
        String base = prefix + badExt;
        int hdu = 3;
        URL u0 = new URL( base );
        URL u1 = new URL( base + '#' + hdu );
        URL u2 = new URL( base + '[' + hdu + ']' );
        FitsURL fu0 = FitsURL.parseURL( u0, allowed );
        FitsURL fu1 = FitsURL.parseURL( u1, allowed );
        FitsURL fu2 = FitsURL.parseURL( u2, allowed );
        assertNull( fu0 );
        assertNull( fu1 );
        assertNull( fu2 );
    }

    public void testExtensions() {
        List permitted = FitsConstants.defaultFitsExtensions();
        assertTrue( permitted.contains( ".fits" ) );
        assertTrue( permitted.contains( ".fit" ) );
        assertTrue( ! permitted.contains( "fits" ) );
        assertTrue( ! permitted.contains( "Sir Not-Appearing-In-This-List" ) );
    }
}
