/*
 * $Id: SceneDelta.java,v 1.5 2001/07/22 22:01:54 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.recognition;

import diva.util.ArrayIterator;
import diva.util.NullIterator;
import java.util.Iterator;

/**
 * A class that represents a change in the scene database.
 * Deltas can either be additive or subtractive, meaning
 * that they can
 *
 * TODO: more documentation on SceneDelta
 *
 * @author 	Michael Shilman (michaels@eecs.berkeley.edu)
 * @version	$Revision: 1.5 $
 * @rating Red
 */
public abstract class SceneDelta {
    /**
     * Root that was added
     */
    public abstract CompositeElement getRoot();

    /**
     * Entries that were added but are not the root.
     */
    public abstract Iterator elements();	
	
    /**
     * Commit the results of this delta to the scene.
     */
    public abstract void commit();

    /**
     * Remove this delta from the database.  This will
     * remove the root and all of the children that were
     * generated in this delta.
     */
    public abstract void veto();
	
    /**
     * Get the confidence of the root of the delta.
     */
    public abstract double getConfidence();	

    /**
     * A subtractive version of the SceneDelta class.  Subtractive
     * deltas represent changes that have already been made to the
     * database.  Therefore the commit() method does nothing and the
     * 
     */
    public static class Subtractive extends SceneDelta {
        private CompositeElement _root;
	
        private SceneElement[] _elements;
	
        private Scene _db;
		
        /**
         * Construct a new scene delta on the given database,
         * consisting of the given root that was added to the
         * database, as well as the 
         */
        public Subtractive(Scene db, CompositeElement root,
                SceneElement[] elements) {
            _db = db;
            _root = root;
            _elements = elements;
        }
	
        /**
         * Construct a new scene delta on the given database,
         * consisting of the given root that was added to the
         * database, as well as the 
         */
        public Subtractive(Scene db, CompositeElement root) {
            this(db, root, null);
        }

        /**
         * Commit the results of this delta to the scene.
         */
        public void commit() {
            //do nothing
        }
		
        /**
         * Get the confidence of the root of the delta.
         */
        public double getConfidence() {
            return _root.getConfidence();
        }
		
        /**
         * Root that was added
         */
        public CompositeElement getRoot() {
            return _root;
        }

        /**
         * Entries that were added but are not the root.
         */
        public Iterator elements() {
            if (_elements == null) {
                return new NullIterator();
            }
            else {
                return new ArrayIterator(_elements);
            }
        }

        /**
         * Remove this delta from the database.  Since additive
         * deltas have not been added to the database, this method
         * won't do anything until commit() has been called. 
         */
        public void veto() {
            _db.removeElement(_root);
            for(Iterator i = elements(); i.hasNext(); ) {
                SceneElement e = (SceneElement)i.next();
		/* TODO: reference count */
                _db.removeElement(e);
            }
        }
		
        public String toString() {
            return "SceneDelta.Subtractive[" + _root + "]";
        }
    }
	
    /**
     * An additive version of the SceneDelta class.  Additive
     * deltas add themselves to the scene database on demand,
     * when the user calls the commit() method.
     */
    public static class Additive extends SceneDelta {
        private TypedData _data;
        private double _confidence;
        private String[] _names;
        private CompositeElement _root;
        private SceneElement[] _elements;
        private Scene _db;
		
        /**
         * Construct a new scene delta on the given database,
         * consisting of the given root that was added to the
         * database, as well as the 
         */
        public Additive(Scene db, TypedData data, double confidence, 
                SceneElement[] elements, String[] names) {
            _db = db;
            _root = null;
            _data = data;
            _confidence = confidence;
            _names = names;
            _elements = elements;
        }

        /**
         * Construct a new scene delta on the given database,
         * consisting of the given root that was added to the
         * database, as well as the 
         */
        public Additive(Scene db, TypedData data, double confidence, 
                SceneElement[] elements) {
            this(db, data, confidence, elements, null);
        }		

        /**
         * Get the confidence of the root of the delta.
         */
        public double getConfidence() {
            return _confidence;
        }
		
        /**
         * Root that was added
         */
        public CompositeElement getRoot() {
            return _root;
        }

        /**
         * Entries that were added but are not the root.
         */
        public Iterator elements() {
            return new ArrayIterator(_elements);
        }
		
        /**
         * Commit the results of this delta to the scene.
         */
        public void commit() {
            //TODO: add the children
            _root = _db.addComposite(_data, _confidence, _elements, _names);
        }

        /**
         * Remove this delta from the database.  Since additive
         * deltas have not been added to the database, this method
         * won't do anything until commit() has been called. 
         */
        public void veto() {
            //do nothing
        }
		
        public String toString() {
            return "SceneDelta.Additive[" + _root + "]";
        }		
    }
}

