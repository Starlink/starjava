/*
 * $Id: HeightRatioRelation.java,v 1.6 2000/08/12 10:59:43 michaels Exp $
 *
 * Copyright (c) 1998 The Regents of the University of California.
 * All rights reserved.  See the file COPYRIGHT for details.
 */
package diva.sketch.parser2d;
import diva.sketch.recognition.SceneElement;
import java.awt.geom.Rectangle2D;

/**
 * Calculate the ratio of the height of the first input
 * scene element to the height of the second.
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.6 $
 * @rating  Red
  */
public class HeightRatioRelation implements Relation {
    /**
     * The name that this relation goes by in text form.
     */
    public static String NAME = "heightRatio";
    
    /**
     * Calculate the ratio of the height of the first element to the
     * height of the second.
     */
    public double apply (SceneElement e1, SceneElement e2) {
        Rectangle2D num = e1.getBounds();
        Rectangle2D denom = e2.getBounds();
        return num.getHeight() / denom.getHeight();
    }

    /**
     * The name of this relation.
     */
    public String getName() {
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
        String out = "HeightRelation[]\n";
        return out;
    }    
}





