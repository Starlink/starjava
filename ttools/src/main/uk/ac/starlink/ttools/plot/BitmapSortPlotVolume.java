package uk.ac.starlink.ttools.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
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
    private boolean seenLabels_;
    private final int xdim_;
    private final int ydim_;
    private final int xoff_;
    private final int yoff_;
    private final Pixellator[] markPixoffs_;
    private final MarkStyle[] styles_;
    private final float[][] rgbas_;
    private final float[][] labelRgbas_;
    private final float[][] rgbaBufs_;
    private final int[] rgbBuf_;
    private final BufferedImage image_;
    private final DataColorTweaker tweaker_;

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
     * @param   fogginess  thickness of fog for depth shading
     * @param   hasLabels  must be true if any of the points to be plotted
     *          will have associated labels
     * @param   hasErrors  must be true if any of the points to be plotted
     *          will contain errors
     * @param   zmin  a lower limit for z coordinates of plotted points
     * @param   zmax  an upper limit for z coordinates of plotted points
     * @param   tweaker  colour adjuster for using auxiliary axis coords
     * @param   ws   workspace object
     */
    public BitmapSortPlotVolume( Component c, Graphics g, MarkStyle[] styles,
                                 double padFactor, int[] padBorders,
                                 double fogginess, boolean hasLabels,
                                 boolean hasErrors, double zmin, double zmax,
                                 DataColorTweaker tweaker, Workspace ws ) {
        super( c, g, styles, padFactor, padBorders, fogginess );
        styles_ = styles;

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
         * fill the pixel array with the pixels for each style
         * efficiently for each marker that is plotted. */
        int nstyle = styles.length;
        markPixoffs_ = new Pixellator[ nstyle ];
        for ( int is = 0; is < nstyle; is++ ) {
            MarkStyle style = styles[ is ];
            markPixoffs_[ is ] = style.getPixelOffsets();
        }

        /* Set up basic RGB colours for each style. */
        rgbas_ = new float[ nstyle ][];
        labelRgbas_ = new float[ nstyle ][];
        for ( int is = 0; is < nstyle; is++ ) {
            MarkStyle style = styles[ is ];
            rgbas_[ is ] = style.getColor().getRGBComponents( null );
            rgbas_[ is ][ 3 ] = 1.0f / style.getOpaqueLimit();
            labelRgbas_[ is ] = style.getLabelColor().getRGBComponents( null );
            labelRgbas_[ is ][ 3 ] = 1.0f;
        }

        /* Work out if there are auxiliary dimensions which may change
         * colouring of plotted points. */
        tweaker_ = tweaker;
        boolean hasAux = tweaker != null;

        /* Construct an appropriate PointStore object.  If only the point
         * positions need to be stored then all the information for each
         * object can be packed into a long, which is good for processing
         * and memory efficiency.  However if errors, labels or auxiliary axes
         * are (may be) present there is too much information for that, 
         * and we need a more conventional way of storing points. */
        if ( hasAux ) {
            pointStore_ = new AuxObjectPointStore( rgbas_, tweaker );
        }
        else if ( hasErrors || hasLabels ) {
            pointStore_ = new ObjectPointStore();
        }
        else {
            pointStore_ = new PackedPointStore( zmin, zmax );
        }
    }

    public void plot2d( int px, int py, double z, double[] coords, int istyle, 
                        boolean showPoint, String label, int nerr, 
                        int[] xoffs, int[] yoffs, double[] zerrs ) {
        pointStore_.addPoint( px, py, z, coords, istyle, showPoint, label,
                              nerr, xoffs, yoffs );
        seenLabels_ = seenLabels_ || label != null;
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
        Rectangle clip = getGraphics().getClipBounds();
        for ( Iterator it = pointStore_.getSortedPointIterator();
              it.hasNext(); ) {
            BitmapPoint3D point = (BitmapPoint3D) it.next();
            double z = point.getZ();
            int base = point.getPixelBase( this );

            /* Draw the marker and error bars if any. */
            Pixellator pixoffs = point.getPixelOffsets( this );
            if ( pixoffs != null ) {
                float[] rgba = point.getRgba( this );

                /* Treat each pixel painted by this point. */
                for ( pixoffs.start(); pixoffs.next(); ) {
                    int ipix = base + pixoffs.getX() + xdim_ * pixoffs.getY();
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

            /* Draw the label if any. */
            String label = point.getLabel();
            if ( label != null ) {
                int is = point.istyle_;
                MarkStyle style = styles_[ is ];
                Pixellator lpixoffs =
                    style.getLabelPixels( label, point.px_ - xoff_,
                                          point.py_ - yoff_, clip );
                if ( lpixoffs != null ) {
                    float[] rgba = labelRgbas_[ is ];
                    for ( lpixoffs.start(); lpixoffs.next(); ) {
                        int ipix = lpixoffs.getX() + xdim_ * lpixoffs.getY();
                        float alpha = aBuf[ ipix ];
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
         * Stores a point with associated errors.
         *
         * @param   px   X coordinate
         * @param   py   Y coordinate
         * @param   z    Z coordinate
         * @param   coords  full coordinate array; first three elements are
         *          data space x,y,z and may contain additional auxiliary coords
         * @param   istyle  index into styles array
         * @param   showPoint  whether to draw the marker as well
         * @param   label  text label to draw near the marker
         * @param   nerr  number of error points
         * @param   xoffs  <code>nerr</code>-element array of error point
         *                 X offsets
         * @param   yoffs  <code>nerr</code>-element array of error point
         *                 Y offsets
         */
        public abstract void addPoint( int px, int py, double z,
                                       double[] coords, int istyle,
                                       boolean showPoint, String label,
                                       int nerr, int[] xoffs, int[] yoffs );

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
        List pointList_ = new ArrayList();

        public void addPoint( int px, int py, double z, double[] coords,
                              int istyle, boolean showPoint, String label,
                              int nerr, int[] xoffs, int[] yoffs ) {
            int np = pointList_.size();
            Point3D p3;
            if ( nerr > 0 || label != null ) {
                p3 = new ExtrasBitmapPoint3D( np, z, istyle, px, py, showPoint,
                                              label, nerr, xoffs, yoffs );
            }
            else if ( showPoint ) {
                p3 = new BitmapPoint3D( np, z, istyle, px, py );
            }
            else {
                p3 = null;
                assert false : "pointless call";
            }
            if ( p3 != null ) {
                pointList_.add( p3 );
            }
        }

        public Iterator getSortedPointIterator() {
            BitmapPoint3D[] points =
                (BitmapPoint3D[]) pointList_.toArray( new BitmapPoint3D[ 0 ] );
            pointList_ = new ArrayList();
            Arrays.sort( points, Point3D.getComparator( true, false ) );
            return Arrays.asList( points ).iterator();
        }
    }

    /**
     * PointStore implementation which uses objects and takes account of
     * coordinates on auxiliary axes.
     */
    private static class AuxObjectPointStore extends ObjectPointStore {

        private final DataColorTweaker tweaker_;
        private final float[][] rgbas_;
        private final float[] buf_;

        /**
         * Constructor.
         *
         * @param   rgbas  per-styleset rgba colour arrays
         * @param   colour tweaker which takes account of aux coordinates
         */
        public AuxObjectPointStore( float[][] rgbas,
                                    DataColorTweaker tweaker ) {
            tweaker_ = tweaker;
            rgbas_ = rgbas;
            buf_ = new float[ 4 ];
        }

        public void addPoint( int px, int py, double z, double[] coords,
                              int istyle, boolean showPoint, String label,
                              int nerr, int[] xoffs, int[] yoffs ) {
            float[] rgbaBuf = getRgba( istyle, coords );
            if ( rgbaBuf != null ) {
                final int rgba = packRgba( rgbaBuf );
                int np = pointList_.size();
                Point3D p3;
                if ( nerr > 0 || label != null ) {
                    p3 = new ExtrasBitmapPoint3D( np, z, istyle, px, py,
                                                  showPoint, label,
                                                  nerr, xoffs, yoffs ) {
                        public float[] getRgba( BitmapSortPlotVolume vol ) {
                            unpackRgba( rgba, buf_ );
                            return buf_;
                        }
                    };
                }
                else if ( showPoint ) {
                    p3 = new BitmapPoint3D( np, z, istyle, px, py ) {
                        public float[] getRgba( BitmapSortPlotVolume vol ) {
                            unpackRgba( rgba, buf_ );
                            return buf_;
                        }
                    };
                }
                else {
                    assert false : "pointless call";
                    p3 = null;
                }
                if ( p3 != null ) {
                    pointList_.add( p3 );
                }
            }
        }

        /**
         * Returns the RGBA colour for a plotted point.
         *
         * @param   istyle  point style index
         * @param   coords  full coordinate array
         * @return  RGBA colour, or null if the coords are invalid
         */
        private float[] getRgba( int istyle, double[] coords ) {
            if ( tweaker_.setCoords( coords ) ) {
                float[] rgba = rgbas_[ istyle ];
                buf_[ 0 ] = rgba[ 0 ];
                buf_[ 1 ] = rgba[ 1 ];
                buf_[ 2 ] = rgba[ 2 ];
                buf_[ 3 ] = rgba[ 3 ];
                tweaker_.tweakColor( buf_ );
                return buf_;
            }
            else {
                return null;
            }
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

        public void addPoint( int px, int py, double z, double[] coords,
                              int istyle, boolean showPoint, String label,
                              int nerr, int[] xoffs, int[] yoffs ) {
            if ( nerr > 0 || label != null ) {
                throw new UnsupportedOperationException(
                    "No errors or labels" );
            }
            else if ( showPoint ) {
                if ( z > zmin_ && z <= zmax_ &&
                     px >= 0 && px <= TWO12 &&
                     py >= 0 && py <= TWO12 ) {
                    pointList_.add( pack( px, py, z, istyle ) );
                }
            }
            else {
                assert false : "pointless";
            }
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
         * since z is stored at the most significant end.  After that,
         * the style index is a tie-breaker, highest first.  After that,
         * the Y and X coordinates are used, though these aren't likely
         * to be very useful.
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
            long mis = 0xff - is;
            return (long) zint << 32 | mis << 24 | xp << 12 | yp << 0;
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
         * Unpacks style index value from packed long integer.
         *
         * @param   val  value produced by <code>pack</code>
         * @return  style index
         */
        private int unpackStyleIndex( long val ) {
            int mis = (int) ( ( val >> 24 ) & 0xff );
            assert mis >= 0;
            return 0xff - mis;
        }

        /**
         * Unpacks x coordinate value from packed long integer.
         *
         * @param   val  value produced by <code>pack</code>
         * @return   x coordinate
         */
        private int unpackX( long val ) {
            int xp = ( ((int) val) >> 12 ) & 0xfff;
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
            int yp = ( ((int) val) >> 0 ) & 0xfff;
            assert yp >= 0 && yp < TWO12;
            return yp;
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
         * @param  istyle  plotting style
         * @param  px    X coordinate
         * @param  py    Y coordinate
         */
        public BitmapPoint3D( int iseq, double z, int istyle, int px, int py ) {
            super( iseq, z );
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
         * Returns an iterator over pixel offsets 
         * relative to the value returned by {@link #getPixelBase} 
         * which should be coloured in by this point.
         * The returned array must not contain any repeated pixel positions.
         *
         * @param  vol  plot volume on which to operate
         * @return   pixel offset iterator relative to base
         */
        public Pixellator getPixelOffsets( BitmapSortPlotVolume vol ) {
            return vol.markPixoffs_[ istyle_ ];
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

        /**
         * Returns a text label associated with this point if there is one.
         *
         * @return   label, or null
         */
        public String getLabel() {
            return null;
        }
    }

    /**
     * Point3D implementation with errors.
     */
    private static class ExtrasBitmapPoint3D extends BitmapPoint3D {

        final boolean showPoint_;
        final String label_;
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
         * @param  label   associated text label
         * @param  nerr  number of error points
         * @param  xoffs  <code>nerr</code>-element array of error point
         *                X offsets
         * @param  yoffs  <code>nerr</code>-element array of error point
         *                Y offsets
         */
        public ExtrasBitmapPoint3D( int iseq, double z, int istyle,
                                    int px, int py, boolean showPoint,
                                    String label, int nerr, int[] xoffs,
                                    int[] yoffs ) {
            super( iseq, z, istyle, px, py );
            showPoint_ = showPoint;
            label_ = label;
            nerr_ = nerr;
            xoffs_ = nerr > 0 ? (int[]) xoffs.clone() : null;
            yoffs_ = nerr > 0 ? (int[]) yoffs.clone() : null;
        }

        public Pixellator getPixelOffsets( BitmapSortPlotVolume vol ) {
            Rectangle clip = vol.getGraphics().getClipBounds();
            clip.translate( -px_, -py_ );
            if ( nerr_ > 0 ) {
                Pixellator ePixer =
                    vol.getStyles()[ istyle_ ].getErrorRenderer()
                       .getPixels( clip, 0, 0, xoffs_, yoffs_ );
                return showPoint_
                     ? Drawing.combinePixellators( new Pixellator[] {
                           vol.markPixoffs_[ istyle_ ],
                           ePixer,
                       } )
                     : ePixer;
            }
            else {
                return showPoint_
                     ? vol.markPixoffs_[ istyle_ ]
                     : null;
            }
        }

        public String getLabel() {
            return label_;
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
