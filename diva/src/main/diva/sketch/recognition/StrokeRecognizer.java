/*
 * $Id: StrokeRecognizer.java,v 1.1 2000/05/10 19:11:19 hwawen Exp $
 *
 * Copyright (c) 1998 The Regents of the University of California.
 * All rights reserved.  See the file COPYRIGHT for details.
 */

package diva.sketch.recognition;

/**
 * A recognizer responds to changes in strokes and returns recognition
 * sets (interpretations of the stroke or of some or all of the scene)
 * based on these changes.
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 * @rating Red
 */
public interface StrokeRecognizer {
    /**
     * Invoked when a stroke starts.  This occurs when the mouse down
     * event has been detected.
     */
    public StrokeRecognitionSet strokeStarted(TimedStroke s);

    /**
     * Invoked when a stroke has been modified, for example, points
     * have been added to the stroke.  It is probably safe to assume
     * that this will be called every time a point is added to a
     * stroke.
     */
    public StrokeRecognitionSet strokeModified(TimedStroke s);
	
    /**
     * Invoked when a stroke is completed. This occurs when the mouse
     * up event has been detected.
     */
    public StrokeRecognitionSet strokeCompleted(TimedStroke s);
}
