/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     20-FEB-2002 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.diva;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.AlphaComposite;

import uk.ac.starlink.diva.interp.Interpolator;;

/**
 * FigureProps is a simple container class for passing configuration
 * properties of Diva Figures around.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see Draw
 * @see DrawFigureFactory
 * @see DrawActions
 */
public class FigureProps
{
    /**
     * First X coordinate
     */
    private double x1;

    /**
     * First Y coordinate
     */
    private double y1;

    /**
     * Second X coordinate
     */
    private double x2;

    /**
     * Second Y coordinate
     */
    private double y2;

    /**
     * Width
     */
    private double width;

    /**
     * Height
     */
    private double height;

    /**
     * Outline colour.
     */
    private Paint outline;

    /**
     * Fill colour.
     */
    private Paint fill;

    /**
     * A thickness for lines
     */
    private double thickness;

    /**
     * An Interpolator (for curves).
     */
    private Interpolator interpolator;

    /**
     * A String for a text display.
     */
    private String text = null;

    /**
     * A Font for the text string.
     */
    private Font font = null;

    /**
     * AlphaComposite for combining the figure.
     */
    private AlphaComposite composite  = null;
    private static final AlphaComposite 
        defaultComposite = AlphaComposite.SrcOver;

    /**
     *  Default constructor. All items keep their default values.
     */
    public FigureProps()
    {
        reset();
    }

    /**
     *  Constructor that provides enough information to describe a
     *  rectangle.
     */
    public FigureProps( double x1, double y1, double width,
                        double height )
    {
        reset();
        setX1( x1 );
        setY1( y1 );
        setWidth( width );
        setHeight( height );
    }

    /**
     *  Constructor that provides enough information to describe a
     *  rectangle.
     */
    public FigureProps( double x1, double y1, double width,
                        double height, Paint outline )
    {
        reset();
        setX1( x1 );
        setY1( y1 );
        setHeight( width );
        setWidth( height );
        setOutline( outline );
    }

    /**
     *  Constructor that provides enough information to describe a
     *  rectangle, with other colour.
     */
    public FigureProps( double x1, double y1,
                        double width, double height,
                        double x2, double y2,
                        Paint outline, Paint fill )
    {
        reset();
        setX1( x1 );
        setY1( y1 );
        setX2( x2 );
        setY2( y2 );
        setWidth( width );
        setHeight( height );
        setOutline( outline );
        setFill( fill );
    }

    /**
     *  Constructor that provides enough information to describe an
     *  interpolated curve.
     */
    public FigureProps( Interpolator interpolator,
                        double x1, double y1,
                        Paint outline, double thickness )
    {
        reset();
        setInterpolator( interpolator );
        setX1( x1 );
        setY1( y1 );
        setOutline( outline );
        setThickness( thickness );
    }

    /**
     *  Constructor that provides enough information to describe a
     *  text String.
     */
    public FigureProps( double x1, double y1, String text, Font font,
                        Paint outline )
    {
        reset();
        setX1( x1 );
        setY1( y1 );
        setOutline( outline );
        setText( text );
        setFont( font );
    }

    /**
     *  Reset all items to their defaults.
     */
    public void reset()
    {
        setX1( 0.0 );
        setY1( 0.0 );
        setX2( 0.0 );
        setY2( 0.0 );
        setWidth( 1.0 );
        setHeight( 1.0 );
        setOutline( Color.black );
        setFill( Color.blue );
        setInterpolator( null );
        setThickness( 1.0 );
        setText( null );
        setFont( null );
        setComposite( null );
    }

    /**
     * Get the value of x1
     *
     * @return value of x1.
     */
    public double getX1()
    {
        return x1;
    }

    /**
     * Set the value of x1.
     *
     * @param x1 Value to assign to x1.
     */
    public void setX1( double x1 )
    {
        this.x1 = x1;
    }

    /**
     * Get the value of y1
     *
     * @return value of y1
     */
    public double getY1()
    {
        return y1;
    }

    /**
     * Set the value of y1.
     *
     * @param y1 Value to assign to y1.
     */
    public void setY1( double y1 )
    {
        this.y1 = y1;
    }

    /**
     * Get the value of x2.
     *
     * @return value of x2.
     */
    public double getX2()
    {
        return x2;
    }

    /**
     * Set the value of x2.
     * @param x2  Value to assign to x2.
     */
    public void setX2( double x2 )
    {
        this.x2 = x2;
    }

    /**
     * Get the value of y2.
     * @return value of y2.
     */
    public double getY2()
    {
        return y2;
    }

    /**
     * Set the value of y2.
     * @param y2 Value to assign to y2.
     */
    public void setY2( double y2 )
    {
        this.y2 = y2;
    }

    /**
     * Get the value of width
     * @return value of width.
     */
    public double getWidth()
    {
        return width;
    }

    /**
     * Set the value of width
     * @param width Value to assign to xLength.
     */
    public void setWidth( double width )
    {
        this.width = width;
    }

    /**
     * Get the value of height
     * @return value of height.
     */
    public double getHeight()
    {
        return height;
    }

    /**
     * Set the value of height
     * @param height Value to assign to height
     */
    public void setHeight( double height )
    {
        this.height = height;
    }

    /**
     * Get the value of outline
     * @return value of outline.
     */
    public Paint getOutline()
    {
        return outline;
    }

    /**
     * Set the value of outline
     * @param outline Value to assign to outline
     */
    public void setOutline( Paint outline )
    {
        this.outline = outline;
    }

    /**
     * Get the value of fill
     * @return value of fill
     */
    public Paint getFill()
    {
        return fill;
    }

    /**
     * Set the value of fill
     * @param fill Value to assign to fill
     */
    public void setFill( Paint  fill )
    {
        this.fill = fill;
    }

    /**
     * Get the value of interpolator.
     *
     * @return value of interpolator.
     */
    public Interpolator getInterpolator()
    {
        return interpolator;
    }

    /**
     * Set the value of interpolator.
     *
     * @param interpolator Value to assign to interpolator.
     */
    public void setInterpolator( Interpolator interpolator )
    {
        this.interpolator = interpolator;
    }

    /**
     * Get the value of thickness.
     *
     * @return value of thickness
     */
    public double getThickness()
    {
        return thickness;
    }

    /**
     * Set the value of thickness.
     *
     * @param thickness Value to assign to thickness.
     */
    public void setThickness( double thickness )
    {
        this.thickness = thickness;
    }

    /**
     * Get the Text.
     *
     * @return value of text
     */
    public String getText()
    {
        return text;
    }

    /**
     * Set the text Text.
     *
     * @param text Value to assign to text.
     */
    public void setText( String text )
    {
        this.text = text;
    }

    /**
     * Get the Font.
     *
     * @return value of font
     */
    public Font getFont()
    {
        return font;
    }

    /**
     * Set the Font use together with the text String.
     *
     * @param font Value to assign to font.
     */
    public void setFont( Font font )
    {
        this.font = font;
    }

    /**
     * Get the AlphaComposite.
     *
     * @return value of composite
     */
    public AlphaComposite getComposite()
    {
        return composite;
    }

    /**
     * Set the AlphaComposite to use when drawing the figure.
     *
     * @param composite Value to assign to composite.
     */
    public void setComposite( AlphaComposite composite )
    {
        if ( composite != null ) {
            this.composite = composite;
        }
        else {
            this.composite = defaultComposite;
        }
    }

    public String toString()
    {
        return "FigureProps: " + 
            " x1 = " + x1 + 
            " y1 = " + y1 +
            " x2 = " + x2 +
            " y2 = " + y2 +
            " width = " + width +
            " height = " + height +
            " outline = " + outline +
            " fill = " + fill + 
            " thickness = " + thickness +
            " interpolator = " + interpolator +
            " text = " + text + 
            " font = " + font + 
            " composite = " + composite;
    }
}
