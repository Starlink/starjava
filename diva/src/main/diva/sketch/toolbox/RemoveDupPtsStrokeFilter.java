/*
 * $Id: RemoveDupPtsStrokeFilter.java,v 1.2 2001/07/22 22:01:58 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import diva.sketch.recognition.TimedStroke;

/**
 * Remove all duplicate adjacent points from a
 * timed stroke.  Useful for algorithms that are
 * sensitive to redundant points of this type.
 *
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 * @rating Red
 */
public class RemoveDupPtsStrokeFilter extends StrokeFilter {
    /**
     * Remove all duplicate adjacent points from a
     * timed stroke.
     */
    public TimedStroke apply(TimedStroke s){
        return removeDupPts(s);
    }

    /**
     * Remove all duplicate adjacent points from a
     * timed stroke.
     */
    public static TimedStroke removeDupPts(TimedStroke s) {
        TimedStroke filteredStroke = new TimedStroke();
        float prevX = (float)s.getX(0);
        float prevY = (float)s.getY(0);
        float x, y;
        filteredStroke.addVertex(prevX, prevY, s.getTimestamp(0));
        for(int i=1; i< s.getVertexCount(); i++){
            x = (float)s.getX(i);
            y = (float)s.getY(i);
            if((x != prevX) || (y != prevY)){
                filteredStroke.addVertex(x, y, s.getTimestamp(i));
                prevX = x;
                prevY = y;
            }
        }
        return filteredStroke;
    }
}

