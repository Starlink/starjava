package uk.ac.starlink.hdx;

import uk.ac.starlink.util.TestCase;
import junit.framework.Assert;

import java.net.URI;
import java.net.URL;


public class HdxTest
        extends TestCase {

    String updir;

    public HdxTest(String name) {
        super(name);
    }
    
    public void testURIResolution() {
        assertEquals("file:/path-to-parent/hdx/HdxTest.java",
                     resolveURI("HdxTest.java"));
        assertEquals("file:/path-to-parent/hdx/HdxTest.java#frag",
                     resolveURI("HdxTest.java#frag"));
        assertEquals("file:/path-to-parent/hdx/HdxTest.java",
                     resolveURI("file:HdxTest.java"));
        assertEquals("file:/path-to-parent/hdx/HdxTest.java#frag",
                     resolveURI("file:HdxTest.java#frag"));
        assertEquals("file:/path-to-parent/hdx/HdxTest.java",
                     resolveURI("FILE:HdxTest.java"));
        assertEquals("file:/path-to-parent/hdx/HdxTest.java",
                     resolveURI("file:../hdx/HdxTest.java"));
        java.io.File fileHdxTest = new java.io.File("HdxTest.java");
        String absfileHdxTest = fileHdxTest.getAbsolutePath();
        assertEquals("file:/path-to-parent/hdx/HdxTest.java",
                     resolveURI(absfileHdxTest));
        assertEquals("file:/path-to-parent/hdx/HdxTest.java",
                     resolveURI("file:/path-to-parent/hdx/HdxTest.java"));
        assertEquals("file:/path-to-parent/MasterFactory.java",
                     resolveURI("../MasterFactory.java"));
        assertEquals("file:/path-to-parent/MasterFactory.java",
                     resolveURI("file:../MasterFactory.java"));
        assertEquals("http://www.starlink.ac.uk/index.html",
                     resolveURI("http://www.starlink.ac.uk/index.html"));
        assertEquals("http:/index.html",
                     resolveURI("http:/index.html"));

    }

    private String resolveURI(String uriString) {
        URI uri;
        try {
            uri = new URI(uriString);

            URL url = HdxFactory.getInstance().fullyResolveURI(uri);
            if (url.getProtocol().equals("file")) {
                return removeCurrdir(url.toString());
            } else {
                return url.toString();
            }
        } catch (HdxException ex) {
            return "(HdxException: " + ex + ")";
        } catch (java.net.URISyntaxException ex) {
            return "(URI exception: " + ex + ")";
        }
    }

    private String removeCurrdir(String s) {
        if (updir == null) {
            updir = new java.io.File("").getAbsolutePath();
            int idx = updir.lastIndexOf('/');
            if (idx >= 0)
                updir = updir.substring(0, idx);
            //System.err.println("Updir=" + updir);
        }

        StringBuffer sb = new StringBuffer(s);
        int updiridx = sb.indexOf(updir);
        if (updiridx >= 0) {
            sb.replace(updiridx, updiridx+updir.length(),
                       "/path-to-parent");
        }
        return sb.toString();
    }
}
