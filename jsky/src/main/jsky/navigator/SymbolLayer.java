/*
 * $Id: SymbolLayer.java,v 1.2 2002/07/09 13:30:37 brighton Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */

package jsky.navigator;

import diva.canvas.CanvasLayer;
import diva.canvas.GraphicsPane;
import diva.canvas.VisibleComponent;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import jsky.catalog.gui.TablePlotter;


public class SymbolLayer extends CanvasLayer implements VisibleComponent {

    /** If true, the layer is visible */
    private boolean _visible = true;

    /** Object used to draw catalog symbols */
    private TablePlotter _plotter;

    /** Set the object used to draw catalog symbols */
    public void setPlotter(TablePlotter plotter) {
        _plotter = plotter;
    }

    public void paint(Graphics2D g) {
        if (_plotter != null)
            _plotter.paintSymbols(g, null);
    }

    public void paint(Graphics2D g, Rectangle2D region) {
        if (_plotter != null)
            _plotter.paintSymbols(g, region);
    }

    public boolean isVisible() {
        return _visible;
    }

    public void setVisible(boolean b) {
        _visible = b;
    }
}

