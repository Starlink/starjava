/*
 * $Id: BubbleGraphDemoApplet.java,v 1.1 2000/06/05 18:54:34 neuendor Exp $
 *
 * Copyright (c) 2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graph.basic;

import diva.gui.AppletContext;

/**
 * A bubble graph demo that can run in an applet.
 * @author Steve Neuendorffer  (neuendor@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 * @rating Red
 */
public class BubbleGraphDemoApplet extends AppletContext {
    public BubbleGraphDemoApplet() {
       new BubbleGraphDemo(this);
    }
}






