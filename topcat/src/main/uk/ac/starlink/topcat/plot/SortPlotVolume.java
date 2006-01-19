package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * PlotVolume implementation which accumulates all points to plot,
 * sorts them according to Z coordinate, and then plots them in order.
 *
 * @author   Mark Taylor
 * @since    22 Nov 2005
 */
public class SortPlotVolume extends PlotVolume {

    private final SortedSet points_;
    private int iseq_;

    /**
     * Constructs a new SortPlotVolume.
     *
     * @param   c  component
     * @param   g  graphics context
     * @param   styles array of marker styles which may be used to plot
     * @param   padFactor  minimum amount of space outside the unit cube 
     *          in both dimensions - 1 means no extra space
     * @param   padBorders  space, additional to padFactor, to be left around
     *          the edges of the plot; order is (left,right,bottom,top)
     */
    public SortPlotVolume( Component c, Graphics g, MarkStyle[] styles,
                           double padFactor, int[] padBorders ) {
        super( c, g, styles, padFactor, padBorders );
        points_ = new TreeSet();
    }

    public void plot( int px, int py, double z, int istyle ) {
        points_.add( new Point3D( px, py, z, getStyles()[ istyle ], ++iseq_ ) );
    }

    public void flush() {
        Graphics g = getGraphics();
        FogColor foggy = new FogColor();
        for ( Iterator it = points_.iterator(); it.hasNext(); ) {
            Point3D point = (Point3D) it.next();
            foggy.depth_ = point.z_;
            point.style_.drawMarker( g, point.px_, point.py_, foggy );
        }
        points_.clear();
    }

    /**
     * Implements ColorTweaker to perform fogging of distant points.
     */
    private class FogColor implements ColorTweaker {
        double depth_;
        final Fogger fog_ = getFogger();
        public Color tweakColor( Color color ) {
            return fog_.fogAt( depth_, color );
        }
    }

    /**
     * Object which encapsulates a point to be plotted.
     * Point3Ds are comparable; items later in the collation sequence
     * are nearer the front of the image than earler ones.
     */
    private static class Point3D implements Comparable {
        final short px_;
        final short py_;
        final float z_;
        final MarkStyle style_;
        final int iseq_;

        /**
         * Constructs a new Point3D.
         *
         * @param   px  graphics space X coordinate
         * @param   py  graphics space Y coordinate
         * @param   z   Z coordinate, used for sorting
         * @param   style  marker style to use for plotting the point
         * @param   iseq   sequence value, used as a tie-breaker for comparisons
         */
        Point3D( int px, int py, double z, MarkStyle style, int iseq ) {
            px_ = (short) px;
            py_ = (short) py;
            z_ = (float) z;
            style_ = style;
            iseq_ = iseq;
        }

        public int compareTo( Object other ) {
            Point3D o = (Point3D) other;
            if ( this.z_ > o.z_ ) {
                return -1;
            }
            else if ( this.z_ < o.z_ ) {
                return +1;
            }
            else {
                if ( this.iseq_ < o.iseq_ ) {
                    return -1;
                }
                else if ( this.iseq_ > o.iseq_ ) {
                    return +1;
                }
                else {
                    assert false : "Two points shouldn't have same sequence ID";
                    return 0;
                }
            }
        }
    }
}
