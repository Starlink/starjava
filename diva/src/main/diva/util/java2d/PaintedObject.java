/*
 * $Id: PaintedObject.java,v 1.3 2000/07/17 18:13:17 neuendor Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.util.java2d;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

/** The interface for a set of utility classes that paint shapes
 * or other kinds of graphical objects. The purpose of these classes
 * is to provide a simple interface for basic graphical drawing
 * operations. Generally, they combine a number of different
 * objects from the Java2D API in the most commonly useful way.
 *
 * @version	$Revision: 1.3 $
 * @author 	John Reekie
 */
public interface PaintedObject {

    /** Get the bounding box of the object when painted. Implementations
     * of this method should take account of the thickness of the
     * stroke, if there is one.
     */
    public Rectangle2D getBounds ();

    /** Paint the shape. Implementations are expected to redraw
     * the entire object. Whether or not the paint overwrites
     * fields in the graphics context such as the current
     * paint, stroke, and composite, depends on the implementing class.
     */
    public void paint (Graphics2D g);
}

