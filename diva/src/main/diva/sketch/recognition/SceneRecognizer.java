/*
 * $Id: SceneRecognizer.java,v 1.3 2001/07/22 22:01:54 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.recognition;

/**
 * An incremental recognizer of scene structures.
 *
 * @author 	Michael Shilman (michaels@eecs.berkeley.edu)
 * @version	$Revision: 1.3 $
 * @rating Red
 */
public interface SceneRecognizer {
    public SceneDeltaSet strokeStarted(StrokeElement stroke, Scene db);
    public SceneDeltaSet strokeModified(StrokeElement stroke, Scene db);
    public SceneDeltaSet strokeCompleted(StrokeElement stroke, Scene db);
    public SceneDeltaSet sessionCompleted(StrokeElement[] session, Scene db);
}

