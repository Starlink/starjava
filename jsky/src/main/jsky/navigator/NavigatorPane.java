/*
 * $Id: NavigatorPane.java,v 1.2 2002/07/09 13:30:37 brighton Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */

package jsky.navigator;

import diva.canvas.GraphicsPane;
import diva.canvas.CanvasLayer;
import jsky.catalog.gui.TablePlotter;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;


/**
 * A Diva GraphicsPane with a layer added for plotting catalog symbols.
 */
public class NavigatorPane extends GraphicsPane {

    /** A layer on which to draw catalog symbols */
    private SymbolLayer _symbolLayer;

    /**
     * Initialize a new NavigatorPane, which is a Diva GraphicsPane with a layer added
     * for catalog symbols.
     */
    public NavigatorPane() {
        _symbolLayer = new SymbolLayer();
        _initNewLayer(_symbolLayer);
        _rebuildLayerArray();
    }

    /**
     * Return the layer to use to draw teh catalog symbols.
     */
    public SymbolLayer getSymbolLayer() {
        return _symbolLayer;
    }

    /** Set the object used to draw catalog symbols */
    public void setPlotter(TablePlotter plotter) {
        _symbolLayer.setPlotter(plotter);
    }

    /**
     * Rebuild the array of layers for use by iterators.
     * Override superclass to include the new layer.
     */
    protected void _rebuildLayerArray() {
        _layers = new CanvasLayer[6];
        int cursor = 0;
        _layers[cursor++] = _foregroundEventLayer;
        _layers[cursor++] = _symbolLayer;
        _layers[cursor++] = _overlayLayer;
        _layers[cursor++] = _foregroundLayer;
        _layers[cursor++] = _backgroundLayer;
        _layers[cursor++] = _backgroundEventLayer;
    }
}

