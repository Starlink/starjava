/*
 * $Id: AngleRelation.java,v 1.9 2001/07/22 22:01:49 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.parser2d;
import diva.sketch.recognition.SceneElement;
import java.awt.geom.Rectangle2D;

/**
 * Calculate the direction angle (between 0 and 2*PI) between
 * user-specified sites on a pair of rectangles.  The angle is given
 * relative to an "origin" rectangle, which is specified by the
 * <i>which</i> parameter of the relation.
 * 
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.9 $
 * @rating  Red
 */
public class AngleRelation implements Relation {
    /**
     * The name that this relation goes by in text form.
     */
    public static String NAME = "angle";
    
    /**
     * The site of interest on R1.
     */
    private int _site1;

    /**
     * The site of interest on R2.
     */
    private int _site2;
        
    /**
     * The sites of interest on the first and second rectangles,
     * respectively.  The <i>which</i> argument specifies which
     * rectangle (first or second) the angle is being measured
     * relative to.
     */
    public AngleRelation(int site1, int site2) {
        setSite1(site1);
        setSite2(site2);
    }
    
    /**
     * Return the angle between the constructor-specified
     * sites on e1 and e2, respectively. The answer is given
     * in degrees between 0 and 360.
     */
    public double apply (SceneElement e1, SceneElement e2) {
        Rectangle2D anchor = e1.getBounds();
        Rectangle2D satellite = e2.getBounds();
        double dx = RelationUtilities.siteX(satellite, _site2)-
            RelationUtilities.siteX(anchor, _site1);
        double dy = RelationUtilities.siteY(satellite, _site2)-
            RelationUtilities.siteY(anchor, _site1);
        double theta = Math.atan2(dy,dx);
        theta = (theta < 0) ? (2*Math.PI)+theta : theta;

//          debug("r1 = " + r1);
//          debug("r2 = " + r2);
//          debug("x1 = " + RelationUtilities.siteX(r1, _site1));
//          debug("y1 = " + RelationUtilities.siteY(r1, _site1));
//          debug("x2 = " + RelationUtilities.siteX(r2, _site2));
//          debug("y2 = " + RelationUtilities.siteY(r2, _site2));
//          debug("dx = " + dx);
//          debug("dy = " + dy);
//          debug("theta = " + Math.toDegrees(theta));

        //JDK1.1
        //          return Math.toDegrees(theta);
        return theta*180/Math.PI;
    }

    /**
     * Debugging output.
     */
    public static void debug (String s) {
        System.out.println(s);
    }

    /**
     * The name of this relation.
     */
    public String getName() {
        return NAME;
    }

    /**
     * Get the site for the first rectangle.
     */
    public int getSite1() {
        return _site1;
    }

    /**
     * Get the site for the second rectangle.
     */
    public int getSite2() {
        return _site2;
    }
    
    /**
     * Set the site for the first rectangle.
     */
    public void setSite1(int site1) {
        RelationUtilities.checkSite(site1);
        _site1 = site1;
    }

    /**
     * Set the site for the second rectangle.
     */
    public void setSite2(int site2) {
        RelationUtilities.checkSite(site2);
        _site2 = site2;
    }

    /**
     * Pretty print the relation in the grammar format.
     */
    public String toString(String e1Name, String e2Name) {
        return getName() + "(" + e1Name + "."
            + RelationUtilities.printSite(_site1)
            + ", " + e2Name + "."
            + RelationUtilities.printSite(_site2) + ")";
    }

    /**
     * Print out the contents of this relation.
     */
    public String toString() {
        String out = "AngleRelation[\n";
        out = out + "  Site1: " + RelationUtilities.printSite(_site1) + "\n";
        out = out + "  Site2: " + RelationUtilities.printSite(_site2) + "\n";
        out = out + "]";
        return out;
    }    
}






