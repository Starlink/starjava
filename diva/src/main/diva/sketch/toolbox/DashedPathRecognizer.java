/*
 * $Id: DashedPathRecognizer.java,v 1.3 2001/07/22 22:01:57 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;

import diva.sketch.recognition.SceneRecognizer;
import diva.sketch.recognition.SceneDeltaSet;
import diva.sketch.recognition.Scene;
import diva.sketch.recognition.SceneDelta;
import diva.sketch.recognition.SceneElement;
import diva.sketch.recognition.StrokeElement;
import diva.sketch.recognition.CompositeElement;
import diva.sketch.recognition.TimedStroke;
import diva.sketch.recognition.SimpleData;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This recognizer recognizes dashed paths from sessions of
 * individual strokes.  It first checks to make sure that
 * every stroke in the session is a straight line, then
 * checks to see whether the stroke lengths are equal across
 * strokes and whether endpoints of adjacent strokes are
 * close to equidistant across all the strokes.  If this check
 * passes, it makes the segments into a path, and then runs
 * a filter on the path to clean it up.  It also saves out
 * the average stroke length and the average distance between
 * strokes.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 * @rating Red
 */
public class DashedPathRecognizer implements SceneRecognizer {
    private double _distSqThresh;
    private double _angleThresh;
    private double _minConf;
    private SceneRecognizer _child;
    StrokeFilter _filter = new ApproximateStrokeFilter();
	
    /**
     * The default tolerance for the maximum distance
     * squared between endpoints of lines.
     */
    public static final double DEFAULT_DIST_THRESH = 10;
	
    /**
     * The default tolerance for the maximum angle
     * error over the sum of the edges of the polygon.
     */
    public static final double DEFAULT_ANGLE_THRESH = 90;
	
    /**
     * The default tolerance for the minimum allowable
     * line confidence per stroke.
     */
    public static final double DEFAULT_MIN_CONFIDENCE = .7;
    
    /**
     * Construct a dashed path recognizer with the given child
     * recognizer and the default threshold values
     * DEFAULT_DIST_THRESH, DEFAULT_ANGLE_THRESH, DEFAULT_MIN_CONFIDENCE.
     * 
     * @see DashedPathRecognizer(SceneRecognizer, double, double, double)
     */
    public DashedPathRecognizer(SceneRecognizer child) {
        this(child, DEFAULT_DIST_THRESH, DEFAULT_ANGLE_THRESH, 
                DEFAULT_MIN_CONFIDENCE);
    }
	
    /**
     * Construct a polygon recognizer with the given
     * child recognizer that transitively invokes
     * low-level recognition, as well as threshold
     * tolerances for the maximum distance
     * squared between endpoints, the maximum angle
     * error over the polygon, and the minimum "line"
     * confidence for each stroke in the polygon.
     */
    public DashedPathRecognizer(SceneRecognizer child, double distThresh,
            double angleThresh, double minConf) {
        _child = child;
        _distSqThresh = distThresh*distThresh;
        _angleThresh = angleThresh;
        _minConf = minConf;
    }
	
    public SceneDeltaSet strokeStarted(StrokeElement stroke, Scene db) {
        return SceneDeltaSet.NO_DELTA;
    }
	
    public SceneDeltaSet strokeModified(StrokeElement stroke, Scene db) {
        return SceneDeltaSet.NO_DELTA;
    }
	
    public SceneDeltaSet strokeCompleted(StrokeElement stroke, Scene db) {
        return SceneDeltaSet.NO_DELTA;
    }		
			
    /**
     * Test whether the given strokes make a dashed path.  If so,
     * add it to the scene database.
     */
    public SceneDeltaSet sessionCompleted(StrokeElement[] session, Scene db) {
        SceneDeltaSet out = _child.sessionCompleted(session, db);
        if(out == SceneDeltaSet.NO_DELTA /*FIXME?*/ || session.length < 3) {
            return out;
        }

        /* FIXME - skip this for now
        SceneElement[] lines = new SceneElement[session.length];
        for(int i = 0; i < session.length; i++) {
            boolean isLine = false;
            StrokeElement elt = session[i];
            for(Iterator parents = elt.parents().iterator();
                parents.hasNext(); ) {
                CompositeElement parent = (CompositeElement)parents.next();
                if(parent.getData().getType().getID().equalsIgnoreCase("line")
                        && parent.getConfidence() >= _minConf) {
                    lines[i] = parent;
                    isLine = true;
                    break;
                }
            }
            if(!isLine) {//can't be a dashed path
                return out;
            }
        }
        */

        //we have greater than three segments and they are all
        //lines... let's check them!

        double[] lens = new double[session.length];
        for(int i = 0;  i < session.length; i++) {
            TimedStroke s = session[i].getStroke();
            double dx = s.getX(0) - s.getX(s.getVertexCount()-1);
            double dy = s.getY(0) - s.getY(s.getVertexCount()-1);
            lens[i] = Math.sqrt(dx*dx+dy*dy);
        }

        double[] gaps = new double[session.length-1];
        TimedStroke prev = session[0].getStroke();
        for(int i = 1;  i < session.length; i++) {
            TimedStroke cur = session[i].getStroke();
            double dx = cur.getX(0) - prev.getX(prev.getVertexCount()-1);
            double dy = cur.getY(0) - prev.getY(prev.getVertexCount()-1);
            gaps[i-1] = Math.sqrt(dx*dx+dy*dy);
            prev = cur;
        }

        double gapMean = mean(gaps);
        double gapDev = stdDev(gaps, gapMean);
        double lenMean = mean(lens);
        double lenDev = stdDev(lens, lenMean);

        System.out.println("Stroke count: " + lens.length);
        System.out.println("Gaps: [" + gapMean + ", " + gapDev + "]");
        System.out.println("Lens: [" + lenMean + ", " + lenDev + "]");

        //FIXME - need some better heuristic here??
        if(gapMean > 5 && gapDev < 12 && lenDev < 20) {
            TimedStroke ts = new TimedStroke();
            for(int i = 0; i < session.length; i++) {
                TimedStroke s = session[i].getStroke();
                ts.addVertex((float)s.getX(0), (float)s.getY(0), s.getTimestamp(0));
                int lenMinus = s.getVertexCount()-1;
                ts.addVertex((float)s.getX(lenMinus),
                        (float)s.getY(lenMinus), s.getTimestamp(lenMinus));
            }
            TimedStroke filt = _filter.apply(ts);
            DashedPathData dpd = new DashedPathData(filt, lenMean, gapMean);
        }
        
        return out;
    }


    /**
     * Return the mean of an array of values.
     */
    private static final double mean(double[] vals) {
        double sum = 0;
        for(int i = 0; i < vals.length; i++) {
            sum += vals[i];
        }
        return sum/vals.length;
    }

    /**
     * Return the standard deviation of an array of values given
     * that their mean has already been calculated.
     */
    private static final double stdDev(double[] vals, double mean) {
        double sum = 0;
        for(int i = 0; i < vals.length; i++) {
            sum += ((vals[i]-mean)*(vals[i]-mean));
        }
        return Math.sqrt(sum/vals.length);
    }
}

