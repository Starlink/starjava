/*
 * ESO Archive
 *
 * $Id: ImageLayer.java,v 1.5 2002/07/09 13:30:37 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.image.graphics;


import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import jsky.image.gui.DivaGraphicsImageDisplay;

import diva.canvas.CanvasLayer;
import diva.canvas.VisibleComponent;


/**
 * Defines a Diva canvas layer for displaying an image.
 *
 * @version $Revision: 1.5 $
 * @author Allan Brighton
 */
public class ImageLayer extends CanvasLayer implements VisibleComponent {

    /** Image display canvas */
    protected DivaGraphicsImageDisplay imageDisplay;

    /** If the flag is false, then the object will not be painted on the screen. */
    protected boolean visible = true;


    /**
     * Construct an ImageLayer.
     *
     * @param imageDisplay the image display window
     */
    public ImageLayer(DivaGraphicsImageDisplay imageDisplay) {
        this.imageDisplay = imageDisplay;
    }


    /**
     * Test the visibility flag of this object. Note that this flag
     * does not indicate whether the object is actually visible on
     * the screen, as one of its ancestors may not be visible.
     */
    public boolean isVisible() {
        return visible;
    }


    /**
     * Paint this object onto a 2D graphics object. Implementors
     * should first test if the visibility flag is set, and
     * paint the object if it is.
     */
    public void paint(Graphics2D g2d) {
        imageDisplay.paintLayer(g2d, null);
    }


    /**
     * Paint this object onto a 2D graphics object, within the given
     * region.  Implementors should first test if the visibility flag is
     * set, and paint the object if it is. The provided region can be
     * used to optimize the paint, but implementors can assume that the
     * clip region is correctly set beforehand.
     */
    public void paint(Graphics2D g2D, Rectangle2D region) {
        imageDisplay.paintLayer(g2D, region);
    }


    /**
     * Set the visibility flag of this object. If the flag is false,
     * then the object will not be painted on the screen.
     */
    public void setVisible(boolean flag) {
        visible = flag;
    }
}

