package uk.ac.starlink.util;

import junit.framework.AssertionFailedError;

import java.util.Iterator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.TransformerException;
import junit.framework.AssertionFailedError;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.w3c.dom.*;

/**
 * Tests assertDOMEquals and assertSourceEquals
 *
 * @author   Norman Gray (Starlink)
 * @author   Mark Taylor (Starlink)
 */
public class TestCaseTest extends TestCase {

    private SourceReader srcrdr;

    public TestCaseTest( String name ) {
        super( name );
        srcrdr = new SourceReader();
        srcrdr.setIncludeDeclaration(false);
    }

    public void setUp() throws TransformerException {
    }

    public void testFlags()
            throws Exception {
        
        String plainString =
                "<top><n1/><n2></n2></top>";
        Element plain = StringToDom(plainString);

        assertDOMEquals(plain, StringToDom("<top><n1/><n2></n2></top>"));
        assertDOMEquals(plain,
                        StringToDom("<top><n1/><n2>   </n2></top>"),
                        null,
                        TestCase.IGNORE_WHITESPACE);
        assertDOMEquals(plain,
                        StringToDom("<top><n1/><!-- zip! --><n2><!-- nothing --></n2></top>"),
                        null,
                        TestCase.IGNORE_COMMENTS);
        assertDOMEquals(plain,
                        StringToDom("<top>   <n1/><!-- zip! --><n2>  <!-- nothing -->\t</n2></top>"),
                        null,
                        TestCase.IGNORE_COMMENTS|TestCase.IGNORE_WHITESPACE);
        // Again, but spaces/comments in the expected one
        String spacesString = "<top>   <n1/><!-- zip! --><n2>  <!-- nothing -->\t</n2></top>";
        assertDOMEquals(spacesString,
                        plain,
                        null,
                        TestCase.IGNORE_COMMENTS|TestCase.IGNORE_WHITESPACE);
        // Again, this time with input coming from a stream (in fact,
        // this is equivalent to the previous one, since strings are
        // turned into streams, but it's good to be explicit)
        assertDOMEquals(new java.io.ByteArrayInputStream
                        (spacesString.getBytes()) ,
                        plain,
                        null,
                        TestCase.IGNORE_COMMENTS|TestCase.IGNORE_WHITESPACE);

        // Tests which should fail:
        try {
            assertDOMEquals(plain, 
                            StringToDom("<top><n1/><n2>   </n2></top>"),
                            null,
                            0);
            fail("no assertion thrown for DOM with whitespace");
        } catch (AssertionFailedError ex) {
            // OK -- should happen
        }
        try {
            assertDOMEquals(plain, 
                            StringToDom("<top><n1/><n2><!-- X --></n2></top>"),
                            null,
                            0);
            fail("no assertion thrown for DOM with comment");
        } catch (AssertionFailedError ex) {
            // OK -- should happen
        }
    }

    public void testAssertSource()
            throws Exception {
        
        assertSourceEquals(new StreamSource(new StringReader
                                            ("<top><n1/><n2></n2></top>")),
                           new StreamSource(new StringReader
                                            ("<top><n1/><n2></n2></top>")),
                           null, 
                           0);
    }
    
    private Element StringToDom(String s) 
            throws TransformerException {
        return srcrdr.getElement(new StreamSource(new StringReader(s)));
    }

    public void testValidation() throws IOException, SAXException {
        String decl = new StringBuffer()
            .append( "<?xml version='1.0'?>" )
            .append( "<!DOCTYPE animal [" )
            .append( "<!ELEMENT animal EMPTY>" )
            .append( "<!ATTLIST animal species CDATA #REQUIRED>" )
            .append( "]>" )
            .toString();
        validateString( decl + "<animal species='rabbit'/>" );
        try {
            validateString( decl + "<animal species='cow' name='daisy'/>" );
            fail( "Parse should have failed" );
        }
        catch ( SAXException e ) {
            // ok
        }
    }

    private void validateString( String text )
            throws IOException, SAXException {
        assertValidXML( new InputSource( 
                            new ByteArrayInputStream( text.getBytes() ) ) );
    }
}
