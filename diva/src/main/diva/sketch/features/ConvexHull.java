/*
 * $Id: ConvexHull.java,v 1.10 2001/09/15 15:18:10 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
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
 * @version $Revision: 1.10 $
 * @rating Red
 */
public class ConvexHull {
    /**
     * A list storing the points that forms a convex hull.
     */
    private ArrayList _hull = null;    

    /**
     * Instantiate a ConvexHull object and call quickHull on the given
     * set of points.
     */
    public ConvexHull(double xvals[], double yvals[]){
        quickHull(xvals,yvals);
    }
        
    /**
     *  This method computes whether a point p, (px, py), is above the
     *  line formed by p1 and p2.  If the return value is positive, p
     *  is above the line.  If the return value is negative, p lies
     *  below the line.  If the return value is 0, p lies on the line,
     *  meaning p, px, py are colinear.  Note that this does not
     *  require p to be somewhere in between p1 and p2, (p1x<px<p2x is
     *  not necessarily true).  One should think of the line formed by
     *  p1 and p2 that extends to infinity on both end.<p>
     *
     *  The computation is simply (p1x-px)*(p2y-py)-(p1y-py)*(p2x-px)
     *  This is taking the cross product of two vectors (p1,p2) and
     *  (p1,p) where the z component is set to 0.  The cross product
     *  will yield a vector in pointing in the z direction, and the
     *  vector's x and y components are 0 (easy to verify using
     *  right-hand rule).  The return value is simply the z component
     *  of the resulting vector, the sign of which indicates up or
     *  down, and hence indicates where the point (p) lies with
     *  respect to the line (p1, p2).
     */
    private double crossProd(Point2D p, Point2D p1, Point2D p2){
        return (p1.getX()-p.getX())*(p2.getY()-p.getY())-(p1.getY()-p.getY())*(p2.getX()-p.getX());
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
     *  Return the perimeter of this convex hull.  Calculate the
     *  length of consecutive line segments including wrap around.
     *  ex. (p0,p1)(p1,p2)....(pLast-1, pLast)(pLast, p0)
     */
    public double getPerimeter(){
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
            //(pLast, p0), p1 is now the last point after the for loop
            p2 = (Point2D)_hull.get(0);
            plen += FEUtilities.distance(p1.getX(), p1.getY(),
                        p2.getX(), p2.getY());
        }
        return plen;
    }


    /**
     * Return an iterator over the points (Point2D) in the convex hull.
     */
    public Iterator points() {
        return _hull.iterator();
    }

    
    /**
     *  This method finds all the points on the hull between p1 and p2
     *  clockwise, inclusive of p1 but not of p2 (because p2 has been
     *  included in the other half of the recursive calls).<p>
     *
     *  Given two points on the convex hull, p1 and p2, hsplit first
     *  identifies all the points (candidates) that lie on one side of p1
     *  and p2 using crossProd.  From this set of points, it finds the
     *  farthest point (p) from the line formed by p1 and p2.  It is
     *  clearly evident that p must also lie on the convex hull.  Then
     *  it recursively calls hsplit on (candidates, p1, p) and (candidates, p,
     *  p2).  The base case is when candidates is empty (p is not found),
     *  then just return an ArrayList containing p1.  Or when candidates
     *  has only one element (only one point lies on the positive side
     *  of p1 and p2), then return an ArrayList containing p and p1.
     */
    private ArrayList hsplit(ArrayList points, Point2D p1, Point2D p2){
        ArrayList candidates = new ArrayList(points.size());
        for(Iterator e = points.iterator(); e.hasNext(); ){
            Point2D p = (Point2D)e.next();
            double result = crossProd(p, p1, p2);
            if(result > 0){
                candidates.add(p);
            }
        }
        candidates.trimToSize();
        if(candidates.size()<2){
            candidates.add(0, p1);
            return candidates;
        }
        else{
            int maxIndex = 0;
            double maxCross = crossProd((Point2D)candidates.get(maxIndex), p1, p2);
            for(int i=1; i< candidates.size(); i++){
                Point2D p = (Point2D)candidates.get(i);
                double result = crossProd(p, p1, p2);
                if(result > maxCross){
                    maxIndex = i;
                    maxCross = result;
                }
            }
            Point2D pm = (Point2D)candidates.get(maxIndex);
            candidates.remove(maxIndex);
            ArrayList upper = hsplit(candidates, p1, pm);
            ArrayList lower = hsplit(candidates, pm, p2);
            for(Iterator e = lower.iterator(); e.hasNext();){//combine
                upper.add(e.next());
            }
            return upper;
        }
    }

    
    /**
     *  Quickhull algorithm is similar to Quicksort algorithm.  It
     *  chooses a pivot, split the data based on the pivot, and
     *  recurse on each of the split sets.  <p> In the algorithm, we
     *  first pick two points, the left most (min x) and the right
     *  most (max x).  These two points (obviously lie on the hull)
     *  form a split line separating the region into two.  Then we
     *  recursively split each region by finding the point that's the
     *  farthest from the split line.
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

        ArrayList points = new ArrayList(xpath.length-2);
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


