package uk.ac.starlink.ttools.plot2.paper;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

/**
 * An efficiently stored RGB or RGBA image backed by an int buffer.
 * Each pixel is represented by a 4-byte int, with the three least-significant
 * bytes giving 0-255 levels of red, green, blue (msb-&gt;lsb) 
 * and, optionally, the most significant byte giving alpha.
 * This corresponds to the {@link java.awt.image.BufferedImage} constants
 * <code>TYPE_INT_RGB</code> and <code>TYPE_INT_ARGB</code>.
 * The values in the buffer are therefore the same as those used by
 * <code>BufferedImage</code> <code>setRGB</code>/<code>getRGB</code>,
 * but can (presumably) be manipulated more efficiently by addressing the
 * array here directly.
 * These RGB valueas are also the same as those used by {@link Compositor}.
 *
 * @author   Mark Taylor
 * @since    14 Feb 2013
 */
public class RgbImage {

    private final BufferedImage image_;
    private final int[] buffer_;
    private final boolean hasAlpha_;

    /**
     * Constructor.
     *
     * @param   image  buffered image
     * @param   buffer  buffer backing image
     * @param   hasAlpha  true if image supports transparency
     */
    private RgbImage( BufferedImage image, int[] buffer, boolean hasAlpha ) {
        image_ = image;
        buffer_ = buffer;
        hasAlpha_ = hasAlpha;
    }

    /**
     * Returns the data as a BufferedImage.
     *
     * @return  image
     */
    public BufferedImage getImage() {
        return image_;
    }

    /**
     * Returns the data as a modifiable int array.
     *
     * @return  int buffer
     */
    public int[] getBuffer() {
        return buffer_;
    }

    /**
     * Constructs an RgbImage.
     *
     * @param  width  width in pixels
     * @param  height  height in pixels
     * @param  hasAlpha  true for ARGB, false for RGB
     */
    public static RgbImage createRgbImage( int width, int height,
                                           boolean hasAlpha ) {

        /* You could do this in a more respectable way by starting with
         * an int buffer and working your way through creating a Raster
         * based on it etc, but it's a bit hard to know whether you're
         * doing it right at every stage.  This way certainly gives the
         * most efficient result - as long as it doesn't fail at runtime
         * with a ClassCastException. */
        BufferedImage image =
            new BufferedImage( width, height,
                               hasAlpha ? BufferedImage.TYPE_INT_ARGB
                                        : BufferedImage.TYPE_INT_RGB );
        int[] buf = ((DataBufferInt) image.getRaster().getDataBuffer())
                   .getData();
        Arrays.fill( buf, hasAlpha ? 0 : 0xffffffff );
        return new RgbImage( image, buf, hasAlpha );
    }
}
