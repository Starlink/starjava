/*
 * $Id: LineRecognizer.java,v 1.4 2001/08/28 06:37:12 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;

import diva.sketch.recognition.SimpleData;
import diva.sketch.recognition.Recognition;
import diva.sketch.recognition.RecognitionSet;
import diva.sketch.recognition.StrokeRecognizer;
import diva.sketch.recognition.TimedStroke;

import java.awt.geom.Line2D;
import java.awt.geom.Line2D.Double;

/**
 * This recognizer recognizes lines from strokes.  It forms a line
 * with the first and last points in a stroke, and for each point on
 * the stroke, it calculates the distance of the point from the line
 * and computes the confidence value based on the maximum distance.
 *
 * @author Heloise Hse  (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 * @rating Red
 */
public class LineRecognizer implements StrokeRecognizer {
    /**
     * The type ID for lines.
     */
    public static final String LINE_TYPE_ID = "LINE";

    public static final double DEFAULT_DIST_THRESH = 5;

    private double _distThresh;
    
    public LineRecognizer(){
        _distThresh = DEFAULT_DIST_THRESH;
    }

    
    /**
     * Invoked when a stroke is completed. This occurs when the mouse
     * up event has been detected.  First, filter the input stroke.
     * If there's only 1 data point (lineCheck result is -1), this is
     * definitely not a line, confidence = 0.  If there are 2 data
     * points, create a SimpleData with LINE_TYPE_ID and confidence
     * value of 1.  If there are more than 2 points in the filtered
     * stroke, form a line with the first and the last points, then
     * for every point in between, calculate the distance between the
     * point and the line, record the maximum distance (maxDist)from
     * the line, then create a SimpleData with LINE_TYPE_ID and
     * confidence value of * (1-maxDist/100).
     */
    public RecognitionSet strokeCompleted(TimedStroke s) {
        TimedStroke fs = ApproximateStrokeFilter.approximate(s);
        int len = fs.getVertexCount();
        Recognition[] rs = new Recognition[1];
        if(len == 1){
            rs[0] = new Recognition(new SimpleData(LINE_TYPE_ID), 0);
        }
        else if(len == 2){
            rs[0] = new Recognition(new SimpleData(LINE_TYPE_ID), 1);
        }
        else {
            //Form a line with first and last points, check the in
            //between points, see how far off they are from the line
            Line2D line = new Line2D.Double(fs.getX(0),fs.getY(0),fs.getX(len-1),fs.getY(len-1));
            double maxDist = 0;
            for(int i=1; i<len; i++){
                double dist = line.ptLineDist(fs.getX(i),fs.getY(i));
                if(dist > maxDist){
                    maxDist = dist;
                }
            }
            double val = 1.0-(maxDist/100.0);
            rs[0] = new Recognition(new SimpleData(LINE_TYPE_ID), val);
        }
        return new RecognitionSet(rs);
    }

    /**
     * Invoked when a stroke has been modified, for example, points
     * have been added to the stroke.  It is probably safe to assume
     * that this will be called every time a point is added to a
     * stroke.
     */
    public RecognitionSet strokeModified(TimedStroke s) {
        return RecognitionSet.NO_RECOGNITION;
    }
	
    /**
     * Invoked when a stroke starts.  This occurs when the mouse down
     * event has been detected.
     */
    public RecognitionSet strokeStarted(TimedStroke s) {
        return RecognitionSet.NO_RECOGNITION;
    }
}

