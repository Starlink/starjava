/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ImageLabel.java,v 1.8 2002/07/09 13:30:37 brighton Exp $
 */

package jsky.image.graphics;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.swing.SwingConstants;

import jsky.graphics.CanvasFigure;
import jsky.graphics.CanvasFigureListener;
import jsky.graphics.CanvasFigureListenerManager;

import diva.canvas.CanvasUtilities;
import diva.canvas.Figure;
import diva.canvas.interactor.Interactor;
import diva.canvas.interactor.SelectionInteractor;
import diva.canvas.toolbox.LabelFigure;

/**
 * Represents a label figure drawn on the image.
 *
 * @version $Revision: 1.8 $
 * @author Allan Brighton
 */
public class ImageLabel extends LabelFigure implements CanvasFigure {

    /** An arbitrary object to store with the figure for later reference */
    protected Object clientData;

    /** Manages a list of listeners for figure events */
    protected CanvasFigureListenerManager listenerManager;

    /** Optional linked list of slave figures, which should be moved with this figure. */
    protected LinkedList slaves;

    /** Make sure composites from other figures are not used here */
    protected Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0F);


    /**
     * Create an image figure with the given shape, fill, outline and line width.
     *
     * @param text the text of the label to draw
     * @param pos the label position
     * @param fill the paint to use to draw the text
     * @param font the font to use for the label
     * @param interactor determines the behavior of the figure (may be null)
     */
    public ImageLabel(String text, Point2D.Double pos, Paint fill, Font font, Interactor interactor) {
        super(text, font);

        listenerManager = new CanvasFigureListenerManager(this);

        setAnchor(SwingConstants.SOUTH_WEST);
        setFillPaint(fill);

        if (interactor != null)
            setInteractor(interactor);

        translate(pos.x, pos.y);
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
     * Paint the label.
     */
    public void paint(Graphics2D g) {
        g.setComposite(composite);
        super.paint(g);
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

    /**
     * Get the location at which the anchor is currently located.
     * This method looks at the anchor and padding attributes to
     * figure out the point.
     */
    public Point2D getAnchorPoint() {
        return CanvasUtilities.getLocation(getBounds(), SwingConstants.SOUTH_WEST);
    }
}


