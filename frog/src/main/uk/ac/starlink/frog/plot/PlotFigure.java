package uk.ac.starlink.frog.plot;

/**
 * PlotFigure defines an interface that any Figures drawn on a Plot
 * should implement. see BasicPlotFigure for a concrete implementation
 * of this interface.
 *
 * @since $Date$
 * @since 08-JAN-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 * @see Plot, Figure
 */
public interface PlotFigure
{
    //  Common BasicFigure members.
    //public Paint getFillPaint ();

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
