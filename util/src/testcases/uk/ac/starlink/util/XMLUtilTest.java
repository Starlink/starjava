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

    class TestXMLEncodeAndDecode implements XMLEncodeAndDecode
    {
        // The configuration.
        String value1 = "Zebra";
        String value2 = "Horse";

        TestXMLEncodeAndDecode()
        {
            // Do nothing.
        }

        public boolean sameValue( TestXMLEncodeAndDecode comparison )
        {
            if ( value1.equals( comparison.value1 ) &&
                 value2.equals( comparison.value2 ) ) {
                return true;
            }
            return false;
        }

        public void encode( Element rootElement ) 
        {
            addChildElement( rootElement, "animal1", value1 );
            addChildElement( rootElement, "animal2", value2 );
        }

        public void decode( Element rootElement ) 
        {
            List children = getChildElements( rootElement );
            int size = children.size();
            Element element = null;
            String name = null;
            String value = null;
            for ( int i = 0; i < size; i++ ) {
                element = (Element) children.get( i );
                name = element.getTagName();
                value = element.getFirstChild().getNodeValue();

                //  Set the value...
                if ( "animal1".equals( name ) ) {
                    value1 = value;
                }
                else if ( "animal2".equals( name ) ) {
                    value2 = value;
                }
            }
        }
        
        public List getChildElements( Element element )
        {
            NodeList nodeList = element.getChildNodes();
            List elementList = new ArrayList();
            for ( int i = 0; i < nodeList.getLength(); i++ ) {
                if ( nodeList.item( i ) instanceof Element ) {
                    elementList.add( nodeList.item( i ) );
                }
            }
            return elementList;
        }

        public String getTagName() 
        {
            return "test";
        }
        
        protected void addChildElement( Element rootElement, String name,
                                        String value )
        {
            Document parent = rootElement.getOwnerDocument();
            Element newElement = parent.createElement( name );
            if ( value != null ) {
                CDATASection cdata = parent.createCDATASection( value );
                newElement.appendChild( cdata );
            }
            rootElement.appendChild( newElement );
        }
    }
}
