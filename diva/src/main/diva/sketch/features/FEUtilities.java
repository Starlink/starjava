/*
 * $Id: FEUtilities.java,v 1.7 2001/01/25 06:14:38 hwawen Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.features;
import java.awt.geom.Point2D;

/**
 * Common calculation methods used in path filtering and feature
 * extraction processes.
 *
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.7 $
 */

public class FEUtilities {

    /**
     * Return the Euclidean distance between
     * two points, (x1, y1) and (x2, y2).
     */
    public static double distance(double x1, double y1, double x2, double y2){
        return Math.sqrt(Math.pow(x2-x1, 2)+Math.pow(y2-y1, 2));
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
    
}

