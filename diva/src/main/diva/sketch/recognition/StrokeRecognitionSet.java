/*
 * $Id: StrokeRecognitionSet.java,v 1.3 2000/10/15 08:05:16 michaels Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */

package diva.sketch.recognition;
import java.util.ArrayList;
import java.util.Iterator;
import diva.util.NullIterator;

/**
 * The result of a stroke recognizer's computations: a set of
 * mutually exclusive interpretations of a stroke, expressed
 * by StrokeRecognition objects as typed data with confidences.
 *
 * @see StrokeRecognition
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 * @rating Red
 */
public class StrokeRecognitionSet {
    /**
     * A constant that represents no recognition results.
     */
    public static final StrokeRecognitionSet NO_RECOGNITION =
    new StrokeRecognitionSet() {
        public void addRecognition(StrokeRecognition r) {
            String err = "Attempt to modify NO_RECOGNITION constant";
            throw new UnsupportedOperationException(err);
        }
        public void removeRecognition(StrokeRecognition r) {
            String err = "Attempt to modify NO_RECOGNITION constant";
            throw new UnsupportedOperationException(err);
        }
        public int getRecognitionCount() {
            return 0;
        }
        public Iterator recognitions() {
            return new NullIterator();
        }
        public StrokeRecognition getBestRecognition() {
            return null;
        }
        public StrokeRecognition getRecognitionOfType(Type type) {
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
    public StrokeRecognitionSet() {
        _set = new ArrayList();
    }
	
    /**
     * Construct a recognition set that contains the
     * given recognitions.
     */
    public StrokeRecognitionSet(StrokeRecognition[] rs) {
        _set = new ArrayList(rs.length);
        for(int i = 0; i < rs.length; i++) {
            addRecognition(rs[i]);
        }
    }


    /**
     * Add a recognition to the set by inserting it in descending
     * order of confidence value.
     */
    public void addRecognition(StrokeRecognition r) {
        boolean inserted = false;
        for(int i = 0; i < _set.size(); i++){
            StrokeRecognition r1 = (StrokeRecognition)_set.get(i);
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
    public StrokeRecognition getBestRecognition() {
        return (getRecognitionCount()>0) ? ((StrokeRecognition)_set.get(0)) :
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
    public StrokeRecognition getRecognitionOfType(Type type) {
        for(Iterator i = recognitions(); i.hasNext(); ) {
            StrokeRecognition r = (StrokeRecognition)i.next();
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
    public void removeRecognition(StrokeRecognition r) {
        _set.remove(r);
    }

    /**
     * Return the text representation of the recognition set.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("StrokeStrokeRecognitionSet[\n");
        for(Iterator iter = _set.iterator(); iter.hasNext(); ) {
            StrokeRecognition r = (StrokeRecognition)iter.next();
            buf.append(r.toString()).append("\n");
        }
        buf.append("]");
        return buf.toString();
    }
}
