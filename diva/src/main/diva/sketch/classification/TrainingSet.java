/*
 * $Id: TrainingSet.java,v 1.7 2001/10/29 22:26:38 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.classification;
import diva.util.NullIterator;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A TrainingSet contains a set of types, and for each type a
 * corresponding set of positive and negative examples.  It used to
 * train a TrainableClassifier.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.7 $
 */
public final class TrainingSet {
    /**
     * Internal constant for the array slot of negative examples.
     */
    private static final int NEGATIVE = 0;

    /**
     * Internal constant for the array slot of positive examples.
     */
    private static final int POSITIVE = 1;

    /**
     * Store a mapping from types to their positive/negative examples.
     */
    private HashMap _map;

    /**
     * Construct an empty training set.
     */
    public TrainingSet() {
        _map = new HashMap();
    }

    /**
     * Add a negative example to this training set for the given type.
     */
    public final void addNegativeExample(String t, FeatureSet s) {
        addExample(t, s, NEGATIVE);
    }

    /**
     * Add a positive example to this training set for the given type.
     */
    public final void addPositiveExample(String t, FeatureSet s) {
        addExample(t, s, POSITIVE);
    }

    
    /**
     * Add a example to this training set for the given type
     * (either positive or negative, denoted by the "which" argument).
     */
    public final void addExample(String t, FeatureSet s, int which) {
        ArrayList[] l = (ArrayList[])_map.get(t);
        if(l == null) {
            l = new ArrayList[2];
            l[0] = new ArrayList();
            l[1] = new ArrayList();
            _map.put(t, l);
        }
        l[which].add(s);
    }
    
    /**
     * Return true if the training type with the specified name is
     * in the set, or false otherwise.
     */
    public final boolean containsType(String t){
        return _map.get(t) != null;
    }

    /**
     * An internal method to get the example count for a particular type
     * (either positive or negative, denoted by the "which" argument).
     */
    private final int getExampleCount(String t, int which) {
        ArrayList[] l = (ArrayList[])_map.get(t);
        if(l == null) {
            return 0;
        }
        else {
            return l[which].size();
        }
    }

    /**
     * An internal method to get the examples for a particular type
     * (either positive or negative, denoted by the "which" argument).
     */
    private final Iterator getExamples(String t, int which) {
        ArrayList[] l = (ArrayList[])_map.get(t);
        if(l == null) {
            return new NullIterator();
        }
        return l[which].iterator();
    }

    /**
     * Return how many types are contained in this training set.
     */
    public final int getTypeCount() {
        return _map.size();
    }
    
    /**
     * Returns the number of negative examples for the given type.
     */
    public final int negativeExampleCount(String t) {
        return getExampleCount(t, NEGATIVE);
    }

    /**
     * An iterator over the negative examples for the given type.
     */
    public final Iterator negativeExamples(String t) {
        return getExamples(t, NEGATIVE);
    }

    /**
     * Returns the number of positive examples for the given type.
     */
    public final int positiveExampleCount(String t) {
        return getExampleCount(t, POSITIVE);
    }
    
    /**
     * An iterator over the positive examples for the given type.
     */
    public final Iterator positiveExamples(String t) {
        return getExamples(t, POSITIVE);
    }

    /**
     * An iterator over the types contained in this training set.
     */
    public Iterator types() {
        return _map.keySet().iterator();
    }

    public String toString(){
        StringBuffer buf = new StringBuffer();
        for(Iterator iter = types(); iter.hasNext();){
            String type = (String)iter.next();
            int num = positiveExampleCount(type);
            buf.append(num + "\t" + type + "s\n");
        }
        return buf.toString();
    }
}


