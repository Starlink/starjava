/*
 * $Id: LLRSceneRecognizer.java,v 1.2 2001/07/22 22:01:58 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */

package diva.sketch.toolbox;
import java.util.List;
import java.util.Set;
import java.util.Iterator;
import diva.sketch.recognition.*;

/**
 * A scene recognizer that groups the highest-confidence LLR results
 * into a single tree with 100% confidence.
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 * @rating Red
 */
public class LLRSceneRecognizer implements SceneRecognizer {
    /** The stroke recognzier
     */
    private StrokeSceneRecognizer _llr;

    /** The typed data that is associated
     */
    private static final TypedData _resultData = new SimpleData("llr_rec");
    
    /**
     * Construct a scene recognizer using the given
     * scene recognizer to perform single-stroke
     * recognition.
     */
    public LLRSceneRecognizer(StrokeSceneRecognizer llr) {
        _llr = llr;
    }
	
    /**
     * Call the child recognizer and add the results, if any,
     * to the given scene database.
     */
    public SceneDeltaSet strokeCompleted (StrokeElement se, Scene db) {
        return genDelta(_llr.strokeCompleted(se, db),
                se, db);
    }

    /**
     * Call the child recognizer and add the results, if any,
     * to the given scene database.
     */
    public SceneDeltaSet strokeModified (StrokeElement se, Scene db) {
        return genDelta(_llr.strokeModified(se, db),
                se, db);
    }

    /**
     * Call the child recognizer and add the results, if any,
     * to the given scene database.
     */
    public SceneDeltaSet strokeStarted (StrokeElement se, Scene db) {
        return genDelta(_llr.strokeStarted(se, db),
                se, db);
    }
	
    /**
     * Call the child recognizer and add the results, if any,
     * to the given scene database.  If the session contains
     * more than one stroke, call the child recognizer for
     * each stroke in the session.  (FIXME: does this make
     * sense?)
     */
    public SceneDeltaSet sessionCompleted (StrokeElement[] session, Scene db) {
        SceneDeltaSet out = new SceneDeltaSet();
        for(int i = 0; i < session.length; i++) {
            out.addAll(genDelta(_llr.strokeCompleted(session[i], db),
                    session[i], db));
        }
        return (out.getDeltaCount() == 0) ? SceneDeltaSet.NO_DELTA : out;
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
    private SceneDeltaSet genDelta (SceneDeltaSet set,
            StrokeElement se, Scene db) {
        Set parents = se.parents();
        if(parents.size() == 0) {
            return set;
        }
        Iterator i = parents.iterator();
        CompositeElement bestResult = (CompositeElement)i.next();
        while(i.hasNext()) {
            CompositeElement elt = (CompositeElement)i.next();
            if(elt.getConfidence() > bestResult.getConfidence()) {
                bestResult = elt;
            }
        }
        List llrRecTypes = db.elementsOfType(_resultData.getType(), null);
        SceneElement[] children;
        if(llrRecTypes.size() == 0) {
            children = new SceneElement[1];
            children[0] = bestResult;
        }
        else {
            CompositeElement root = (CompositeElement)llrRecTypes.get(0);
            List l = root.children();
            children = new SceneElement[l.size()+1];
            l.toArray(children);
            children[l.size()] = bestResult;
            db.removeElement(root);
        }
        String[] childNames = new String[children.length];
        for(int j = 0; j < childNames.length; j++) {
            childNames[j] = "child";
        }
        CompositeElement nroot = db.addComposite(_resultData, 100,
                children, childNames);
        SceneDelta delta = new SceneDelta.Subtractive(db,nroot,children);
        if(set == SceneDeltaSet.NO_DELTA) {
            set = new SceneDeltaSet();
        }
        set.addDelta(delta);
        return set;
    }
}

