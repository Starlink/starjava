/*
 * $Id: BackgroundedCompositeFigure.java,v 1.4 1998/12/07 06:10:20 johnr Exp $
 *
 * Copyright (c) 1998 The Regents of the University of California.
 * All rights reserved.  See the file COPYRIGHT for details.
 */

package diva.canvas.toolbox;

import diva.canvas.Figure;
import diva.canvas.CompositeFigure;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A composite figure which has a background shape and
 * appropriate event-handling capabilities, behaving in
 * a way similar to PaneWrapper.
 *
 * @version $Revision: 1.4 $
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @rating  Red
 */
public class BackgroundedCompositeFigure extends CompositeFigure {
    /**
     * The figure to be displayed in the background.
     */
    private Figure _background;

    /**
     * Construct a backgrounded composite figure with
     * no background and no children.
     */
    public BackgroundedCompositeFigure() {
        this(null);
    }
    
    /**
     * Construct a backgrounded composite figure with the
     * given background and no children.
     */
    public BackgroundedCompositeFigure(Figure background) {
        super();
        setBackgroundFigure(background);
    }

    /**
     * Set the figure that is displayed in the background of this
     * figure and which handles the events that this figure's children
     * do not.
     */
    public void setBackgroundFigure(Figure background) {
        _background = background;
    }

    /**
     * Return the figure that is displayed in the background of this
     * figure and which handles the events that this figure's children
     * do not.
     */
    public Figure getBackgroundFigure() {
        return _background;
    }
    
    /**
     * Return a union of the bounding box of the children and the
     * bounding box of the background figure.
     */
    public Rectangle2D getBounds () {
        //FIXME: use caching.
        if(_background == null) {
            return super.getBounds();
        }
        else {
            Rectangle2D r1 = super.getBounds();
            Rectangle2D r2 = _background.getBounds();
            Rectangle2D rout = new Rectangle2D.Double();
            Rectangle2D.union(r1, r2, rout);
            return rout;
        }
    }


    /**
     * Paint this composite figure onto a 2D graphics object.  This
     * implementation pushes the transform context onto the transform
     * stack, and then paints all children.
     */
    public void paint (Graphics2D g) {
        //        debug("PAINT!");
        if(_background != null) {
            _background.paint(g);
        }
        super.paint(g);
    }

    /** Paint this composite figure onto a 2D graphics object, within
     * the given region.  If the figure is not visible, return
     * immediately. Otherwise paint all figures that overlap the given
     * region, from highest index to lowest index.
     */
    public void paint (Graphics2D g, Rectangle2D region) {
        //        debug("PAINT-region!");
        if(_background != null) {
            _background.paint(g, region);
        }
        super.paint(g, region);
    }

    private void debug(String s) {
        System.err.println(s);
    }

    /**
     * Get the picked figure. This method recursively traverses the
     * tree until it finds a figure that is "hit" by the region. Note
     * that a region is given instead of a point so that "hysteresis"
     * can be implemented. If no figure is hit, return null. Note that
     * the region should not have zero size, or no figure will be hit.
     */
    public Figure pick (Rectangle2D region) {
        Figure f = super.pick(region);
        if(f != null) {
            //  debug("picked child!");
            return f;
        }
        else if(_background != null) {
            //  debug("picked background!");
            Rectangle2D bb =_background.getBounds();
            double bx = bb.getX();
            double by = bb.getY();
            double bw = bb.getWidth();
            double bh = bb.getHeight();
            if(region.intersects(bx, by, bw, bh)) {
                return this;
            }
        }
        // debug("picked nothing!");
        return null;
    }

    /**
     * Transform this figure with the supplied transform.  This
     * implementation modifies the transformcontext.
     */
    public void transform (AffineTransform at) {
        repaint();
        super.transform(at);
        if(_background != null) {
            _background.transform(at);
        }
        repaint();
    }

     /**
      * Translate this figure the given distance.
      */
    public void translate (double x, double y) {
        repaint();
        super.translate(x,y);
        if(_background != null) {
            _background.translate(x, y);
        }
        repaint();
    }
}
