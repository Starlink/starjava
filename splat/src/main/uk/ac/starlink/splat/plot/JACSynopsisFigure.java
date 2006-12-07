/*
 * Copyright (C) 2006 Particle Physics and Astronomy Research Council
 *
 *  History:
 *     01-DEC-2006 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.plot;

import diva.canvas.AbstractFigure;
import diva.canvas.interactor.Interactor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.diva.DrawLabelFigure;
import uk.ac.starlink.diva.DrawRectangleFigure;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.util.SplatException;

/**
 * A Figure that describes some of the meta-data properties of a
 * spectrum. These are modelled on those liked by the JCMT observers
 * at the JAC (basically those displayed by Specx). The Figure is
 * really a {@link DrawLabelFigure}, but one which retains a fixed position
 * inside a {@link JViewport}, so that it remains visible.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class JACSynopsisFigure
    extends DrawLabelFigure
{
    /** The SpecData instance */
    private SpecData specData = null;

    /** The JViewport we're at a fixed position within */
    private JViewport viewport = null;

    /** The underlying location within viewport */
    private Point anchor = null;

    /**
     * Create an instance.
     *
     * @param specData the {@link SpecData} instance.
     * @param viewport the {@link JViewport} that this figure is being
     *                 displayed within.
     * @param anchor the initial position within the {@link JViewport}.
     *               This will change as the figure is dragged around.
     */
    public JACSynopsisFigure( SpecData specData, JViewport viewport,
                              Point anchor )
    {
        super( "JACSynopsisFigure\nName=\nTSYS=\n" );

        this.anchor = anchor.getLocation();
        setSpecData( specData );
        setViewport( viewport );
    }

    /**
     * Set the SpecData instance.
     */
    public void setSpecData( SpecData specData )
    {
        this.specData = specData;
        gatherProperties();
        fireChanged();
    }

    //  Create the String that represents the synopsis.
    protected void gatherProperties()
    {
        FrameSet frameSet = specData.getFrameSet();

        StringBuffer b = new StringBuffer();

        b.append( "Epoch: " + frameSet.getEpoch() + "\n" );
        b.append( "Short name: " + specData.getShortName() + "\n" );
        b.append( "Full name: " + specData.getFullName() + "\n" );
        b.append( "Rest Freq: " + frameSet.getC( "RestFreq" ) + " (GHz)\n" );
        setString( b.toString() );
    }

    /**
     * Set the {@link JViewport} instance. The position of our Rectangle is
     * fixed within the visible portion of this.
     */
    public void setViewport( JViewport viewport )
    {
        this.viewport = viewport;

        // We want to know when this changes so we can redraw the text at the
        // correct position so register for any changes.
        viewport.addChangeListener( new ChangeListener()
            {
                public void stateChanged( ChangeEvent e )
                {
                    reAnchor();
                }
            });

        fireChanged();
    }

    /**
     * Reset the text anchor position so that the position within the viewport
     * is retained.
     */
    protected void reAnchor()
    {
        Point p = viewport.getViewPosition();
        p.translate( anchor.x, anchor.y );

        Point2D a = getAnchorPoint();
        translate( p.getX() - a.getX(), p.getY() - a.getY(), false );
    }

    // Don't want this to transform, needs to retain its size.
    public void transform( AffineTransform at )
    {
        // Do nothing.
    }

    //  Like translate, but with optional reset of local anchor position
    //  (do after interactive drag, but not when viewport updates).
    public void translate( double x, double y, boolean reset )
    {
        //  Cheat big time and use transform of super-class, not translate.
        //  That avoids having a transform in this class, which is where the
        //  translation request end up.
        super.transform( AffineTransform.getTranslateInstance( x, y ) );

        if ( reset ) {
            //  Reset local anchor wrt to viewport.
            Point2D a = getAnchorPoint();
            Point p = viewport.getViewPosition();
            anchor.setLocation( (int)(a.getX() - p.getX()),
                                (int)(a.getY() - p.getY()) );
        }
    }

    //  Normal translate.
    public void translate( double x, double y )
    {
        //  Reset anchor as should be from an interactive movement.
        translate( x, y, true );
    }
}
