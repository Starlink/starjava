package uk.ac.starlink.ttools.plot;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.SampleModel;
import java.util.Arrays;

/**
 * Allows one to draw 1-bit graphcs onto a graphics context and to retrieve
 * the resulting bitmap in the form of a Pixellator.
 *
 * <p>It took me <em>ages</em> slogging through the <code>java.awt.image</code>
 * javadocs to work out how to get a graphics context backed by a primitive
 * array buffer.
 *
 * @author   Mark Taylor
 * @since    23 Aug 2007
 */
public class GraphicsBitmap {

    private final int width_;
    private final int height_;
    private final BufferedImage image_;
    private final DataBuffer dbuffer_;
    private final SampleModel sampler_;

    /**
     * Constructor.
     *
     * @param  width   width of the bitmapped region
     * @param  height  height of the bitmapped region
     */
    public GraphicsBitmap( int width, int height ) {
        width_ = width;
        height_ = height;

        /* Construct a BufferedImage which will be backed by a byte array. 
         * TYPE_BYTE_BINARY would be more efficient for space, but it's
         * much slower. */
        image_ = new BufferedImage( width, height,
                                    BufferedImage.TYPE_BYTE_GRAY );

        /* Get the byte buffer which backs the image. */
        dbuffer_ = image_.getRaster().getDataBuffer();
        assert dbuffer_ instanceof DataBufferByte;

        /* Get the sample model which allows interpretation of the buffer. */
        sampler_ = image_.getSampleModel();
    }

    /**
     * Returns a graphics context which can draw monochrome graphics on 
     * this bitmap.
     *
     * @return   graphics context
     */
    public Graphics2D createGraphics() {
        Graphics2D g2 = image_.createGraphics();
        g2.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING,
                             RenderingHints.VALUE_TEXT_ANTIALIAS_OFF );
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_OFF );
        return g2;
    }

    /**
     * Clears the bitmap.
     */
    public void clear() {
        if ( dbuffer_ instanceof DataBufferByte ) {
            Arrays.fill( ((DataBufferByte) dbuffer_).getData(), (byte) 0 );
        }
        else {
            assert false;
            for ( int iy = 0; iy < height_; iy++ ) {
                for ( int ix = 0; ix < width_; ix++ ) {
                    sampler_.setSample( ix, iy, 0, 0, dbuffer_ );
                }
            }
        }
    }

    /**
     * Returns the width of this bitmap.
     *
     * @return  width
     */
    public int getWidth() {
        return width_;
    }

    /**
     * Returns the height of this bitmap.
     *
     * @return  height
     */
    public int getHeight() {
        return height_;
    }

    /**
     * Returns a pixellator which will iterate over the painted pixels in
     * this bitmap.
     * Pixels in the pixellator reflect the pixel state at iteration time.
     *
     * @return  bitmap pixellator
     */
    public Pixellator createPixellator() {
        return createPixellator( null );
    }

    /**
     * Returns a pixellator which will iterate over a sub-region of the painted
     * pixels in this bitmap.
     * Pixels in the pixellator reflect the pixel state at iteration time.
     *
     * @param    bounds   region of space for which the pixellator is wanted
     * @return   bitmap subset pixellator
     */
    public Pixellator createPixellator( Rectangle bounds ) {
        return ( ( bounds == null ||
                   bounds.contains( new Rectangle( 0, 0, width_, height_ ) ) )
                 && sampler_.getSampleSize( 0 ) == 8 )
             ? (Pixellator) new TotalPixellator()
             : (Pixellator) new PartialPixellator( bounds );
    }

    /**
     * Pixellator which iterates over all the pixels in this bitmap.
     * It makes some assumptions about the layout of the SampleModel in use -
     * these are probably good, but it might possibly fail under weirdly
     * implemented (i.e. non-Sun) JREs.
     */
    private class TotalPixellator implements Pixellator {

        private final byte[] bytes_;
        private final int npix_;
        private int ipix_;

        /**
         * Constructor.
         */
        TotalPixellator() {
            bytes_ = ((DataBufferByte) dbuffer_).getData();
            npix_ = width_ * height_;
            ipix_ = npix_;
        }

        public Rectangle getBounds() {
            return new Rectangle( 0, 0, width_, height_ );
        }

        public void start() {
            ipix_ = -1;
        }

        public boolean next() {
            while ( ++ipix_ < npix_ && bytes_[ ipix_ ] == 0 );
            return ipix_ < npix_;
        }

        public int getX() {
            return ipix_ % width_;
        }

        public int getY() {
            return ipix_ / width_;
        }
    }

    /**
     * Pixellator which iterates over a subregion of the pixels in this
     * bitmap.
     */
    private class PartialPixellator implements Pixellator {

        private final Rectangle bounds_;
        private int ix_;
        private int iy_;

        /**
         * Constructor.
         *
         * @param   bounds  region of pixels within this bitmap's range
         *          which we are interested in
         */
        PartialPixellator( Rectangle bounds ) {

            /* Check that the request is not for a region larger than the
             * buffered image.  It would be possible to satisfy this request,
             * but anyone who wants an outsize pixellator has almost certainly
             * made a logic error, so better to fail. */
            if ( bounds.x < 0 || bounds.y < 0 ||
                 bounds.width > width_ || bounds.height > height_ ) {
                throw new IllegalArgumentException(
                    "Bounds bigger than bitmap" );
            }
            bounds_ = bounds;
            ix_ = bounds_.x + bounds_.width;
            iy_ = bounds_.y + bounds_.height;
        }

        public Rectangle getBounds() {
            return new Rectangle( bounds_ );
        }

        public void start() {
            ix_ = bounds_.x;
            iy_ = bounds_.y;
        }

        public boolean next() {
            boolean done = false;
            do {
                if ( ++ix_ >= bounds_.x + bounds_.width ) {
                    ix_ = 0;
                    if ( ++iy_ >= bounds_.y + bounds_.height ) {
                        done = true;
                    }
                }
            } while ( ! done &&
                      sampler_.getSample( ix_, iy_, 0, dbuffer_ ) == 0 );
            return ! done;
        }

        public int getX() {
            return ix_;
        }

        public int getY() {
            return iy_;
        }
    }
}
