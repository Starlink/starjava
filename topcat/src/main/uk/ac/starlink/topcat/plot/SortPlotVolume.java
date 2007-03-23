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

    public void plot2d( int px, int py, double z, int istyle ) {
        points_.add( new Point3D( ++iseq_, z, getStyles()[ istyle ], px, py ) );
    }

    public void plot2d( int px, int py, double z, int istyle,
                        boolean showPoint, int nerr, int[] xoffs, int[] yoffs,
                        double[] zerrs ) {
        points_.add( new ErrorsPoint3D( ++iseq_, z, getStyles()[ istyle ],
                                        px, py, showPoint,
                                        nerr, xoffs, yoffs ) );
    }

    public void flush() {
        Graphics g = getGraphics();
        FogColor foggy = new FogColor();
        for ( Iterator it = points_.iterator(); it.hasNext(); ) {
            Point3D point = (Point3D) it.next();
            foggy.depth_ = point.z_;
            point.render( g, foggy );
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
         * @param   iseq   sequence value, used as a tie-breaker for comparisons
         * @param   z   Z coordinate, used for sorting
         * @param   style  marker style to use for plotting the point
         * @param   px  graphics space X coordinate
         * @param   py  graphics space Y coordinate
         */
        Point3D( int iseq, double z, MarkStyle style, int px, int py ) {
            px_ = (short) px;
            py_ = (short) py;
            z_ = (float) z;
            style_ = style;
            iseq_ = iseq;
        }

        /**
         * Draws this point onto the graphics context.
         *
         * @param  g  graphics context
         * @param  fixer    hook for modifying the colour
         */
        public void render( Graphics g, ColorTweaker fixer ) {
            style_.drawMarker( g, px_, py_, fixer );
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

    /**
     * Point3D subclass which contains information about error bars to draw
     * as well as (or instead of) the central point.
     */
    private static class ErrorsPoint3D extends Point3D {

        final boolean showPoint_;
        final int nerr_;
        final int[] xoffs_;
        final int[] yoffs_;

        /**
         * Constructor.
         *
         * @param   iseq   sequence value, used as a tie-breaker for comparisons
         * @param   z   Z coordinate, used for sorting
         * @param   style  marker style to use for plotting the point
         * @param   px  graphics space X coordinate
         * @param   py  graphics space Y coordinate
         * @param   showPoint  whether to draw the marker as well as error bars
         * @param   nerr  number of error points
         * @param   xoffs  nerr-element array of error point X coords
         * @param   yoffs  nerr-element array of error point Y coords
         */
        ErrorsPoint3D( int iseq, double z, MarkStyle style, int px, int py,
                       boolean showPoint, int nerr, int[] xoffs, int[] yoffs ) {
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
