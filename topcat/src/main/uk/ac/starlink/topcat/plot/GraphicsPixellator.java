package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.Arrays;
import java.util.BitSet;

/**
 * Allows one to draw 1-bit graphcs onto a graphics context and to retrieve
 * the resulting bitmap in the form of a Pixellator.
 *
 * @author   Mark Taylor
 * @since    23 Aug 2007
 */
public class GraphicsPixellator implements Pixellator {

    private final Rectangle bounds_;
    private final int npix_;
    private final Graphics2D g_;
    private final byte[] buffer_;
    private final byte clearByte_;
    private int ipix_;

    /**
     * Constructor.
     *
     * @param   bounds   bounds of the bitmapped region
     */
    public GraphicsPixellator( Rectangle bounds ) {
        bounds_ = new Rectangle( bounds );
        npix_ = bounds.width * bounds.height;

        /* Construct a BufferedImage which will be backed by a byte array
         * (one byte per pixel).  Could try TYPE_BYTE instead for a one bit
         * per pixel array? */
        BufferedImage image = 
            new BufferedImage( bounds.width, bounds.height,
                               BufferedImage.TYPE_BYTE_GRAY );

        /* Obtain the actual byte array which backs this image. 
         * This relies on some not-quite-explicitly-written assumptions
         * from java.awt.image.BufferedImage and friends, but using this
         * buffer directly, rather than going through SampleModels and such,
         * should be more efficient. */
        buffer_ = ((DataBufferByte) image.getRaster().getDataBuffer())
                 .getData();

        /* Acquire a Graphics object which can write to the image. */
        Graphics2D g = image.createGraphics();

        /* Make sure that we know what white looks like. */
        g.setColor( Color.WHITE );
        g.fillRect( 0, 0, 1, 1 );
        clearByte_ = buffer_[ 0 ];
        clear();

        /* Since we only want a 1 bit deep image, antialiasing is not our
         * friend today. */
        g.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_OFF );
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_OFF );

        /* Translate the graphics context and fix the clip properly. */
        g_ = (Graphics2D) g.create( - bounds_.x, - bounds_.y,
                                    bounds_.width, bounds_.height );
        g_.setColor( Color.BLACK );
        g_.setClip( bounds_.x, bounds_.y, bounds_.width, bounds_.height );
    }

    /**
     * Returns the graphics context for drawing on this object.
     *
     * @return   graphics context
     */
    public Graphics2D getGraphics() {
        return g_;
    }

    /**
     * Clears the bitmap to a white background.
     */
    public void clear() {
        Arrays.fill( buffer_, clearByte_ );
    }

    public Rectangle getBounds() {
        return new Rectangle( bounds_ );
    }

    public void start() {
        ipix_ = -1;
    }

    public boolean next() {
        while ( ++ipix_ < npix_ && buffer_[ ipix_ ] == clearByte_ );
        return ipix_ < npix_;
    }

    public int getX() {
        return ipix_ % bounds_.width;
    }

    public int getY() {
        return ipix_ / bounds_.width;
    }
}
