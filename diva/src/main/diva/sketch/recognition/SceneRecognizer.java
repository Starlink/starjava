/*
 * $Id: SceneRecognizer.java,v 1.2 2000/05/29 21:10:38 michaels Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.recognition;

/**
 * An incremental recognizer of scene structures.
 *
 * @author 	Michael Shilman (michaels@eecs.berkeley.edu)
 * @version	$Revision: 1.2 $
 * @rating Red
 */
public interface SceneRecognizer {
    public SceneDeltaSet strokeStarted(StrokeElement stroke, Scene db);
    public SceneDeltaSet strokeModified(StrokeElement stroke, Scene db);
    public SceneDeltaSet strokeCompleted(StrokeElement stroke, Scene db);
    public SceneDeltaSet sessionCompleted(StrokeElement[] session, Scene db);
}
