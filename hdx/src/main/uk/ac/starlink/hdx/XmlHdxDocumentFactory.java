package uk.ac.starlink.hdx;

import java.net.URL;
import org.w3c.dom.*;
import javax.xml.parsers.*;

/** 
 * Handles the construction of {@link HdxContainer} objects from XML files.
 *
 * @author Norman Gray (norman@astro.gla.ac.uk)
 * @version $Id$
 */
class XmlHdxDocumentFactory
        implements HdxDocumentFactory {

    private static XmlHdxDocumentFactory instance = null;
    private DocumentBuilder docBuilder;

    private XmlHdxDocumentFactory()
            throws ParserConfigurationException {
        // private constructor
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true); // this is _not_ the default!
        docBuilder = dbf.newDocumentBuilder();
    }

    /** Obtains an instance of the factory. */
    public static HdxDocumentFactory getInstance()
            throws HdxException {
        if (instance == null)
            try {
                instance = new XmlHdxDocumentFactory();
            } catch (ParserConfigurationException ex) {
                throw new HdxException("Can't create parser: " + ex);
            }
        return instance;
    }

    /**
     * Constructs a DOM from a URL pointing to an XML file.
     *
     * @return a new DOM
     * @throws HdxException if this does appear to be an XML file, but
     * there is some parsing problem.
     */
    public Document makeHdx(URL url)
            throws HdxException {
        // We detect which type of file this is by checking that there
        // is a `file extension', and that it is `xml'.  This isn't
        // terribly robust, but should work in the short term.
        String fn = url.getFile();
        int extpos = fn.lastIndexOf('.');
        if (extpos < 0)
            return null;        // no extension
        if (! fn.substring(extpos+1).toLowerCase().equals("xml"))
            return null;        // not .xml

        // The URL appears to be an XML file, and our responsibility.
        // If there's any problem reading the XML file, readXML() will
        // throw an exception
        return readXML(url);
    }

    /** Constructs a DOM from a URL which refers to an XML document.
     * @return the document
     * @throws HdxException if there's any problem reading the URL
     */
    private Document readXML (URL url)
            throws HdxException {
        try {
            Document doc = docBuilder.parse(url.openStream());
            return doc;
        } catch (org.xml.sax.SAXException ex) {
            throw new HdxException("Can't parse XML: " + ex);
        } catch (java.io.IOException ex) {
            throw new HdxException("Can't open stream to URL: " + url);
        }
    }
}
