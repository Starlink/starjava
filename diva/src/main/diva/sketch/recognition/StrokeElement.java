/*
 * $Id: StrokeElement.java,v 1.2 2000/08/04 01:24:02 michaels Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.recognition;
import diva.sketch.recognition.TimedStroke;

/**
 * A scene entry that represents a single stroke with no type
 * information.  Its parents are composite elements that each contain
 * a typed interpretation of this stroke.
 *
 * @author 	Michael Shilman (michaels@eecs.berkeley.edu)
 * @version	$Revision: 1.2 $
 * @rating Red
 */
public interface StrokeElement extends SceneElement {
    /**
     * Return the timed stroke object that is contained by this
     * element.
     */
    public TimedStroke getStroke();
}
