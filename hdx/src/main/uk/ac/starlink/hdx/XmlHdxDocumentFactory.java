package uk.ac.starlink.hdx;

import java.net.URL;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.util.logging.Logger;

/** 
 * Handles the construction of {@link HdxContainer} objects from XML files.
 *
 * @author Norman Gray (norman@astro.gla.ac.uk)
 * @version $Id$
 */
class XmlHdxDocumentFactory
        implements HdxDocumentFactory {

    private static Logger logger = Logger.getLogger("uk.ac.starlink.hdx");

    private static XmlHdxDocumentFactory instance;
    static {
        try {
            instance = new XmlHdxDocumentFactory();
        
            // Immediately register it as a HdxDocumentFactory
            HdxFactory.registerHdxDocumentFactory(getInstance());
        } catch (HdxException e) {
            logger.severe("Failed to create XmlHdxDocumentFactory: " + e);
        }
    }

    private DocumentBuilder docBuilder;

    /** 
     * Constructor is protected on purpose.
     */
    private XmlHdxDocumentFactory()
            throws HdxException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true); // this is _not_ the default!
            docBuilder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            throw new HdxException("Can't create parser: " + ex);
        }
    }

    /**
     * Obtains an instance of the factory.
     *
     * @return an instance of the factory
     */
    public static HdxDocumentFactory getInstance() {
        return instance;
    }

    /**
     * Constructs a DOM from a URL pointing to an XML file.
     *
     * @param url a URL which we will handle if it points to an XML file
     * @return a new DOM
     * @throws HdxException if this does appear to be an XML file, but
     * there is some parsing problem
     */
    public Document makeHdxDocument(URL url)
            throws HdxException {
        // We detect whether or not this is an XML file by testing if
        // the last three characters of the URL are "xml".  This isn't
        // terribly robust, but should work in the short term.  We
        // don't look for ".xml", firstly because it barely makes
        // anything more robust, and secondly because it allows things like
        // ".starxml" should we be daft enough to use that, and more
        // importantly because it allows us to have XML files as
        // Java resources, with names ending in, say, "-xml".
        //
        // Would it be better to try parsing it as XML and see if it
        // breaks?  Probably not: I don't wish to think of what a SAX
        // parser would make of an NDF.
        String fn = url.getFile();
        if (fn.length()>3 &&
            fn.substring(fn.length()-3).toLowerCase().equals("xml"))
            // URL appears to be an XML file, and our responsibility.
            // If there's any problem reading the XML file, readXML() will
            // throw an exception
            return readXML(url);
        else
            return null;
    }

    public javax.xml.transform.Source makeHdxSource(URL url)
            throws HdxException {
        Document d = makeHdxDocument(url);
        if (d == null)
            return null;
        else
            return new javax.xml.transform.dom.DOMSource(d);
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
