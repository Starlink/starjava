/*
 * Copyright (C) 2000-2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     01-NOV-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.ast.gui;

import java.awt.Color;

import org.w3c.dom.Element;
import uk.ac.starlink.ast.grf.DefaultGrf;

/**
 * AstGrid is a model of the Grid element shown in say an AST Plot. It
 * encompasses all the values that describe its representation and returns
 * these in various formats (such as the complete AST Plot options list for
 * drawing it).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class AstGrid extends AbstractPlotControlsModel
{
    /**
     * Whether grid is set or unset.
     */
    protected boolean isSet;

    /**
     * Whether the grid should be shown (different from unset).
     */
    protected boolean show;

    /**
     * The colour of the grid.
     */
    protected Color colour;

    /**
     * The line style of the grid lines.
     */
    protected int style;

    /**
     * The width of the grid lines.
     */
    protected double width;

    /**
     * Suggested minimum width.
     */
    public static int MIN_WIDTH = 1;

    /**
     * Suggested maximum width.
     */
    public static int MAX_WIDTH = 20;


    /**
     * Create a empty instance. This indicates that the grid element should
     * remain at the AST Plot default.
     */
    public AstGrid()
    {
        setDefaults();
    }


    /**
     * Set object to default state.
     */
    public void setDefaults()
    {
        isSet = false;
        show = false;
        colour = Color.black;
        style = DefaultGrf.PLAIN;
        width = 1.0;
        fireChanged();
    }


    /**
     * Set whether the grid is set or unset (unset implies that all properties
     * should remain at their AST defaults).
     *
     * @param isSet The new state value
     */
    public void setState( boolean isSet )
    {
        this.isSet = isSet;
    }


    /**
     * Return if the grid is set or unset.
     *
     * @return The state value
     */
    public boolean getState()
    {
        return isSet;
    }


    /**
     * Set whether the grid should be shown or not.
     *
     * @param show The new shown value
     */
    public void setShown( boolean show )
    {
        this.show = show;
        setState( true );
        fireChanged();
    }


    /**
     * Get whether the grid is to be shown.
     *
     * @return The shown value
     */
    public boolean getShown()
    {
        return show;
    }


    /**
     * Set the colour of the grid.
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
     * Get the colour of the grid.
     *
     * @return The colour value
     */
    public Color getColour()
    {
        return colour;
    }


    /**
     * Set the line width. The value DefaultGrf.BAD means no value.
     *
     * @param width The new width value
     */
    public void setWidth( double width )
    {
        this.width = width;
        if ( width != DefaultGrf.BAD ) {
            setState( true );
        }
        fireChanged();
    }


    /**
     * Get the line width.
     *
     * @return The width value
     */
    public double getWidth()
    {
        return width;
    }


    /**
     * Set the line style. This should be a style known to the Grf class (i.e.
     * Grf.PLAIN, Grf.DASH etc.)
     *
     * @param style The new style value
     */
    public void setStyle( int style )
    {
        this.style = style;
        setState( true );
        fireChanged();
    }


    /**
     * Get the line style.
     *
     * @return The style value
     */
    public int getStyle()
    {
        return style;
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
        if ( show ) {
            buffer.append( "Grid=1" );
            if ( colour != null ) {
                buffer.append( ",Colour(grid)=" );
                int value = DefaultGrf.encodeColor( colour );
                if ( value == -1 ) {
                    value = 0;
                }
                buffer.append( value );
            }
            if ( width != DefaultGrf.BAD ) {
                buffer.append( ",Width(grid)=" );
                buffer.append( width );
            }
            buffer.append( ",Style(grid)=" );
            buffer.append( style );
        }
        else {
            buffer.append( "Grid=0" );
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
        return "grid";
    }

    /**
     * Description of the Method
     *
     * @param rootElement Description of the Parameter
     */
    public void encode( Element rootElement )
    {
        addChildElement( rootElement, "isSet", isSet );
        addChildElement( rootElement, "show", show );
        if ( colour != null ) {
            addChildElement( rootElement, "colour", colour );
        }
        addChildElement( rootElement, "style", style );
        addChildElement( rootElement, "width", width );
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
        if ( name.equals( "show" ) ) {
            setShown( booleanFromString( value ) );
            return;
        }
        if ( name.equals( "colour" ) ) {
            setColour( colorFromString( value ) );
            return;
        }
        if ( name.equals( "style" ) ) {
            setStyle( intFromString( value ) );
            return;
        }
        if ( name.equals( "width" ) ) {
            setWidth( doubleFromString( value ) );
            return;
        }
        System.err.println( "AstGrid: unknown configuration property:" +
            name + " (" + value + ")" );
    }
}
