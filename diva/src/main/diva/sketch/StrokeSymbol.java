/*
 * $Id: StrokeSymbol.java,v 1.2 2001/07/22 22:01:42 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch;
import diva.sketch.recognition.TimedStroke;
import java.awt.Color;
import java.awt.Shape;
import java.util.Iterator;

/**
 * A visual symbol derived from sketch input.  It is derived from a
 * stroke, and it keeps the outline/fill color and pen width
 * information of the stroke.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 * @rating Red
 */
public class StrokeSymbol implements Symbol {
    /**
     * The color used to paint the outline of this symbol.
     */
    private Color _outline;

    /**
     * The color used to fill the inside of this symbol.
     */
    private Color _fill;
    
    /**
     * The line width used to draw this symbol.
     */
    private float _lineWidth;

    /**
     * The stroke that this symbol wraps.
     */
    private TimedStroke _stroke;

    /**
     * Create a symbol for the given stroke with the outline/fill color
     * and line width information.
     */
    public StrokeSymbol(TimedStroke stroke, Color outline,
            Color fill, float lineWidth){
        setStroke(stroke);
        setOutline(outline);
        setFill(fill);
        setLineWidth(lineWidth);
    }

    /**
     * Return the stroke that's wrapped by this symbol.
     */
    public TimedStroke getStroke(){
        return _stroke;
    }
    
    /**
     * Return the outline color of this symbol.
     */
    public Color getOutline() {
        return _outline;
    }

    /**
     * Return the fill color of this symbol.  NULL denotes
     * no fill.
     */
    public Color getFill() {
        return _fill;
    }

    /**
     * Return the line width used to draw this symbol.
     */
    public float getLineWidth() {
        return _lineWidth;
    }

    /**
     * Set the outline color of this symbol.
     */
    public void setOutline(Color c) {
        _outline = c;
    }

    /**
     * Set the fill color of this symbol.   NULL denotes
     * no fill
     */
    public void setFill(Color c) {
        _fill = c;
    }

    /**
     * Set the line width used to draw this symbol.
     */
    public void setLineWidth(float w) {
        _lineWidth = w;
    }

    /**
     * Set the timed stroke of this symbol.  Note, this
     * method is not intended for general use.
     */
    public void setStroke(TimedStroke stroke) {
        _stroke = stroke;
    }
}


