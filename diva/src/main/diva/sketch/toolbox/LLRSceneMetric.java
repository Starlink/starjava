/*
 * $Id: LLRSceneMetric.java,v 1.3 2001/07/22 22:01:57 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import diva.sketch.recognition.*;
import java.util.List;
import java.util.Iterator;

/**
 * A scene metric that calculates the accuracy based on the
 * accuracy of the terminal assignments, where a terminal
 * is the direct parent of the stroke in the highest confidence
 * scene interpretation.
 *
 * <pre>
 * # of incorrect terminals / total # of terminals
 * </pre>
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 * @rating Red
 */
public class LLRSceneMetric implements SceneMetric {
    /** The name of this metric is "llr"
     */
    public String getName() {
        return "llr";
    }

    /**
     * Return a similarity metric based only on the LLR (terminal)
     * values of the test and reference databases:
     *
     * <pre>
     * # of incorrect terminals / total # of terminals
     * </pre>
     * 
     * Assumes that the two scenes are composed of the
     * same number of strokes in the same order.
     *
     * @throws IllegalArgumentException If the scene is empty,
     * if the two scenes have different numbers of strokes, or
     * if either scene does not have a root that covers all of
     * its strokes.
     */
    public double apply(Scene db1, Scene db2) {
        List strokes1 = db1.strokes();
        List strokes2 = db2.strokes();
        if(strokes1.size() != strokes2.size()) {
            String err = "Scenes have different number of strokes!";
            throw new IllegalArgumentException(err);
        }
        if(strokes1.size() == 0) {
            throw new IllegalArgumentException("Empty scene!");
        }

        SceneElement root1 = bestRoot(db1);
        SceneElement root2 = bestRoot(db2);
        CompositeElement[] terminals1 = new CompositeElement[strokes1.size()];
        CompositeElement[] terminals2 = new CompositeElement[strokes1.size()];
        gatherTerminals(root1, terminals1, strokes1);
        gatherTerminals(root2, terminals2, strokes2);        
               
        int match = 0;
        for(int i = 0; i < terminals1.length; i++) {
            TypedData data1 = terminals1[i].getData();
            TypedData data2 = terminals2[i].getData();
            if(data1.equals(data2)) {
                match++;
            }
        }
        return ((double)match)/terminals1.length;
    }

    /** Return the highest-confidence root that covers
     * all of the children.
     *
     * @throws IllegalArgumentException if the scene does
     * not have a root tha covers all the strokes.
     */
    private final SceneElement bestRoot(Scene db) {
        for(Iterator i = db.roots().iterator(); i.hasNext(); ) {
            SceneElement tmp = (SceneElement)i.next();
            if(db.isCoveringAll(tmp)) {
                return tmp;
            }
        }
        String err = "No root composite element"; 
        throw new IllegalArgumentException(err);
    }
        

    /**
     * Gather the terminal elements from the given root
     * (i.e. the elements that are the direct parents of
     * the strokes in the tree).  Place the gathered
     * terminals in the given "terminals" array.
     */
    private final void gatherTerminals(SceneElement root,
            CompositeElement[] terminals, List strokes) {
        if(root instanceof CompositeElement) {
            CompositeElement comp = (CompositeElement)root;
            if(comp.children().size() == 1) {
                SceneElement child = (SceneElement)comp.children().get(0);
                if(child instanceof StrokeElement) {
                    terminals[strokes.indexOf(child)] = comp;
                    return;
                }
            }
            for(Iterator i = comp.children().iterator(); i.hasNext(); ) {
                SceneElement child = (SceneElement)i.next();
                gatherTerminals(child, terminals, strokes);
            }
        }
        else if(root instanceof ChoiceElement) {
            //use the highest-confidence choice (FIXME??)
            ChoiceElement choice = (ChoiceElement)root;
            SceneElement child = (SceneElement)choice.choices().get(0);
            gatherTerminals(child, terminals, strokes);
        }
    }
}




