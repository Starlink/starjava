/*
 * $Id: GraphEditorApplet.java,v 1.4 2001/07/22 22:01:25 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graph.schematic;

import diva.gui.AppletContext;
import diva.gui.DesktopContext;

/**
 * A graph editor that runs as an applet.
 *
 * @author Steve Neuendorffer  (neuendor@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 * @rating Red
 */
public class GraphEditorApplet extends AppletContext {
    public GraphEditorApplet() {
        new GraphEditor(new DesktopContext(this));
    }
}







