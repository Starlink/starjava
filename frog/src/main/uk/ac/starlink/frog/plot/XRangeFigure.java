package uk.ac.starlink.frog.plot;

import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/*
 * Copyright (c) 1998 The Regents of the University of California.
 * All rights reserved.  See the file COPYRIGHT for details.
 */
/**
 * A figure that displays a optionally fixed height rectangle that can
 * only be moved side to side, or a plain rectangle. This is meant to
 * represent a "range" along the X axis of a Plot.
 *
 * @since $Date$
 * @since 08-JAN-2001
 * @author John Reekie (original version, was
 *         diva.canvas.toolbox.BasicRectangle).
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 * @see BasicPlotFigure, BasicFigure
 *
 */
public class XRangeFigure extends BasicPlotFigure
{
    /**
     * Create a new rectangle with the given rectangle shape, a
     * unit-width continuous stroke and no paint pattern.
     */
    public XRangeFigure ( Rectangle2D rect )
    {
        super( rect );
    }

    /**
     * Create a new rectangle with the given origin and size, a
     * unit-width continuous stroke and no paint pattern.
     */
    public XRangeFigure ( double x, double y, double width,
                          double height )
    {
        super( new Rectangle2D.Double( x, y, width, height ) );
    }

    /**
     * Create a new rectangle with the given origin, size, and
     * fill paint. It has no outline.
     */
    public XRangeFigure ( double x, double y, double width, double height,
                          Paint fill )
    {
        super( new Rectangle2D.Double( x, y, width, height ), fill );
    }

    /**
     * Create a new rectangle with the given origin, size, and
     * outline width. It has no fill.
     */
    public XRangeFigure ( double x, double y, double width, double height,
                          float lineWidth )
    {
        super( new Rectangle2D.Double( x, y, width, height ),
               lineWidth );
    }

    /**
     * Create a new rectangle with the given origin, size, fill, and
     * outline width.
     */
    public XRangeFigure ( double x, double y, double width, double height,
                          Paint fill, float lineWidth )
    {
        super( new Rectangle2D.Double( x, y, width, height ), fill,
               lineWidth );
    }

    /**
     * Whether XRangeFigures are free to transform.
     */
    protected boolean constrain = true;

    /**
     * Whether figures are XRangeFigure with a free or constrained
     * geometry or not.
     */
    public void setConstrain( boolean constrain )
    {
        this.constrain = constrain;
    }

    /**
     * Find out if the XRangeFigures are contrained.
     */
    public boolean getConstrain()
    {
        return constrain;
    }

    /**
     * Translate the rectangle the given distance, but only in X,
     * unless we're unconstrained.
     */
    public void translate ( double x, double y )
    {
        Shape s = getShape();
        if ( constrain ) y = 0.0; 
        if ( s instanceof Rectangle2D ) {
            Rectangle2D r = (Rectangle2D)s;
            repaint();
            r.setFrame( r.getX() + x, r.getY() + y, r.getWidth(),
                        r.getHeight() );
            repaint();
            fireChanged();
        } else {
            super.translate( x, y );
        }
    }

    /**
     * Transform the figure. Just allow transforms of X scale.
     */
    public void transform ( AffineTransform at )
    {
        if ( isTransformFreely() || ! constrain ) {
            super.transform( at );
        } else {
            AffineTransform xOnly = new AffineTransform();
            xOnly.translate( at.getTranslateX(), 0.0 );
            xOnly.scale( at.getScaleX(), 1.0 );
            super.transform( xOnly );
        }
    }
}
