/*
 * $Id: Classification.java,v 1.6 2001/10/29 22:26:38 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.classification;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Data structure for storing a list of classifer type and confidence
 * value pairs in the order of descending confidence values. <p>
 *
 * During classification, an example is compared to each classifier
 * which in turn generates a value indicating how confident it thinks
 * that the example belongs in the class.  This value is called the
 * confidence value.  A classification contains a list of classifiers
 * along with their corresponding confidence values for the example.
 * <p>
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.6 $
 */
public class Classification {
    private String _types[];
    private double _confidences[];
    private String _highestConfidenceType;
    private double _highestConfidence;
    
    /**
     * Construct a classification with no types.
     */
    public Classification (String[] types, double[]confidences) {
        _types = types;
        _confidences = confidences;

        int maxIndex = 0;
        double maxConfidence = _confidences[maxIndex];
        for(int i=0; i<_types.length; i++){
            if(_confidences[i]>_confidences[maxIndex]){
                maxIndex = i;
                maxConfidence = _confidences[maxIndex];
            }
        }
        _highestConfidenceType = _types[maxIndex];
        _highestConfidence = maxConfidence;
    }

    public double getHighestConfidence(){
        return _highestConfidence;
    }

    public String getHighestConfidenceType(){
        return _highestConfidenceType;
    }
    
    /**
     * Return the number of types in this classification.
     */
    public int getTypeCount () {
        return (_types == null) ? 0 :_types.length;
    }

    /**
     * Return the i'th type.
     */
    public String getType (int i) {
        return _types[i];
    }

    /**
     * Return the i'th confidence.
     */
    public double getConfidence (int i) {
        return _confidences[i];
    }

    /**
     * Return a string representation of this classification
     * consisting of type and confidence pairs.
     */
    public String toString(){
        String s = getTypeCount() + " types: (";
        for(int i=0; i<getTypeCount(); i++){
            s = s + "[" + getType(i) + ", " + getConfidence(i) + "] ";
        }
        s = s + ")";
        return s;
    }
}


