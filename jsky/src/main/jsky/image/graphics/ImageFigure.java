/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ImageFigure.java,v 1.6 2002/07/09 13:30:37 brighton Exp $
 */

package jsky.image.graphics;

import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.ListIterator;

import jsky.graphics.CanvasFigure;
import jsky.graphics.CanvasFigureListener;
import jsky.graphics.CanvasFigureListenerManager;

import diva.canvas.Figure;
import diva.canvas.interactor.Interactor;
import diva.canvas.interactor.SelectionInteractor;
import diva.canvas.interactor.SelectionModel;
import diva.canvas.toolbox.BasicFigure;


/**
 * Represents a figure or drawing in an image overlay. For now, this is just
 * a simple subclass of the Diva BasicFigure class. See the
 * <A href="http://www-cad.eecs.berkeley.edu/diva">Diva documentation and examples</A>
 *  for more information.
 *
 * @version $Revision: 1.6 $
 * @author Allan Brighton
 */
public class ImageFigure extends BasicFigure implements CanvasFigure {

    /** An arbitrary object to store with the figure for later reference */
    protected Object clientData;

    /** Manages a list of listeners for figure events */
    protected CanvasFigureListenerManager listenerManager;

    /** Optional linked list of slave figures, which should be moved with this figure. */
    protected LinkedList slaves;


    /**
     * Create an image figure with the given shape, fill, outline and line width.
     *
     * @param shape the shape to draw
     * @param fill the paint to use to fill the shape
     * @param outline the paint to use for the outline
     * @param lineWidth the width of the shape lines in pixels
     * @param interactor determines the behavior of the figure (may be null)
     */
    public ImageFigure(Shape shape, Paint fill, Paint outline, float lineWidth,
                       Interactor interactor) {
        super(shape, fill, lineWidth);

        listenerManager = new CanvasFigureListenerManager(this);

        if (interactor != null)
            setInteractor(interactor);

        setStrokePaint(outline);
    }

    /** Store an arbitrary object with the figure for later reference */
    public void setClientData(Object o) {
        clientData = o;
    }

    /** Return the client data object, or null if none was set */
    public Object getClientData() {
        return clientData;
    }


    /** Return the bounds of this figure, ignoring the label, if there is one. */
    public Rectangle2D getBoundsWithoutLabel() {
        return getBounds();
    }

    /**
     * Test if this shape is hit by the given rectangle
     * (Override parent class version, since the 25-sep-00 version only hits the outline).
     */
    public boolean hit(Rectangle2D r) {
        return getShape().intersects(r);
    }

    /**
     * Set the visibility flag of this object. If the flag is false,
     * then the object will not be painted on the screen.
     */
    public void setVisible(boolean flag) {
        super.setVisible(flag);

        // don't leave selection decorators around...
        if (!flag) {
            Interactor interactor = getInteractor();
            if (interactor instanceof SelectionInteractor) {
                // remove any selection handles, etc.
                SelectionModel model = ((SelectionInteractor) interactor).getSelectionModel();
                if (model.containsSelection(this)) {
                    model.removeSelection(this);
                }
            }
        }

        repaint();
    }

    /** Return true if the figure is selected. */
    public boolean isSelected() {
        return (getInteractor() instanceof SelectionInteractor);
    }

    /** Add a listener for events on the canvas figure */
    public void addCanvasFigureListener(CanvasFigureListener listener) {
        listenerManager.addCanvasFigureListener(listener);
    }

    /** Remove a listener for events on the canvas figure */
    public void removeCanvasFigureListener(CanvasFigureListener listener) {
        listenerManager.removeCanvasFigureListener(listener);
    }

    /**
     * Fire an event on the canvas figure.
     *
     * @param eventType one of the CanvasFigure constants: SELECTED, DESELECTED, RESIZED, MOVED
     */
    public void fireCanvasFigureEvent(int eventType) {
        listenerManager.fireCanvasFigureEvent(eventType);
    }


    /** Add a slave figure. When this figure is moved, the slaves will also move. */
    public void addSlave(CanvasFigure fig) {
        if (slaves == null)
            slaves = new LinkedList();
        slaves.add(fig);
    }

    /** Translate the figure by the given distance.
     * As much as possible, this method attempts
     * to preserve the type of the shape: if the shape of this figure
     * is an of RectangularShape or Polyline, then the shape may be
     * modified directly. Otherwise, a general transformation is used
     * that loses the type of the shape, converting it into a
     * GeneralPath.
     */
    public void translate(double x, double y) {
        super.translate(x, y);

        if (slaves != null) {
            ListIterator it = slaves.listIterator(0);
            while (it.hasNext()) {
                Figure fig = (Figure) it.next();
                fig.translate(x, y);
            }
        }
    }
}


