/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     08-JAN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.diva;

import diva.util.java2d.Polyline2D;
import java.awt.AlphaComposite;
import java.awt.Paint;

/**
 * A figure that displays a polyline that can have its vertices moved.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see DrawBasicFigure
 */
public class DrawPolylineFigure 
    extends DrawPathFigure
{
    /**
     * The Polyline that we're using.
     */
    protected Polyline2D.Double polyline = null;

    /**
     * Create a new polyline using the given polyline shape.
     * This has unit-width continuous stroke and no paint pattern.
     */
    public DrawPolylineFigure( Polyline2D poly )
    {
        super( poly );
    }

    /**
     * Create a new polyline using the given polyline shape, colour
     * and linewidth.
     */
    public DrawPolylineFigure( Polyline2D poly, Paint fill, float lineWidth,
                               AlphaComposite composite )
    {
        super( poly, fill, lineWidth );
        setComposite( composite );
    }

    /**
     * Create a new polyline with the given origin and colour.
     */
    public DrawPolylineFigure( double x, double y, Paint fill )
    {
        super( null );
        polyline = createPolyline( x, y );
        setShape( polyline );
    }

    /**
     * Create a new polyline with the given origin, colour and linewidth.
     */
    public DrawPolylineFigure( double x, double y, Paint fill, 
                               float lineWidth, AlphaComposite composite )
    {
        super( null );
        polyline = createPolyline( x, y );
        setShape( polyline );
        setLineWidth( lineWidth );
        setStrokePaint( fill );
        setComposite( composite );
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
