package uk.ac.starlink.util;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class XMLUtilTest extends TestCase 
{
    public XMLUtilTest( String name ) 
    {
        super( name );
    }
        
    public void testXMLEncodeAndDecode()
        throws ParserConfigurationException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        TestXMLEncodeAndDecode obj1 = new TestXMLEncodeAndDecode();
        Element rootElement = doc.createElement( obj1.getTagName() );
        obj1.encode( rootElement );

        TestXMLEncodeAndDecode obj2 = new TestXMLEncodeAndDecode();
        obj2.decode( rootElement );
        assertTrue( obj1.sameValue( obj2 ) );
        assertTrue( obj2.sameValue( obj1 ) );
    }

    // Create a class whose instances can be encoded and decoded to an
    // Element.
    class TestXMLEncodeAndDecode extends PrimitiveXMLEncodeAndDecode
    {
        // The configuration, one for each primitive type known.
        String  value1 = "Java";
        double  value2 = Double.MAX_VALUE;
        int     value3 = Integer.MIN_VALUE;
        boolean value4 = false;
        

        TestXMLEncodeAndDecode()
        {
            // Do nothing.
        }

        public boolean sameValue( TestXMLEncodeAndDecode comparison )
        {
            if ( value1.equals( comparison.value1 ) &&
                 ( Double.compare( value2, comparison.value2 ) == 0 ) &&
                 value3 == comparison.value3 &&
                 value4 == comparison.value4 ) {
                return true;
            }
            return false;
        }

        public void encode( Element rootElement ) 
        {
            addChildElement( rootElement, "name1", value1 );
            addChildElement( rootElement, "name2", value2 );
            addChildElement( rootElement, "name3", value3 );
            addChildElement( rootElement, "name4", value4 );
        }

        public String getTagName() 
        {
            return "test";
        }

        public void setFromString( String name, String value )
        {
            if ( name.equals( "name1" ) ) {
                value1 = value;
                return;
            }
            if ( name.equals( "name2" ) ) {
                value2 = doubleFromString( value );
                return;
            }
            if ( name.equals( "name3" ) ) {
                value3 = intFromString( value );
                return;
            }
            if ( name.equals( "name4" ) ) {
                value4 = booleanFromString( value );
                return;
            }
            throw new RuntimeException( "unknown name: "+name+" ("+value+")");
        }
    }
}
