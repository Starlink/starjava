/*
 * $Id: GaussianWeightSet.java,v 1.4 2001/07/22 22:01:44 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.classification;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Given a set of training examples (each example is a feature
 * vector), a Gaussian classifier computes the mu and sigma for each
 * type of features.  The mu of a particular type of feature is
 * calculated by taking the mean of the features of that type from the
 * examples.  The sigma is the standard deviation.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 */
public class GaussianWeightSet extends WeightSet {
    /**
     * Constructs a gaussian weight set with the specified type.
     */
    public GaussianWeightSet(String type){
        super(type);
    }

    /**
     * Calculates the mu value for Gaussian distribution by taking the
     * mean of each feature type.
     */
    protected FeatureSet computeMu() throws ClassifierException {
        if(getExampleCount()<1){
            throw new ClassifierException("No examples to train.");
        }
        int numFeatures =  ((FeatureSet)(examples().next())).getFeatureCount();
        FeatureSet mus = new FeatureSet(numFeatures);
        for(int i = 0; i < numFeatures; i++) {
            double sum = 0;
            for(Iterator j = examples(); j.hasNext(); ){
                double val = ((FeatureSet)(j.next())).getFeature(i);
                if((Double.isNaN(val))&&(Double.isInfinite(val))) {
                    throw new ClassifierException(getType() + " " + i + ": " + val);
                }
                sum += val;
            }
            double avg = sum/(double)getExampleCount();
            debug(i + " avg:  " + sum + "/" + getExampleCount() + " = " + avg);
            mus.setFeature(i, avg);
        }
        return mus;
    }

    /**
     * Calculates the sigma of each feature in this classifier.  In
     * Gaussian distribution, sigma equals standard deviation.  This
     * method assumes that the mu values have already been calculated,
     * and will throw an exception if this is not the case.
     *
     * <pre>
     *   sigma[i] = (sqrt (sum (X[i]-u)^2)))/N
     * </pre>
     */
    protected FeatureSet computeSigma() throws ClassifierException {
        FeatureSet mus = getMuValues();
        if(mus == null){
            throw new ClassifierException("No mu values to compute sigma values");
        }

        int numFeatures = mus.getFeatureCount();
        FeatureSet sigmas = new FeatureSet(numFeatures);
        for(int i = 0; i < numFeatures; i++) {
            double meanValue = mus.getFeature(i);
            double sum = 0;
            for(Iterator j = examples(); j.hasNext(); ){
                FeatureSet f = (FeatureSet)j.next();
                double val = f.getFeature(i);
                if((Double.isNaN(val))&&(Double.isInfinite(val))) {
                    throw new ClassifierException(getType() + " " + i + ": " + val);
                }
                sum += ((val-meanValue)*(val-meanValue));
            }
            double sigma = Math.sqrt(sum/getExampleCount());
            sigmas.setFeature(i, sigma);
        }
        return sigmas;
    }
}


