/*
 * $Id: Topology.java,v 1.4 2002/01/16 01:27:17 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.pod.lwgraph;

import java.util.Arrays;

/** The primitive data structure underlying a light-weight graph. Clients
 * can instantiate and use this class directly if they don't mind
 * dealing with integer indices to represent nodes and edges. Doing so
 * is recommended only if large graphs are being constructed with no
 * need for information attached to the nodes and/or edges. Note that
 * very little error checking is performed in this class. If information
 * needs to be attached to nodes or edges, then the LightweightGraph class is
 * recommended instead.
 *
 * @version $Revision: 1.4 $
 * @author John Reekie
 * @rating Red
 */
public final class Topology {

    // The highest node id
    private int _maxnodeid = 0;

    // Initial number of edges
    private static int INITEDGEMAX = 64;

    // The highest edge id
    private int _maxedgeid = 0;

    // The array of head nodes
    int[] _headNodes = new int[INITEDGEMAX];

    // The array of tail nodes
    int[] _tailNodes = new int[INITEDGEMAX];

    /** Create a new, empty, topology
     */
    public Topology () {
        Arrays.fill(_headNodes, -1);
        Arrays.fill(_tailNodes, -1);
    }

    /** Connect the tail and head nodes, using the given edge id.
     * The amount is storage used by this class depends on the
     * maximum edge id, so edge ids should be alloctated by the
     * caller from zero on up, if possible. More than one edge
     * between two nodes is allowed. If an edge is to be connected
     * at one end only, use an id of -1 for the other end.
     */
    public void connect (int edge, int tail, int head) {
	// Check size and grow if needed
	if (edge >= _headNodes.length) {
	    int i;
	    int[] temp;
            int oldlength = _headNodes.length;
            int newlength = Math.max(oldlength * 2, edge+1);

	    // Isn't there something in the JDK to do this??
	    temp = new int[newlength];
	    for (i = 0; i < oldlength; i++) {
		temp[i] = _headNodes[i];
	    }
            Arrays.fill(temp, oldlength, newlength-1, -1);
	    _headNodes = temp;

	    temp = new int[newlength];
	    for (i = 0; i < oldlength; i++) {
		temp[i] = _tailNodes[i];
	    }
            Arrays.fill(temp, oldlength, newlength-1, -1);
	    _tailNodes = temp;
        }
	// Do assignment
	_headNodes[edge] = head;
	_tailNodes[edge] = tail;

        // Remember highest node id
        if (head > _maxnodeid) {
            _maxnodeid = head;
        }
        if (tail > _maxnodeid) {
            _maxnodeid = tail;
        }

        // Remember highest edge id
        if (edge > _maxedgeid) {
            _maxedgeid = edge;
        }
    }

    /** Find an edge between the given tail and head nodes, starting
     * from the given edge. Return -1 if none is found.
     */
    public int find (int firstedge, int tail, int head) {
	for (int i = firstedge; i < _maxedgeid; i++) {
	    if (_tailNodes[i] == tail && _headNodes[i] == head) {
		return i;
	    }
	}
	return -1;
    }

    /** Get the maximum edge id.
     */
    public int getMaxEdgeId () {
	return _maxedgeid;
    }

    /** Get the head node of the given edge.
     */
    public int getHead (int edge) {
	return _headNodes[edge];
    }

    /** Get the highest node id known
     */
    public int getMaxNodeId () {
	return _maxnodeid;
    }

    /** Get the tail node of the given edge.
     */
    public int getTail (int edge) {
	return _tailNodes[edge];
    }

    /** Remove the given edge.
     */
    public void removeEdge (int edge) {
	_tailNodes[edge] = -1;
	_headNodes[edge] = -1;
    }

    /** Reverse the graph
     */
    public void reverse () {
        for (int i = 0; i < _headNodes.length; i++) {
            int tmp = _headNodes[i];
            _headNodes[i] = _tailNodes[i];
            _tailNodes[i] = tmp;
        }
    }

    /** Set the head node of the given edge. The edge and node *must*
     * have already been introduced to the graph with the connect()
     * method. The node id can be -1 to "disconnect" the edge.
     */
    public void setHead(int edge, int head) {
        _headNodes[edge] = head;
    }
    
    /** Set the tail node. The edge and node *must*
     * have already been introduced to the graph with the connect()
     * method. The node id can be -1 to "disconnect" the edge.
     */
    public void setTail(int edge, int tail) {
        _tailNodes[edge] = tail;
    }
}
