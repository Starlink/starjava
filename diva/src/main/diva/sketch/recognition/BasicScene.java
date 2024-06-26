/*
 * $Id: BasicScene.java,v 1.8 2001/07/22 22:01:53 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.recognition;

import diva.sketch.recognition.TimedStroke;
import diva.sketch.recognition.TypedData;
import diva.sketch.recognition.SimpleData;
import diva.sketch.recognition.Type;
import diva.util.IteratorIterator;

import java.util.Set;
import java.util.BitSet;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import diva.util.ArrayIterator;
import diva.util.NullIterator;
import java.awt.geom.Rectangle2D;

/**
 * A scene database that keeps track of multiple interpretations of a
 * set of strokes.  Groups of strokes are stored hierarchically, and
 * these also have multiple interpretations.  There are also choice
 * elements that represent a mutually exclusive choice between objects
 * of the same type that use the same type.  Using this data
 * structure, a client can view all of the interpretations of the
 * scene and perform efficient operations to filter interpretations.
 *
 * <p>
 * BasicScene uses the Java collections classes to implement the
 * scene infrastructure in an obvious and unoptimized way.  Future
 * implementations of the scene interface may 
 *
 * @author 	Michael Shilman (michaels@eecs.berkeley.edu)
 * @version	$Revision: 1.8 $
 * @rating Red
 */
public class BasicScene implements Scene {
    /**
     * Store the roots for fast access.  The roots are CompositeElt's
     * (ChoiceElt's extends from CompositeElt, so it could be a root)
     * and they are stored in the order of descending confidence values.
     */
    private ArrayList _roots = new ArrayList();
    
    /**
     * Store the strokes for fast access.  These are StrokeElt's each
     * of which contains a TimedStroke.
     */
    private ArrayList _strokes = new ArrayList();
	
    /**
     * Store the choices indexed by type for fast access.  This is a hash
     * table whose key is a Type object (ChoiceElt's type) and
     * value is an ArrayList containing ChoiceElt's of that Type).
     */
    private HashMap _choices = new HashMap();
	
    /**
     * Fast index for scene elements based on types.  This is a hash
     * table whose key is a Type object (CompositeElt's type) and
     * value is an ArrayList containing CompositeElt's of that Type).
     */
    private HashMap _typeIndex = new HashMap();

    /**
     * Store a count of the strokes.  The number of TimedStrokes in the
     * scene.
     */
    private int _strokeCnt = 0;

    /**
     * Store a marking tag for certain graph traversals.
     */
    private short _mark = 0;

    /**
     * Store which leaves are still in the database.  If a bit is set
     * to 1, it means that the stroke has been deleted.
     */
    private BitSet _deletedStrokes = new BitSet();

    /**
     * Book-keeping of how many internal nodes we have.  This is a
     * count of CompositeElt (including ChoiceElt since it's a
     * subclass) in the scene.
     */
    private int _compositeCnt = 0;
	
    /**
     * Add a new choice element to the database and return it.  This
     * method will create a new choice element if no appropriate
     * choice already exists, otherwise it will use an existing
     * choice.  This element will appear in the roots() iterator until
     * it is added as a child of another composite node, or until it
     * is removed from the database.
     *
     * @param data    The typed data associated with the choice element.
     * @param child  The single child of this element in the tree.
     * @param name    The name of the child.
     */
    private ChoiceElement _addChoice(TypedData data, double confidence,
            CompositeElement child, String name) {
        BitSet id = ((AbstractElt)child).getID();
        ArrayList l = (ArrayList)_choices.get(data.getType());
        if(l == null) {
            l = new ArrayList();
            _choices.put(data.getType(), l);
        }

        for(Iterator i = l.iterator(); i.hasNext(); ) {
            ChoiceElt existing = (ChoiceElt)i.next();
            if(existing.getData().equals(data) && _sameID(id, existing.getID())) {
                existing.addChoice(confidence, child, name);
                _setParent(existing, child);
                return null;
            }
        }

        //we didn't find any existing choice
        ChoiceElt choice = new ChoiceElt(id, data, confidence, child, name);
        l.add(choice);
        _indexElt(choice);
        _setParent(choice, child);
        return choice;
    }

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
            SceneElement[] children, String[] names) {

        /*
        if(children.length == 1 && (children[0] instanceof CompositeElement)) {
            return _addChoice(data, confidence, (CompositeElement)children[0], names[0]);
        }
        */

        //figure out the bit set id for the new element
        AbstractElt child0 = (AbstractElt)children[0];
        BitSet id = (BitSet)(child0.getID().clone());
        for(int i = 1; i < children.length; i++) {
            AbstractElt absElt = (AbstractElt)children[i];
            id.or(absElt.getID());
        }

        //build the new element and make it a parent of the children
        //children.
        CompositeElt out = new CompositeElt(id, data, confidence,
                children, names);

        for(int i = 0; i < children.length; i++) {
            _setParent(out, children[i]);
        }
        _indexElt(out);
        return out;
    }


    /**
     * Return a list of all of the choices contained in the scene.
     * This list will only have choice elements that contain more
     * than one choice.
     */
    public List choices() {
        ArrayList out = new ArrayList();
        for(Iterator i = _choices.values().iterator(); i.hasNext(); ) {
            ArrayList l = (ArrayList)i.next();
            for(Iterator j = l.iterator(); j.hasNext(); ) {
                ChoiceElt choice = (ChoiceElt)j.next();
                if(choice.choices().size() > 1) {
                    out.add(choice);
                }
            }
        }
        return out;
        //FIXME - IteratorIterator?
    }

    /**
     * Add a new stroke element to the database.  This element will
     * appear in the strokes() iterator until it is removed from the
     * database.
     */
    public StrokeElement addStroke(TimedStroke stroke) {
        BitSet id = new BitSet();
        id.set(_strokeCnt++);
        StrokeElt element = new StrokeElt(id, stroke);
        _strokes.add(element);
        return element;
    }

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
    public void confirm(CompositeElement elt, boolean makeChoices) {
        _markDescendents(elt, _mark, makeChoices);
        _confirmHelper((CompositeElt)elt, _mark);
        _mark++;
    }

    /**
     * Find the elements in the scene with the given type that do not
     * contradict the given element, and return them as a list.  (FIXME-
     * somehow ordered by relevance?)
     *
     * @see isConsistent(SceneElement, SceneElement)
     * @param type The type of elements that will be returned.
     * @param elt The element that the returned set must be consistent with,
     *              or null if it doesn't matter.
     * @return A list of scene elements that match the query.
     */
    public List elementsOfType(Type type, CompositeElement elt) {
        ArrayList l = (ArrayList)_typeIndex.get(type);
        ArrayList results = new ArrayList(0);
        if(l == null) {
            return results;
        }
        for(Iterator i = l.iterator(); i.hasNext(); ) {
            SceneElement e2 = (SceneElement)i.next();
            if(isConsistent(elt, e2)) {
                results.add(e2);
            }
        }
        return results;
    }

    /**
     * Return a count of the total number of composite
     * nodes in the database, for a crude measure of
     * performance.
     */
    public int getCompositeCount() {
        return _compositeCnt;
    }
    
    /**
     * Return a count of the total number of stroke
     * nodes in the database, for a crude measure of
     * performance.
     */
    public int getStrokeCount() {
        return _strokeCnt;
    }
    
    /**
     * Return whether or not this element covers all of the leaves.
     * An element covers a leaf if the leaf is a descendent of the
     * element.
     */
    public boolean isCoveringAll(SceneElement elt) {
        //FIXME: a -> b -> cd -> ... (?)
        if(!_roots.contains(elt)) {
            return false;
        }
        BitSet id = (BitSet)(((AbstractElt)elt).getID().clone());
        //if some of the children have been deleted, we
        //shouldn't count it.
        id.or(_deletedStrokes);
        for(int i = 0; i < _strokeCnt; i++) {
            if(!id.get(i)) {
                return false;
            }
        }
//          if(((CompositeElt)elt).getData().getType().getID().equals("slide")) {
//              System.out.println("_strokeCnt = " + _strokeCnt);
//              System.out.println("_deletedStrokes = " + _deletedStrokes);
//              System.out.println("id = " + ((AbstractElt)elt).getID());
//          }
        return true;
    }

    /**
     * Return whether or not the two elements are consistent, that is,
     * whether the leaf nodes that they cover (their support) are
     * strictly disjoint.  Return true if either of the elements are
     * null.
     */
    public boolean isConsistent(SceneElement e1, SceneElement e2) {
        if(e1 == null || e2 == null) {
            return true;
        }
        BitSet b1 = ((AbstractElt)e1).getID();
        BitSet b2 = ((AbstractElt)e2).getID();
        BitSet tmp = (BitSet)b1.clone();
        tmp.and(b2);
        for(int i = 0; i < _strokeCnt; i++) {
            if(tmp.get(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Remove an element from the database.  This will also trigger the
     * removal of every element that depends on the given element.
     */
    public void removeElement(SceneElement elt) {
        if(elt instanceof CompositeElt) {
            CompositeElt compElt = (CompositeElt)elt;
            Type type = compElt.getData().getType();
            ArrayList l = (ArrayList)_typeIndex.get(type);
            _assert((l != null), "unexpected null index on: " + type);
            l.remove(compElt);
            for(Iterator i = compElt.elements().iterator(); i.hasNext(); ) {
                AbstractElt child = (AbstractElt)i.next();
                child.removeParent(compElt);
            }
        }

        for(Iterator i = elt.parents().iterator(); i.hasNext(); ) {
            AbstractElt parent = (AbstractElt)i.next();
            i.remove(); //avoid concurrent mod
            if(parent instanceof ChoiceElement) {
                ChoiceElt choice = (ChoiceElt)parent;
                CompositeElt compElt = (CompositeElt)elt;
                choice.removeChoice(compElt);
                if(choice.choices().size() == 0) {
                    removeElement(choice);
                }
            }
            else {
                removeElement(parent);
            }
        }
        _roots.remove(elt);
        _strokes.remove(elt);
        _choices.remove(elt);

        if(elt instanceof StrokeElt) {
            // keep track that it has been deleted in the id set
            BitSet id = ((AbstractElt)elt).getID();
            _deletedStrokes.or(id);
        }
    }
	
    /**
     * Return a list of every root node in the database, i.e.
     * all top-level scene interpretations, from high to low
     * confidence.  This iterator may contain composite elements
     * or choice elements; it will not contain strokes.
     */
    public List roots() {
        return _roots;
    }

    /**
     * Return true if the supports of the two elements (i.e. the
     * sets elements that they cover) are identical.
     */
    public boolean sameSupport(SceneElement e1, SceneElement e2) {
        BitSet id1 = ((AbstractElt)e1).getID();
        BitSet id2 = ((AbstractElt)e2).getID();
        return _sameID(id1, id2);
    }
    
//      /**
//       * Return all root elements that contain the given internal
//       * element, from high to low confidence.
//       */
//      public List roots(SceneElement contained) {
//          //FIXME
//      }

    /**
     * Return a list the stroke elements of the scene in
     * the order that they were added to the database.
     */
    public List strokes() {
        return _strokes;
    }

    /**
     * Utility for checking invariants.
     */
    private void _assert(boolean condition, String err) {
        if(!condition) {
            throw new RuntimeException(err);
        }
    }

    /**
     * Index the new element by type and add it to the roots array in
     * order of confidence.<p>
     *
     * Using elt's type as the key, this method indexes into the
     * _typeIndex hash table to get the ArrayList containing
     * CompositeElt's of this type.  If no such ArrayList exists
     * (meaning no CompositeElt of this type is in the scene), create
     * an ArrayList.  Add 'elt' to the ArrayList.  Then add 'elt' into
     * _roots in descending confidence order.
     */
    private void _indexElt(CompositeElt elt) {
        Type type = elt.getData().getType();
        ArrayList l = (ArrayList)_typeIndex.get(type);
        if(l==null) {
            l = new ArrayList();
            _typeIndex.put(type, l);
        }
        _assert(!l.contains(elt), "duplicate type index: " + elt);
        l.add(elt);

        // Add a root in sorted order.
        _assert(!_roots.contains(elt), "duplicate root index: " + elt);
        int i;
        for(i = 0; i < _roots.size(); i++) {
            CompositeElement root = (CompositeElement)_roots.get(i);
            if(elt.getConfidence() > root.getConfidence()) {
                break;
            }
        }
        _roots.add(i, elt);
        _compositeCnt++;
    }


    /**
     * Add the parent to the child's list of parents
     * and remove the child from the array of roots.
     */
    private void _setParent(CompositeElt parent, SceneElement child) {
        AbstractElt absChild = (AbstractElt)child;
        absChild.addParent(parent);
        _roots.remove(absChild);
    }

    /**
     * Mark the given element and all of its descendents
     * with the given mark, so that a subsequent traversal
     * will be able to distinguish between elements inside
     * and outside of the marked set.
     */
    public void _markDescendents(SceneElement elt, short mark,
            boolean makeChoices) {
        AbstractElt absElt = (AbstractElt)elt;
        absElt.setMark(mark);
        if(elt instanceof CompositeElt) {
            if(makeChoices && elt instanceof ChoiceElt) {
                ChoiceElt choice = (ChoiceElt)elt;
                SceneElement child = (SceneElement)choice.elements().get(choice.getWhich());
                _markDescendents(child, mark, makeChoices);
            }
            else {
                List elements = ((CompositeElt)elt).elements();
                for(Iterator i = elements.iterator(); i.hasNext(); ) {
                    SceneElement child = (SceneElement)i.next();
                    _markDescendents(child, mark, makeChoices);
                }
            }
        }
    }
    
    /**
     * For all elements on the traversal and their direct parents,
     * remove elements that are not marked with the given mark.
     */
    private void _confirmHelper(CompositeElt elt, short mark) {
        if(elt.getMark() != mark) {
            removeElement(elt);
        }
        for(Iterator i = elt.elements().iterator(); i.hasNext(); ) {
            SceneElement child = (SceneElement)i.next();
            //avoid concurrent-modification errors by using the
            //iterator to do removal
            for(Iterator parents = child.parents().iterator();
                parents.hasNext(); ) {
                AbstractElt parent = (AbstractElt)parents.next();
                if(parent.getMark() != mark) {
                    parents.remove();
                    removeElement(parent);
                }
            }
            if(child instanceof CompositeElt) {
                _confirmHelper((CompositeElt)child, mark);
            }
        }
    }

    /**
     * Return true if the supports of the two elements (i.e. the
     * sets elements that they cover) are identical.
     */
    public boolean _sameID(BitSet id1, BitSet id2) {
        BitSet tmp = (BitSet)id1.clone();
        tmp.xor(id2);
        for(int i = 0; i < _strokeCnt; i++) {
            if(tmp.get(i)) {
                return false;
            }
        }
        return true;
    }
    

    /**
     * An abstract base class for all elements.
     */
    private static abstract class AbstractElt implements SceneElement {
        private HashSet _parents = new HashSet();
        private BitSet _id;
        private short _mark = -1;
        public AbstractElt(BitSet id) {
            _id = id;
        }
        public BitSet getID() {
            return _id;
        }		
        public Set parents() {
            return _parents;
        }
        public void addParent(CompositeElement e) {
            if(!_parents.contains(e)) {
                _parents.add(e);
            }
        }
        public abstract Rectangle2D getBounds();
        public void removeParent(CompositeElement e) {
            _parents.remove(e);
        }
        public void setMark(short mark) {
            _mark = mark;
        }
        public short getMark() {
            return _mark;
        }
    }
    
    /**
     * 
     */
    private static final class ChoiceElt extends CompositeElt
        implements ChoiceElement {
        
        private ArrayList _choices;
        private ArrayList _choiceNames;
        private ArrayList _confidences;
        private int _which;

        public ChoiceElt(BitSet id, TypedData data, double confidence,
                CompositeElement child, String name) {
            super(id);

            _data = data;
            _children = new SceneElement[1];
            _childNames = new String[1];
            _bounds = child.getBounds();
            
            _choices = new ArrayList(1);
            _choiceNames = new ArrayList(1);
            _confidences = new ArrayList(1);
            _choices.add(child);
            _choiceNames.add(name);
            _confidences.add(Double.valueOf(confidence));
            
            setWhich(0);
            //FIXME - assertions about type and makeup
        }
        public List elements() {
            return choices();
        }
        public void addChoice(double confidence, CompositeElement choice, String name) {
            if(_choices.contains(choice)) {
                return;  //FIXME - why do we get here?
            }
            
            int i;
            for(i = 0; i < _choices.size(); i++) {
                CompositeElement existing = (CompositeElement)_choices.get(i);
                if(choice.getConfidence() > existing.getConfidence()) {
                    break;
                }
            }
            _choices.add(i, choice);
            _choiceNames.add(i, name);
            _confidences.add(i, Double.valueOf(confidence));
            setWhich(0);
        }
        public void removeChoice(CompositeElement choice) {
            _choices.remove(choice);
            if(_choices.size() > 0) {
                setWhich(0);
            }
        }
        public List choices() {
            return _choices;
        }
        public int getWhich() {
            return _which;
        }
        public List choiceNames() {
            return _choiceNames;
        }
        public void setWhich(int which) {
            if(which < 0 || which >= _choices.size()) {
                String err = "Illegal choice: " + which +
                    ", should be between 0 and " + (_choices.size()-1);
                throw new IllegalArgumentException(err);
            }
            _which = which;
            _children[0] = (SceneElement)_choices.get(_which);
            _childNames[0] = (String)_choiceNames.get(_which);
            _confidence = ((Double)_confidences.get(_which)).doubleValue();
        }
        public String toString() {
            return "ChoiceElement@" + hashCode() + "[" + getData() + ", " + getConfidence() + "]";
        }
    }

    /**
     * A composite element with children; a logical
     * grouping of strokes.
     */
    private static class CompositeElt extends AbstractElt
        implements CompositeElement {
        protected TypedData _data;
        protected double _confidence;
        protected SceneElement[] _children;
        protected String[] _childNames;
        protected Rectangle2D _bounds;
        protected CompositeElt(BitSet id) {
            super(id);
        }
        public CompositeElt(BitSet id, TypedData data, double confidence, 
                SceneElement[] children, String[] childNames) {
            super(id);
            _data = data;
            _confidence = confidence;
            _children = children;
            _childNames = childNames;
        }
        public List elements() {
            return children();
        }
        public List children() {
            return Arrays.asList(_children);
        }
        public Rectangle2D getBounds() {
            if(_bounds == null) {
                _bounds = _children[0].getBounds();
                for(int i = 1; i < _children.length; i++) {
                    _bounds = _bounds.createUnion(_children[i].getBounds());
                }
            }
            return _bounds;
        }
        public TypedData getData() {
            return _data;
        }
        public double getConfidence() { 
            return _confidence;
        }
        public String toString() {
            return "CompositeElement@" + hashCode() + "[" + getData() + ", "
                + getConfidence() + "]";
        }
        public List childNames() {
            return Arrays.asList(_childNames);
        }
    }
	
	
    /**
     * An element with no children but a single stroke
     * "user object".
     */
    private static final class StrokeElt extends AbstractElt
        implements StrokeElement {
        
        private TimedStroke _stroke;
        
        public StrokeElt(BitSet id, TimedStroke stroke) {
            super(id);
            _stroke = stroke;
        }
        public Rectangle2D getBounds() {
            return _stroke.getBounds2D();
        }
        public TimedStroke getStroke() {
            return _stroke;
        }        
        public String toString() {
            //return super.toString() + "[" + _stroke + "]";
            return "StrokeElement@" + hashCode() + "[" + getBounds() + "]";
        }
    }
}


