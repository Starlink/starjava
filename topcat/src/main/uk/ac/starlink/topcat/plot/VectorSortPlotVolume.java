package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
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

    private final DataColorTweaker tweaker_;
    private List pointList_;
    private int iseq_;
    private final int[] rgbs_;
    private final Color[] colors_;
    private final MarkStyle[] styles_;
    private final FixColorTweaker fixer_;

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
    public VectorSortPlotVolume( Component c, Graphics g, MarkStyle[] styles,
                                 double padFactor, int[] padBorders,
                                 double fogginess, DataColorTweaker tweaker ) {
        super( c, g, styles, padFactor, padBorders, fogginess );
        pointList_ = new ArrayList();
        tweaker_ = createFoggingTweaker( tweaker );
        int nstyle = styles.length;
        styles_ = styles;
        rgbs_ = new int[ nstyle ];
        colors_ = new Color[ nstyle ];
        for ( int is = 0; is < nstyle; is++ ) {
            colors_[ is ] = styles[ is ].getColor();
            rgbs_[ is ] = colors_[ is ].getRGB();
        }
        fixer_ = new FixColorTweaker();
    }

    public void plot2d( int px, int py, double z, double[] coords,
                        int istyle ) {
        addPoint( new VectorPoint3D( iseq_++, z, px, py, istyle,
                                     getRgb( istyle, coords ) ) );
    }

    public void plot2d( int px, int py, double z, double[] coords, int istyle,
                        boolean showPoint, int nerr, int[] xoffs, int[] yoffs,
                        double[] zerrs ) {
        addPoint( new ErrorsVectorPoint3D( iseq_++, z, px, py, istyle,
                                           getRgb( istyle, coords ), showPoint,
                                           nerr, xoffs, yoffs ) );
    }

    public void flush() {

        /* Iterate from back to front over points, so that nearer ones get
         * drawn over more distant ones. */
        VectorPoint3D[] points = getSortedPoints();
        int np = points.length;
        Graphics g = getGraphics();
        for ( int ip = 0; ip < np; ip++ ) {
            points[ ip ].render( g, this );
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
        VectorPoint3D[] points =
            (VectorPoint3D[]) pointList_.toArray( new VectorPoint3D[ 0 ] );
        pointList_ = new ArrayList();
        Arrays.sort( points, Point3D.DOWN );
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
    private int getRgb( int istyle, double[] coords ) {
        if ( tweaker_ == null ) {
            return rgbs_[ istyle ];
        }
        else {
            tweaker_.setCoords( coords );
            return tweaker_.tweakColor( colors_[ istyle ] ).getRGB();
        }
    }

    /**
     * Utility class providing a trivial implementation of ColorTweaker
     * which always returns a given colour.
     */
    private static class FixColorTweaker implements ColorTweaker {
        private Color color_;

        /**
         * Sets the colour which this object tweaks to.
         *
         * @param  rgb  integer representation of target colour
         */
        void setRgb( int rgb ) {
            color_ = new Color( rgb );
        }

        public Color tweakColor( Color color ) {
            return color_;
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
        ColorTweaker getColorTweaker( VectorSortPlotVolume plotVol ) {
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
         * Draws this point on a vector-like graphics context, ignoring
         * transparency.
         *
         * @param   g  graphics context
         * @param   plotVol  owner
         */
        public void render( Graphics g, VectorSortPlotVolume plotVol ) {
            MarkStyle style = getStyle( plotVol );
            ColorTweaker tweaker = getColorTweaker( plotVol );
            style.drawMarker( g, px_, py_, tweaker );
        }
    }

    /**
     * VectorPoint3D implementation which also draws error bars.
     */
    private static class ErrorsVectorPoint3D extends VectorPoint3D {

        final boolean showPoint_;
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
         * @param  nerr  number of error points
         * @param  xoffs  <code>nerr</code>-element array of error point 
         *                X offsets
         * @param  yoffs  <code>nerr</code>-element array of error point 
         *                Y offsets
         */
        public ErrorsVectorPoint3D( int iseq, double z, int px, int py,
                                    int istyle, int rgb, boolean showPoint,
                                    int nerr, int[] xoffs, int[] yoffs ) {
            super( iseq, z, px, py, istyle, rgb );
            showPoint_ = showPoint;
            nerr_ = nerr;
            xoffs_ = (int[]) xoffs.clone();
            yoffs_ = (int[]) yoffs.clone();
        }

        public void render( Graphics g, VectorSortPlotVolume plotVol ) {
            MarkStyle style = getStyle( plotVol );
            ColorTweaker tweaker = getColorTweaker( plotVol );
            style.drawErrors( g, px_, py_, xoffs_, yoffs_, tweaker );
            if ( showPoint_ ) {
                style.drawMarker( g, px_, py_, tweaker );
            }
        }
    }
}
