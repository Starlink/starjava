/*
 * $Id: BayesClassifier.java,v 1.3 2001/08/06 18:48:21 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.classification;

import java.util.Iterator;

/**
 * A naive bayes classifier.  The training process calculates mu and
 * sigma for each feature (a random variable) of a class, and the
 * classification process uses the joint p.d.f. to compute the
 * probability of an example belonging to a particular class.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 */
public class BayesClassifier extends AbstractClassifier {
    /** The minimum sigma value; used to avoid divide-by-zero errors. */
    protected static final double MIN_SIGMA = .01;

    /**
     * Coefficents of the joint p.d.f., one for each class.
     * p(x)=(1/product(sigma_i)*(2*PI)^n/2)*exp(-0.5*sum((x_i-mu_i)/sigma_i)^2)
     * i = 1,2,...n
     * n = number of features
     */
    private double _coefficients[];

    /**
     * Compute mu's and sigma's for each class, also computes the
     * coefficients (one per class) that are used in the joint
     * p.d.f. calculation.
     */
    public void train(TrainingSet tset) throws ClassifierException {
        super.train(tset);
        int numTypes = _weights.size();
        _coefficients = new double[numTypes];
        int ct=0;
        for(Iterator weights = _weights.iterator(); weights.hasNext(); ) {
            WeightSet wset = (WeightSet)weights.next();
            double[] sigmas = wset.getSigmaValues().getFeatures();
            double k = 1;
            for(int i = 0; i < sigmas.length; i++) {
                k *= sigmas[i];
            }
            double denom = k*Math.pow(2*Math.PI,sigmas.length/2);
            _coefficients[ct] = 1/denom;
            ct++;
        }        
    }

    /**
     * Given a feature vector (fs), compute the joint probability of
     * each class.
     * p(x)=(1/product(sigma_i)*(2*PI)^n/2)*exp(-0.5*sum((x_i-mu_i)/sigma_i)^2)
     * i = 1,2,...n
     * n = number of features
     */
    public Classification classify(FeatureSet fs) throws ClassifierException {
        if((fs == null) || (fs.getFeatureCount() == 0)){
            return new Classification(null,null);
        }

        String[] types = new String[_weights.size()];
        double[] probs = new double[_weights.size()];
        int ct = 0;
        
        for(Iterator weights = _weights.iterator(); weights.hasNext(); ) {
            WeightSet wset = (WeightSet)weights.next();
            double[] mus = wset.getMuValues().getFeatures();
            double[] sigmas = wset.getSigmaValues().getFeatures();
            double[] inputs = fs.getFeatures();
            if(mus.length != inputs.length) {
                String err = "Wrong number of features: " +
                    mus.length + " != " + inputs.length;
                throw new ClassifierException(err);
            }

            double sum = 0;
            for(int i = 0; i < inputs.length; i++) {
                double diff = inputs[i] - mus[i];
                double sigma = sigmas[i];
                sigma = (sigma == 0) ? MIN_SIGMA : sigma;
                sum += (diff*diff)/(sigma*sigma);
            }
            int n = types.length;
            double k = _coefficients[ct];
            double ex = Math.exp(-0.5*sum);
            double val = k*ex;
            types[ct] = wset.getType();
            probs[ct] = val;
            ct++;
        }

        Classification results = new Classification(types, probs);
        return results;
    }
}

