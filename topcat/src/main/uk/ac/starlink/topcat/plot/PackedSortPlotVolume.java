package uk.ac.starlink.topcat.plot;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.Arrays;

/**
 * PlotVolume implementation which accumulates all the points to plot,
 * sorts them according to Z coordinate, and then plots them in order.
 * Variable transparency and fog are rendered properly.
 * The points are stored in an efficient form (each one packed into a long) -
 * the naive implementation which uses objects for this turns out to
 * be much slower (as previously implemented in <code>SortPlotVolume</code>).
 * 
 * @author   Mark Taylor
 * @since    19 Jan 2005
 */
public class PackedSortPlotVolume extends PlotVolume {

    private final int xdim_;
    private final int ydim_;
    private final int xoff_;
    private final int yoff_;
    private final long[] points_;
    private final float[][] rgbaBufs_;
    private final int[] rgbBuf_;
    private final BufferedImage image_;
    private final double zmin_;
    private final double zmax_;
    private final double zmult_;
    private int ipoint_;

    /**
     * Upper limit for the X/Y dimensions of the component which will be
     * drawn.  Anything points outside this limit will be ignored.
     */
    private static final int TWO12 = 2 << 12;

    /**
     * Constructs a new plot volume.
     * @param   c  component on which points will be plotted
     * @param   g  graphics context on which points will be plotted
     * @param   styles  array of marker styles which may be used to plot
     * @param   padFactor  minimum amount of space outside the unit cube
     *          in both dimensions - 1 means no extra space
     * @param   padBorders  space, additional to padFactor, to be left around
     *          the edges of the plot; order is (left,right,bottom,top)
     * @param   npoint  an upper limit on number of points which will be
     *          plotted (number of times <code>plot</code> will be called)
     * @param   zmin  a lower limit for z coordinates of plotted points
     * @param   zmax  an upper limit for z coordinates of plotted points
     * @param   ws   workspace object
     */
    public PackedSortPlotVolume( Component c, Graphics g, MarkStyle[] styles,
                                 double padFactor, int[] padBorders,
                                 int npoint, double zmin, double zmax,
                                 Workspace ws ) {
        super( c, g, styles, padFactor, padBorders );
        ipoint_ = 0;

        /* Store the z dimension scaling factors. */
        zmin_ = zmin;
        zmax_ = zmax;
        zmult_ = ( zmax - zmin ) * Integer.MAX_VALUE;

        /* Work out the dimensions of the pixel grid that we're going
         * to need. */
        int ppad = 6;
        xdim_ = c.getWidth() + 2 * ppad;
        ydim_ = c.getHeight() + 2 * ppad;
        xoff_ = - ppad;
        yoff_ = - ppad;

        /* Initialise the workspace accordingly, and acquire from it the
         * various buffer objects we will require. */
        ws.init( npoint, xdim_, ydim_ );
        points_ = ws.points_;
        rgbaBufs_ = ws.rgbaBufs_;
        rgbBuf_ = ws.rgbBuf_;
        image_ = ws.image_;
    }

    /**
     * Accept a point for plotting.
     */
    public void plot( int xp, int yp, double z, int is ) {
        if ( z >= zmin_ && z <= zmax_ &&
             xp >= 0 && xp <= TWO12 &&
             yp >= 0 && yp <= TWO12 ) {
            points_[ ipoint_++ ] = pack( xp, yp, z, is );
        }
    }

    /**
     * Perform the plot.
     */
    public void flush() {
        MarkStyle[] styles = getStyles();
        int nstyle = styles.length;

        /* Set up pixel offset arrays for each style - this enables us to
         * fill the pixel array with the pixels for each style very
         * efficiently for each marker that is plotted. */
        int[][] pixoffs = new int[ nstyle ][];
        for ( int is = 0; is < nstyle; is++ ) {
            MarkStyle style = styles[ is ];
            int[] xypixoffs = style.getPixelOffsets();
            int npixoff = xypixoffs.length / 2;
            pixoffs[ is ] = new int[ npixoff ];
            for ( int ioff = 0; ioff < npixoff; ioff++ ) {
                int xoffi = xypixoffs[ ioff * 2 + 0 ];
                int yoffi = xypixoffs[ ioff * 2 + 1 ];
                pixoffs[ is ][ ioff ] = xoffi + yoffi * xdim_;
            }
        }

        /* Get basic RGB colours for each style. */
        float[][] rgbas = new float[ nstyle ][];
        for ( int is = 0; is < nstyle; is++ ) {
            MarkStyle style = styles[ is ];
            rgbas[ is ] = style.getColor().getRGBComponents( null );
            rgbas[ is ][ 3 ] = 1.0f / style.getOpaqueLimit();
        }

        /* Prepare buffers to hold normalized red, green, blue, alpha 
         * components. */
        float[] rBuf = rgbaBufs_[ 0 ];
        float[] gBuf = rgbaBufs_[ 1 ];
        float[] bBuf = rgbaBufs_[ 2 ];
        float[] aBuf = rgbaBufs_[ 3 ];

        /* Sort the points into Z-coordinate order, with the nearest ones
         * first.  This works because the highest 32 bits of the packed
         * point reprensentations (longs) give the Z coordinate as a scaled
         * integer.  The lower 32 bits contain junk in this context, but
         * 32 bits will give easily good enough ordering for the plot to 
         * look right. */
        Arrays.sort( points_, 0, ipoint_ );

        /* Now work backwards from the nearest to the most distant points.
         * For each one fill the RGB and alpha components until we have
         * an alpha of unity, and then stop adding. */
        float[] b4 = new float[ 4 ];
        Fogger fogger = getFogger();
        for ( int ip = 0; ip < ipoint_; ip++ ) {
            long datum = points_[ ip ];
            int xp = unpackX( datum );
            int yp = unpackY( datum );
            float z = (float) unpackZ( datum );
            int is = unpackStyleIndex( datum );
            int[] poffs = pixoffs[ is ];
            int npoff = poffs.length;
            float[] rgba = rgbas[ is ];
            int ix = xp - xoff_;
            int iy = yp - yoff_;
            int base = ix + iy * xdim_;

            /* Treat each pixel painted by this marker. */
            for ( int ioff = 0; ioff < npoff; ioff++ ) {
                int ipix = base + poffs[ ioff ];
                float alpha = aBuf[ ipix ];

                /* Only continue if we haven't used up all the opacity of this
                 * pixel. */
                if ( alpha < 1f ) {
                    float remain = 1f - alpha;
                    b4[ 0 ] = rgba[ 0 ];
                    b4[ 1 ] = rgba[ 1 ];
                    b4[ 2 ] = rgba[ 2 ];
                    b4[ 3 ] = rgba[ 3 ];
                    fogger.fogAt( z, b4 );
                    float weight = Math.min( remain, rgba[ 3 ] );
                    aBuf[ ipix ] += weight * 1f;
                    rBuf[ ipix ] += weight * b4[ 0 ];
                    gBuf[ ipix ] += weight * b4[ 1 ];
                    bBuf[ ipix ] += weight * b4[ 2 ];
                }
            }
        }

        /* Turn the RGBA float arrays into something you can put into an
         * image for painting.  You'd think that with all the complication
         * of BufferedImage, WritableRaster and DataBuffer it would be 
         * possible to render directly from RGBA arrays of floats, but
         * it seems not. */
        int npix = xdim_ * ydim_;
        ColorModel colorModel = image_.getColorModel();
        float[] rgba = new float[ 4 ];
        for ( int ipix = 0; ipix < npix; ipix++ ) {
            float a1 = 1f / aBuf[ ipix ];
            rgba[ 0 ] = rBuf[ ipix ] * a1;
            rgba[ 1 ] = gBuf[ ipix ] * a1;
            rgba[ 2 ] = bBuf[ ipix ] * a1;
            rgba[ 3 ] = aBuf[ ipix ];
            rgbBuf_[ ipix ] = colorModel.getDataElement( rgba, 0 );
        }
        image_.setRGB( 0, 0, xdim_, ydim_, rgbBuf_, 0, xdim_ );

        /* Finally paint the image onto the graphics context. */
        getGraphics().drawImage( image_, xoff_, yoff_, null );
    }

    /**
     * Packs information about a point into a long integer.
     * The packing results (to a good approximation) in values which
     * represent a smaller z coordinate having a lower numeric value.
     *
     * @param  xp  graphics x coordinate
     * @param  yp  graphics y coordinate
     * @param  z   z coordinate
     * @param  is  style index
     * @return   packed representation of parameters
     */
    private long pack( int xp, int yp, double z, int is ) {
        long zint = (long) ( ( z - zmin_ ) * zmult_ );
        assert zint >= 0 && zint <= Integer.MAX_VALUE;
        assert xp >= 0 && xp < TWO12;
        assert yp >= 0 && yp < TWO12;
        return zint << 32
             | (long) ( xp << 20 | yp << 8 | ( is & 0xff ) );
    }

    /**
     * Unpacks z coordinate value from packed long integer.
     *
     * @param   val  value produced by <code>pack</code>
     * @return   z coordinate
     */
    private double unpackZ( long val ) {
        int zint = (int) ( val >> 32 );
        double z = ( zint / zmult_ ) + zmin_;
        assert z >= zmin_ && z <= zmax_;
        return z;
    }

    /**
     * Unpacks x coordinate value from packed long integer.
     *
     * @param   val  value produced by <code>pack</code>
     * @return   x coordinate
     */
    private int unpackX( long val ) {
        int xp = ( ((int) val) >> 20 ) & 0xfff;
        assert xp >= 0 && xp < TWO12;
        return xp;
    }

    /**
     * Unpacks y coordinate value from packed long integer.
     *
     * @param   val  value produced by <code>pack</code>
     * @return   y coordinate
     */
    private int unpackY( long val ) {
        int yp = ( ((int) val) >> 8 ) & 0xfff;
        assert yp >= 0 && yp < TWO12;
        return yp;
    }

    /**
     * Unpacks style index value from packed long integer.
     *
     * @param   val  value produced by <code>pack</code>
     * @return  style index
     */
    private int unpackStyleIndex( long val ) {
        int is = ((int) val) & 0xff;
        assert is >= 0 && is < getStyles().length;
        return is;
    }

    /**
     * Opaque workspace object for use with PackedSortPlotVolume instances.
     * These buffers are expensive to create and to garbage collect, so
     * if you are going to use a sequence of PackedSortPlotVolumes, you are
     * encouraged to use the same Workspace object for each one.
     * You cannot however use the same Workspace object for two
     * PackedSortPlotVolumes which are simultaneously active.
     */
    public static class Workspace {
        private int npoint_ = -1;
        private int xdim_ = -1;
        private int ydim_ = -1;

        private long[] points_;
        private float[][] rgbaBufs_;
        private int[] rgbBuf_;
        private BufferedImage image_;

        /**
         * Initialise this buffer for use with a pixel buffer of dimension
         * <code>xdim x ydim<code> able to plot at <code>npoint</code> points.
         *
         * @param   npoint  number of points
         * @param   xdim  X dimension
         * @param   ydim  Y dimension
         */
        private void init( int npoint, int xdim, int ydim ) {

            if ( npoint == npoint_ ) {
                Arrays.fill( points_, 0L );
            }
            else {
                npoint_ = npoint;
                points_ = new long[ npoint ];
            }

            if ( xdim == xdim_ && ydim == ydim_ ) {
                for ( int i = 0; i < 4; i++ ) {
                    Arrays.fill( rgbaBufs_[ i ], 0f );
                }
                Arrays.fill( rgbBuf_, 0 );
            }
            else {
                xdim_ = xdim;
                ydim_ = ydim;
                int npix = xdim * ydim;
                rgbaBufs_ = new float[ 4 ][ npix ];
                rgbBuf_ = new int[ npix ];
                image_ = new BufferedImage( xdim, ydim,
                                            BufferedImage.TYPE_INT_ARGB );
            }
        }
    }
}
