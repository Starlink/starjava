// Copyright 2002
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: SaxParserUtil.java,v 1.4 2002/08/20 09:57:58 brighton Exp $

package jsky.util;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Stack;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import java.io.InputStream;


/**
 * Utility bass class for parsing an XML stream using a SAX parser.
 * <p>
 * This class uses reflection to call subclass methods for each start
 * and end element. The method names are _${element}Start and _${element}End.
 * For example, for the following XML code:
 * <p>
 * &lt;para&gt;some text&lt;/para&gt;
 * <p>
 * would cause the methods _paraStart(Attributes)  and _paraEnd() to be called.
 * The text between the two tags can be retrieved with the getCData() method.
 *
 * @version $Revision: 1.4 $
 * @author Allan Brighton
 */
public abstract class SaxParserUtil extends DefaultHandler {

    /** Value of character data (CDATA section) for current element */
    private String _cdata = null;

    /** Used in startElement for calling start<ELEMENT> method */
    private Class[] _parameterTypes = {Attributes.class};

    /** Used to keep track of Elements */
    private Stack _stack = new Stack();

    // The URL of the last XML file parsed by this instance
    private URL _url;


    /**
     * Default constructor. Call parse(urlStr) to do the actual parsing.
     */
    public SaxParserUtil() {
    }


    /**
     * Parse the given XML file.
     *
     * @param url the URL for the XML file
     */
    public void parse(URL url) {
	_url = url;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            XMLReader parser = factory.newSAXParser().getXMLReader();
            parser.setEntityResolver(this);
            parser.setContentHandler(this);
            parser.setErrorHandler(this);
            parser.parse(url.toString());
        }
        catch (SAXException e) {
            e.printStackTrace();
            Exception ee = e.getException();
            while (ee != null) {
                //System.out.println("XXX SaxParserUtil parse embedded exception: " + ee.toString());
                ee.printStackTrace();
                if (ee instanceof SAXException)
                    ee = ((SAXException) ee).getException();
                else
                    ee = null;
            }
            throw new RuntimeException("SAX Error parsing XML document: " + e.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    /**
     * Parse the given XML file.
     *
     * @param urlStr the URL string for the XML file
     */
    public void parse(String urlStr) throws MalformedURLException {
	URL url = new URL(urlStr);
	parse(url);
    }


    /**
     * Parse an XML file from an already open input stream.
     *
     * @param url the URL for the XML file, for reference
     * @param in the input stream for the given URL
     */
    public void parse(URL url, InputStream in)  {
	_url = url;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            XMLReader parser = factory.newSAXParser().getXMLReader();
            parser.setEntityResolver(this);
            parser.setContentHandler(this);
            parser.setErrorHandler(this);
            parser.parse(new InputSource(in));
        }
        catch (SAXException e) {
            e.printStackTrace();
            Exception ee = e.getException();
            while (ee != null) {
                //System.out.println("XXX SaxParserUtil parse embedded exception: " + ee.toString());
                ee.printStackTrace();
                if (ee instanceof SAXException)
                    ee = ((SAXException) ee).getException();
                else
                    ee = null;
            }
            throw new RuntimeException("SAX Error parsing XML document: " + e.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }



    /** Return the URL of the last XML file parsed by this instance. */
    public URL getURL() {
	return _url;
    }

    /**
     * This method tries to locate a local copy of the DTD as a JSky resource.
     * Derived classes will need to redefine this method if a different resource
     * class is used.
     *
     * @see jsky.util.Resources
     */
    public InputSource resolveEntity(String publicId, String systemId) {
        try {
            String fileName = new File(new URL(systemId).getFile()).getName();
            URL url = Resources.getResource(fileName);
            return new InputSource(url.openStream());
        }
        catch (Exception e) {
            throw new RuntimeException("Error resolving entity: " + systemId + ": " + e.toString());
        }
    }


    /** Called for recoverable errors */
    public void fatalError(SAXParseException e) throws SAXParseException {
        System.out.println("XML input line " + e.getLineNumber()
                + ": Fatal error: " + e.getMessage());
        throw e;
    }

    /** Called for recoverable errors */
    public void error(SAXParseException e) throws SAXParseException {
        System.out.println("XML input line " + e.getLineNumber()
                + ": Error: " + e.getMessage());
        throw e;
    }

    /** Called for XML parser warnings */
    public void warning(SAXParseException e) throws SAXParseException {
        System.out.println("XML input line " + e.getLineNumber()
                + ": Warning: " + e.getMessage());
    }


    /**
     * Called for each element start tag. Calls a method named: ("_" + qName + "Start") and passes
     * the Attributes as a parameter.
     */
    public void startElement(String namespaceURI, String localName, String qName, Attributes attrs)
            throws SAXException {
        // Use the reflection API to call "start<ElementName>(attrs)", if it exists.
        Object[] args = {attrs};
        String methodName = "_" + qName + "Start";
        try {
            this.getClass().getMethod(methodName, _parameterTypes).invoke(this, args);
        }
        catch (InvocationTargetException e1) {
	    Throwable t = e1.getTargetException();
            System.out.println("SaxParserUtil.startElement: error calling " + methodName + ": " + t);
        }
        catch (Exception e2) {
            System.out.println("SaxParserUtil.startElement: error calling " + methodName + ": " + e2);
        }

        // keep a stack to note where where are in the XML hierarchy
        _stack.push(qName);
        _cdata = null;
    }


    /**
     * Called for each element end tag. Calls a method named ("_" + qName + "End") with no
     * arguments.
     */
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {

        _stack.pop();

        // use the reflection API to call "end<ElementName>()", if it exists
        String methodName = "_" + qName + "End";
        try {
            this.getClass().getMethod(methodName, null).invoke(this, null);
        }
        catch (Exception e) {
            System.out.println("SaxParserUtil.endElement: error calling " + methodName + ": " + e + ": " + e.getMessage());
	    e.printStackTrace();
        }

        _cdata = null;
    }


    /** Receive notification of character data (CDATA) inside an element. */
    public void characters(char buf[], int offset, int len) throws SAXException {
        String s = new String(buf, offset, len);
        if (_cdata == null)
            _cdata = s;
        else
            _cdata += s;
    }

    /** Return the value of character data (CDATA section) for current element. */
    protected String getCData() {
        return _cdata;
    }

}
