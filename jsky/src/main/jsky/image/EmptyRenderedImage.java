/*
 * ESO Archive
 *
 * $Id: EmptyRenderedImage.java,v 1.2 2002/07/09 13:30:36 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.image;

import java.awt.Point;
import java.awt.image.*;
import java.util.*;
import java.io.IOException;
import javax.media.jai.*;

import com.sun.media.jai.codec.*;
import jsky.image.fits.codec.FITSImage;


/**
 * Implements a dummy RenderedImage, for use when there is no image to display.
 *
 * @version $Revision: 1.2 $
 * @author Allan Brighton
 */
public class EmptyRenderedImage extends jsky.image.SimpleRenderedImage {

    /** The one tile. */
    private Raster tile;

    /** Construct an "empty" image with the given width and height in pixels */
    public EmptyRenderedImage(int w, int h) {
        // set variables required by the base class for tiling
        minX = minY = 0;
        tileWidth = width = w;
        tileHeight = height = h;
        sampleModel = RasterFactory.createPixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, w, h, 3);
        colorModel = ImageCodec.createComponentColorModel(sampleModel);
    }

    /** Construct an "empty" image */
    public EmptyRenderedImage() {
        this(2, 2);
    }

    /**
     * Generate and return the given tile (required by the RenderedImage interface).
     *
     * @param tileX the X index of the requested tile in the tile array.
     * @param tileY the Y index of the requested tile in the tile array.
     * @return the tile given by (tileX, tileY).
     */
    public synchronized Raster getTile(int tileX, int tileY) {
        if (tile == null)
            tile = RasterFactory.createWritableRaster(sampleModel, new Point(0, 0));
        return tile;
    }

    /**
     * Gets a property from the property set of this image.
     * (redefined from parent class to support teh #preview_image property
     *  as for FITS files).
     *
     * @see jsky.image.fits.codec.FITSImage
     * @param name the name of the property to get, as a String.
     * @return a reference to the property value or null if not found.
     */
    public Object xxxgetProperty(String name) {
        if (name.equals("#preview_image")) {
            return getPreviewImage(FITSImage.getPreviewSize());
        }
        return super.getProperty(name);
    }

    /**
     * Return a prescaled PlanarImage that fits entirely in a window of the given size,
     * of null if there are any errors.
     */
    protected PlanarImage getPreviewImage(int size) {
        return PlanarImage.wrapRenderedImage(new EmptyRenderedImage(size, size));
    }
}

