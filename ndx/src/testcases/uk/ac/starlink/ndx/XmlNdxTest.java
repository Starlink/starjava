package uk.ac.starlink.ndx;

import uk.ac.starlink.hdx.HdxContainer;
import uk.ac.starlink.hdx.HdxFactory;
import uk.ac.starlink.hdx.HdxResourceType;
import uk.ac.starlink.hdx.HdxException;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.util.TestCase;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import javax.xml.transform.*;

public class XmlNdxTest extends TestCase {
    String testDir = "";
    HdxResourceType ndxtype;

    public XmlNdxTest(String name) {
        super(name);
    }

    public void setUp()
            throws HdxException {
        /*
        * Get the ndx type: this loads the BridgeNdx class, which
        * registers the ndx, data, etc types.  Could alternatively do
        * this with a properties file, as described in
        * HdxResourceType
        */
        ndxtype = BridgeNdx.getHdxResourceType();

        if (false) {
            // Set the hdx logger and its associated handlers to log everything
            java.util.logging.Logger logger
                    = java.util.logging.Logger.getLogger("uk.ac.starlink.hdx");
            logger.setLevel(java.util.logging.Level.ALL);
            for (java.util.logging.Logger tl=logger;
                 tl!=null;
                 tl=tl.getParent()) {
                java.util.logging.Handler[] h = tl.getHandlers();
                for (int i=0; i<h.length; i++)
                    h[i].setLevel(java.util.logging.Level.FINE);
            }
        }
    }
    
    public void testNoNS()
            throws HdxException, URISyntaxException {
        URL url = this.getClass().getResource("no-ns.xml");
        HdxContainer hdx = HdxFactory.getInstance().newHdxContainer(url);
        List ndxlist = hdx.getList(ndxtype);
        assertEquals(1, ndxlist.size());
        Ndx ndx = (Ndx)hdx.get(ndxtype);
        assertNotNull(ndx);
        NDArray a = ndx.getImage();
        assertNotNull(a);
        assertTrue(a.getURL().toString().endsWith("etc/testcases/test1.fits"));
        a = ndx.getVariance();
        assertNotNull(a);
        assertTrue(a.getURL().toString().endsWith("etc/testcases/test2.fits"));
        assertTrue(!ndx.hasQuality());
    }

    public void testMultiNS()
            throws HdxException, URISyntaxException,
            javax.xml.transform.TransformerException {
        /*
         * redefining.xml constantly changes its namespace prefix 
         * -- can we keep up?  This test is a near-duplicate of the
         * testMultiNS test in uk.ac.starlink.hdx's HdxTest.java, but
         * here we query the resulting Hdx using the Ndx interface
         * rather than exclusively through the DOM
         */
        URL url = this.getClass().getResource("redefining.xml");
        HdxContainer hdx = HdxFactory.getInstance().newHdxContainer(url);

        List ndxlist = hdx.getList(ndxtype);
        assertEquals(2, ndxlist.size());

        Ndx ndx = (Ndx)ndxlist.get(0);
        assertNotNull(ndx);
        NDArray a = ndx.getImage();
        assertNotNull(a);
        assertTrue(a.getURL().toString().endsWith("etc/testcases/test1.fits"));
        a = ndx.getVariance();
        assertNotNull(a);
        assertTrue(a.getURL().toString().endsWith("etc/testcases/test2.fits"));
        assertTrue(!ndx.hasQuality());
        
        ndx = (Ndx)ndxlist.get(1);
        assertNotNull(ndx);
        a = ndx.getImage();
        assertNotNull(a);
        assertTrue(a.getURL().toString().endsWith("etc/testcases/test1.fits"));
        assertTrue(!ndx.hasVariance());
        a = ndx.getQuality();
        assertNotNull(a);
        assertTrue(a.getURL().toString().endsWith("etc/testcases/test2.fits"));
    }
}
