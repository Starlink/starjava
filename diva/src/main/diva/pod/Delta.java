/*
 * $Id: Delta.java,v 1.2 2002/02/07 02:58:07 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.pod;

import diva.util.PropertyContainer;


/** A Delta is an incremental change in the data within some
 * protocol. This is the object that is passed between
 * receivers and transmitters, via a channel. The structure
 * of a Delta is highly-specialized to a particular protocol.
 * However, the following design principle should be observed
 * when designing a protocol and its delta:
 *
 * <P>
 * <blockquote> A Delta is able to encompass an arbitrary amount
 * of change.</blockquote>
 *
 * <p> To illustrate, a delta that encodes only, for example, addition
 * and deletion of child nodes (in the tree protocol), does not follow
 * this guideline: the deltas are too fine-grained. While it should be
 * possible to encode these atomic operations, it must also be
 * possible to encode larger operations, such as addition or
 * pruning of a whole subtree, or modification of attribute
 * over an arbitrary collection of nodes.
 *
 * <p>The reason for this is partly performance (it reduces the number
 * of objects that are passed across channels, a real concern in a
 * large visualization), but mostly a design sensibility that we
 * encourage you to absorb. Don't think in terms of primitive
 * operations on data, think in terms gross transformations on that
 * data. Your life will be better, you will feel more at ease with
 * yourself, and your cat will thank you.
 *
 * @version $Revision: 1.2 $
 * @author John Reekie
 * @rating Red
 */
public interface Delta  {

    /** Get the very first originating Pod of this delta.
     * Pods that receive this delta and pass it on must not change
     * the source, as it is needed to prevent circularities.
     */
    public Object getSource ();

    /** -- more-- ? **/
}
