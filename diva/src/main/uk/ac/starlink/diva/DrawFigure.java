/*
 * Copyright (C) 2001-2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     08-JAN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.diva;

import diva.canvas.Figure;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Paint;
import java.awt.Shape;

/**
 * DrawFigure defines an interface that any Figures drawn on a {@link
 * Draw} implementation should implement. See {@link DrawBasicFigure}
 * and {@link DrawPathFigure} for concrete implementations of this
 * interface.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see Draw
 * @see DrawBasicFigure
 * @see DrawActions
 */
public interface DrawFigure
    extends Figure
{
    //
    //  See the whole Figure interface...
    //

    /**
     * Set the Figure shape.
     */
    public void setShape( Shape shape );

    /**
     * Set the visibility. Needs re-implementing to also remove any
     * decorators.
     */
    public void setVisible( boolean flag );

    /**
     * All Figures can be potentially filled. Ignored for non-filled figures.
     */
    public void setFillPaint( Paint fill );

    /**
     * Get the fill.
     */
    public Paint getFillPaint();

    /**
     * All Figures have composite fills/colours.
     */
    public void setComposite( AlphaComposite composite );

    /**
     * Get the composite (null for unset).
     */
    public Composite getComposite();

    /**
     * All Figures have a line width.
     */
    public void setLineWidth( float lineWidth );

    /**
     * Get the line width.
     */
    public float getLineWidth();

    /**
     * All Figures have an outline colour.
     */
    public void setStrokePaint( Paint outline );

    /**
     * Get the outline.
     */
    public Paint getStrokePaint();

    //
    //  Transform freely interface.
    //
    /**
     * Enable the hint that a figure should allow itself to transform
     * freely, rather than obey any constraints (this is meant for
     * figures that could not otherwise redraw themselves to fit a
     * resized {@link Draw}, given their normal constraints,
     * e.g. XRangeFigure).
     */
    public void setTransformFreely( boolean state );

    //
    //  Events interface.
    //
    /**
     *  Registers a listener for to be informed when figure changes
     *  occur.
     *
     *  @param l the FigureListener
     */
    public void addListener( FigureListener l );

    /**
     * Remove a listener.
     *
     * @param l the FigureListener
     */
    public void removeListener( FigureListener l );
}
