/*
 * $Id: LightweightNetwork.java,v 1.5 2002/02/05 23:16:20 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.pod.lwgraph;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import diva.util.FilteredArrayIterator;
import diva.util.IteratorAdapter;

/** A light-weight network is an extension of light-weight graph,
 * and adds methods for dealing with explicit port objects.
 *
 * @version $Revision: 1.5 $
 * @author John Reekie
 * @rating Red
 */
public class LightweightNetwork extends LightweightGraph {

    // The initial space for port storage
    private static int INITPORTMAX = 64;
    private static int INITEDGEMAX = 64;

    // The current port id
    private int _portid = 0;

    // The array of head ports indexed by edge id
    private LWPort[] _headPorts = new LWPort[INITEDGEMAX];

    // The array of tail ports indexed by edge id
    private LWPort[] _tailPorts = new LWPort[INITEDGEMAX];

    // The array of port objects idnexed by port id
    private LWPort[] _ports = new LWPort[INITPORTMAX];

    // The array of port object parent nodes
    private LWNode[] _portParents = new LWNode[INITPORTMAX];

    /** Create a new, empty, light-weight network
     */
    public LightweightNetwork () {
	super();
    }

    /** Add a port to the graph, and give it the given node as its parent
     */
    public void addPort (LWNode node, LWPort port) {
	// Check size and grow if needed
	if (_portid == _ports.length) {
	    LWPort[] temp1 = new LWPort[_ports.length * 2];
	    LWNode[] temp2 = new LWNode[_ports.length * 2];
	    for (int i = 0; i < _ports.length; i++) {
		temp1[i] = _ports[i];
		temp2[i] = _portParents[i];
	    }
	    _ports = temp1;
	    _portParents = temp2;
	}
	
	int portid = _portid++;
	port.setPortId(portid);
	_ports[portid] = port;
	_portParents[portid] = node;
    }

    /** Add an edge to the graph. Override the superclass method
     * only for implementation reasons (to check and extend the \
     * length of some internal arrays if necessary).
     */
    public void addEdge (LWEdge edge) {
	super.addEdge(edge);

	// We may have to grow the head and tail port arrays here
	int edgeid = edge.getEdgeId();
	if (edgeid >= _headPorts.length) {
	    int i;
	    LWPort[] temp;
            int oldlength = _headPorts.length;
            int newlength = Math.max(oldlength * 2, edgeid);

	    temp = new LWPort[newlength];
	    for (i = 0; i < oldlength; i++) {
	    	temp[i] = _headPorts[i];
	    }
	    _headPorts = temp;

	    temp = new LWPort[newlength];
	    for (i = 0; i < oldlength; i++) {
	    	temp[i] = _tailPorts[i];
	    }
	    _tailPorts = temp;
	}
    }

    /** Connect the given tail and head ports using the given edge.
     * The ports must have previously been added to this network, as
     * must the edge. If for some reason you don't want
     * one end connected, use setHeadPort or setTailPort instead.
     */
    public void connect (LWEdge edge, LWPort tailport, LWPort headport) {
	int tailid = tailport.getPortId();
	int headid = headport.getPortId();
	int edgeid = edge.getEdgeId();

	LWNode tailnode = _portParents[tailid];
	LWNode headnode = _portParents[headid];

	connect(edge, tailnode, headnode);
 
	_tailPorts[edgeid] = tailport;
	_headPorts[edgeid] = headport;
    }

    /** Get the head port of the given edge, or null if there
     * isn't one.
     */
    public LWPort getHeadPort (LWEdge edge) {
	int edgeid = edge.getEdgeId();
	return _headPorts[edgeid];
    }

    /** Get the tail port of the given edge, or null if there
     * isn't one.
     */
    public LWPort getTailPort (LWEdge edge) {
	int edgeid = edge.getEdgeId();
	return _tailPorts[edgeid];
    }

    /** Return an iterator over the ports that belong to the given node.
     * This method is not particularly efficient. In general, a client
     * of this class will ahve their own implementation of LWNode that
     * has its own notion of ports, and that class would usually be
     * a better way of performaing an operation like this. The remove()
     * method is not supported.
     */
    public Iterator ports (LWNode node) {
            return new PortIterator(node);
    }

    /** Connect the given head port to an edge. To disconnect the edge
     * from the port, set the port to null.  The edge must have previously
     * been added to the graph.
     */
    public void setHeadPort (LWEdge edge, LWPort head) {
	int edgeid = edge.getEdgeId();
	if (getNode(getTopology().getHead(edgeid)) != _portParents[head.getPortId()]) {
	    setHeadNode(edge, _portParents[head.getPortId()]);
	}
	_headPorts[edgeid] = head;
    }

    /** Connect the given tail port to an edge. To disconnect the edge
     * from the port, set the port to null. The edge must have previously
     * been added to the graph.
     */
    public void setTailPort (LWEdge edge, LWPort tail) {
	int edgeid = edge.getEdgeId();
	if (getNode(getTopology().getTail(edgeid)) != _portParents[tail.getPortId()]) {
	    setTailNode(edge, _portParents[tail.getPortId()]);
	}
	_tailPorts[edgeid] = tail;
    }

    /** An iterator class for iterating over the ports belonging to a
     * node.  I tried to use an anonymous inner subclass of
     * diva.util.FilteredArrayIterator for this, but Java seemed to
     * have a weird bug where a reference outside the iterator (in
     * this case, to _portParents) would just throw a null pointer
     * exception (even though _portParents wasn't null... as far as I
     * could tell...)
     */
    private class PortIterator extends IteratorAdapter {

        int _lastindex = -1;
        int _nextindex = -1;
        LWNode _parent;

        /** Construct the iterator
         */
        public PortIterator(LWNode parent) {
            _parent = parent;
            advance();
        }

        /** Advance the next index to the next non-null element. Set it
         * to -1 if there are no more elements.
         */
        protected void advance() {
            _nextindex++;
            while (_nextindex < _portid) {
                LWPort p = _ports[_nextindex];
                if (p != null) {
                    if (_portParents[p.getPortId()] == _parent) {
                        break;
                    }
                }
                _nextindex++;
            }
            if (_nextindex == _portid) {
                _nextindex = -1;
            }
        }
    
        /** Return true if there are more non-null elements in the array.
         */
        public boolean hasNext() {
            return _nextindex >= 0;
        }
        
        /** Return the next non-null element in the array.
         */
        public Object next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more elements");
            }
            Object result = _ports[_nextindex];
            _lastindex = _nextindex;
            advance();
            return result;
        }
    }
}
