/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     25-OCT-2002 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.sog.photom; // Move to sog when useful.

import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import javax.swing.AbstractAction;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import diva.canvas.AbstractFigure;
import diva.canvas.Figure;
import diva.canvas.event.LayerAdapter;
import diva.canvas.event.LayerEvent;
import diva.canvas.interactor.CircleManipulator;
import diva.canvas.interactor.SelectionInteractor;
import diva.canvas.interactor.GrabHandle;
import diva.canvas.toolbox.BasicFigure;

import jsky.graphics.CanvasFigure;
import jsky.image.graphics.gui.CanvasDraw;
import jsky.image.gui.DivaMainImageDisplay;
import jsky.image.graphics.ImageFigure;

/**
 * Extend the JSKY CanvasDraw to add our own figures.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SOGCanvasDraw extends CanvasDraw
{
    public SOGCanvasDraw( DivaMainImageDisplay imageDisplay )
    {
        super( imageDisplay );
    }

    // ChangeListener implementation for notification when figure
    // creation is completed. Users should implement ChangeListener.
    protected EventListenerList finishedList = new EventListenerList();

    /**
     * Add a ChangeListener for notification of when a Figure is
     * completed.
     */
    public void addFinishedListener( ChangeListener l )
    {
        finishedList.add( ChangeListener.class, l );
    }

    /**
     * Remove a ChangeListener of when a Figure is completed.
     */
    public void removeFinishedListener( ChangeListener l )
    {
        finishedList.remove( ChangeListener.class, l );
    }

    /**
     * Notify any listeners that figure creation has completed.
     */
    protected void fireFinished( ChangeEvent e )
    {
        Object[] listeners = finishedList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                ((ChangeListener) listeners[i + 1]).stateChanged( e );
            }
        }
    }

    /**
     * Finish off the current figure and select it, also notify any
     * listeners that the figure is finished.
     */
    protected void finishFigure()
    {
        if ( figure != null ) {
            //  Let any interested listeners know and pass on the Figure.
            fireFinished( new ChangeEvent( figure ) );
        }
        super.finishFigure();
    }

    //
    //  Create Actions for our extra figures...
    //

    /** Mode to create a circle with annulus */
    public static final int ANNULAR_CIRCLE = NUM_DRAWING_MODES; // next +1

    /** Return the action for the given mode */
    public AbstractAction getDrawingModeAction( int drawingMode )
    {
        if ( drawingMode < drawingModeActions.length ) {
            return super.getDrawingModeAction( drawingMode );
        }
        return new SOGDrawingModeAction( ANNULAR_CIRCLE ); // Only one valid.
    }

    /** Local class used to set the drawing mode. */
    class SOGDrawingModeAction extends AbstractAction
    {
        int drawingMode;

        public SOGDrawingModeAction( int drawingMode )
        {
            super( "annularcircle" );
            this.drawingMode = drawingMode;
        }

        public void actionPerformed( ActionEvent evt )
        {
            setDrawingMode( drawingMode );
        }
    }

    //
    // Deal with Figure interactions.
    //
    public void mousePressed( MouseEvent e )
    {
        if ( drawingMode == ANNULAR_CIRCLE ) {
            startX = e.getX();
            startY = e.getY();
            SelectionInteractor interactor = graphics.getSelectionInteractor();
            CircleManipulator manipulator = new CircleManipulator();

            // Make sure resized events get passed as from DivaImageGraphics.
            manipulator.getHandleInteractor()
                .addLayerListener( new LayerAdapter()
                    {
                        public void mouseReleased( LayerEvent e )
                        {
                            Figure fig = e.getFigureSource();
                            if ( fig instanceof GrabHandle ) {
                                fig = ((GrabHandle) fig).getSite().getFigure();
                                fig = ((CircleManipulator) fig).getDecoratedFigure();
                                if ( fig instanceof CanvasFigure ) {
                                    ((CanvasFigure) fig)
                                        .fireCanvasFigureEvent(CanvasFigure.RESIZED);
                                }
                            }
                        }
                    });
            interactor.setPrototypeDecorator( manipulator );

            AnnulusFigure afigure = new AnnulusFigure( 10.0,
                                                       fill, outline,
                                                       lineWidth,
                                                       interactor );
            afigure.setPosition( new Point2D.Double( startX, startY ) );
            this.figure = (AbstractFigure) afigure;
            graphics.add( (CanvasFigure) figure );
            figureList.add( figure );
        }
        else {
            super.mousePressed( e );
        }
    }

    public void mouseDragged( MouseEvent e )
    {
        if ( drawingMode == ANNULAR_CIRCLE ) {
            // Change the radius.
            if ( figure != null ) {
                int radius = e.getX() - startX;
                ((AnnulusFigure) figure).setRadius( radius );
            }
        }
        else {
            super.mouseDragged( e );
        }
    }

    public void mouseReleased( MouseEvent e )
    {
        if ( drawingMode == ANNULAR_CIRCLE ) {
            finishFigure();
        }
        else {
            super.mouseReleased( e );
        }
    }

    /**
     * Remove a figure from display.
     */
    public void removeFigure( CanvasFigure figure )
    {
        if ( figureList.contains( figure ) ) {
            graphics.remove( figure );
            figureList.remove( figure );
        }
    }

}
