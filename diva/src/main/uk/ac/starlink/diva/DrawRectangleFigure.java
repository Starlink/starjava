/*
 * Copyright (c) 1998 The Regents of the University of California.
 * All rights reserved.  See the file DIVA COPYRIGHT for details.
 *
 * Copyright (C) 2001 Central Laboratory of the Research Councils
 */
package uk.ac.starlink.diva;

import java.awt.Paint;
import java.awt.Shape;
import java.awt.AlphaComposite;
import java.awt.geom.Rectangle2D;

/**
 * A figure that displays as a rectangle. This is a convenience class
 * for creating rectangles. It inherits from DrawBasicFigure, and so 
 * contains a single Rectangle2D as its shape. It provides a useful 
 * set of constructors.
 *
 * Changed by Peter W. Draper to inherit from DrawBasicFigure so we
 * can use it on {@link Draw} implementations.
 *
 * @version	$Revision$
 * @author 	John Reekie
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see Draw
 */
public class DrawRectangleFigure
    extends DrawBasicFigure
{
    /** Create a new rectangle with the given rectangle shape, a
     * unit-width continuous stroke and no paint pattern.
     */
    public DrawRectangleFigure( Rectangle2D rect )
    {
        super( rect );
    }

    /** Create a new rectangle with the given origin and size, a
     * unit-width continuous stroke and no paint pattern.
     */
    public DrawRectangleFigure( double x, double y,
                                double width, double height )
    {
        super( new Rectangle2D.Double( x, y, width, height ) );
    }

    /** Create a new rectangle with the given origin, size, and
     * fill paint. It has no outline.
     */
    public DrawRectangleFigure( double x, double y, 
                                double width, double height,
                                Paint fill )
    {
        super( new Rectangle2D.Double( x ,y, width, height ), fill );
    }

    /** Create a new rectangle with the given origin, size, and
     * outline width. It has no fill.
     */
    public DrawRectangleFigure( double x, double y, 
                                double width, double height,
                                float lineWidth )
    {
        super( new Rectangle2D.Double( x, y, width, height ), lineWidth );
    }

    /** Create a new rectangle with the given origin, size, fill, and
     * outline width.
     */
    public DrawRectangleFigure ( double x, double y, 
                                 double width, double height,
                                 Paint fill, Paint outline, float lineWidth,
                                 AlphaComposite composite )
    {
        super(new Rectangle2D.Double( x, y, width, height ), fill, lineWidth);
        setStrokePaint( outline );
        setComposite( composite );
    }

    /**
     * Translate the rectangle the given distance
     */
    public void translate( double x, double y )
    {
        Shape s = getShape();
        if ( s instanceof Rectangle2D ) {
            Rectangle2D r = (Rectangle2D)s;
            repaint();
            r.setFrame( r.getX() + x, r.getY() + y, 
                        r.getWidth(), r.getHeight() );
            repaint();
            fireChanged();
        } 
        else {
            super.translate( x, y );
        }
    }
}
