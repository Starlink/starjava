/*
 * $Id: CrossValidation.java,v 1.4 2001/10/31 02:17:44 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.classification;

import diva.sketch.classification.*;
import java.util.*;

/**
 * @author Heloise Hse   (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 * @rating Red
 */
public class CrossValidation {
    /** Number of subsets to divide the data set into, using 1/K of
        the data as our held-out set*/
    public final static int K = 10;

    /** K-fold cross validation where K is the number of subsets to use */
    private int _k;

    /** The type of learning algorithm to use */
    private String _classifierType;

    /** The training data on which the cross-validation is performed */
    private CVData _data;
    
    public CrossValidation(int k, String classifierType, CVData data){
        _k = k;
        _classifierType = classifierType;
        _data = data;
    }
    
    public CrossValidation(String classifierType, CVData data){
        this(K, classifierType, data);
    }

    public static CVResult crossValidate(int k, TrainableClassifier classifier, CVData cvData){
        CVResult finalCVResult = new CVResult();
        try{
            float alpha = (float)1/k;
            String types[] = new String[cvData.getTypeCount()];
            int i=0;
            for(Iterator iter = cvData.types(); iter.hasNext();){
                String type = (String)iter.next();
                types[i++] = type;
            }
            CVResult results[] = new CVResult[k];
            for(i=0; i<k; i++){
                //do k iterations, each time using a different
                //held-out data
                TrainingSet trainingData = new TrainingSet();
                TrainingSet heldoutData = new TrainingSet();
                for(int j=0; j<types.length; j++){
                    String type = types[j];
                    FeatureSet examples[] = cvData.getExamples(type);
                    int num = examples.length;
                    if(num < k){//don't have enough data to do CV
                        continue;
                    }
                    int beta = (int)(num*alpha);
                    int minIndex = beta*i;
                    int maxIndex = minIndex+beta-1;
                    for(int m=0; m<num; m++){
                        if(m>=minIndex && m<=maxIndex){
                            heldoutData.addPositiveExample(type, examples[m]);
                        }
                        else {
                            trainingData.addPositiveExample(type, examples[m]);
                        }
                    }
                }

                classifier.train(trainingData);

                //test
                CVResult cvResult = new CVResult();
                for(Iterator iter = heldoutData.types(); iter.hasNext();){
                    String trueType = (String)iter.next();
                    for(Iterator examples = heldoutData.positiveExamples(trueType); examples.hasNext();){
                        FeatureSet fs = (FeatureSet)examples.next();
                        Classification c = classifier.classify(fs);
                        String recoType = c.getHighestConfidenceType();
                        if(recoType.equals(trueType)){//correct
                            cvResult.incrCorrect(trueType);
                        }
                        else {//misrecognition, trueType->recoType
                            cvResult.incrIncorrect(trueType);
                        }
                    }
                }
                results[i]=cvResult;
            }
            //combine result from all the iterations
            for(i=0; i<k; i++){
                finalCVResult.combine(results[i]);
            }
            
        }
        catch(ClassifierException ex){
            ex.printStackTrace();
        }

        return finalCVResult;
    }

    /**
     * Containing the training set on which the cross-vali
     */
    public static class CVData {
        /**
         * This HashMap maps a class type to its examples (ArrayList
         * containing FeatureSet objects)
         */
        HashMap _mapTypeToExamples;

        public CVData(){
            _mapTypeToExamples = new HashMap();
        }

        /** Add examples of the specified type.  The ArrayList
         *  contains FeatureSet objects.
         */
        public void addClass(String type, ArrayList examples){
            ArrayList list = (ArrayList)_mapTypeToExamples.get(type);
            if(list == null){
                list = new ArrayList();
                _mapTypeToExamples.put(type, list);
            }
            list.add(examples);            
        }

        /** Add an example of the specified type */
        public void addExample(String type, FeatureSet example){
            ArrayList list = (ArrayList)_mapTypeToExamples.get(type);
            if(list == null){
                list = new ArrayList();
                _mapTypeToExamples.put(type, list);
            }
            list.add(example);
        }

        public int getTypeCount(){
            return _mapTypeToExamples.size();
        }

        public Iterator types(){
            return _mapTypeToExamples.keySet().iterator();
        }

        public FeatureSet[] getExamples(String type){
            ArrayList examples = (ArrayList)_mapTypeToExamples.get(type);
            Object objs[] = examples.toArray();
            FeatureSet featureVecs[] = new FeatureSet[objs.length];
            for(int i=0; i<objs.length; i++){
                featureVecs[i] = (FeatureSet)objs[i];
            }
            return featureVecs;
        }

        public String toString(){
            StringBuffer buf = new StringBuffer();
            for(Iterator iter = types(); iter.hasNext();){
                String type = (String)iter.next();
                FeatureSet set[] = getExamples(type);
                int num = set.length;
                buf.append(num + " " +  type);
                String postFix = (num<2)?"\n":"s\n";
                buf.append(postFix);
            }
            return buf.toString();
        }
    }

    public static class CVResult {
        HashMap _mapTypeToStat = new HashMap();
        
        public CVResult(){}

        /**
         * Increment the number of correct count by 1 for the
         * specified type.
         */
        public void incrCorrect(String trueType){
            Stat stat = getStat(trueType);
            stat.numCorrect++;
        }

        /**
         * Increment the number of incorrect count by 1 for the
         * specified type.  This means the a "trueType" has been
         * misrecognized as something else.
         */
        public void incrIncorrect(String trueType){
            Stat stat = getStat(trueType);
            stat.numIncorrect++;
        }

        public void combine(CVResult cvResult){
            for(Iterator types = cvResult.types(); types.hasNext();){
                String type = (String)types.next();
                int numCorr = cvResult.getCorrectCount(type);
                int numIncorr = cvResult.getIncorrectCount(type);

                Stat stat = getStat(type);
                stat.numCorrect += numCorr;
                stat.numIncorrect += numIncorr;
            }
        }

        private Stat getStat(String type){
            Stat stat = (Stat)_mapTypeToStat.get(type);
            if(stat == null){
                stat = new Stat();
                _mapTypeToStat.put(type, stat);
            }
            return stat;
        }
        
        public int getCorrectCount(String type){
            Stat stat = (Stat)_mapTypeToStat.get(type);
            if(stat == null){
                return 0;
            }
            else {
                return stat.numCorrect;
            }
        }

        public int getIncorrectCount(String type){
            Stat stat = (Stat)_mapTypeToStat.get(type);
            if(stat == null){
                return 0;
            }
            else {
                return stat.numIncorrect;
            }
        }

        public double getAccuracyRate(String type){
            Stat stat = (Stat)_mapTypeToStat.get(type);
            if(stat == null){
                return Double.NaN;
            }
            else {
                return stat.getAccuracyRate();
            }
        }

        public double getErrorRate(String type){
            Stat stat = (Stat)_mapTypeToStat.get(type);
            if(stat == null){
                return Double.NaN;
            }
            else {
                return stat.getErrorRate();
            }
        }
        
        public Iterator types(){
            return _mapTypeToStat.keySet().iterator();
        }

        public String toString(){
            StringBuffer buf = new StringBuffer();
            for(Iterator iter = types(); iter.hasNext();){
                String type = (String)iter.next();
                int numCorr = getCorrectCount(type);
                int numIncorr = getIncorrectCount(type);
                double acc = getAccuracyRate(type);
                double err = getErrorRate(type);                
                buf.append(type + ": " + numCorr + " correct, " + numIncorr + " misses, accuracy rate: " + acc + "%, error rate: " + err+"%\n");
            }
            return buf.toString();
        }

        /** private data structure used only by CVResult to store data */
        private static class Stat {
            public int numCorrect = 0;
            public int numIncorrect = 0;

            /** ( #correct/(#correct+#incorrect) ) * 100% */
            public double getAccuracyRate(){
                return 100*numCorrect/(numCorrect+numIncorrect);
            }

            /** ( #incorrect/(#correct+#incorrect) ) * 100% */            
            public double getErrorRate(){
                return 100*numIncorrect/(numCorrect+numIncorrect);
            }
        }
    }
}
