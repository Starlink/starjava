/*
 * $Id: AppletTutorial.java,v 1.1 2000/08/16 20:24:34 neuendor Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.gui.tutorial;

import diva.gui.AppletContext;

/**
 * A graph editor that runs as an applet.
 *
 * @author Steve Neuendorffer  (neuendor@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 * @rating Red
 */
public class AppletTutorial extends AppletContext {
    public AppletTutorial() {
       new ApplicationTutorial(this);
    }
}






