/*
 * $Id: OverlapRelation.java,v 1.10 2001/07/22 22:01:50 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.parser2d;
import diva.sketch.recognition.SceneElement;
import java.awt.geom.Rectangle2D;

/**
 * Calculate the percentage of one of the rectangles
 * that overlaps the other.  Divide the area of the
 * intersection by the area of the "which" rectangle
 * specified in the constructor.
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.10 $
 * @rating  Red
 */
public class OverlapRelation implements Relation {
    /**
     * The name that this relation goes by in text form.
     */
    public static String NAME = "overlap";
    
    /**
     * Return the ratio of the area of the intersection
     * of the given elements to the area of the first
     * rectangle.
     */
    public double apply (SceneElement e1, SceneElement e2) {
        Rectangle2D r1 = e1.getBounds();
        Rectangle2D r2 = e2.getBounds();
        if(r1.intersects(r2)) {
            Rectangle2D intersect = r1.createIntersection(r2);
            return (intersect.getWidth()*intersect.getHeight())/
                (r1.getWidth()*r1.getHeight());
        }
        return 0;
    }

    /**
     * The name of this relation.
     */
    public String getName () {
        return NAME;
    }


    /**
     * Pretty print the relation in the grammar format.
     */
    public String toString(String e1Name, String e2Name) {
        return getName() + "(" + e1Name + ", " + e2Name + ")";
    }

    /**
     * Print out the contents of this relation.
     */
    public String toString() {
        String out = "OverlapRelation[]\n";
        return out;
    }    
}





