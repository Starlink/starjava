/*
 * $Id: BasicGraphDemoApplet.java,v 1.2 2000/07/02 03:23:45 michaels Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graph.basic;

import diva.gui.AppletContext;

/**
 * An applet version of the graph demo.
 *
 * @see BasicGraphDemo
 * @author Steve Neuendorffer  (neuendor@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 * @rating Red
 */
public class BasicGraphDemoApplet extends AppletContext {
    public BasicGraphDemoApplet() {
       new BasicGraphDemo(this);
    }
}






