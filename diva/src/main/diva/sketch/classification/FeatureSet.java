/*
 * $Id: FeatureSet.java,v 1.4 2001/07/22 22:01:44 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.classification;

/**
 * A data structure for storing features for a classifier; it is
 * basically a typesafe array of doubles with appropriate accessor
 * methods.  It is up to the client to maintain consistency between
 * different instances of feature sets, i.e. that they have the
 * same number of elements and that the features at specific indices
 * are 
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 */
public class FeatureSet {
    private double _features[];

    public FeatureSet(int num) {
        _features = new double[num];
    }

    public FeatureSet(double features[]) {
        _features = features;
    }

    public final double getFeature(int i) {
        return _features[i];
    }

    public final int getFeatureCount() {
        return _features.length;
    }
    
    public double[] getFeatures() {
        return _features;
    }
    
    public final void setFeature(int i, double val) {
        _features[i] = val;
    }

    public String toString(){
        StringBuffer buf = new StringBuffer();
        double []vals = getFeatures();
        for(int i=0; i< vals.length; i++){
            buf.append(vals[i] + ", ");
        }
        return buf.toString(); 
    }
}


