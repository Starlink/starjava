/*
 * $Id: WeightSet.java,v 1.5 2001/10/31 02:00:37 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */

package diva.sketch.classification;
import diva.util.NullIterator;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A WeightSet object represents a training type (e.g. square, class,
 * circle, etc.)  It has a mu vector and a sigma vector. Each element
 * in the mu vector represents the average value (e.g. mean) for a
 * particular feature of this class, and the corresponding sigma value
 * indicates how much the value of the feature may vary (e.g. standard
 * deviation).  However, the exact statistical calculation for mu and
 * sigma vary depending on the type of classification algorithms.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.5 $
 */
public abstract class WeightSet {
    /**
     * The type that this weight set represents.
     */
    private String _type;

    /**
     * The cached mu values in this weight set.
     */
    private FeatureSet _mus = null;

    /**
     * The cached sigma values in this weight set.
     */
    private FeatureSet _sigmas = null;

    /*
     * The training examples in this weight set.
     */
    private ArrayList _examples = new ArrayList();

    /**
     * Construct a WeightSet of the specified type.
     */
    public WeightSet(String type) {
        _type = type;
    }

    /**
     * Add an example to this classifier.  The example will be used to
     * compute mu and sigma.
     */
    public void addExample(FeatureSet f){
       _examples.add(f);
    }
    
    /**
     * Clear the examples.  Once the training is complete
     * it is a waste of memory to retain all of the examples.
     */
    public void clearExamples() {
        _examples = null;
    }
    
    /**
     * Calculate the mu values for each feature in this class.
     * Return the feature set representing the mu values.
     */
    protected abstract FeatureSet computeMu() throws ClassifierException;

    /**
     * Calculate the sigma values for each feature in this class.
     * Return the feature set representing the sigma values.
     */
    protected abstract FeatureSet computeSigma() throws ClassifierException;
    
    protected void debug(String s) {
        //System.err.println(s);
    }

    /**
     * Return an iterator over the examples of this classifier.
     * The examples are cleared once training is complete, so this
     * method will return an empty iterator if it is called
     * post-training.
     */
    protected Iterator examples() {
        return (_examples == null) ? new NullIterator() : _examples.iterator();
    }

    /**
     * Return the number of examples.
     */
    public int getExampleCount() {
        return (_examples == null) ? 0 : _examples.size();
    }

    /**
     *  Return the mu values.
     */
    public FeatureSet getMuValues() {
        return _mus;
    }

    
    /**
     *  Return the sigma values.
     */
    public FeatureSet getSigmaValues() {
        return _sigmas;
    }

    /**
     *  Return the type of this classifier.
     */
    public String getType(){
        return _type;
    }

    /**
     * Train on the examples by computing the mu and sigma values for
     * each feature.  This method clears the examples once the training is
     * complete, to avoid wasted space.
     */
    public void train() throws ClassifierException {
        _mus = computeMu();
        _sigmas = computeSigma();
        
        debug("mu:  " + _mus.toString());
        debug("sigma: " + _sigmas.toString());

        clearExamples();
    }


    /**
     * The text representation of this weight set.
     */
    public String toString() {
        //return "WeightSet[" + _type + "]";
        StringBuffer buf = new StringBuffer();
        buf.append(_type+":\n");
        buf.append("mu     :"+_mus.toString()+"\n");
        buf.append("sigma  :"+_sigmas.toString()+"\n");
        return buf.toString();
    }
}


