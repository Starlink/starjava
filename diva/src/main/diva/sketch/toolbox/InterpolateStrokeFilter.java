/*
 * $Id: InterpolateStrokeFilter.java,v 1.1 2002/07/14 00:05:02 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import diva.sketch.recognition.TimedStroke;

/**
 * For every 2 consecutive points in a stroke, fill in with evenly
 * spaced points until the distance in between any 2 consecutive
 * points no longer exceeds the threshold distance.
 *
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 * @rating Red
 */
public class InterpolateStrokeFilter extends StrokeFilter {
    public static final double DEFAULT_SPACING = 20.0;

    private double _spacing;

    /**
     * Create a InterpolateStrokeFilter with the default parameters.
     */
    public InterpolateStrokeFilter(){
        _spacing = DEFAULT_SPACING;
        
    }    

    /**
     * Interpolate the given stroke such that no 2 consecutive points
     * in the stroke has a distance greater than the specified
     * spacing.
     */
    public TimedStroke apply(TimedStroke s){
        return interpolate(s,_spacing);
    }

    /**
     * Interpolate the points in the given stroke using the default
     * spacing value (20.0).
     */
    public static TimedStroke interpolate(TimedStroke s){
        return interpolate(s,DEFAULT_SPACING);
    }

    /**
     * Interpolate the points in the given stroke using the specified
     * spacing value.
     */
    public static TimedStroke interpolate(TimedStroke s, double spacing) {
        //interpolate in between points
        TimedStroke result = new TimedStroke();
        int n=s.getVertexCount();
        result.addVertex((float)s.getX(0),(float)s.getY(0),s.getTimestamp(0));
        double thresh = Math.pow(spacing,2);
        for(int i=1; i<n; i++){
            double pts[][]=half(s.getX(i-1),s.getY(i-1),s.getTimestamp(i-1),s.getX(i),s.getY(i),s.getTimestamp(i),thresh);
            if(pts!=null){
                for(int j=0; j<pts.length; j++){
                    result.addVertex((float)pts[j][0],(float)pts[j][1],(long)pts[j][2]);
                }
            }
            result.addVertex((float)s.getX(i),(float)s.getY(i),s.getTimestamp(i));
        }
        return result;
    }

    /**
     * return a nx3 array interpolating the given points
     */
    private static double[][] half(double x1,double y1,double t1,double x2,double y2,double t2,double thresh){
        double mag = Math.pow(x1-x2,2)+Math.pow(y1-y2,2);
        if(mag>thresh){
            double xmid=(x2-x1)/2+x1;
            double ymid=(y2-y1)/2+y1;
            double tmid=(t2-t1)/2+t1;
            double[][] r1 = half(x1,y1,t1,xmid,ymid,tmid,thresh);
            double[][] r2 = half(xmid,ymid,tmid,x2,y2,t2,thresh);
            int n=1;
            int r1n=0;
            int r2n=0;            
            if(r1!=null){
                r1n=r1.length;
                n+=r1n;
            }
            if(r2!=null){
                r2n=r2.length;
                n+=r2n;
            }
            double result[][] = new double[n][3];
            int ct=0;
            for(int i=0; i<r1n; i++){
                result[ct][0]=r1[i][0];
                result[ct][1]=r1[i][1];
                result[ct][2]=r1[i][2];
                ct++;
            }
            result[ct][0]=xmid;
            result[ct][1]=ymid;
            result[ct][2]=tmid;
            ct++;
            for(int i=0; i<r2n; i++){
                result[ct][0]=r2[i][0];
                result[ct][1]=r2[i][1];
                result[ct][2]=r2[i][2];
                ct++;
            }
            return result;
        }
        else{
            return null;
        }
    }

    /**
     * Return the spacing parameter.
     */
    public double getSpacing(){ return _spacing; }

    /**
     * Set the spacing value for adding interpolated points.  Throw an
     * IllegalArgumentException if the input value is <= 0.
     */
    public void setSpacing(double val) {
        if(val <= 0){
            throw new IllegalArgumentException("Spacing must be positive");
        }
        _spacing = val;
    }

}
