package uk.ac.starlink.ttools.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.BitSet;

/**
 * PlotVolume which uses a Z-buffer to keep track of which pixels are in
 * front.  It can only render opaque markers.
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
    private final int[] rgbBuf_;
    private final BitSet mask_;
    private final int[][] pixoffs_;
    private final BufferedImage image_;
    private final MarkStyle[] styles_;
    private final Graphics graphics_;
    private final Rectangle clip_;
    private final Paint markPaint_;
    private final Paint labelPaint_;

    /**
     * Packed RGBA value representing no colour.  This is used as a flag,
     * but must also be a valid packed colour which has no effect when
     * plotting.  Anything with alpha == 0 would do.
     */
    private static final int NO_RGBA = 0;

    /**
     * Constructs a new plot volume.
     *
     * @param   c  component on which points will be plotted
     * @param   g  graphics context on which points will be plotted
     * @param   styles  array of marker styles which may be used to plot
     * @param   padFactor  minimum amount of space outside the unit cube
     *          in both dimensions - 1 means no extra space
     * @param   fogginess  thickness of fog for depth shading
     * @param   hasLabels  true if any of the points may have associated labels
     * @param   padBorders  space, additional to padFactor, to be left around
     *          the edges of the plot; order is (left,right,bottom,top)
     * @param   tweaker  colour adjuster for using auxiliary axis coords
     * @param   ws  workspace object 
     */
    @SuppressWarnings("this-escape")
    public ZBufferPlotVolume( Component c, Graphics g, MarkStyle[] styles,
                              double padFactor, int[] padBorders,
                              double fogginess, boolean hasLabels,
                              DataColorTweaker tweaker, Workspace ws ) {
        super( c, g, styles, padFactor, padBorders, fogginess );
        graphics_ = g;
        styles_ = styles.clone();

        /* Work out the dimensions of the pixel grid that we're going
         * to need. */
        int ppad = 2;
        for ( int i = 0; i < styles.length; i++ ) {
            ppad = Math.max( ppad, 2 + 2 * styles[ i ].getMaximumRadius() );
        }
        xdim_ = c.getWidth() + 2 * ppad;
        ydim_ = c.getHeight() + 2 * ppad;
        xoff_ = - ppad;
        yoff_ = - ppad;
        clip_ = new Rectangle( graphics_.getClipBounds() );
        clip_.translate( ppad, ppad );

        /* Initialise the workspace accordingly, and acquire from it the
         * various buffer objects we will require. */
        ws.init( xdim_, ydim_ );
        zbuf_ = ws.zbuf_;
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
            pixoffs_[ is ] = styles[ is ].getFlattenedPixelOffsets( xdim_ );
        }

        /* Set up a suitable object for filling in the pixel values during
         * the plotting operations. */
        Color[] markColors = new Color[ nstyle ];
        Color[] labelColors = new Color[ nstyle ];
        for ( int is = 0; is < nstyle; is++ ) {
            markColors[ is ] = styles[ is ].getColor();
            labelColors[ is ] = Color.BLACK;
        }
        DataColorTweaker markTweaker = createFoggingTweaker( tweaker );
        markPaint_ = markTweaker != null
                   ? (Paint) new TweakedPaint( markColors, markTweaker )
                   : (Paint) new FixedPaint( markColors );
        DataColorTweaker labelTweaker = createFoggingTweaker( null );
        labelPaint_ = labelTweaker != null
                    ? (Paint) new TweakedPaint( labelColors, labelTweaker )
                    : (Paint) new FixedPaint( labelColors );
    }

    public void plot2d( int xp, int yp, double zd, double[] coords, int is,
                        boolean showPoint, String label,
                        int nerr, int[] xoffs, int[] yoffs, double[] zerrs ) {
        float z = (float) zd;
        int xbase = xp - xoff_;
        int ybase = yp - yoff_;
        int base = xbase + xdim_ * ybase;
        markPaint_.setColor( is, coords );

        /* Draw marker if required. */
        if ( showPoint ) {
            int[] pixoffs = pixoffs_[ is ];
            int npixoff = pixoffs.length;
            for ( int ioff = 0; ioff < npixoff; ioff++ ) {
                hitPixel( base + pixoffs[ ioff ], z, markPaint_ );
            }
        }

        /* Draw error bars if required. */
        if ( nerr > 0 ) {
            Pixellator epixer =
                styles_[ is ].getErrorRenderer()
               .getPixels( clip_, xbase, ybase, xoffs, yoffs );
            for ( epixer.start(); epixer.next(); ) {
                int pixoff = epixer.getX() + xdim_ * epixer.getY();
                hitPixel( pixoff, z, markPaint_ );
            }
        }

        /* Draw label if required. */
        if ( label != null && z <= zbuf_[ base ] ) {
            labelPaint_.setColor( is, coords );
            Pixellator lpixer =
                styles_[ is ].getLabelPixels( label, xbase, ybase, clip_ );
            if ( lpixer != null ) {
                for ( lpixer.start(); lpixer.next(); ) {
                    int pixoff = lpixer.getX() + xdim_ * lpixer.getY();
                    hitPixel( pixoff, z, labelPaint_ );
                }
            }
        }
    }

    public void flush() {
        Graphics g = getGraphics();

        /* Work out the region of the plot which has been affected. */
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
     * Deposit a point at a given index into the pixel buffer.
     * If it's behind an existing pixel there will be no effect.
     *
     * @param  ipix  pixel index
     * @param  z     Z buffer depth
     * @param  paint  determines pixel colour
     */
    private void hitPixel( int ipix, float z, Paint paint ) {

        /* If the pixel isn't already filled with something nearer the
         * viewer than this... */
        if ( z <= zbuf_[ ipix ] ) {

            /* Get the colour and check it's valid. */
            int rgb = paint.getRgb();
            if ( rgb != NO_RGBA ) {

                /* Record that we've touched this pixel. */
                mask_.set( ipix );

                /* Set the Z buffer element to the current z position. */
                zbuf_[ ipix ] = z;

                /* Set the style buffer element to the current style index. */
                rgbBuf_[ ipix ] = rgb;
            }
        }
    }

    /**
     * Defines the colour of pixels which will be drawn into the RGB buffer.
     * The point of defining this class rather than just working out the
     * RGB values for each (potentially) plotted point is so that 
     * calculation of actual RGB colour values only has to be done when
     * it is actually necessary.
     */
    private static abstract class Paint {

        /**
         * Sets the parameters which determine what colour pixels will be
         * painted in.
         *
         * @param   iset  set index  
         * @param   coords  the full coordinate array which may affect colouring
         */
        abstract void setColor( int iset, double[] coords );

        /**
         * The sRGB-encoded integer representing colour.
         *
         * @return  rgb colour value, or NO_RGBA for an invalid colour
         */
        abstract int getRgb();
    }

    /**
     * Paint implementation where each indexed set corresponds to a single
     * colour.
     */
    private static class FixedPaint extends Paint {
        final int[] rgbs_;
        int rgb_;

        /**
         * Constructor.
         *
         * @param  colors  array of per-set base colours
         */
        public FixedPaint( Color[] colors ) {
            int nstyle = colors.length;
            rgbs_ = new int[ nstyle ];
            for ( int is = 0; is < nstyle; is++ ) {
                rgbs_[ is ] = colors[ is ].getRGB();
            }
        }

        public void setColor( int iset, double[] coords ) {
            rgb_ = rgbs_[ iset ];
        }

        public int getRgb() {
            return rgb_;
        }
    }

    /**
     * Paint implementation where the colour of each set is influenced by a
     * DataColorTweaker object.
     */
    private static class TweakedPaint extends Paint {
        final float[][] frgbs_;
        final float[] frgb_;
        final DataColorTweaker tweaker_;
        final int ncoord_;
        final double[] coords_;
        int iset_;
        boolean rgbOk_;
        int rgb_;

        /**
         * Constructor.
         *
         * @param  styles  array of per-set base colours
         * @param  tweaker  colour adjuster
         */
        public TweakedPaint( Color[] colors, DataColorTweaker tweaker ) {
            int nstyle = colors.length;
            frgb_ = new float[ 4 ];
            frgbs_ = new float[ nstyle ][];
            for ( int is = 0; is < nstyle; is++ ) {
                frgbs_[ is ] = colors[ is ].getRGBComponents( null );
            }
            tweaker_ = tweaker;
            ncoord_ = tweaker.getNcoord();
            coords_ = new double[ ncoord_ ];
        }

        public void setColor( int iset, double[] coords ) {
            rgbOk_ = false;
            iset_ = iset;
            System.arraycopy( coords, 0, coords_, 0, ncoord_ );
        }

        public int getRgb() {
            if ( ! rgbOk_ ) {
                float[] frgb = frgbs_[ iset_ ];
                frgb_[ 0 ] = frgb[ 0 ];
                frgb_[ 1 ] = frgb[ 1 ];
                frgb_[ 2 ] = frgb[ 2 ];
                frgb_[ 3 ] = frgb[ 3 ];
                if ( tweaker_.setCoords( coords_ ) ) {
                    tweaker_.tweakColor( frgb_ );
                    float r = frgb_[ 0 ];
                    float b = frgb_[ 2 ];
                    frgb_[ 2 ] = r;
                    frgb_[ 0 ] = b;
                    rgb_ = packRgba( frgb_ );
                }
                else {
                    rgb_ = NO_RGBA;
                }
            }
            return rgb_;
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
                Arrays.fill( rgbBuf_, 0 );
                mask_.clear();
            }

            /* Otherwise, create new buffers the right shape. */
            else {
                xdim_ = xdim;
                ydim_ = ydim;
                int npix = xdim * ydim;
                zbuf_ = new float[ npix ];
                rgbBuf_ = new int[ npix ];
                mask_ = new BitSet( npix );
                image_ = new BufferedImage( xdim, ydim,
                                            BufferedImage.TYPE_INT_ARGB );
            }
        }
    }
}
