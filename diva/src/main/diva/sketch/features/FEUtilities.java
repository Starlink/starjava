/*
 * $Id: FEUtilities.java,v 1.17 2002/07/18 00:12:25 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.features;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;

/**
 * Common calculation methods used in path filtering and feature
 * extraction processes.
 *
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.17 $
 */

public class FEUtilities {

    /**
     * Calculate the angle (in radians) in between the given vectors.
     */
    public static double calcAngle(Line2D line1, Line2D line2){
        if(line1.getX1()==line2.getX1()&&line1.getY1()==line2.getY1()&&
                line1.getX2()==line2.getX2()&&line1.getY2()==line2.getY2()){
            return 0;
        }
        double v1x = line1.getX2()-line1.getX1();
        double v1y = line1.getY2()-line1.getY1();
        double v2x = line2.getX2()-line2.getX1();
        double v2y = line2.getY2()-line2.getY1();
        double dot = v1x*v2x+v1y*v2y;
        double magV1 = Math.sqrt(v1x*v1x+v1y*v1y);
        double magV2 = Math.sqrt(v2x*v2x+v2y*v2y);
        double val = dot/(magV1*magV2);
        double theta = Math.acos(val);
        if(Double.isNaN(theta)&&((val>1)&&(val<1.0000001))){
            //argh! deal with floating point problem
            theta = 0;
        }
        return theta;
    }

    /**
     * Calculate the ratio of line1's length and line2's length.
     * (line1/line2);
     */
    public static double calcLengthRatio(Line2D line1, Line2D line2){
        double dist1 = FEUtilities.distance(line1.getX1(),line1.getY1(),line1.getX2(),line1.getY2());
        double dist2 = FEUtilities.distance(line2.getX1(),line2.getY1(),line2.getX2(),line2.getY2());
        return dist1/dist2;
    }

    /**
     * Given a 2D vector, find the angle (in radians) formed between
     * this vector and the horizontal vector pointing in the positive
     * x direction.  Return Double.NaN if the 2 points given are the
     * same points.
     */
    public static double computeAngle(double xhead, double yhead, double xtail, double ytail){
        double vlength = distance(xhead, yhead, xtail, ytail);
        if(vlength == 0){
            //            throw new RuntimeException("ERROR: Cannot compute angle because the two points are the same");
            return Double.NaN;
        }

        double angle = Math.acos((xtail-xhead)/vlength);
        if(ytail<yhead){
            angle = 2*Math.PI-angle;
        }
        return angle;
    }


    /**
     * Return the Euclidean distance between
     * two points, (x1, y1) and (x2, y2).
     */
    public static double distance(double x1, double y1, double x2, double y2){
        return Math.sqrt(Math.pow(x2-x1, 2)+Math.pow(y2-y1, 2));
    }

    
    /**
     * Return the square of the distance between
     * two points, (x1, y1) and (x2, y2).
     */
    public static double distanceSquared(double x1, double y1, double x2, double y2){
        return Math.pow(x2-x1, 2)+Math.pow(y2-y1, 2);
    }

    
    /**
     * Return the dot product of the vector (x1, y1)(x2, y2)
     * and the vector (x2, y2)(x3, y3).
     */
    public static double dotProduct(double x1, double y1, double x2, double y2, double x3, double y3){
        double nVx1 = x2-x1;
        double nVy1 = y2-y1;
        double nVx2 = x3-x2;
        double nVy2 = y3-y2;
        double nVMag1 = Math.sqrt(nVx1*nVx1 + nVy1*nVy1);
        double nVMag2 = Math.sqrt(nVx2*nVx2 + nVy2*nVy2);
        if((nVMag1==0) || (nVMag2 == 0)){
            return 1; // 0 angle
        }
        else {
            double dot = (nVx1/nVMag1)*(nVx2/nVMag2) + (nVy1/nVMag1)*(nVy2/nVMag2);
            return dot;
        }
    }

    /**
     * Return the dot product of the vector (p1xh, p1yh)(p1xt, p1yt)
     * and the vector (p2xh,p2yh)(p2xt, p2yt)
     * h = head, t = tail, tail-------->head
     */
    public static double dotProduct(double p1xh, double p1yh, double p1xt, double p1yt, double p2xh, double p2yh, double p2xt, double p2yt){
        double vec_fx = p1xh-p1xt;
        double vec_fy = p1yh-p1yt;
        double vec_bx = p2xh-p2xt;
        double vec_by = p2yh-p2yt;
        double vec_f_mag = Math.sqrt(Math.pow(vec_fx,2)+Math.pow(vec_fy,2));
        double vec_b_mag = Math.sqrt(Math.pow(vec_bx,2)+Math.pow(vec_by,2));
        double dot = (vec_fx*vec_bx+vec_fy*vec_by)/(vec_f_mag*vec_b_mag);
        return dot;
    }
            

    /**
     *  Given three points each represents the vertex of a triangle,
     *  return the area of this triangle.
     */
    public static double findArea(Point2D p1, Point2D p2, Point2D p3){
        if(!preprocess(p1, p2, p3)){
            return 0.0;
        }
        else {
            double p4x;
            double p4y;
            double m13 = findSlope(p1.getX(), p1.getY(),
                    p3.getX(), p3.getY());
            double m24 = 0;
            if(Double.isNaN(m13)){
                p4x = p2.getX();
                p4y = p1.getY();
            }
            else if(m13 == 0){
                p4x = p1.getX();
                p4y = p2.getY();
            }
            else {
                m24 = -1/m13;
                p4y = (p2.getX()- m24*p2.getY()-p1.getX()+m13*p1.getY())/
                    (m13-m24);
                p4x = p2.getX()-p2.getY()*m24+p4y*m24;
                if(findSlope(p2.getX(), p2.getY(), p4x, p4y) == m13){
                    // 3 points on the same line
                    return 0;
                }
            }
            double dist24 = Math.sqrt(Math.pow((p2.getX()-p4x),2)+
                    Math.pow((p2.getY()-p4y),2));
            double dist13 = Math.sqrt(Math.pow((p1.getX()-p3.getX()),2)+
                    Math.pow((p1.getY()-p3.getY()),2));
            double area = 0.5*dist13*dist24;
            return area;
        }
    }

    
    /**
     * Find the slope of the line formed by two points
     * (p1x, p1y) and (p2x, p2y).
     */
    public static double findSlope(double p1x, double p1y,
            double p2x, double p2y){
        double nominator = p2x-p1x;
        double denominator = p2y-p1y;
        if(denominator == 0){
            return Double.NaN;
        }
        else {
            return nominator/denominator;
        }
    }

    
    /**
     * Return false when two of the points are the same, because
     * the area would be 0.
     */
    private static boolean preprocess(Point2D pa, Point2D pb, Point2D pc){
        if((pa.getX() == pb.getX())&&(pa.getY() == pb.getY())){
            return false;
        }
        else if((pa.getX() == pc.getX())&&(pa.getY() == pc.getY())){
            return false;
        }
        else if((pb.getX() == pc.getX())&&(pb.getY() == pc.getY())){
            return false;
        }
        else {
            return true;
        }
    }

    /**
     * Return the total pathlength by summing over the distance of
     * consecutive points in the array.
     */
    public static double pathLength(double xvals[], double yvals[], int num){
        double len = 0;
        double v,dx,dy;
        for(int i=1; i<num; i++){
            dx=xvals[i]-xvals[i-1];
            dy=yvals[i]-yvals[i-1];            
            v=Math.sqrt(dx*dx+dy*dy);
            len += v;
        }
        return len;
    }

    /**
     * calculate the path length starting from the startIndex to the
     * endIndex (inclusive) of the points in the array.
     */
    public static double pathLength(double xvals[], double yvals[], int startIndex, int endIndex){
        double len = 0;
        for(int i=startIndex+1; i<=endIndex; i++){
            len += distance(xvals[i], yvals[i], xvals[i-1], yvals[i-1]);
        }
        return len;
    }
}


