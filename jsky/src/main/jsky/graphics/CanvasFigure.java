/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: CanvasFigure.java,v 1.3 2002/07/09 13:30:36 brighton Exp $
 */

package jsky.graphics;

import java.awt.geom.Rectangle2D;

/**
 * This defines an abstract interface for figures drawn on a canvas.
 *
 * @version $Revision: 1.3 $
 * @author Allan Brighton
 */
public abstract interface CanvasFigure {

    /** Indicates that the figure was selected */
    public static final int SELECTED = 0;

    /** Indicates that the figure was deselected */
    public static final int DESELECTED = 1;

    /** Indicates that the figure was resized */
    public static final int RESIZED = 2;

    /** Indicates that the figure was dragged */
    public static final int MOVED = 3;


    /** Store an arbitrary object with the figure for later reference */
    public void setClientData(Object o);

    /** Return the client data object, or null if none was set */
    public Object getClientData();

    /** Return true if the figure is selected. */
    public boolean isSelected();

    /**
     * Test the visibility flag of this object.
     */
    public boolean isVisible();

    /** Return the bounds of this figure */
    public Rectangle2D getBounds();

    /** Return the bounds of this figure, ignoring the label, if there is one. */
    public Rectangle2D getBoundsWithoutLabel();

    /**
     * Set the visibility flag of this object.
     */
    public void setVisible(boolean flag);


    /** Add a listener for events on the canvas figure */
    public void addCanvasFigureListener(CanvasFigureListener listener);

    /** Remove a listener for events on the canvas figure */
    public void removeCanvasFigureListener(CanvasFigureListener listener);

    /**
     * Fire an event on the canvas figure.
     *
     * @param eventType one of SELECTED, DESELECTED, RESIZED, MOVED
     */
    public void fireCanvasFigureEvent(int eventType);

    /** Add a slave figure. When this figure is moved, the slaves will also move. */
    public void addSlave(CanvasFigure fig);
}

