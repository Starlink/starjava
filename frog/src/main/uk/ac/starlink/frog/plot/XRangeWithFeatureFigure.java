package uk.ac.starlink.frog.plot;

import diva.canvas.event.MouseFilter;
import diva.canvas.interactor.BoundedDragInteractor;
import diva.canvas.interactor.Interactor;

import java.awt.Color;
import java.awt.geom.Rectangle2D;

/**
 * Create a composite figure that contains a range defining figure and
 * a position defining figure.
 *
 * @since $Date$
 * @since 19-JAN-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 * @see PlotCompositeFigure
 */
public class XRangeWithFeatureFigure extends PlotCompositeFigure
{
    /**
     * Reference to background Figure.
     */
    protected XRangeFigure backFigure;

    /**
     * Reference to line figure.
     */
    protected Pointer lineFigure;

    /**
     *  Create the composite from the two required figures.
     */
    public XRangeWithFeatureFigure( double xleft, double ytop, 
                                    double xcentre, double width, 
                                    double height, Color backColour, 
                                    Color lineColour )
    {
        super();

        //  Create figure parts, a normal XRangeFigure and thin Figure
        //  to represent an X position.
        backFigure = new XRangeFigure( xleft, ytop, width, height, 
                                       backColour );
        //lineFigure = new XRangeFigure( xcentre, ytop, 2.0, height, 
        //                               lineColour );
        lineFigure = new Pointer( true, xcentre, ytop, height * 0.5 );

        //  Main figure is set as background.
        setBackgroundFigure( backFigure );

        //  Line figure is added, but needs to constrained to not
        //  leave the bounds of the XRangeFigure.
        Rectangle2D bounds = backFigure.getBounds();
        Interactor boundedDragger = new BoundedDragInteractor( bounds );
        boundedDragger.setMouseFilter( MouseFilter.defaultFilter);
        lineFigure.setInteractor( boundedDragger );
        add( lineFigure );
    }
}
