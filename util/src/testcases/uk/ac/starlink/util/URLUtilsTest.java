package uk.ac.starlink.util;

import java.io.File;
import java.net.URL;
import java.net.URI;
import java.net.MalformedURLException;

/**
 * Tests for the URLUtils class.
 *
 * @author Norman Gray (Starlink)
 */
public class URLUtilsTest extends junit.framework.TestCase {

    public URLUtilsTest(String name) {
        super(name);
    }

    public void testMakeURL() {
        String[] okurls = {
            "http://www.starlink.ac.uk",
            "file:/etc/blarfle",
            "file://localhost/etc/blarfle",
        };
        for (int i=0; i<okurls.length; i++) {
            URL url = URLUtils.makeURL(okurls[i]);
            assertEquals(okurls[i], url.toString());
        }

        String[] okpairs = {
            // first should turn into second
            "/etc/blarfle",        "file:/etc/blarfle", // add file: scheme
            "file:///etc/blarfle", "file:/etc/blarfle", // remove empty auth.
        };
        for (int i=0; i<okpairs.length; i+=2) {
            assertEquals(okpairs[i+1],
                         URLUtils.makeURL(okpairs[i]).toString());
        }

        // Context defaults correctly?
        File me = new File("wibble");
        assertEquals("file:" + me.getAbsolutePath(),
                     URLUtils.makeURL("wibble").toString());
        
        // Following should fail
        String[] badurls = {
            // XXX I would have expected these to fail, but they don't
            // Ought they to? [NG]

            // "invalid:/path",    // invalid scheme
            // "http://example.com/bad path", // space in URL
        };
        for (int i=0; i<badurls.length; i++) {
            assertNull(URLUtils.makeURL(badurls[i]));
        }

        assertEquals( "file:foo/bar",
                      URLUtils.makeURL("file:foo/bar").toString());
    }

    public static void testUrlToUri() {
        String[] goodurls = {
            "http://www.starlink.ac.uk",
            "http://norman@www.starlink.ac.uk",
            "http://norman:password@www.starlink.ac.uk",
            "http://x.com?query",
            "http://x.com/#frag",
            "http://x.com?query#frag",
            "file:/etc/blarfle",
            "file://localhost/etc/blarfle",
            "file:///etc/blarfle",
        };
        try {
            for (int i=0; i<goodurls.length; i++) {
                URL url = new URL(goodurls[i]);
                URI uri = URLUtils.urlToUri(url);
                assertEquals(goodurls[i], uri.toString());
            }
        } catch (MalformedURLException e) {
            fail("Unexpected exception " + e + " from good URL");
        }

        java.util.List escapedurls = new java.util.LinkedList();
        // in each case, the first should turn into the second
        escapedurls.add("http://x.com/bad path");
        escapedurls.add("http://x.com/bad%20path");
        
        escapedurls.add("http://x.com/odd%path");
        escapedurls.add("http://x.com/odd%25path");

        if (File.separatorChar == '\\') {
            // We appear to be on a Windows filesystem.  Check
            // that drive letters are escaped properly.
            escapedurls.add("file:c:\\autoexec.bat");
            escapedurls.add("file:c|/autoexec.bat"); // XXX what?
        }

        try {
            for (java.util.Iterator li = escapedurls.iterator();
                 li.hasNext(); ) {
                URL url = new URL((String)li.next());
                URI uri = URLUtils.urlToUri(url);
                assertEquals((String)li.next(), uri.toString());
            }
        } catch (MalformedURLException e) {
            fail("Unexpected exception " + e + " from good URL");
        }

        // The following are good URLs (in the sense that the URL
        // constructor accepts them), but bad URIs.  Since this method
        // only accepts URL arguments, it doesn't have to worry about
        // strings that are so malformed they can't be formed into
        // URLs.
        String[] badurls = {
            "http:relative/path", // scheme + relative path
            "http:registry$authority/path", // non-server-based auth
        };
        for (int i=0; i<badurls.length; i++) {
            URL url = null;
            try {
                url = new URL(badurls[i]);
            } catch (MalformedURLException e) {
                fail("Unexpected exception (" + e 
                     + ") with bad URL " + badurls[i]);
            }
            try {
                URI uri = URLUtils.urlToUri(url);
                fail("Bad URL " + url + " did not provoke an exception: "
                     + uri);
            } catch (MalformedURLException e ) {
                // correct -- do nothing
            } catch (Exception e) {
                fail("Unexpected exception " + e
                     + " from bad URL " + badurls[i]);
            }
        }
    }

    public void testFileUrl() throws MalformedURLException {
        assertEquals( new URL( "file://localhost/etc/motd" ),
                  URLUtils.fixURL( new URL( "file:/etc/motd" ) ) );
        assertEquals( new URL( "file://localhost/etc/motd" ),
                  URLUtils.fixURL( new URL( "file://localhost/etc/motd" ) ) );
        assertEquals( new URL( "file://localhost/etc/motd" ),
                      URLUtils.makeFileURL( new File( "/etc/motd" ) ) );
        assertEquals( new URL( "ftp://rtfm.mit.edu/pub/" ),
                      URLUtils.fixURL( new URL( "ftp://rtfm.mit.edu/pub/" ) ) );
    }

    public void testUrlToFile() {
        assertEquals( new File( "/etc/motd" ),
                      URLUtils.urlToFile( "file://localhost/etc/motd" ) );
        assertEquals( new File( "/data/table/x++m.xml" ),
                      URLUtils.urlToFile( "file:///data/table/x++m.xml" ) );
        assertEquals( new File( "/data/table/a b.txt" ),
                      URLUtils.urlToFile( "file:///data/table/a%20b.txt" ) );
    }

    public void testRelative() {
        assertEquals( "http://example.com/foo/bar",
                      URLUtils.makeURL( "http://example.com/foo/", "bar" )
                              .toString() );
        assertEquals( "file:/src/lib",
                      URLUtils.makeURL( "file:/src/etc", "file:lib" )
                              .toString() );
        assertEquals( "https://foo.com/bar",
                      URLUtils.makeURL( null, "https://foo.com/bar" )
                              .toString() );
        assertEquals( "file:/etc/motd",
                      URLUtils.makeURL( null, "/etc/motd" )
                              .toString() );
    }

    public void testCoding() {
        String pre = "1 + 1 = 2";
        String post = "1+%2b+1+%3d+2";
        assertEquals( post, URLUtils.urlEncode( pre ).toLowerCase() );
        assertEquals( pre, URLUtils.urlDecode( post ) );
        assertEquals( "abc", URLUtils.urlEncode( "abc" ) );
        assertEquals( "def", URLUtils.urlDecode( "def" ) );

        assertEquals( "1%20+%201%20=%202",
                      URLUtils.percentEncodeIllegalCharacters( pre ) );
        assertEquals( "%22II/246/out%22",
                      URLUtils
                     .percentEncodeIllegalCharacters( "\"II/246/out\"" ) );
        assertEquals( "-*-%0A-%EA%99%AE-",
                      URLUtils
                     .percentEncodeIllegalCharacters( "-*-\n-\ua66e-" ) );
    }
}
