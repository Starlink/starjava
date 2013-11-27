package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.Arrays;
import uk.ac.starlink.ttools.plot2.Pixer;

/**
 * A greyscale image backed by a a byte buffer.
 * The buffer is initially set to zero values, corresponding to white.
 * Black corresponds to byte values of 255.
 * Trial and error suggests that it is more efficient in time,
 * though not in memory, to use greyscale than bitmap images
 * (BufferedImage.TYPE_BYTE_BINARY), even if only bitmap type values
 * are required.
 *
 * @author   Mark Taylor
 * @since    26 Nov 2013
 * @see      uk.ac.starlink.ttools.plot.GraphicsBitmap
 * @see      uk.ac.starlink.ttools.plot2.paper.RgbImage
 */
public class GreyImage {

    private final BufferedImage image_;
    private final byte[] buffer_;

    /**
     * Private constructor.
     *
     * @param   image  buffered image
     * @param   buffer  byte buffer backing image
     */
    private GreyImage( BufferedImage image, byte[] buffer ) {
        image_ = image;
        buffer_ = buffer;
    }

    /**
     * Returns the greyscale image.
     *
     * @return  image
     */
    public BufferedImage getImage() {
        return image_;
    }

    /**
     * Returns the byte array backing the image.
     *
     * @return  byte array
     */
    public byte[] getBuffer() {
        return buffer_;
    }

    /**
     * Returns a pixel iterator that iterates over all the pixels that
     * have been altered (are non-white) in this image.
     * In the current implementation this is not efficient to be re-used.
     *
     * @return  iterator over non-white pixels
     */
    public Pixer createPixer() {
        final int npix = buffer_.length;
        final int width = image_.getWidth();
        return new Pixer() {
            int ip = -1;
            public boolean next() {
                while ( ++ip < npix ) {
                    if ( buffer_[ ip ] != 0 ) {
                        return true;
                    }
                }
                return false;
            }
            public int getX() {
                return ip % width;
            }
            public int getY() {
                return ip / width;
            }
        };
    }

    /**
     * Factory method to create an instance.
     *
     * @param  width  image width in pixels
     * @param  height  image height in pixels
     */
    public static GreyImage createGreyImage( int width, int height ) {
        BufferedImage image =
            new BufferedImage( width, height, BufferedImage.TYPE_BYTE_GRAY );
        byte[] buf = ((DataBufferByte) image.getRaster().getDataBuffer())
                    .getData();
        return new GreyImage( image, buf );
    }
}
