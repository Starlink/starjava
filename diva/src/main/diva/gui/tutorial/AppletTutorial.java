/*
 * $Id: AppletTutorial.java,v 1.2 2001/07/22 22:01:33 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.gui.tutorial;

import diva.gui.AppletContext;

/**
 * A graph editor that runs as an applet.
 *
 * @author Steve Neuendorffer  (neuendor@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 * @rating Red
 */
public class AppletTutorial extends AppletContext {
    public AppletTutorial() {
       new ApplicationTutorial(this);
    }
}







