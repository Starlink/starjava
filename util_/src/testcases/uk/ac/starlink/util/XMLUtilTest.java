package uk.ac.starlink.util;

import java.awt.Font;
import java.awt.Color;
import java.awt.AlphaComposite;

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

import uk.ac.starlink.util.gui.AWTXMLEncodeDecode;

public class XMLUtilTest extends TestCase 
{
    public XMLUtilTest( String name ) 
    {
        super( name );
    }
        
    public void testXMLEncodeDecode()
        throws ParserConfigurationException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        TestXMLEncodeDecode obj1 = new TestXMLEncodeDecode();
        Element rootElement = doc.createElement( obj1.getTagName() );
        obj1.encode( rootElement );

        TestXMLEncodeDecode obj2 = new TestXMLEncodeDecode();
        obj2.decode( rootElement );
        assertTrue( obj1.sameValue( obj2 ) );
        assertTrue( obj2.sameValue( obj1 ) );
    }

    // Create a class whose instances can be encoded and decoded to an
    // Element.
    class TestXMLEncodeDecode extends PrimitiveXMLEncodeDecode
    {
        // The configuration, one for each primitive type known.
        String  value1 = "Java";
        double  value2 = Double.MAX_VALUE;
        int     value3 = Integer.MIN_VALUE;
        boolean value4 = false;
        Font    value5 = new Font( "SansSerif", Font.BOLD, 32 );
        Color   value6 = Color.red;
        AlphaComposite value7 = 
            AlphaComposite.getInstance( AlphaComposite.SRC_ATOP, 0.7999F );

        TestXMLEncodeDecode()
        {
            // Do nothing.
        }

        public boolean sameValue( TestXMLEncodeDecode comparison )
        {
            if ( value1.equals( comparison.value1 ) &&
                 ( Double.compare( value2, comparison.value2 ) == 0 ) &&
                 value3 == comparison.value3 &&
                 value4 == comparison.value4 &&
                 value5.equals( comparison.value5 ) &&
                 value6.equals( comparison.value6 ) &&
                 value7.equals( comparison.value7 ) ) {
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
            AWTXMLEncodeDecode.addChildElement( rootElement,
                                                "name5", value5 );
            AWTXMLEncodeDecode.addChildElement( rootElement,
                                                "name6", value6 );
            AWTXMLEncodeDecode.addChildElement( rootElement, 
                                                "name7", value7 );
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
            if ( name.equals( "name5" ) ) {
                value5 = AWTXMLEncodeDecode.fontFromString( value );
                return;
            }
            if ( name.equals( "name6" ) ) {
                value6 = AWTXMLEncodeDecode.colorFromString( value );
                return;
            }
            if ( name.equals( "name7" ) ) {
                value7 = AWTXMLEncodeDecode.compositeFromString( value );
                return;
            }
            throw new RuntimeException( "unknown name: "+name+" ("+value+")");
        }
    }
}
