/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: SelectedAreaListener.java,v 1.1 2000/07/14 21:17:27 brighton Exp $
 */

package jsky.graphics;

import java.awt.geom.Rectangle2D;
import java.util.EventListener;

/**
 * This defines the interface for listening for area selection events.
 *
 * @version $Revision: 1.1 $
 * @author Allan Brighton
 */
public abstract interface SelectedAreaListener extends EventListener {

    /**
     * Invoked when an area of the canvas has been dragged out.
     */
    public void setSelectedArea(Rectangle2D r);

}
