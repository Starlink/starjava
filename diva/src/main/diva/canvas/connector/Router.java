/*
 * $Id: Router.java,v 1.2 2000/05/02 00:43:24 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.canvas.connector;

import diva.canvas.Figure;
import java.awt.Shape;

/** A Router is an object that can be used to help a connector
 * route itself. Specific implementations of Router are used
 * by connectors according to the Shape that they use to draw
 * themselves.
 *
 * @version $Revision: 1.2 $
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @author  John Reekie (johnr@eecs.berkeley.edu)
 */
public interface Router {

    /** Reroute the given Shape, given that the head site moved.
     * The router can assume that the tail site has not moved.
     * The shape is modified by the router.
     */
    public void rerouteHead (Connector c, Shape s);

    /** Reroute the given Shape, given that the tail site moved.
     * The router can assume that the head site has not moved.
     * The shape is modified by the router.
     */
    public void rerouteTail (Connector c, Shape s);

    /** Reroute the given shape, given that both the head the tail
     * sites moved. The shape is modified by the router.
     */
    public void reroute (Connector c, Shape s);

    /** Route the given connector, returning a shape of the
     * appropriate type that it can used to draw itself with.
     */
    public Shape route (Connector c);
}

