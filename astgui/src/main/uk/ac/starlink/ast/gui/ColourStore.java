/*
 * Copyright (C) 2001-2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     10-OCT-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.ast.gui;

import uk.ac.starlink.ast.grf.DefaultGrf;
import java.awt.Color;
import org.w3c.dom.Element;

/**
 * Store a java.awt.Color. Offers the ability for ChangeListeners to
 * be informed when the Color changes and the encode and decode of the
 * Color as an integer XML snippet. 
 * <p>
 * This model is not directly related an AST plot.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class ColourStore extends AbstractPlotControlsModel
{
    /** 
     * The Color.
     */
    protected Color colour = Color.white;

    /**
     * Name of the tag.
     */
    protected String tagName = "componentcolor";

    /**
     * Create an instance with the default color and tag name.
     */
    public ColourStore()
    {
        setTagName( tagName );
        setColor( colour );
    }

    /**
     * Create an instance with the default color.
     * The tagName defines what the colour is stored as.
     */
    public ColourStore( String tagName )
    {
        setTagName( tagName );
        setColor( colour );
    }

    /**
     * Create an instance with a given colour.
     */
    public ColourStore( String tagName, Color colour )
    {
        setTagName( tagName );
        setColor( colour );
    }

    /**
     * Set the colour.
     */
    public void setColor( Color colour )
    {
        this.colour = colour;
        fireChanged();
    }

    /**
     * Get the colour.
     */
    public Color getColour()
    {
        return colour;
    }
    
    /**
     * Set the colour from an RGB integer (supplied from DefaultGrf).
     */
    public void setIntColour( int icolour )
    {
        colour = DefaultGrf.decodeColor( icolour );
        fireChanged();
    }

    /**
     * Get the colour as an RGB integer (can be used by DefaultGrf).
     */
    public int getIntColour()
    {
        return DefaultGrf.encodeColor( colour );
    }

    /**
     * The name of our enclosing tag.
     */
    public String getTagName()
    {
        return tagName;
    }

    /**
     * The name of our enclosing tag.
     */
    public void setTagName( String tagName )
    {
        this.tagName = tagName;
    }

    /**
     * Encode this value in an XML snippet.
     */
    public void encode( Element rootElement ) 
    {
        addChildElement( rootElement, "colour", colour );
    }

    /**
     * Set the colour from an integer encode as a String.
     */
    public void setFromString( String name, String value ) 
    {
        setIntColour( intFromString( value ) );
    }
}
