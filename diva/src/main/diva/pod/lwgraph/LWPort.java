/*
 * $Id: LWPort.java,v 1.2 2002/01/15 04:31:11 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.pod.lwgraph;

/** The interface to be implemented for ports that are used
 * used with the light-weight component network.
 *
 * @version $Revision: 1.2 $
 * @author John Reekie
 * @rating Red
 */
public interface LWPort {

    /** Return the integer id of this port. Although not strictly necessary,
     * it is recommended that the default value returned by this method
     * be -1, in order to catch errors in graph usage.
     */
    public int getPortId ();

    /** Set the integer id of this port. Clients can throw an exception
     * if the id has previsouly been set, in the reasonable assumption
     * that indicates a programming error.
     */
    public void setPortId (int id);
}
