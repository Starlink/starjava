/*
 * $Id: NormalizeLengthStrokeFilter.java,v 1.3 2001/07/22 22:01:58 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import diva.sketch.recognition.TimedStroke;
import diva.sketch.features.PathLengthFE;
import diva.sketch.features.FEUtilities;

/**
 * Interpolate a timed stroke so that it contains
 * a specified number of points.
 *
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 * @rating Red
 */
public class NormalizeLengthStrokeFilter extends StrokeFilter {
    /** The number of points that the filter interpolates to.
     */
    private int _numPoints;

    /** Interpolate to the given number of points.
     */
    public NormalizeLengthStrokeFilter(int numPoints) {
        _numPoints = numPoints;
    }

    /** Return the number of points that the filter interpolates to.
     */
    public int getPointCount() {
        return _numPoints;
    }

    /** Set the number of points that the filter interpolates to.
     */
    public void setPointCount(int numPoints) {
        _numPoints = numPoints;
    }
    
    /**
     * Interpolate a timed stroke so that it contains a certain number
     * of points (specified through the constructor).
     */
    public TimedStroke apply(TimedStroke s){
        return interpolate(s, _numPoints);
    }

    /**
     * Interpolate the given stroke to make it the given number of
     * points.
     */
    public static TimedStroke interpolate(TimedStroke s, int numPoints) {
        double pathLength = PathLengthFE.pathLength(s);
        double segmentLength = pathLength/(numPoints-1);
        TimedStroke newStroke = new TimedStroke(numPoints);
        double curX = s.getX(0);
        double curY = s.getY(0);
        newStroke.addVertex((float)curX,(float)curY,s.getTimestamp(0));
        double nextX, nextY, prevDist;
        prevDist = 0;
        int index=1;
        boolean flag = false;
        while((index < s.getVertexCount())&&
                (newStroke.getVertexCount()<numPoints)){
            nextX = s.getX(index);
            nextY = s.getY(index);
            double dist = 0;
            if(!flag){
                dist = FEUtilities.distance(curX, curY, nextX, nextY) +
                    prevDist;
            }
            else {
                flag = false;
                //this is when we've advanced the "next" pointer
                double startX = s.getX(index-1);
                double startY = s.getY(index-1);
                dist = FEUtilities.distance(startX, startY, nextX, nextY) +
                    prevDist;
            }
            if(dist==segmentLength){
                float x = (float)nextX;
                float y = (float)nextY;
                newStroke.addVertex(x, y, s.getTimestamp(index));
                curX = nextX;
                curY = nextY;
                prevDist = 0;
                index++;
            }
            else if(dist > segmentLength){//interpolate point
                double ratio = segmentLength/dist;
                float x = (float)(curX + (nextX-curX)*ratio);
                float y = (float)(curY + (nextY-curY)*ratio);
                newStroke.addVertex(x, y, s.getTimestamp(index));
                curX = x;
                curY = y;
                prevDist = 0;                
            }
            else{//advance "next" pointer
                index++;
                prevDist = dist;
                flag = true;
                continue;
            }
        }
        double p = PathLengthFE.pathLength(newStroke);
        if(newStroke.getVertexCount() == (numPoints-1)){
            float x = (float)s.getX(s.getVertexCount()-1);
            float y = (float)s.getY(s.getVertexCount()-1);
            long t = s.getTimestamp(s.getVertexCount()-1);
            newStroke.addVertex(x,y,t);
        }
        else if(newStroke.getVertexCount() <= (numPoints-2)){
            System.out.println("Append extra points!");
            //append segments at the end of the stroke, using the
            //last vector direction
            int numPointsToAdd = numPoints-newStroke.getVertexCount();
            int lastIndex = newStroke.getVertexCount()-1;
            float prevX = (float)newStroke.getX(lastIndex);
            float prevY = (float)newStroke.getY(lastIndex);
            float dx = prevX - (float)newStroke.getX(lastIndex-1);
            float dy = prevY - (float)newStroke.getY(lastIndex-1);
            float x, y;
            for(int i=0; i<numPointsToAdd; i++){
                x = prevX+dx;
                y = prevY+dy;
                newStroke.addVertex(x, y, 0);
                prevX = x;
                prevY = y;
            }
        }
        return newStroke;
    }
}

