/*
 * $Id: SketchLayer.java,v 1.4 2001/07/22 22:01:41 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch;
import diva.canvas.CanvasPane;
import diva.canvas.CanvasLayer;
import diva.util.java2d.PaintedShape;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;

/**
 * A layer class that is specialized to make sketching
 * strokes fast.  This layer only paints the last segment
 * of a hand-drawn stroke, so it doesn't have to redraw
 * any of the layers behind it.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 * @rating Red
 */
public class SketchLayer extends CanvasLayer {
    /**
     * The x coordinate of the first point in a line segment.
     */
    private double _prevX;

    /**
     * The y coordinate of the first point in a line segment.
     */
    private double _prevY;

    /**
     * The line segment that the _shape paints.  This is the last line
     * segment in the stroke and is the only segment that we're
     * painting.  We're not repaining the whole stroke.
     */
    private Line2D _line;

    /**
     * This shape paints a line segment (_line).
     */
    private PaintedShape _shape;

    /** Create a new layer that is not in a pane. The layer will not
     * be displayed, and its coordinate tranformation will be as
     * though it were a one-to-one mapping. Use of this constructor is
     * strongly discouraged, as many of the geometry-related methods
     * expect to see a pane.
     *
     * Create a SketchLayer with a PaintedShape object that contains a
     * line.  The shape is initialized to be drawn with black ink and
     * 1 unit line width.
     */
    public SketchLayer () {
        super();
        _line = new Line2D.Double();
        _shape = new PaintedShape(_line);
        setPenColor(Color.black);
        setLineWidth(1);
    }

    /** Create a new layer within the given pane.
     *
     * Create a SketchLayer with a PaintedShape object that contains a
     * line.  The shape is initialized to be drawn with black ink and
     * 1 unit line width.
     */
    public SketchLayer (CanvasPane pane) {
        super(pane);
        _line = new Line2D.Double();
        _shape = new PaintedShape(_line);        
        setPenColor(Color.black);
        setLineWidth(1);
    }


    /**
     * This completes a line segement with start point (_prevX,prevY)
     * and (x,y) and asks the shape to paint itself.  Normally this
     * would be called from within mouseDragged.
     */
    public void appendStroke(double x, double y) {
        Graphics2D g2d =
            (Graphics2D)(getCanvasPane().getCanvas().getGraphics());
        if(g2d != null) {
            if(getCanvasPane().isAntialiasing()) {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
            }
            _line.setLine(_prevX, _prevY, x, y);
            _shape.paint(g2d);
        }
        _prevX = x;
        _prevY = y;
    }

    
    /**
     * Finish the current stroke and clear the layer.  Normally this
     * would be called from within mouseReleasd.  This method is empty
     * right now, because there's nothing that needs to be cleared up.
     * The line segment is reset upon every appendStroke call, and is
     * painted only when appendStroke is called.  When a stroke has
     * been completed (finishStroke in BasicInterpreter will be
     * called), a figure will be created for it and be added to the
     * canvas which will repaint, therefore the last segment is
     * accounted for in the figure and need not be drawn here because
     * it'll be cleared anyway when the canvas repaints.
     */
    public void finishStroke() {
    }


    /**
     * Set the pen width used to paint the shape.
     */
    public void setLineWidth(float width) {
        _shape.setLineWidth(width);
    }

    
    /**
     * Set the pen color used to paint the shape.
     */
    public void setPenColor(Color c) {
        _shape.setStrokePaint(c);
    }


    /**
     * This sets the start point (_prevX, _prevY) of the line segment
     * to be drawn.  Normally this would be called from within
     * mousePressed.
     */
    public void startStroke(double x, double y) {
        _prevX = x;
        _prevY = y;
    }
}

