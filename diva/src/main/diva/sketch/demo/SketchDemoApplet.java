/*
 * $Id: SketchDemoApplet.java,v 1.1 2000/07/14 23:47:05 hwawen Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.demo;

import diva.gui.AppletContext;

/**
 * An applet version of the sketch demo.
 *
 * @see SketchDemo
 * @author Heloise Hse  (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 * @rating Red
 */
public class SketchDemoApplet extends AppletContext {
    public SketchDemoApplet() {
       new SketchDemo(this);
    }
}






