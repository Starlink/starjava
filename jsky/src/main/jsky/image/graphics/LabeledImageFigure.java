/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: LabeledImageFigure.java,v 1.6 2002/07/09 13:30:37 brighton Exp $
 */

package jsky.image.graphics;

import java.awt.Font;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.swing.SwingConstants;

import jsky.graphics.CanvasFigure;
import jsky.graphics.CanvasFigureListener;
import jsky.graphics.CanvasFigureListenerManager;

import diva.canvas.Figure;
import diva.canvas.interactor.Interactor;
import diva.canvas.interactor.SelectionInteractor;
import diva.canvas.interactor.SelectionModel;
import diva.canvas.toolbox.LabelFigure;
import diva.canvas.toolbox.LabelWrapper;


/**
 * Represents a labeled figure or drawing in an image overlay.
 *
 * @version $Revision: 1.6 $
 * @author Allan Brighton
 */
public class LabeledImageFigure extends LabelWrapper implements CanvasFigure {

    /** An arbitrary object to store with the figure for later reference */
    protected Object clientData;

    /** Manages a list of listeners for figure events */
    protected CanvasFigureListenerManager listenerManager;

    /** Optional linked list of slave figures, which should be moved with this figure. */
    protected LinkedList slaves;

    /**
     * Create an image figure with the given shape, fill, outline and line width.
     *
     * @param fig the figure to display with the label
     * @param label the text of the label
     * @param anchor SwingConstants value for label position
     * @param color the text color
     * @param font the label's font
     * @param interactor determines the behavior of the figure (may be null)
     */
    public LabeledImageFigure(Figure fig, String label, int anchor, Paint color, Font font,
                              Interactor interactor) {
        super(fig, label);

        listenerManager = new CanvasFigureListenerManager(this);

        setAnchor(anchor);
        LabelFigure labelFig = getLabel();
        labelFig.setFillPaint(color);
        labelFig.setFont(font);
        labelFig.setAnchor(SwingConstants.WEST);

        if (interactor != null)
            setInteractor(interactor);
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
        return getChild().getBounds();
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

        // don't leave selection decorators around...
        Interactor interactor = getInteractor();
        if (interactor instanceof SelectionInteractor) {
            // remove any selection handles, etc.
            SelectionModel model = ((SelectionInteractor) interactor).getSelectionModel();
            model.removeSelection(this);
        }

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


