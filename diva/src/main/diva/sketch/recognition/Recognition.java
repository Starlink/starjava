/*
 * $Id: Recognition.java,v 1.1 2001/08/28 06:34:11 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */

package diva.sketch.recognition;

/**
 * An interpretation of a stroke or a set of strokes as a
 * data/confidence pair.  The confidence varies between 0 and 1
 * inclusive.
 * 
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 * @rating Red
 */
public class Recognition {
    private double _confidence;
    private TypedData _data;
	
    /**
     * Construct a new recognition using the given data
     * and confidence.
     */
    public Recognition(TypedData data, double confidence) {
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
        return "Recognition[" + getType().getID() + ", " + _confidence + "]";
    }
}

