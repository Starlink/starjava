/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     26-JUL-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.ast.gui;

import java.awt.Color;
import java.awt.Font;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import uk.ac.starlink.util.XMLEncodeAndDecode;
import uk.ac.starlink.util.PrimitiveXMLEncodeAndDecode;
import uk.ac.starlink.util.gui.AWTXMLEncodeAndDecode;
import uk.ac.starlink.util.gui.StoreConfiguration;

/**
 * This abstract class provides a default implementation for a
 * XMLEncodeAndDecode. An AbstractPlotControlsModel provides backing
 * store for the state of some controls that define properties that
 * are related to a {@link Plot}. The state of the model can be saved
 * and restored from an XML description.
 * <p>
 * This implementation also provides default implementations of {@link
 * Changelistener} methods that allow the model to register, respond
 * and issue {@link ChangeEvents}. It also forces any sub-classes to
 * provide methods for encoding and decoding their internal
 * configurations as XML Elements so that they can be written-to and
 * restored-from permanent store.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see XMLEncodeAndDecode
 * @see ChangeEvent
 * @see ChangeListener
 */
public abstract class AbstractPlotControlsModel
    implements XMLEncodeAndDecode
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
// Implementations of the XMLEncodeAndDecode methods.
//
    // Encode is object specific and has an empty default implementation.
    abstract public void encode( Element rootElement );

    //  A default implementation of decode use a setFromString
    //  implementation in the specific class.
    public void decode( Element rootElement )
    {
        List children = StoreConfiguration.getChildElements( rootElement );
        int size = children.size();
        Element element = null;
        String name = null;
        String value = null;
        for ( int i = 0; i < size; i++ ) {
            element = (Element) children.get( i );
            name = getElementName( element );
            value = getElementValue( element );
            setFromString( name, value );
        }
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
     * Add an element with String value as a child of another element.
     * The String is stored as CDATA.
     */
    protected void addChildElement( Element rootElement, String name,
                                    String value )
    {
        PrimitiveXMLEncodeAndDecode.addChildElement( rootElement, 
                                                     name, value );
    }

    /**
     * Add an element with boolean value as a child of another element.
     */
    protected void addChildElement( Element rootElement, String name,
                                    boolean value )
    {
        PrimitiveXMLEncodeAndDecode.addChildElement( rootElement,
                                                     name, value );
    }

    /**
     * Add an element with integer value as a child of another element.
     */
    protected void addChildElement( Element rootElement, String name,
                                    int value )
    {
        PrimitiveXMLEncodeAndDecode.addChildElement( rootElement,
                                                     name, value );
    }

    /**
     * Add an element with double value as a child of another element.
     */
    protected void addChildElement( Element rootElement, String name,
                                    double value )
    {
        PrimitiveXMLEncodeAndDecode.addChildElement( rootElement, name, 
                                                     value ); 
    }

    /**
     * Add an element with Color value as a child of another element.
     */
    protected void addChildElement( Element rootElement, String name,
                                    Color value )
    {
        AWTXMLEncodeAndDecode.addChildElement( rootElement, name, value);
    }

    /**
     * Add an element with Font value as a child of another element.
     */
    protected void addChildElement( Element rootElement, String name,
                                    Font value )
    {
        AWTXMLEncodeAndDecode.addChildElement( rootElement, name, value);
    }

    /**
     * Return a List of all children. Use the List interface to step
     * through these.
     */
    protected NodeList getChildren( Element rootElement )
    {
        return PrimitiveXMLEncodeAndDecode.getChildren( rootElement );
    }

    /**
     * Get the name of an element.
     */
    protected String getElementName( Element element )
    {
        return PrimitiveXMLEncodeAndDecode.getElementName( element );
    }

    /**
     * Get the "value" of an element (really the content).
     */
    protected String getElementValue( Element element )
    {
        return PrimitiveXMLEncodeAndDecode.getElementValue( element );
    }

    /**
     * Convert a Font to a string.
     */
    protected String fontToString( Font value )
    {
        return AWTXMLEncodeAndDecode.fontToString( value );
    }

    /**
     * Convert a String back to a Font.
     */
    protected Font fontFromString( String value )
    {
        return AWTXMLEncodeAndDecode.fontFromString( value );
    }

    /**
     * Convert a double to a String.
     */
    protected String doubleToString( double value )
    {
        return PrimitiveXMLEncodeAndDecode.doubleToString( value );
    }

    /**
     * Convert a String back to a double.
     */
    protected double doubleFromString( String value )
    {
        return PrimitiveXMLEncodeAndDecode.doubleFromString( value );
    }

    /**
     * Convert a boolean to a String.
     */
    protected String booleanToString( boolean value )
    {
        return PrimitiveXMLEncodeAndDecode.booleanToString( value );
    }

    /**
     * Convert a String back to a boolean.
     */
    protected boolean booleanFromString( String value )
    {
        return PrimitiveXMLEncodeAndDecode.booleanFromString( value );
    }

    /**
     * Convert an integer to a String.
     */
    protected String intToString( int value )
    {
        return PrimitiveXMLEncodeAndDecode.intToString( value );
    }

    /**
     * Convert a String back to an integer.
     */
    protected int intFromString( String value )
    {
        return PrimitiveXMLEncodeAndDecode.intFromString( value );
    }

    /**
     * Convert a Color object to a string.
     */
    protected String colorToString( Color value )
    {
        return AWTXMLEncodeAndDecode.colorToString( value );
    }

    /**
     * Convert a String object back to a Color object.
     */
    protected Color colorFromString( String value )
    {
        return AWTXMLEncodeAndDecode.colorFromString( value );
    }
}
