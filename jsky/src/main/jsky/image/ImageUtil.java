/*
 * ESO Archive
 *
 * $Id: ImageUtil.java,v 1.4 2002/07/09 13:30:36 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/12/06  Created
 */

package jsky.image;

import java.lang.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.image.renderable.*;
import javax.media.jai.*;


/**
 * Contains static convenience and utility methods
 * for dealing with JAI images.
 *
 * @version $Revision: 1.4 $
 * @author Allan Brighton
 */
public class ImageUtil {

    /**
     * Return the value of the given property for the given image as a double,
     * or return the given default value, if the property was not defined.
     */
    public static double getImageProperty(PlanarImage im, String name, double defaultValue) {
        Object o = im.getProperty(name);
        if (o != null && o != java.awt.Image.UndefinedProperty) {
            return Double.parseDouble(o.toString());
        }
        return defaultValue;
    }

    /**
     * Return the value of the given property for the given image as an int,
     * or return the given default value, if the property was not defined.
     */
    public static int getImageProperty(PlanarImage im, String name, int defaultValue) {
        Object o = im.getProperty(name);
        if (o != null && o != java.awt.Image.UndefinedProperty) {
            return Integer.parseInt(o.toString());
        }
        return defaultValue;
    }

    /**
     * Return the value of the given property for the given image as a String,
     * or return the given default value, if the property was not defined.
     */
    public static String getImageProperty(PlanarImage im, String name, String defaultValue) {
        Object o = im.getProperty(name);
        if (o != null && o != java.awt.Image.UndefinedProperty) {
            //System.out.println("XXX " + name + " = " + o.toString());
            return o.toString();
        }
        return defaultValue;
    }


    /**
     * Return a RenderingHints object defining a tile cache with the given number of tiles
     * of the given size. This can be passed to JAI.create(...) to specify the tile cache
     * for a given operation.
     */
    public static RenderingHints getTileCacheHint(int numTiles, int tileWidth, int tileHeight) {
        // from: Lincoln Perry <lincoln.perry@ENG.SUN.COM>:
        // Set the destination image tile size in an ImageLayout
        // and add to a RenderingHint.
        // Having smaller destination tiles for zooms is usually a
        // good idea, since it minimizes source data cobbling and "wasted"
        // space in partially-filled destination tiles.
        //
        ImageLayout layout = new ImageLayout();
        layout.setTileWidth(tileWidth);
        layout.setTileHeight(tileHeight);
        RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);

        // Create our own tile cache for this OpImage's output tiles only
        // XXX JAI 1.0.2 TileCache tileCache = JAI.createTileCache(numTiles, (long)(numTiles*tileWidth*tileHeight));
        TileCache tileCache = JAI.createTileCache((long) (numTiles * tileWidth * tileHeight));
        hints.put(JAI.KEY_TILE_CACHE, tileCache);
        return hints;
    }


    /**
     * Return a RenderingHints object defining the sample model (data type) of the resulting
     * image of an operation.
     */
    public static RenderingHints getSampleModelHint(int tileWidth, int tileHeight, int dataType) {
        int[] bandOffsets = new int[1];
        bandOffsets[0] = 0;
        int pixelStride = 1;
        int scanlineStride = tileWidth;
        SampleModel sampleModel = RasterFactory.createPixelInterleavedSampleModel(dataType,
                tileWidth, tileHeight,
                pixelStride,
                scanlineStride,
                bandOffsets);
        ImageLayout layout = new ImageLayout();
        layout.setTileWidth(tileWidth);
        layout.setTileHeight(tileHeight);
        layout.setSampleModel(sampleModel);
        RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);

        // Create our own tile cache for this OpImage's output tiles only
        //TileCache tileCache = JAI.createTileCache(numTiles, (long)(numTiles*tileWidth*tileHeight));
        //hints.put(JAI.KEY_TILE_CACHE, tileCache);

        return hints;
    }
}


