/*
 * $Id: FigureContainer.java,v 1.22 2000/05/02 00:43:15 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.canvas;

import java.awt.geom.Rectangle2D;
import java.util.Iterator;

/** The FigureContainer interface is implemented by any
 * visible component that can contain figures. It includes
 * all of the methods in VisibleComponent and FigureSet,
 * and adds methods related to containment of a known
 * and finite set of figures.
 *
 * @version	$Revision: 1.22 $
 * @author John Reekie
 * @rating Yellow
 */
public interface FigureContainer extends FigureSet, VisibleComponent {

    /** Test if this container contains the given figure. As a general
     * rule, the implementation of this method is not required to be
     * efficient -- O(n) in the length of the list is acceptable.
     * Clients should note that, in general, a much better way
     * of making this same test is to check if the parent of the figure
     * is the same object as this container.
     */
    public boolean contains (Figure f);

    /** Decorate a child figure, replacing the
     * child figure with the decorator.
     */
    public void decorate (Figure f, FigureDecorator d);
 
    /** Return the number of figures in this container.
     */
    public int getFigureCount ();

    /** Given a rectangle, return the top-most descendent figure
     * that hits it. Otherwise, return null. Implementors
     * should not call their own hit() method, but only
     * those of their children.
     *
     * <P>Note that a region is given instead of a point so
     * that "pick halo" can be implemented. The region should
     * not have zero size, or no figure will be hit.
     */
    public Figure pick (Rectangle2D region);

    /** Remove a figure from the given decorator and add
     * it back into this container.
     */
    public void undecorate (FigureDecorator d);
}

