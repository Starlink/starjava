/*
 * $Id: BasicConstraint.java,v 1.8 2001/07/22 22:01:50 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.parser2d;
import diva.sketch.recognition.SceneElement;
import java.awt.geom.Rectangle2D;

/**
 * An object that returns whether a pair of rectangles satisfies
 * constraints on the values of a specified relation.  Constraints are
 * specified as a range from minimum to maximum value, inclusive.
 * Constants NO_MIN_CONSTRAINT and NO_MAX_CONSTRAINT are used to
 * specify an open range in either or both directions.
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.8 $
 * @rating  Red
 */
public class BasicConstraint implements RelationConstraint {
    /**
     * Use this constant to specify no maximum constraint
     * for a particular relation.
     */
    public static double NO_MAX_CONSTRAINT = Double.POSITIVE_INFINITY;

    /**
     * Use this to constant specify no minimum constraint
     * for a particular relation.
     */
    public static double NO_MIN_CONSTRAINT = Double.NEGATIVE_INFINITY;
    
    /**
     * The relation that is applied.
     */
    private Relation _relation;

    /**
     * Constraint on the minimum value for the relation.
     */
    private double _minVal;
    
    /**
     * Constraint on the maximum value for the relation.
     */
    private double _maxVal;

    /**
     * Create the constraints object with the given relations and
     * given min/max constraints.  The relations will be applied
     * sequentially in the order given, so put the cheaper ones
     * or the more discriminatory ones first in the input.
     */
    public BasicConstraint (Relation r, double minVal, double maxVal) {
        _relation = r;
        _minVal = minVal;
        _maxVal = maxVal;
    }

    /**
     * Debugging output.
     */
    public static void debug (String s) {
        System.out.println(s);
    }

    /**
     * Parse a number string, NO_MIN, or NO_MAX into the corresponding
     * range boundary.
     */
    public static double parseMinMax(String s) {
        if(s.equalsIgnoreCase("NO_MIN") ||
                s.equalsIgnoreCase("NO_MIN_CONSTRAINT")) {
            return NO_MIN_CONSTRAINT;
        }
        if(s.equalsIgnoreCase("NO_MAX") ||
                s.equalsIgnoreCase("NO_MAX_CONSTRAINT")) {
            return NO_MAX_CONSTRAINT;
        }
        else {
            return Double.valueOf(s).doubleValue();
        }
    }

    /**
     * Parse a number string, NO_MIN, or NO_MAX into the corresponding
     * range boundary.
     */
    public static String printMinMax(double d) {
        if(d == NO_MIN_CONSTRAINT) {
            return "NO_MIN";
        }
        else if(d == NO_MAX_CONSTRAINT) {
            return "NO_MAX";
        }
        else {
            return Double.toString(d);
        }
    }

    /**
     * Test the constraint on the given elements; return true if
     * all constraints are met, false otherwise.
     */
    public boolean test (SceneElement e1, SceneElement e2) {
        double val = _relation.apply(e1, e2);
        System.out.println(" val = " + val + ", (" + _minVal + ", " + _maxVal + ")");
        return (val >= _minVal && val <= _maxVal);
    }
    /**
     * Print out the contents of this constraint in
     * a grammar format.
     */
    public String toString(String e1Name, String e2Name) {
        return _relation.toString(e1Name, e2Name) + " = ["
            + printMinMax(_minVal) + ", " + printMinMax(_maxVal) + "]";
    }

    /**
     * Print out the contents of this constraint.
     */
    public String toString() {
        String out = "BasicConstraint[\n";
        out = out + "  Relation:\n" + _relation + "\n";
        out = out + "  Min: " + printMinMax(_minVal) + "\n";
        out = out + "  Max: " + printMinMax(_maxVal) + "\n";
        out = out + "]";
        return out;
    }
}






