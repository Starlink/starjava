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
 * AstAxes is a model of the axes elements shown in an AST Plot.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class AstAxes 
    extends AbstractPlotControlsModel
{
    /**
     * Whether X axis is set or unset.
     */
    protected boolean isXSet;

    /**
     * Whether Y axis is set or unset.
     */
    protected boolean isYSet;

    /**
     * Whether the X axis should be shown (different from unset).
     */
    protected boolean showX;

    /**
     * Whether the Y axis should be shown (different from unset).
     */
    protected boolean showY;

    /**
     * Whether the X axis should be logarithmic.
     */
    protected boolean logX;

    /**
     * Whether the Y axis should be logarithmic.
     */
    protected boolean logY;

    /**
     * Axes placement, interior or exterior. This is just a suggestion.
     */
    protected boolean interior;

    /**
     * A position to start the X labelling at, not used when axes are
     * drawn externally.
     */
    protected double xLabelAt;

    /**
     * A position to start the Y labelling at, not used when axes are
     * drawn externally.
     */
    protected double yLabelAt;

    /**
     * The colour of the X axis.
     */
    protected Color xColour;

    /**
     * The colour of the Y axis.
     */
    protected Color yColour;

    /**
     * The line style of the X axis.
     */
    protected int xStyle;

    /**
     * The line style of the Y axis.
     */
    protected int yStyle;

    /**
     * The width of the X axis.
     */
    protected double xWidth;

    /**
     * The width of the Y axis.
     */
    protected double yWidth;

    /**
     * Suggested minimum width.
     */
    public static int MIN_WIDTH = 1;

    /**
     * Suggested maximum width.
     */
    public static int MAX_WIDTH = 20;

    /**
     * Whether exterior axes should be forced.
     */
    private boolean forceExterior = false;

    /**
     * Create a empty instance. This indicates that the axis elements
     * should remain at the AST Plot default.
     */
    public AstAxes()
    {
        setDefaults();
    }


    /**
     * Set object to default state.
     */
    public void setDefaults()
    {
        isXSet = true;
        isYSet = true;
        showX = true;
        showY = true;
        logX = false;
        logY = false;
        xLabelAt = DefaultGrf.BAD;
        yLabelAt = DefaultGrf.BAD;
        xColour = Color.black;
        yColour = xColour;
        xStyle = DefaultGrf.PLAIN;
        yStyle = DefaultGrf.PLAIN;
        xWidth = 1.0;
        yWidth = 1.0;
        interior = false;
        forceExterior = false;
        fireChanged();
    }


    /**
     * Set whether the X axis is set or unset (unset implies that all
     * properties should remain at their AST defaults).
     *
     * @param isXSet The new state value
     */
    public void setXState( boolean isXSet )
    {
        this.isXSet = isXSet;
    }


    /**
     * Return if the X axis is set or unset.
     *
     * @return The state value
     */
    public boolean getXState()
    {
        return isXSet;
    }

    /**
     * Set whether the Y axis is set or unset (unset implies that all
     * properties should remain at their AST defaults).
     *
     * @param isYSet The new state value
     */
    public void setYState( boolean isYSet )
    {
        this.isYSet = isYSet;
    }


    /**
     * Return if the Y axis is set or unset.
     *
     * @return The state value
     */
    public boolean getYState()
    {
        return isYSet;
    }

    /**
     * Set whether the X axis should be shown or not.
     *
     * @param showX The new shown value
     */
    public void setXShown( boolean showX )
    {
        this.showX = showX;
        setXState( true );
        fireChanged();
    }


    /**
     * Get whether the X axis is to be shown.
     *
     * @return The shown value
     */
    public boolean getXShown()
    {
        return showX;
    }


    /**
     * Set whether the Y axis should be shown or not.
     *
     * @param showY The new shown value
     */
    public void setYShown( boolean showY )
    {
        this.showY = showY;
        setYState( true );
        fireChanged();
    }


    /**
     * Get whether the Y axis is to be shown.
     *
     * @return The shown value
     */
    public boolean getYShown()
    {
        return showY;
    }

    /**
     * Set whether the X axis should be drawn with logarithmic spacing.
     *
     * @param logX The new logX value
     */
    public void setXLog( boolean logX )
    {
        this.logX = logX;
        setXState( true );
        fireChanged();
    }


    /**
     * Get whether the X axis is to be drawn with logarithmic spacing.
     *
     * @return The logX value.
     */
    public boolean getXLog()
    {
        return logX;
    }

    /**
     * Set whether the Y axis should be drawn with logarithmic spacing.
     *
     * @param logY The new logY value
     */
    public void setYLog( boolean logY )
    {
        this.logY = logY;
        setYState( true );
        fireChanged();
    }

    /**
     * Get whether the Y axis is to be drawn with logarithmic spacing.
     *
     * @return The logY value.
     */
    public boolean getYLog()
    {
        return logY;
    }

    /**
     * Set position along the X axis at which to start labelling
     * (i.e. the Y axis intersection with the X axis). Ignore if
     * labelling is drawn externally.  DefaultGrf.BAD indicates that
     * the default value should be used. The units are those of the X
     * axis.
     *
     * @param xLabelAt The new position to start labelling.
     */
    public void setXLabelAt( double xLabelAt )
    {
        this.xLabelAt = xLabelAt;
        if ( xLabelAt != DefaultGrf.BAD ) {
            setXState( true );
        }
        fireChanged();
    }


    /**
     * Get the position along the X axis at which labelling is to
     * start. DefaultGrf.BAD indicates that the default value should
     * be used.
     *
     * @return The xLabelAt value
     */
    public double getXLabelAt()
    {
        return xLabelAt;
    }

    /**
     * Set position along the Y axis at which to start labelling
     * (i.e. the X axis intersection with the Y axis). Ignore if
     * labelling is drawn externally.  DefaultGrf.BAD indicates that
     * the default value should be used. The units are those of the Y
     * axis.
     *
     * @param yLabelAt The new position to start labelling.
     */
    public void setYLabelAt( double yLabelAt )
    {
        this.yLabelAt = yLabelAt;
        if ( yLabelAt != DefaultGrf.BAD ) {
            setYState( true );
        }
        fireChanged();
    }

    /**
     * Get the position along the Y axis at which labelling is to
     * start. DefaultGrf.BAD indicates that the default value should
     * be used.
     *
     * @return The yLabelAt value
     */
    public double getYLabelAt()
    {
        return yLabelAt;
    }

    /**
     * Set the colour of the X axis.
     *
     * @param colour The new colour value
     */
    public void setXColour( Color xColour )
    {
        this.xColour = xColour;
        if ( xColour != null ) {
            setXState( true );
        }
        fireChanged();
    }


    /**
     * Get the colour of the X axis.
     *
     * @return The colour value
     */
    public Color getXColour()
    {
        return xColour;
    }

    /**
     * Set the colour of the Y axis.
     *
     * @param colour The new colour value
     */
    public void setYColour( Color yColour )
    {
        this.yColour = yColour;
        if ( yColour != null ) {
            setYState( true );
        }
        fireChanged();
    }


    /**
     * Get the colour of the Y axis.
     *
     * @return The colour value
     */
    public Color getYColour()
    {
        return yColour;
    }


    /**
     * Set the X axis line width. The value DefaultGrf.BAD means no value.
     *
     * @param width The new width value
     */
    public void setXWidth( double xWidth )
    {
        this.xWidth = xWidth;
        if ( xWidth != DefaultGrf.BAD ) {
            setXState( true );
        }
        fireChanged();
    }


    /**
     * Get the X axis line width.
     *
     * @return The width value
     */
    public double getXWidth()
    {
        return xWidth;
    }


    /**
     * Set the Y axis line width. The value DefaultGrf.BAD means no value.
     *
     * @param width The new width value
     */
    public void setYWidth( double yWidth )
    {
        this.yWidth = yWidth;
        if ( yWidth != DefaultGrf.BAD ) {
            setYState( true );
        }
        fireChanged();
    }


    /**
     * Get the Y axis line width.
     *
     * @return The width value
     */
    public double getYWidth()
    {
        return yWidth;
    }


    /**
     * Set the X axis line style. This should be a style known to the
     * Grf class (i.e. Grf.PLAIN, Grf.DASH etc.)
     *
     * @param xStyle The new style value
     */
    public void setXStyle( int xStyle )
    {
        this.xStyle = xStyle;
        setXState( true );
        fireChanged();
    }


    /**
     * Get the X axis line style.
     *
     * @return The style value
     */
    public int getXStyle()
    {
        return xStyle;
    }


    /**
     * Set the Y axis line style. This should be a style known to the
     * Grf class (i.e. Grf.PLAIN, Grf.DASH etc.)
     *
     * @param yStyle The new style value
     */
    public void setYStyle( int yStyle )
    {
        this.yStyle = yStyle;
        setYState( true );
        fireChanged();
    }


    /**
     * Get the Y axis line style.
     *
     * @return The style value
     */
    public int getYStyle()
    {
        return yStyle;
    }

    /**
     * Set whether the suggested placement for axes is interior. If
     * false then it is suggested that axes are placed on the
     * exterior.
     *
     * @param interior Whether to place axes on the interior
     */
    public void setInterior( boolean interior )
    {
        this.interior = interior;
        fireChanged();
    }

    /**
     * Return if the suggestion axis placement is interior
     *
     * @return The state value
     */
    public boolean getInterior()
    {
        return interior;
    }

    /**
     * Set whether exterior axes placement should be forced, when 
     * the labelling choice is exterior.
     *
     * @param forceExterior Whether to force exterior axes.
     */
    public void setForceExterior( boolean forceExterior )
    {
        this.forceExterior = forceExterior;
        fireChanged();
    }

    /**
     * Return if the exterior axes will be forced.
     *
     * @return The state value
     */
    public boolean getForceExterior()
    {
        return forceExterior;
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
        
        if ( interior ) {
            buffer.append( "Labelling=interior" );
        }
        else {
            buffer.append( "Labelling=exterior" );
            if ( forceExterior ) { 
                buffer.append( ",ForceExterior=1" );
            }
        }

        if ( isXSet ) {
            if ( showX ) {
                buffer.append( ",DrawAxes(1)=1" );
                if ( logX ) { 
                    buffer.append( ",LogPlot(1)=1" );
                }
                if ( xColour != null ) {
                    buffer.append( ",Colour(axis1)=" );
                    int value = DefaultGrf.encodeColor( xColour );
                    buffer.append( value );
                }
                if ( xWidth != DefaultGrf.BAD ) {
                    buffer.append( ",Width(axis1)=" );
                    buffer.append( xWidth );
                }
                buffer.append( ",Style(axis1)=" );
                buffer.append( xStyle );
                if ( xLabelAt != DefaultGrf.BAD ) {
                    buffer.append( ",LabelAt(1)=" );
                    buffer.append( xLabelAt );
                }
            }
            else {
                buffer.append( ",DrawAxes(1)=0" );
            }
        }

        if ( isYSet ) {
            if ( showY ) {
                buffer.append( ",DrawAxes(2)=1" );
                if ( logY ) { 
                    buffer.append( ",LogPlot(2)=1" );
                }
                if ( yColour != null ) {
                    buffer.append( ",Colour(axis2)=" );
                    int value = DefaultGrf.encodeColor( yColour );
                    buffer.append( value );
                }
                if ( yWidth != DefaultGrf.BAD ) {
                    buffer.append( ",Width(axis2)=" );
                    buffer.append( yWidth );
                }
                buffer.append( ",Style(axis2)=" );
                buffer.append( yStyle );
                if ( yLabelAt != DefaultGrf.BAD ) {
                    buffer.append( ",LabelAt(2)=" );
                    buffer.append( yLabelAt );
                }
            }
            else {
                buffer.append( ",DrawAxes(2)=0" );
            }
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
        return "axes";
    }

    /**
     * Description of the Method
     *
     * @param rootElement Description of the Parameter
     */
    public void encode( Element rootElement )
    {
        addChildElement( rootElement, "interior", interior );

        addChildElement( rootElement, "forceExterior", forceExterior );

        addChildElement( rootElement, "isXSet", isXSet );
        addChildElement( rootElement, "showX", showX );
        addChildElement( rootElement, "xLog", logX );
        addChildElement( rootElement, "xLabelAt", xLabelAt );
        if ( xColour != null ) {
            addChildElement( rootElement, "xColour", xColour );
        }
        addChildElement( rootElement, "xStyle", xStyle );
        addChildElement( rootElement, "xWidth", xWidth );

        addChildElement( rootElement, "isYSet", isYSet );
        addChildElement( rootElement, "showY", showY );
        addChildElement( rootElement, "yLog", logY );
        addChildElement( rootElement, "yLabelAt", yLabelAt );
        if ( yColour != null ) {
            addChildElement( rootElement, "yColour", yColour );
        }
        addChildElement( rootElement, "yStyle", yStyle );
        addChildElement( rootElement, "yWidth", yWidth );
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
        if ( name.equals( "interior" ) ) {
            setInterior( booleanFromString( value ) );
            return;
        }

        if ( name.equals( "forceExterior" ) ) {
            setForceExterior( booleanFromString( value ) );
            return;
        }

        if ( name.equals( "isXSet" ) ) {
            setXState( booleanFromString( value ) );
            return;
        }
        if ( name.equals( "showX" ) ) {
            setXShown( booleanFromString( value ) );
            return;
        }
        if ( name.equals( "xLog" ) ) {
            setXLog( booleanFromString( value ) );
            return;
        }
        if ( name.equals( "xLabelAt" ) ) {
            setXLabelAt( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "xColour" ) ) {
            setXColour( colorFromString( value ) );
            return;
        }
        if ( name.equals( "xStyle" ) ) {
            setXStyle( intFromString( value ) );
            return;
        }
        if ( name.equals( "xWidth" ) ) {
            setXWidth( doubleFromString( value ) );
            return;
        }

        if ( name.equals( "isYSet" ) ) {
            setYState( booleanFromString( value ) );
            return;
        }
        if ( name.equals( "showY" ) ) {
            setYShown( booleanFromString( value ) );
            return;
        }
        if ( name.equals( "yLog" ) ) {
            setYLog( booleanFromString( value ) );
            return;
        }
        if ( name.equals( "yLabelAt" ) ) {
            setYLabelAt( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "yColour" ) ) {
            setYColour( colorFromString( value ) );
            return;
        }
        if ( name.equals( "yStyle" ) ) {
            setYStyle( intFromString( value ) );
            return;
        }
        if ( name.equals( "yWidth" ) ) {
            setYWidth( doubleFromString( value ) );
            return;
        }
        System.err.println( "AstAxes: unknown configuration property: " +
            name + " (" + value + ")" );
    }
}
