/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     08-JAN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.diva;

import diva.util.java2d.Polygon2D;
import java.awt.AlphaComposite;
import java.awt.Paint;

/**
 * A figure that displays a polyline that can have its vertices moved.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see DrawBasicFigure
 */
public class DrawPolygonFigure 
    extends DrawBasicFigure
{
    /**
     * The Polygon that we're using.
     */
    protected Polygon2D.Double polygon = null;

    /**
     * Create a new polygon using the given polygon shape.
     * This has unit-width continuous stroke and no paint pattern.
     */
    public DrawPolygonFigure( Polygon2D poly )
    {
        super( poly );
    }

    /**
     * Create a new polygon using the given polygon shape, colour,
     * fill and linewidth.
     */
    public DrawPolygonFigure( Polygon2D poly, Paint fill, Paint outline, 
                              float lineWidth, AlphaComposite composite )
    {
        super( poly, fill, outline, lineWidth );
        setComposite( composite );
    }

    /**
     * Create a new polygon with the given origin and colour.
     */
    public DrawPolygonFigure( double x, double y, Paint fill, Paint outline, 
                              float lineWidth, AlphaComposite composite )
    {
        super( null );
        polygon = createPolygon( x, y );
        setShape( polygon );
        setLineWidth( lineWidth );
        setStrokePaint( outline );
        setFillPaint( fill );
        setComposite( composite );
    }

    /**
     * Create a new polyline with the given origin, colour and linewidth.
     */
    public DrawPolygonFigure( double x, double y, Paint fill, 
                              float lineWidth )
    {
        super( null );
        polygon = createPolygon( x, y );
        setShape( polygon );
        setFillPaint( fill );
    }

    /**
     * Create a polygon and position it at the origin.
     */
    public Polygon2D.Double createPolygon( double x, double y )
    {
        polygon = new Polygon2D.Double();
        polygon.moveTo( x, y );
        return polygon;
    }
}
