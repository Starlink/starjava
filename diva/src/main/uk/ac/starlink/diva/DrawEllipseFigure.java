/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 */
package uk.ac.starlink.diva;

import java.awt.AlphaComposite;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

/**
 * A figure that displays as an ellipse. This is a convenience class
 * for creating ellipses for use on a {@link Draw} component.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see Draw
 */
public class DrawEllipseFigure
    extends DrawBasicFigure
{
    /** 
     * Create a new instance with the given ellipse, a unit-width
     * continuous stroke and no paint pattern.
     */
    public DrawEllipseFigure( Ellipse2D ellipse )
    {
        super( ellipse );
    }

    /** 
     * Create a new instance with the given origin and size, a
     * unit-width continuous stroke and no paint pattern.
     */
    public DrawEllipseFigure( double x, double y, double width, double height )
    {
        super( new Ellipse2D.Double( x, y, width, height ) );
    }

    /** 
     * Create a new instance with the given origin, size, and fill
     * paint. It has no outline.
     */
    public DrawEllipseFigure( double x, double y, double width, double height,
                              Paint fill )
    {
        super( new Ellipse2D.Double( x ,y, width, height ), fill );
    }

    /** 
     * Create a new instance with the given origin, size, and
     * outline width. It has no fill.
     */
    public DrawEllipseFigure( double x, double y, double width, double height,
                              float lineWidth )
    {
        super( new Ellipse2D.Double( x, y, width, height ), lineWidth );
    }

    /** 
     * Create a new instance with the given origin, size, fill, and
     * outline width.
     */
    public DrawEllipseFigure ( double x, double y, double width, double height,
                               Paint fill, Paint outline, float lineWidth,
                               AlphaComposite composite )
    {
        super( new Ellipse2D.Double( x, y, width, height ), fill, lineWidth );
        setStrokePaint( outline );
        setComposite( composite );
    }

    /**
     * Translate the ellipse the given distance
     */
    public void translate( double x, double y )
    {
        Shape s = getShape();
        if ( s instanceof Ellipse2D ) {
            Ellipse2D ell = (Ellipse2D)s;
            repaint();
            ell.setFrame( ell.getX() + x, ell.getY() + y, 
                          ell.getWidth(), ell.getHeight() );
            repaint();
            fireChanged();
        } 
        else {
            super.translate( x, y );
        }
    }
}
