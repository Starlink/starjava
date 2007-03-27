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

    private List pointList_;
    private int iseq_;

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
     */
    public VectorSortPlotVolume( Component c, Graphics g, MarkStyle[] styles,
                                 double padFactor, int[] padBorders ) {
        super( c, g, styles, padFactor, padBorders );
        pointList_ = new ArrayList();
    }

    public void plot2d( int px, int py, double z, int istyle ) {
        addPoint( new VectorPoint3D( iseq_++, z, getStyles()[ istyle ],
                                     px, py ) );
    }

    public void plot2d( int px, int py, double z, int istyle,
                        boolean showPoint, int nerr, int[] xoffs, int[] yoffs,
                        double[] zerrs ) {
        addPoint( new ErrorsVectorPoint3D( iseq_++, z, getStyles()[ istyle ], 
                                           px, py, showPoint, nerr,
                                           xoffs, yoffs ) );
    }

    public void flush() {
        Graphics g = getGraphics();
        FogColor foggy = new FogColor( getFogger() );

        /* Iterate from back to front over points, so that nearer ones get
         * drawn over more distant ones. */
        VectorPoint3D[] points = getSortedPoints();
        int np = points.length;
        for ( int ip = 0; ip < np; ip++ ) {
            VectorPoint3D point = points[ ip ];
            foggy.depth_ = point.getZ();
            point.render( g, foggy );
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
        Arrays.sort( points );
        return points;
    }

    /**
     * Implements ColorTweaker to perform fogging of distant points.
     */
    private static class FogColor implements ColorTweaker {
        final Fogger fog_;
        double depth_;
        FogColor( Fogger fog ) {
            fog_ = fog;
        }
        public Color tweakColor( Color color ) {
            return fog_.fogAt( depth_, color );
        }
    }

    /**
     * Point3D implementation which can render itself to a vector context.
     */
    private static class VectorPoint3D extends Point3D {
 
        final MarkStyle style_;
        final int px_;
        final int py_;

        /**
         * Constructor.
         *
         * @param  iseq  unique sequence number
         * @param  z     Z coordinate
         * @param  style  plotting style
         * @param  px    X coordinate
         * @param  py    Y coordinate
         */
        public VectorPoint3D( int iseq, double z, MarkStyle style,
                              int px, int py ) {
            super( iseq, z, false );
            style_ = style;
            px_ = px;
            py_ = py;
        }

        /**
         * Draws this point on a vector-like graphics context, ignoring
         * transparency.
         *
         * @param   g  graphics context
         * @param   fixer  color tweaker
         */
        public void render( Graphics g, ColorTweaker fixer ) {
            style_.drawMarker( g, px_, py_, fixer );
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
        public ErrorsVectorPoint3D( int iseq, double z, MarkStyle style,
                                    int px, int py, boolean showPoint,
                                    int nerr, int[] xoffs, int[] yoffs ) {
            super( iseq, z, style, px, py );
            showPoint_ = showPoint;
            nerr_ = nerr;
            xoffs_ = (int[]) xoffs.clone();
            yoffs_ = (int[]) yoffs.clone();
        }

        public void render( Graphics g, ColorTweaker fixer ) {
            style_.drawErrors( g, px_, py_, xoffs_, yoffs_, fixer );
            if ( showPoint_ ) {
                super.render( g, fixer );
            }
        }
    }
}
