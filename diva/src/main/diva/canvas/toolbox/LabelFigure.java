/*
 * $Id: LabelFigure.java,v 1.14 2001/01/28 03:53:58 neuendor Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.canvas.toolbox;

import diva.canvas.Figure;
import diva.canvas.AbstractFigure;
import diva.canvas.CanvasUtilities;
import diva.util.java2d.PaintedString;

import java.awt.Font;
import java.awt.Shape;
import java.awt.Paint;
import java.awt.Color;
import java.awt.Graphics2D;

import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;

import javax.swing.SwingConstants;

/**
 * A figure which draws user-specified text in a user-specified
 * font. Labels are also "anchored" in the center or on one of
 * the edges or corners, so that when the font or text changes,
 * the label appears to stay in the right location.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @author John Reekie  (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.14 $
 */
public class LabelFigure extends AbstractFigure {

    /** The transform of the label
     */
    private AffineTransform _transform = new AffineTransform();

    /** The "padding" around the text
     */
    private double _padding = 4.0;

    /** The anchor on the label. This must be one of the
     * constants defined in SwingConstants.
     */
    private int _anchor = SwingConstants.CENTER;

    /** The painted string that holds the string,
     * font, fill, etc. and actually does the painting.
     */
    private PaintedString _paintedString;
    
    /** The order of anchors used by the autoanchor method.
     */
    private static int _anchors[] = {
        SwingConstants.SOUTH,
        SwingConstants.NORTH,
        SwingConstants.WEST,
        SwingConstants.EAST,
        SwingConstants.SOUTH_WEST,
        SwingConstants.SOUTH_EAST,
        SwingConstants.NORTH_WEST,
        SwingConstants.NORTH_EAST
    };

    /**
     * Construct an empty label figure.
     */
    public LabelFigure() {
        this("");
    }

    /**
     * Construct a label figure displaying the
     * given string, using the default font.
     */
    public LabelFigure(String s) {
        _paintedString = new PaintedString(s);
    }

    /**
     * Construct a label figure displaying the
     * given string in the given font. This is the best constructor
     * to use if you are creating a lot of labels in a font other
     * than the default, as a single instance of Font can then
     * be shared by many labels.
     */
    public LabelFigure(String s, Font f) {
        _paintedString = new PaintedString(s,f);
    }

    /**
     * Construct a label figure displaying the
     * given string in the given face, style, and size. A new
     * Font object representing the face, style, and size is
     * created for this label.
     */
    public LabelFigure(String s, String face, int style, int size) {
        _paintedString = new PaintedString(s,face,style,size);
    }


    /**
     * Construct a label figure displaying the
     * given string in the given font, with the given padding and anchor.
     */
    public LabelFigure(String s, Font font, double padding, int anchor) {
        _paintedString = new PaintedString(s, font);
        _padding = padding;
        _anchor = anchor;
    }
    
    /** Choose an anchor point so as not to intersect a given
     * figure. The anchor point is cycled through until one is reached
     * such that the bounding box of the label does not intersect
     * the given shape.  If there is none,
     * the anchor is not changed. The order of preference is the
     * current anchor, the four edges, and the four corners.
     */
    public void autoAnchor (Shape s) {
        Rectangle2D.Double r = new Rectangle2D.Double();
        r.setRect(_paintedString.getBounds());
        
        // Try every anchor and if there's no overlap, use it
        Point2D location = getAnchorPoint(); 
        for (int i = 0; i < _anchors.length; i++) {
            Point2D pt = CanvasUtilities.getLocation(r, _anchors[i]);
            CanvasUtilities.translate(pt, _padding, _anchors[i]);
            r.x += location.getX() - pt.getX();
            r.y += location.getY() - pt.getY();
            if (!s.intersects(r)) {
                //// System.out.println("Setting anchor to " + _anchors[i]);
                setAnchor(_anchors[i]);
                break;
            }
        }
    }

    /**
     * Get the point at which this figure is "anchored." This
     * will be one of the positioning constants defined in 
     * javax.swing.SwingConstants.
     */
    public int getAnchor () {
        return _anchor;
    }

    /**
     * Get the location at which the anchor is currently located.
     * This method looks at the anchor and padding attributes to
     * figure out the point.
     */
    public Point2D getAnchorPoint () {
        Rectangle2D bounds = _paintedString.getBounds();
        Point2D pt = CanvasUtilities.getLocation(bounds, _anchor);
        if (_anchor != SwingConstants.CENTER) {
            CanvasUtilities.translate(pt, _padding, _anchor);
        }
        return pt;
    }

    /**
     * Get the bounds of this label
     */
    public Rectangle2D getBounds () {
        return _paintedString.getBounds();
    }

    /**
     * Get the font that this label is drawn in.
     */
    public Font getFont() {
        return _paintedString.getFont();
    }

    /**
     * Get the fill paint for this label.
     */
    public Paint getFillPaint() {
        return _paintedString.getFillPaint();
    }
    
    /**
     * Get the font name.
     */
    public String getFontName() {
        return _paintedString.getFontName();
    }
    
    /**
     * Get the padding around the text.
     */
    public double getPadding () {
        return _padding;
    }
    
    /**
     * Get the font style.
     */
    public int getStyle() {
        return _paintedString.getStyle();
    }
    
    /**
     * Get the font size.
     */
    public int getSize() {
        return _paintedString.getSize();
    }

    /**
     * Get the shape of this label figure. This just returns
     * the bounds, since hit-testing on the actual filled
     * latter shapes is way slow (and not that useful, since
     * usually you want to treat the whole label as a single
     * object anyway, and not have to click on an actual
     * filled pixel).
     */
    public Shape getShape () {
        return _paintedString.getBounds();
    }

    /**
     * Get the string of this label.
     */
    public String getString() {
        return _paintedString.getString();
    }

    /**
     * Paint the label.
     */
    public void paint(Graphics2D g) {
        if (!isVisible()) {
             return;
        }
        if(getString() != null) {
            _paintedString.paint(g);
        }
    }

    /**
     * Set the point at which this figure is "anchored." This
     * must be one of the positioning constants defined in 
     * javax.swing.SwingConstants. The default is
     * SwingConstants.CENTER. Whenever the font or string is changed,
     * the label will be moved so that the anchor remains at
     * the same position on the screen. When this method is called,
     * the figure is adjusted so that the new anchor is at the
     * same position as the old anchor was. The actual position of
     * the text relative to the anchor point is shifted by the
     * padding attribute.
     */
    public void setAnchor (int anchor) {
        Point2D oldpt = getAnchorPoint();
        this._anchor = anchor;
        Point2D newpt = getAnchorPoint();

        repaint();
        _paintedString.translate(
                oldpt.getX() - newpt.getX(),
                oldpt.getY() - newpt.getY());
        repaint();
    }

    /**
     * Set the fill paint that this shape
     * is drawn with.
     */
    public void setFillPaint(Paint p) {
        _paintedString.setFillPaint(p);
        repaint();
    }

    /**
     * Set the font.
     */
    public void setFont(Font f) {
        // Remember the current anchor point
        Point2D pt = getAnchorPoint();
        _paintedString.setFont(f);
        // Move it back
        translateTo(pt);
    }

    /**
     * Set the font family by name.
     */
    public void setFontName(String s) {
        Point2D pt = getAnchorPoint();
        _paintedString.setFontName(s);
        translateTo(pt);
    }

    /**
     * Set the "padding" around the text. This is used
     * only if anchors are used -- when the label is positioned
     * relative to an anchor, it is also shifted by the padding
     * distance so that there is some space between the anchor
     * point and the text. The default padding is two, and the
     * padding must not be set to zero if automatic anchoring
     * is used.
     */
    public void setPadding (double padding) {
        _padding = padding;
        setAnchor(_anchor);
    }

    /**
     * Set the font style.
     */
    public void setStyle(int style) {
        Point2D pt = getAnchorPoint();
        _paintedString.setStyle(style);
        translateTo(pt);
    }

    /**
     * Set the font size.
     */
    public void setSize(int size) {
        Point2D pt = getAnchorPoint();
        _paintedString.setSize(size);
        translateTo(pt);
    }

    /**
     * Set the string.
     */
    public void setString(String s) {
        // FIXME something seems wrong here.
        // repaint the string where it currently is
        repaint();
        // Remember the current anchor point
        Point2D pt = getAnchorPoint();
        _paintedString.setString(s);
        // Update the bounds
        translateTo(pt);
        repaint();
    }

    /**
     * Change the transform of this label. Note that the anchor
     * of the figure will appear to nmove -- use translateTo()
     * to move it back again if this method being called to
     * (for example) rotate the label.
     */
    public void setTransform (AffineTransform at) {
        repaint();
        _paintedString.setTransform(at);
        repaint();
    }

    /**
     * Transform the label with the given transform.  Note that the anchor
     * of the figure will appear to nmove -- use translateTo()
     * to move it back again if this method being called to
     * (for example) rotate the label.
     */
    public void transform (AffineTransform at) {
        repaint();
        _paintedString.transform(at);
        repaint();
     }

    /**
     * Translate the label so that the current anchor is located
     * at the given point. Use this if you apply a transform to
     * a label in order to rotate or scale it, but don't want
     * the label to actually go anywhere.
     */
    public void translateTo (double x, double y) {
        // FIXME: this might not work in the presence of
        // scaling. If not, modify to preconcatenate instead
        repaint();
        Point2D pt = getAnchorPoint();
        _paintedString.translate(x-pt.getX(),y-pt.getY());
        repaint();
     }

    /**
     * Translate the label so that the current anchor is located
     * at the given point. Use this if you apply a transform to
     * a label in order to rotate or scale it, but don't want
     * the label to actually go anywhere.
     */
    public void translateTo (Point2D pt) {
        translateTo(pt.getX(), pt.getY());
     }
}

