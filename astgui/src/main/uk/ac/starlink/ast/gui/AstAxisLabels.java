/*
 * Copyright (C) 2000-2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     01-NOV-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.ast.gui;

import java.awt.Font;
import java.awt.Color;

import org.w3c.dom.Element;

import uk.ac.starlink.ast.grf.DefaultGrf;
import uk.ac.starlink.ast.grf.DefaultGrfFontManager;

/**
 * AstAxisLabel is a model of the Axis label elements shown in say an AST
 * Plot. It encompasses all the values that describe their representations
 * (which are not independent, hence what seems to be two elements in one
 * class) and returns these in various formats (such as the complete AST Plot
 * options list for configuring it).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class AstAxisLabels extends AbstractPlotControlsModel
{
    /**
     * Whether X label is set or unset.
     */
    protected boolean isXSet;

    /**
     * Whether Y label is set or unset.
     */
    protected boolean isYSet;

    /**
     * Whether X label should be shown.
     */
    protected boolean xShown;

    /**
     * Whether Y label should be shown.
     */
    protected boolean yShown;

    /**
     * The value for the X label.
     */
    protected String xLabel;

    /**
     * The value for the Y label.
     */
    protected String yLabel;

    /**
     * The Font used to display the labels.
     */
    protected Font font;

    /**
     * The colour of the labels.
     */
    protected Color colour;

    /**
     * The gap between X label and plot border.
     */
    protected double xGap;

    /**
     * The gap between Y label and the plot border.
     */
    protected double yGap;

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
     * A label to show that is the same as null.
     */
    public final static String NULL_LABEL = "Using default label";

    /**
     * The edge that the X label should be displayed on.
     */
    protected int xEdge;

    /**
     * The edge that the Y label should be displayed on.
     */
    protected int yEdge;

    /**
     * Whether the X label should also have the units displayed.
     */
    protected boolean showXUnits;

    /**
     * Whether the Y label should also have the units displayed.
     */
    protected boolean showYUnits;

    /**
     * Enumerations of the possible edges.
     */
    public final static int LEFT = 1;
    /**
     * Description of the Field
     */
    public final static int RIGHT = 2;
    /**
     * Description of the Field
     */
    public final static int TOP = 3;
    /**
     * Description of the Field
     */
    public final static int BOTTOM = 4;

    /**
     * Reference to the GrfFontManager object. Use this to add and remove
     * fonts from the global list. Also provides the index of the font as
     * known to Grf.
     */
    protected DefaultGrfFontManager grfFontManager = DefaultGrfFontManager.getReference();


    /**
     * Create an empty instance.
     */
    public AstAxisLabels()
    {
        setDefaults();
    }


    /**
     * Create an instance with initial values.
     *
     * @param xLabel Description of the Parameter
     * @param yLabel Description of the Parameter
     */
    public AstAxisLabels( String xLabel, String yLabel )
    {
        setDefaults();
        setXLabel( xLabel );
        setYLabel( yLabel );
    }


    /**
     * Set/reset all values to their defaults.
     */
    public void setDefaults()
    {
        isXSet = true;
        // fudged should be false.
        isYSet = true;
        // ""
        xShown = true;
        yShown = true;
        xLabel = NULL_LABEL;
        yLabel = NULL_LABEL;
        colour = Color.black;
        font = null;
        xGap = 0.01;
        yGap = 0.01;
        xEdge = BOTTOM;
        yEdge = LEFT;
        showXUnits = true;
        showYUnits = true;
        fireChanged();
    }


    /**
     * Set whether the X label is set or unset (unset implies that all label
     * properties should remain at their AST defaults).
     *
     * @param isXSet The new xState value
     */
    public void setXState( boolean isXSet )
    {
        this.isXSet = isXSet;
    }


    /**
     * Return if the X label is set or unset.
     *
     * @return The xState value
     */
    public boolean getXState()
    {
        return isXSet;
    }


    /**
     * Set whether the Y label is set or unset (unset implies that all label
     * properties should remain at their AST defaults).
     *
     * @param isYSet The new yState value
     */
    public void setYState( boolean isYSet )
    {
        this.isYSet = isYSet;
    }


    /**
     * Return if the Y label is set or unset.
     *
     * @return The yState value
     */
    public boolean getYState()
    {
        return isYSet;
    }


    /**
     * Set whether the X label should be shown.
     *
     * @param xShown The new xShown value
     */
    public void setXShown( boolean xShown )
    {
        this.xShown = xShown;
        setXState( true );
        fireChanged();
    }


    /**
     * Return if the X label is to be shown.
     *
     * @return The xShown value
     */
    public boolean getXShown()
    {
        return xShown;
    }


    /**
     * Set whether the Y label should be shown.
     *
     * @param yShown The new yShown value
     */
    public void setYShown( boolean yShown )
    {
        this.yShown = yShown;
        setYState( true );
        fireChanged();
    }


    /**
     * Return if the Y labels is to be shown.
     *
     * @return The yShown value
     */
    public boolean getYShown()
    {
        return yShown;
    }


    /**
     * Set the X label. If null then label is unset.
     *
     * @param xLabel The new xLabel value
     */
    public void setXLabel( String xLabel )
    {
        this.xLabel = xLabel;
        if ( xLabel != null && ! xLabel.equals( NULL_LABEL ) ) {
            setXState( true );
        }
        fireChanged();
    }


    /**
     * Get the current X label.
     *
     * @return The xLabel value
     */
    public String getXLabel()
    {
        return xLabel;
    }


    /**
     * Set the Y label. If null then label is unset.
     *
     * @param yLabel The new yLabel value
     */
    public void setYLabel( String yLabel )
    {
        this.yLabel = yLabel;
        if ( yLabel != null && ! yLabel.equals( NULL_LABEL ) ) {
            setYState( true );
        }
        fireChanged();
    }


    /**
     * Get the current Y label.
     *
     * @return The yLabel value
     */
    public String getYLabel()
    {
        return yLabel;
    }


    /**
     * Set the Font to be used when displaying the labels.
     *
     * @param font The new font value
     */
    public void setFont( Font font )
    {
        //  Make sure the new font is available in the graphics
        //  interface and release our hold on the old one.
        if ( font != null ) {
            setXState( true );
            setYState( true );
            grfFontManager.add( font );
        }
        if ( this.font != null ) {
            grfFontManager.remove( this.font );
        }
        this.font = font;
        fireChanged();
    }


    /**
     * Get the Font used to draw the labels.
     *
     * @return The font value
     */
    public Font getFont()
    {
        return font;
    }


    /**
     * Set the colour of the labels.
     *
     * @param colour The new colour value
     */
    public void setColour( Color colour )
    {
        this.colour = colour;
        if ( colour != null ) {
            setXState( true );
            setYState( true );
        }
        fireChanged();
    }


    /**
     * Get the colour of the labels.
     *
     * @return The colour value
     */
    public Color getColour()
    {
        return colour;
    }


    /**
     * Set the gap between X label and border. The value DefaultGrf.BAD means no
     * value.
     *
     * @param xGap The new xGap value
     */
    public void setXGap( double xGap )
    {
        this.xGap = xGap;
        if ( xGap != DefaultGrf.BAD ) {
            setXState( true );
        }
        fireChanged();
    }


    /**
     * Get the gap between X label and border.
     *
     * @return The xGap value
     */
    public double getXGap()
    {
        return xGap;
    }


    /**
     * Set the gap between Y label and border. The value DefaultGrf.BAD means no
     * value.
     *
     * @param yGap The new yGap value
     */
    public void setYGap( double yGap )
    {
        this.yGap = yGap;
        if ( yGap != DefaultGrf.BAD ) {
            setYState( true );
        }
        fireChanged();
    }


    /**
     * Get the gap between Y label and border.
     *
     * @return The yGap value
     */
    public double getYGap()
    {
        return yGap;
    }


    /**
     * Set the edge to display the X label.
     *
     * @param xEdge The new xEdge value
     */
    public void setXEdge( int xEdge )
    {
        if ( xEdge == BOTTOM ) {
            this.xEdge = BOTTOM;
        }
        else {
            this.xEdge = TOP;
        }
        setXState( true );
        fireChanged();
    }


    /**
     * Get the edge used to display the X axis.
     *
     * @return The xEdge value
     */
    public int getXEdge()
    {
        return xEdge;
    }


    /**
     * Set the edge to display the Y label.
     *
     * @param yEdge The new yEdge value
     */
    public void setYEdge( int yEdge )
    {
        if ( yEdge == RIGHT ) {
            this.yEdge = RIGHT;
        }
        else {
            this.yEdge = LEFT;
        }
        setYState( true );
        fireChanged();
    }


    /**
     * Get the edge used to display the Y axis.
     *
     * @return The yEdge value
     */
    public int getYEdge()
    {
        return yEdge;
    }


    /**
     * Set whether to display the units string on the X axis.
     *
     * @param showXUnits The new showXUnits value
     */
    public void setShowXUnits( boolean showXUnits )
    {
        this.showXUnits = showXUnits;
        fireChanged();
    }


    /**
     * Get whether we're displaying the units string on the X axis.
     *
     * @return The showXUnits value
     */
    public boolean getShowXUnits()
    {
        return showXUnits;
    }


    /**
     * Set whether to display the units string on the Y axis.
     *
     * @param showYUnits The new showYUnits value
     */
    public void setShowYUnits( boolean showYUnits )
    {
        this.showYUnits = showYUnits;
        fireChanged();
    }


    /**
     * Get whether we're displaying the units string on the Y axis.
     *
     * @return The showYUnits value
     */
    public boolean getShowYUnits()
    {
        return showYUnits;
    }


    /**
     * Get the AST plot options description of this object.
     *
     * @return The astOptions value
     */
    public String getAstOptions()
    {
        if ( ! isXSet && ! isYSet ) {
            return "";
        }
        StringBuffer buffer = new StringBuffer();

        // Add the general stuff.
        if ( font != null ) {
            buffer.append( ",Font(TextLab)=" );
            buffer.append( grfFontManager.getIndex( font ) );
        }
        if ( colour != null ) {
            buffer.append( ",Colour(TextLab)=" );
            int value = DefaultGrf.encodeColor( colour );
            if ( value == -1 ) {
                value = 0;
            }
            buffer.append( value );
        }

        // Now the axis specific stuff.
        if ( isXSet && xShown ) {
            buffer.append( ",TextLab(1)=1" );
            if ( xLabel != null && ! xLabel.equals( "" ) &&
                ! xLabel.equals( NULL_LABEL ) ) {
                buffer.append( ",label(1)=" );
                buffer.append( xLabel );
            }
            if ( xGap != DefaultGrf.BAD ) {
                buffer.append( ",TextLabGap(1)=" );
                buffer.append( xGap );
            }
            if ( xEdge == TOP ) {
                buffer.append( ",Edge(1)=top" );
            }
            else {
                buffer.append( ",Edge(1)=bottom" );
            }
            if ( showXUnits ) {
                buffer.append( ",LabelUnits(1)=1" );
            }
            else {
                buffer.append( ",LabelUnits(1)=0" );
            }
        }
        else if ( isXSet && ! xShown ) {
            buffer.append( ",TextLab(1)=0" );
        }

        if ( isYSet && yShown ) {
            buffer.append( ",TextLab(2)=1" );
            if ( yLabel != null && ! yLabel.equals( "" ) &&
                ! yLabel.equals( NULL_LABEL ) ) {
                buffer.append( ",label(2)=" );
                buffer.append( yLabel );
            }
            if ( yGap != DefaultGrf.BAD ) {
                buffer.append( ",TextLabGap(2)=" );
                buffer.append( yGap );
            }
            if ( yEdge == RIGHT ) {
                buffer.append( ",Edge(2)=right" );
            }
            else {
                buffer.append( ",Edge(2)=left" );
            }
            if ( showYUnits ) {
                buffer.append( ",LabelUnits(2)=1" );
            }
            else {
                buffer.append( ",LabelUnits(2)=0" );
            }
        }
        else if ( isYSet && ! yShown ) {
            buffer.append( ",TextLab(2)=0" );
        }
        buffer.deleteCharAt( 0 );
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
        return "axislabels";
    }

    /**
     * Description of the Method
     *
     * @param rootElement Description of the Parameter
     */
    public void encode( Element rootElement )
    {
        addChildElement( rootElement, "isXSet", isXSet );
        addChildElement( rootElement, "isYSet", isYSet );

        addChildElement( rootElement, "xShown", xShown );
        addChildElement( rootElement, "yShown", yShown );

        addChildElement( rootElement, "xLabel", xLabel );
        addChildElement( rootElement, "yLabel", yLabel );

        if ( font != null ) {
            addChildElement( rootElement, "font", font );
        }
        if ( colour != null ) {
            addChildElement( rootElement, "colour", colour );
        }
        addChildElement( rootElement, "xGap", xGap );
        addChildElement( rootElement, "yGap", yGap );

        addChildElement( rootElement, "xEdge", xEdge );
        addChildElement( rootElement, "yEdge", yEdge );

        addChildElement( rootElement, "showXUnits", showXUnits );
        addChildElement( rootElement, "showYUnits", showYUnits );
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
        if ( name.equals( "isXSet" ) ) {
            setXState( booleanFromString( value ) );
            return;
        }
        if ( name.equals( "isYSet" ) ) {
            setYState( booleanFromString( value ) );
            return;
        }

        if ( name.equals( "xShown" ) ) {
            setXShown( booleanFromString( value ) );
            return;
        }

        if ( name.equals( "yShown" ) ) {
            setYShown( booleanFromString( value ) );
            return;
        }

        if ( name.equals( "xLabel" ) ) {
            setXLabel( value );
            return;
        }

        if ( name.equals( "yLabel" ) ) {
            setYLabel( value );
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

        if ( name.equals( "xGap" ) ) {
            setXGap( doubleFromString( value ) );
            return;
        }

        if ( name.equals( "yGap" ) ) {
            setYGap( doubleFromString( value ) );
            return;
        }

        if ( name.equals( "xEdge" ) ) {
            setXEdge( intFromString( value ) );
            return;
        }

        if ( name.equals( "yEdge" ) ) {
            setYEdge( intFromString( value ) );
            return;
        }

        if ( name.equals( "showXUnits" ) ) {
            setShowXUnits( booleanFromString( value ) );
            return;
        }

        if ( name.equals( "showYUnits" ) ) {
            setShowYUnits( booleanFromString( value ) );
            return;
        }
        System.err.println( "AstAxisLabels: unknown configuration property:" +
            name + " (" + value + ")" );
    }
}
