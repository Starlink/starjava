/*
 * ESO Archive
 *
 * $Id: BasicImageReadableProcessor.java,v 1.3 2002/07/09 13:30:36 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Frank Tanner   2001/12/19  Created
 */

package jsky.image;

import java.awt.geom.Rectangle2D;


/**
 * Responsible for providing read only information for image processing.
 *
 *
 * @version $Revision: 1.3 $
 * @author Franklin R. Tanner
 */
public abstract interface BasicImageReadableProcessor {

    /**
     * Return the image processor object.
     *
     * @return The Object responsible for most image transformations
     * 		that processes PlanarImages.
     * @see javax.media.jai.PlanarImage
     */
    public ImageProcessor getImageProcessor();

    /**
     * Return a rectangle describing the visible area of the image
     * (in user coordinates).
     *
     * @return User Coordinates of retangle.  Note, this may be a
     *			a SuperRectangle if the PlanarImage is non-rectangular.
     */
    public Rectangle2D.Double getVisibleArea();
}
