/*
 * $Id: BasicFigure.java,v 1.27 2002/09/26 10:35:13 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
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

import diva.util.java2d.ShapeUtilities;

/** A BasicFigure is one that contains a single instance of
 *  Shape. The figure can have a fill with optional compositing (for
 *  translucency), and a stroke with a different fill. With this
 *  class, simple objects can be created on-the-fly simply by passing
 *  an instance of java.awt.Shape to the constructor. This class
 *  is  mainly intended for use for closed shapes -- for open shapes,
 *  use the PathFigure class. For more complex Figures, use the
 *  VectorFigure class.
 *
 * @version	$Revision: 1.27 $
 * @author 	John Reekie
 * @author      Nick Zamora
 */
public class BasicFigure extends AbstractFigure implements ShapedFigure {

    /** The shape of this figure
     */
    private Shape _shape;
    
    /** The paint for the fill.
     */
    private Paint _fillPaint;

    /** The stroke.
     */
    private Stroke _stroke;

    /** The stroke paint.
     */
    private Paint _strokePaint;

     /** The color compositing operator
     */
    private Composite _composite = AlphaComposite.SrcOver; // opaque

    /** Create a new figure with the given shape. The figure, by
     *  default, has a unit-width continuous black outline and no fill.
     */
    public BasicFigure (Shape shape) {
        _shape = shape;
        _stroke = ShapeUtilities.getStroke(1);
	_strokePaint = Color.black;
    }

    /** Create a new figure with the given shape and outline width.
     * It has no fill. The default outline paint is black.
     *
     * @deprecated
     */
    public BasicFigure (Shape shape, int lineWidth) {
        _shape = shape;
        _stroke = ShapeUtilities.getStroke(lineWidth);
	_strokePaint = Color.black;
    }

    /** Create a new figure with the given shape and outline width.
     * It has no fill. The default outline paint is black.
     */
    public BasicFigure (Shape shape, float lineWidth) {
        _shape = shape;
        _stroke = ShapeUtilities.getStroke(lineWidth);
	_strokePaint = Color.black;
    }

    /** Create a new figure with the given paint pattern. The figure,
     *  by default, has no stroke.
     */
    public BasicFigure (Shape shape, Paint fill) {
        _shape = shape;
        _fillPaint = fill;
    }

    /** Create a new figure with the given paint pattern and line width.
     */
    public BasicFigure (Shape shape, Paint fill, float lineWidth) {
        _shape = shape;
        _fillPaint = fill;
	_stroke = ShapeUtilities.getStroke(1);
        _strokePaint = Color.black;
    }

    /** Get the bounding box of this figure. This method overrides
     * the inherited method to take account of the thickness of
     * the stroke, if there is one.
     */
    public Rectangle2D getBounds () {
        if (_stroke == null) {
            return _shape.getBounds2D();
        } else {
	    return ShapeUtilities.computeStrokedBounds(_shape, _stroke);
        }
    }

    /**
     * Get the compositing operator
     */
    public Composite getComposite() {
        return _composite;
    }

    /** Get the dash array. If the stroke is not a BasicStroke
     * then null will always be returned.
     */
    public float[] getDashArray () {
        if (_stroke instanceof BasicStroke) {
            return ((BasicStroke) _stroke).getDashArray();
        } else {
            return null;
        }
    }

    /** Get the line width. If the stroke is not a BasicStroke
     * then 1.0 will always be returned.
     */
    public float getLineWidth () {
        if (_stroke instanceof BasicStroke) {
            return ((BasicStroke) _stroke).getLineWidth();
        } else {
            return 1.0f;
        }
    }

    /**
     * Get the fill paint
     */
    public Paint getFillPaint() {
        return _fillPaint;
    }

    /** Get the shape of this figure.
     */
    public Shape getShape () {
        return _shape;
    }

    /** Get the paint used to stroke this figure
     */
    public Paint getStrokePaint () {
        return _strokePaint;
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
        boolean hit = false;
        if (_fillPaint != null) {
            hit = _shape.intersects(r);
        }
        if (!hit && _stroke != null && _strokePaint != null) {
            hit = hit || ShapeUtilities.intersectsOutline(r, _shape);
        }
        return hit;
    }

    /** Paint the figure. The figure is redrawn with the current
     *  shape, fill, and outline.
     */
    public void paint (Graphics2D g) {
        if (!isVisible()) {
             return;
        }
        if (_fillPaint != null) {
	    g.setPaint(_fillPaint);
	    g.setComposite(_composite);
	    g.fill(_shape);
        }
        if (_stroke != null && _strokePaint != null) {
	    g.setStroke(_stroke);
	    g.setPaint(_strokePaint);
	    g.draw(_shape);
        }
    }

    /** Set the compositing operation for this figure.
     */
    public void setComposite (AlphaComposite c) {
        _composite = c;
	repaint();
    }

   /** Set the dash array of the stroke. The existing stroke will
    * be removed, but the line width will be preserved if possible.
    */
    public void setDashArray (float dashArray[]) {
	repaint();
        if (_stroke instanceof BasicStroke) {
            _stroke = new BasicStroke(
                    ((BasicStroke) _stroke).getLineWidth(),
                    ((BasicStroke) _stroke).getEndCap(),
                    ((BasicStroke) _stroke).getLineJoin(),
                    ((BasicStroke) _stroke).getMiterLimit(),
                    dashArray,
                    0.0f);
        } else {
            _stroke = new BasicStroke(
                    1.0f,
		    BasicStroke.CAP_SQUARE,
		    BasicStroke.JOIN_MITER,
		    10.0f,
                    dashArray,
                    0.0f);
        }
	repaint();
    }

    /**
     * Set the fill paint. If p is null then
     * the figure will not be filled.
     */
    public void setFillPaint(Paint p) {
	repaint();
        _fillPaint = p;
	repaint();
    }

    /** Set the line width. The existing stroke will
     * be removed, but the dash array will be preserved if possible.
     */
    public void setLineWidth (float lineWidth) {
	repaint();
        if (_stroke instanceof BasicStroke) {
            _stroke = new BasicStroke(
                    lineWidth,
                    ((BasicStroke) _stroke).getEndCap(),
                    ((BasicStroke) _stroke).getLineJoin(),
                    ((BasicStroke) _stroke).getMiterLimit(),
                    ((BasicStroke) _stroke).getDashArray(),
                    0.0f);
        } else {
             new BasicStroke(
		    lineWidth,
		    BasicStroke.CAP_SQUARE,
		    BasicStroke.JOIN_MITER,
		    10.0f,
		    null,
		    0.0f);
        }
	repaint();
    }

    /** Set the shape of this figure.
     */
    public void setShape (Shape s) {
        repaint();
        _shape = s;
        repaint();
    }

    /**
     * Set the stroke paint
     */
    public void setStrokePaint(Paint p) {
        _strokePaint = p;
	repaint();
    }

    /**
     * Set the stroke
     */
    public void setStroke(Stroke s) {
        repaint();
	_stroke = s;
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
        _shape = ShapeUtilities.transformModify(_shape, at);
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
        _shape = ShapeUtilities.translateModify(_shape, x, y);
	repaint();
    }
}
