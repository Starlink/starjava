/*
 * $Id: RelationConstraint.java,v 1.3 2000/08/12 10:59:45 michaels Exp $
 *
 * Copyright (c) 1998 The Regents of the University of California.
 * All rights reserved.  See the file COPYRIGHT for details.
 */
package diva.sketch.parser2d;
import diva.sketch.recognition.SceneElement;

/**
 * An object that returns whether a pair of elements satisfies
 * constraints on the values of a specified relation.
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 * @rating  Red
 */
public interface RelationConstraint {
    /**
     * Test the constraint on the given elements; return true if the
     * constraint is met, false otherwise.
     */
    public boolean test (SceneElement e1, SceneElement e2);

    /**
     * Hack for code generation.
     *
    public String toString(String root, String relative);
    */
}




