/*
 * $Id: RubineClassifier.java,v 1.5 2001/07/22 22:01:44 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.classification;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

/**
 * This classifier implements the classic linear discriminator.
 * More information can be obtained from Rubine's paper.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.5 $
 */
public class RubineClassifier implements TrainableClassifier {

    /**
     * Types of training classes (e.g. square, circle, triangle...)
     */
    private String[] _types = null;
    
    /**
     * The weight set for each training class.  There is a weight
     * associated with each feature in the class. _weights[] is an
     * array of FeatureSets.
     */
    private FeatureSet[] _weights = null;

    /**
     * Average feature vectors, one for each training class.
     */
    private FeatureSet[] _means = null;

    /**
     * The maximum distance of a training example from the mean of its
     * class.
     */
    private double[] _thresholds = null;

    /**
     * The normalizing constant for each training class.  This value
     * is added to the sum of weighted features.
     */
    private double[] _constants = null;

    /**
     * Inverted common covariance matrix stored in a 2D array.
     * [row][col]  This matrix is produced in train().
     */
    private double[][] _commonCovarianceInverse = null;

    public static void main(String[] argv) {
        TrainingSet tset = new TrainingSet();
        double[] vals1 = {1.0, 2.0, 3.0, 4.0, 5.0};
        FeatureSet f = new FeatureSet(vals1);
        tset.addPositiveExample("A", f);
        double[] vals2 = {4.0, 20.0, 33.0, 64.0, 75.0};
        f = new FeatureSet(vals2);
        tset.addPositiveExample("A", f);
        double[] vals3 = {56.0, 10.0, 42.0, 88.0, 5.0};
        f = new FeatureSet(vals3);        
        tset.addPositiveExample("A", f);

        double[] vals4 = {98.0, 100.0, 42.0, 88.0, 5.0};
        f = new FeatureSet(vals4);        
        tset.addPositiveExample("B", f);
        double[] vals5 = {2.0, 18.0, 33.0, 8.0, 23.0};
        f = new FeatureSet(vals5);        
        tset.addPositiveExample("B", f);
        double[] vals6 = {56.0, 10.0, 42.0, 68.0, 5.0};
        f = new FeatureSet(vals6);        
        tset.addPositiveExample("B", f);

        RubineClassifier tc = new RubineClassifier();
        try{
            tc.train(tset);

            double[] v1 = {2.0, 20.0, 30.0, 10.0, 100};
            FeatureSet g1 = new FeatureSet(v1);
            tc.classify(g1);

            double[] v2 = {95.0, 95.0, 30.0, 88.0, 55.0};
            FeatureSet g2 = new FeatureSet(v2);
            tc.classify(g2);
        }
        catch(Exception e){
            e.printStackTrace();
        }

    }

    public RubineClassifier(){}
        
    /**
     * Return a classification for the specified feature set, or throw
     * a runtime exception if the feature set that is given does not
     * have the same number of features as the training set that it
     * was trained on.
     */
    public Classification classify2(FeatureSet fs) throws ClassifierException {
        System.out.println("classify");
        int num = _types.length;
        double[] values = new double[num];
        double[] features = fs.getFeatures();
        int indexBest = -1;
        double valueBest = -1;
        double EXPONENT_THRESHOLD = -7.0;
        //confidence values
        for(int i = 0; i < num; i++) {
            double[] w = _weights[i].getFeatures();
            double sum = 0;
            for(int j = 0; j < features.length; j++) {
                sum += (w[j]*features[j]);
            }
            values[i] = sum+_constants[i];
            System.out.println("\t"+_types[i]+": "+values[i]);
            if(values[i]>valueBest){
                valueBest = values[i];
                indexBest = i;
            }
        }

        if(indexBest == -1){
            //no match
            return new Classification(null, null);
        }

        double denom = 0;
        for(int i=0; i<num; i++){
            double power = values[i]-valueBest;
            if(power > EXPONENT_THRESHOLD){
                denom += Math.exp(power);
            }
        }
        double accuracy = 1/denom;
        
        //distance from mean
        /*
        double deviation = mahalanobisDistance(features,
                _means[indexBest].getFeatures(),
                _commonCovarianceInverse);
        */
        /*
        //my own way of normalizing confidence value between 0 and 1
        double confidence;
        double threshold = _thresholds[indexBest];
        confidence = 1-deviation/(4*threshold);
        System.out.println("deviation = " + deviation + ", threshold = " + threshold);        
        */
        Classification c;
        String[] s = new String[1];
        s[0] = _types[indexBest];
        double[] v = new double[1];
        v[0] = accuracy;
        c = new Classification(s, v);
        
        return c;
    }

    public Classification classify(FeatureSet fs) throws ClassifierException {
        int num = _types.length;
        double[] features = fs.getFeatures();
        double EXPONENT_THRESHOLD = -7.0;
        TypeAndValuePair pairs[] = new TypeAndValuePair[num];
            
        //confidence values
        for(int i = 0; i < num; i++) {
            double[] w = _weights[i].getFeatures();
            double sum = 0;
            for(int j = 0; j < features.length; j++) {
                sum += (w[j]*features[j]);
            }
            pairs[i] = new TypeAndValuePair(_types[i], sum+_constants[i]);
        }
        
        //sort values[] in ascending numeric order
        Arrays.sort(pairs, new MyComparator());
        String[] s = new String[num];
        double[] v = new double[num];
        for(int i=num-1, j=0; i>=0; i--, j++){
            s[j] = pairs[i].type;
            v[j] = pairs[i].value; //descending order
        }
        Classification c = new Classification(s,v);
        return c;
    }

    /**
     * Internal data structure used by the classify method.  A pair is
     * a type and confidence value.
     */
    private class TypeAndValuePair {
        public String type;
        public double value;
        
        public TypeAndValuePair(String t, double v){
            type = t;
            value = v;
        }

        public String toString(){
            return type+": "+value;
        }
    }

    /**
     * Compares 2 TypeAndValuePair objects based on their "values".
     */
    private class MyComparator implements Comparator {
        public MyComparator(){}
        public int compare (Object o1, Object o2){
            double v1 = ((TypeAndValuePair)o1).value;
            double v2 = ((TypeAndValuePair)o2).value;
            int result = 0;
            if(v1<v2){
                result = -1;
            }
            else if(v1>v2){
                result = 1;
            }
            return result;
        }
    }
    
    /**
     * Clear all results of previous trainings (presumably so that
     * this classifier can be trained again from scratch).
     */
    public void clear() {
        _weights = null;
        _constants = null;
        _types = null;
        _means = null;
        _thresholds = null;
    }
    
    
    /**
     * Compute the covariance matrix and put the result in 2D "covariance"
     * arrays [row][col].
     */
    private void computeCovariance(Iterator examples, FeatureSet avg, double[][] covariance) {
        int num = avg.getFeatureCount();
        for(int i=0; i< num; i++){
            for(int j=0; j<num; j++){
                covariance[i][j]=0;
            }
        }
        for(;examples.hasNext();){
            FeatureSet fs = (FeatureSet)examples.next();
            for(int i=0; i<num; i++){
                for(int j=0; j<num; j++){
                    double val = (fs.getFeature(i)-avg.getFeature(i))*(fs.getFeature(j)-avg.getFeature(j));
                    val += covariance[i][j];
                    covariance[i][j] = val;
                }
            }
        }
    }
    
    /**
     * Return whether this classifier is incremental, i.e. whether
     * this classifier can support multiple calls to "train".
     */
    public boolean isIncremental() {
        return false;
    }

    /**
     * Mahalanobis distance is used to determine number of standard
     * deviations the given feature set is away from the mean feature
     * vector of a training class.  Return the square of the distance
     * between the two feature vectors, using the specified dispersion
     * matrix.
     */
    static double mahalanobisDistance(double[] v, double[] u,
            double[][] sigma) {
        int n = v.length;
        double[] space = new double[n];
        for (int i = 0; i < n; i++){
            space[i] = v[i] - u[i];
        }
        double result = quadraticForm(space, sigma);
        return result;
    }
    
    /**
     * Train the classifier with a given training set and produce a
     * weight vector per training class (a weight for each feature,
     * _weights[]).  It is assumed that all training classes use the
     * same set of features.  Also a normalizing constant for each
     * class is computed as well (_constants[]).
     *
     * This method throws a ClassifierException if the training set is
     * not self consisistent, i.e. the feature sets that it contains
     * do not have the same number of features in them.  An if matrix
     * inversion fails due to singular matrix.
     */
    public void train(TrainingSet tset) throws ClassifierException {
        try {
            int numTypes = tset.getTypeCount();
            _constants = new double[numTypes];
            _weights = new FeatureSet[numTypes];
            _types = new String[numTypes];
            _means = new FeatureSet[numTypes]; // average feature vector per class
            _thresholds = new double[numTypes];
            // figure out the number of features used
            String t = (String)tset.types().next();
            int numFeatures = ((FeatureSet)(tset.positiveExamples(t).next())).getFeatureCount();
            double[][][] covarianceMatrices = new double[numTypes][numFeatures][numFeatures];//[class][row][col]
            int ct = 0;
            int totalExamples = 0;
            // Calculate average feature values
            for(Iterator types = tset.types(); types.hasNext(); ) {
                String type = (String)types.next();
                _types[ct] = type;
                int numExamples = tset.positiveExampleCount(type);
                if(numExamples<1){
                    throw new ClassifierException("Error: " + type + " has not data to train.");
                }
                totalExamples += numExamples;
                // Calculate the average value for each feature in this class.
                FeatureSet fs = new FeatureSet(numFeatures);
                for(int i = 0; i < numFeatures; i++) {
                    double sum = 0;
                    for(Iterator examples = tset.positiveExamples(type);
                        examples.hasNext();){
                        FeatureSet example = (FeatureSet)examples.next();
                        sum += (example.getFeature(i));
                    }
                    double avg = sum/(double)numExamples;
                    fs.setFeature(i, avg);
                }
                _means[ct] = fs;
                computeCovariance(tset.positiveExamples(type), fs,
                        covarianceMatrices[ct]);
                ct++;
            }
            // compute common covariance matrix
            double denom = (double)(-numTypes + totalExamples);
            if(denom <= 0){
                throw new ClassifierException("Too few examples in the training set");
            }
            double[][] common = new double[numFeatures][numFeatures];//[row][col]
            for(int i=0; i< numFeatures; i++){
                for(int j=0; j< numFeatures; j++){
                    double val=0;
                    for(int k=0; k<numTypes; k++){
                        val += covarianceMatrices[k][i][j];
                    }
                    val = val/denom;
                    common[i][j] = val;
                }
            }
            // compute weights and constants
            _commonCovarianceInverse = new double[numFeatures][numFeatures];//[row][col]
            myInvert(common, _commonCovarianceInverse);

            for(int k=0; k<numTypes; k++){
                _weights[k] = new FeatureSet(numFeatures);
                for(int j=0; j< numFeatures; j++){
                    double val=0;
                    for(int i=0; i< numFeatures; i++){
                        val += _commonCovarianceInverse[i][j]*_means[k].getFeature(i);
                    }
                    _weights[k].setFeature(j, val);
                }
                double sum = 0;
                for(int j=0; j< numFeatures; j++){
                    sum += _weights[k].getFeature(j)*_means[k].getFeature(j);
                }
                _constants[k] = sum/(-2.0);
            }

            // for each class, find the maximum distance of an example
            // from its mean.
            for(int k=0; k<numTypes; k++){
                String type = _types[k];
                double maxDist = 0;
                for(Iterator examples = tset.positiveExamples(type);
                    examples.hasNext();){
                    FeatureSet example = (FeatureSet)examples.next();
                    double deviation =
                        mahalanobisDistance(example.getFeatures(),
                                _means[k].getFeatures(),
                                _commonCovarianceInverse);
                    if(deviation > maxDist){
                        maxDist = deviation;
                    }
                }
                _thresholds[k] = maxDist;
                //                System.out.println("Threshold for " + type + ": " + maxDist);
            }
        }
        catch(RuntimeException ex){
            throw new ClassifierException(ex.getMessage());
        }
                
    }

    /////////////////////////////////////////////////////////////////////
    // The following two functions are taken from Chris Long's
    // gesture.util.Matrix class without modification.
    // Note: matrix[row][col]
    /////////////////////////////////////////////////////////////////////

    /**
     * A new invert routine taken directly from Amulet's gest_matrix.cc
     * (where it's called InvertMatrix).
     * Their comment is
     * Matrix inversion using full pivoting.
     * The standard Gauss-Jordan method is used.
     * The return value is the determinant.
     * The input matrix may be the same as the result matrix
     *
     *	det = InvertMatrix(inputmatrix, resultmatrix);
     *
     * HISTORY
     * 26-Feb-82  David Smith (drs) at Carnegie-Mellon University
     *	Written.
     * Sun Mar 20 19:36:16 EST 1988 - stolen by dandb,
     * and converted to this form
     */
    public static double myInvert(double[][] ym, double[][] rm) {
        int i, j, k;
        double det, biga, recip_biga, hold;
        int[] l = new int[ym.length];
        int[] m = new int[ym.length];
        int n;

        if (ym.length != ym[0].length) {
            System.err.println("myInvert: matrix not square");
            return Double.NaN;
        }
        n = ym.length;

        if(n != rm.length || n != rm[0].length) {
            System.err.println("myInvert: result wrong size");
            return Double.NaN;
        }

        /* Copy ym to rm */
    
        if(ym != rm)
            for(i = 0; i < n; i++)
                for(j = 0; j < n; j++)
                    rm[i][j] = ym[i][j];

        /* if(DebugInvertMatrix) PrintMatrix(rm, "Inverting (det=%g)\n", det);*/

        /* Allocate permutation vectors for l and m, with the same origin
           as the matrix. */

        det = 1.0;
        for (k = 0; k < n;  k++) {
            l[k] = k;  m[k] = k;
            biga = rm[k][k];

            /* Find the biggest element in the submatrix */
            for (i = k;  i < n;  i++)
                for (j = k; j < n; j++)
                    if (Math.abs(rm[i][j]) > Math.abs(biga)) {
                        biga = rm[i][j];
                        l[k] = i;
                        m[k] = j;
                    }

            /* if(DebugInvertMatrix) 
               if(biga == 0.0)
               PrintMatrix((Matrix)m, "found zero biga = %g\n", biga);*/

            /* Interchange rows */
            i = l[k];
            if (i > k)
                for (j = 0; j < n; j++) {
                    hold = -rm[k][j];
                    rm[k][j] = rm[i][j];
                    rm[i][j] = hold;
                }

            /* Interchange columns */
            j = m[k];
            if (j > k)
                for (i = 0; i < n; i++) {
                    hold = -rm[i][k];
                    rm[i][k] = rm[i][j];
                    rm[i][j] = hold;
                }

            /* Divide column by minus pivot
               (value of pivot element is contained in biga). */
            if (biga == 0.0) {
                return 0.0;
            }

            /*
              if(DebugInvertMatrix) printf("biga = %g\n", biga);
            */
            recip_biga = 1/biga;
            for (i = 0; i < n; i++)
                if (i != k)
                    rm[i][k] *= -recip_biga;

            /* Reduce matrix */
            for (i = 0; i < n; i++)
                if (i != k) {
                    hold = rm[i][k];
                    for (j = 0; j < n; j++)
                        if (j != k)
                            rm[i][j] += hold * rm[k][j];
                }

            /* Divide row by pivot */
            for (j = 0; j < n; j++)
                if (j != k)
                    rm[k][j] *= recip_biga;

            det *= biga;	/* Product of pivots */
            /*
              if(DebugInvertMatrix) printf("det = %g\n", det);
            */
            rm[k][k] = recip_biga;

        }	/* K loop */

        /* Final row & column interchanges */
        for (k = n - 1; k >= 0; k--) {
            i = l[k];
            if (i > k)
                for (j = 0; j < n; j++) {
                    hold = rm[j][k];
                    rm[j][k] = -rm[j][i];
                    rm[j][i] = hold;
                }
            j = m[k];
            if (j > k)
                for (i = 0; i < n; i++) {
                    hold = rm[k][i];
                    rm[k][i] = -rm[j][i];
                    rm[j][i] = hold;
                }
        }

        /*
          if(DebugInvertMatrix) printf("returning, det = %g\n", det);
        */

        return det;
    }

    public static double quadraticForm(double[] v, double[][] m) {
        int n;
        double result = 0;

        n = v.length;

        if (n != m.length || n != m[0].length) {
            System.err.println("Matrix.QuadraticForm: bad matrix size");
            return 0;
        }

        for(int i = 0; i < n; i++)
            for(int j = 0; j < n; j++) {
                result += m[i][j] * v[i] * v[j];
            }
        return result;
    }
}

