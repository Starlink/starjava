/*
 * Copyright (C) 2000-2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     01-NOV-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.ast.gui;

import java.awt.Color;
import java.awt.Font;

import org.w3c.dom.Element;
import uk.ac.starlink.ast.grf.DefaultGrf;
import uk.ac.starlink.ast.grf.DefaultGrfFontManager;

/**
 * AstTitle is a model of the Title element shown in say an AST Plot. It
 * encompasses all the values that describe its representation and returns
 * these in various formats (such as the complete AST Plot options list for
 * drawing it).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class AstTitle 
    extends AbstractPlotControlsModel
{
    /**
     * Whether title is set or unset.
     */
    protected boolean isSet;

    /**
     * Whether the title should be shown (different from unset).
     */
    protected boolean show;

    /**
     * The value for the title.
     */
    protected String title;

    /**
     * The Font used to display the title.
     */
    protected Font font;

    /**
     * The colour of the title.
     */
    protected Color colour;

    /**
     * The gap between title and plot border.
     */
    protected double gap;

    /**
     * The suggested minimum gap.
     */
    public final static double GAP_MIN = -0.5;

    /**
     * The suggested maximum gap.
     */
    public final static double GAP_MAX = 0.5;

    /**
     * The suggested gap resolution (i.e. interval between steps).
     */
    public final static double GAP_STEP = 0.005;

    /**
     * A title to show that is the same as null.
     */
    public final static String NULL_TITLE = "Using default title";

    /**
     * Reference to the GrfFontManager object. Use this to add and remove
     * fonts from the global list. Also provides the index of the font as
     * known to Grf.
     */
    protected DefaultGrfFontManager grfFontManager = DefaultGrfFontManager.getReference();


    /**
     * Create a empty instance. This indicates that the title element should
     * remain at the AST Plot default.
     */
    public AstTitle()
    {
        setDefaults();
    }


    /**
     * Create an instance with initial value.
     *
     * @param title Description of the Parameter
     */
    public AstTitle( String title )
    {
        setDefaults();
        setTitle( title );
    }


    /**
     * Set/reset all values to their defaults.
     */
    public void setDefaults()
    {
        isSet = false;
        show = true;
        title = NULL_TITLE;
        colour = Color.black;
        font = null;
        gap = 0.05;
        fireChanged();
    }


    /**
     * Set whether the title is set or unset (unset implies that all title
     * properties should remain at their AST defaults).
     *
     * @param isSet The new state value
     */
    public void setState( boolean isSet )
    {
        this.isSet = isSet;
        fireChanged();
    }


    /**
     * Return if the title is set or unset.
     *
     * @return The state value
     */
    public boolean getState()
    {
        return isSet;
    }


    /**
     * Set whether the title should be shown or not.
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
     * Get whether the title is to be shown.
     *
     * @return The shown value
     */
    public boolean getShown()
    {
        return show;
    }


    /**
     * Set the title string. If null then title is unset.
     *
     * @param title The new title value
     */
    public void setTitle( String title )
    {
        this.title = title;
        if ( title != null && ! title.equals( NULL_TITLE ) ) {
            setState( true );
        }
        fireChanged();
    }


    /**
     * Get the current title.
     *
     * @return The title value
     */
    public String getTitle()
    {
        return title;
    }


    /**
     * Set the Font to be used when displaying title.
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
     * Get the Font used to draw title.
     *
     * @return The font value
     */
    public Font getFont()
    {
        return font;
    }


    /**
     * Set the colour of the title.
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
     * Get the colour of the title.
     *
     * @return The colour value
     */
    public Color getColour()
    {
        return colour;
    }


    /**
     * Set the gap between title and border. The value DefaultGrf.BAD means no
     * value.
     *
     * @param gap The new gap value
     */
    public void setGap( double gap )
    {
        this.gap = gap;
        if ( gap != DefaultGrf.BAD ) {
            setState( true );
        }
        fireChanged();
    }


    /**
     * Get the gap between title and border.
     *
     * @return The gap value
     */
    public double getGap()
    {
        return gap;
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
            buffer.append( "DrawTitle=1" );
            if ( title != null && ! title.equals( "" ) &&
                ! title.equals( NULL_TITLE ) ) {
                buffer.append( ",title=" );
                buffer.append( title );
            }
            if ( font != null ) {
                buffer.append( ",Font(Title)=" );
                buffer.append( grfFontManager.getIndex( font ) );
            }
            if ( colour != null ) {
                buffer.append( ",Colour(title)=" );
                int value = DefaultGrf.encodeColor( colour );
                if ( value == -1 ) {
                    value = 0;
                }
                buffer.append( value );
            }
            if ( gap != DefaultGrf.BAD ) {
                buffer.append( ",TitleGap=" );
                buffer.append( gap );
            }
        }
        else {
            buffer.append( "DrawTitle=0" );
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
        return "title";
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
        addChildElement( rootElement, "title", title );
        if ( font != null ) {
            addChildElement( rootElement, "font", font );
        }
        if ( colour != null ) {
            addChildElement( rootElement, "colour", colour );
        }
        addChildElement( rootElement, "gap", gap );
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
        if ( name.equals( "title" ) ) {
            setTitle( value );
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
        if ( name.equals( "gap" ) ) {
            setGap( doubleFromString( value ) );
            return;
        }
        System.err.println( "AstTitle: unknown configuration property:" +
            name + " (" + value + ")" );
    }
}
