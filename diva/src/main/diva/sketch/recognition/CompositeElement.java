/*
 * $Id: CompositeElement.java,v 1.2 2001/07/22 22:01:53 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.recognition;
import diva.sketch.recognition.TypedData;
import java.util.List;

/**
 * A composite element is the encapsulation of all the information
 * known about a parsed element in a scene.  It is composed of other
 * scene elements, which are represented as named children.  It has
 * typed data that represent an interpretation of its children, and
 * a confidence to go along with that data.
 *
 * @author 	Michael Shilman (michaels@eecs.berkeley.edu)
 * @version	$Revision: 1.2 $
 * @rating Red
 */
public interface CompositeElement extends SceneElement {
    /**
     * Return a list of the children of this element,
     * each of which is of type SceneElement.
     */
    public List children();

    /**
     * Return an array of the names of the children of this
     * element, each of which sequentially corresponds to an item in
     * the array returned by children().
     */
    public List childNames();
    
    /**
     * Return the confidence associated with the typed data returned
     * by getData().
     */
    public double getConfidence();

    /**
     * Return the typed data associated with this element (the
     * recognition result it represents).
     */
    public TypedData getData();
}

