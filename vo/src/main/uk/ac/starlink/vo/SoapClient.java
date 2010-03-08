package uk.ac.starlink.vo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Lightweight, freestanding SOAP client which can make simple requests
 * and allows the responses to be processed as a SAX stream.
 *
 * <p>Why write yet another SOAP client?  Last time I tried to get Axis
 * to do this (stream processing of the response) it took me several 
 * days of misery, and still didn't work.  The actual job I need to 
 * do here is quite straightforward, so it's not difficult to write it
 * from scratch.
 *
 * @author   Mark Taylor
 * @since    9 Dec 2009
 */
public class SoapClient {

    private final URL endpoint_;
    private OutputStream echoStream_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );
    private static final String SOAP_ENV_NS =
        "http://schemas.xmlsoap.org/soap/envelope/";

    /**
     * Constructor.
     *
     * @param  endpoint  SOAP endpoint of service
     */
    public SoapClient( URL endpoint ) {
        endpoint_ = endpoint;
    }

    /**
     * Returns the endpoint of the service this client talks to.
     *
     * @return  SOAP endpoint
     */
    public URL getEndpoint() {
        return endpoint_;
    }

    /**
     * Sets an output stream to which all input and output HTTP 
     * traffic will be logged.  If null (the default), no such logging
     * is performed.
     *
     * @param  echoStream  logging destination stream
     */
    public void setEchoStream( OutputStream echoStream ) {
        echoStream_ = echoStream;
    }

    /**
     * Sends a SOAP request, and passes the result body to a supplied
     * SAX content handler.  It will be parsed by a SAX parser which
     * is namespace aware, and is not validating.
     *
     * @param   bodyContent  content of the &lt;soapenv:Body&gt; element
     *          to be sent
     * @param   soapAction  unquoted value of the SOAPAction HTTP header
     *          to be sent; if null, no SOAPAction is set
     * @param   bodyHandler  a SAX ContentHandler which will be invoked on
     *          the content of the &lt;soapenv:Body&gt; element in case
     *          of a successful (200) response
     */
    public void execute( String bodyContent, String soapAction,
                         ContentHandler bodyHandler )
            throws IOException, SAXException {
        byte[] request = createRequest( bodyContent );
        logger_.info( "SOAP " + soapAction + " -> " + endpoint_ );
        URLConnection conn = endpoint_.openConnection();
        if ( ! ( conn instanceof HttpURLConnection ) ) {
            throw new IOException( "Not an HTTP connection??" );
        }
        HttpURLConnection hconn = (HttpURLConnection) conn;
        hconn.setRequestProperty( "Content-Length",
                                  Integer.toString( request.length ) );
        hconn.setRequestProperty( "Content-Type", "text/xml; charset=utf-8" );
        hconn.setRequestProperty( "Accept", "application/soap+xml" );
        if ( soapAction != null && soapAction.trim().length() > 0 ) {
            hconn.setRequestProperty( "SOAPAction", "\"" + soapAction + "\"" );
        }
        hconn.setRequestMethod( "POST" );
        hconn.setDoOutput( true );
        hconn.setDoInput( true );
        OutputStream out = new BufferedOutputStream( hconn.getOutputStream() );
        if ( echoStream_ != null ) {
            out = new EchoOutputStream( out, echoStream_ );
        }
        out.write( request );
        out.flush();
        out.close();
        int responseCode = hconn.getResponseCode();
        logger_.info( "SOAP response " + responseCode );

        /* Success. */
        if ( responseCode == 200 ) {
            ResponseHandler handler = new ResponseHandler( bodyHandler );
            InputStream in = new BufferedInputStream( hconn.getInputStream() );
            if ( echoStream_ != null ) {
                in = new EchoInputStream( in, echoStream_ );
            }
            try {
                createSaxParser().parse( in, handler );
            }
            finally {
                in.close();
                hconn.disconnect();
            }
        }

        /* SOAP fault. */
        else if ( responseCode == 500 ) {
            FaultHandler handler = new FaultHandler();
            InputStream err = hconn.getErrorStream();
            if ( echoStream_ != null ) {
                err = new EchoInputStream( err, echoStream_ );
            }
            try {
                createSaxParser().parse( err, handler );
                throw new IOException( "SOAP Fault: "
                                     + handler.faultCode_ + "; "
                                     + handler.faultString_ );
            }
            catch ( SAXException e ) {
                throw (IOException)
                      new IOException( "Badly formed SOAP fault?" )
                     .initCause( e );
            }
            finally {
                err.close();
                hconn.disconnect();
            }
        }

        /* Transport error. */
        else {
            String msg = "SOAP connection failed: "
                       + responseCode + ": "
                       + hconn.getResponseMessage();
            hconn.disconnect();
            throw new IOException( msg );
        }
    }

    /**
     * Turns a string containing body content into a complete SOAP request
     * document.
     *
     * @param   bodyContent  content
     * @return   UTF-8 byte array of document to POST
     */
    private byte[] createRequest( String bodyContent ) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintStream pout = new PrintStream( bout );
        pout.println( "<?xml version='1.0' encoding='utf-8'?>" );
        pout.println( "<soapenv:Envelope" );
        pout.println( "  xmlns:soapenv='" + SOAP_ENV_NS + "'" );
        pout.println( "  xmlns:xsi="
                    + "'http://www.w3.org/1999/XMLSchema-instance'" );
        pout.println( "  xmlns:xsd='http://www.w3.org/1999/XMLSchema'>" );
        pout.println( "  <soapenv:Body>" );
        pout.flush();
        bout.write( bodyContent.getBytes( "utf-8" ) );
        bout.flush();
        pout.println( "  </soapenv:Body>" );
        pout.println( "</soapenv:Envelope>" );
        pout.flush();
        bout.flush();
        return bout.toByteArray();
    }

    /**
     * Utility method to serialize a DOM Node to a String.
     *
     * @param  node  node to serialize
     * @return   string version
     */
    public static String nodeToString( Node node ) throws IOException {
        Source xsrc = new DOMSource( node );
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        Result xres = new StreamResult( bout );
        try {
            Transformer trans =
                TransformerFactory.newInstance().newTransformer();
            trans.setOutputProperty( "omit-xml-declaration", "yes" );
            trans.setOutputProperty( "indent", "yes" );
            trans.setOutputProperty( "encoding", "UTF-8" );
            trans.transform( xsrc, xres );
        }
        catch ( TransformerException e ) {
            throw (IOException) new IOException().initCause( e );
        }
        bout.flush();
        return new String( bout.toByteArray(), "utf-8" );
    }

    /**
     * Returns a new SAX parser.
     *
     * @return  sax parser, namespace aware, not validating
     */
    private static SAXParser createSaxParser() throws IOException {
        try { 
            SAXParserFactory spfact = SAXParserFactory.newInstance();
            spfact.setNamespaceAware( true );
            spfact.setValidating( false );
            return spfact.newSAXParser();
        }
        catch ( ParserConfigurationException e ) {
            throw (IOException)
                  new IOException( "SAX parse trouble" )
                 .initCause( e );
        }
        catch ( SAXException e ) {
            throw (IOException)
                  new IOException( "SAX parse trouble" )
                 .initCause( e );
        }
    }

    /**
     * SAX ContentHandler implementation which takes care of a SOAP envelope
     * and body, and passes the body contents to a supplied handler.
     */
    private static class ResponseHandler extends DefaultHandler {
        private final ContentHandler bodyHandler_;
        private boolean inEnvelope_;
        private boolean inBody_;

        /**
         * Constructor.
         *
         * @param   bodyHandler   handler invoked for body contents
         */
        ResponseHandler( ContentHandler bodyHandler ) {
            bodyHandler_ = bodyHandler;
        }

        public void startElement( String namespaceURI, String localName,
                                  String qName, Attributes atts )
                throws SAXException {
            if ( ! inEnvelope_ ) {
                if ( SOAP_ENV_NS.equals( namespaceURI ) &&
                     "Envelope".equals( localName ) ) {
                    inEnvelope_ = true;
                }
            }
            else if ( ! inBody_ ) {
                if ( SOAP_ENV_NS.equals( namespaceURI ) &&
                     "Body".equals( localName ) ) {
                    inBody_ = true;
                }
            }
            else {
                bodyHandler_.startElement( namespaceURI, localName, qName,
                                           atts );
            }
        }

        public void endElement( String namespaceURI, String localName,
                                String qName )
                throws SAXException {
            if ( inBody_ ) {
                if ( SOAP_ENV_NS.equals( namespaceURI ) &&
                     "Body".equals( localName ) ) {
                    inBody_ = false;
                }
                else {
                    bodyHandler_.endElement( namespaceURI, localName, qName );
                }
            }
            else if ( inEnvelope_ ) {
                if ( SOAP_ENV_NS.equals( namespaceURI ) &&
                     "Envelope".equals( localName ) ) {
                    inEnvelope_ = false;
                }
            }
        }

        public void characters( char[] ch, int start, int length )
                throws SAXException {
            if ( inBody_ ) {
                bodyHandler_.characters( ch, start, length );
            }
        }

        public void ignorableWhitespace( char[] ch, int start, int length )
                throws SAXException {
            if ( inBody_ ) {
                bodyHandler_.ignorableWhitespace( ch, start, length );
            }
        }

        public void skippedEntity( String name )
                throws SAXException {
            if ( inBody_ ) {
                bodyHandler_.skippedEntity( name );
            }
        }

        public void startDocument() throws SAXException {
            bodyHandler_.startDocument();
        }

        public void endDocument() throws SAXException {
            bodyHandler_.endDocument();
        }

        public void startPrefixMapping( String prefix, String uri )
                throws SAXException {
            bodyHandler_.startPrefixMapping( prefix, uri );
        }

        public void endPrefixMapping( String prefix )
                throws SAXException {
            bodyHandler_.endPrefixMapping( prefix );
        }

        public void processingInstruction( String target, String data )
                throws SAXException {
            bodyHandler_.processingInstruction( target, data );
        }

        public void setDocumentLocator( Locator locator ) {
            bodyHandler_.setDocumentLocator( locator );
        }
    }

    /**
     * SAX ContentHandler implementation which makes sense of a SOAP fault
     * (response from a 500 code).
     */
    private static class FaultHandler extends DefaultHandler {
        private String faultString_;
        private String faultCode_;
        private StringBuffer charBuf_;

        public void startElement( String namespaceURI, String localName,
                                  String qName, Attributes atts ) {
            if ( "faultstring".equals( localName ) ||
                 "faultcode".equals( localName ) ) {
                charBuf_ = new StringBuffer();
            }
        }

        public void endElement( String namespaceURI, String localName,
                                String qName ) {
            if ( "faultstring".equals( localName ) ) {
                faultString_ = charBuf_.toString();
                charBuf_ = null;
            }
            else if ( "faultcode".equals( localName ) ) {
                faultCode_ = charBuf_.toString();
                charBuf_ = null;
            }
        }

        public void characters( char[] ch, int start, int length ) {
            if ( charBuf_ != null ) {
                charBuf_.append( ch, start, length );
            }
        }
    }

    /**
     * OutputStream which writes everything to a logging stream as well
     * as its normal desination.
     */
    private static class EchoOutputStream extends FilterOutputStream {
        private final OutputStream out_;
        private final OutputStream echo_;

        /**
         * Constructor.
         *
         * @param   out  base output stream
         * @return  echo  logging stream
         */
        EchoOutputStream( OutputStream out, OutputStream echo ) {
            super( out );
            out_ = out;
            echo_ = echo;
        }
        public void write( byte[] b ) throws IOException {
            out_.write( b );
            echo_.write( b );
        }
        public void write( byte[] b, int off, int len ) throws IOException {
            out_.write( b, off, len );
            echo_.write( b, off, len );
        }
        public void write( int b ) throws IOException {
            out_.write( b );
            echo_.write( b );
        }
        public void flush() throws IOException {
            out_.flush();
            echo_.flush();
        }
    }

    /**
     * InputStream which writes everything to a logging stream as well
     * as reading it.
     */
    private static class EchoInputStream extends FilterInputStream {
        private final InputStream in_;
        private final OutputStream echo_;

        /**
         * Constructor.
         *
         * @param  in  base input stream
         * @param  echo   logging stream
         */
        EchoInputStream( InputStream in, OutputStream echo ) {
            super( in );
            in_ = in;
            echo_ = echo;
        }
        public int read() throws IOException {
            int c = in_.read();
            if ( c >= 0 ) {
                echo_.write( c );
            }
            return c;
        }
        public int read( byte[] b ) throws IOException {
            int n = in_.read( b );
            if ( n > 0 ) {
                echo_.write( b, 0, n );
            }
            return n;
        }
        public int read( byte[] b, int off, int len ) throws IOException {
            int n = in_.read( b, off, len );
            if ( n > 0 ) {
                echo_.write( b, off, n );
            }
            return n;
        }
    }
}
