/*
 * $Id: CornerFE.java,v 1.7 2000/05/10 18:54:53 hwawen Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.features;
import diva.sketch.recognition.TimedStroke;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * CornerFE computes the number of corners in a stroke by enumerating
 * over the data points in the stroke.  For every three points, it
 * computes the angle formed by two vectors.  If the dot product of
 * the two neighboring vectors is less than 0.6 (empirical value), a
 * cusp is detected.  Then the magnitudes of the two vectors are
 * calculated.  If both of the vector magnitudes divided by the total
 * path length are less than a threshold value (20%), then this is not
 * really a corner.  Otherwise it is likely to be a corner. <p>
 *
 *  @author Heloise Hse (hwawen@eecs.berkeley.edu)
 *  @version $Revision: 1.7 $
 */
public class CornerFE implements FeatureExtractor {

    /**
     * The threshold ratio of a segment to the entire path length of
     * the stroke.  If the ratio is smaller than this threshold
     * ratio, then the segment is too small for determining a corner.
     */
    private double _thresholdMagRatio = 0.2;

    /**
     * Threshold value for definite angles
     */
    private double _thresholdDotProduct = 0.3;

    /**
     * Threshold for likely angles
     */
    private double _thresholdRelaxedDotProduct = 0.6;

    /**
     * Create a corner feature extractor.  Default threshold values are
     * used.  The default value for vector dot product is 0.6, and
     * the default value for the ratio of magnitude is 0.2 (20%).
     */
    public CornerFE() { }

    /**
     * Create a corner feature extractor with the specified threshold
     * values used to determine possible corners.  If two vector's dot
     * product is less than dotThreshold, the two vectors form a cusp.
     * However, this may not necessarily be a corner, because if the
     * two vector are very short compare to the entire stroke path,
     * this may just be an irregularity in drawing.  Therefore, we
     * need to compute the ratio of each vector and the stroke path
     * length.  If both exceed magThreshold value, then we can confirm
     * that there is indeed a corner.
     */
    public CornerFE(double dotThreshold, double magThreshold) {
        _thresholdDotProduct = dotThreshold;
        _thresholdMagRatio = magThreshold;
    }

    /**
     * Return the number of corners in the specified stroke.  Iterate
     * over the points in the stroke, for every three points we form
     * two vectors.  From the dot product and the magnitude of the
     * vectors, we determine how many corners there are in the
     * stroke.
     */
    public double apply(TimedStroke s) {
        debug("=====");
        int numCorners = 0;
        
        // find corner points.
        if(s != null) {
            int numPoints = s.getVertexCount();
            
            if((numPoints == 1)||(numPoints == 2)){
                numCorners = 0;
            }
            else {
                ArrayList cornerIndices = new ArrayList();
                double pathLength = (new StrokePathLengthFE()).apply(s);
                double x1, y1, x2, y2, x3, y3;
                for(int i=0; i<numPoints-2; i++){
                    x1 = s.getX(i);
                    y1 = s.getY(i);
                    x2 = s.getX(i+1);
                    y2 = s.getY(i+1);
                    x3 = s.getX(i+2);
                    y3 = s.getY(i+2);
                    double dot = FEUtilities.dotProduct(x1,y1,x2,y2,x3,y3);
                    
                    if(dot < _thresholdDotProduct) {//definitely an angle
                        double angle = Math.acos(dot);
                        angle = radianToDegree(angle);
                        debug("definite angle = " + angle);
                        int j = i+1;
                        cornerIndices.add(new Integer(j));
                        numCorners++;
                    }
                    else if(dot < _thresholdRelaxedDotProduct) { //likely to be an angle
                        double l1 = FEUtilities.distance(x1, y1, x2, y2);
                        double l2 = FEUtilities.distance(x2, y2, x3, y3);
                        l1 = l1/pathLength;
                        l2 = l2/pathLength;
                        if((l1 > _thresholdMagRatio)||(l2 > _thresholdMagRatio)){
                            double angle = Math.acos(dot);
                            angle = radianToDegree(angle);
                            numCorners++;
                            int j = i+1;
                            debug("Unsure angle = " + angle);
                            debug(", [" + l1 + ", " + l2 + "]");
                            cornerIndices.add(new Integer(j));
                        }
                    }
                }
                s.setProperty(getName(), cornerIndices);
            }
        }
        debug("num corners = " + numCorners);
        return numCorners;
    }

    private void debug(String s) {
        //        System.err.println(s);
    }

    /*
    private boolean isCusp(int x1, int y1, int x2, int y2, int x3, int y3){
        int nVx1 = x2-x1;
        int nVy1 = y2-y1;
        int nVx2 = x3-x2;
        int nVy2 = y3-y2;
        double nVMag1 = Math.sqrt(nVx1*nVx1 + nVy1*nVy1);
        double nVMag2 = Math.sqrt(nVx2*nVx2 + nVy2*nVy2);
        if((nVMag1==0) || (nVMag2 == 0)){
            return false;
        }
        else {
            double dot = (nVx1/nVMag1)*(nVx2/nVMag2) + (nVy1/nVMag1)*(nVy2/nVMag2);
            if(dot < 0.6){
                return true;
            }
            else {
                return false;
            }
        }
    }
    */

    /**
     * Return the name of this feature extractor.
     */
    public String getName() {
        return "Corners";
    }

    /**
     * Return the degree of the specified radian angle.
     */
    private double radianToDegree(double angle) {
        return (angle/Math.PI * 180.0);
    }

    /**
     * Set the vector dot product threshold which is used to determine
     * whether a cusp is formed between the vectors.  If the dot
     * product between two vectors is less than the threshold, then
     * the vectors form a cusp.
     */
    public void setDotThreshold(double val){
        _thresholdDotProduct = val;
    }

    /**
     * Set the magnitude ratio threshold which is used to determine
     * whether a cusp is indeed a corner.  If we know that there is a
     * cusp between two vectors, if the ratio of the vector's
     * magnitude and the stroke path length for both vectors exceed
     * the threshold value, then there is a corner.
     */
    public void setMagRatioThreshold(double val){
        _thresholdMagRatio = val;
    }
}

