/*
 * $Id: StrokeRecognition.java,v 1.2 2000/05/29 21:10:38 michaels Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */

package diva.sketch.recognition;

/**
 * An interpretation of a stroke as a data/confidence
 * pair.  The confidence varies between 0 and 1 inclusive.
 * 
 * @see TimedStroke
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 * @rating Red
 */
public class StrokeRecognition {
    private double _confidence;
    private TypedData _data;
	
    /**
     * Construct a new recognition using the given data
     * and confidence.
     */
    public StrokeRecognition(TypedData data, double confidence) {
        _data = data;
        _confidence = confidence;
    }
	
    /**
     * Return the confidence of this recognition as
     * a value between 0 and 1, inclusive.
     */
    public double getConfidence() {
        return _confidence;
    }
	
    /**
     * Return the type of this recognition.
     */
    public Type getType() {
        return _data.getType();
    }	
	
    /**
     * Return the data of this recognition.
     */
    public TypedData getData() {
        return _data;
    }
	
    /**
     * Print the contents of this recognition.
     */
    public String toString() {
        return "Recognition[" + _data + ", " + _confidence + "]";
    }
}
