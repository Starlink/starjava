package uk.ac.starlink.splat.util;

import java.awt.Color;
import java.awt.Font;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import org.jdom.CDATA;
import org.jdom.Element;

/**
 * This abstract class provides default implementations for objects
 * that have a simple name value internal configuration and that need
 * the ability to register, respond and issue ChangeEvents. It also
 * forces any sub-classes to provide methods for encoding and decoding
 * their internal configurations as XML Elements so that they can be
 * written-to and restored-from permanent store.
 *
 * @since $Date$
 * @since 26-JUL-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 * @see #XMLEncodeAndDecode, ChangeEvent, ChangeListener.
 */
public abstract class AbstractStorableConfig
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
        java.util.List children = getChildren( rootElement );
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
        Element newElement = new Element( name );
        if ( value != null ) {
            newElement.addContent( new CDATA( value ) );
        }
        rootElement.addContent( newElement );
    }

    /**
     * Add an element with boolean value as a child of another element.
     */
    protected void addChildElement( Element rootElement, String name,
                                    boolean value )
    {
        rootElement.addContent
            ( new Element( name ).setText( booleanToString( value ) ) );
    }

    /**
     * Add an element with integer value as a child of another element.
     */
    protected void addChildElement( Element rootElement, String name,
                                    int value )
    {
        rootElement.addContent
            ( new Element( name ).setText( intToString( value ) ) );
    }

    /**
     * Add an element with double value as a child of another element.
     */
    protected void addChildElement( Element rootElement, String name,
                                    double value )
    {
        rootElement.addContent
            ( new Element( name ).setText( doubleToString( value ) ) );
    }

    /**
     * Add an element with Color value as a child of another element.
     */
    protected void addChildElement( Element rootElement, String name,
                                    Color value )
    {
        rootElement.addContent
            ( new Element( name ).setText( colorToString( value ) ) );
    }

    /**
     * Add an element with Font value as a child of another element.
     */
    protected void addChildElement( Element rootElement, String name,
                                    Font value )
    {
        rootElement.addContent
            ( new Element( name ).setText( fontToString( value ) ) );
    }

    /**
     * Return a List of all children. Use the List interface to step
     * through these.
     */
    protected List getChildren( Element rootElement )
    {
        return rootElement.getChildren();
    }

    /**
     * Get the name of an element.
     */
    protected String getElementName( Element element )
    {
        return element.getName();
    }

    /**
     * Get the "value" of an element (really the content).
     */
    protected String getElementValue( Element element )
    {
        return element.getText();
    }

    /**
     * Convert a Font to a string.
     */
    protected String fontToString( Font font )
    {
        String style;
        switch ( font.getStyle() ) {
           case Font.BOLD | Font.ITALIC:
               style =  "bolditalic";
               break;
           case Font.BOLD:
               style = "bold";
               break;
           case Font.ITALIC:
               style = "italic";
               break;
           default:
               style = "plain";
        }
        return font.getFamily() + "-" + style + "-" + font.getSize();
    }

    /**
     * Convert a String back to a Font.
     */
    protected Font fontFromString( String value )
    {
        return Font.decode( value );
    }

    /**
     * Convert a double to a String.
     */
    protected String doubleToString( double value )
    {
        return new Double( value ).toString();
    }

    /**
     * Convert a String back to a double.
     */
    protected double doubleFromString( String value )
    {
        return Double.parseDouble( value );
    }

    /**
     * Convert a boolean to a String.
     */
    protected String booleanToString( boolean value )
    {
        return new Boolean( value ).toString();
    }

    /**
     * Convert a String back to a boolean.
     */
    protected boolean booleanFromString( String value )
    {
        return new Boolean( value ).booleanValue();
    }

    /**
     * Convert an integer to a String.
     */
    protected String intToString( int value )
    {
        return new Integer( value ).toString();
    }

    /**
     * Convert a String back to an integer.
     */
    protected int intFromString( String value )
    {
        return Integer.parseInt( value );
    }

    /**
     * Convert a Color object to a string.
     */
    protected String colorToString( Color value )
    {
        return new Integer( value.getRGB() ).toString();
    }

    /**
     * Convert a String object back to a Color object.
     */
    protected Color colorFromString( String value )
    {
        return new Color( intFromString( value ) );
    }
}
