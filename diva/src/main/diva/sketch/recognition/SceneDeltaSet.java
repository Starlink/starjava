/*
 * $Id: SceneDeltaSet.java,v 1.5 2001/07/22 22:01:54 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.recognition;

import java.util.ArrayList;
import java.util.Iterator;
import diva.util.NullIterator;

/**
 * A collection of deltas that represents the cumulative set of
 * possible changes recognized by a scene recognizer in response to a
 * given event.  If a recognizer does not recognize anything, it will
 * return the constant NO_DELTA.
 *
 * @author 	Michael Shilman (michaels@eecs.berkeley.edu)
 * @version	$Revision: 1.5 $
 * @rating Red
 */
public class SceneDeltaSet {
    /**
     * A constant representing no change to a scene.  Many recognizers
     * will return this as a result when they have failed to recognize
     * anything; a client should check for this constant explicitly
     * because it has a different behavior from a normal delta set,
     * i.e. it will throw exceptions when clients try to modify it.
     */
    public static final SceneDeltaSet NO_DELTA = new SceneDeltaSet() {
        /**
         * Throw an exception.  A constant shouldn't be
         * modified.
         */
        public void addDelta(SceneDelta d) {
            String err = "Unable to add deltas to NO_DELTA";
            throw new UnsupportedOperationException(err);
        }
        /**
         * Throw an exception.  A constant shouldn't be
         * modified.
         */
        public void removeDelta(SceneDelta d) {
            String err = "Unable to remove deltas from NO_DELTA";
            throw new UnsupportedOperationException(err);
        }
        /**
         * Return the number of deltas in this set: zero.
         */
        public int getDeltaCount() {
            return 0;
        }
        /**
         * Return an empty iterator.
         */
        public Iterator deltas() {
            return new NullIterator();
        }
        /**
         * Return the string "NO_DELTA"
         */
        public String toString() {
            return "NO_DELTA";
        }
    };

    /**
     * Store the deltas.
     */
    ArrayList _deltas;

    /**
     * Construct an empty scene delta set.
     */
    public SceneDeltaSet() {
        _deltas = new ArrayList(4);
    }
	
    /**
     * Add a scene delta to this set in sorted
     * order of confidence from highest to lowest.
     */
    public void addDelta(SceneDelta d) {
        //FIXME - what to do about strokes & their confidences????
        //insert in sorted order.
        int i;
        for(i = 0; i<_deltas.size(); i++) {
            SceneDelta d2 = (SceneDelta)_deltas.get(i);
            if(d2.getConfidence() < d.getConfidence()) {
                break;
            }
        }
        _deltas.add(i, d);
    }
	
    /**
     * Remove a scene delta from this set.
     */
    public void removeDelta(SceneDelta d) {
        _deltas.remove(d);
    }
	
    /**
     * Return the number of deltas
     * contained in this set.
     */
    public int getDeltaCount() {
        return _deltas.size();
    }
	
    /**
     * Return the deltas in sorted order from
     * highest confidence to lowest.
     */
    public Iterator deltas() {
        return _deltas.iterator();
    }
	
    /**
     * Add all of the deltas from the given
     * delta set to this delta set in sorted order
     * from highest to lowest confidence.
     */
    public void addAll(SceneDeltaSet s) {
        if(s != NO_DELTA) {
            _deltas.addAll(s._deltas);
        }
    }

    /**
     * Print the contents of this delta set
     * in sorted order.
     */
    public String toString() {
        StringBuffer out = new StringBuffer("SceneDeltaSet[");
        for(Iterator i = deltas(); i.hasNext(); ) {
            SceneDelta d = (SceneDelta)i.next();
            out.append("\t");
            out.append(d.toString());
            out.append("\n");
        }
        out.append("]");
        return out.toString();
    }
}

