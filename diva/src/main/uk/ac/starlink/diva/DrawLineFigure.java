/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     02-01-2004 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.diva;

import java.awt.AlphaComposite;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 * A figure that displays as a line. This is a convenience class
 * so that a line can be distinguished from other figure types on a
 * {@link Draw} implementation.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class DrawLineFigure
    extends DrawPathFigure
{
    /**
     * Create a new instance with unit-width continuous stroke and no
     * paint pattern.
     */
    public DrawLineFigure( Line2D line )
    {
        super( line );
    }

    /**
     * Create a new instance with the given end points, a
     * unit-width continuous stroke and no paint pattern.
     */
    public DrawLineFigure( double x1, double y1, double x2, double y2 )
    {
        super( new Line2D.Double( x1, y1, x2, y2 ) );
    }

    /** 
     * Create a new instance with the given end points, fill, and
     * outline width.
     */
    public DrawLineFigure ( double x1, double y1, double x2, double y2,
                            Paint fill, float lineWidth, 
                            AlphaComposite composite )
    {
        super( new Line2D.Double( x1, y1, x2, y2 ), fill, lineWidth );
        setComposite( composite );
    }

    /**
     * Translate the line by the given distance
     */
    public void translate( double x, double y )
    {
        Shape s = getShape();
        if ( s instanceof Line2D ) {
            Line2D l = (Line2D)s;
            repaint();
            l.setLine( l.getX1() + x, l.getY1() + y,
                       l.getX2() + x, l.getY2() + y );
            repaint();
            fireChanged();
        }
        else {
            super.translate( x, y );
        }
    }
}
