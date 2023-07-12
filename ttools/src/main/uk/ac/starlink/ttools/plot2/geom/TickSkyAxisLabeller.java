package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import uk.ac.starlink.ttools.plot2.Anchor;
import uk.ac.starlink.ttools.plot2.Caption;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.Surround;

/**
 * Partial SkyAxisLabeller implementation that labels axes with positioned
 * numeric labels.
 *
 * @author   Mark Taylor
 * @since    21 Feb 2013
 */
public abstract class TickSkyAxisLabeller implements SkyAxisLabeller {

    private final String name_;
    private final String description_;
    private final boolean labelSys_;
    public static Anchor X_ANCHOR = Anchor.N;
    public static Anchor Y_ANCHOR = Anchor.E;

    /**
     * Constructor.
     *
     * @param  name  labeller name
     * @param  description  labeller description
     * @param  labelSys  labelSys   determines whether the sky system
     *                              coordinate names are written on the axes
     */
    public TickSkyAxisLabeller( String name, String description,
                                boolean labelSys ) {
        name_ = name;
        description_ = description;
        labelSys_ = labelSys;
    }

    public String getLabellerName() {
        return name_;
    }

    public String getLabellerDescription() {
        return description_;
    }

    public AxisAnnotation createAxisAnnotation( GridLiner gridLiner,
                                                Captioner captioner,
                                                SkySys skySys ) {
        Caption[] labels = Arrays.stream( gridLiner.getLabels() )
                                 .map( SkyAxisLabellers::labelCaption )
                                 .collect( Collectors.toList() )
                                 .toArray( new Caption[ 0 ] );
        SkyTick[] ticks = calculateTicks( gridLiner.getLines(), labels,
                                          gridLiner.getBounds() );
        ticks = removeOverlaps( ticks, captioner );
        return new TickAxisAnnotation( ticks, gridLiner.getBounds(), captioner,
                                       labelSys_ ? skySys : null );
    }

    /**
     * Returns a list of ticks for labelling lines produced by a
     * GridLiner.
     *
     * @param   lines  gridliner line point array
     * @param   labels  gridliner line label array
     * @param   plotBounds  extent of plot region in graphics coordinates
     * @see  GridLiner
     */
    protected abstract SkyTick[] calculateTicks( double[][][] lines,
                                                 Caption[] labels,
                                                 Rectangle plotBounds );

    /**
     * Takes a list of ticks and removes some elements if they are
     * so crowded together the labels overlap.
     *
     * @param   ticks  input tick list
     * @param  captioner  text renderer
     * @return   output tick list, with some items removed if necessary
     */
    protected SkyTick[] removeOverlaps( SkyTick[] ticks, Captioner captioner ) {
        Map<SkyTick,Rectangle> tickMap = new LinkedHashMap<SkyTick,Rectangle>();
 
        /* Look at each tick. */
        for ( int it = 0; it < ticks.length; it++ ) {
            SkyTick tick1 = ticks[ it ];
            Rectangle box1 = tick1.getCaptionBounds( captioner );

            /* Compare its bounding box for overlap with
             * each previously encountered tick. */
            for ( Iterator<SkyTick> tit = tickMap.keySet().iterator();
                  tit.hasNext() && tick1 != null; ) {
                SkyTick tick0 = tit.next();
                Rectangle box0 = tick0.getCaptionBounds( captioner );
                if ( box0.intersects( box1 ) ) {
                    tick1 = null;
                }
            }

            /* Add the new tick to the list only in case of no overlaps. */
            if ( tick1 != null ) {
                tickMap.put( tick1, box1 );
            }
        }

        /* Return retained ticks. */
        return tickMap.keySet().toArray( new SkyTick[ 0 ] );
    }

    /**
     * Constructs a single tick that sits outside the plot bounding box
     * for a given grid line.
     *
     * @param  label  tick text
     * @param  line   grid line coordinates - array of (x,y) arrays
     * @param  bounds   plot region bounds
     */
    public static SkyTick createExternalTick( Caption label, double[][] line,
                                              Rectangle bounds ) {

        /* Iterate over line segments in this grid line. */
        for ( int is = 0; is < line.length; is++ ) {

            /* Work out whether the line segment should have a label on the
             * X or Y axis.  There may be ways of doing this that are both
             * better and quicker. */
            double[] seg = line[ is ];
            double px = seg[ 0 ];
            double py = seg[ 1 ];
            int iseg1 = is == 0 ? 1 : is;
            int iseg0 = iseg1 - 1;
            assert iseg0 >= 0;
            final double theta;
            if ( iseg1 < line.length ) {
                double dx = line[ iseg1 ][ 0 ] - line[ iseg0 ][ 0 ];
                double dy = line[ iseg1 ][ 1 ] - line[ iseg0 ][ 1 ];
                theta = Math.atan2( dy, dx );
            }
            else {
                theta = Double.NaN;
            }
            double upness = 1 - Math.abs( 1 - Math.abs( 2 * theta / Math.PI ) );
            assert upness >= 0 && upness <= 1;

            /* If this segment hits the X axis and needs an X axis label,
             * return a tick for this position. */
            if ( Math.abs( py - bounds.y - bounds.height ) < 0.5 ) {
                if ( upness >= 0.5 ) {
                    int x = (int) Math.round( px );
                    if ( x >= bounds.x & x < bounds.x + bounds.width ) {
                        return new SkyTick( label, x, bounds.y + bounds.height,
                                            X_ANCHOR );
                    }
                }
            }

            /* If this segment hits the Y axis and needs a Y axis label,
             * return a tick for this position. */
            if ( Math.abs( px - bounds.x ) < 0.5 ) {
                if ( upness <= 0.5 ) {
                    int y = (int) Math.round( py );
                    if ( y >= bounds.y && y < bounds.y + bounds.height ) {
                        return new SkyTick( label, bounds.x, y, Y_ANCHOR );
                    }
                }
            }
        }

        /* Line did not hit an axis where a label was appropriate.
         * Return null. */
        return null;
    }

    /**
     * Constructs a single tick that sits inside the plot bounding box
     * for a given grid line.
     *
     * @param  label  tick text
     * @param  line   grid line coordinates - array of (x,y) arrays
     */
    public static SkyTick createInternalTick( Caption label, double[][] line ) {
        int nseg = line.length;

        /* Work out running cumulative line lengths for each segment of the
         * grid line. */
        double[] dists = new double[ nseg ];
        for ( int is1 = 1; is1 < nseg; is1++ ) {
            int is0 = is1 - 1;
            double[] seg0 = line[ is0 ];
            double[] seg1 = line[ is1 ];
            double segleng = Math.hypot( seg1[ 0 ] - seg0[ 0 ],
                                         seg1[ 1 ] - seg0[ 1 ] );
            dists[ is1 ] = dists[ is0 ] + segleng;
        }

        /* Identify segment in which the halfway mark occurs and the
         * fractional distance along it. */
        double halfway = 0.5 * dists[ nseg - 1 ];
        int iseg0 = -1;
        double segfrac = Double.NaN;
        for ( int is1 = 1; is1 < nseg && iseg0 < 0; is1++ ) {
            int is0 = is1 - 1;
            double d0 = dists[ is0 ];
            double d1 = dists[ is1 ];
            if ( d1 >= halfway ) {
                iseg0 = is0;
                segfrac = d1 == d0 ? 0 : ( halfway - d0 ) / ( d1 - d0 );
            }
        }
        assert iseg0 >= 0 && iseg0 < nseg - 1 && segfrac >= 0 && segfrac <= 1;
        int iseg1 = iseg0 + 1;

        /* Identify the graphics coordinates for this halfway mark. */
        double x0 = line[ iseg0 ][ 0 ];
        double y0 = line[ iseg0 ][ 1 ];
        double x1 = line[ iseg1 ][ 0 ];
        double y1 = line[ iseg1 ][ 1 ];
        double dx = x1 - x0;
        double dy = y1 - y0;
        int px = (int) Math.round( x0 + segfrac * dx );
        int py = (int) Math.round( y0 + segfrac * dy );

        /* Get the line angle of the halfway line segment. */
        double theta = Math.atan2( dy, dx );

        /* Construct an anchor which will rotate the text appropriately
         * to run along the line. */
        if ( theta > Math.PI / 2 ) {
            theta -= Math.PI;
        }
        else if ( theta < - Math.PI / 2 ) {
            theta += Math.PI;
        }
        Anchor onAnchor = new Anchor.HorizontalAnchor() {
            protected int[] getOffset( Rectangle bounds, int pad ) {
                return new int[] { -bounds.width / 2, -2 };
            }
        };
        Anchor anchor = Anchor.createAngledAnchor( theta, onAnchor );

        /* Return a tick with the label positioned and angled correctly
         * to sit in the middle of the grid line. */
        return new SkyTick( label, px, py, anchor );
    }

    /**
     * Creates a caption from a text string that may contain spaces.
     * No attempt is made to handle other potentially problematic characters.
     *
     * @param   txt  caption text
     * @return   caption object
     */
    private static Caption textToCaption( String txt ) {
        return Caption.createCaption( txt, txt.replace( " ", "\\ " ) );
    }

    /**
     * AxisAnnotation implementation that gets its labels from a supplied
     * list of SkyTick objects.
     */
    private static class TickAxisAnnotation implements AxisAnnotation {

        private final SkyTick[] ticks_;
        private final Rectangle plotBounds_;
        private final Captioner captioner_;
        private final SkySys skySys_;
        private final Caption lonCaption_;
        private final Caption latCaption_;
        private final int hLon_;
        private final int hLat_;

        /**
         * Constructor.
         *
         * @param  ticks  tick list
         * @param  plotBounds  plot graphics region
         * @param  captioner  text renderer
         * @param  skySys  sky system with which to label axes,
         *                  or null if not labelled
         */
        TickAxisAnnotation( SkyTick[] ticks, Rectangle plotBounds,
                            Captioner captioner, SkySys skySys ) {
            ticks_ = ticks;
            plotBounds_ = plotBounds;
            captioner_ = captioner;
            skySys_ = skySys;
            if ( skySys != null ) {
                lonCaption_ = textToCaption( skySys_.getLongitudeName() );
                latCaption_ = textToCaption( skySys_.getLatitudeName() );
                hLon_ = captioner_.getCaptionBounds( lonCaption_ ).height;
                hLat_ = captioner_.getCaptionBounds( latCaption_ ).height;
            }
            else {
                lonCaption_ = null;
                latCaption_ = null;
                hLon_ = 0;
                hLat_ = 0;
            }
        }

        public Surround getSurround( boolean withScroll ) {
            Surround surround = getTickSurround( withScroll );
            if ( skySys_ != null ) {
                int lonExtra = hLon_ + hLon_ / 2;
                surround.bottom.extent += lonExtra;
                int latExtra = hLat_ + hLat_ / 2;
                surround.left.extent += latExtra;
            }
            return surround;
        }

        /**
         * Returns the required space around the edge of the plot bounds
         * due only to the ticks themselves managed by this annotation.
         * Additional padding may be added outside this.
         *
         * @param  withScroll  true if the padding should be large enough to
         *                     accommodate labelling requirements if the
         *                     surface is scrolled
         * @return  surround space
         */
        private Surround getTickSurround( boolean withScroll ) {

            /* If the tickmarks are aligned with the axes, treat this
             * like a 2d plot.  This will calculate the surround nicely
             * taking proper account of tick labels that overflow the X/Y
             * boundaries of the axes etc. */
            boolean isAllExternal = Arrays.stream( ticks_ )
                                   .allMatch( t -> t.anchor_ == X_ANCHOR ||
                                                   t.anchor_ == Y_ANCHOR );
            if ( isAllExternal ) {
                Surround surround = new Surround();
                for ( SkyTick tick : ticks_ ) {
                    Rectangle tickBox = getTickBounds( tick, withScroll );
                    boolean isExternal =
                        surround.addExternalRectangle( plotBounds_, tickBox );
                    assert isExternal;
                }
                return surround;
            }

            /* Otherwise, the job is more complicated to do properly. */
            else {

                /* In case of scrolling, don't even attempt it,
                 * because the usual method of moving the labels to
                 * both ends of the axis won't work to find out their
                 * maximum footprint, and it's too hard to do it properly.
                 * The effect is that internal tick labels may overflow
                 * the plot bounds sometimes. */
                if ( withScroll ) {
                    return new Surround();
                }

                /* If not scrolling, get the actual position of the tick
                 * labels and create a simple Surround object that just
                 * leaves any required space clear round the edges. */
                else {
                    Rectangle bounds = new Rectangle( plotBounds_ );
                    for ( SkyTick tick : ticks_ ) {
                        bounds.add( tick.getCaptionBounds( captioner_ ) );
                    }
                    int top = plotBounds_.y - bounds.y;
                    int left = plotBounds_.x - bounds.x;
                    int bottom = bounds.y + bounds.height
                               - plotBounds_.y - plotBounds_.height;
                    int right = bounds.x + bounds.width
                              - plotBounds_.x - plotBounds_.width;
                    assert top >= 0 && left >= 0 && bottom >= 0 && right >= 0;
                    return Surround
                          .fromInsets( new Insets( top, left, bottom, right ));
                }
            }
        }

        public void drawLabels( Graphics g ) {
            for ( int i = 0; i < ticks_.length; i++ ) {
                SkyTick tick = ticks_[ i ];
                tick.anchor_.drawCaption( tick.label_, tick.px_, tick.py_,
                                          captioner_, g );
            }
            if ( skySys_ != null ) {
                Insets insets = getTickSurround( false ).toInsets();
                Point pLon =
                    new Point( plotBounds_.x + plotBounds_.width / 2,
                               plotBounds_.y + plotBounds_.height
                             + insets.bottom );
                Point pLat =
                    new Point( plotBounds_.x - insets.left - 2 * hLat_,
                               plotBounds_.y + plotBounds_.height / 2 );
                Anchor.N.drawCaption( lonCaption_, pLon.x, pLon.y,
                                      captioner_, g );
                Anchor.createAngledAnchor( -0.5 * Math.PI, Anchor.N )
                      .drawCaption( latCaption_, pLat.x, pLat.y,
                                    captioner_, g );
            }
        }

        /**
         * Returns the effective bounding rectangle for a tick's annotation.
         *
         * @param  tick  tick
         * @param  withScroll  true if the padding should be large enough to
         *                     accommodate labelling requirements if the
         *                     surface is scrolled
         * @return  space on axis to reserve for this tick
         */
        private Rectangle getTickBounds( SkyTick tick, boolean withScroll ) {
            Rectangle box = tick.getCaptionBounds( captioner_ );
            if ( withScroll ) {
                Anchor anchor = tick.anchor_;
                final Point[] points;
                if ( anchor == X_ANCHOR ) {
                    int py = plotBounds_.y + plotBounds_.height;
                    points = new Point[] {
                        new Point( plotBounds_.x, py ),
                        new Point( plotBounds_.x + plotBounds_.width, py ),
                    };
                }
                else if ( anchor == Y_ANCHOR ) {
                    int px = plotBounds_.x;
                    points = new Point[] {
                        new Point( px, plotBounds_.y ),
                        new Point( px, plotBounds_.y + plotBounds_.height ),
                    };
                }
                else {
                    points = new Point[ 0 ];
                }
                for ( int ip = 0; ip < points.length; ip++ ) {
                    Point p = points[ ip ];
                    box.add( tick.getCaptionBoundsAt( p.x, p.y, captioner_ ) );
                }
            }
            return box;
        }
    }

    /**
     * Aggregates a line label, graphics position and text anchor.
     */
    public static class SkyTick {
        private final Caption label_;
        private final int px_;
        private final int py_;
        private final Anchor anchor_;

        /**
         * Constructor.
         *
         * @param  label   grid line label text
         * @param  px  graphics X coordinate
         * @param  py  graphics Y coordinate
         * @param  anchor  text anchor (includes baseline angle)
         */
        public SkyTick( Caption label, int px, int py, Anchor anchor ) {
            label_ = label;
            px_ = px;
            py_ = py;
            anchor_ = anchor;
        }

        /**
         * Returns the label bounds for this tick.
         *
         * @param  captioner  text renderer
         * @return  bounds
         */
        private Rectangle getCaptionBounds( Captioner captioner ) {
            return getCaptionBoundsAt( px_, py_, captioner );
        }

        /**
         * Returns the label bounds for this tick drawn at
         * a given point.
         *
         * @param  captioner  text renderer
         * @return  bounds
         */
        private Rectangle getCaptionBoundsAt( int px, int py,
                                              Captioner captioner ) {
            return anchor_.getCaptionBounds( label_, px, py, captioner );
        }
    }
}
