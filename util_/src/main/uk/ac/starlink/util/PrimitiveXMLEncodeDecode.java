/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     21-SEP-2002 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.util;

import java.util.ArrayList;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An abstract base-class for objects that want to encode and decode
 * themselves from an XML stream.
 * <p>
 * It contains the {@link XMLEncodeDecode} interface (with an
 * unimplemented encode() method) and a number of support methods for
 * encoding and decoding primitive values.
 * <p>
 * This implementation also provides default implementations of
 * {@link ChangeListener} methods that allow an extending class to
 * provide services for registering, responding and issuing
 * {@link ChangeEvent}s.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public abstract class PrimitiveXMLEncodeDecode
    implements XMLEncodeDecode
{
    //
    //  Define change listeners interface.
    //
    protected EventListenerList listeners = new EventListenerList();

    /**
     * Registers a listener who wants to be informed about changes.
     *
     *  @param l the ChangeListener listener.
     */
    public void addChangeListener( ChangeListener l )
    {
        listeners.add( ChangeListener.class, l );
    }

    /**
     * De-registers a listener for changes.
     *
     *  @param l the ChangeListener listener.
     */
    public void removeChangeListener( ChangeListener l )
    {
        listeners.remove( ChangeListener.class, l );
    }

    /**
     * Send ChangeEvent event to all listeners.
     */
    protected void fireChanged()
    {
        Object[] la = listeners.getListenerList();
        ChangeEvent e = null;
        for ( int i = la.length - 2; i >= 0; i -= 2 ) {
            if ( la[i] == ChangeListener.class ) {
                if ( e == null ) {
                    e = new ChangeEvent( this );
                }
                ((ChangeListener)la[i+1]).stateChanged( e );
            }
        }
    }

    //
    // Implementations of the XMLEncodeDecode methods.
    //

    // Encode is object specific and has an empty default implementation.
    abstract public void encode( Element rootElement );

    //  A default implementation of decode use a setFromString
    //  implementation in the specific class.
    public void decode( Element rootElement )
    {
        List<Element> children = getChildElements( rootElement );
        int size = children.size();
        Element element = null;
        String name = null;
        String value = null;
        for ( int i = 0; i < size; i++ ) {
            element = children.get( i );
            name = getElementName( element );
            value = getElementValue( element );
            setFromString( name, value );
        }
    }

    /**
     * Return all the Elements that are children of another Element.
     */
    public static List<Element> getChildElements( Element element )
    {
        NodeList nodeList = element.getChildNodes();
        List<Element> elementList = new ArrayList<Element>();
        for ( int i = 0; i < nodeList.getLength(); i++ ) {
            if ( nodeList.item( i ) instanceof Element ) {
                elementList.add( (Element) nodeList.item( i ) );
            }
        }
        return elementList;
    }

    /**
     * Set the value of a object field using string representation of
     * the field name and its value. Users of the default decode
     * implementation must re-implement this method.
     */
    abstract public void setFromString( String name, String value );

    //
    // Utilities for adding new elements to another element.
    //

    /**
     * Create a child element with no content and add it to the given
     * element.
     */
    public static Element addChildElement( Element rootElement, 
                                           String name )
    {
        Document parent = rootElement.getOwnerDocument();
        Element element = parent.createElement( name );
        rootElement.appendChild( element );
        return element;
    }

    /**
     * Create a new CDATA section with the given content and add it as
     * a child of a given element.
     */
    public static CDATASection addCDATASection( Element rootElement, 
                                                String value )
    {
        Document parent = rootElement.getOwnerDocument();
        CDATASection cdata = parent.createCDATASection( value );
        rootElement.appendChild( cdata );
        return cdata;
    }

    /**
     * Add an element with String value as a child of another element.
     * The String is stored as CDATA.
     */
    public static Element addChildElement( Element rootElement, String name,
                                           String value )
    {
        Element newElement = addChildElement( rootElement, name );
        if ( value != null ) {
            addCDATASection( newElement, value );
        }
        return newElement;
    }

    /**
     * Add an element with boolean value as a child of another element.
     */
    public static Element addChildElement( Element rootElement, String name,
                                           boolean value )
    {
        return addChildElement( rootElement, name, booleanToString( value ) );
    }

    /**
     * Add an element with integer value as a child of another element.
     */
    public static Element addChildElement( Element rootElement, String name,
                                          int value )
    {
        return addChildElement( rootElement, name, intToString( value ) );
    }

    /**
     * Add an element with double value as a child of another element.
     */
    public static Element addChildElement( Element rootElement, String name,
                                           double value )
    {
        return addChildElement( rootElement, name, doubleToString( value ) );
    }

    /**
     * Return a List of all children. Use the NodeList interface to
     * step through these.
     */
    public static NodeList getChildren( Element rootElement )
    {
        return rootElement.getChildNodes();
    }

    /**
     * Get the name of an element.
     */
    public static String getElementName( Element element )
    {
        return element.getTagName();
    }

    /**
     * Get the "value" of an element (really the content).
     */
    public static String getElementValue( Element element )
    {
        // Value is the content, should be a text/CDATA node.
        Node firstChild = element.getFirstChild();
        if ( firstChild != null ) {
            return firstChild.getNodeValue();
        }
        return null;
    }

    /**
     * Convert a double to a String.
     */
    public static String doubleToString( double value )
    {
        return Double.toString( value );
    }

    /**
     * Convert a String back to a double.
     */
    public static double doubleFromString( String value )
    {
        return Double.parseDouble( value );
    }

    /**
     * Convert a boolean to a String.
     */
    public static String booleanToString( boolean value )
    {
        return Boolean.toString( value );
    }

    /**
     * Convert a String back to a boolean.
     */
    public static boolean booleanFromString( String value )
    {
        return Boolean.parseBoolean( value );
    }

    /**
     * Convert an integer to a String.
     */
    public static String intToString( int value )
    {
        return Integer.toString( value );
    }

    /**
     * Convert a String back to an integer.
     */
    public static int intFromString( String value )
    {
        return Integer.parseInt( value );
    }

    /**
     * Convert a long to a String.
     */
    public static String longToString( long value )
    {
        return Long.toString( value );
    }

    /**
     * Convert a String back to a long.
     */
    public static long longFromString( String value )
    {
        return Long.parseLong( value );
    }

}
