package uk.ac.starlink.ttools.taplint;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import junit.framework.TestCase;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class ResourceTest extends TestCase {

    public void testResources()
            throws IOException, SAXException, ParserConfigurationException {
        int nSchema = 0;
        Map<String,URL> schemaMap =
            new IvoaSchemaResolver( false ).getSchemaMap();
        for ( Map.Entry<String,URL> entry : schemaMap.entrySet() ) {
            String namespace = entry.getKey();
            URL url = entry.getValue();
            assertNotNull( "No resource for " + namespace, url );
            if ( url != null &&
                 IvoaSchemaResolver.IVOA_SCHEMA_MAP.containsKey( namespace ) ) {
                assertEquals( "Wrong namespace for " + url,
                              namespace, getTargetNamespace( url ) );
            }
            nSchema++;
        }
        assertTrue( nSchema > 8 );
        assertTrue( schemaMap.containsKey( IvoaSchemaResolver.UWS_URI ) );
        assertTrue( schemaMap.containsKey( "http://www.w3.org/2001/XMLSchema"));
    }

    private String getTargetNamespace( URL schemaUrl )
            throws IOException, SAXException, ParserConfigurationException {
        Element el = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse( schemaUrl.openStream() )
                    .getDocumentElement();
        assertTrue( el.getTagName().endsWith( "schema" ) );
        return el.getAttribute( "targetNamespace" );
    }
}
