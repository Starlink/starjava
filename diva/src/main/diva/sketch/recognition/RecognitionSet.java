/*
 * $Id: RecognitionSet.java,v 1.1 2001/08/28 06:34:11 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */

package diva.sketch.recognition;
import java.util.ArrayList;
import java.util.Iterator;
import diva.util.NullIterator;

/**
 * The result of a recognizer's computations: a set of mutually
 * exclusive interpretations of a stroke or a set of strokes,
 * expressed by Recognition objects as typed data with confidences.
 *
 * @see Recognition
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 * @rating Red
 */
public class RecognitionSet {
    /**
     * A constant that represents no recognition results.
     */
    public static final RecognitionSet NO_RECOGNITION =
    new RecognitionSet() {
        public void addRecognition(Recognition r) {
            String err = "Attempt to modify NO_RECOGNITION constant";
            throw new UnsupportedOperationException(err);
        }
        public void removeRecognition(Recognition r) {
            String err = "Attempt to modify NO_RECOGNITION constant";
            throw new UnsupportedOperationException(err);
        }
        public int getRecognitionCount() {
            return 0;
        }
        public Iterator recognitions() {
            return new NullIterator();
        }
        public Recognition getBestRecognition() {
            return null;
        }
        public Recognition getRecognitionOfType(Type type) {
            return null;
        }
        public String toString() {
            return "NO_RECOGNITION";
        }
    };

    /**
     * The list of recognitions.
     */
    private ArrayList _set;

    /**
     * Construct an empty recognition set.
     */
    public RecognitionSet() {
        _set = new ArrayList();
    }
	
    /**
     * Construct a recognition set that contains the
     * given recognitions.
     */
    public RecognitionSet(Recognition[] rs) {
        _set = new ArrayList(rs.length);
        for(int i = 0; i < rs.length; i++) {
            addRecognition(rs[i]);
        }
    }


    /**
     * Add a recognition to the set by inserting it in descending
     * order of confidence value.
     */
    public void addRecognition(Recognition r) {
        boolean inserted = false;
        for(int i = 0; i < _set.size(); i++){
            Recognition r1 = (Recognition)_set.get(i);
            if(r.getConfidence() > r1.getConfidence()){
                _set.add(i, r);
                inserted = true;
                break;
            }
        }
        if(!inserted){
            _set.add(r);
        }
    }

    /**
     * Return the recognition object that has the highest confidence
     * value, or null if there are no recognitions in this set.
     */
    public Recognition getBestRecognition() {
        return (getRecognitionCount()>0) ? ((Recognition)_set.get(0)) :
            null;
    }

    /**
     * Return the number of recognitions in this set.
     */
    public int getRecognitionCount() {
        return _set.size();
    }

    /**
     * Return the recognition contained by this set with the given
     * type, or null if it's not contained.
     */
    public Recognition getRecognitionOfType(Type type) {
        for(Iterator i = recognitions(); i.hasNext(); ) {
            Recognition r = (Recognition)i.next();
            if(type.equals(r.getData().getType())) {
                return r;
            }
        }
        return null;
    }
    
    /**
     * Return an iterator over the recognized types in sorted order
     * from highest confidence to lowest.
     */
    public Iterator recognitions() {
        return _set.iterator();
    }
    
    /**
     * Remove the given recognition from the set.
     */
    public void removeRecognition(Recognition r) {
        _set.remove(r);
    }

    /**
     * Return the text representation of the recognition set.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("RecognitionSet[\n");
        for(Iterator iter = _set.iterator(); iter.hasNext(); ) {
            Recognition r = (Recognition)iter.next();
            buf.append(r.toString()).append("\n");
        }
        buf.append("]");
        return buf.toString();
    }
}

