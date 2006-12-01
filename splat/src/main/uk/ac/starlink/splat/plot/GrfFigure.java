/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     05-NOV-2003 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.plot;

import diva.canvas.AbstractFigure;
import diva.canvas.interactor.Interactor;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import uk.ac.starlink.splat.util.SplatException;

/**
 * A Figure that encapsulates all the AST graphics drawn in a DivaPlot.
 * This is a very simple figure in that it does not respond to events
 * or transformations. Its only reason for existence is so that the
 * drawing sequence for AST graphics is controllable as part of the
 * Figure drawing (this allows it to be part of the JCanvas ZList and
 * to be made invisible).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class GrfFigure
    extends AbstractFigure
{
    protected DivaPlot plot = null;

    //  This should largely be a proxy class for the DivaPlot.
    public GrfFigure( DivaPlot plot )
    {
        this.plot = plot;
    }

    public void paint( Graphics2D g )
    {
        if ( isVisible() ) {
            if ( ! plot.redrawAll( g ) ) {
                //  Allow a redraw on one failure, this should rescale the
                //  plot.
                plot.redrawAll( g );
            }
        }
    }

    public void transform( AffineTransform at )
    {
        //  Does nothing.
    }

    public void translate( double x, double y )
    {
        //  Does nothing.
    }

    //  Bounds of a plot is the visible part, we don't draw more than
    //  that in general?
    public Rectangle2D getBounds()
    {
        return plot.getVisibleRect();
    }

    //  The nominal shape is the something that encapsulates all the spectra.
    public Shape getShape()
    {
        return getBounds();
    }

    //  No interaction with this is possible.
    public Interactor getInteractor()
    {
        return null;
    }

    //  Has no fill.
    public boolean hit( Rectangle2D r )
    {
        return false;
    }

    //  Contains nothing.
    public boolean contains( Point2D p ) 
    {
        return false;
    }

    //  Never intersects anything.
    public boolean intersects( Rectangle2D r ) 
    {
        return false;
    }
}

