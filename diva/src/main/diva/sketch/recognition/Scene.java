/*
 * $Id: Scene.java,v 1.16 2001/07/22 22:01:54 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.recognition;
import java.util.Set;
import java.util.List;
import diva.sketch.recognition.TimedStroke;
import diva.sketch.recognition.TypedData;
import diva.sketch.recognition.SimpleData;
import diva.sketch.recognition.Type;

/**
 * A scene database that keeps track of multiple interpretations of a
 * set of strokes.  Groups of strokes are stored hierarchically, and
 * these also have multiple interpretations.  There are also choice
 * elements that represent a mutually exclusive choice between objects
 * of the same type that use the same type.  Using this data
 * structure, a client can view all of the interpretations of the
 * scene and perform efficient operations to filter interpretations.
 *
 * @author 	Michael Shilman (michaels@eecs.berkeley.edu)
 * @version	$Revision: 1.16 $
 * @rating Red
 */
public interface Scene {
    /**
     * Add a new composite element to the database and return
     * it.  This element will appear in the roots() iterator until it
     * is added as a child of another composite node, or until it is
     * removed from the database.  This method will return null
     * if a pre-existing choice node is used, in which case no node
     * was actually added to the database.
     *
     * @param data       The typed data associated with the composite.
     * @param confidence The confidence of recognition, between 0 and 1.
     * @param children   The children of this element in the tree.
     * @param names      The names of the children.
     */
    public CompositeElement addComposite(TypedData data, double confidence, 
            SceneElement[] children, String[] names);

    /**
     * Add a new stroke element to the database.  This element will
     * appear in the strokes() iterator until it is removed from the
     * database.
     */
    public StrokeElement addStroke(TimedStroke stroke);

    /**
     * Return a list of all of the choices contained in the scene.
     * This list will only have choice elements that contain more
     * than one choice.
     */
    public List choices();

    /**
     * Accept the given interpretation of the set of strokes that the
     * given element covers as <i>the</i> correct interpretation of
     * those strokes.  This implies that all elements in the scene
     * that contradict this interpretation are <i>incorrect</i>
     * interpretations, so these interpretations are removed.
     *
     * @param elt The interpretation to confirm
     * @param makeChoices Whether or not to confirm the existing
     *                    choice nodes at or under <i>elt</i>.
     */
    public void confirm(CompositeElement elt, boolean makeChoices);

    /**
     * Find the elements in the scene with the given type that do not
     * contradict the given element, and return them as a list.  (FIXME-
     * ordered by relevance?)
     *
     * @see isConsistent(SceneElement, SceneElement)
     * @param type The type of elements that will be returned.
     * @param element The elements that the returned set must be
     *                consistent with, or null if it doesn't matter.
     * @return A list of scene elements that match the query.
     */
    public List elementsOfType(Type type, CompositeElement elt);
    
    /**
     * Return whether or not this element covers all of the leaves.
     * An element covers a leaf if the leaf is a descendent of the
     * element.
     */
    public boolean isCoveringAll(SceneElement elt); 

    /**
     * Return whether or not the two elements are consistent, that is,
     * whether the leaf nodes that they span are strictly disjoint.
     */
    public boolean isConsistent(SceneElement e1, SceneElement e2);

    /**
     * Remove an element from the database.  This will also trigger the
     * removal of every element that depends on the given element.
     */
    public void removeElement(SceneElement elt);
	
    /**
     * Return a list of every root node in the database, i.e.
     * all top-level scene interpretations, from high to low
     * confidence.  This iterator may contain composite elements
     * or choice elements; it will not contain strokes.
     */
    public List roots();
	
    /**
     * Return a list the stroke elements of the scene in
     * the order that they were added to the database.
     */
    public List strokes();
}


