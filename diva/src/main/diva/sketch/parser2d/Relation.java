/*
 * $Id: Relation.java,v 1.14 2001/07/22 22:01:50 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.parser2d;
import diva.sketch.recognition.SceneElement;

/**
 * An object that specifies a directed spatial relationship between
 * two scene elements.  This relationship returns a scalar value,
 * which can be used to check constraints, or as a feature for a
 * classifier of some sort.
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.14 $
 * @rating  Red
 */
public interface Relation {
    /**
     * Return the value of the relation extracted from the given
     * rectangles.
     */
    public double apply(SceneElement e1, SceneElement e2);

    /**
     * Return the name of the relation extractor.
     */
    public String getName();

    /**
     * Pretty print the relation in the grammar format.
     */
    public String toString(String e1Name, String e2Name);
}





