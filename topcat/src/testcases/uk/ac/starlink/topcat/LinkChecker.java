package uk.ac.starlink.topcat;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class LinkChecker extends DefaultHandler {

    final URL context;
    private int localFailures;
    private int extFailures;

    public LinkChecker( URL context ) {
        this.context = context;
    }

    public void startElement( String namespaceURI, String localName,
                              String qName, Attributes atts )
            throws SAXException {
        if ( qName.equalsIgnoreCase( "a" ) ) {
            String href = atts.getValue( "href" );
            if ( href != null &&
                 ! href.startsWith( "#" ) &&
                 ! href.startsWith( "mailto:" ) ) {
                checkURL( href );
            }
        }
        else if ( qName.equalsIgnoreCase( "img" ) ) {
            String src = atts.getValue( "src" );
            if ( src != null ) {
                checkURL( src );
            }
        }
    }

    public void endDocument() throws SAXException {
        if ( extFailures > 0 ) {
            System.out.println( extFailures +
                                " external link resolution errors" );
        }
        if ( localFailures > 0 ) {
            System.out.println( localFailures + 
                                " local link resolution errors" );
        }
    }

    void checkURL( String href ) {
        URL url = null;
        try {
            url = new URL( context, href );
            URLConnection conn = url.openConnection();
            if ( conn instanceof HttpURLConnection ) {
                HttpURLConnection hconn = (HttpURLConnection) conn;
                hconn.setRequestMethod( "HEAD" );
                int response = hconn.getResponseCode();
                if ( response != HttpURLConnection.HTTP_OK ) {
                    System.out.println( "Response " + response +
                                        ": " + href );
                    extFailures++;
                }
                hconn.disconnect();
            }
            else {
                conn.getInputStream().close();
            }
        }
        catch ( MalformedURLException e ) {
            System.out.println( "Badly formed URL: " + href +"\t" );
            localFailures++;
        }
        catch ( Exception e ) {
            System.out.println( "Bad link: " + href +"\t" );
            String proto = url == null ? null : url.getProtocol();
            if ( "file".equals( proto ) ) {
                localFailures++;
            }
            else {
                extFailures++;
            }
        }
    }

    public static void main( String[] args ) 
            throws MalformedURLException, TransformerException {
        if ( args.length != 2 ) {
            System.err.println( "usage: LinkChecker stylesheet xmldoc" );
            System.exit( 1 );
        }
        String xslt = args[ 0 ];
        String xml = args[ 1 ];
        try {
            boolean ok = checkLinks( new StreamSource( xslt ),
                                     new StreamSource( xml ),
                                     new File( "." ).toURL() );
            System.exit( ok ? 0 : 1 );
        }
        catch ( TransformerException e ) {
            System.err.println( e );
            System.exit( 1 );
        }
        catch ( MalformedURLException e ) {
            System.err.println( e );
            System.exit( 1 );
        }
    }

    public static boolean checkLinks( Source xsltSrc, Source xmlSrc,
                                      URL context )
            throws TransformerException, MalformedURLException {
        Transformer trans = TransformerFactory.newInstance()
                           .newTransformer( xsltSrc );
        trans.setOutputProperty( OutputKeys.METHOD, "xml" );
        LinkChecker checker = new LinkChecker( context );
        trans.transform( xmlSrc, new SAXResult( checker ) );
        return checker.localFailures == 0;
    }

}
