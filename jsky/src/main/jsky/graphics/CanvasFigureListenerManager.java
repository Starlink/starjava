/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: CanvasFigureListenerManager.java,v 1.2 2002/07/09 13:30:36 brighton Exp $
 */

package jsky.graphics;

import javax.swing.event.EventListenerList;


/**
 * Manages a list of listeners for events on a CanvasFigure.
 *
 * @version $Revision: 1.2 $
 * @author Allan Brighton
 */
public class CanvasFigureListenerManager {

    /** list of listeners for figure events */
    protected EventListenerList listenerList = new EventListenerList();

    /** The target figure. */
    protected CanvasFigure figure;

    /** The event fired by this class */
    CanvasFigureEvent event;


    /** Initialize to manage listeners for the given figure */
    public CanvasFigureListenerManager(CanvasFigure figure) {
        this.figure = figure;
        event = new CanvasFigureEvent(figure);
    }


    /** Add a listener for events on the canvas figure */
    public void addCanvasFigureListener(CanvasFigureListener listener) {
        listenerList.add(CanvasFigureListener.class, listener);
    }

    /** Remove a listener for events on the canvas figure */
    public void removeCanvasFigureListener(CanvasFigureListener listener) {
        listenerList.remove(CanvasFigureListener.class, listener);
    }

    /**
     * Notify any listeners of a figure event on the given figure.
     *
     * @param eventType one of the CanvasFigure constants:
     *                  SELECTED, DESELECTED, RESIZED, MOVED
     */
    public void fireCanvasFigureEvent(int eventType) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == CanvasFigureListener.class) {
                CanvasFigureListener listener = (CanvasFigureListener) listeners[i + 1];
                switch (eventType) {
                case CanvasFigure.SELECTED:
                    listener.figureSelected(event);
                    break;
                case CanvasFigure.DESELECTED:
                    listener.figureDeselected(event);
                    break;
                case CanvasFigure.RESIZED:
                    listener.figureResized(event);
                    break;
                case CanvasFigure.MOVED:
                    listener.figureMoved(event);
                    break;
                }
            }
        }
    }
}


