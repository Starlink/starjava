/*
 * $Id: AndConstraint.java,v 1.6 2001/07/22 22:01:49 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.parser2d;
import java.util.ArrayList;
import java.util.Iterator;
import diva.sketch.recognition.SceneElement;
import java.awt.geom.Rectangle2D;

/**
 * A composite constraint that <i>ANDs</i> together the results of its
 * child constraint objects.
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.6 $
 * @rating  Red
 */
public class AndConstraint implements RelationConstraint {
    /**
     * The constraints that are AND'ed.
     */
    private ArrayList _constraints = new ArrayList();

    /**
     * Debugging output.
     */
    public static void debug (String s) {
        System.out.println(s);
    }

    /**
     * Test the child constraints on the given rectangles; return true
     * if all of them are met or there are no child constraints, false
     * otherwise.
     */
    public boolean test (SceneElement e1, SceneElement e2) {
        for(Iterator i = _constraints.iterator(); i.hasNext(); ) {
            RelationConstraint r = (RelationConstraint)i.next();
            if(!r.test(e1, e2)) {
                //                debug("RELATION FAILED: " + r);
                return false;
            }
            //            debug("RELATION PASSED: " + r);
        }
        return true;
    }

    /**
     * Add another child constraint to the test.
     */
    public void addConstraint(RelationConstraint r) {
        _constraints.add(r);
    }

    /**
     * Print out the contents of this constraint.
     */
    public String toString() {
        String out = "AndConstraint[\n";
        for(Iterator i = _constraints.iterator(); i.hasNext(); ) {
            out = out + (i.next().toString()) + "\n";
        }
        out = out + "]";
        return out;
    }
}






