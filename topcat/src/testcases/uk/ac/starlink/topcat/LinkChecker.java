package uk.ac.starlink.topcat;

import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

public class LinkChecker {

    private final URL context;
    private int localFailures;
    private int extFailures;
    private Map urlNames = new HashMap();
    private static Pattern namePattern1 =
        Pattern.compile( "<a\\b[^>]*\\bname=['\"]([^'\"]*)['\"]",
                         Pattern.CASE_INSENSITIVE );
    private static Pattern namePattern2 =
        Pattern.compile( "<a\\s+name=(\\w+)>",
                         Pattern.CASE_INSENSITIVE );

    public LinkChecker( URL context ) {
        this.context = context;
    }

    private boolean checkURL( String href ) {
        int hashPos = href.indexOf( '#' );
        URL url;
        String frag;
        try {
            if ( hashPos < 0 ) {
                url = new URL( context, href );
                frag = null;
            }
            else {
                url = new URL( context, href.substring( 0, hashPos ) );
                frag = href.substring( hashPos + 1 );
            }
        }
        catch ( MalformedURLException e ) {
            logMessage( "Badly formed URL: " + href );
            localFailures++;
            return false;
        }
        boolean ok;
        try {
            if ( frag == null ) {
                ok = checkUrlExists( url );
            }
            else {
                ok = checkUrlContains( url, frag );
            }
         }
         catch ( IOException e ) {
            logMessage( "Connection failed: " + e );
            ok = false;
         }
         if ( ! ok ) {
            String proto = url.getProtocol();
            if ( "file".equals( proto ) ) {
                logMessage( "Bad link: " + href );
                localFailures++;
            }
            else {
                extFailures++;
                logMessage( "Bad remote link: " + href );
            }
        }
        return ok;
    }

    private boolean checkUrlExists( URL url ) throws IOException {
        if ( urlNames.containsKey( url ) ) {
            return true;
        }
        else {
            URLConnection conn = url.openConnection();
            boolean ok;
            if ( conn instanceof HttpURLConnection ) {
                HttpURLConnection hconn = (HttpURLConnection) conn;
                hconn.setRequestMethod( "HEAD" );
                int response = hconn.getResponseCode();
                if ( response == HttpURLConnection.HTTP_OK ) {
                    ok = true;
                }
                else {
                    logMessage( "Response " + response + ": " + url );
                    ok = false;
                }
                hconn.disconnect();
            }
            else {
                conn.getInputStream().close();
                ok = true;
            }
            if ( ok ) {
                urlNames.put( url, null );
            }
            return ok;
        }
    }

    private boolean checkUrlContains( URL url, String frag )
            throws IOException {
        if ( urlNames.get( url ) == null ) {
            Set names = new HashSet();
            BufferedReader strm = 
                new BufferedReader( new InputStreamReader( url.openStream() ) );
            for ( String line; ( line = strm.readLine() ) != null; ) {
                for ( Matcher matcher = namePattern1.matcher( line );
                      matcher.find(); ) {
                    names.add( matcher.group( 1 ) );
                }
                for ( Matcher matcher = namePattern2.matcher( line );
                      matcher.find(); ) {
                    names.add( matcher.group( 1 ) );
                }
            }
            strm.close();
            urlNames.put( url, names );
        }
        return ((Collection) urlNames.get( url )).contains( frag );
    }

    public boolean checkLinks( Source xsltSrc, Source xmlSrc )
            throws TransformerException, MalformedURLException {
        Transformer trans = TransformerFactory.newInstance()
                           .newTransformer( xsltSrc );
        trans.setOutputProperty( OutputKeys.METHOD, "xml" );
        LinkCheckHandler handler = new LinkCheckHandler();
        trans.transform( xmlSrc, new SAXResult( handler ) );
        return handler.ok;
    }

    protected void logMessage( String msg ) {
        System.out.println( msg );
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
            LinkChecker checker = new LinkChecker( new File( "." ).toURL() );
            boolean ok = checker.checkLinks( new StreamSource( xslt ),
                                             new StreamSource( xml ) );
            if ( checker.extFailures > 0 ) {
                System.out.println( checker.extFailures +
                                    " external link resolution errors" );
            }
            if ( checker.localFailures > 0  ) {
                System.out.println( checker.localFailures + 
                                    " local link resolution errors" );
            }
            System.exit( checker.localFailures == 0 ? 0 : 1 );
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

    private class LinkCheckHandler extends DefaultHandler {

        boolean ok = true;
        private Set namesSeen = new HashSet();
        private List namesReferenced = new ArrayList();

        public void startElement( String namespaceURI, String localName,
                                  String qName, Attributes atts )
                throws SAXException {

            if ( qName.equalsIgnoreCase( "a" ) ) {
                String name = atts.getValue( "name" );
                if ( name != null ) {
                    namesSeen.add( name );
                }
                String href = atts.getValue( "href" );
                if ( href != null ) {
                    if ( href.startsWith( "#" ) ) {
                        namesReferenced.add( href.substring( 1 ) );
                    }
                    else if ( href.startsWith( "mailto:" ) ) {
                        // ignore
                    }
                    else {
                        if ( ! checkURL( href ) ) {
                            ok = false;
                        }
                    }
                }
            }
            else if ( qName.equalsIgnoreCase( "img" ) ) {
                String src = atts.getValue( "src" );
                if ( src != null ) {
                    if ( ! checkURL( src ) ) {
                        ok = false;
                    }
                }
            }
        }

        public void endDocument() throws SAXException {
            for ( Iterator it = namesReferenced.iterator(); it.hasNext(); ) {
                String name = (String) it.next();
                if ( ! namesSeen.contains( name ) ) {
                    localFailures++;
                    ok = false;
                    logMessage( "Bad link: #" + name );
                }
            }
        }
    }
}
