/*
 * $Id: PathFigure.java,v 1.9 2002/09/26 10:26:51 johnr Exp $
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

/** A PathFigure is one that contains a single instance of
 *  Shape. The figure can have a fill with optional compositing (for
 *  translucency), and a stroke with a different fill. With this
 *  class, simple objects can be created on-the-fly simply by passing
 *  an instance of java.awt.Shape to the constructor. This class
 *  is mainly intended for use for open shapes (without fill).
 *  For filled shapes, use the BasicFigure class, and for more complex
 *  figures, use VectorFigure or create a custom Figure class.
 *
 * @version	$Revision: 1.9 $
 * @author 	John Reekie
 * @author Peter Draper (p.w.draper@durham.ac.uk)
 */ 
public class PathFigure extends AbstractFigure implements ShapedFigure {

    /** The stroke.
     */
    private Stroke _stroke;

    /** The stroke paint.
     */
    private Paint _paint;

     /** The shape of the figure
     */
    private Shape _shape;

    /** The color compositing operator
     */
    private Composite _composite = AlphaComposite.SrcOver; // opaque

    /** Create a new figure with the given shape. The figure, by
     *  default, is stroked with a unit-width continuous black stroke.
     */
    public PathFigure (Shape shape) {
        _shape = shape;
        _stroke = ShapeUtilities.getStroke(1);
	_paint = Color.black;
    }

    /** Create a new figure with the given shape and width.
     * The default paint is black.
     */
    public PathFigure (Shape shape, float lineWidth) {
        _shape = shape;
        _stroke = ShapeUtilities.getStroke(lineWidth);
	_paint = Color.black;
    }

    /** Create a new figure with the given paint and width.
     */
    public PathFigure (Shape shape, Paint paint, float lineWidth) {
        _shape = shape;
        _stroke = ShapeUtilities.getStroke(lineWidth);
	_paint = paint;
    }

    /** Get the bounding box of this figure. This method overrides
     * the inherited method to take account of the thickness of
     * the stroke.
     */
    public Rectangle2D getBounds () {
	return ShapeUtilities.computeStrokedBounds(_shape, _stroke);
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

    /** Get the line width of this figure. If the stroke is not a BasicStroke
     * then 1.0 will always be returned.
     */
    public float getLineWidth () {
        if (_stroke instanceof BasicStroke) {
            return ((BasicStroke) _stroke).getLineWidth();
        } else {
            return 1.0f;
        }
    }

    /** Get the shape of this figure.
     */
    public Shape getShape () {
        return _shape;
    }

    /** Get the stroke of this figure.
     */
    public Stroke getStroke () {
        return _stroke;
    }

    /** Get the stroke paint of this figure.
     */
    public Paint getStrokePaint () {
        return _paint;
    }

    /**
     * Get the compositing operator
     */
    public Composite getComposite() {
        return _composite;
    }

    /** Test if this figure intersects the given rectangle. 
     * If the figure is not visible, always return false.
     */
    public boolean hit (Rectangle2D r) {
        if (!isVisible()) {
             return false;
        }
        return  ShapeUtilities.intersectsOutline(r, _shape);
    }

    /** Paint the figure. The figure is redrawn with the current
     *  shape, fill, and outline.
     */
    public void paint (Graphics2D g) {
        if (!isVisible()) {
             return;
        }
	g.setStroke(_stroke);
        g.setPaint(_paint);
        g.setComposite(_composite);
        g.draw(_shape);
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

    /** Set the stroke of this figure.
     */
    public void setStroke (BasicStroke s) {
        repaint();
        _stroke = s;
        repaint();
    }

    /** Set the stroke paint of this figure.
     */
    public void setStrokePaint (Paint p) {
	repaint();
        _paint = p;
	repaint();
    }

    /** Set the compositing operation for this figure.
     */
    public void setComposite (AlphaComposite c) {
        _composite = c;
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


