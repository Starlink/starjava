/*
 * $Id: SketchDemoApplet.java,v 1.2 2001/07/22 22:01:46 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.demo;

import diva.gui.AppletContext;

/**
 * An applet version of the sketch demo.
 *
 * @see SketchDemo
 * @author Heloise Hse  (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 * @rating Red
 */
public class SketchDemoApplet extends AppletContext {
    public SketchDemoApplet() {
       new SketchDemo(this);
    }
}







