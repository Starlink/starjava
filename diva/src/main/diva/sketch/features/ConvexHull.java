/*
 * $Id: ConvexHull.java,v 1.5 2000/05/02 00:45:09 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */

package diva.sketch.features;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

/**
 * A ConvexHull object implements the Quickhull algorithm to find the
 * planar convex hull of a set of points.
 * <p>
 *
 * Given a set of points, a convex hull is the smallest convex region
 * that contains all the points.
 * <p>
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.5 $
 */
public class ConvexHull {
    /**
     * A list storing the points that forms a convex hull.
     */
    private ArrayList _hull = null;    

    /**
     *  Find the distance of a point, p, from a line formed by p1 and p2.
     */
    private double crossProd(Point2D p, Point2D p1, Point2D p2){
        return ((p1.getX()-p.getX())*(p2.getY()-p.getY())-
                (p1.getY()-p.getY())*(p2.getX()-p.getX()));
    }
    
    /**
     *  Return the area of the convex hull.
     */
    public double getArea() {
        double area = 0;
        if(_hull.size()>2){
            Point2D p1 = (Point2D)_hull.get(0);
            Point2D p2, p3;
            for(int i=1; i<_hull.size()-1; i++){
                p2 = (Point2D)_hull.get(i);
                p3 = (Point2D)_hull.get(i+1);
                double subarea = FEUtilities.findArea(p1, p2, p3);
                area += subarea;
            }
        }
        return area;
    }

    /**
     * Return an iterator over the points in the convex hull.
     */
    public Iterator getConvexHullPoints() {
        return _hull.iterator();
    }

    /**
     *  Return the path length of this convex hull from the
     *  first point to the last point.
     */
    public double getPathLength(){
        double plen = 0;
        if(_hull.size()>1){
            Point2D p1 = (Point2D)_hull.get(0);
            Point2D p2;
            for(int i = 1; i< _hull.size(); i++){
                p2 = (Point2D)_hull.get(i);
                plen += FEUtilities.distance(p1.getX(), p1.getY(),
                        p2.getX(), p2.getY());
                p1 = p2;
            }
        }
        return plen;
    }

     /**
     *  Given two points on the convex hull, p1 and p2, hsplit finds
     *  all the points on the hull between p1 and p2 clockwise, inclusive
     *  of p1 but not of p2.
     */
    private ArrayList hsplit(ArrayList points, Point2D p1, Point2D p2){
        ArrayList packed = new ArrayList();
        for(Iterator e = points.iterator(); e.hasNext(); ){
            Point2D p = (Point2D)e.next();
            double result = crossProd(p, p1, p2);
            if(result > 0){
                packed.add(p);
            }
        }
        if(packed.size()<2){
            packed.add(0, p1);
            return packed;
        }
        else{
            int maxIndex = 0;
            double maxCross = crossProd((Point2D)packed.get(maxIndex), p1, p2);
            for(int i=1; i< packed.size(); i++){
                Point2D p = (Point2D)packed.get(i);
                double result = crossProd(p, p1, p2);
                if(result > maxCross){
                    maxIndex = i;
                    maxCross = result;
                }
            }
            Point2D pm = (Point2D)packed.get(maxIndex);
            packed.remove(maxIndex);
            ArrayList upper = hsplit(packed, p1, pm);
            ArrayList lower = hsplit(packed, pm, p2);
            for(Iterator e = lower.iterator(); e.hasNext();){
                upper.add(e.next());
            }
            return upper;
        }
    }

    /**
     * Return the number of points in the convex hull.
     public int numPoints(){
        return _hull.size();
    }
    */
    
    /**
     *  Quickhull algorithm is similar to Quicksort algorithm.  It chooses
     *  a pivot,  split the data based on the pivot, and recurse on each of
     *  the split sets.
     *  <p>
     *  In the algorithm, we first pick two points, the most left one and
     *  the most right one.  These two points (they must be on the hull)
     *  form a split line separating the region into two.  Then we
     *  recursively split each region by finding the point that's the
     *  farthest form the split line.
     */
    public void quickHull(double xpath[], double ypath[]) {
      int maxAt = 0;
      int minAt = 0;

      // find the max and min x-coord point
      for(int i=1; i<xpath.length; i++){
        if(xpath[i]>xpath[maxAt]){
            maxAt = i;
        }
        if(xpath[i]<xpath[minAt]){
            minAt = i;
        }
      }

      ArrayList points = new ArrayList();
      for(int i=0; i<xpath.length; i++){
        if((i!=maxAt)&&(i!=minAt)){
            points.add(new Point2D.Double(xpath[i], ypath[i]));
        }
      }
      Point2D p1 = new Point2D.Double(xpath[minAt], ypath[minAt]);
      Point2D p2 = new Point2D.Double(xpath[maxAt], ypath[maxAt]);
      ArrayList upper = hsplit(points, p1, p2);
      ArrayList lower = hsplit(points, p2, p1);
      for(Iterator e = lower.iterator(); e.hasNext();){
        upper.add(e.next());
      }
      _hull = upper;
    }
  
  }

