/*
 * $Id: StrokeSceneRecognizer.java,v 1.12 2001/08/28 06:34:12 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */

package diva.sketch.recognition;
import java.util.Iterator;

/**
 * A scene recognizer that uses a given stroke recognizer
 * so that the results of its single-stroke recognition get added
 * to the scene properly.
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.12 $
 * @rating Red
 */
public class StrokeSceneRecognizer implements SceneRecognizer {
    /**
     * The string ID to access the child of a scene element.
     * When StrokeSceneRecognizer calls a StrokeRecognizer
     * to perform recognition of individual strokes, it uses
     * this name to point from the SceneElement to the StrokeElement
     * in the scene database.  To access the stroke use:
     * <pre>
     * SceneElement parent = ...
     * StrokeElement stroke = parent.getChild(
     *      StrokeSceneRecognizer.STROKE_NAME);
     * </pre>
     * assuming that "parent" is the parent of a stroke element.
     */
    public final static String STROKE_NAME = "stroke";
    
    /**
     * The recognizer that is being adapted.
     */
    private StrokeRecognizer _strokeRecognizer;
	
    /**
     * Construct a scene recognizer using the given
     * stroke recognizer to perform single-stroke
     * recognition.
     */
    public StrokeSceneRecognizer(StrokeRecognizer r) {
        _strokeRecognizer = r;
    }

    /** Return the stroke recognizer that this wraps.
     */
    public StrokeRecognizer getStrokeRecognzer() {
        return _strokeRecognizer;
    }
	
    /**
     * Call the child recognizer and add the results, if any,
     * to the given scene database.
     */
    public SceneDeltaSet strokeCompleted (StrokeElement se, Scene db) {
        return genDeltas(_strokeRecognizer.strokeCompleted(se.getStroke()),
                se, db);
    }

    /**
     * Call the child recognizer and add the results, if any,
     * to the given scene database.
     */
    public SceneDeltaSet strokeModified (StrokeElement se, Scene db) {
        return genDeltas(_strokeRecognizer.strokeModified(se.getStroke()),
                se, db);
    }

    /**
     * Call the child recognizer and add the results, if any,
     * to the given scene database.
     */
    public SceneDeltaSet strokeStarted (StrokeElement se, Scene db) {
        return genDeltas(_strokeRecognizer.strokeStarted(se.getStroke()),
                se, db);
    }
	
    /**
     * Call the child recognizer and add the results, if any, to the
     * given scene database.  If the session contains more than one
     * stroke, call the child recognizer for each stroke in the
     * session and append the deltas.  (FIXME: does this make
     * sense?)
     */
    public SceneDeltaSet sessionCompleted (StrokeElement[] session, Scene db) {
        RecognitionSet s = null;
        if(session.length == 1) {
            StrokeElement se = session[0];
            return genDeltas(_strokeRecognizer.strokeCompleted(se.getStroke()),
                    se, db);
        }
        else {
            SceneDeltaSet out = SceneDeltaSet.NO_DELTA;
            for(int i = 0; i < session.length; i++) {
                StrokeElement se = session[i];
                SceneDeltaSet sub =
                    genDeltas(_strokeRecognizer.strokeCompleted(se.getStroke()), se, db);
                if(sub != SceneDeltaSet.NO_DELTA) {
                    if(out == SceneDeltaSet.NO_DELTA) {
                        out = sub;
                    }
                    else {
                        for(Iterator ds = sub.deltas(); ds.hasNext(); ) {
                            SceneDelta delta = (SceneDelta)ds.next();
                            out.addDelta(delta);
                        }
                    }
                }
            }
            return out;
        }
    }
    
    /**
     * Go through each recognition, check to see if it's already
     * in the tree, and add it if it's not.
     *
     * <p>
     * TODO: if it is already in the tree, what do we do?
     * update the confidence?  take the highest confidence?
     * vote using another voting mechanism of some kind?
     */
    private SceneDeltaSet genDeltas (RecognitionSet srs,
            StrokeElement se, Scene db) {
        SceneDeltaSet out = SceneDeltaSet.NO_DELTA;
        int ct=0;
        for(Iterator i = srs.recognitions(); i.hasNext(); ) {
            Recognition sr = (Recognition)i.next();
            //System.out.println("\trecognition #" + ct++ +" "+sr.getType());
            boolean found = false;
            for(Iterator j = se.parents().iterator(); j.hasNext(); ) {
                CompositeElement parent = (CompositeElement)j.next();
                //System.out.println("\t\t parent (" + parent.getData().getType() + ")");
                if(sr.getType().equals(parent.getData().getType())) {
                    found = true;
                    //TODO: what do we do here?  update the confidence??
                    System.err.println(" Warning: duplicate type in vote: " + sr.getType());
                    break;
                }
            }
            if(!found) {
                SceneElement[] children = new SceneElement[1];
                children[0] = se;
                String[] names = { STROKE_NAME };
                CompositeElement newElt = db.addComposite(sr.getData(),
                        sr.getConfidence(), children, names);
               // db.setRoot(newElt);
                if(out == SceneDeltaSet.NO_DELTA) {
                    out = new SceneDeltaSet();
                    //                    out.addDelta(new SceneDelta.Subtractive(db, newElt));
                }
                out.addDelta(new SceneDelta.Subtractive(db, newElt));    
            }
        }
        //System.out.println("Num deltas = " + out.getDeltaCount());
        return out;
    }
}

