/*
 * $Id: WeightedEuclideanClassifier.java,v 1.4 2000/05/02 00:44:48 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.classification;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * WeightedEuclideanClassifier is a trainable classifier that uses a
 * weighted N-dimensional distance between feature sets to classify
 * its input.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 */
public class WeightedEuclideanClassifier implements TrainableClassifier {
    /**
     * The minimum sigma value; used to avoid divide-by-zero errors.
     */
    private static final double MIN_SIGMA = .01;

    /**
     * A normalization constant: 10 divided by 30, every 30 step
     * in distance results in a 10% recognition drop.
     */
    private static final double NORMALIZATION = 0.3;

    /**
     * The weights that are used to perform the classification.
     */
    private ArrayList _weights;
    
    /**
     * Construct a classifier with no weight set.  The weight
     * set is created by the train method.
     */
    public WeightedEuclideanClassifier() {
        _weights = new ArrayList();
    }

    /**
     * Classify the specified feature set using each weight, by
     * comparing them to the mu (mean) value of the weight and
     * weighting it by the sigma value (standard deviation).  For each
     * feature <i>f</i>, <p>
     *
     * <pre>
     *    value = sum((input[f] - mu[f])^2/sigma[f]^2)
     * </pre>
     *
     * Finally, normalize the value into a confidence between 0 and 100.
     */
    public final Classification classify(FeatureSet fs) throws ClassifierException {
        if((fs == null) || (fs.getFeatureCount() == 0)){
            return new Classification(null,null);
        }

        String[] types = new String[_weights.size()];
        double[] confidences = new double[_weights.size()];
        int cnt = 0;
        
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
            double confidence = 100 - sum*NORMALIZATION;
            types[cnt] = wset.getType();
            confidences[cnt] = confidence;
            cnt++;
        }

        Classification results = new Classification(types, confidences);
        debug(results.toString());
        return results;
    }

    /**
     * Reset the weight sets.
     */
    public void clear() {
        _weights.clear();
    }

    /**
     * Debugging output.
     */
    private final void debug(String s) {
        //System.err.println(s);
    }

    /**
     * Return false; WeightedEuclideanClassifiers are not incremental.
     */
    public final boolean isIncremental() {
        return false;
    }
    
    /**
     * Train on the given data set by building the set of weights that
     * are used in the classify() method.  There is one weight per
     * type, so for each positive example in the training class,
     * create a weight set and train it.
     */
    public final void train(TrainingSet tset) throws ClassifierException {
        for(Iterator i = tset.types(); i.hasNext(); ){
            String type = (String)i.next();
            WeightSet ws = new GaussianWeightSet(type);
            for(Iterator j = tset.positiveExamples(type); j.hasNext(); ) {
                ws.addExample((FeatureSet)j.next());
            }
            ws.train();
            _weights.add(ws);
        }
    }
}

