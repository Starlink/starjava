/*
 * $Id: BubbleGraphDemoApplet.java,v 1.2 2001/07/22 22:01:20 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graph.basic;

import diva.gui.AppletContext;

/**
 * A bubble graph demo that can run in an applet.
 * @author Steve Neuendorffer  (neuendor@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 * @rating Red
 */
public class BubbleGraphDemoApplet extends AppletContext {
    public BubbleGraphDemoApplet() {
       new BubbleGraphDemo(this);
    }
}







