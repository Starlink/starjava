/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: CanvasFigureListenerAdapter.java,v 1.2 2002/07/09 13:30:36 brighton Exp $
 */

package jsky.graphics;

import java.util.EventListener;

/**
 * Adapter that does nothing, but implements the CanvasFigureListener interface.
 *
 * @version $Revision: 1.2 $
 * @author Allan Brighton
 */
public class CanvasFigureListenerAdapter implements CanvasFigureListener {

    /**
     * Invoked when the figure is selected.
     */
    public void figureSelected(CanvasFigureEvent e) {
    }

    /**
     * Invoked when the figure is deselected.
     */
    public void figureDeselected(CanvasFigureEvent e) {
    }

    /**
     * Invoked when the figure's size changes.
     */
    public void figureResized(CanvasFigureEvent e) {
    }

    /**
     * Invoked when the figure's position changes.
     */
    public void figureMoved(CanvasFigureEvent e) {
    }
}
