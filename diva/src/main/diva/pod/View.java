/*
 * $Id: View.java,v 1.2 2002/02/07 02:58:07 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.pod;

import diva.util.PropertyContainer;

import java.util.HashMap;


/** A View displays data in the Layout protocol. This is the
 * only recommended means by which anything be placed on
 * a computer screen. Pods must never create a graphical
 * object.
 *
 * <p>
 * Although Views can only understand the Layout protocol,
 * their interpretion of the data may differ wildly. A View
 * is not a "general-purpose data viewer" (although a default
 * one is provided in this package). Rather, it is simple
 * the root abstraction for the various data viewers and
 * editors that can be used to display and manipulate Pod
 * data.
 *
 * <p>
 * Views can have string-named properties attached to them.
 *
 * @version $Revision: 1.2 $
 * @author John Reekie
 * @rating Red
 */
public interface View extends PropertyContainer {

    /** Get a receiver in the Layout protocol from the View.  The
     * properties hash map, if supplied, can be used in a
     * protocol-specific way to place constraints on the selection of
     * a receiver.
     */
    public Receiver getReceiver (HashMap properties);
}
