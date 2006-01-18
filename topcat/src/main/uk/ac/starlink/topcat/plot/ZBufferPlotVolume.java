package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.BitSet;

/**
 * PlotVolume which uses a Z-buffer to keep track of which pixels are in
 * front.
 *
 * @author   Mark Taylor
 * @since    18 Jan 2006
 */
public class ZBufferPlotVolume extends PlotVolume {

    private final int xdim_;
    private final int ydim_;
    private final int xoff_;
    private final int yoff_;
    private final float[] zbuf_;
    private final byte[] sbuf_;
    private final int[] rgbBuf_;
    private final BitSet mask_;
    private final int[][] pixoffs_;
    private final BufferedImage image_;

    /**
     * Constructs a new plot volume.
     * Note that a workspace <code>ws</code> must be provided.
     * If you're going to create multiple successive ZBufferPlotVolume
     * instances, you can cut down dramatically on garbage collection time
     * by supplying the same workspace to each one.
     * Don't use the same one for multiple volumes operating concurrently
     * though.
     *
     * @param   c  component on which points will be plotted
     * @param   g  graphics context on which points will be plotted
     * @param   styles  array of marker styles which may be used to plot
     * @param   padFactor  minimum amount of space outside the unit cube
     *          in both dimensions - 1 means no extra space
     * @param   padBorders  space, additional to padFactor, to be left around
     *          the edges of the plot; order is (left,right,bottom,top)
     * @param   ws  workspace object 
     */
    public ZBufferPlotVolume( Component c, Graphics g, MarkStyle[] styles,
                              double padFactor, int[] padBorders,
                              Workspace ws ) {
        super( c, g, styles, padFactor, padBorders );

        /* Work out the dimensions of the pixel grid that we're going
         * to need. */
        int ppad = 6;
        xdim_ = c.getWidth() + 2 * ppad;
        ydim_ = c.getHeight() + 2 * ppad;
        xoff_ = - ppad;
        yoff_ = - ppad;

        /* Initialise the workspace accordingly, and acquire from it the
         * various buffer objects we will require. */
        ws.init( xdim_, ydim_ );
        zbuf_ = ws.zbuf_;
        sbuf_ = ws.sbuf_;
        rgbBuf_ = ws.rgbBuf_;
        mask_ = ws.mask_;
        image_ = ws.image_;

        /* Initialise the Z buffer to be full of infinitely distant
         * background. */
        Arrays.fill( zbuf_, Float.MAX_VALUE );

        /* Set up pixel offset arrays for each style - this enables us to
         * fill the pixel array with the pixels for each style very 
         * efficiently for each marker that is plotted. */
        int nstyle = styles.length;
        pixoffs_ = new int[ nstyle ][];
        for ( int is = 0; is < nstyle; is++ ) {
            MarkStyle style = styles[ is ];
            int[] xypixoffs = style.getPixelOffsets();
            int npixoff = xypixoffs.length / 2;
            pixoffs_[ is ] = new int[ npixoff ];
            for ( int ioff = 0; ioff < npixoff; ioff++ ) {
                int xoffi = xypixoffs[ ioff * 2 + 0 ];
                int yoffi = xypixoffs[ ioff * 2 + 1 ];
                pixoffs_[ is ][ ioff ] = xoffi + yoffi * xdim_;
            }
        }
    }

    public void plot( int xp, int yp, double zd, int isi ) {
        float z = (float) zd;
        byte is = (byte) isi;

        /* Iterate over each pixel which is painted by a marker in the
         * current style at the current position. */
        int[] pixoffs = pixoffs_[ is ];
        int npix = pixoffs.length;
        int xbase = xp - xoff_;
        int ybase = yp - yoff_;
        int base = xbase + xdim_ * ybase;
        int npixoff = pixoffs.length;
        for ( int ioff = 0; ioff < npixoff; ioff++ ) {
            int ipix = base + pixoffs[ ioff ];

            /* If the pixel isn't already filled with something nearer the
             * viewer than this... */
            if ( z <= zbuf_[ ipix ] ) {

                /* Record that we've touched this pixel. */
                mask_.set( ipix );

                /* Set the Z buffer element to the current z position. */
                zbuf_[ ipix ] = z;

                /* Set the style buffer element to the current style index. */
                sbuf_[ ipix ] = is;
            }
        }
    }

    public void flush() {
        Graphics g = getGraphics();
        Fogger fogger = getFogger();

        /* Get basic RGB colours for each style. */
        MarkStyle[] styles = getStyles();
        int nstyle = styles.length;
        int[] argbs = new int[ nstyle ];
        for ( int is = 0; is < nstyle; is++ ) {
            Color color = styles[ is ].getColor();
            argbs[ is ] = styles[ is ].getColor().getRGB();
        }

        /* Fill a buffer of RGB values based on the Z and style index
         * buffers. */
        int xmin = xdim_;
        int xmax = 0;
        int ymin = ydim_;
        int ymax = 0;
        for ( int ipix = mask_.nextSetBit( 0 ); ipix >= 0;
              ipix = mask_.nextSetBit( ipix + 1 ) ) {
            int iy = ipix / xdim_;
            int ix = ipix % xdim_;
            xmin = Math.min( xmin, ix );
            xmax = Math.max( xmax, ix );
            ymin = Math.min( ymin, iy );
            ymax = Math.max( ymax, iy );
            float z = zbuf_[ ipix ];
            int is = (int) sbuf_[ ipix ];
            fogger.setZ( z );
            rgbBuf_[ ipix ] = fogger.tweakARGB( argbs[ is ] );
        }

        /* Take the rectangle of the RGB buffer which was affected, 
         * use it to populate a BufferedImage, and write that to the
         * graphics context we are supposed to be plotting to. */
        if ( xmax >= xmin && ymax >= ymin ) {
            int width = xmax - xmin + 1;
            int height = ymax - ymin + 1;
            image_.setRGB( xmin, ymin, width, height, rgbBuf_,
                           xmin + ymin * xdim_, xdim_ );
            g.drawImage( image_.getSubimage( xmin, ymin, width, height ),
                         xoff_ + xmin, yoff_ + ymin, null );
        }
    }

    /**
     * Opaque workspace object for use with ZBufferPlotVolume instances.
     * These buffers are expensive to create and to garbage collect, so
     * if you are going to use a sequence of ZBufferPlotVolumes, you are
     * encouraged to use the same Workspace object for each one.
     * You cannot however use the same Workspace object for two
     * ZBufferPlotVolumes which are simultaneously active.
     */
    public static class Workspace {

        private int xdim_ = -1;
        private int ydim_ = -1;
        private float[] zbuf_;
        private byte[] sbuf_;
        private int[] rgbBuf_;
        private BitSet mask_;
        private BufferedImage image_;

        /**
         * Initialise this buffer for use with a pixel buffer of dimension
         * <code>xdim x ydim<code>.
         *
         * @param   xdim  X dimension
         * @param   ydim  Y dimension
         */
        private void init( int xdim, int ydim ) {

            /* If we are already this shape, just clear the buffers. */
            if ( xdim == xdim_ && ydim == ydim_ ) {
                Arrays.fill( zbuf_, 0f );
                Arrays.fill( sbuf_, (byte) 0 );
                Arrays.fill( rgbBuf_, 0 );
                mask_.clear();
            }

            /* Otherwise, create new buffers the right shape. */
            else {
                xdim_ = xdim;
                ydim_ = ydim;
                int npix = xdim * ydim;
                zbuf_ = new float[ npix ];
                sbuf_ = new byte[ npix ];
                rgbBuf_ = new int[ npix ];
                mask_ = new BitSet( npix );
                image_ = new BufferedImage( xdim, ydim,
                                            BufferedImage.TYPE_INT_ARGB );
            }
        }
    }
}
