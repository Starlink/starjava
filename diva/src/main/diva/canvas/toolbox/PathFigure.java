/*
 * $Id: PathFigure.java,v 1.5 2000/09/11 20:49:56 neuendor Exp $
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

import diva.util.java2d.PaintedPath;
import diva.util.java2d.ShapeUtilities;

/** A PathFigure is one that contains a single instance of
 *  Shape. The figure can have a fill with optional compositing (for
 *  translucency), and a stroke with a different fill. With this
 *  class, simple objects can be created on-the-fly simply by passing
 *  an instance of java.awt.Shape to the constructor. This class
 *  is mainly intended for use for open shapes (without fill).
 *  For filled shapes, use the BasicFigure class.
 *
 * @version	$Revision: 1.5 $
 * @author 	John Reekie
 */
public class PathFigure extends AbstractFigure implements ShapedFigure {

    /** The color compositing operator
     */
    private Composite _composite = AlphaComposite.SrcOver; // opaque
    
     /** The painted shape that we use to draw the connector.
     */
    private PaintedPath _paintedPath;

    /** Create a new figure with the given shape. The figure, by
     *  default, is stroked with a unit-width continuous black stroke.
     */
    public PathFigure (Shape shape) {
        super();
        _paintedPath = new PaintedPath(shape);
    }

    /** Create a new figure with the given shape and width.
     * The default paint is black.
     */
    public PathFigure (Shape shape, float lineWidth) {
        super();
        _paintedPath = new PaintedPath(shape, lineWidth);
    }

    /** Create a new figure with the given paint and width.
     */
    public PathFigure (Shape shape, Paint paint, float lineWidth) {
        super();
        _paintedPath = new PaintedPath(shape, lineWidth);
        _paintedPath.strokePaint = paint;
    }

    /** Get the bounding box of this figure. This method overrides
     * the inherited method to take account of the thickness of
     * the stroke.
     */
    public Rectangle2D getBounds () {
        return _paintedPath.getBounds();
    }

    /** Get the color composition operator of this figure.
     */
    public Composite getComposite () {
        return _composite;
    }

    /** Get the line width of this figure.
     */
    public float getLineWidth () {
        return _paintedPath.getLineWidth();
    }

    /** Get the shape of this figure.
     */
    public Shape getShape () {
        return _paintedPath.shape;
    }

    /** Get the stroke of this figure.
     */
    public Stroke getStroke () {
        return _paintedPath.getStroke();
    }

    /** Get the stroke paint of this figure.
     */
    public Paint getStrokePaint () {
        return _paintedPath.strokePaint;
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
        return _paintedPath.hit(r);
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
        _paintedPath.paint(g);
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

   /** Set the line width of this figure. If the width is zero,
    * then the stroke will be removed.
    */
    public void setLineWidth (float lineWidth) {
        repaint();
        _paintedPath.setLineWidth(lineWidth);
        repaint();
    }

    /** Set the shape of this figure.
     */
    public void setShape (Shape s) {
        repaint();
        _paintedPath.shape = s;
        repaint();
    }

    /** Set the stroke of this figure.
     */
    public void setStroke (BasicStroke s) {
        repaint();
        _paintedPath.stroke = s;
        repaint();
    }

    /** Set the stroke paint of this figure.
     */
    public void setStrokePaint (Paint p) {
        _paintedPath.strokePaint = p;
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
        _paintedPath.shape = ShapeUtilities.transformModify(
                _paintedPath.shape, at);
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
        _paintedPath.shape = ShapeUtilities.translateModify(
                _paintedPath.shape, x, y);
	repaint();
    }
}

