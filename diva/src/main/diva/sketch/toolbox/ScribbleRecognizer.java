/*
 * $Id: ScribbleRecognizer.java,v 1.10 2001/08/28 06:37:13 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;

import diva.sketch.recognition.StrokeRecognizer;
import diva.sketch.recognition.Recognition;
import diva.sketch.recognition.RecognitionSet;
import diva.sketch.recognition.TimedStroke;
import diva.sketch.recognition.SimpleData;

import diva.sketch.features.AspectRatioFE;
import diva.sketch.features.CornerFE;
import diva.sketch.features.StrokeBBox;
import diva.sketch.features.SumOfAbsDeltaRatioFE;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A recognizer that recognizes scribble shapes: /\/\/\/\
 *
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.10 $
 * @rating Red
 */
public class ScribbleRecognizer implements StrokeRecognizer {
    /**
     * The type ID that is used when scribble objects are
     * recognized.
     */
    public static final String SCRIBBLE_TYPE_ID = "SCRIBBLE";

    /**
     * A stroke's delta ratio has to meet this criteria for it to be
     * considered as a possible scribble gesture.  This says that the
     * sum of |delta y's| has to exceed the sum of |delta x's|.  This
     * feature is computed by _deltaFE.
     */
    private double _minDeltaRatio = 1.5;

    /**
     * If a stroke's delta ratio exceeds _threshDeltaRatio, then it
     * receives a high score in confidence of being a scribble.
     */
    private double _threshDeltaRatio = 2.2;

    /**
     * A stroke's aspect ratio (w/h) has to meet this criteria for it
     * to be considered as a possible scribble gesture.  Most of the
     * time a scribble is longer in the x direction (e.g. /\/\/\/\/\).
     * However, sometimes it can be tall and a little skinny.
     */
    private double _minAspectRatio = 0.5;

    /**
     * This places a constraint in how many times a pen has to move
     * up and down (creating corners) for the stroke to be considered
     * as a scribble.  For example, /\/\ will not be considered as a
     * scribble, where as /\/\/ will be.
     */
    private int _minNumCorners = 4;

    /**
     * An empirical value derived from testing a bunch of scribbles on
     * their (deltaRatio * aspectRatio) values.
     */
    private double _standard = 3.5;

    /**
     * The highest confidence possible.
     */
    private double _perfectScore = 100;  

    /**
     * This function assumes that the gesture has been corner detected
     * and so it has a list of corner indices cached in its property
     * table.  For each pair of corners, we want to check that there
     * is a line (not an arc) between the corners.  In addtion, we
     * check for alternating up-and-down motion in a stroke.
     *
     * <P>
     * Return false if the segment in between two corner indices is
     * not a line or if the checking for alternating up-and-down
     * motion fails.
     */
    private boolean checkSegments(TimedStroke stroke){
        debug("checkSegments");
        ArrayList cornerIndices =
            (ArrayList)stroke.getProperty(CornerFE.PROPERTY_KEY);
        int numPointsOnALine = 2;
        int prev = 0;
        double dist;
        boolean isFirst = true;
        boolean wasUp = true;
        boolean isScribble = true;
        double startY, endY; //startX, endX, 
        debug("indices = " + cornerIndices);
            
        for(Iterator i = cornerIndices.iterator(); i.hasNext();){
            int index = ((Integer)(i.next())).intValue();
            //startX = stroke.getX(prev);
            //endX = stroke.getX(index);
            startY = stroke.getY(prev);
            endY = stroke.getY(index);
            double diff = endY-startY;
            debug("diff between ["+prev+", " +index+"]: " + diff);
            //check for up and down motion
            if(isFirst){//first check
                if(diff != 0){
                    wasUp = (diff > 0) ? false : true;
                    isFirst = false;
                }
                //else initial direction of stroke still undetermined.
            }
            else{
                if(((diff > 0)&& !wasUp)||((diff < 0) && wasUp)){
                    isScribble = false;
                }
                else if (diff != 0) {
                    wasUp = !wasUp;
                }
            }
            if(!isScribble){
                break;
            }
            // check for line segments in between corner points.  A
            // very simple way to check for lines between corners is
            // to check the number of points in between two corner
            // indices.  The reason is that if there's too many points
            // in between, then this is probably an arc.  Keep in mind
            // that the stroke has already been filtered, and points
            // that lie on the same line are reduced.
            
            if((index-prev)>numPointsOnALine){
                //too many points in between corner indices meaning
                //this is probably not a line segment.
                debug("ARC between [" + prev + ", " + index + "]");
                isScribble = false;
                break;
            }
            if(!isScribble){
                break;
            }
            prev = index;
        }
        return isScribble;
    }

    /**
     * Debugging output.
     */
    private void debug (String s) {
        //        System.err.println(s);
    }
    
    /**
     * Compute the ratio of the sum of the absolute values of the
     * delta y's and the sum of the absolute values of the delta x's,
     * the aspect ratio, and the number of corners.  These are used to
     * determine the scribble confidence value for the gesture.
     */
    public RecognitionSet strokeCompleted (TimedStroke s) {
        TimedStroke fs = ApproximateStrokeFilter.approximate(s);
        //extract features
        double deltaRatio = SumOfAbsDeltaRatioFE.sumOfAbsDeltaRatio(fs);
        double aspectRatio = AspectRatioFE.aspectRatio(fs);
        double corners = CornerFE.numCorners(fs);

        debug("dy/dx = " + deltaRatio + ", w/h: " + aspectRatio +
                ", corners: " + corners);

        if((corners >= _minNumCorners) &&
                (aspectRatio >= _minAspectRatio) &&
                (deltaRatio >= _minDeltaRatio)) {
            
            // linear slope is okay??
            double deltaConfidence =
                (deltaRatio >= _threshDeltaRatio) ? _perfectScore :
                (Math.sqrt((deltaRatio - _minDeltaRatio) /
                        (_threshDeltaRatio - _minDeltaRatio))*_perfectScore);

            if(deltaConfidence < (.9*_perfectScore)) {
                deltaConfidence =
                    (_standard-Math.abs(deltaRatio*aspectRatio-_standard)) /
                    _standard * _perfectScore;
            }

            if(checkSegments(fs)){
                debug("Scribble " + deltaConfidence);
                Recognition[] rs = new Recognition[1];
                rs[0] = new Recognition(new SimpleData(SCRIBBLE_TYPE_ID), deltaConfidence);
                return new RecognitionSet(rs);
            }
            else{
                debug("Scribble failed line and slope checks");
            }
        }
        return RecognitionSet.NO_RECOGNITION;
    }

    /**
     * Return NO_RECOGNITION; this recognizer is not incremental.
     */
    public RecognitionSet strokeModified (TimedStroke s) {
        return RecognitionSet.NO_RECOGNITION;
    }
    
    /**
     * Return NO_RECOGNITION; this recognizer is not incremental.
     */
    public RecognitionSet strokeStarted (TimedStroke s) {
        return RecognitionSet.NO_RECOGNITION;
    }
    
}



