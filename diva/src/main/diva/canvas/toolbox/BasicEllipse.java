/*
 * $Id: BasicEllipse.java,v 1.4 2001/07/22 22:00:44 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.canvas.toolbox;

import java.awt.BasicStroke;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;

/** A figure that displays as an ellipse. This is a convenience class
 * for creating ellipses. It inherits from BasicFigure, and so contains
 * a single Ellipse2D as its shape. It provides a useful set of
 * constructors.
 *
 * @version	$Revision: 1.4 $
 * @author 	John Reekie
 */
public class BasicEllipse extends BasicFigure {

    /** Create a new ellipse with the given ellipse shape, a
     * unit-width continuous stroke and no paint pattern.
     */
    public BasicEllipse (Ellipse2D ellipse) {
        super(ellipse);
    }

    /** Create a new ellipse with the given origin and size, a
     * unit-width continuous stroke and no paint pattern.
     */
    public BasicEllipse (double x, double y, double width, double height) {
        super(new Ellipse2D.Double(x,y,width,height));
    }

    /** Create a new ellipse with the given origin, size, and
     * fill paint. It has no outline.
     */
    public BasicEllipse (
			 double x, double y, double width, double height,
			 Paint fill) {
        super(new Ellipse2D.Double(x,y,width,height),fill);
    }
    
    /** Create a new ellipse with the given origin, size, and
     * outline width. It has no fill.
     */
    public BasicEllipse (
			 double x, double y, double width, double height,
			 float lineWidth) {
        super(new Ellipse2D.Double(x,y,width,height),lineWidth);
    }
    
    /** Create a new ellipse with the given origin, size, fill, and
     * outline width.
     */
    public BasicEllipse (
			 double x, double y, double width, double height,
			 Paint fill,
			 float lineWidth) {
        super(new Ellipse2D.Double(x,y,width,height),fill,lineWidth);
    }
}


