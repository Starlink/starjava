/*
 * $Id: LWEdge.java,v 1.2 2002/01/15 04:31:11 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.pod.lwgraph;

/** The interface to be implemented for edge objects to be
 * used with the light-weight graph.
 *
 * @version $Revision: 1.2 $
 * @author John Reekie
 * @rating Red
 */
public interface LWEdge {

    /** Return the integer id of this edge.  Although not strictly necessary,
     * it is recommended that the default value returned by this method
     * be -1, in order to catch errors in graph usage.
     */
    public int getEdgeId ();

    /** Set the integer id of this edge. Clients can throw an exception
     * if the id has previously been set, in the reasonable assumption
     * that indicates a programming error.
     */
    public void setEdgeId (int id);
}
