/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     31-MAY-2002 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.ast.grf;

import java.awt.Color;
import java.awt.Rectangle;

/**
 * Java class to store the current GRF state. The GRF state is defined as the
 * current colour, line style & width, character and marker size.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class DefaultGrfState implements Cloneable
{
    //  Internal state.
    private double style = 1;
    private double width = 1.0;
    private double size = 1.0;
    private double font = 0;
    private double colour = DefaultGrf.encodeColor( Color.black );
    private Rectangle clip = null;
    private double alpha = 1.0;


    /**
     * Constructor for the DefaultGrfState object
     */
    public DefaultGrfState()
    {
        // Do nothing.
    }


    /**
     * Constructor for the DefaultGrfState object
     */
    public DefaultGrfState( double istyle, double iwidth, double isize,
                         double ifont, double icolour )
    {
        style = istyle;
        width = iwidth;
        size = isize;
        font = ifont;
        colour = icolour;
    }


    /**
     * Constructor for the DefaultGrfState object
     */
    public DefaultGrfState( double istyle, double iwidth, double isize,
                         double ifont, double icolour, Rectangle iclip,
                         double ialpha )
    {
        style = istyle;
        width = iwidth;
        size = isize;
        font = ifont;
        colour = icolour;
        clip = iclip;
        alpha = ialpha;
    }


    /**
     * Gets the width attribute of the DefaultGrfState object
     */
    public double getWidth()
    {
        return width;
    }


    /**
     * Gets the size attribute of the DefaultGrfState object
     */
    public double getSize()
    {
        return size;
    }


    /**
     * Gets the font attribute of the DefaultGrfState object
     */
    public double getFont()
    {
        return font;
    }


    /**
     * Gets the colour attribute of the DefaultGrfState object
     */
    public double getColour()
    {
        return colour;
    }


    /**
     * Gets the style attribute of the DefaultGrfState object
     */
    public double getStyle()
    {
        return style;
    }


    /**
     * Gets the clip attribute of the DefaultGrfState object
     */
    public Rectangle getClip()
    {
        return clip;
    }


    /**
     * Gets the alpha attribute of the DefaultGrfState object
     */
    public double getAlpha()
    {
        return alpha;
    }


    /**
     * Sets the width attribute of the DefaultGrfState object
     */
    public void setWidth( double value )
    {
        width = value;
    }


    /**
     * Sets the size attribute of the DefaultGrfState object
     */
    public void setSize( double value )
    {
        size = value;
    }


    /**
     * Sets the font attribute of the DefaultGrfState object
     */
    public void setFont( double value )
    {
        font = value;
    }


    /**
     * Sets the colour attribute of the DefaultGrfState object
     */
    public void setColour( double value )
    {
        colour = value;
    }


    /**
     * Sets the style attribute of the DefaultGrfState object
     */
    public void setStyle( double value )
    {
        style = value;
    }


    /**
     * Sets the clip attribute of the DefaultGrfState object
     */
    public void setClip( Rectangle value )
    {
        clip = value;
    }


    /**
     * Sets the alpha attribute of the DefaultGrfState object
     */
    public void setAlpha( double value )
    {
        alpha = value;
    }


    //  Create a clone of this object. Copies all fields to new object.
    public Object clone()
    {
        try {
            return super.clone();
        }
        catch ( CloneNotSupportedException e ) {
            throw new InternalError( e.toString() );
        }
    }


    //  Output the current state.
    public String toString()
    {
        if ( clip == null ) {
            return "style = " + style +
                ", width = " + width +
                ", size = " + size +
                ", font = " + font +
                ", colour = " + colour +
                ", alpha = " + alpha;
        }
        else {
            return "style = " + style +
                ", width = " + width +
                ", size = " + size +
                ", font = " + font +
                ", colour = " + colour +
                ", alpha = " + alpha +
                ", clip = " + clip;
        }
    }
}
