package uk.ac.starlink.splat.iface;

import java.awt.Color;

import org.jdom.Element;

import uk.ac.starlink.splat.util.AbstractStorableConfig;

/**
 * Store a java.awt.Color. Offers the ability for ChangeListeners to
 * be informed when the Color changes and the encode and decode of the
 * Color as an integer XML snippet.
 *
 * @since $Date$
 * @since 10-OCT-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 */
public class ColourStore extends AbstractStorableConfig
{
    /** 
     * The Color.
     */
    protected Color colour = Color.white;

    /**
     * Create an instance with the default color.
     */
    public ColourStore()
    {
        fireChanged();
    }

    /**
     * Create an instance with a given colour.
     */
    public ColourStore( Color colour )
    {
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
     * Set the colour as an RGB integer.
     */
    public void setIntColour( int icolour )
    {
        colour = new Color( icolour );
        fireChanged();
    }

    /**
     * Get the colour as an RGB integer.
     */
    public int getIntColour()
    {
        return colour.getRGB();
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
