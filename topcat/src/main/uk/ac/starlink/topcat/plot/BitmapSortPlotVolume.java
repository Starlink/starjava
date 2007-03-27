package uk.ac.starlink.topcat.plot;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.util.LongList;

/**
 * PlotVolume implementation which accumulates all points to plot and then
 * draws them in a way suitable for a bitmap-type context.
 * Transparency is rendered.  Points with and without error bars can be
 * plotted.
 *
 * @author   Mark Taylor
 * @since    26 Mar 2007
 */
public class BitmapSortPlotVolume extends PlotVolume {

    private PointStore pointStore_;
    private int iseq_;
    private final int xdim_;
    private final int ydim_;
    private final int xoff_;
    private final int yoff_;
    private final int[][] pixoffs_;
    private final float[][] rgbas_;
    private final float[][] rgbaBufs_;
    private final int[] rgbBuf_;
    private final BufferedImage image_;

    /**
     * Constructor.
     *
     * @param   c  component
     * @param   g  graphics context
     * @param   styles array of marker styles which may be used to plot
     * @param   padFactor  minimum amount of space outside the unit cube
     *          in both dimensions - 1 means no extra space
     * @param   padBorders  space, additional to padFactor, to be left around
     *          the edges of the plot; order is (left,right,bottom,top)
     * @param   hasErrors  must be true if any of the points to be plotted
     *          will contain errors
     * @param   zmin  a lower limit for z coordinates of plotted points
     * @param   zmax  an upper limit for z coordinates of plotted points
     * @param   ws   workspace object
     */
    public BitmapSortPlotVolume( Component c, Graphics g, MarkStyle[] styles,
                                 double padFactor, int[] padBorders,
                                 boolean hasErrors, double zmin, double zmax,
                                 Workspace ws ) {
        super( c, g, styles, padFactor, padBorders );

        /* Construct an appropriate PointStore object.  If there are no 
         * errors then all information for each object can be packed into
         * a long, which is good for processing and memory efficiency.
         * If errors are (may be) present there is too much information
         * for that, and we need a more conventional way of storing points. */
        pointStore_ = hasErrors
                    ? (PointStore) new ObjectPointStore()
                    : (PointStore) new PackedPointStore( zmin, zmax );

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

        /* Initialise the workspace accordingly, and acquire from it the
         * various buffer objects we will require. */
        ws.init( xdim_, ydim_ );
        rgbaBufs_ = ws.rgbaBufs_;
        rgbBuf_ = ws.rgbBuf_;
        image_ = ws.image_;

        /* Set up pixel offsets for each style - this enables us to
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

        /* Set up basic RGB colours for each style. */
        rgbas_ = new float[ nstyle ][];
        for ( int is = 0; is < nstyle; is++ ) {
            MarkStyle style = styles[ is ];
            rgbas_[ is ] = style.getColor().getRGBComponents( null );
            rgbas_[ is ][ 3 ] = 1.0f / style.getOpaqueLimit();
        }
    }

    public void plot2d( int px, int py, double z, int istyle ) {
        pointStore_.addPoint( px, py, z, istyle );
    }

    public void plot2d( int px, int py, double z, int istyle, 
                        boolean showPoint, int nerr, int[] xoffs, int[] yoffs,
                        double[] zerrs ) {
        pointStore_.addPoint( px, py, z, istyle, showPoint, nerr,
                              xoffs, yoffs );
    }

    /**
     * Do the plotting.
     */
    public void flush() {

        /* Prepare buffers to hold normalized red, green, blue, alpha
         * components. */
        float[] rBuf = rgbaBufs_[ 0 ];
        float[] gBuf = rgbaBufs_[ 1 ];
        float[] bBuf = rgbaBufs_[ 2 ];
        float[] aBuf = rgbaBufs_[ 3 ];

        /* Now work from the nearest to the most distant points.
         * For each one fill the RGB and alpha components until we have
         * an alpha of unity, and then stop adding. */
        float[] b4 = new float[ 4 ];
        Fogger fogger = getFogger();
        for ( Iterator it = pointStore_.getSortedPointIterator();
              it.hasNext(); ) {
            BitmapPoint3D point = (BitmapPoint3D) it.next();
            int base = point.getPixelBase( this );
            int[] pixoffs = point.getPixelOffsets( this );
            float[] rgba = point.getRgba( this );
            double z = point.getZ();

            /* Treat each pixel painted by this point. */
            int npix = pixoffs.length;
            for ( int ioff = 0; ioff < npix; ioff++ ) {
                int ipix = base + pixoffs[ ioff ];
                float alpha = aBuf[ ipix ];

                /* Only continue if we haven't used up all the opacity of
                 * this pixel. */
                if ( alpha < 1f ) {
                    b4[ 0 ] = rgba[ 0 ];
                    b4[ 1 ] = rgba[ 1 ];
                    b4[ 2 ] = rgba[ 2 ];
                    b4[ 3 ] = rgba[ 3 ];
                    fogger.fogAt( z, b4 );
                    float remain = 1f - alpha;
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
        ColorModel colorModel = image_.getColorModel();
        float[] rgba = new float[ 4 ];
        Arrays.fill( rgbBuf_, colorModel.getDataElement( rgba, 0 ) );
        int npix = xdim_ * ydim_;
        for ( int ipix = 0; ipix < npix; ipix++ ) {
            float a = aBuf[ ipix ];
            if ( a > 0f ) {
                float a1 = 1f / aBuf[ ipix ];
                rgba[ 0 ] = rBuf[ ipix ] * a1;
                rgba[ 1 ] = gBuf[ ipix ] * a1;
                rgba[ 2 ] = bBuf[ ipix ] * a1;
                rgba[ 3 ] = a;
                rgbBuf_[ ipix ] = colorModel.getDataElement( rgba, 0 );
            }
        }
        image_.setRGB( 0, 0, xdim_, ydim_, rgbBuf_, 0, xdim_ );

        /* Finally paint the image onto the graphics context. */
        getGraphics().drawImage( image_, xoff_, yoff_, null );
    }

    /**
     * Abstract superclass defining an object which can store points 
     * to be plotted and later return them sorted by Z coordinate.
     */
    private static abstract class PointStore {

        /**
         * Stores a point.
         *
         * @param   px   X coordinate
         * @param   py   Y coordinate
         * @param   z    Z coordinate
         * @param   istyle  index into styles array
         */
        public abstract void addPoint( int px, int py, double z, int istyle );

        /**
         * Stores a point with associated errors.
         *
         * @param   px   X coordinate
         * @param   py   Y coordinate
         * @param   z    Z coordinate
         * @param   istyle  index into styles array
         * @param   showPoint  whether to draw the marker as well
         * @param   nerr  number of error points
         * @param   xoffs  <code>nerr</code>-element array of error point
         *                 X offsets
         * @param   yoffs  <code>nerr</code>-element array of error point
         *                 Y offsets
         */
        public abstract void addPoint( int px, int py, double z, int istyle,
                                       boolean showPoint, int nerr, 
                                       int[] xoffs, int[] yoffs );

        /**
         * Returns an iterator over all the points which have been stored
         * in this object, sorted by Z coordinate.
         *
         * @return  iterator returning {@link BitmapPoint3D} objects
         */
        public abstract Iterator getSortedPointIterator();
    }

    /**
     * Straightforward PointStore implementation based on an ArrayList.
     */
    private static class ObjectPointStore extends PointStore {
        private List pointList_ = new ArrayList();

        public void addPoint( int px, int py, double z, int istyle ) {
            pointList_.add( new BitmapPoint3D( pointList_.size(), z,
                                               istyle, px, py ) );
        }

        public void addPoint( int px, int py, double z, int istyle, 
                              boolean showPoint, int nerr, int[] xoffs,
                              int[] yoffs ) {
            pointList_.add( new ErrorsBitmapPoint3D( pointList_.size(), z,
                                                     istyle, px, py, showPoint,
                                                     nerr, xoffs, yoffs ) );
        }

        public Iterator getSortedPointIterator() {
            BitmapPoint3D[] points =
                (BitmapPoint3D[]) pointList_.toArray( new BitmapPoint3D[ 0 ] );
            pointList_ = new ArrayList();
            Arrays.sort( points );
            return Arrays.asList( points ).iterator();
        }
    }

    /**
     * PointStore implementation which packs point representations into
     * long integers.
     */
    private static class PackedPointStore extends PointStore {
        private LongList pointList_;
        private final double zmin_;
        private final double zmax_;
        private final double zmult_;

        /**
         * Constructor.
         *
         * @param   zmin  a lower limit for z coordinates of plotted points
         * @param   zmax  an upper limit for z coordinates of plotted points
         */
        PackedPointStore( double zmin, double zmax ) {
            pointList_ = new LongList();
            zmax_ = zmax;
            zmin_ = zmin;
            zmult_ = Integer.MAX_VALUE / ( zmax - zmin );
        }

        /**
         * Upper limit for the X/Y dimensions of the component which will be
         * drawn.  Any points outside this limit will be ignored.
         */
        private static final int TWO12 = 2 << 12;

        public void addPoint( int px, int py, double z, int istyle ) {
            if ( z > zmin_ && z <= zmax_ && 
                 px >= 0 && px <= TWO12 &&
                 py >= 0 && py <= TWO12 ) {
                pointList_.add( pack( px, py, z, istyle ) );
            }
        }

        public void addPoint( int px, int py, double z, int istyle,
                              boolean showPoint, int nerr, int[] xoffs,
                              int[] yoffs ) {
            throw new UnsupportedOperationException();
        }

        public Iterator getSortedPointIterator() {
            final long[] points = pointList_.toLongArray();
            pointList_ = new LongList();

            /* Sort the points into Z-coordinate order, with the nearest
             * ones first.  This works because the highest 32 bits of the
             * packed point reprensentations (longs) give the Z coordinate
             * as a scaled integer.  The lower 32 bits contain junk in
             * this context, but 32 bits will give easily good enough
             * ordering for the plot to look right. */
            Arrays.sort( points );

            /* Return an iterator which will return BitmapPoint3D objects. */
            return new Iterator() {
                int ip;

                public boolean hasNext() {
                    return ip < points.length;
                }

                public Object next() {
                    long packed = points[ ip++ ];
                    double z = unpackZ( packed );
                    int px = unpackX( packed );
                    int py = unpackY( packed );
                    int istyle = unpackStyleIndex( packed );
                    return new BitmapPoint3D( -1, z, istyle, px, py );
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        /** 
         * Packs information about a point into a long integer.
         * The packing results (to a good approximation) in values which
         * represent a smaller z coordinate having a lower numeric value,
         * since z is stored at the most significant end.
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
            return ((int) val) & 0xff;
        }
    }

    /**
     * Point3D implementation which can render itself in a bitmap context.
     */
    private static class BitmapPoint3D extends Point3D {

        final int istyle_;
        final int px_;
        final int py_;

        /**
         * Constructor.
         *
         * @param  iseq  sequence number
         * @param  z     Z coordinate
         * @param  style  plotting style
         * @param  px    X coordinate
         * @param  py    Y coordinate
         */
        public BitmapPoint3D( int iseq, double z, int istyle, int px, int py ) {
            super( iseq, z, true );
            istyle_ = istyle;
            px_ = px;
            py_ = py;
        }

        /**
         * Returns an index into the volume's pixel buffer from which indices
         * returned from {@link #getPixelOffsets} should be offset.
         *
         * @param  vol   plot volume on which to operate
         * @return  base pixel offset
         */
        public int getPixelBase( BitmapSortPlotVolume vol ) {
            int ix = px_ - vol.xoff_;
            int iy = py_ - vol.yoff_;
            return ix + iy * vol.xdim_;
        }

        /**
         * Returns an array of pixel offsets into the plot volume's pixel
         * buffer relative to the value returned by {@link #getPixelBase} 
         * which should be coloured in by this point.
         *
         * @param  vol  plot volume on which to operate
         * @return   list of pixel offsets relative to base
         */
        public int[] getPixelOffsets( BitmapSortPlotVolume vol ) {
            return vol.pixoffs_[ istyle_ ];
        }

        /**
         * Returns the RGBA colour to plot this point in.
         *
         * @param   vol  plot volume on which to operate
         * @return   4-element array giving red, green, blue, alpha components
         */
        public float[] getRgba( BitmapSortPlotVolume vol ) {
            return vol.rgbas_[ istyle_ ];
        }
    }

    /**
     * Point3D implementation with errors.
     */
    private static class ErrorsBitmapPoint3D extends BitmapPoint3D {

        final boolean showPoint_;
        final int nerr_;
        final int[] xoffs_;
        final int[] yoffs_;
        private final static int[] NO_OFFSETS = new int[ 0 ];
  
        /**
         * Constructor.
         *
         * @param  iseq  unique sequence number
         * @param  z     Z coordinate
         * @param  style  plotting style
         * @param  px    X coordinate
         * @param  py    Y coordinate
         * @param  showPoint  whether to draw the marker as well
         * @param  nerr  number of error points
         * @param  xoffs  <code>nerr</code>-element array of error point
         *                X offsets
         * @param  yoffs  <code>nerr</code>-element array of error point
         *                Y offsets
         */
        public ErrorsBitmapPoint3D( int iseq, double z, int istyle,
                                    int px, int py, boolean showPoint,
                                    int nerr, int[] xoffs, int[] yoffs ) {
            super( iseq, z, istyle, px, py );
            showPoint_ = showPoint;
            nerr_ = nerr;
            xoffs_ = (int[]) xoffs.clone();
            yoffs_ = (int[]) yoffs.clone();
        }

        public int[] getPixelOffsets( BitmapSortPlotVolume vol ) {
            MarkStyle style = vol.getStyles()[ istyle_ ];
            ErrorRenderer erend = style.getErrorRenderer();
            int xbase = vol.xoff_;
            int ybase = vol.yoff_;
            int[] markOffs = showPoint_ ? super.getPixelOffsets( vol )
                                        : NO_OFFSETS;
            int[] errXYs = erend.getPixels( vol.getGraphics(),
                                            px_ - vol.xoff_, py_ - vol.yoff_,
                                            xoffs_, yoffs_ );
            if ( errXYs.length == 0 ) {
                return markOffs;
            }
            else {
                int nMark = markOffs.length;
                int nErr = errXYs.length / 2;
                int xdim = vol.xdim_;
                int base = getPixelBase( vol );
                int[] pixOffs = new int[ nMark + nErr ];
                System.arraycopy( markOffs, 0, pixOffs, 0, nMark );
                int ioff = nMark;
                for ( int ie = 0; ie < nErr; ie++ ) {
                    int xe = errXYs[ ie * 2 + 0 ];
                    int ye = errXYs[ ie * 2 + 1 ];
                    pixOffs[ ioff++ ] = xe + xdim * ye - base;
                }
                assert ioff == pixOffs.length;
                return pixOffs;
            }
        }
    }

    /**
     * Opaque workspace object for use with BitmapSortPlotVolume instances.
     * These buffers are expensive to create and to garbage collect, so
     * if you are going to use a sequence of BitmapSortPlotVolumes, you are
     * encouraged to use the same Workspace object for each one.
     * You cannot however use the same Workspace object for two
     * BitmapSortPlotVolumes which are simultaneously active.
     */
    public static class Workspace {
        private int xdim_ = -1;
        private int ydim_ = -1;

        private float[][] rgbaBufs_;
        private int[] rgbBuf_;
        private BufferedImage image_;

        /**
         * Initialise this buffer for use with a pixel buffer of dimension
         * <code>xdim</code> * <code>ydim<code>.
         * 
         * @param   xdim  X dimension 
         * @param   ydim  Y dimension
         */
        private void init( int xdim, int ydim ) {

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
