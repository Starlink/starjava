 package uk.ac.starlink.splat.ast;

import java.awt.Color;

import org.jdom.Element;
import uk.ac.starlink.splat.util.AbstractStorableConfig;
import uk.ac.starlink.ast.grf.DefaultGrf;

/**
 * AstBorder is a model of the Border element shown in say an AST Plot. It
 * encompasses all the values that describe its representation and returns
 * these in various formats (such as the complete AST Plot options list for
 * drawing it).
 *
 * @author Peter W. Draper
 * @created May 31, 2002
 * @since $Date$
 * @since 01-NOV-2000
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 * @version $Id$
 */
public class AstBorder extends AbstractStorableConfig
{
    /**
     * Whether border is set or unset.
     */
    protected boolean isSet;

    /**
     * Whether the border should be shown (different from unset).
     */
    protected boolean show;

    /**
     * The colour of the border.
     */
    protected Color colour;

    /**
     * The line style of the border lines.
     */
    protected int style;

    /**
     * The width of the border lines.
     */
    protected double width;


    /**
     * Create a empty instance. This indicates that the border element should
     * remain at the AST Plot default.
     */
    public AstBorder()
    {
        setDefaults();
    }


    /**
     * Set/reset all values to their defaults.
     */
    public void setDefaults()
    {
        isSet = false;
        show = true;
        colour = null;
        style = DefaultGrf.PLAIN;
        width = DefaultGrf.BAD;
        fireChanged();
    }


    /**
     * Set whether the border is set or unset (unset implies that all
     * properties should remain at their AST defaults).
     *
     * @param isSet The new state value
     */
    public void setState( boolean isSet )
    {
        this.isSet = isSet;
    }


    /**
     * Return if the border is set or unset.
     *
     * @return The state value
     */
    public boolean getState()
    {
        return isSet;
    }


    /**
     * Set whether the border should be shown or not.
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
     * Get whether the border is to be shown.
     *
     * @return The shown value
     */
    public boolean getShown()
    {
        return show;
    }


    /**
     * Set the colour of the border.
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
     * Get the colour of the border.
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
     * DefaultGrf.PLAIN, DefaultGrf.DASH etc.)
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
    public double getStyle()
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
            buffer.append( "Border=1" );
            if ( colour != null ) {
                buffer.append( ",Colour(border)=" );
                buffer.append( colour.getRGB() );
            }
            if ( width != DefaultGrf.BAD ) {
                buffer.append( ",Width(border)=" );
                buffer.append( width );
            }
            buffer.append( ",Style(border)=" );
            buffer.append( style );
        }
        else {
            buffer.append( "Border=0" );
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
        System.err.println( "AstBorder: unknown configuration property:" +
            name + " (" + value + ")" );

    }
}
