/*
 * $Id: TestUtilities.java,v 1.3 2001/07/22 22:02:09 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.util.jester;

import java.awt.BasicStroke;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;

/** Utility functions to make tests easier to write.
 *
 * @version $Revision: 1.3 $
 * @author  John Reekie (johnr@eecs.berkeley.edu)
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)

 */
public final class TestUtilities {

    /** Test if two shapes are equal. The epsilon is a number such
     * that if two points are different by less than epsilon in both
     * coordinates they are considered "equal". This method is
     * not very efficient but is particularly useful for testing
     * purposes.
     */
    public static boolean shapeEquals (Shape s1, Shape s2, double epsilon) {
        PathIterator p1 = s1.getPathIterator(new AffineTransform());
        PathIterator p2 = s2.getPathIterator(new AffineTransform());
        double c1[] = new double[6];
        double c2[] = new double[6];
        int t1, t2, i, n = 0;

        while (!p1.isDone() && !p2.isDone()) {
            t1 = p1.currentSegment(c1);
            t2 = p2.currentSegment(c2); 
            if (t1 != t2) {
                return false;
            }
            switch (t1) {
            case PathIterator.SEG_MOVETO:
            case PathIterator.SEG_LINETO:
                n = 2;
                break;
            case PathIterator.SEG_QUADTO:
                n = 4;
                break;
            case PathIterator.SEG_CUBICTO:
                n = 6;
                break;
            default:
                // presumably SEG_CLOSE
            }
            for (i = 0; i < n; i++) {
                if (Math.abs(c1[i]-c2[i]) > epsilon) {
                    return false;
                }
            }
            p1.next();
            p2.next();
        }
        // Make sure that both are the same length
        return (p1.isDone() && p2.isDone());
    } 
}


