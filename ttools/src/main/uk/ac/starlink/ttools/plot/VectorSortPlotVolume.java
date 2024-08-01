package uk.ac.starlink.ttools.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * PlotVolume implementation which accumulates all points to plot and then
 * plots them by drawing, suitable for a vector-like context.
 * Transparency is not renderered.
 *
 * @author   Mark Taylor
 * @since    26 Mar 2007
 */
public class VectorSortPlotVolume extends PlotVolume {

    private final DataColorTweaker markTweaker_;
    private final DataColorTweaker labelTweaker_;
    private final Rectangle bounds_;
    private List<VectorPoint3D> pointList_;
    private int iseq_;
    private boolean hasLabels_;
    private final int[] rgbs_;
    private final float[][] frgbs_;
    private final int[] labelRgbs_;
    private final float[][] labelFrgbs_;
    private final MarkStyle[] styles_;
    private final FixColorTweaker fixer_;
    private final float[] frgba_;

    /**
     * Packed RGBA value representing no colour.  This is used as a flag,
     * but must also be a valid packed colour which has no effect when
     * plotting.  Anything with alpha == 0 would do.
     */
    private static final int NO_RGBA = 0;

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
     * @param   tweaker  colour adjuster for using auxiliary axis coords
     */
    @SuppressWarnings("this-escape")
    public VectorSortPlotVolume( Component c, Graphics g, MarkStyle[] styles,
                                 double padFactor, int[] padBorders,
                                 double fogginess, DataColorTweaker tweaker ) {
        super( c, g, styles, padFactor, padBorders, fogginess );
        bounds_ = c.getBounds();
        pointList_ = new ArrayList<VectorPoint3D>();
        markTweaker_ = createFoggingTweaker( tweaker );
        labelTweaker_ = createFoggingTweaker( null );
        int nstyle = styles.length;
        frgba_ = new float[ 4 ];
        styles_ = styles;
        rgbs_ = new int[ nstyle ];
        labelRgbs_ = new int[ nstyle ];
        frgbs_ = new float[ nstyle ][];
        labelFrgbs_ = new float[ nstyle ][];
        for ( int is = 0; is < nstyle; is++ ) {
            Color color = styles[ is ].getColor();
            rgbs_[ is ] = color.getRGB();
            frgbs_[ is ] = color.getRGBComponents( null );
            Color labelColor = styles[ is ].getLabelColor();
            labelRgbs_[ is ] = labelColor.getRGB();
            labelFrgbs_[ is ] = labelColor.getRGBComponents( null );
        }
        fixer_ = new FixColorTweaker();
    }

    public void plot2d( int px, int py, double z, double[] coords, int istyle,
                        boolean showPoint, String label,
                        int nerr, int[] xoffs, int[] yoffs, double[] zerrs ) {
        int rgb = getMarkRgb( istyle, coords );
        if ( rgb != NO_RGBA ) {
            VectorPoint3D p3;
            boolean hasErr = nerr > 0;
            boolean hasLabel = label != null;
            hasLabels_ = hasLabels_ || hasLabel;
            if ( hasErr || hasLabel ) {
                int labelRgb = getLabelRgb( istyle, coords );
                p3 = new ExtrasVectorPoint3D( iseq_++, z, px, py, istyle, rgb,
                                              showPoint, label, labelRgb,
                                              nerr, xoffs, yoffs );
            }
            else if ( showPoint ) {
                p3 = new VectorPoint3D( iseq_++, z, px, py, istyle, rgb );
            }
            else {
                assert false : "pointless call";
                p3 = null;
            }
            addPoint( p3 );
        }
    }

    public void flush() {

        /* Iterate from back to front over points, so that nearer ones get
         * drawn over more distant ones. */
        VectorPoint3D[] points = getSortedPoints();
        int np = points.length;
        Graphics g = getGraphics();

        /* If we may be encountering labels, keep track of which points have
         * had an associated label drawn.  Do not attempt to draw a label 
         * based at the same point that another label has already been drawn.
         * This (a) cuts down on visual clutter (unreadable overplotted labels)
         * and (b) reduces the number of expensive label drawing actions when
         * there are many points.  The number of overplots could be restricted
         * even (much) further by avoiding labels near points which already
         * have them as well as right on top. */
        if ( hasLabels_ ) {
            PixelMask mask = new PixelMask( bounds_ );
            for ( int ip = 0; ip < np; ip++ ) {
                VectorPoint3D point = points[ ip ];
                if ( point.hasLabel() ) {
                    int px = point.px_;
                    int py = point.py_;
                    if ( ! mask.get( px, py ) ) {
                        point.render( g, this, true );
                    }
                    else {
                        point.render( g, this, false );
                        mask.set( px, py );
                    }
                }
            }
        }

        /* No labels: just go through and plot all the points. */
        else {
            for ( int ip = 0; ip < np; ip++ ) {
                points[ ip ].render( g, this, false );
            }
        }
    }

    /**
     * Adds a point to the list to be plotted.
     *
     * @param  point  point
     */
    private void addPoint( VectorPoint3D point ) {
        pointList_.add( point );
    }

    /**
     * Obtains the list of all points to be plotted, sorted by Z coordinate.
     * The list is cleared as a side effect.
     *
     * @return   sorted list of points
     */
    private VectorPoint3D[] getSortedPoints() {
        VectorPoint3D[] points = pointList_.toArray( new VectorPoint3D[ 0 ] );
        pointList_ = new ArrayList<VectorPoint3D>();
        Arrays.sort( points, Point3D.getComparator( false, true ) );
        return points;
    }

    /**
     * Returns the RGB integer corresponding to a given marker style with
     * a given set of coordinates.  This takes into account the effects of
     * both Z-fogging and of any defined auxiliary axes.
     *
     * @param  istyle  style index
     * @param  coords  full coordinate array
     */
    private int getMarkRgb( int istyle, double[] coords ) {
        return getRgb( coords, markTweaker_, rgbs_[ istyle ],
                       frgbs_[ istyle ] );
    }

    /**
     * Returns the RGB integer for label drawing corresponding to a given
     * marker style at a tiven set of coordinates.  This takes into account
     * any Z-fogging.
     *
     * @param   istyle  style index
     * @param   coords  full coordinate array
     */
    private int getLabelRgb( int istyle, double[] coords ) {
        return getRgb( coords, labelTweaker_, labelRgbs_[ istyle ],
                       labelFrgbs_[ istyle ] );
    }

    /**
     * Gets an RGB integer taking into account colour modifications at a 
     * given set of coordinates.
     *
     * @param   coords  full coordinate array
     * @param   tweaker  colour tweaker sensitive to coords
     * @param   rgb   integer representation of base colour
     * @param   frgb  float[] array representation of base colour
     */
    private int getRgb( double[] coords, DataColorTweaker tweaker,
                        int rgb, float[] frgb ) {
        if ( tweaker == null ) {
            return rgb;
        }
        else {
            frgba_[ 0 ] = frgb[ 0 ];
            frgba_[ 1 ] = frgb[ 1 ];
            frgba_[ 2 ] = frgb[ 2 ];
            frgba_[ 3 ] = frgb[ 3 ];
            if ( tweaker.setCoords( coords ) ) {
                tweaker.tweakColor( frgba_ );
                float r = frgba_[ 0 ];
                float b = frgba_[ 2 ];
                frgba_[ 2 ] = r;
                frgba_[ 0 ] = b;
                return packRgba( frgba_ );
            }
            else {
                return NO_RGBA;
            }
        }
    }

    /**
     * Utility class providing a trivial implementation of ColorTweaker
     * which always returns a given colour.
     */
    private static class FixColorTweaker implements ColorTweaker {
        private static final float BYTE_SCALE = 1f / 255.99f;
        private int rgb_;
        private Color color_;
        private boolean fok_;
        private float fr_;
        private float fg_;
        private float fb_;

        /**
         * Sets the colour which this object tweaks to.
         *
         * @param  rgb  integer representation of target colour
         */
        void setRgb( int rgb ) {
            rgb_ = rgb;
            color_ = null;
            fok_ = false;
        }

        public Color tweakColor( Color color ) {
            if ( color_ == null ) {
                color_ = new Color( rgb_ );
            }
            return color_;
        }

        public void tweakColor( float[] rgba ) {
            if ( ! fok_ ) {
                fr_ = (float) ( ( rgb_ & 0xff0000 ) >> 16 ) * BYTE_SCALE;
                fg_ = (float) ( ( rgb_ & 0x00ff00 ) >> 8 ) * BYTE_SCALE;
                fb_ = (float) ( ( rgb_ & 0x0000ff ) >> 0 ) * BYTE_SCALE;
                fok_ = true;
            }
            rgba[ 0 ] = fr_;
            rgba[ 1 ] = fg_;
            rgba[ 2 ] = fb_;
        }
    }

    /**
     * Point3D implementation which can render itself to a vector context.
     */
    private static class VectorPoint3D extends Point3D {
 
        final int px_;
        final int py_;
        final int isrgb_;

        /**
         * Constructor.
         *
         * @param  iseq  unique sequence number
         * @param  z     Z coordinate
         * @param  px    X coordinate
         * @param  py    Y coordinate
         * @param  istyle  style index
         * @param  rgb     sRGB colour value
         */
        public VectorPoint3D( int iseq, double z, int px, int py,
                              int istyle, int rgb ) {
            super( iseq, z );
            px_ = px;
            py_ = py;
            isrgb_ = ( ( istyle & 0xff ) << 24 ) | rgb & 0xffffff;
        }

        /**
         * Returns an object which will set the colour correctly for drawing
         * this point.
         *
         * @param  plotVol  owner
         */
        FixColorTweaker getColorTweaker( VectorSortPlotVolume plotVol ) {
            FixColorTweaker fixer = plotVol.fixer_;
            fixer.setRgb( isrgb_ & 0xffffff );
            return fixer;
        }

        /**
         * Returns the mark stle for drawing this point.
         *
         * @param   plotVol  owner
         */
        MarkStyle getStyle( VectorSortPlotVolume plotVol ) {
            return plotVol.styles_[ ( isrgb_ & 0xff000000 ) >> 24 ];
        }

        /**
         * Returns true if there is a text label associated with this point.
         *
         * @return   true  iff there is a non-blank label
         */
        public boolean hasLabel() {
            return false;
        }

        /**
         * Draws this point on a vector-like graphics context, ignoring
         * transparency.
         *
         * @param   g  graphics context
         * @param   plotVol  owner
         * @param   withLabel  whether an associated label should be 
         *          drawn if this point has one
         */
        public void render( Graphics g, VectorSortPlotVolume plotVol,
                            boolean withLabel ) {
            MarkStyle style = getStyle( plotVol );
            ColorTweaker tweaker = getColorTweaker( plotVol );
            style.drawMarker( g, px_, py_, tweaker );
        }
    }

    /**
     * VectorPoint3D implementation which can also draw error bars and 
     * marker labels.
     */
    private static class ExtrasVectorPoint3D extends VectorPoint3D {

        final boolean showPoint_;
        final String label_;
        final int labelRgb_;
        final int nerr_;
        final int[] xoffs_;
        final int[] yoffs_;

        /**
         * Constructor.
         *
         * @param  iseq  unique sequence number
         * @param  z     Z coordinate
         * @param  px    X coordinate
         * @param  py    Y coordinate
         * @param  istyle  style index
         * @param  rgb     sRGB colour value
         * @param  showPoint  whether to draw the marker as well
         * @param  label   text label
         * @param  nerr  number of error points
         * @param  xoffs  <code>nerr</code>-element array of error point 
         *                X offsets
         * @param  yoffs  <code>nerr</code>-element array of error point 
         *                Y offsets
         */
        public ExtrasVectorPoint3D( int iseq, double z, int px, int py,
                                    int istyle, int rgb, boolean showPoint,
                                    String label, int labelRgb, int nerr, 
                                    int[] xoffs, int[] yoffs ) {
            super( iseq, z, px, py, istyle, rgb );
            showPoint_ = showPoint;
            label_ = label;
            labelRgb_ = labelRgb;
            nerr_ = nerr;
            xoffs_ = nerr > 0 ? xoffs.clone() : null;
            yoffs_ = nerr > 0 ? yoffs.clone() : null;
        }

        public boolean hasLabel() {
            return label_ != null;
        }

        public void render( Graphics g, VectorSortPlotVolume plotVol,
                            boolean withLabel ) {
            MarkStyle style = getStyle( plotVol );
            FixColorTweaker tweaker = getColorTweaker( plotVol );
            if ( nerr_ > 0 ) {
                style.drawErrors( g, px_, py_, xoffs_, yoffs_, tweaker );
            }
            if ( showPoint_ ) {
                style.drawMarker( g, px_, py_, tweaker );
            }
            if ( withLabel && label_ != null ) {
                tweaker.setRgb( labelRgb_ );
                style.drawLabel( g, px_, py_, label_, tweaker );
            }
        }
    }
}
