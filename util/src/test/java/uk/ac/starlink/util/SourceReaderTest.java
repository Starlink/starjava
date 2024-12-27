package uk.ac.starlink.util;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import static org.junit.jupiter.api.Assertions.*;

public class SourceReaderTest {

    private Element dom;
    private File tmpfile;

    public void setUp() throws ParserConfigurationException {

        DocumentBuilderFactory dfact = DocumentBuilderFactory.newInstance();
        DocumentBuilder dbuild;
        dbuild = dfact.newDocumentBuilder();
        Document doc = dbuild.newDocument();

        dom = doc.createElement( "animals" );
        doc.appendChild( dom );

        Element el1 = doc.createElement( "loris" );
        el1.setAttribute( "speed", "slow" );
        dom.appendChild( el1 );

        Element el2 = doc.createElement( "sloth" );
        el2.appendChild( doc.createElement( "toe" ) );
        el2.appendChild( doc.createElement( "toe" ) );
        el2.appendChild( doc.createElement( "toe" ) );
        dom.appendChild( el2 );

    }
        
    public void testGetDOM() throws TransformerException {
        SourceReader sr = new SourceReader();
        Transformer srtrans = sr.getTransformer();
        assertNotNull( srtrans );

        Transformer trans = TransformerFactory.newInstance().newTransformer();
        Source domsrc = new DOMSource( dom );
        Element domcopy = (Element) sr.getDOM( domsrc );
        Source domcopysrc = new DOMSource( domcopy );

        ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
        ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
        sr.writeSource( domsrc, bos1 );
        sr.writeSource( domcopysrc, bos2 );
        assertArrayEquals( bos1.toByteArray(), bos2.toByteArray() );

        StringWriter swr1 = new StringWriter();
        StringWriter swr2 = new StringWriter();
        sr.writeSource( domsrc, swr1 );
        sr.writeSource( domcopysrc, swr2 );
        assertEquals( swr1.toString(), swr2.toString() );

        InputStream bis1 = new ByteArrayInputStream( bos1.toByteArray() );
        Reader rdr2 = new StringReader( swr2.toString() );

        Source stsrc1 = new StreamSource( bis1 );
        Source stsrc2 = new StreamSource( rdr2 );

        Node d1 = sr.getDOM( stsrc1 );
        Node d2 = sr.getDOM( stsrc2 );

        StringWriter dswr1 = new StringWriter();
        StringWriter dswr2 = new StringWriter();
        sr.writeSource( new DOMSource( d1 ), dswr1 );
        sr.writeSource( new DOMSource( d2 ), dswr2 );

    }

}
