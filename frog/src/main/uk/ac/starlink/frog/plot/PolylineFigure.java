package uk.ac.starlink.frog.plot;

import diva.util.java2d.Polyline2D;

import java.awt.Paint;

/**
 * A figure that displays a polyline that can have its vertices moved.
 *
 * @since $Date$
 * @since 08-JAN-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 * @see BasicPlotFigure, BasicFigure
 *
 */
public class PolylineFigure extends PathPlotFigure
{
    /**
     * The Polyline that we're using.
     */
    protected Polyline2D.Double polyline = null;

    /**
     * Create a new polyline using the given polyline shape.
     * This has unit-width continuous stroke and no paint pattern.
     */
    public PolylineFigure ( Polyline2D poly )
    {
        super( poly );
    }

    /**
     * Create a new polyline using the given polyline shape, colour
     * and linewidth.
     */
    public PolylineFigure ( Polyline2D poly, Paint fill, float lineWidth )
    {
        super( poly, fill, lineWidth );
    }

    /**
     * Create a new polyline with the given origin and colour.
     */
    public PolylineFigure ( double x, double y, Paint fill )
    {
        super( null );
        polyline = createPolyline( x, y );
        setShape( polyline );
    }

    /**
     * Create a new rectangle with the given origin, colour and linewidth.
     */
    public PolylineFigure ( double x, double y, Paint fill, 
                            float lineWidth )
    {
        super( null );
        polyline = createPolyline( x, y );
        setShape( polyline );
        setStrokePaint( fill );
    }

    /**
     * Create a polyline and position it at the origin.
     */
    public Polyline2D.Double createPolyline( double x, double y )
    {
        polyline = new Polyline2D.Double();
        polyline.moveTo( x, y );
        return polyline;
    }
}
