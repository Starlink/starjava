/*
 * $Id: PolygonRecognizer.java,v 1.6 2000/08/04 01:24:02 michaels Exp $
 *
 * Copyright (c) 1998 The Regents of the University of California.
 * All rights reserved.  See the file COPYRIGHT for details.
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
 * This recognizer recognizes polygons from sessions of
 * individual strokes.  It first checks to make sure that
 * every stroke in the session is a straight line, then
 * checks to see whether the endpoints of the lines line
 * up, then checks the sum of the angles to see whether
 * the polygon is convex.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.6 $
 */
public class PolygonRecognizer implements SceneRecognizer {
    private double _distSqThresh;
    private double _angleThresh;
    private double _minConf;
    private SceneRecognizer _child;
	
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
     * Construct a polygon recognizer with the given child
     * recognizer and the default threshold values
     * DEFAULT_DIST_THRESH, DEFAULT_ANGLE_THRESH, DEFAULT_MIN_CONFIDENCE.
     * 
     * @see PolygonRecognizer(double, double)
     */
    public PolygonRecognizer(SceneRecognizer child) {
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
    public PolygonRecognizer(SceneRecognizer child, double distThresh,
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
     * Test whether the given strokes make a polygon.  If so,
     * add it to the scene database.
     */
    public SceneDeltaSet sessionCompleted(StrokeElement[] session, Scene db) {
        SceneDeltaSet out = _child.sessionCompleted(session, db);
        if(out == SceneDeltaSet.NO_DELTA /*FIXME?*/ || session.length < 3) {
            return out;
        }
		
        SceneElement[] lines = new SceneElement[session.length];
        for(int i = 0; i < session.length; i++) {
            boolean isLine = false;
            StrokeElement elt = session[i];
            for(Iterator parents = elt.parents().iterator();
                parents.hasNext(); ) {
                CompositeElement parent = (CompositeElement)parents.next();
	//if(t instanceof LineType && conf >= _minConf) {
                if(parent.getData().getType().getID().equalsIgnoreCase("line")
                        && parent.getConfidence() >= _minConf) {
                    lines[i] = parent;
                    isLine = true;
                    break;
                }
            }
            if(!isLine) {
				//can't be a polygon
                return out;
            }		
        }
		
        //x[0] is the tail of the 0'th line, x[1] is
        //the head of the 0'th line, etc.
        double xs[] = new double[2*session.length];
        double ys[] = new double[2*session.length];
        for(int i = 0; i < session.length; i++) {
            TimedStroke s = session[i].getStroke();
            xs[2*i] = s.getX(0);
            ys[2*i] = s.getX(0);
            int len = s.getVertexCount();
            xs[2*i+1] = s.getX(len-1);
            ys[2*i+1] = s.getY(len-1);
        }
		
        //for each coordinate, store the vertex index
        //that it belongs to (-1 means unassigned)
        int indices[] = new int[2*session.length];
        for(int i = 0; i < 2*session.length; i++) {
            indices[i] = -1;
        }
		
        int curIndex = 0;
        for(int i = 0; i < 2*(session.length-1); i++) {
            if(indices[i] != -1) {
                indices[i] = curIndex;
                int next = (i%2 == 0) ? i+2 : i+1;
                boolean found = false;
                for(int j = next; j < 2*session.length; j++) {
                    if(indices[j] != -1) {
                        double dx = xs[j] - xs[i];
                        double dy = ys[j] - ys[i];
                        double dsq = dx*dx + dy*dy;
                        if(dsq <= _distSqThresh) {
                            if(found) {
                                return out;
                            }
                            else {
                                found = true;
                                indices[j] = curIndex;
                            }
                        }
                    }
                }
                if(!found) {
                    return out;
                }
                curIndex++;
            }
        }
		
        //check to make sure the last line got properly
        //assigned also, since we didn't check that in the
        //loop
        if(indices[2*session.length-2] == -1 || 
                indices[2*session.length-1] == -1) {
            return out;
        }
		
        //ASSERT
        if(curIndex != session.length) {
            throw new RuntimeException("curIndex = " + curIndex);
        }
		
        //now sort the polygon vertices by starting
        //at the tail of the first segment and simply
        //connecting the dots.
        int sorted[][] = new int[session.length][2];
        curIndex = indices[0];
        int curHit = 0;
        sorted[0][0] = curIndex;
        for(int i = 1; i < session.length; i++) {
            //do this N-1 times, since I don't know
            //a better algorithm
			
            for(int j = 0; j < 2*session.length; j++) {
                if(j != curHit && indices[j] == curIndex) {
                    sorted[i-1][1] = j;
                    int adjacent = (j%2 == 0) ? j+1 : j-1;
                    curIndex = indices[adjacent];
                    curHit = adjacent;
                    sorted[i][0] = curIndex;
                    break;
                }
            }
        }
		
        //create an aggregate polygon, where each vertex
        //is the average of the endpoints of the lines that form
        //the vertex
        double[] pts = new double[2*session.length];
        for(int i = 0; i < session.length; i++) {
            pts[2*i] = (xs[sorted[i][0]] + xs[sorted[i][1]])/2;
            pts[2*i+1] = (ys[sorted[i][0]] + ys[sorted[i][1]])/2;			
        }
		
        //FIXME - now check the angles of the aggregate polygon
        // ...

        //finally, if everything has passed, add the polygon to
        //the scene
        if(out == SceneDeltaSet.NO_DELTA) {
            out = new SceneDeltaSet();
        }
        //FIXME - calculate a better confidence
        //SceneElement elt = db.addComposite(new PolygonType(pts), 1 , session);
        String[] names = new String[session.length];
        for(int i = 0; i < session.length; i++) {
            names[i] = "stroke";
        }
        CompositeElement elt =
            db.addComposite(new SimpleData("polygon"), 1 , session, names);
        SceneDelta d = new SceneDelta.Subtractive(db, elt, session);
        out.addDelta(d);
        return out;
    }	
}
