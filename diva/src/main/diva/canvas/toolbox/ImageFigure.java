/*
 * $Id: ImageFigure.java,v 1.7 2001/07/22 22:00:44 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.canvas.toolbox;

import diva.canvas.Figure;
import diva.canvas.AbstractFigure;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * A figure which draws a user-specified image.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.7 $
 */
public class ImageFigure extends AbstractFigure {
    /**
     * The local transform
     */
    private AffineTransform _xf = new AffineTransform();

    /**
     * The image of this figure.
     */
    private Image _image;

    /**
     * Create an empty image figure.
     */
    public ImageFigure() {
        this(null);
    }

    /**
     * Create an image figure displaying
     * the given image.
     */
    public ImageFigure(Image i) {
        setImage(i);
    }

    /**
     * Return the figure's image.
     */
    public Image getImage() {
        return _image;
    }

    /**
     * Return the rectangular shape of the
     * image, or a small rectangle if the
     * image is null.
     */
    public Shape getShape() {
        if(_image != null) {
            int w = _image.getWidth(null);
            int h = _image.getHeight(null);
            Rectangle2D r = new Rectangle2D.Double(0, 0, w, h);
            return _xf.createTransformedShape(r);
        }
        else {
            return new Rectangle2D.Double();
        }
    }


    /**
     * Paint the figure's image.
     */
    public void paint(Graphics2D g) {
        if(_image != null) {
            g.drawImage(_image, _xf, null);
        }
    }

    /**
     * Set the figure's image.
     */
    public void setImage(Image i) {
        _image = i;
    }

    /**
     * Perform an affine transform on this
     * image.
     */
    public void transform(AffineTransform t) {
        repaint();
        _xf.preConcatenate(t);
        repaint();
    }
}



