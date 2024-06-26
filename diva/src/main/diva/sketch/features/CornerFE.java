/*
 * $Id: CornerFE.java,v 1.13 2002/01/19 19:33:48 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
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
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.13 $
 * @rating Red
 */
public class CornerFE implements FeatureExtractor {
    /**
     * The key to access the cached corner property.
     */
    public static final String PROPERTY_KEY = "Corners";
    
    /**
     * The default threshold ratio of a segment length to the entire
     * path length of the stroke.
     */
    public static final double DEFAULT_THRESH_MAG_RATIO = 0.2;

    /**
     * The default dot product value for determining whether two
     * vectors form a corner.  If the dot product of two vectors is
     * less than this value, there is definitely a corner.
     */
    public static final double DEFAULT_THRESH_DOT = 0.3;

    /**
     * If the dot product of two vectors is greater than
     * DEFAULT_THRESH_DOT but less than this parameter
     * (DEFAULT_THRESH_RELAXED_DOT), we identify this to be a likely
     * corner, and more checkings are performed on the two vectors to
     * verify.  See numCorners for more details.
     */
    public static final double DEFAULT_THRESH_RELAXED_DOT = 0.6;
    
    /**
     * The default threshold ratio of a segment length to the entire
     * path length of the stroke.
     */
    private double _threshMagRatio;

    /**
     * The dot product value for determining whether two vectors form
     * a corner.  If the dot product of two vectors is less than this
     * value, there is definitely a corner.
     */
    private double _threshDot;

    /**
     * If the dot product of two vectors is greater than _threshDot
     * but less than this parameter (_threshRelaxedDot), we identify
     * this to be a likely corner, and more checkings are performed on
     * the two vectors to verify.  See numCorners for more details.
     */
    private double _threshRelaxedDot;

    /**
     * Create a CornerFE with default parameters.  The default value
     * for vector dot product is 0.3, the relaxed dot product is 0.6,
     * and the ratio of magnitude is 0.2 (20%).  See numCorners for
     * more detail on how these parameters are being used.
     */
    public CornerFE() {
        _threshMagRatio = DEFAULT_THRESH_MAG_RATIO;
        _threshDot = DEFAULT_THRESH_DOT;
        _threshRelaxedDot = DEFAULT_THRESH_RELAXED_DOT;
    }

    /**
     * Create a CornerFE with the specified parameters.
     */
    public CornerFE(double magThresh, double dotThresh, double dotRelaxThresh) {
        _threshDot = dotThresh;
        _threshMagRatio = magThresh;
        _threshRelaxedDot = dotRelaxThresh;
    }

    /**
     * Return the number of corners in the specified stroke.  This
     * calls numCorners with threshold values either initialized in
     * the constructor or specified by the user. <p>
     *
     * First check to see if this feature already exists in the
     * stroke's property table (access using PROPERTY_KEY).  If so,
     * return that value (cast to double).  Otherwise call numCorners
     * to compute and cache the result in the table. <p>
     */
    public double apply(TimedStroke s) {
        Integer num = (Integer)s.getProperty(PROPERTY_KEY);
        if(num == null){
            int val = numCorners(s,_threshDot,_threshMagRatio,_threshRelaxedDot);
            s.setProperty(PROPERTY_KEY, Integer.valueOf(val));
            return (double)val;
        }
        else {
            return num.doubleValue();
        }
    }


    /**
     * Return the indices where the corners occur in the given stroke.
     * Return null if no corners exist in the stroke.  This methods
     * makes a call to cornerIndices with default threshold values.
     */
    public static final int[] cornerIndices(TimedStroke s){
        return cornerIndices(s, DEFAULT_THRESH_MAG_RATIO,
                DEFAULT_THRESH_DOT, DEFAULT_THRESH_RELAXED_DOT);
    }

    
    /**
     * Return the indices where the corners occur in the given stroke.
     * Return null if no corners exist in the stroke.  Iterate over
     * the points in the stroke, for every three points (p1, p2, p3)
     * we form two vectors, (p1,p2) and (p2, p3).  If the dot product
     * of these two vectors is less than threshDot, then there is
     * definitely an angle at p2.  If the dot product is greater than
     * threshDot but less than threshRelaxedDot, it is likely that
     * there is an angle at p2, but we're not sure.  In this case, the
     * magnitudes of both vectors are calculated, m1 and m2.  Let
     * 'plen' be the stroke path length, we calculate r1 = m1/plen and
     * r2 = m2/plen.  If either r1 or r2 is greater than
     * threshMagRatio, then we say that there is a corner.  If both
     * vectors are really short (r1<threshMagRatio &&
     * r2<threshMagRatio), then they are not significant enough to be
     * used to determine a corner.  They are more likely to be noise
     * due to the pen/tablet hardware.
     */
    public static final int[] cornerIndices(TimedStroke s,
            double threshMagRatio, double threshDot,
            double threshRelaxedDot) {
        if(s == null) {
            return null;
        }
        else if(s.getVertexCount()<3){
            return null;
        }
        else {
            int numPoints = s.getVertexCount();
            ArrayList cornerIndices = new ArrayList();
            double pathLength = PathLengthFE.pathLength(s);
            double x1, y1, x2, y2, x3, y3;
            for(int i=0; i<numPoints-2; i++){
                x1 = s.getX(i);
                y1 = s.getY(i);
                x2 = s.getX(i+1);
                y2 = s.getY(i+1);
                x3 = s.getX(i+2);
                y3 = s.getY(i+2);
                double dot = FEUtilities.dotProduct(x1,y1,x2,y2,x3,y3);
                
                if(dot < threshDot) {//definitely an angle
                    double angle = Math.acos(dot);
                    angle = Math.toDegrees(angle);
                    int j = i+1;
                    cornerIndices.add(Integer.valueOf(j));
                    //                        numCorners++;
                }
                else if(dot < threshRelaxedDot) {
                    //likely to be an angle
                    double l1 = FEUtilities.distance(x1, y1, x2, y2);
                    double l2 = FEUtilities.distance(x2, y2, x3, y3);
                    l1 = l1/pathLength;
                    l2 = l2/pathLength;
                    if((l1 > threshMagRatio)||(l2 > threshMagRatio)){
                        double angle = Math.acos(dot);
                        angle = Math.toDegrees(angle);
                        //                            numCorners++;
                        int j = i+1;
                        cornerIndices.add(Integer.valueOf(j));
                    }
                }
            }
            if(cornerIndices.size()==0){
                return null;
            }
            else {
                int arr[] = new int[cornerIndices.size()];
                //System.out.println("Corner indices:-----");
                int k=0;
                for(Iterator i = cornerIndices.iterator(); i.hasNext();){
                    Integer index = (Integer)i.next();
                    int val = index.intValue();
                    arr[k++]=val;
                    //System.out.println(val+":("+s.getX(val)+", "+s.getY(val)+")");
                }
                //System.out.println("--------------------");                
                return arr;
            }
        }
    }

    
    /**
     * Return the name of this feature extractor.
     */
    public String getName() {
        return "Corners";
    }

    
    /**
     * Return the number of corners in the given stroke.  This method
     * calls numCorners with default threshold values.
     */
    public static final int numCorners(TimedStroke s) {
        return numCorners(s, DEFAULT_THRESH_MAG_RATIO,
                DEFAULT_THRESH_DOT, DEFAULT_THRESH_RELAXED_DOT);
    }

    /**
     * Return the number of corners in the given stroke.  This method
     * calls cornerIndices.
     */
    public static final int numCorners(TimedStroke s,
            double threshMagRatio, double threshDot,
            double threshRelaxedDot) {
        int indices[] = cornerIndices(s,threshMagRatio,threshDot,threshRelaxedDot);
        if(indices==null){
            return 0;
        }
        else{
            return indices.length;
        }
    }
    

    /**
     * Set the dot product threshold for determining whether two
     * vectors form a corner.  If the dot product of two vectors is
     * less than this value, there is definitely a corner.
     */
    public void setDotThreshold(double val){
        _threshDot = val;
    }


    /**
     * Set the magnitude ratio threshold.  The default threshold ratio
     * of a segment length to the entire path length of the stroke.
     */
    public void setMagRatioThreshold(double val){
        _threshMagRatio = val;
    }

    
    /**
     * If the dot product of two vectors is greater than _threshDot
     * but less than this parameter (_threshRelaxedDot), we identify
     * this to be a likely corner, and more checkings are performed on
     * the two vectors to verify.  See numCorners for more details.
     */
    public void setRelaxedDotThreshold(double val){
        _threshRelaxedDot = val;
    }
}


