/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: CanvasFigureEvent.java,v 1.2 2002/07/09 13:30:36 brighton Exp $
 */

package jsky.graphics;

import java.util.EventObject;


/**
 * This event is generated when a canvas figure is seletced, deselected,
 * resized or dragged.
 *
 * @version $Revision: 1.2 $
 * @author Allan Brighton
 */
public class CanvasFigureEvent extends EventObject {

    public CanvasFigureEvent(CanvasFigure fig) {
        super(fig);
    }

    /** Return the figure for the event. */
    public CanvasFigure getFigure() {
        return (CanvasFigure) getSource();
    }
}
