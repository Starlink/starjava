package uk.ac.starlink.ast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.TransformerException;
import junit.framework.TestCase;
import org.w3c.dom.Element;
import uk.ac.starlink.util.SourceReader;

public class XmlChanTest extends TestCase {

    private String confusingID = 
        "<test>test &lt; &quotone&quot <![CDATA[&&<<>>]]>" +
        " test 'two' </test>";

    public XmlChanTest( String name ) {
        super( name );
    }

    public void testEncoding() throws IOException {
        AstObject obj1 = new Frame( 1 );
        obj1.setID( confusingID );
        XmlChan xc = new MemoryXmlChan();
        assertEquals( 1, xc.write( obj1 ) );
        AstObject obj2 = xc.read();
        assertEquals( obj1.getID(), obj2.getID() );
    }

    public void testConstants() {
        assertEquals( "http://www.starlink.ac.uk/ast/xml/", 
                      XmlChan.AST__XMLNS );
    }

    public void testNamespace() throws TransformerException, IOException {
        Frame frame = new Frame( 2 );
        MemoryXmlChan chan = new MemoryXmlChan();
        assertEquals( 1, chan.write( frame ) );
        Element top = new SourceReader().getElement( chan.getSource() );
        assertEquals( "Frame", top.getNodeName() );
        assertEquals( XmlChan.AST__XMLNS, top.getNamespaceURI() );
    }

}
