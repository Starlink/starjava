/*
 * $Id: PathManipulator.java,v 1.14 2001/07/24 06:33:01 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.canvas.interactor;

import diva.canvas.Site;
import diva.canvas.DamageRegion;
import diva.canvas.Figure;
import diva.canvas.FigureDecorator;
import diva.canvas.CanvasUtilities;

import diva.canvas.event.MouseFilter;
import diva.canvas.event.LayerEvent;

import diva.canvas.interactor.Interactor;
import diva.canvas.interactor.DragInteractor;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import java.util.Iterator;
import java.util.ArrayList;

/**
 * A manipulator which attaches grab handles to the sites
 * of the child figure.  It renders the grab handles and gives them a
 * chance to intercept picks.
 *
 * @author John Reekie      (johnr@eecs.berkeley.edu)
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.14 $
 */
public class PathManipulator extends Manipulator {

    /** The path geometry
     */
    private PathGeometry _geometry;

    /**
     * Construct a new manipulator that uses rectangular grab-handles.
     */
    public PathManipulator() {
        this(new BasicGrabHandleFactory());
    }

    /**
     * Construct a new manipulator using the given grab-handle factory. 
     */
    public PathManipulator(GrabHandleFactory f) {
        setGrabHandleFactory(f);
	setHandleInteractor(new Resizer());
    }

    /** Return the geometry of this manipulator
     */
    private PathGeometry getGeometry () {
        return _geometry;
    }

    /** Create a new instance of this manipulator. The new
     * instance will have the same grab handle, and interactor
     * for grab-handles, as this one.
     */
    public FigureDecorator newInstance (Figure f) {
        PathManipulator m = new PathManipulator();
        m.setGrabHandleFactory(this.getGrabHandleFactory());
        m.setHandleInteractor(this.getHandleInteractor());
        return m;
    }

    /** Refresh the geometry.
     */
    public void refresh () {
        if (_geometry != null) {
            _geometry.setShape(getChild().getShape());
        }
    }

    /** Set the child figure. If we have any grab-handles, lose them.
     * Then get a path geometry object set on this figure (the
     * manipulator, not the child) and create grab-handles on it.
     */
    public void setChild (Figure f) {
        super.setChild(f);
        clearGrabHandles();
        _geometry = null;

        // Process new child
        Figure child = getChild();
        if (child != null) {
            // Check that we can mess with this figure
            if (!(child instanceof ShapedFigure)) {
                throw new IllegalArgumentException(
                        "PathManipulator can only decorate a ShapedFigure");
            }
            // Create the geometry defining the sites
            _geometry = new PathGeometry(this, getChild().getShape());
            Iterator i = _geometry.vertices();
	    GrabHandle g = null;
            while (i.hasNext()) {
                // Create a grab handle and set up the interactor.
                // Unless it's a close segment, in which case we ignore it.
                Site site = (Site)i.next();
                if ( !(site instanceof PathGeometry.CloseSegment)) {
                    g = getGrabHandleFactory().createGrabHandle(site);
                    g.setParent(this);
                    g.setInteractor(getHandleInteractor());
                    addGrabHandle(g);
                }
            }
        }
        // repaint();
    }

    ///////////////////////////////////////////////////////////////////////
    //// Resizer

    /** An interactor class that changes a vertex of the child figure
     *  and triggers a repaint.
     */
    private static class Resizer extends DragInteractor {
        /** Translate the grab-handle
         */
        public void translate (LayerEvent e, double x, double y) {
            // Translate the grab-handle, resizing the geometry
            GrabHandle g = (GrabHandle) e.getFigureSource();
            g.translate(x, y);

            // Transform the child -- could be made more efficient?...
            PathManipulator parent = (PathManipulator) g.getParent();
            PathGeometry geometry = parent.getGeometry();

           ((ShapedFigure)parent.getChild()).setShape(geometry.getShape());
        }
    }
}


