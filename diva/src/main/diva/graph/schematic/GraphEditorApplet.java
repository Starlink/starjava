/*
 * $Id: GraphEditorApplet.java,v 1.3 2000/09/15 21:53:15 michaels Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graph.schematic;

import diva.gui.AppletContext;
import diva.gui.DesktopContext;

/**
 * A graph editor that runs as an applet.
 *
 * @author Steve Neuendorffer  (neuendor@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 * @rating Red
 */
public class GraphEditorApplet extends AppletContext {
    public GraphEditorApplet() {
        new GraphEditor(new DesktopContext(this));
    }
}






