package uk.ac.starlink.util;

import junit.framework.AssertionFailedError;

import java.util.Iterator;
import java.io.StringReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.TransformerException;

import org.w3c.dom.*;

/**
 * Tests assertDOMEquals and assertSourceEquals
 *
 * @author   Norman Gray (Starlink)
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
        
        String inputPlain =
                "<top><n1/><n2></n2></top>";
        Element plain = StringToDom(inputPlain);

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
}
