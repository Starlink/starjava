/*
 * $Id: AbstractConnector.java,v 1.19 2000/08/16 20:31:03 neuendor Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.canvas.connector;

import diva.canvas.Figure;
import diva.canvas.AbstractFigure;
import diva.canvas.Site;
import diva.canvas.toolbox.LabelFigure;

import diva.util.java2d.PaintedPath;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;

/** An abstract implementation of Connector. The implementation
 * provides default implementations of all routing methods except
 * for route(). It also provides a set of methods for setting
 * the appearance of the connector, such as line width, dashes,
 * and color. To do so, it uses an instance of PaintedPath, so
 * see that class for a more detailed description of the
 * paint- and stroke-related methods.
 *
 * @version $Revision: 1.19 $
 * @author  John Reekie (johnr@eecs.berkeley.edu)
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 */
public abstract class AbstractConnector extends AbstractFigure implements Connector {

    /** The head end
     */
    private ConnectorEnd _headEnd = null;

    /** The tail end
     */
    private ConnectorEnd _tailEnd = null;

    /** The head site
     */
    private Site _headSite = null;

    /** The tail site
     */
    private Site _tailSite = null;

    /** The label figure
     */
    private LabelFigure _labelFigure;

    /** The painted shape that we use to draw the connector.
     */
    private PaintedPath _paintedPath;

    /** Create a new connector between the given sites.
     */
    public AbstractConnector (Site tail, Site head) {
        _tailSite = tail;
        _headSite = head;
        _paintedPath = new PaintedPath(null); // shape is set in subclass
    }

    /** Get the bounding box of this connector.
     */
    public Rectangle2D getBounds () {
        Rectangle2D bounds = (Rectangle2D)_paintedPath.getBounds().clone();
        if (_headEnd != null) {
            Rectangle2D.union(bounds, _headEnd.getBounds(), bounds);
        }
        if (_tailEnd != null) {
            Rectangle2D.union(bounds, _tailEnd.getBounds(), bounds);
        }
        if (_labelFigure != null) {
            Rectangle2D.union(bounds, _labelFigure.getBounds(), bounds);
        }
        return bounds;
    }

    /** Get the dash array used to stroke the connector.
     */
    public float[] getDashArray () {
        return _paintedPath.getDashArray();
    }

    /** Get the object drawn at the head end of the connector, if there
     * is one. 
     */
    public ConnectorEnd getHeadEnd () {
        return _headEnd;
    }

   /** Get the site that marks the "head" of the connector.
     */
    public Site getHeadSite () {
        return _headSite;
    }

   /** Get the figure that display's this connector's label.
    * This may be null.
    */
    public LabelFigure getLabelFigure () {
        return _labelFigure;
    }

   /** Get the line width of this connector.
    */
    public float getLineWidth () {
        return _paintedPath.getLineWidth();
    }

    /** Get the PaintedPath that paints this connector.
     */
    protected PaintedPath getPaintedPath () {
        return _paintedPath;
    }

    /** Get the object drawn at the tail end of the connector, if there
     * is one. 
     */
    public ConnectorEnd getTailEnd () {
        return _tailEnd;
    }

    /** Get the outline shape of this connector.
     */
    public Shape getShape () {
        return _paintedPath.shape;
    }

    /** Get the stroke of this connector.
     */
    public Stroke getStroke () {
        return _paintedPath.stroke;
    }

    /** Get the stroke paint pattern of this connector.
     */
    public Paint getStrokePaint () {
        return _paintedPath.strokePaint;
    }

    /** Get the site that marks the "tail" of the connector.
     */
    public Site getTailSite () {
        return _tailSite;
    }

    /** Inform the connector that the head site has moved.
     * This default implementation simply calls reroute().
     */
    public void headMoved () {
        repaint();
        reroute();
        repaint();
    }

    /** Test if this connector is hit by the given rectangle.
     * If the connector is not visible, always return false, otherwise
     * check to see if the rectangle intersects the path of the connector, 
     * either of its ends, or the label.
     */
    public boolean hit (Rectangle2D r) {
        if (!isVisible()) {
             return false;
        }
	
	boolean hit = _paintedPath.hit(r);
	if (_labelFigure != null) {
            hit = hit || _labelFigure.hit(r);
        }

        // Do the ends too. Does ConnectorEnd needs a proper hit() method?
        if (_headEnd != null) {
            hit = hit || r.intersects(_headEnd.getBounds());
        }
        if (_tailEnd != null) {
            hit = hit || r.intersects(_tailEnd.getBounds());
        }
        return hit;
    }

    /** Test if this connector intersects the given rectangle. This default
     *  implementation checks to see if the rectangle intersects with the
     *  path of the connector, the label, or either of the connector ends.
     */
    public boolean intersects (Rectangle2D r) {
       	boolean hit = _paintedPath.intersects(r);
	if (_labelFigure != null) {
            hit = hit || _labelFigure.intersects(r);
        }

        // Do the ends too. Does ConnectorEnd needs a proper hit() method?
        if (_headEnd != null) {
            hit = hit || r.intersects(_headEnd.getBounds());
        }
        if (_tailEnd != null) {
            hit = hit || r.intersects(_tailEnd.getBounds());
        }
	return hit;
    }
    
    /** Paint the connector.
     * This call is forwarded to the internal PaintedPath object,
     * and then the connector ends are drawn, if they exist.
     */
    public void paint (Graphics2D g) {
	_paintedPath.paint(g);
        if (_headEnd != null) {
            _headEnd.paint(g);
        }
        if (_tailEnd != null) {
            _tailEnd.paint(g);
        }
        if (_labelFigure != null) {
            _labelFigure.paint(g);
        }
    }

    /** Tell the connector to reposition its label if it has one.
     * This is an abstract method that must be implemented by
     * subclasses. In general, implementations of the routing
     * methods will also call this method.
     */
    public abstract void repositionLabel ();

    /** Tell the connector to re-route itself. This default implementation
     * simply calls route(). In general, this method should be overridden
     * to perform this more efficiently.
     */
    public void reroute () {
	route();
    }

    /** Tell the connector to route itself completely,
     * using all available information. 
     */
    public abstract void route ();

   /** Set the dash array of this connector.
    * This call is forwarded to the internal PaintedPath object.
    */
    public void setDashArray (float dashArray[]) {
        _paintedPath.setDashArray(dashArray);
        repaint();
    }

    /**
     * Set the object drawn at the head end of the connector.
     */
    public void setHeadEnd (ConnectorEnd e) {
 	// We can't just call reroute, because then route() doesn't have a
	// chance to set the normal of the end before painting it.
	repaint();
	_headEnd = e;
	repaint();
	reroute();
    }

    /** Set the site that marks the "head" of the connector,
     * and call headMoved();
     */
    public void setHeadSite (Site s) {
        _headSite = s;
        headMoved();
    }

    /** Set the LabelFigure of this connector. If there is no label
     *  figure currently, one is created and placed on the connector.
     */
    public void setLabelFigure (LabelFigure label) {
        _labelFigure = label;
        repositionLabel();
    }

   /** Set the line width of this connector.
    * This call is forwarded to the internal PaintedPath object.
    */
    public void setLineWidth (float lineWidth) {
        repaint();
        _paintedPath.setLineWidth(lineWidth);
        repaint();
    }

    /** Set the stroke of this connector.
     * This call is forwarded to the internal PaintedPath object.
    */
    public void setStroke (Stroke s) {
        _paintedPath.setStroke(s);
        repaint();
    }
 
    /** Set the stroke paint pattern of this connector.
     */
    public void setStrokePaint (Paint p) {
        _paintedPath.strokePaint = p;
	repaint();
    }

    /**
     * Set the object drawn at the tail end of the connector.
     */
    public void setTailEnd (ConnectorEnd e) {
	// We can't just call reroute, because then route() doesn't have a
	// chance to set the normal of the end before painting it.
	repaint();
	_tailEnd = e;
	repaint();
	reroute();
    }

    /** Set the site that marks the "tail" of the connector.
     */
    public void setTailSite (Site s) {
        _tailSite = s;
        tailMoved();
    }

    /** Inform the connector that the tail site has moved.
     * This default implementation simply calls reroute().
     */
    public void tailMoved () {
        repaint();
        reroute();
        repaint();
    }

    /** Transform the connector. This method is ignored, since
     * connectors are defined by the head and tail sites.
     */
    public void transform (AffineTransform at) {
        // do nothing
    }

    /** Translate the connector. This method must be implemented, since
     * controllers may wish to translate connectors when the
     * sites at both ends are moved the same distance.
     */
    public abstract void translate (double x, double y);
}

