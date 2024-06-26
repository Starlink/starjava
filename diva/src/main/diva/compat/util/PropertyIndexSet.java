/*
 * $Id: PropertyIndexSet.java,v 1.1 2002/05/19 22:03:44 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.compat.util;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Vector;

/**
 * PropertyIndexSet is a mechanism for providing efficient
 * dynamic object annotation for data structures that might be used
 * by a number of cooperating packages. <P>
 *
 * A nearly equivalent functionality may be provided by a more
 * naive implementation, using a Hashtable to store object propertys:
 *
 * <PRE>
 *     void traverse(HashPropertyContainer obj) {
 *         Boolean visited = (Boolean)obj.getProperty("visited");
 *         if((visited == null) || (!visited.booleanValue())) {
 *             // (some traversal code here...)
 *             obj.setAttr("visited", Boolean.TRUE);
 *         }
 *     }
 * </PRE>
 *
 * This allows an algorithm to mark an object as "visited" without
 * modifying the object's code and without building external data
 * structures.  However, providing annotation this way, though flexible,
 * can be prohibitively expensive because a hash function must be called
 * for every "getAttr" and "setAttr" invocation. <P>
 *
 * PropertyIndexSet provides nearly the same functionality, but more
 * efficiently.  It does this by a "hash once" technique, where algorithms
 * or packages can reserve an integer "slot" once from the index set, and
 * use it thereafter with no lookup time and constant access time.  Equivalent
 * code to the above "visited" example:
 *
 * <PRE>
 *     // do this only once
 *     static int VISITED_INDEX = getPropertyIndexSet().getIndex("visited);
 *
 *     void traverse(PropertyContainer obj) {
 *         Boolean visited = (Boolean)obj.getProperty(VISITED_INDEX);
 *         if((visited == null) || (!visited.booleanValue())) {
 *             // (some traversal code here...)
 *             obj.setAttr(VISITED_INDEX, Boolean.TRUE);
 *         }
 *     }
 * </PRE>
 *
 * Obviously there are issues about when it is appropriate to use this
 * scheme.  For example, if the propertys are very sparse, it might be
 * better to use an external data structure to maintain the data.  It also
 * requires a global "name space", which is not true of a one-hashtable-per-
 * object implementation.  A complementary class <i>PropertyManager</i> is
 * provided to manage "scopes".  For exmple, in a graph package if you would
 * like one set of indexes for nodes in a graph and a separate set for edges,
 * you can use PropertyManager to define a "node" scope and an "edge"
 * scope.  Alternately, you can manage multiple PropertyIndexSets in a way
 * which makes sense to your application (e.g. make them global variables,
 * or store them at the top of collections of annotated objects...).
 *
 * @see PropertyContainer
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 */
public class PropertyIndexSet {
	/**
	 * A constant which designates that a particular
	 * slot name does not yet have an index in the property
	 * table.
	 */
	public static int NO_INDEX = -1;

	/**
	 * Storage for the indexes, hashed on the
	 * string "slotName".
	 */
    private Hashtable _hash = new Hashtable(5);

	/**
	 * A count of the current number of indexes that have
	 * been allocated.
	 */
    private int _numattr = 0;

	/**
	 * A free list for indexes.
	 */
    private LinkedList _free = new LinkedList();

	/**
	 * Maintain the names of the indexes.
	 */
    private Vector _names = new Vector();

    /**
     * Create an empty index set.
     */
    public void PropertyIndexSet() {

    }

    /**
     * Add an index to the free list.
     */
    private void addFree(Integer index) {
        _free.add(index);
    }

    /**
     * Free the index of <i>slotName</i> for use by
     * another property.  Note that this does not
     * free the the propertys on the objects which
     * are actually being tagged, so it is up to the
     * user of the PropertyIndexSet management facility
     * to free that themselves.
     */
    public void freeIndex(String slotName) {
        Integer index = (Integer)_hash.remove(slotName);
        if(index != null) {
            addFree(index);
            _names.setElementAt(null, index.intValue());
        }
    }

    /**
     * Get an index from the free lst.
     */
    private Integer getFree() {
        if(_free.isEmpty()) {
            return null;
        }
        return (Integer)_free.removeFirst();
    }

    /**
     * Returns the index of the property named by <i>slotName</i>.
     * If <i>slotName</i> doesn't yet have an index, it creates
     * a new slot for it.
     */
    public int getIndex(String slotName) {
    	int i = queryIndex(slotName);
		if(i != NO_INDEX) {
			return i;
		}
		else {
            Integer index = getFree();
            if(index == null) {
                index = Integer.valueOf(_numattr++);
            }
            _hash.put(slotName, index);

            i = index.intValue();
            if(_names.size() < i+1) {
               _names.setSize(i+3);
            }
            _names.setElementAt(slotName, i);
            return i;
        }
    }

    /**
     * Returns the string name of the property held by
     * index.
     */
    public String indexName(int index) {
        String name = null;
        return (String)_names.elementAt(index);
    }

    /**
     * Returns the index of the property named by <i>slotName</i>.
     * If <i>slotName</i> doesn't yet have an index, it returns
     * the constant <b>NO_INDEX</b>.
     */
    public int queryIndex(String slotName) {
        Integer index = (Integer)_hash.get(slotName);

        if(index != null) {
            return index.intValue();
		}
        else {
        	return NO_INDEX;
        }
    }
}


