/*
 * Created by Ray Plante for the National Virtual Observatory
 * c. 2008
 */
package net.ivoa.registry.search;

import net.ivoa.registry.RegistryAccessException;
import net.ivoa.registry.RegistryFormatException;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory; 
import javax.xml.parsers.ParserConfigurationException; 
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.DOMException;

/**
 * A class for retrieving and analyzing a WSDL
 * <p>
 * This class (currently only) checks the variant of the keywordSearch 
 * interface supported by the service.  This check became necessary due 
 * to an error in the official (working draft) standard WSDL published on 
 * the IVOA web site, against several registries were built.  To provide 
 * backward compatibility with these pre-Recommendation implementations, 
 * this class can adjust its keyword search message accordingly.  
 */
public class AssessWSDL {

    protected URL wsdlurl = null;
    protected Document wsdl = null;
    protected DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    /**
     * the expected namespace for the WSDL.  The WSDL pointed to by the WSDL
     * URL may have a different namespace; however it may import the desired
     * WSDL and therefore be compliant with it.
     */
    public final String targetNamespace;

    public final static String defaultSearchTargetNamespace = 
        "http://www.ivoa.net/wsdl/RegistrySearch/v1.0";

    /**
     * create an instance for a given WSDL document URL
     * @param wsdl              the URL of the WSDL document
     * @param targetNamespace   the expected targetNamespace.  
     */
    public AssessWSDL(String wsdl) throws MalformedURLException {
        this(new URL(wsdl), defaultSearchTargetNamespace);
    }

    /**
     * create an instance for a given WSDL document URL
     * @param wsdl              the URL of the WSDL document
     * @param targetNamespace   the expected targetNamespace.  
     */
    public AssessWSDL(File wsdl) throws MalformedURLException {
        this("file:" + wsdl.toString());
    }

    /**
     * create an instance for a given WSDL document URL
     * @param wsdl              the URL of the WSDL document
     * @param targetNamespace   the expected targetNamespace.  
     */
    public AssessWSDL(URL wsdl) {
        this(wsdl, defaultSearchTargetNamespace);
    }

    /**
     * create an instance for a given WSDL document URL
     * @param wsdl              the URL of the WSDL document
     * @param targetNamespace   the expected targetNamespace.  
     */
    public AssessWSDL(URL wsdl, String targetNamespace) {
        wsdlurl = wsdl;
        this.targetNamespace = targetNamespace;

        dbf.setAttribute(NAMESPACES_FEATURE_ID, Boolean.TRUE);
    }

    /**
     * return the URL for the WSDL document
     */
    public URL getWSDLURL() { return wsdlurl; }

    static XPath xpengine = null;
    static XPathExpression definitionsxp = null;
    static XPathExpression inclxp = null;
    static XPathExpression kwsxp = null;
    static XPathExpression maxxp = null;
    static XPathExpression toxp = null;
    static XPathExpression importxp = null;
    static XPathExpression theimportxp = null;
    XPathExpression wschemaxp = null;
    XPathExpression schemaxp = null;

    /**
     * return an XPath engine.  A static XPath instance is held for all 
     * AssessWSDL instances so that expressions can be compiled and reused.
     */
    protected XPath getXPathEngine() { 
        if (schemaxp == null || wschemaxp == null || xpengine == null) {
          try {
              if (xpengine == null) {
                xpengine = XPathFactory.newInstance().newXPath();

                NSCntxt nsc = new NSCntxt();
                nsc.addMapping("w", "http://schemas.xmlsoap.org/wsdl/");
                nsc.addMapping("x", "http://www.w3.org/2001/XMLSchema");
                xpengine.setNamespaceContext(nsc);

                definitionsxp = xpengine.compile("w:definitions");
                inclxp = xpengine.compile("x:include");
                kwsxp = xpengine.compile("x:element[@name='KeywordSearch']");
                maxxp = xpengine.compile("x:complexType/x:sequence/" + 
                                         "x:element[@name='max']");
                toxp = xpengine.compile("x:complexType/x:sequence/" +
                                        "x:element[@name='to']");
                theimportxp = xpengine.compile("w:import[@namespace='" + 
                                               SOAPSearchClient.WSDL_NS + "']");
                importxp = xpengine.compile("w:import");
              }

              if (schemaxp == null) 
                  schemaxp = xpengine.compile("x:schema[@targetNamespace='" +
                                              targetNamespace + "']");
              if (wschemaxp == null)
                  wschemaxp = xpengine.compile("w:types/x:schema" + 
                                               "[@targetNamespace='" +
                                               targetNamespace + "']");
            }
            catch (XPathExpressionException ex) {
                throw new InternalError("programmer error: bad XPath syntax: " +
                                        ex.getMessage());
            }
        }

        return xpengine;
    }

    /**
     * resolve the WSDL URL, parse it, and cache the document internally.
     * Any previously cached document will be overwritten unless an error 
     * occurs.  
     */
    public void recacheWSDLDoc() 
        throws IOException, ParserConfigurationException, SAXException
    {
        InputStream fromurl = wsdlurl.openStream();
        Document doc = dbf.newDocumentBuilder().parse(new InputSource(fromurl));
        try {
            fromurl.close();
        } catch (IOException ex) { }

        wsdl = doc;
    }

    final static String NAMESPACES_FEATURE_ID = 
        "http://xml.org/sax/features/namespaces";

    /**
     *  retrieve and cache the WSDL internally, if necessary.  If the document
     *  has already been cached, it will not be re-retrieved.
     *  @exception RegistryAccessException   if an IO error occurs while reading
     *     document from the URL
     *  @exception RegistryFormatException   if an XML parsing error occurs
     *  @see {@link #recacheWSDLDoc() recacheWSDLDoc}
     */
    public void cacheWSDLDoc() throws RegistryAccessException {
        if (wsdl == null) {
            try {
                recacheWSDLDoc();
            }
            catch (IOException ex) {
                throw new RegistryAccessException("Failed to read WSDL: " +
                                                  ex.getMessage());
            }
            catch (ParserConfigurationException ex) {
                throw new InternalError("configuration error: can't create " +
                                        "XML parser: " + ex.getMessage());
            }
            catch (SAXException ex) {
                throw new RegistryFormatException("error parsing WSDL: " +
                                                  ex.getMessage());
            }
        }
    }

    /**
     * return the WSDL as a DOM Document
     *  @exception RegistryAccessException   if an IO error occurs while reading
     *     document from the URL
     *  @exception RegistryFormatException   if an XML parsing error occurs
     */
    public Document getDocument() throws RegistryAccessException {
        cacheWSDLDoc();
        return wsdl;
    }

    /**
     * a bit-mask representing an unrecognized variant.  
     */
    public final static int VARIANT_UNRECOGNIZED = 0;

    /**
     * a bit-mask representing a keyword search interface that accepts 
     * the "max" input.
     */
    public final static int VARIANT_ACCEPTS_MAX = 1;

    /**
     * a bit-mask representing a keyword search interface that accepts 
     * the deprecated "to" input.
     */
    public final static int VARIANT_ACCEPTS_TO = 2;

    /**
     * 
     * @exception RegistryFormatException if there appears to be a fatal 
     *    format error in the WSDL.  
     * @exception RegistryAccessException if there is an I/O or network error
     *    while accessing the WSDL.
     */
    public int determineKeywordSearchInterfaceVariant() 
        throws RegistryAccessException 
    {
        Document doc = getDocument();
        int variant = VARIANT_UNRECOGNIZED;
        String xpe = null;
        getXPathEngine();

//         javax.xml.namespace.QName node = XPathConstants.NODE;

        try {
            // start by getting the root element, <wsdl:definitions>
            Element defs = null;
            defs = (Element) definitionsxp.evaluate(doc, XPathConstants.NODE);

            if (defs == null) return variant;

            if (targetNamespace.equals(defs.getAttribute("targetNamespace"))) {

                // descend to the xsd:schema node
                Element schema = 
                    (Element) wschemaxp.evaluate(defs, XPathConstants.NODE);
                if (schema == null) return variant;

                Element ksel = 
                    (Element) kwsxp.evaluate(schema, XPathConstants.NODE);
                if (ksel == null) {
                    // the KeywordSearch <xsd:element> is missing; we will 
                    // look through any included schemas.
//                     NodeList incls = 
//                       (NodeList) inclxp.evaluate(schema, XPathConstants.NODESET);
                    Object oinc = inclxp.evaluate(schema, XPathConstants.NODESET);
                    NodeList incls = (NodeList) oinc;
                    if (incls == null) return variant;
                    
                    int ni = incls.getLength();
                    for(int i=0; i < ni && ksel == null; i++) {
                        Element incl = (Element) incls.item(i);
                        ksel = 
                          findKwsInInclude(incl.getAttribute("schemaLocation"));
                    }
                }
                if (ksel == null) return variant;

                if (maxxp.evaluate(ksel, XPathConstants.NODE) != null) 
                    variant |= VARIANT_ACCEPTS_MAX;
                
                if (toxp.evaluate(ksel, XPathConstants.NODE) != null)
                    variant |= VARIANT_ACCEPTS_TO;
            }
            else {
                // search look through the imported WSDLs.
                // first see if we directly import the target WSDL here
                NodeList imports = 
                  (NodeList) theimportxp.evaluate(defs, XPathConstants.NODESET);

                // if the target is not imported directly, we'll loop through
                // the imports that might import the one we want.
                if (imports == null || imports.getLength() == 0)
                    imports = (NodeList) importxp.evaluate(defs, 
                                                        XPathConstants.NODESET);

                Element imp = null;
                String ns = null;
                String url = null;

                if (imports == null) return variant;
                int l = imports.getLength();

                for(int i=0; i < l; i++) {
                    imp = (Element) imports.item(i);
                    ns = imp.getAttribute("namespace");
                    if (ns == null) continue;

                    if (targetNamespace.equals(ns)) {

                        // importing the WSDL with the standard namespace
                        url = imp.getAttribute("location");
                        if (url == null) {
                            // we assume the IVOA location; thus it should 
                            // support max.
                            // For now, it's ambiguous, so we'll keep looking.
                        }
                        else if (targetNamespace.equals(url)) {
                            // explicitly give IVOA location; assume supports
                            // max
                            variant |= VARIANT_ACCEPTS_MAX;
                            break;
                        }
                        else {
                            // 3rd-party location: load it and search there
                            try {
                              AssessWSDL nxtwsdl = 
                                  new AssessWSDL(new URL(url), targetNamespace);
                              variant |= 
                                nxtwsdl.determineKeywordSearchInterfaceVariant();
                              break;
                            }
                            catch (MalformedURLException ex) {
                                // they gave us a bum location
                                throw new RegistryFormatException(
                                    "bad URL for standard search WSDL: " + url);
                            }
                        }
                    }
                    else if (ns.startsWith("http://www.ivoa.net/xml")) {
                        // for efficiency, don't bother searching schemas
                        continue;
                    }
                    else {
                        // unknown import; load it and check it out
                        url = imp.getAttribute("schemaLocation");
                        try {
                            AssessWSDL nxtwsdl = new AssessWSDL(new URL(url),
                                                                targetNamespace);
                            variant |= 
                                nxtwsdl.determineKeywordSearchInterfaceVariant();
                        }
                        catch (MalformedURLException ex) { 
                            // bad URL given; skip over this one
                        }
                        catch (RegistryAccessException ex) { 
                            // read error; skip over this one
                        }
                    }
                }
            }
        }
        catch (XPathExpressionException ex) {
            throw new InternalError("programmer error: bad XPath syntax?: " +
                                    ex.getMessage());
        }

        return variant;
    }

    Element findKwsInInclude(String locurl) {
        try {
            Document doc = 
                dbf.newDocumentBuilder().parse(new InputSource(locurl));
            Element schel = 
                (Element) schemaxp.evaluate(doc, XPathConstants.NODE);
            if (schel == null) return null;
            Element out = 
                (Element) kwsxp.evaluate(schel, XPathConstants.NODE);
            return out;
        }
        catch (Exception ex) {
            return null;
        }
    }

    static class NSCntxt implements NamespaceContext {
        HashMap<String, String> p2u = new HashMap<String, String>(5);
        HashMap<String, Set<String> > u2p = new HashMap<String, Set<String> >(5);
        NSCntxt() {
            addMapping(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
            addMapping(XMLConstants.XMLNS_ATTRIBUTE, 
                       XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
            addMapping(XMLConstants.DEFAULT_NS_PREFIX, XMLConstants.NULL_NS_URI);
        }
        public void addMapping(String prefix, String ns) {
            p2u.put(prefix, ns);
            Set<String> prefixes = u2p.get(ns);
            if (prefixes == null) {
                prefixes = new HashSet<String>(1);
                u2p.put(ns, prefixes);
            }
            prefixes.add(prefix);
        }
        public String getNamespaceURI(String prefix) { 
            if (prefix == null) 
                throw new IllegalArgumentException("null prefix");

            String out = p2u.get(prefix);
            if (out == null) return XMLConstants.NULL_NS_URI;
            return out;
        }
        public String getPrefix(String uri) {
            Set<String> out = u2p.get(uri);
            if (out == null || out.size() == 0) return null;
            return out.iterator().next();
        }
        public Iterator getPrefixes(String ns) { 
            Set out = u2p.get(ns);
            if (out == null) out = new HashSet<String>(1);
            return out.iterator();
        }
    }

    /**
     * a convenience function for quickly checking a WSDL
     */
    public static int keywordSearchVariant(URL url) 
        throws RegistryAccessException
    {
        AssessWSDL aw = new AssessWSDL(url);
        return aw.determineKeywordSearchInterfaceVariant();
    }

    /**
     * test this class
     */
    public static void main(String[] args) {
        String wsdlurl = null;
        wsdlurl = (args.length > 0) ? args[0] 
                                    : AssessWSDL.defaultSearchTargetNamespace;

        try {
            int variant = AssessWSDL.keywordSearchVariant(new URL(wsdlurl));
            if ((variant & AssessWSDL.VARIANT_ACCEPTS_MAX) > 0) 
                System.out.println("max is accepted.");
            if ((variant & AssessWSDL.VARIANT_ACCEPTS_TO) > 0) 
                System.out.println("to is accepted.");
            if (variant == 0)
                System.out.println("unrecognized interface");
        }
        catch (Exception ex) {
            System.err.println("Failed to assess WSDL: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
