/*
 * $Id: MultiStrokeSceneRecognizer.java,v 1.1 2001/08/28 06:34:11 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.recognition;

/**
 * A scene recognizer that uses a given multi-stroke recognizer
 * so that the results of its multi-stroke recognition get added
 * to the scene properly.
 *
 * @author  Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 * @rating Red
 */
public class MultiStrokeSceneRecognizer implements SceneRecognizer {

    /**
     * The recognizer that is being adapted.
     */
    private MultiStrokeRecognizer _multiStrokeRecognizer;
    
    /**
     * Construct a scene recognizer using the given
     * stroke recognizer to perform multi-stroke
     * recognition.
     */
    public MultiStrokeSceneRecognizer(MultiStrokeRecognizer r) {
        _multiStrokeRecognizer = r;
    }

    public MultiStrokeRecognizer getMultiStrokeRecognizer(){
        return _multiStrokeRecognizer;
    }

    /**
     * Return SceneDeltaSet.NO_DELTA 
     */
    public SceneDeltaSet strokeStarted(StrokeElement se, Scene db) {
        return SceneDeltaSet.NO_DELTA;
    }

    /**
     * Return SceneDeltaSet.NO_DELTA
     */
    public SceneDeltaSet strokeModified(StrokeElement stroke, Scene db) {
        return SceneDeltaSet.NO_DELTA;
    }
    
    /**
     * Return SceneDeltaSet.NO_DELTA
     */
    public SceneDeltaSet strokeCompleted(StrokeElement stroke, Scene db) {
        return SceneDeltaSet.NO_DELTA;
    }
    
    /**
     * Call the child recognizer and add the results, if any,
     * to the given scene database.
     * FIXME: Return NO_DELTA for now.
     */
    public SceneDeltaSet sessionCompleted(StrokeElement[] session, Scene db) {
        return SceneDeltaSet.NO_DELTA;
    }
}
