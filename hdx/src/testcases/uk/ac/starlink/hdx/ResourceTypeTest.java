package uk.ac.starlink.hdx;

import uk.ac.starlink.hdx.extension.SimpleWeather;

import junit.framework.Assert;
// Use JUnit TestCase rather than uk.ac.starlink.util.TestCase, 
// to keep down dependencies (plus we don't need the extra facilities)
import junit.framework.TestCase;

import org.w3c.dom.*;           // for testWeather

public class ResourceTypeTest
        extends TestCase {
    
    public ResourceTypeTest(String name) {
        super(name);
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
}
