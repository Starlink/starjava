/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     08-JAN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.plot;

import diva.canvas.Figure;
import java.awt.Shape;

/**
 * PlotFigure defines an interface that any Figures drawn on a Plot
 * should implement. See BasicPlotFigure for a concrete implementation
 * of this interface.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see DivaPlot
 */
public interface PlotFigure
    extends Figure
{
    //
    //  The whole Figure interface.
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

    //
    //  Transform freely interface.
    //
    /**
     * Enable the hint that a figure should allow itself to transform
     * freely, rather than obey any constraints (this is meant for
     * figures that could not otherwise redraw themselves to fit a
     * resized Plot, given their normal constraints, e.g. XRangeFigure).
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
