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
//import java.net.MalformedURLException;
import java.util.List;

import javax.xml.transform.*;

public class XmlNdxTest extends TestCase {
    HdxFactory fact;
    String testDir = "";
    HdxResourceType ndxtype;

    public XmlNdxTest(String name) {
        super(name);
    }

    public void setUp()
            throws HdxException {
        fact = HdxFactory.getInstance();
        // Get the ndx type: this loads the BridgeNdx class, which
        // registers the ndx, data, etc types.  Could alternatively do
        // this with a properties file, as described in
        // HdxResourceType
        ndxtype = BridgeNdx.getHdxType();
    }
    
    public void testNoNS()
            throws HdxException, URISyntaxException {
        URL url = this.getClass().getResource("no-ns.xml");
        System.err.println("testNoNS: url=" + url);
        HdxContainer hdx = fact.newHdxContainer(url);
        List ndxlist = hdx.getList(ndxtype);
        assertEquals(1, ndxlist.size());
        Ndx ndx = (Ndx)hdx.get(ndxtype);
        assertNotNull(ndx);
        NDArray a = ndx.getImage();
        assertNotNull(a);
        assertEquals("file:test1.fits", a.getURL().toString());
        a = ndx.getVariance();
        assertNotNull(a);
        assertEquals("file:test2.fits", a.getURL().toString());
        assertTrue(!ndx.hasQuality());
    }

    public void testMultiNS()
            throws HdxException, URISyntaxException {
        // redefining.xml constantly changes its namespace prefix 
        // -- can we keep up?
        URL url = this.getClass().getResource("redefining.xml");
        HdxContainer hdx = fact.newHdxContainer(url);

        List ndxlist = hdx.getList(ndxtype);
        assertEquals(2, ndxlist.size());

        Ndx ndx = (Ndx)ndxlist.get(0);
        assertNotNull(ndx);
        NDArray a = ndx.getImage();
        assertNotNull(a);
        assertEquals("file:test1.fits", a.getURL().toString());
        a = ndx.getVariance();
        assertNotNull(a);
        assertEquals("file:test2.fits", a.getURL().toString());
        assertTrue(!ndx.hasQuality());
        
        ndx = (Ndx)ndxlist.get(1);
        assertNotNull(ndx);
        a = ndx.getImage();
        assertNotNull(a);
        assertEquals("file:test1.fits", a.getURL().toString());
        assertTrue(!ndx.hasVariance());
        a = ndx.getQuality();
        assertNotNull(a);
        assertEquals("file:test2.fits", a.getURL().toString());
    }

    public void testToSource()
            throws HdxException,
            javax.xml.transform.TransformerConfigurationException,
            javax.xml.transform.TransformerException {
        // Tests the Source obtained from HdxContainer
        URL url = this.getClass().getResource("no-ns.xml");
        HdxContainer hdx = fact.newHdxContainer(url);
        Source source = hdx.getSource();
        assertNotNull(source);
        Transformer trans = TransformerFactory.newInstance().newTransformer();
        java.io.OutputStream os = new java.io.ByteArrayOutputStream();
        trans.transform(source,
                        new javax.xml.transform.stream.StreamResult(os));
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<hdx><ndx><image uri=\"file:test1.fits\"/><variance uri=\"file:test2.fits\"/></ndx></hdx>",
                     os.toString());
    }
    
}
