/*
 * $Id: ApproximateStrokeFilter.java,v 1.12 2002/01/20 02:29:42 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import diva.sketch.recognition.TimedStroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.BitSet;

/**
 * An object which filters a pen stroke using "approximation by line
 * segments" technique, (Hanaki, Temma, Yoshida, "An On-line Character
 * Recognition Aimed at a Substitution for a Billing Machine
 * Keyboard", Pattern Recognition, Vol.8, pp63-71, 1976).
 *
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.12 $
 * @rating Red
 */
public class ApproximateStrokeFilter extends StrokeFilter {
    /**
     * If the farthest point from a line segment is less than the
     * DEFAULT_THRESH_DISTANCE, we can throw away all the in-between
     * points, because they are almost colinear.  If the distance is
     * greather than this value, we recursively filter the stroke by
     * breaking off at the farthest point.  The default value is set
     * to 2 units.
     */
    public static final double DEFAULT_THRESH_DISTANCE = 4.0;
    
    /**
     * If the farthest point from a line segment is less than the
     * DEFAULT_THRESH_DISTANCE, we can throw away all the in-between
     * points, because they are almost colinear.  If the distance is
     * greather than this value, we recursively filter the stroke by
     * breaking off at the farthest point.
     */
    private double _threshDistance;

    /**
     * Construct a ApproximateStrokeFilter with default threshold
     * distance  of 2 units.
     */
    public ApproximateStrokeFilter(){
        _threshDistance = DEFAULT_THRESH_DISTANCE;
    }

    
    /**
     * Reduce the number of points in the given pen stroke using the
     * "approximation by line segments" algorithm.  <p>
     *
     * Algorithm: Form a line with the first and last points of the
     * stroke.  Find a farthest point (P) from the line segment such
     * that distance between the line and P exceeds a threshold value.
     * Then, break the line segment into 2 and recursively apply
     * filtering on the 2 parts, (first, P) and (P, last).  The
     * threshold value is defaulted to DEFAULT_THRESH_DISTANCE.<p>
     *
     * @param s  the stroke to be filtered.
     */
    public TimedStroke apply(TimedStroke s) {
        return approximate(s, _threshDistance);
    }


    /**
     * Reduce the number of points in the given pen stroke using the
     * "approximation by line segments" algorithm.  <p>
     *
     * Algorithm: Form a line with the first and last points of the
     * stroke.  Find a farthest point (P) from the line segment such
     * that distance between the line and P exceeds a threshold value.
     * Then, break the line segment into 2 and recursively apply
     * filtering on the 2 parts, (first, P) and (P, last).  The
     * threshold value is defaulted to DEFAULT_THRESH_DISTANCE.<p>
     *
     * @param s  the stroke to be filtered.
     */
    public static TimedStroke approximate(TimedStroke s) {
        return approximate(s, DEFAULT_THRESH_DISTANCE);
    }

    
    /**
     * Reduce the number of points in the given pen stroke using the
     * "approximation by line segments" algorithm.  <p>
     *
     * Algorithm: Form a line with the first and last points of the
     * stroke.  Find a farthest point (P) from the line segment such
     * that distance between the line and P exceeds a threshold value
     * (threshDist).  Then, break the line segment into 2 and
     * recursively apply filtering on the 2 parts, (first, P) and (P,
     * last).  <p>
     */
    public static TimedStroke approximate(TimedStroke s, double threshDist) {
        TimedStroke fs = new TimedStroke();
        int numpoints = s.getVertexCount();
        if(numpoints > 2){
            double xpos[] = new double[numpoints];
            double ypos[] = new double[numpoints];
            long timestamps[] = new long[numpoints];
            for(int i=0; i<s.getVertexCount(); i++){
                xpos[i] = s.getX(i);
                ypos[i] = s.getY(i);
                timestamps[i] = s.getTimestamp(i);
            }
            BitSet onset = new BitSet(numpoints);
            onset.set(0);
            onset.set(numpoints-1);
            approximateRecurse(onset, xpos, ypos, 0, numpoints-1, threshDist);
            for(int i=0; i<onset.size(); i++){
                if(onset.get(i)){
                    fs.addVertex((float)xpos[i],(float)ypos[i],timestamps[i]);
                }
            }
        }
        else {
            for(int i=0; i<numpoints; i++){
                fs.addVertex((float)s.getX(i), (float)s.getY(i), s.getTimestamp(i));
            }
        }
        return fs;
    }

    
    /**
     * Recursively filter the sequence of points, xpath and ypath, by
     * first finding the farthest point (P) from the line segment
     * formed by 'first' and 'last' end points, then break the line
     * segment into 2, (first, P) and (P, last).  The way P is
     * determined is that it's the farthest point from line segment
     * (first, last) and the distance exceeds 'threshDist'.  If no
     * such point is found, then the recursion comes to an end.  The
     * 'onset' is a bit array that is used to mark the points that we
     * keep along a path.  It should contain the first and last points
     * of the path and all the P's that we've identified throughout
     * the path.
     *
     * @param onset  a bit set marking the points to keep
     * @param xpath  a sequence of x coordinates
     * @param ypath  a sequence of y coordinates
     * @param first  the index of the first point in the line segment
     * @param last   the index of the last point in the line segment
     * @param threshDist the distance that the farthest point from a
     * line segment has to exceed to be considered significent and
     * therefore kept.
     */
    private static void approximateRecurse(BitSet onset, double xpath[], double ypath[], int first, int last, double threshDist) {
        int numpoints = last-first+1;
        if(numpoints > 2){
            int maxIndex = -1;
            double maxDist = 0;
            //finding the farthest point from the line segment formed by
            //the first and last point in the sequence
            if((xpath[first]==xpath[last])&&(ypath[first]==ypath[last])){
                for(int i=first+1; i<last; i++){
                    //excluding first and last points
                    double d = Point2D.distance(xpath[first], ypath[first], xpath[i], ypath[i]);
                    if((d>threshDist)&&(d>maxDist)){
                        maxDist = d;
                        maxIndex = i;
                    }
                }
            }
            else {
                for(int i=first+1; i<last; i++){
                    //excluding first and last points
                    double d = Line2D.ptLineDist(xpath[first], ypath[first], xpath[last], ypath[last], xpath[i], ypath[i]);
                    if((d>threshDist)&&(d>maxDist)){
                        maxDist = d;
                        maxIndex = i;
                    }
                }
            }
            //there is a farthest point whose distance from the line
            //segment exceeds the threshDist.
            if(maxIndex > -1){
                //mark the index of the point to keep, P, then
                //recurse on the two segments, (first,P) and (P,
                //last).
                onset.set(maxIndex);
                approximateRecurse(onset,xpath,ypath,first,maxIndex,threshDist);
                approximateRecurse(onset,xpath,ypath,maxIndex,last, threshDist);
            }
        }
    }

    
    /**
     * Set the threshold distance of a point from a line segment.  If
     * 'val' is negative, throws an IllegalArgumentException.
     */
    public void setThreshDistance(int val){
        if(val < 0){
            throw new IllegalArgumentException("Threshold distance cannot be negative");
        }
        else{
            _threshDistance = val;
        }
    }
}

