/*
 * $Id: Traversal.java,v 1.6 2002/01/12 03:46:47 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.pod.lwgraph;

import java.util.Arrays;

/** A primitive data structure that makes graph traversal and
 * algorithms more efficient. Clients can instantiate and use this
 * class directly if they don't mind dealing with integer indices to
 * represent nodes and edges. A more friendly but less direct and
 * efficient API is exposed by the LightweightGraph class.
 *
 * <P> An object of this class can be instantiated on an Topology,
 * and then used to perform more efficient graph traversals. The
 * contents of this class are valid <b>only</b> while the graph
 * structure is maintained. As soon as the graph is modified, this
 * object must be discarded and a new one created.
 * 
 * <P> A traversal stores only the "forward" direction of a graph.
 * If a client wants to be able to traverse a graph in both
 * directions, then it will need to create a traversal on the
 * graph, reverse the graph and create a traversal on the reversed
 * graph, and then reverse the graph back again.
 *
 * <P> Although it seems like the approach like constructing a new
 * Traversal object many times is inefficient, it is actually not.
 * I also wrote a class the contained the data and functionality
 * of both Topology and Traversal. As edges were added, the
 * arrays of successor/predecessor nodes were updated each time.
 * This class was consistently slower (by 10% - 25%) than
 * constructing separate Traversal objects. Probably something
 * to do with the way the java optimizer works. Also, the code was
 * trickier when deleting things, so I left it like this. The performance
 * is still about 50 times better than diva.graph.
 *
 * <P> If a graph becomes very sparse through a lot of node deletion,
 * this class will get a little (time and space) inefficient. This can
 * be avoided by compacting the graph first.
 *
 * @version $Revision: 1.6 $
 * @author John Reekie
 * @rating Red
 */
public final class Traversal {

    // The topology that this traversal is on
    private Topology _topology;

    // The array of "out" edges
    private int[][] _edges;

    // The count of "out" edges
    private int[] _edgeCounts;

    // The array of node ids
    private int[] _nodes;

    // The count of nodes
    private int _nodeCount;

    // The roots of the graph
    private int[] _roots;

    // Number of root nodes
    private int _rootCount;

    // The arrays of successor nodes
    private int[][] _successors;

    // The count[] of successor nodes
    private int[] _successorCounts;

    /** Create a new traversal on the given topology
     */
    public Traversal (Topology top) {
	_topology = top;

        int nc = top.getMaxNodeId() + 1;
        int ec = top.getMaxEdgeId() + 1;

        if (nc == 1 || ec == 0) {
            return;
        }

        // Allocate arrays
        _edges = new int[nc][];
        _edgeCounts = new int[nc];
        _successors = new int[nc][];
        _successorCounts = new int[nc];

        // Iterate across the edges, storing stuff
        boolean[] tailflag = new boolean[nc];
        boolean[] headflag = new boolean[nc];
        for (int edge = 0; edge < ec; edge++) {
            int tail = top._tailNodes[edge];
            int head = top._headNodes[edge];

            if (tail == -1 || head == -1) {
                continue;
            }

            tailflag[tail] = true;
            headflag[head] = true;

            if (head >= 0 && tail >= 0) {
                // Add head to successors of tail
                if (_successors[tail] == null) {
                    _successors[tail] = new int[4];
                    Arrays.fill(_successors[tail], -1);

                } else if (_successorCounts[tail] == _successors[tail].length) {
                    int[] temp = new int[_successors[tail].length*2];
                    for (int i = 0; i < _successors[tail].length; i++) {
                        temp[i] = _successors[tail][i];
                    }
                    Arrays.fill(temp,
                            _successors[tail].length,
                            temp.length-1, -1);
                    _successors[tail] = temp;
                }
                _successors[tail][_successorCounts[tail]++] = head;

               // Add edge to out edges of tail
                if (_edges[tail] == null) {
                    _edges[tail] = new int[4];
                    Arrays.fill(_edges[tail], -1);

                } else if (_edgeCounts[tail] == _edges[tail].length) {
                    int[] temp = new int[_edges[tail].length*2];
                    for (int i = 0; i < _edges[tail].length; i++) {
                        temp[i] = _edges[tail][i];
                    }
                    Arrays.fill(temp,
                            _edges[tail].length,
                            temp.length-1, -1);
                    _edges[tail] = temp;
                }
                _edges[tail][_edgeCounts[tail]++] = edge;
             }
        }

        // Initialize nodes and roots
        _nodes = new int[64];
        _roots = new int[16];
        Arrays.fill(_nodes, -1);
        Arrays.fill(_roots, -1);

        for (int i = 0; i < nc; i++) {
            if (headflag[i] || tailflag[i]) {
                // This is a connected node
                if (_nodeCount == _nodes.length) {
                    int[] temp = new int[_nodes.length*2];
                    for (int j = 0; j < _nodes.length; j++) {
                        temp[j] = _nodes[j];
                    }
                    Arrays.fill(temp, _nodes.length, temp.length-1, -1);
                    _nodes = temp;
                }
                _nodes[_nodeCount++] = i;
            }
            if (!headflag[i] && tailflag[i]) {
                // This is a root node
                if (_rootCount == _roots.length) {
                    int[] temp = new int[_roots.length*2];
                    for (int j = 0; j < _roots.length; j++) {
                        temp[j] = _roots[j];
                    }
                    Arrays.fill(temp, _roots.length, temp.length-1, -1);
                    _roots = temp;
                }
                _roots[_rootCount++] = i;
            }
        }
    }

    /** Return the number of edges leaving the given node.
     */
    public int getEdgeCount (int node) {
        return _edgeCounts[node];
    }

    /** Return the array of edges leaving the given node. The
     * result must not be modified. Result is null if the
     * node has no edges.
     */
    public int[] getEdges (int node) {
        return _edges[node];
    }

    /** Return the number of roots of the graph
     */
    public int getRootCount () {
        return _rootCount;
    }

    /** Return the array of roots of the graph. The
     * result must not be modified. Result is null if the
     * graph has no roots.
     */
    public int[] getRoots () {
        return _roots;
    }

    /** Return the number of nodes in the graph
     */
    public int getNodeCount () {
        return _nodeCount;
    }

    /** Return the array of nodes in the graph. The
     * result must not be modified.
     */
    public int[] getNodes () {
        return _nodes;
    }

    /** Return the number of successors of the given node.
     */
    public int getSuccessorCount (int node) {
        return _successorCounts[node];
    }

    /** Return the array of successors of the given node. The
     * result must not be modified. Result is null if the
     * node has no successors. The successor array may contain
     * the same node more than once (if there is more than one
     * edge to that node).
     */
    public int[] getSuccessors (int node) {
        return _successors[node];
    }
}
