/*
 * $Id: OrConstraint.java,v 1.6 2001/07/22 22:01:50 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.parser2d;
import java.util.ArrayList;
import java.util.Iterator;
import diva.sketch.recognition.SceneElement;

/**
 * A composite constraint that <i>ORs</i> together the results of its
 * child constraint objects.
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.6 $
 * @rating  Red
 */
public class OrConstraint implements RelationConstraint {
    /**
     * The constraints that are OR'ed.
     */
    private ArrayList _constraints = new ArrayList();
    
    /**
     * Test the child constraints on the given rectangles; return true
     * if any constraints are met or there are no constraints, false
     * otherwise.
     */
    public boolean test (SceneElement e1, SceneElement e2) {
        for(Iterator i = _constraints.iterator(); i.hasNext(); ) {
            RelationConstraint r = (RelationConstraint)i.next();
            if(r.test(e1, e2)) {
                return true;
            }
        }
        //if no constraints return true, else return false.
        return (_constraints.size() == 0);
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
        String out = "OrConstraint[\n";
        for(Iterator i = _constraints.iterator(); i.hasNext(); ) {
            out = out + (i.next().toString()) + "\n";
        }
        out = out + "]";
        return out;
    }
}






