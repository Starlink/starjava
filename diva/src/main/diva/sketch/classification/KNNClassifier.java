/*
 * $Id: KNNClassifier.java,v 1.2 2001/07/22 22:01:44 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.classification;

import java.util.Iterator;
import java.util.Vector;

/**
 * A K Nearest Neighbor classifier compares a given example (feature
 * set) to the training set and make its prediction based on the
 * majority match in the top K candidates.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 */
public class KNNClassifier implements TrainableClassifier {
    /** K by default is set to 5.  This selects the majority match
        using the top 5 candidates from classification. */
    private int _k = 5;
    private TrainingSet _set;
    
    public KNNClassifier(){}

    /**
     * Create a K nearest neighbor classifier in which K is set to
     * specified number.
     */
    public KNNClassifier(int k){
        _k = k;
    }

    /**
     * Set the value of K.  The classifier finds the closest K matches.
     */
    public void setK(int k){
        _k = k;
    }
    
    /**
     * Store the given training set which will be used during
     * classification.  If a training set already exist, this method
     * simply adds the example in the given training set (s) to the
     * existing set.  This also means that if an example in "s"
     * already exists in the current training set, it'll be added
     * again.
     */
    public void train(TrainingSet s) throws ClassifierException {
        if(_set == null){
            _set = s;
        }
        else {
            for(Iterator i = s.types(); i.hasNext();){
                String type = (String)i.next();
                for(Iterator e = s.positiveExamples(type); e.hasNext();){
                    FeatureSet fs = (FeatureSet)e.next();
                    _set.addPositiveExample(type, fs);
                }
                for(Iterator e = s.negativeExamples(type); e.hasNext();){
                    FeatureSet fs = (FeatureSet)e.next();
                    _set.addNegativeExample(type, fs);
                }
            }
        }
    }

    /**
     * Return true.  This classifier is incremental.  It supports
     * multiple calls to "train".
     */
    public boolean isIncremental() {
        return true;
    }

    /**
     * Clear all results of previous trainings (presumably so that
     * this classifier can be trained again from scratch).  This
     * simply sets the current training set to null.
     */
    public void clear() {
        _set = null;        
    }

    /**
     * Compare the given example (features) to the examples in the
     * training set and save the top K closest matches in ascending
     * distance values.
     */
    public Classification classify(FeatureSet s) throws ClassifierException {
        Vector v = new Vector(_k);
        int matchSoFar = 0;
        int numFeatures = s.getFeatureCount();
        
        for(Iterator i = _set.types(); i.hasNext();){
            String type = (String)i.next();
            for(Iterator e = _set.positiveExamples(type); e.hasNext();){
                FeatureSet fs = (FeatureSet)e.next();
                double dist = compare(fs, s);
                boolean isAdded = false;
                for(int j=0; j<matchSoFar; j++){
                    Pair p = (Pair)v.get(j);
                    if(dist < p.distance){
                        v.insertElementAt(new Pair(type, dist), j);
                        isAdded = true;
                        if(v.size() > _k){
                            v.removeElementAt(v.size()-1);
                        }
                        else {
                            matchSoFar++;
                        }
                        break;
                    }
                }
                if(!isAdded && (matchSoFar<_k)){
                    v.add(new Pair(type, dist));
                    matchSoFar++;
                }
            }
        }
        String types[] = new String[matchSoFar];
        double values[] = new double[matchSoFar];
        for(int i=0; i<matchSoFar; i++){
            Pair p = (Pair)v.get(i);
            types[i] = p.type;
            values[i] = -p.distance;//change to negative, so the smaller distance gives greater confidence value
        }
        Classification result = new Classification(types, values);
        return result;
    }

    /**
     * A pair contains a type string and a distance value.  This is
     * used by the classify method to record the distance between an
     * input feature vector and a training example of a particular type.
     */
    private class Pair {
        public String type;
        public double distance;
        public Pair(String t, double d){
            type = t;
            distance = d;
        }
    }

    /**
     * Return the distance of the two feature sets by computing the
     * Euclidean distance between each features and sum them up.  If
     * the two given feature sets have different number of features,
     * Double.MAX_VALUE is returned.
     */
    private static double compare(FeatureSet f1, FeatureSet f2){
        if(f1.getFeatureCount() != f2.getFeatureCount()){
            return Double.MAX_VALUE;
        }
        else {
            double v1[] = f1.getFeatures();
            double v2[] = f2.getFeatures();
            int sum = 0;
            for(int i=0; i<v1.length; i++){
                sum += Math.abs(v1[i]-v2[i]);
            }
            return (double)sum;
        }
    }

    public static double f1[] = {2,2,2,2,2};
    public static double f2[] = {3,3,3,3,3};
    public static double f3[] = {4,4,4,4,4};
    
    public static double f4[] = {1,1,1,1,1};
    public static double f5[] = {10,10,10,10,10};
    public static double f6[] = {20,25,40,45,100};
    
    public static double f7[] = {0,0,0,0,0};
    public static double f8[] = {-1,-1,-1,-1,-1};
    public static double f9[] = {-2,-2,-2,-2,-2};
    
    public static void main(String argv[]){
        try{
            KNNClassifier knn = new KNNClassifier();
            TrainingSet set = new TrainingSet();
            set.addPositiveExample("t1",new FeatureSet(f1));
            set.addPositiveExample("t1",new FeatureSet(f2));
            set.addPositiveExample("t1",new FeatureSet(f3));
            set.addPositiveExample("t2",new FeatureSet(f4));
            set.addPositiveExample("t2",new FeatureSet(f5));
            set.addPositiveExample("t2",new FeatureSet(f6));        
            set.addPositiveExample("t3",new FeatureSet(f7));
            set.addPositiveExample("t3",new FeatureSet(f8));
            set.addPositiveExample("t3",new FeatureSet(f9));        
            knn.train(set);
            
            double test[] = new double[5];
            test[0] = 1;
            test[1] = 1;
            test[2] = 1;
            test[3] = 1;
            test[4] = 1;        
            FeatureSet f = new FeatureSet(test);
            Classification result = knn.classify(f);
            System.out.println(result);
        }
        catch(ClassifierException ce){
            ce.printStackTrace();
        }
    }
}

