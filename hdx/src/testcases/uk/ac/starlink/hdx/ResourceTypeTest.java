package uk.ac.starlink.hdx;

import uk.ac.starlink.hdx.extension.SimpleWeather;

import uk.ac.starlink.util.TestCase;

import org.w3c.dom.*;           // for testWeather

import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;


public class ResourceTypeTest
        extends TestCase {
    
    public ResourceTypeTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        try {
            ResourceTypeTest t = new ResourceTypeTest("hello");
            t.testFacade();
        } catch (Exception e) {
            System.err.println("Test threw exception: " + e);
        }
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
            SimpleWeather sw = (SimpleWeather)weather2;
            assertEquals("black", sw.getCloudColour());

            // Check resulting DOM
            Element hdxdom = hdx.getDOM(null);
            assertDOMEquals("<hdx><weather><cloud colour='black'/></weather></hdx>",
                            hdxdom);

            // Check serialisation of DOM -- does this really check
            // Sun's DOMSource?
            assertEquals("<hdx><weather><cloud colour=\"black\"/></weather></hdx>",
                         serializeNode(hdxdom));
        } catch (Exception ex) {
            fail("Error constructing weather document: " + ex);
        }
    }

    public void testFacade() 
            throws Exception {
        SimpleWeather sw = new SimpleWeather("golden", null);
        HdxFactory fact = HdxFactory.getInstance();

        // Basic case: making a new HdxContainer from a Facade
        HdxContainer hdx = fact.newHdxContainer(sw.getHdxFacade());
        Node hdxdom = hdx.getDOM(null);
        HdxTest.assertDOMSane(hdxdom);
        assertDOMEquals
                ("<hdx><weather><cloud colour='golden'/></weather></hdx>",
                 hdxdom);

        // Most of the following tests are redundant after
        // assertDOMSane and assertDOMEquals -- they were initially
        // used to debug the problems which assertDOMSane now checks
        // quite thoroughly.  There's no harm in keeping them in,
        // though.
        Node kid = hdxdom.getFirstChild();
        Node grandkid = kid.getFirstChild();
        
        assertEquals(Node.ELEMENT_NODE, hdxdom.getNodeType());
        assertNull(hdxdom.getPreviousSibling());
        assertNull(hdxdom.getNextSibling());
        assertTrue(hdxdom.hasChildNodes());

        NodeList kids = hdxdom.getChildNodes();
        assertEquals(1, kids.getLength());
        assertNull(hdxdom.getNodeValue());

        assertSame(hdxdom.getLastChild(), kid);
        assertEquals(Node.ELEMENT_NODE, kid.getNodeType());
        assertEquals("weather", kid.getNodeName());
        assertNull(kid.getNamespaceURI());
        assertNull(kid.getPrefix());
        assertSame(kid, kids.item(0));
        assertNull(kid.getNextSibling());
        assertNull(kid.getPreviousSibling());
        assertSame(hdxdom, kid.getParentNode());
        assertTrue(kid.hasChildNodes());

        assertSame(kid.getLastChild(), grandkid);
        assertEquals(Node.ELEMENT_NODE, grandkid.getNodeType());
        assertEquals("cloud", grandkid.getNodeName());
        assertNull(grandkid.getNamespaceURI());
        assertNull(grandkid.getPrefix());
        assertNull(grandkid.getNextSibling());
        assertNull(grandkid.getPreviousSibling());
        assertNull(grandkid.getNextSibling());
        assertNull(grandkid.getPreviousSibling());
        assertSame(kid, grandkid.getParentNode());

        // This is another test which is vulnerable to the Xalan bug
        if (XmlParserFeatureset.getInstance().xalanHasEndEndDocumentBug())
            return;

        // Check serialisation -- this is another way of checking the
        // sanity of the DOM which the Facade produces, since the
        // transformation has to traverse the DOM using the Node methods.
        javax.xml.transform.Transformer trans
                = javax.xml.transform.TransformerFactory
                .newInstance()
                .newTransformer();
        SummarisingContentHandler sch = new SummarisingContentHandler();
        javax.xml.transform.sax.SAXResult sr
                = new javax.xml.transform.sax.SAXResult(sch);
        trans.transform(new javax.xml.transform.dom.DOMSource(hdxdom), sr);
        assertEquals
                ("[<:hdx/hdx><:weather/weather><:cloud/cloud colour=golden></cloud></weather></hdx>]",
                 sch.getSummary());

        // Same, but with a non-null URI
        sw = new SimpleWeather("golden", "http://example.edu/t1.fits");
        hdx = fact.newHdxContainer(sw.getHdxFacade());
        assertDOMEquals("<hdx><weather><cloud colour='golden'/><data uri='http://example.edu/t1.fits'/></weather></hdx>",
                        hdx.getDOM(null));

        // Same, but this time we relativize the resulting DOM
        assertDOMEquals("<hdx><weather><cloud colour='golden'/><data uri='t1.fits'/></weather></hdx>",
                        hdx.getDOM(new java.net.URI("http://example.edu/")));
    }

    static private String serializeNode (Node n) {
        String ret;
        try {
            java.io.StringWriter sw = new java.io.StringWriter();
            javax.xml.transform.Transformer trans
                    = javax.xml.transform.TransformerFactory
                    .newInstance()
                    .newTransformer();
            trans.setOutputProperty
                    (javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION,
                     "yes");
            trans.transform
                    (new javax.xml.transform.dom.DOMSource(n),
                     new javax.xml.transform.stream.StreamResult(sw));
            ret = sw.toString();
        } catch (javax.xml.transform.TransformerException ex) {
            ret = "";
        }
        return ret;
    }

    /**
     * Summarises the list of SAX events reported to it.
     */
    static private class SummarisingContentHandler 
            implements org.xml.sax.ContentHandler {
        private java.util.SortedSet attset;
        private StringBuffer sb;
        
        public SummarisingContentHandler() {
            if (attset == null)
                attset = new java.util.TreeSet();
            sb = new StringBuffer();
        }
        public void setDocumentLocator (Locator locator) {
        }
        public void startDocument ()
                throws SAXException {
            sb.append('[');
        }
        public void endDocument()
                throws SAXException {
            sb.append(']');
        }
        public void startPrefixMapping (String prefix, String uri)
                throws SAXException {
            sb.append("{+").append(prefix).append(':').append(uri).append('}');
        }
        public void endPrefixMapping (String prefix)
                throws SAXException {
            sb.append("{-").append(prefix).append('}');
        }
        public void startElement (String namespaceURI, String localName,
                                  String qName, Attributes atts)
                throws SAXException {
            sb.append('<')
                    .append(namespaceURI == null ? "" : namespaceURI)
                    .append(':')
                    .append(localName)
                    .append('/')
                    .append(qName);
            attset.clear();
            for (int i=0; i<atts.getLength(); i++)
                attset.add(atts.getQName(i)+'='+atts.getValue(i));
            for (java.util.Iterator i=attset.iterator(); i.hasNext(); ) 
                sb.append(' ').append((String)i.next());
            sb.append('>');
        }
        public void endElement (String namespaceURI, String localName,
                                String qName)
                throws SAXException {
            sb.append("</").append(qName).append('>');
        }
        public void characters (char ch[], int start, int length)
                throws SAXException {
            sb.append(new String(ch, start, length));
        }
        public void ignorableWhitespace (char ch[], int start, int length)
                throws SAXException {
            sb.append('-');
        }
        public void processingInstruction (String target, String data)
                throws SAXException {
            sb.append("<?")
                    .append(target)
                    .append(' ')
                    .append(data)
                    .append('>');
        }
        public void skippedEntity (String name)
                throws SAXException {
            sb.append('&');
        }
        public String getSummary() {
            return sb.toString();
        }
    }

    static private class ReportingContentHandler 
            implements org.xml.sax.ContentHandler {

        private Locator parserLocator;
        public ReportingContentHandler() {
            System.err.println("Created ReportingContentHandler");
        }
        public void setDocumentLocator (Locator locator) {
            parserLocator = locator;
        }
        public void startDocument ()
                throws SAXException {
            System.err.println ("StartDocument - " + printLocation());
        }
        public void endDocument()
                throws SAXException {
            System.err.println("EndDocument - " + printLocation());
        }
        public void startPrefixMapping (String prefix, String uri)
                throws SAXException {
            System.err.println("PrefixMapping(" + prefix
                               + "-->" + uri + '-' + printLocation());
        }
        public void endPrefixMapping (String prefix)
                throws SAXException {
            System.err.println("EndPrefixMapping(" + prefix
                               + '-' + printLocation());
        }
        public void startElement (String namespaceURI, String localName,
                                  String qName, Attributes atts)
                throws SAXException {
            System.err.println("StartElement("
                               + namespaceURI + ','
                               + localName + ','
                               + qName + " +");
            for (int i=0; i<atts.getLength(); i++)
                System.err.println("    " + atts.getQName(i)
                                   + '=' + atts.getValue(i));
            System.err.println("    ) - " + printLocation());
        }
        public void endElement (String namespaceURI, String localName,
                                String qName)
                throws SAXException {
            System.err.println("EndElement("
                               + namespaceURI + ','
                               + localName + ','
                               + qName + " - " + printLocation());
        }
        public void characters (char ch[], int start, int length)
                throws SAXException {
            String s = new String(ch, start, length);
            System.err.println("characters(" + s + ") - " + printLocation());
        }
        public void ignorableWhitespace (char ch[], int start, int length)
                throws SAXException {
            System.err.println("IgnorableWhitespace - " + printLocation());
        }
        public void processingInstruction (String target, String data)
                throws SAXException {
            System.err.println("ProcessingInstruction("
                               + target + ':' + data
                               + ") - " + printLocation());
        }
        public void skippedEntity (String name)
                throws SAXException {
            System.err.println("SkippedEntity(" + name 
                               + ") - " + printLocation());
        }
        private String printLocation() {
            if (parserLocator == null)
                return "?";
            else
                return parserLocator.getSystemId() + ":"
                        + parserLocator.getLineNumber();
        }
    }
}
