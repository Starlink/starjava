/*
 * $Id: BasicFigure.java,v 1.23 2000/07/11 19:00:04 nzamor Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.canvas.toolbox;

import diva.canvas.Figure;
import diva.canvas.AbstractFigure;
import diva.canvas.interactor.ShapedFigure;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.RectangularShape;
import java.awt.geom.Rectangle2D;

import diva.util.java2d.AbstractPaintedGraphic;
import diva.util.java2d.PaintedGraphic;
import diva.util.java2d.PaintedShape;
import diva.util.java2d.PaintedPath;
import diva.util.java2d.ShapeUtilities;

/** A BasicFigure is one that contains a single instance of
 *  Shape. The figure can have a fill with optional compositing (for
 *  translucency), and a stroke with a different fill. With this
 *  class, simple objects can be created on-the-fly simply by passing
 *  an instance of java.awt.Shape to the constructor. This class
 *  is  mainly intended for use for closed shapes -- for open shapes,
 *  use the PathFigure class.
 *
 * @version	$Revision: 1.23 $
 * @author 	John Reekie
 * @author      Nick Zamora
 */
public class BasicFigure extends AbstractFigure implements ShapedFigure {

    /** The color compositing operator
     */
    private Composite _composite = AlphaComposite.SrcOver; // opaque
    
     /** The painted shape that we use to draw the connector.
     */
    private AbstractPaintedGraphic _paintedObject;

    /** Create a new figure with the given shape. The figure, by
     *  default, has a unit-width continuous black outline and no fill.
     */
    public BasicFigure (Shape shape) {
        super();
        _paintedObject = new PaintedShape(shape);
    }

    /** Create a new figure with the given shape and outline width.
     * It has no fill. The default outline paint is black.
     */
    public BasicFigure (Shape shape, float lineWidth) {
        super();
        _paintedObject = new PaintedShape(shape, null, lineWidth);
    }

    /** Create a new figure with the given painted shape.
     */
    public BasicFigure (AbstractPaintedGraphic painted_object) {
        super();
	_paintedObject = painted_object;
    }

    /** Create a new figure with the given paint pattern. The figure,
     *  by default, has no stroke.
     */
    public BasicFigure (Shape shape, Paint fill) {
        super();
        _paintedObject = new PaintedShape(shape, fill);
    }

    /** Create a new figure with the given paint pattern and outline width.
     * The default outline paint is black.
     */
    public BasicFigure (Shape shape, Paint fill, float lineWidth) {
        super();
        _paintedObject = new PaintedShape(shape, fill, lineWidth);
    }

    /** Get the bounding box of this figure. This method overrides
     * the inherited method to take account of the thickness of
     * the stroke, if there is one.
     */
    public Rectangle2D getBounds () {
        return _paintedObject.getBounds();
    }

    /** Get the color composition operator of this figure.
     */
    public Composite getComposite () {
        return _composite;
    }

    /** Get the fill paint pattern of this figure if this figure
     *	represents a shape with a fill paint pattern, otherwise
     *	return null.
     */
    public Paint getFillPaint () {
        if (_paintedObject instanceof PaintedShape) {
	    return ((PaintedShape)_paintedObject).fillPaint;
	}
	else {
	    return null;
	}
    }

    /** Get the line width of this figure.
     */
    public float getLineWidth () {
        return _paintedObject.getLineWidth();
    }

    /** Get the shape of this figure.
     */
    public Shape getShape () {
        return _paintedObject.shape;
    }

    /** Get the stroke of this figure.
     */
    public Stroke getStroke () {
        return _paintedObject.getStroke();
    }

    /** Get the stroke paint pattern of this figure.
     */
    public Paint getStrokePaint () {
        return _paintedObject.strokePaint;
    }

    /** Test if this figure intersects the given rectangle. If there
     * is a fill but no outline, then there is a hit if the shape
     * is intersected. If there is an outline but no fill, then the
     * area covered by the outline stroke is tested. If there
     * is both a fill and a stroke, the region bounded by the outside
     * edge of the stroke is tested. If there is neither a fill nor
     * a stroke, then return false. If the figure is not visible,
     * always return false.
     */
    public boolean hit (Rectangle2D r) {
        if (!isVisible()) {
             return false;
        }
        return _paintedObject.hit(r);
    }

    /** Paint the figure. The figure is redrawn with the current
     *  shape, fill, and outline.
     */
    public void paint (Graphics2D g) {
        if (!isVisible()) {
             return;
        }
        if (_composite != null) {
            g.setComposite(_composite);
        }
        _paintedObject.paint(g);
    }

    /** Set the color composition operator of this figure. If the
     * composite is set to null, then the composite will not be
     * changed when the figure is painted. By default, the composite
     * is set to opaque.
     */
    public void setComposite (Composite c) {
        _composite = c;
        repaint();
    }

    /** Set the fill paint pattern of this figure. The figure will be
     *  filled with this paint pattern. If no pattern is given, do not
     *  fill it.
     */
    public void setFillPaint (Paint p) {
        if (_paintedObject instanceof PaintedShape) {
	    ((PaintedShape)_paintedObject).fillPaint = p;
	}
	repaint();
    }

   /** Set the line width of this figure. If the width is zero,
    * then the stroke will be removed.
    */
    public void setLineWidth (float lineWidth) {
      repaint();
      _paintedObject.setLineWidth(lineWidth);
      repaint();
    }

    /** Set the shape of this figure.
     */
    public void setShape (Shape s) {
        repaint();
        _paintedObject.shape = s;
        repaint();
    }

    /** Set the stroke of this figure.
     */
    public void setStroke (BasicStroke s) {
        repaint();
        _paintedObject.stroke = s;
        repaint();
    }

    /** Set the stroke paint pattern of this figure.
     */
    public void setStrokePaint (Paint p) {
        _paintedObject.strokePaint = p;
	repaint();
    }

    /** Transform the figure with the supplied transform. This can be
     * used to perform arbitrary translation, scaling, shearing, and
     * rotation operations. As much as possible, this method attempts
     * to preserve the type of the shape: if the shape of this figure
     * is an of RectangularShape or Polyline, then the shape may be
     * modified directly. Otherwise, a general transformation is used
     * that loses the type of the shape, converting it into a
     * GeneralPath.
     */
    public void transform (AffineTransform at) {
        repaint();
        _paintedObject.shape = ShapeUtilities.transformModify(
                _paintedObject.shape, at);
	repaint();
    }

    /** Translate the figure with by the given distance.
     * As much as possible, this method attempts
     * to preserve the type of the shape: if the shape of this figure
     * is an of RectangularShape or Polyline, then the shape may be
     * modified directly. Otherwise, a general transformation is used
     * that loses the type of the shape, converting it into a
     * GeneralPath.
     */
    public void translate (double x, double y) {
	repaint();
        _paintedObject.shape = ShapeUtilities.translateModify(
                _paintedObject.shape, x, y);
	repaint();
    }
}

