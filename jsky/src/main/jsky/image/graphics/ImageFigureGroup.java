/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ImageFigureGroup.java,v 1.4 2002/07/09 13:30:37 brighton Exp $
 */

package jsky.image.graphics;

import diva.canvas.CompositeFigure;
import diva.canvas.Figure;
import diva.canvas.interactor.Interactor;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import jsky.graphics.CanvasFigure;
import jsky.graphics.CanvasFigureGroup;
import jsky.graphics.CanvasFigureListener;
import jsky.graphics.CanvasFigureListenerManager;
import diva.canvas.interactor.SelectionInteractor;


/**
 * Represents a group of figures, that are treated like a single figure.
 *
 * @version $Revision: 1.4 $
 * @author Allan Brighton
 */
public class ImageFigureGroup extends CompositeFigure implements CanvasFigureGroup {

    /** An arbitrary object to store with the figure for later reference */
    protected Object clientData;

    /** Manages a list of listeners for figure events */
    protected CanvasFigureListenerManager listenerManager;

    /** Optional linked list of slave figures, which should be moved with this figure. */
    protected LinkedList slaves;


    /**
     * Create an image figure group with the given shape, fill, outline and line width.
     *
     * @param interactor determines the selection behavior of the figure group (may be null)
     */
    public ImageFigureGroup(Interactor interactor) {
        super();

        listenerManager = new CanvasFigureListenerManager(this);

        if (interactor != null)
            setInteractor(interactor);
    }

    /**
     * Add a figure to the group.
     */
    public void add(CanvasFigure fig) {
        super.add((Figure) fig);
    }

    /**
     * Remove a figure from the group.
     */
    public void remove(CanvasFigure fig) {
        super.remove((Figure) fig);
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
        return getBounds();  // not applicable here
    }

    /** Return true if the figure is selected. */
    public boolean isSelected() {
        return (getInteractor() instanceof SelectionInteractor);
    }

    /**
     * Set the visibility flag of this object. If the flag is false,
     * then the object will not be painted on the screen.
     */
    public void setVisible(boolean flag) {
        super.setVisible(flag);
        repaint();
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

    /** Translate the figure with by the given distance.
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


