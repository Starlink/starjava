/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ImageGraphicsHandler.java,v 1.2 2002/07/09 13:30:37 brighton Exp $
 */

package jsky.image.gui;

import java.awt.Graphics2D;
import java.util.EventListener;


/**
 *  A callback interface for classes that need to draw graphics over an image.
 *
 * @version $Revision: 1.2 $
 * @author Allan Brighton
 */
public abstract interface ImageGraphicsHandler extends EventListener {

    /** Called each time the image is repainted */
    public void drawImageGraphics(BasicImageDisplay imageDisplay, Graphics2D g);
}


