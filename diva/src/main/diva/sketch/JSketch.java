/*
 * $Id: JSketch.java,v 1.6 2001/07/22 22:01:41 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */

package diva.sketch;
import diva.canvas.JCanvas;

/**
 * JSketch is a canvas which supports sketch input.  It contains a
 * sketch pane which can be embedded with an application-specific
 * controller to interpret sketch.  By default there is no sketch
 * recognition support, but by substituting an "intelligent" sketch
 * controller into the pane, application-specific recognition can be
 * added.
 *
 * @see SketchPane
 * @author 	Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version	$Revision: 1.6 $
 */
public class JSketch extends JCanvas {
    /**
     * Create a JSketch object with a blank drawing area.
     */
    public JSketch() {
        this(new SketchPane());
    }

    /**
     * Create a JSketch object with the given drawing pane.
     */
    public JSketch(SketchPane p) {
        setSketchPane(p);
    }
    
    /**
     * Return the sketch pane which displays and handles sketched
     * input.
     */
    public SketchPane getSketchPane () {
        return (SketchPane)getCanvasPane();
    }

    /**
     * Set the sketch pane which is used to display and handle sketched
     * input.
     */
    public void setSketchPane(SketchPane p) {
        setCanvasPane(p);
    }
}


