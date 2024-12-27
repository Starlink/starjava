package uk.ac.starlink.util;

import java.io.StringReader;
//import java.util.Iterator;
import java.net.URI;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.dom.DOMSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.ac.starlink.util.TestCase.assertDOMEquals;

/**
 * Provides some utility functions for working with {@link org.w3c.dom}
 * classes.
 *
 * @author   Mark Taylor (Starlink)
 * @author   Norman Gray (Starlink)
 */
public class DOMUtilsTest {

    private Element farm;
    private SourceReader srcrdr;

    public DOMUtilsTest() throws TransformerException {
        srcrdr = new SourceReader();
        srcrdr.setIncludeDeclaration(false);
        farm = setUp();
    }

    public Element setUp() throws TransformerException {
        String xtext = 
              "<farm owner='Old MacDonald'>"
            + "<owner>Old <![CDATA[Mac]]>Donald</owner>"
            + "<animals>"
            + "<pigs><pig name='Crosspatch'/><pig name='Spot'/></pigs>"
            + "</animals>"
            + "</farm>";
        Source xsrc = new StreamSource( new StringReader( xtext ) );
        Document doc = (Document) new SourceReader().getDOM( xsrc );
        return doc.getDocumentElement();
    }

    @Test
    public void testGetChildElementByName() {
        Element animals = DOMUtils.getChildElementByName( farm, "animals" );
        Element pigs = DOMUtils.getChildElementByName( animals, "pigs" );
        Element crosspatch = DOMUtils.getChildElementByName( pigs, "pig" );
        assertEquals( crosspatch.getAttribute( "name" ), "Crosspatch" );
        Element giraf = DOMUtils.getChildElementByName( animals, "giraffes" );
        assertNull( giraf );
    }

    @Test
    public void testGetTextContent() {
        Element animals = DOMUtils.getChildElementByName( farm, "animals" );
        assertEquals( DOMUtils.getTextContent( animals ), "" );
        Element owner = DOMUtils.getChildElementByName( farm, "owner" );
        assertEquals( DOMUtils.getTextContent( owner ), "Old MacDonald" );
    }

    @Test
    public void testNewDocument() {
        Document doc = DOMUtils.newDocument();
        String name = "Trevor";
        doc.appendChild( doc.createElement( name ) );
        assertEquals( doc.getDocumentElement().getTagName(), name );
    }

    @Test
    public void testRelativizeDOM()
            throws Exception {
        URI base = new URI("http://example.edu/ex");
        String inputXML =
                "<top>"
                + "<n1 uri='http://example.edu/ex/n1' another='http://example.edu/another/n1'>"
                + "<n1a uri='http://another.com/ex/n1a' another='http://another.com/ex/n1a'/>"
                + "</n1>"
                + "<n2/>"
                + "<n3>"
                + "<n3a nothing1='http://example.edu/ex/n3a' uri='rubbish'><br/></n3a>"
                + "<n3b uri='http://example.edu/n3b' another='http://example.edu/ex/dummy/../n3b'/>"
                + "<n3c uri='n3c'>Hello there</n3c>"
                + "</n3>"
                + "</top>";
        String resolvedXML =
                "<top>"
                + "<n1 uri='n1' another='http://example.edu/another/n1'>"
                + "<n1a uri='http://another.com/ex/n1a' another='http://another.com/ex/n1a'/>"
                + "</n1>"
                + "<n2/>"
                + "<n3>"
                + "<n3a nothing1='http://example.edu/ex/n3a' uri='rubbish'><br/></n3a>"
                + "<n3b uri='http://example.edu/n3b' another='http://example.edu/ex/dummy/../n3b'/>"
                + "<n3c uri='n3c'>Hello there</n3c>"
                + "</n3>"
                + "</top>";
        String anotherXML =
                "<top>"
                + "<n1 uri='http://example.edu/ex/n1' another='http://example.edu/another/n1'>"
                + "<n1a uri='http://another.com/ex/n1a' another='http://another.com/ex/n1a'/>"
                + "</n1>"
                + "<n2/>"
                + "<n3>"
                + "<n3a nothing1='http://example.edu/ex/n3a' uri='rubbish'><br/></n3a>"
                + "<n3b uri='http://example.edu/n3b' another='n3b'/>"
                + "<n3c uri='n3c'>Hello there</n3c>"
                + "</n3>"
                + "</top>";

        // Add a couple of `tests' of URI#relativize, (a) to check I
        // understand it, and (b) just to guard against any
        // functionality changes in the library.
        URI t1 = new URI("http://example.edu/a1");
        URI t2 = new URI("http://example.edu/ex/a2");
        assertEquals("http://example.edu/a1",
                     base.relativize(t1).toString());
        assertEquals("a1",
                     new URI("http://example.edu").relativize(t1).toString());
        assertEquals("a1",
                     new URI("http://example.edu/").relativize(t1).toString());
        assertEquals("a2",
                     base.relativize(t2).toString());
        assertEquals("ex/a2",
                     new URI("http://example.edu").relativize(t2).toString());
        assertEquals("ex/a2",
                     new URI("http://example.edu/").relativize(t2).toString());
        
        Element input    = StringToDom(inputXML);
        Element resolved = StringToDom(resolvedXML);
        Element another  = StringToDom(anotherXML);

//TODO: Not clear what these tests are for.
//        assertDOMEquals(resolved,
//                        DOMUtils.relativizeDOM(input.cloneNode(true),
//                                               base,
//                                               null));
//        assertDOMEquals(another,
//                        DOMUtils.relativizeDOM(input.cloneNode(true),
//                                               base,
//                                               "another"));
    }
        

    /* ******************** PRIVATE HELPER METHODS ******************** */
    
    private String DomToString(Node d) 
            throws TransformerException {
        java.io.OutputStream os = new java.io.ByteArrayOutputStream();
        srcrdr.writeSource(new DOMSource(d), os);
        return os.toString();
    }

    private Element StringToDom(String s) 
            throws TransformerException {
        return srcrdr.getElement(new StreamSource(new StringReader(s)));
    }
}
