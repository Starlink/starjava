/*
 * $Id: BoundsManipulator.java,v 1.17 2001/07/24 06:33:01 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.canvas.interactor;

import diva.canvas.DamageRegion;
import diva.canvas.Figure;
import diva.canvas.FigureDecorator;
import diva.canvas.Site;
import diva.canvas.CanvasUtilities;

import diva.canvas.connector.CenterSite;

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
 * A manipulator which attaches grab handles to the bounds
 * of the child figure.  It renders the grab handles and gives them a
 * chance to intercept picks.
 *
 * @author John Reekie      (johnr@eecs.berkeley.edu)
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version	$Revision: 1.17 $
 */
public class BoundsManipulator extends Manipulator {

    /** The geometry "helper"
     */
    protected BoundsGeometry _geometry;

    /** The interactor that is attached to a move handle.
     */
    protected Interactor _dragInteractor = null;

    /**
     * Construct a new manipulator that uses rectangular grab-handles.
     */
    public BoundsManipulator() {
        this(new BasicGrabHandleFactory());
    }

    /**
     * Construct a new manipulator using the given grab-handle factory. 
     */
    public BoundsManipulator(GrabHandleFactory f) {
        setGrabHandleFactory(f);
        setHandleInteractor(new Resizer());
    }

    /** Return the geometry of this manipulator
     * PWD: change protection to protected from private(!).
     */
    protected BoundsGeometry getGeometry () {
        return _geometry;
    }

    /** Create a new instance of this manipulator. The new
     * instance will have the same grab handle, and interactor
     * for grab-handles, as this one.
     */
    public FigureDecorator newInstance (Figure f) {
        BoundsManipulator m = new BoundsManipulator();
        m.setGrabHandleFactory(this.getGrabHandleFactory());
        m.setHandleInteractor(this.getHandleInteractor());
        m.setDragInteractor(_dragInteractor);
        return m;
    }

    /** Refresh the geometry. This adjusts the bounds of the geometry
     * to match the bounds of the child figure.
     */
    public void refresh () {
        if (_geometry != null) {
            _geometry.setBounds(getChild().getBounds());
        }
    }

    /** Set the child figure. If we have any grab-handles, lose them.
     * Then get a rectangle geometry object and create grab-handles
     * on its sites.
     */
    public void setChild (Figure child) {
        super.setChild(child);
        clearGrabHandles();

        // Process new child
        if (child != null) {
            // Create the geometry defining the sites
            _geometry = new BoundsGeometry(this, getChild().getBounds());
            Iterator i = _geometry.sites();
            GrabHandle g = null;
            while (i.hasNext()) {
                // Create a grab handle and set up the interactor
                Site site = (Site)i.next();
                g = getGrabHandleFactory().createGrabHandle(site);
                g.setParent(this);
                g.setInteractor(getHandleInteractor());
                addGrabHandle(g);
            }
            // Add a center handle for dragging
            if (_dragInteractor != null) {
                CenterSite center = new CenterSite(getChild());
                GrabHandle mover = new MoveHandle(center);
                mover.setParent(this);
                mover.setInteractor(_dragInteractor);
                addGrabHandle(mover);
            }

            // Move them where they should be - ?
            relocateGrabHandles();

            // Set the minimum size
            // FIXME: this is bogus: set it in the interactor instead!
            //_geometry.setMinimumSize(4*g.getSize()); //PWD: bad.

        }
    }

    /** Set the drag interactor for figures wrapped by this
     * manipulator. If set, the manipulator displays an additional
     * handle that can be used to drag the figure. This is useful
     * for certain types of figure that are outlines only.
     */
    public void setDragInteractor(Interactor dragger) {
        _dragInteractor = dragger;
    }

    ///////////////////////////////////////////////////////////////////////
    //// Resizer

    /** An interactor class that changes the bounds of the child
     * figure and triggers a repaint.
     */
    private static class Resizer extends DragInteractor {

        /** Translate the grab-handle
         */
        public void translate (LayerEvent e, double x, double y) {
            // Translate the grab-handle, resizing the geometry
            GrabHandle g = (GrabHandle) e.getFigureSource();
            g.translate(x, y);

            // Transform the child.
            BoundsManipulator parent = (BoundsManipulator) g.getParent();
            BoundsGeometry geometry = parent.getGeometry();

            parent.getChild().transform(CanvasUtilities.computeTransform(
                    parent.getChild().getBounds(),
                    geometry.getBounds()));
        }
    }
}



