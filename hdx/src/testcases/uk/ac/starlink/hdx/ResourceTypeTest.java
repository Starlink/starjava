package uk.ac.starlink.hdx;

// import uk.ac.starlink.util.TestCase;
// import junit.framework.Assert;

import org.w3c.dom.*;           // for testWeather

// public class ResourceTypeTest
//         extends TestCase {
public class ResourceTypeTest {
    
    public ResourceTypeTest(String name) {
        //super(name);
    }

    public static void main(String[] args) {
        ResourceTypeTest t = new ResourceTypeTest("hello");
        t.testWeather();
    }

    public void testWeather() {
        try {
            Document weatherDoc = javax.xml.parsers.DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .newDocument();

            Element hdxEl = weatherDoc.createElement("hdx");
            weatherDoc.appendChild(hdxEl);
            Element weatherEl = weatherDoc.createElement("weather");
            hdxEl.appendChild(weatherEl);
            Element cloudEl = weatherDoc.createElement("cloud");
            cloudEl.setAttribute("colour", "black");
            weatherEl.appendChild(cloudEl);

            HdxFactory fact = HdxFactory.getInstance();

            Object weather1 = fact.getObject(weatherEl);
            assertNotNull(weather1);
            assertTrue(weather1 instanceof SimpleWeather);
            assertEquals("black", ((SimpleWeather)weather1).getCloudColour());

            HdxContainer hdx = fact.newHdxContainer(hdxEl);
            Object weather2 = hdx.get(HdxResourceType.match("weather"));
            // or could be HdxResourceType.match(weatherEl)
            assertNotNull(weather2);
            assertTrue(weather2 instanceof SimpleWeather);
            assertEquals("black", ((SimpleWeather)weather2).getCloudColour());
        } catch (Exception ex) {
            fail("Error constructing weather document: " + ex);
        }
    }

    public void assertNotNull(Object o) {
        assertTrue(o != null);
    }
    public void assertEquals(String s1, String s2) {
        assertTrue(s1.equals(s2));
    }
    public void fail(String msg) {
        System.err.println(msg);
        assertTrue(false);
    }
    public void assertTrue(boolean t) {
        if (! t) {
            System.err.println("Test failed");
            System.exit(1);
        }
    }
}
