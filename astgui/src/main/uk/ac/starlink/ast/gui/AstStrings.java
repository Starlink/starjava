/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     31-JUL-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.ast.gui;

import java.awt.Color;
import java.awt.Font;

import org.w3c.dom.Element;
import uk.ac.starlink.ast.grf.DefaultGrf;
import uk.ac.starlink.ast.grf.DefaultGrfFontManager;

/**
 * AstStrings is a model for all the general text elements shown in
 * an AST Plot (i.e. those with element name "strings"). It
 * encompasses all the values that describe its representation and
 * returns these in various formats (such as the complete AST Plot
 * options list for drawing it).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class AstStrings
    extends AbstractPlotControlsModel
{
    /**
     * Whether values are set or unset.
     */
    protected boolean isSet;

    /**
     * The Font used to display text.
     */
    protected Font font;

    /**
     * The colour of text
     */
    protected Color colour;

    /**
     * Reference to the GrfFontManager object. Use this to add and remove
     * fonts from the global list. Also provides the index of the font as
     * known to Grf.
     */
    protected DefaultGrfFontManager grfFontManager = 
        DefaultGrfFontManager.getReference();

    /**
     * Create a empty instance. This indicates that string elements should
     * be drawn using the AST defaults.
     */
    public AstStrings()
    {
        setDefaults();
    }

    /**
     * Set/reset all values to their defaults.
     */
    public void setDefaults()
    {
        isSet = false;
        colour = Color.black;
        font = null;
        fireChanged();
    }

    /**
     * Set whether this objects state is set or unset (unset implies
     * that all properties should remain at their AST defaults).
     *
     * @param isSet The new state value
     */
    public void setState( boolean isSet )
    {
        this.isSet = isSet;
        fireChanged();
    }

    /**
     * Return if this object is set or unset.
     *
     * @return The state value
     */
    public boolean getState()
    {
        return isSet;
    }

    /**
     * Set the Font to be used.
     *
     * @param font The new font value
     */
    public void setFont( Font font )
    {
        //  Make sure the new font is available in the graphics
        //  interface and release our hold on the old one.
        if ( font != null ) {
            setState( true );
            grfFontManager.add( font );
        }
        if ( this.font != null ) {
            grfFontManager.remove( this.font );
        }
        this.font = font;
        fireChanged();
    }

    /**
     * Get the Font used.
     *
     * @return The font value
     */
    public Font getFont()
    {
        return font;
    }

    /**
     * Set the colour.
     *
     * @param colour The new colour value
     */
    public void setColour( Color colour )
    {
        this.colour = colour;
        if ( colour != null ) {
            setState( true );
        }
        fireChanged();
    }

    /**
     * Get the colour.
     *
     * @return The colour value
     */
    public Color getColour()
    {
        return colour;
    }

    /**
     * Get the AST plot options description of this object.
     *
     * @return The astOptions value
     */
    public String getAstOptions()
    {
        if ( ! isSet ) {
            return "";
        }

        StringBuffer buffer = new StringBuffer();
        if ( font != null ) {
            buffer.append( "Font(strings)=" );
            buffer.append( grfFontManager.getIndex( font ) );
            buffer.append( "," );
        }
        if ( colour != null ) {
            buffer.append( "Colour(strings)=" );
            int value = DefaultGrf.encodeColor( colour );
            if ( value == -1 ) {
                value = 0;
            }
            buffer.append( value );
        }
        return buffer.toString();
    }

    /**
     * Get a string representation of the AST options.
     *
     * @return Description of the Return Value
     */
    public String toString()
    {
        return getAstOptions();
    }


//
// Encode and decode this object to/from XML representation.
//
    /**
     * The name of our enclosing tag.
     */
    public String getTagName()
    {
        return "text";
    }

    /**
     * Description of the Method
     *
     * @param rootElement Description of the Parameter
     */
    public void encode( Element rootElement )
    {
        addChildElement( rootElement, "isSet", isSet );
        if ( font != null ) {
            addChildElement( rootElement, "font", font );
        }
        if ( colour != null ) {
            addChildElement( rootElement, "colour", colour );
        }
    }


    /**
     * Set the value of a member variable by matching its name to a known
     * local property string.
     *
     * @param name The new fromString value
     * @param value The new fromString value
     */
    public void setFromString( String name, String value )
    {
        if ( name.equals( "isSet" ) ) {
            setState( booleanFromString( value ) );
            return;
        }
        if ( name.equals( "font" ) ) {
            setFont( fontFromString( value ) );
            return;
        }
        if ( name.equals( "colour" ) ) {
            setColour( colorFromString( value ) );
            return;
        }
        System.err.println( "AstStrings: unknown configuration property:" +
            name + " (" + value + ")" );
    }
}
