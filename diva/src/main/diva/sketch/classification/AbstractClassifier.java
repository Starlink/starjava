/*
 * $Id: AbstractClassifier.java,v 1.2 2001/10/31 02:00:37 hwawen Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.classification;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Given a training set containing multiple classes, for each class,
 * an AbstractClassifier would compute the mu and sigma of each
 * feature of that class.  These mu's and sigma's are stored in a
 * WeightSet object.  As the result, if there are n classes, there
 * should be n WeightSet objects.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 */
public abstract class AbstractClassifier implements TrainableClassifier {

    /**
     * An array of WeighSet objects, one per class.
     */
    protected ArrayList _weights;
    
    /**
     * Construct an abstract classifier and instantiate its weight
     * array for features.
     */
    public AbstractClassifier() {
        _weights = new ArrayList();
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
    public final void debug(String s) {
        //System.err.println(s);
    }

    /**
     * Return false; not incremental.
     */
    public final boolean isIncremental() {
        return false;
    }
    
    /**
     * Train on the given data set by building the set of weights that
     * are to be used by the classify() method.  There is one
     * WeightSet object per class.  The WeightSet object computes and
     * stores the mu's and sigma's of the features of that class.
     */
    public void train(TrainingSet tset) throws ClassifierException {
        for(Iterator i = tset.types(); i.hasNext(); ){
            String type = (String)i.next();
            WeightSet ws = new GaussianWeightSet(type);
            for(Iterator j = tset.positiveExamples(type); j.hasNext(); ) {
                ws.addExample((FeatureSet)j.next());
            }
            ws.train();
            //System.out.println(ws);
            _weights.add(ws);
        }
    }
}

