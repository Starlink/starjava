/*
 * $Id: SketchLayer.java,v 1.1 2000/09/07 05:01:59 michaels Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch;
import diva.canvas.GraphicsPane;
import diva.canvas.CanvasPane;
import diva.canvas.JCanvas;
import diva.canvas.CanvasLayer;
import diva.canvas.DamageRegion;
import diva.canvas.FigureLayer;
import diva.util.java2d.PaintedShape;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Line2D;

/**
 * A layer class that is specialized to make sketching
 * strokes fast.  This layer only paints the last segment
 * of a hand-drawn stroke, so it doesn't have to redraw
 * any of the layers behind it.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 * @rating Red
 */
public class SketchLayer extends CanvasLayer {
    private double _prevX;
    private double _prevY;
    private Line2D _line;
    private PaintedShape _shape;

    /** Create a new layer that is not in a pane. The layer will not
     * be displayed, and its coordinate tranformation will be as
     * though it were a one-to-one mapping. Use of this constructor is
     * strongly discouraged, as many of the geometry-related methods
     * expect to see a pane.
     */
    public SketchLayer () {
        super();
        _line = new Line2D.Double();
        _shape = new PaintedShape(_line);
        setPenColor(Color.black);
        setLineWidth(1);
    }

    /** Create a new layer within the given pane.
     */
    public SketchLayer (CanvasPane pane) {
        super(pane);
        _line = new Line2D.Double();
        setPenColor(Color.black);
        setLineWidth(1);
    }

    /**
     * Start a stroke at the given point.
     */
    public void startStroke(double x, double y) {
        _prevX = x;
        _prevY = y;
    }

    /**
     * Append the given point to the current stroke.
     */
    public void appendStroke(double x, double y) {
        Graphics2D g2d =
            (Graphics2D)(getCanvasPane().getCanvas().getGraphics());
        if(getCanvasPane().isAntialiasing()) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }
        _line.setLine(_prevX, _prevY, x, y);
        _shape.paint(g2d);
        _prevX = x;
        _prevY = y;
    }

    /**
     * Finish the current stroke and clear the layer.
     */
    public void setLineWidth(float width) {
        _shape.setLineWidth(width);
    }

    /**
     * Finish the current stroke and clear the layer.
     */
    public void setPenColor(Color c) {
        _shape.setStrokePaint(c);
    }
        
    /**
     * Finish the current stroke and clear the layer.
     */
    public void finishStroke() {
    }
}
