/*
 * $Id: ChoiceElement.java,v 1.2 2001/07/22 22:01:53 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.recognition;
import diva.sketch.recognition.Type;
import java.util.List;

/**
 * A choice element represents a choice between multiple
 * typed elements that have the same type (or super-type).
 * A choice element has the following properties:
 * <ul>
 *   <li> All direct children of a choice node cover the same
 *        set of stroke leaves.
 *   <li> The type of a choice node is a super-class of each of
 *        its direct children, either in the object-oriented sense
 *        of the word (Triangle extends Shape) or the grammar
 *        sense of the word (shape ::= triangle | square | circle).
 * </ul>
 *
 * Here is an example of a choice element:
 *
 * <pre>
 * textLine(.9)        textLine(.7)
 *    |                   |
 * text("foo", .9)   text("boo", .7)
 *         \           /
 *         [strokes...]
 * </pre>
 * 
 * can be represented as:
 * 
 * <pre>
 *            textLine
 *               |
 *          CHOICE(text)
 *          /          \
 * text("foo", .9)   text("boo", .7)
 *          \          /
 *          [strokes...]
 * </pre>
 * 
 * <p> This means that all interpretations of text that use the same
 * underlying strokes are handled as one node through the rest of the
 * parsing algorithm.  This greatly improves parsing efficiency.  It
 * is up to the application to choose one of the children of a choice
 * node (probably the highest confidence choice, or allow the user to
 * mediate when the difference in confidences is low).
 *
 * @author 	Michael Shilman (michaels@eecs.berkeley.edu)
 * @version	$Revision: 1.2 $
 * @rating Red
 */
public interface ChoiceElement extends CompositeElement {
    /**
     * Return a list that contains the choices that this
     * element covers, from high to low confidence.
     */
    public List choices();

    /**
     * Return an array of the names of the children of this
     * element, each of which sequentially corresponds to an item in
     * the array returned by children().
     */
    public List choiceNames();
}


