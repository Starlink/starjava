package uk.ac.starlink.splat.ast;

import java.awt.Color;
import java.awt.Font;

import org.jdom.Element;
import uk.ac.starlink.splat.util.AbstractStorableConfig;
import uk.ac.starlink.ast.grf.DefaultGrf;
import uk.ac.starlink.ast.grf.DefaultGrfFontManager;

/**
 * AstNumberLabels is a model of the axis numeric label elements shown in say
 * an AST Plot. It encompasses all the values that describe their
 * representations (which are not independent, hence what seems to be two
 * elements - X & Y - in one class, in fact these are also related to the text
 * labels, which determine the edge) and returns these in various formats
 * (such as the complete AST Plot options list for configuring it).
 *
 * @author Peter W. Draper
 * @created May 31, 2002
 * @since $Date$
 * @since 02-NOV-2000
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 * @version $Id$
 */
public class AstNumberLabels extends AbstractStorableConfig
{
    /**
     * Whether X numeric labels are set or unset.
     */
    protected boolean isXSet;

    /**
     * Whether Y numeric labels are set or unset.
     */
    protected boolean isYSet;

    /**
     * Whether X numeric labels should be shown.
     */
    protected boolean xShown;

    /**
     * Whether Y numeric labels should be shown.
     */
    protected boolean yShown;

    /**
     * The Font used to display the labels.
     */
    protected Font font;

    /**
     * The colour of the labels.
     */
    protected Color colour;

    /**
     * The gap between X labels and plot border.
     */
    protected double xGap;

    /**
     * The gap between Y labels and the plot border.
     */
    protected double yGap;

    /**
     * The number of digits used to determine gap between major axes. (-1 for
     * not set).
     */
    protected int digits;

    /**
     * The suggested minimum gap.
     */
    public final static double GAP_MIN = -0.15;

    /**
     * The suggested maximum gap.
     */
    public final static double GAP_MAX = 0.15;

    /**
     * The suggested gap resolution (i.e. interval between steps).
     */
    public final static double GAP_STEP = 0.005;

    /**
     * Reference to the GrfFontManager object. Use this to add and remove
     * fonts from the global list. Also provides the index of the font as
     * known to Grf.
     */
    protected DefaultGrfFontManager grfFontManager =
        DefaultGrfFontManager.getReference();


    /**
     * Create an empty instance.
     */
    public AstNumberLabels()
    {
        setDefaults();
    }


    /**
     * Set/reset all values to their defaults.
     */
    public void setDefaults()
    {
        isXSet = false;
        isYSet = false;
        xShown = true;
        yShown = true;
        colour = null;
        font = null;
        xGap = 0.01;
        yGap = 0.01;
        digits = -1;
        fireChanged();
    }


    /**
     * Set whether the X labels are set or unset (unset implies that all label
     * properties should remain at their AST defaults).
     *
     * @param isXSet The new xState value
     */
    public void setXState( boolean isXSet )
    {
        this.isXSet = isXSet;
    }


    /**
     * Return if the X labels are set or unset.
     *
     * @return The xState value
     */
    public boolean getXState()
    {
        return isXSet;
    }


    /**
     * Set whether the Y labels are set or unset (unset implies that all label
     * properties should remain at their AST defaults).
     *
     * @param isYSet The new yState value
     */
    public void setYState( boolean isYSet )
    {
        this.isYSet = isYSet;
    }


    /**
     * Return if the Y labels are set or unset.
     *
     * @return The yState value
     */
    public boolean getYState()
    {
        return isYSet;
    }


    /**
     * Set whether the X labels should be shown.
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
     * Return if the X labels are to be shown.
     *
     * @return The xShown value
     */
    public boolean getXShown()
    {
        return xShown;
    }


    /**
     * Set whether the Y labels should be shown.
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
     * Return if the Y labels are to be shown.
     *
     * @return The yShown value
     */
    public boolean getYShown()
    {
        return yShown;
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
     * Set the gap between X labels and border. The value DefaultGrf.BAD means no
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
     * Get the gap between X labels and border.
     *
     * @return The xGap value
     */
    public double getXGap()
    {
        return xGap;
    }


    /**
     * Set the gap between Y labels and border. The value DefaultGrf.BAD means no
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
     * Get the gap between Y labels and border.
     *
     * @return The yGap value
     */
    public double getYGap()
    {
        return yGap;
    }


    /**
     * Set the number of digits of precision that may be used in the numeric
     * labels. This also effects what the major intervals are chosen for ticks
     * and grid positioning. If -1 then the default digits are used.
     *
     * @param digits The new digits value
     */
    public void setDigits( int digits )
    {
        this.digits = digits;
        if ( digits != -1 ) {
            setXState( true );
            setYState( true );
        }
        fireChanged();
    }


    /**
     * Get the number of digits of precision.
     *
     * @return The digits value
     */
    public int getDigits()
    {
        return digits;
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
            buffer.append( ",Font(NumLab)=" );
            buffer.append( grfFontManager.getIndex( font ) );
        }
        if ( colour != null ) {
            buffer.append( ",Colour(NumLab)=" );
            buffer.append( colour.getRGB() );
        }
        if ( digits != -1 ) {
            buffer.append( ",Digits=" );
            buffer.append( digits );
        }

        // Now the axis specific stuff.
        if ( isXSet ) {
            if ( xShown ) {
                buffer.append( ",NumLab(1)=1" );
                if ( xGap != DefaultGrf.BAD ) {
                    buffer.append( ",NumLabGap(1)=" );
                    buffer.append( xGap );
                }
            }
            else {
                buffer.append( ",NumLab(1)=0" );
            }
        }

        if ( isYSet ) {
            if ( yShown ) {
                buffer.append( ",NumLab(2)=1" );
                if ( yGap != DefaultGrf.BAD ) {
                    buffer.append( ",NumLabGap(2)=" );
                    buffer.append( yGap );
                }
            }
            else {
                buffer.append( ",NumLab(2)=0" );
            }
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

        if ( font != null ) {
            addChildElement( rootElement, "font", font );
        }
        if ( colour != null ) {
            addChildElement( rootElement, "colour", colour );
        }
        addChildElement( rootElement, "xGap", xGap );
        addChildElement( rootElement, "yGap", yGap );

        addChildElement( rootElement, "digits", digits );
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

        if ( name.equals( "digits" ) ) {
            setDigits( intFromString( value ) );
            return;
        }
        System.err.println( "AstNumberLabels: unknown configuration property:" +
            name + " (" + value + ")" );
    }
}
