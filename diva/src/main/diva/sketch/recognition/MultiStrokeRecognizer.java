/*
 * $Id: MultiStrokeRecognizer.java,v 1.1 2001/08/28 06:34:11 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.recognition;

/**
 * A recognizer that processes a set of strokes and return the
 * predictions in a RecognitionSet.
 *
 * @author  Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 * @rating Red
 */
public interface MultiStrokeRecognizer extends StrokeRecognizer {
    /** Perform recognition on a set of strokes and return the
     *  recognition result in a ReconitionSet.
     */
    public RecognitionSet sessionCompleted (TimedStroke strokes[]);
}
