package uk.ac.starlink.hdx;

import org.dom4j.*;
import org.dom4j.io.SAXReader;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.io.InputStream;
import uk.ac.starlink.hdx.array.NDArray;

/** Factory returns HDX objects.  Package private.
 */
public class HdxContainerFactory {

    /** Factory instance.  Lazily created in getInstance() */
    private static HdxContainerFactory instance = null;

    private static Properties hdxprops;
    /** Static initialiser fills in the hdxprops property list. */
    static {
        Properties sysprops = System.getProperties();

        String fn = System.getProperty("user.dir") + "/hdx.prop";
        try {
            java.io.FileInputStream propfile = new java.io.FileInputStream(fn);
            hdxprops = new Properties(sysprops);
            hdxprops.load(propfile);
        } catch (java.io.FileNotFoundException e) {
            // Not a problem -- fix silently
            hdxprops = sysprops;
        } catch (java.io.IOException e) {
            System.err.println("IOException reading file " + fn
                               + ": " + e);
            hdxprops = sysprops;
        } catch (java.lang.SecurityException e) {
            System.err.println("Can't open file " + fn
                               + ".  Security exception: " + e);
            hdxprops = sysprops;
        }
    }

    /** XSLT transform engine. */
    private static javax.xml.transform.Transformer transformer;

    /** Constructs an HdxContainerFactory.  This is a private constructor, to
     * prevent more than one instance existing.
     */
    private HdxContainerFactory() {
    }

    /** Returns an instance of the factory. */
    static public HdxContainerFactory getInstance() {
        if (instance == null) {
            // lazily create singleton instance
            instance = new HdxContainerFactory();
        }
        return instance;
    }

    /** Processes a single URL to extract a single HDX.  At present,
     * we detect which type of file this is by examining the `file
     * extension'.  This isn't terribly robust, but should work in the
     * short term.
     * @throws HdxException if we can't read the URL, for whatever reason.
     */
    public HdxContainer readHdx (URL url)
            throws HdxException {

        Document hdxDoc;
        String fn = url.getFile();

        int extpos = fn.lastIndexOf('.');
        if (extpos < 0)
            throw new HdxException
                ("HdxContainerFactory.readHdx(URL): can't find extension in <"
                 + url + ">");

        String ext = fn.substring(extpos+1).toLowerCase();
        if (ext.equals("xml")) {
            hdxDoc = readXML(url);
            if ( hdxDoc == null ) {
                throw new HdxException( "no such resource " + url );
            }
        } else {
            throw new HdxException
                ("HdxContainerFactory.readHdx(URL): can't read URLs of type "
                 + ext);
        }

        return readHdx(hdxDoc);
    }

    /** Obtains an HDX from a org.w3c.dom.Element.
     * @throws HdxException if there is no HDX structure in the DOM.
     */
    public HdxContainer readHdx(org.w3c.dom.Element w3cElement)
            throws HdxException {
        return readHdx(documentFromW3CElement(w3cElement));
    }

    /** Obtains an HDX from a dom4j DOM.
     * @throws HdxException if there is no HDX structure in the DOM
     */
    HdxContainer readHdx(org.dom4j.Document hdxDoc)
            throws HdxException {
        // XXX We will need to be specific here about what we do and
        // don't match.
        //
        // XXX The HDX element should be the root element:
        // do we want to insist on that?
        //
        // XXX We don't make a fuss about namespaces, since the
        // normalization process below should take care of all that
        // for us.  Should we consider normalizing every time, without
        // the test?
        String hdxxpath = "/" + HdxResourceType.HDX.xmlName();
        List hdxlist = hdxDoc.selectNodes(hdxxpath);
        if (hdxlist.isEmpty()) {
            // We haven't found anything immediately, so try
            // normalising the DOM
            Document normalDoc = normalizeHdx(hdxDoc);
            List newHdxlist = normalDoc.selectNodes(hdxxpath);
            if (newHdxlist.isEmpty())
                // Still nothing
                throw new HdxException ("Can't find any HDX structure in DOM");

            hdxlist = newHdxlist;
        }

        return (HdxContainer) new HdxDomImpl ((Element) hdxlist.get(0));
    }

    /** Obtains an HDX from a {@link java.io.Reader Reader} returning XML.
     * @throws HdxException if there is no HDX structure in the DOM.
     */
    public HdxContainer readHdx(java.io.Reader reader)
            throws HdxException {
        Document hdxDoc = readXML(reader);
        return readHdx(hdxDoc);
    }

    /** Constructs a DOM from a URL which refers to an XML document.
     * @return the document, or null if the stream can't be read
     */
    private static Document readXML (URL url) {
        Document xmldoc = null;
        try {
            xmldoc = readXML
                (new java.io.BufferedReader
                 (new java.io.InputStreamReader(url.openStream())));
        } catch (java.io.IOException ex) {
            System.err.println ("HdxContainerFactory.readXML(URL): IO error ("
                                + ex + ")");
        }
        return xmldoc;
    }

    /** Constructs a DOM from a reader which returns an XML document.
     * @return the document, or null if the stream can't be read
     */
    private static Document readXML (java.io.Reader inputReader) {
        SAXReader sr = new SAXReader (new org.dom4j.DocumentFactory());
        sr.setValidation(false);
        sr.setIncludeExternalDTDDeclarations(false);
        Document doc;
        try {
            doc = sr.read(inputReader);
        } catch (org.dom4j.DocumentException ex) {
            System.err.println
                ("HdxContainerFactory.readXML(Reader): can't read stream ("
                 + ex + ")");
            doc = null;
        }
        return doc;
    }

    /** Normalises the DOM tree, exposing only elements in the NDX
     * namespace.
     *
     * <p>The transformation is done using XSLT.  The XSLT script is
     * specified in the property <code>ndx.normalizer</code>, and
     * should point to the file
     * <code>.../support/normalise-ndx.xslt</code> relative to the
     * distribution (at present).  This file can be specified in a
     * property file called <code>ndx.prop</code> in the current
     * directory, or specified as a system property on the java
     * command line.
     *
     * <p>This method, along with its companions {@link
     * #initialiseTransformer} and {@link #transformNdx}, is not
     * necessarily a final solution to the problem of navigating
     * the DOM tree in the presence of namespaces and the `virtual'
     * elements represented by <code>ndx:name</code> and friends.  Its big
     * advantage is that it's clear what's happening -- the document
     * is being normalised to a view where only the ndx namespace
     * elements are present, which means that navigating through it
     * afterwards is programmatically and conceptually simple -- but
     * the disadvantages are (a) it's rather slow to create the
     * transformer; (b) we abandon the parts of the input document
     * which aren't in the namespace, so we can't round-trip
     * documents through this process; (c) there are a couple of
     * surprises to do with default namespaces -- specifically, note
     * that default namespaces do <em>not</em> apply to attributes:
     * <pre>
     * &lt;x xmlns="http://example.org/NS">
     * &lt;foo bar="hello">
     * &lt;/x>
     * </pre>
     * Element <code>foo</code> is in the given namespace, but
     * attribute <code>bar</code> isn't.
     *
     * <p>None of these are killing problems.  (a) we can cope with,
     * and since the transformer is a static object, the start up
     * cost could be amortized quite effectively; (b) isn't a problem
     * since this is only intended to be used for reading XML
     * specifications, so we don't need to round-trip documents
     * carrying other elements unknown to this system.  (c) is an
     * unavoidable consequence of the XML Namespace definition, which
     * we simply have to be slightly careful of, and warn folk not to
     * start being too clever.
     *
     * <p>The alternative is to deal with the namespace trickery
     * up-front, by making the methods which root around the tree very much
     * cleverer.  The problem with that is that I believe we'd have
     * to add similar cleverness in a variety of places, which is
     * errorprone and potentially confusing.
     *
     * @return the normalised DOM tree.
     * @throws HdxException if there is a problem locating or using
     * the XSLT transformation script.
     */
    private Document normalizeHdx (Document dom)
            throws HdxException {
        // The location of the XSLT script which the transformer uses.

        // PWD: remove
        // String normalizeHdxXslt = hdxprops.getProperty("hdx.normalizer");
        // and get the resource from a placed fixed with respect to this class.
        InputStream normalizeHdxXslt =
            getClass().getResourceAsStream( "support/normalize-hdx.xslt" );

        if (normalizeHdxXslt == null)
            throw new HdxException("No value for property hdx.normalizer");

        try {
            System.err.println("normalizeHdx: transforming");
            if (transformer == null)
                initialiseTransformer( normalizeHdxXslt );

            return transformNdx (dom);
        } catch (javax.xml.transform.TransformerException e) {
            throw new HdxException("XSLT error: " + e);
        }
    }

    /** Creates a new XSLT transformer.
     *
     * <p>Constructs the transformer lazily.  Idempotent.
     * @throws HdxException if there is a problem initialising the
     * transformer.
     */
    private static void initialiseTransformer (InputStream stylesheet)
            throws HdxException {

        if (transformer != null)
            return;             // let us be called repeatedly

        try {

            javax.xml.transform.stream.StreamSource xsltScript
                = new javax.xml.transform.stream.StreamSource(stylesheet);
            javax.xml.transform.TransformerFactory factory
                = javax.xml.transform.TransformerFactory.newInstance();
            transformer = factory.newTransformer(xsltScript);

        } catch (javax.xml.transform.TransformerConfigurationException e) {
            throw new HdxException ("Error initialising Transformer: " + e);
        }
    }

    /** Performs the XSLT transformation on the given DOM tree.
     * @return the transformed DOM tree.
     * @throws javax.xml.transform.TransformerException if the
     * transformer fails.
     * @see javax.xml.transform.Transformer
     */
    private static Document transformNdx (Document doc)
            throws javax.xml.transform.TransformerException  {

        org.dom4j.io.DocumentSource source
            = new org.dom4j.io.DocumentSource(doc);
        org.dom4j.io.DocumentResult result
            = new org.dom4j.io.DocumentResult
                (new org.dom4j.io.SAXContentHandler(new DocumentFactory()));

        transformer.transform(source, result);
        return result.getDocument();
    }

    /** debugging method */
    private static void serializeToXML(Document doc,
                                       java.io.OutputStream out) {
        try {
            org.dom4j.io.XMLWriter writer = new org.dom4j.io.XMLWriter(out);
            writer.write(doc);
            writer.flush();
        } catch (java.io.IOException e) {
            System.err.println ("Error serialising XML: " + e);
        }
    }

    /** Converts an org.w3c.dom.Element to a dom4j Document. */
    private static Document documentFromW3CElement (org.w3c.dom.Element el) {
        Document newDoc = DocumentHelper.createDocument();
        newDoc.setRootElement(elementFromW3CElement(el));
        return newDoc;
    }

    /** Converts an org.w3c.dom.Element to a dom4j Element. */
    private static Element elementFromW3CElement (org.w3c.dom.Element el) {
        Element newElem;
        String ns = el.getNamespaceURI();
        if (ns == null)
            newElem = DocumentHelper.createElement(el.getNodeName());
        else
            newElem = DocumentHelper.createElement
                (new QName(el.getLocalName(),
                           new Namespace(el.getPrefix(),
                                         el.getNamespaceURI())));
        org.w3c.dom.Node node;
        org.w3c.dom.NamedNodeMap atts = el.getAttributes();
        for (int i=0; i<atts.getLength(); i++) {
            node = atts.item(i);
            ns = node.getNamespaceURI();
            if (ns == null)
                newElem.addAttribute(node.getNodeName(), node.getNodeValue());
            else
                newElem.addAttribute
                    (new QName(node.getLocalName(),
                               new Namespace(node.getPrefix(),
                                             node.getNamespaceURI())),
                     node.getNodeValue());
        }
        for (org.w3c.dom.Node child = el.getFirstChild();
             child != null;
             child = child.getNextSibling()) {
            switch (child.getNodeType()) {
              case org.w3c.dom.Node.ELEMENT_NODE:
                newElem.add(elementFromW3CElement((org.w3c.dom.Element)child));
                break;

              case org.w3c.dom.Node.TEXT_NODE:
                newElem.addText(child.getNodeValue());
                break;

              default:
                System.err.println ("Ignoring Node "
                                    + child.getNodeName()
                                    + '='
                                    + child.getNodeValue());
            }
        }
        return newElem;
    }
}
