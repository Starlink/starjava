/*
 * $Id: BasicStrokeRecognizer.java,v 1.10 2001/08/28 06:34:10 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.recognition;

import diva.sketch.classification.Classification;
import diva.sketch.classification.ClassifierException;
import diva.sketch.classification.Classifier;
import diva.sketch.classification.FeatureSet;
import diva.sketch.classification.TrainableClassifier;
import diva.sketch.classification.TrainingSet;
import diva.sketch.classification.WeightedEuclideanClassifier;
import diva.sketch.toolbox.StrokeFilter;
import diva.sketch.toolbox.ApproximateStrokeFilter;

import diva.sketch.features.*;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;


/**
 * BasicStrokeRecognizer performs recognition on <b>completed</b> strokes by
 * filtering them, extracting features from them (e.g. aspect
 * ratio), and then passing the extracted features into a classifier
 * that performs classification on the feature set.  It also provides
 * methods to train the classifier if the classifier is trainable.
 * <P>
 *
 * A BasicStrokeRecognizer uses a classifier to perform classification.
 * Different kinds of classifiers use different classification
 * algorithm (e.g. Gaussian, Laplace, parametric, nonparametric,
 * etc.).  Suppose a classifier has been trained on square, circle,
 * and triangle.  When a user sketches a stroke, the classifier will
 * try to determine how closely the strokes resembles each of the
 * trained strokes by generating a confidence value for each stroke.
 * On a scale of 0 through 100, if a stroke receives 95 for being a
 * square, 40 for circle, and 5 for triangle, then it is verly likely
 * that the strokes is a square.
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @author  Heloise Hse     (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.10 $
 */
public class BasicStrokeRecognizer implements StrokeRecognizer {
    /**
     * A classifier which performs classification algorithms.
     */
    private Classifier _classifier;

    /**
     * An array of feature extractors which will generate a feature
     * set given a stroke.
     */
    private FeatureExtractor[] _featureExtractors = null;

    /**
     * A stroke filter which performs preprocessing on a stroke.
     */
    private StrokeFilter _filter = null;

    /**
     * Construct a basic recognizer that performs recognition using a
     * WeightedEuclideanClassifier, the default features, and an
     * ApproximateStrokeFilter.
     *
     * @see #defaultFeatureExtractors()
     */
    public BasicStrokeRecognizer() {
        this(new WeightedEuclideanClassifier(),
                defaultFeatureExtractors(),
                new ApproximateStrokeFilter());
    }

    /**
     * Construct a basic recognizer that performs recognition using
     * the given classifier, the default features, and an
     * ApproximateStrokeFilter.
     *
     * @see #defaultFeatureExtractors()
     */
    public BasicStrokeRecognizer(Classifier classifier){
        this(classifier, defaultFeatureExtractors(),
                new ApproximateStrokeFilter());
    }
    
    /**
     * Construct a classifying recognizer that classifies with the
     * given classifier, set of feature extractors, and filter.  A
     * <i>null</i> filter indicates no filtering should be performed.
     */
    public BasicStrokeRecognizer(Classifier classifier,
            FeatureExtractor[] extractors,
            StrokeFilter filter) {
        _classifier = classifier;
        _featureExtractors = extractors;
        _filter = filter;
    }

    /**
     * Construct a basic recognizer that trains on the gestures in the
     * training file.
     */
    public BasicStrokeRecognizer(Reader trainingFile) throws Exception {
        this();
        SSTrainingParser parser = new SSTrainingParser();
        SSTrainingModel model = (SSTrainingModel)parser.parse(trainingFile);
        train(model);
    }

    /**
     * Construct a basic recognizer that trains on the gestures in the
     * training files.  A SSTrainingParser is used to combine the
     * gestures in the training files and form a training model data
     * structure.  The recognizer then trains its classifier on the
     * training model.
     */
    public BasicStrokeRecognizer(Reader[] trainingFiles) throws Exception {
        this();
        SSTrainingParser parser = new SSTrainingParser();
        SSTrainingModel model = (SSTrainingModel)parser.parse(trainingFiles);
        train(model);
    }
    

    /**
     * Debugging output.
     */
    private void debug(String s) {
        System.err.println(s);
    }

    /**
     * Initialize an array of feature extractors which are used to
     * produce a feature set from a gesture.
     * 
     * <p>
     * Be default the following set of feature extractors are
     * used:
     * 
     * <ol>
     * <li>CosInitAngleFE</li>
     * <li>SineInitAngleFE</li>
     * <li>BBoxDiagonalAngleFE</li>
     * <li>DistanceStartEndPtsFE</li>
     * <li>CosFirstLastPtsFE</li>
     * <li>SineFirstLastPtsFE</li>
     * <li>SumOfAnglesFE</li>
     * <li>SumOfAbsoluteAnglesFE</li>
     * <li>SumOfSquaredAnglesFE</li>
     * <li>AreaRatioFE</li>
     * <li>AspectRatioFE</li>
     * <li>CornerFE</li>
     * </ol>
     * <p>
     */
    public static final FeatureExtractor[] defaultFeatureExtractors() {
        FeatureExtractor[] fes = {
            new CosInitAngleFE(),
            new SineInitAngleFE(),
            new BBoxDiagonalAngleFE(),
            new DistanceStartEndPtsFE(),
            new CosFirstLastPtsFE(),
            new SineFirstLastPtsFE(),
            new SumOfAnglesFE(),
            new SumOfAbsoluteAnglesFE(),
            new SumOfSquaredAnglesFE(),
            new AreaRatioFE(),
            new AspectRatioFE(),
            new CornerFE()
        };
        return fes;
    }
    
    /**
     * First filter the stroke, then extract the features from the
     * filtered copy stroke by running each of the feature extractors
     * on the copy, in order, and setting the corresponding
     * <i>i'th</i> feature in the returned feature set to the
     * extracted value from the <i>i'th</i> feature extractor. <p>
     *
     * If the feature extractor array is empty return null.
     */
    public FeatureSet extractFeatures(TimedStroke s) throws ClassifierException {
        if(_featureExtractors == null || _featureExtractors.length == 0) {
            return null;
        }
        
        TimedStroke filteredStroke = (_filter==null) ? s : _filter.apply(s);
        FeatureSet fset = new FeatureSet(_featureExtractors.length);
        for(int i = 0; i < _featureExtractors.length; i++) {
            FeatureExtractor fe = _featureExtractors[i];
            double val = fe.apply(filteredStroke);
            fset.setFeature(i, val);
        }
        return fset;
    }

    /**
     * Return the stroke filter that is used to preprocess
     * the strokes.
     */
    public StrokeFilter getStrokeFilter() {
        return _filter;
    }

    /**
     * Return the classifier that is used to perform
     * classification.
     */
    public Classifier getClassifier() {
        return _classifier;
    }
    
    /**
     * Perform recognition on the given completed stroke. This occurs
     * when the mouse up event has been detected.  The recognition
     * consists of the following steps:
     * 
     * <ol>
     * <li> (optionally) filter the stroke. </li>
     *
     * <li> extract features from the stroke, producing a feature set. </li>
     *
     * <li> classify the feature set using the default or user-specified
     * classifier. </li>
     * 
     * <li> create a Recognition object for each classification
     * type.  This object stores the type and confidence of the stroke.
     * For example, the stroke may receive 95% confidence for being
     * a square type. </li>
     *
     * <li> Return the Recognition objects. </li>
     * </ol>
     */
    public RecognitionSet strokeCompleted (TimedStroke s) {
        try{
            FeatureSet fset = extractFeatures(s);
            if(fset == null) {
                return RecognitionSet.NO_RECOGNITION;
            }
            Classification classification = getClassifier().classify(fset);
            Recognition[] rs = new Recognition[classification.getTypeCount()];
            for(int i = 0; i < rs.length; i++) {
                String type = classification.getType(i);
                double confidence = classification.getConfidence(i);
                rs[i] = new Recognition(new SimpleData(type), confidence);
            }
            return new RecognitionSet(rs);
        }
        catch(ClassifierException ex) {
            ex.printStackTrace();
            return RecognitionSet.NO_RECOGNITION;
        }
    }

    /**
     * Returns NO_RECOGNITION; BasicStrokeRecognizer only operates
     * on completed strokes.
     */
    public RecognitionSet strokeModified (TimedStroke s) {
        return RecognitionSet.NO_RECOGNITION;
    }

    /**
     * Returns NO_RECOGNITION; BasicStrokeRecognizer only operates
     * on completed strokes.
     */
    public RecognitionSet strokeStarted (TimedStroke s) {
        return RecognitionSet.NO_RECOGNITION;
    }

    /**
     * This function takes in a SSTrainingModel, builds a TrainingSet
     * object from the examples in the model, and trains the
     * classifier with the training set data.  This only makes sense
     * if the classifier used by this recognizer is trainable.
     * Otherwise, a ClassifierException will be thrown.  If the
     * trainable classifier is incremental, then its train function
     * will be called on the training set.  Otherwise, we will first
     * clear its weight sets, and then rebuild on the training data.
     * <p>
     *
     * The following algorithm is applied:
     *
     * <ol>
     * <li> Preprocess strokes (e.g. filter strokes). </li>
     *
     * <li> Extract features from each strokes. </li>
     *
     * <li> Build a TrainingSet object from the types and the feature
     * sets. </li>
     *
     * <li> Train classifier with the training set. </li>
     *
     * </ol>
     */
    public void train(SSTrainingModel model)
            throws ClassifierException {

        Classifier c = getClassifier();
        if(!(c instanceof TrainableClassifier)) {
            throw new ClassifierException("Current classifier is not trainable");
        }
        TrainableClassifier classifier = (TrainableClassifier)c;

        if((model == null) || (model.getTypeCount()==0)){
            throw new ClassifierException("No types to train.");
        }
        
        TrainingSet set = new TrainingSet();
        for(Iterator types = model.types(); types.hasNext();){
            String type = (String)types.next();
            for(Iterator examples = model.positiveExamples(type);
                examples.hasNext();){
                TimedStroke stroke = (TimedStroke)examples.next();
                FeatureSet fset = extractFeatures(stroke);
                if(fset != null){
                    set.addPositiveExample(type, fset);
                }
            }
            for(Iterator examples = model.negativeExamples(type);
                examples.hasNext();){
                TimedStroke stroke = (TimedStroke)examples.next();
                FeatureSet fset = extractFeatures(stroke);
                if(fset != null){
                    set.addNegativeExample(type, fset);
                }
            }
        }
        if(!classifier.isIncremental()){
            classifier.clear();
        }
        classifier.train(set);
    }
}




