package uk.ac.starlink.util;

import java.io.StringReader;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Provides some utility functions for working with {@link org.w3c.dom}
 * classes.
 *
 * @author   Mark Taylor (Starlink)
 */
public class DOMUtilsTest extends TestCase {

    private Element farm;

    public DOMUtilsTest( String name ) {
        super( name );
    }

    public void setUp() throws TransformerException {
        String xtext = 
              "<farm owner='Old MacDonald'>"
            + "<owner>Old <![CDATA[Mac]]>Donald</owner>"
            + "<animals>"
            + "<pigs><pig name='Crosspatch'/><pig name='Spot'/></pigs>"
            + "</animals>"
            + "</farm>";
        Source xsrc = new StreamSource( new StringReader( xtext ) );
        Document doc = (Document) new SourceReader().getDOM( xsrc );
        this.farm = doc.getDocumentElement();
    }

    public void testGetChildElementByName() {
        Element animals = DOMUtils.getChildElementByName( farm, "animals" );
        Element pigs = DOMUtils.getChildElementByName( animals, "pigs" );
        Element crosspatch = DOMUtils.getChildElementByName( pigs, "pig" );
        assertEquals( crosspatch.getAttribute( "name" ), "Crosspatch" );
        Element giraf = DOMUtils.getChildElementByName( animals, "giraffes" );
        assertNull( giraf );
    }

    public void testGetTextContent() {
        Element animals = DOMUtils.getChildElementByName( farm, "animals" );
        assertEquals( DOMUtils.getTextContent( animals ), "" );
        Element owner = DOMUtils.getChildElementByName( farm, "owner" );
        assertEquals( DOMUtils.getTextContent( owner ), "Old MacDonald" );
    }
}
