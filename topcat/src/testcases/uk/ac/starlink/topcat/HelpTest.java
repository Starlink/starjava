package uk.ac.starlink.topcat;

import java.io.IOException;
import java.net.URL;
import javax.help.HelpSet;
import javax.help.HelpSetException;
import junit.framework.TestCase;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import uk.ac.starlink.util.StarEntityResolver;

public class HelpTest extends TestCase {

    private URL hsURL = HelpWindow.class
                       .getResource( HelpWindow.HELPSET_LOCATION );
    private HelpSet hs;

    public HelpTest( String name ) throws HelpSetException {
        super( name );
        hs = new HelpSet( null, hsURL );
    }

    public void testXML() throws Exception {
        parseXML( hsURL );
        parseXML( hsURL.toURI().resolve( "Map.xml" ).toURL() );
        parseXML( hsURL.toURI().resolve( "TOC.xml" ).toURL() );
    }

    public void parseXML( URL url ) throws Exception {
        String loc = url.toString();
        SAXParserFactory fact = SAXParserFactory.newInstance();
        fact.setValidating( true );
        DefaultHandler handler = new DefaultHandler() {
            StarEntityResolver resolver = StarEntityResolver.getInstance();
            public InputSource resolveEntity( String sysId, String pubId ) 
                    throws SAXException {
                try {
                    return resolver.resolveEntity( sysId, pubId );
                }
                catch ( IOException e ) {
                    throw (SAXException) new SAXException( e.getMessage(), e )
                                        .initCause( e );
                }
            }
            public void warning( SAXParseException e ) throws SAXException {
                rethrow( e );
            }
            public void error( SAXParseException e ) throws SAXException {
                rethrow( e );
            }
            public void fatalError( SAXParseException e ) throws SAXException {
                rethrow( e );
            }
            private void rethrow( SAXParseException e ) throws SAXException {
                throw new SAXException( "Parse error in " + e.getSystemId()
                                      + " at line " + e.getLineNumber() 
                                      + " column " + e.getColumnNumber(), e );
            }
        };
        SAXParser parser = fact.newSAXParser();
        parser.parse( url.toString(), handler );
    }

    public void testExpressionHelp() {
        assertTrue( HelpAction.helpIdExists( MethodWindow.SYNTAX_HELP_ID ) );
    }
    
}
