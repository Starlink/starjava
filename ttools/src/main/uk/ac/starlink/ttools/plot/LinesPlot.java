package uk.ac.starlink.ttools.plot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import uk.ac.starlink.table.ValueInfo;

/**
 * Component which paints a stack of line plots.
 *
 * @author   Mark Taylor
 * @since    3 Mar 2006
 */
public class LinesPlot extends TablePlot {

    private PlotSurface[] surfaces_;
    private PlotData rawData_;
    private PlotData sortedData_;
    private int[] sequence_;
    private Rectangle plotRegion_;
    private int[][] work_;
    private final int axPad_ = 16;

    /**
     * Constructor.
     */
    public LinesPlot() {
        surfaces_ = new PlotSurface[ 0 ];
        work_ = new int[ 2 ][ 10240 ];
    }

    public void setState( PlotState state ) {
        super.setState( state );
        PlotData data = state.getPlotData();
        if ( sortedData_ == null || ! data.equals( rawData_ ) ) {
            rawData_ = data;
            sortedData_ = sortX( data );
        }
    }

    public PlotSurface[] getSurfaces() {
        return surfaces_;
    }

    /**
     * Performs the drawing.
     *
     * @param  g  graphics conext
     * @param  c  target component
     */
    private void drawData( Graphics g, Component c ) {

        /* Acquire state. */
        LinesPlotState state = (LinesPlotState) getState();
        if ( state == null || ! state.getValid() ) {
            return;
        }
        PlotData data = sortedData_;
        if ( data == null ) {
            return;
        }
        boolean grid = state.getGrid();
        boolean xLogFlag = state.getLogFlags()[ 0 ];
        boolean xFlipFlag = state.getFlipFlags()[ 0 ];
        boolean[] yLogFlags = state.getYLogFlags();
        final boolean[] yFlipFlags = state.getYFlipFlags();
        ValueInfo[] yInfos = state.getYAxes();
        int ngraph = yInfos.length;
        double[] xRange = state.getRanges()[ 0 ];
        double[][] yRanges = state.getYRanges();

        /* Get component dimensions. */
        FontMetrics fm = g.getFontMetrics();
        int height = getSize().height;
        int width = getSize().width;
        Insets border = getInsets();
        border.top++;
        border.right++;

        /* Work out how much space is required for the annotations at the
         * bottom of the plot (single annotated X axis). */
        int approxWidth = width - border.left - border.right;
        border.bottom +=
            new AxisLabeller( state.getAxisLabels()[ 0 ],
                              xRange[ 0 ], xRange[ 1 ], approxWidth,
                              state.getLogFlags()[ 0 ],
                              state.getFlipFlags()[ 0 ], fm, AxisLabeller.X,
                              10, axPad_, axPad_ )
           .getAnnotationHeight();

        /* Work out the available height for each plotted graph. */
        final int yInc = ( height - border.bottom - border.top ) / ngraph;

        /* Adjust border.top so it mops up any unused pixels (useful for
         * positioning calculations). */
        border.top = height - border.bottom - yInc * ngraph;

        /* Assemble an array of graphs to draw data on, and calculate 
         * the borders we need to leave for axis annotation. */
        GraphSurface[] graphs = new GraphSurface[ ngraph ];
        AxisLabeller[] yAxes = new AxisLabeller[ ngraph ];
        for ( int igraph = 0; igraph < ngraph; igraph++ ) {
            double ylo = yRanges[ igraph ][ 0 ];
            double yhi = yRanges[ igraph ][ 1 ];
            graphs[ igraph ] =
                new GraphSurface( this,
                                  xLogFlag, yLogFlags[ igraph ],
                                  xFlipFlag, yFlipFlags[ igraph ] );
            graphs[ igraph ].setDataRange( xRange[ 0 ], ylo, xRange[ 1 ], yhi );
            yAxes[ igraph ] =
                new AxisLabeller( state.getYAxisLabels()[ igraph ], ylo, yhi,
                                  yInc,
                                  yLogFlags[ igraph ], yFlipFlags[ igraph ],
                                  fm, AxisLabeller.Y, 6, axPad_, axPad_ );
            int left = yAxes[ igraph ].getAnnotationHeight();
            border.left = Math.max( border.left,
                                    yAxes[ igraph ].getAnnotationHeight() );
        }

        /* Work out available width for plotted graphs and set up axis
         * labeller appropriately. */
        final int xInc = width - border.left - border.right;
        final int xPos = border.left;
        int yPos = height - border.bottom - yInc;
        AxisLabeller xAxis = new AxisLabeller( state.getAxisLabels()[ 0 ],
                                               xRange[ 0 ], xRange[ 1 ], xInc,
                                               state.getLogFlags()[ 0 ],
                                               state.getFlipFlags()[ 0 ],
                                               fm, AxisLabeller.X, 10,
                                               axPad_, axPad_ );

        /* Position each graph and draw the axes. */
        for ( int igraph = 0; igraph < ngraph; igraph++ ) {
            final GraphSurface graph = graphs[ igraph ];
            Rectangle displayBox = new Rectangle( xPos, yPos, xInc, yInc );
            graph.setBounds( displayBox );

            /* Paint graph background. */
            Color textColor = Color.BLACK;
            Color gridColor = Color.LIGHT_GRAY;
            Graphics g1 = g.create();
            graph.paintSurface( g1 );
            g1.setColor( Color.WHITE );
            g1.fillRect( xPos, yPos, xInc, yInc );

            /* Draw the X axis. */
            Graphics gx = g1.create();
            gx.translate( xPos, yPos + yInc );
            xAxis.setDrawText( igraph == 0 );
            if ( grid ) {
                gx.setColor( gridColor );
                xAxis.drawGridLines( gx, 0, -yInc );
            }
            gx.setColor( textColor );
            xAxis.annotateAxis( gx );
            gx.dispose();

            /* Draw the Y axis. */
            Graphics2D gy = (Graphics2D) g1.create();
            gy.translate( xPos, yPos + yInc );
            gy.rotate( - Math.PI * 0.5 );

            /* Draw Y grid lines if required. */
            if ( grid ) {
                gy.setColor( gridColor );
                yAxes[ igraph ].drawGridLines( gy, 0, xInc );
            }

            /* Draw a single y=0 grid line if required. */
            if ( state.getYZeroFlag() && yRanges[ igraph ][ 0 ] < 0.0
                                      && yRanges[ igraph ][ 1 ] > 0.0 ) {
                gy.setColor( gridColor );
                yAxes[ igraph ].drawGridLine( gy, 0, xInc, 0.0 );
            }

            /* Annotate the Y axis. */
            gy.setColor( textColor );
            yAxes[ igraph ].annotateAxis( gy );
            gy.dispose();

            /* Draw outline. */
            g1.setColor( textColor );
            g1.drawRect( xPos, yPos, xInc, yInc );
            g1.dispose();

            /* Move to next position. */
            yPos -= yInc;
        }

        /* Draw data points and lines. */
        int[] graphIndices = state.getGraphIndices();
        int nerr = data.getNerror();
        int[] xoffs = new int[ nerr ];
        int[] yoffs = new int[ nerr ];
        int nset = data.getSetCount();
        for ( int iset = 0; iset < nset; iset++ ) {
            MarkStyle style = (MarkStyle) data.getSetStyle( iset );
            GraphSurface graph = graphs[ graphIndices[ iset ] ];
            boolean noLines = style.getLine() != MarkStyle.DOT_TO_DOT;
            boolean hasErrors = MarkStyle.hasErrors( style, data );
            Rectangle graphClip = graph.getClip().getBounds();
            if ( g.hitClip( graphClip.x, graphClip.y,
                            graphClip.width, graphClip.height ) ) {
                Graphics g1 = g.create();
                g1.clipRect( graphClip.x, graphClip.y,
                             graphClip.width, graphClip.height );
                PointPlotter plotter =
                    new PointPlotter( g1, style, state.getAntialias(),
                                      hasErrors, work_[ 0 ], work_[ 1 ] );
                PointSequence pseq = data.getPointSequence();
                while ( pseq.next() ) {
                    if ( pseq.isIncluded( iset ) ) {
                        double[] coords = pseq.getPoint();
                        Point point =
                            graph.dataToGraphics( coords[ 0 ], coords[ 1 ],
                                                  noLines );
                        if ( point != null ) {
                            if ( hasErrors ) {
                                double[][] errors = pseq.getErrors();
                                if ( ScatterPlot
                                    .transformErrors( point, coords, errors,
                                                      graph, xoffs, yoffs ) ) {
                                    plotter.errors( point, xoffs, yoffs );
                                }
                            }
                            plotter.point( point );
                        }
                    }
                }
                pseq.close();
                plotter.flush();
                plotter.dispose();
                g1.dispose();
            }
        }

        /* Draw text labels. */
        if ( data.hasLabels() ) {
            for ( int iset = 0; iset < nset; iset++ ) {
                MarkStyle style = (MarkStyle) data.getSetStyle( iset );
                GraphSurface graph = graphs[ graphIndices[ iset ] ];
                Rectangle graphClip = graph.getClip().getBounds();
                if ( g.hitClip( graphClip.x, graphClip.y,
                                graphClip.width, graphClip.height ) ) {
                    PointSequence pseq = data.getPointSequence();
                    while ( pseq.next() ) {
                        String label = pseq.getLabel();
                        if ( label != null && label.trim().length() > 0 &&
                             pseq.isIncluded( iset ) ) {
                            double[] coords = pseq.getPoint();
                            Point point =
                                graph.dataToGraphics( coords[ 0 ], coords[ 1 ],
                                                      true );
                            if ( point != null ) {
                                style.drawLabel( g, point.x, point.y, label );
                            }
                        }
                    }
                    pseq.close();
                }
            }
        }

        /* Store graph set. */
        surfaces_ = graphs;
        plotRegion_ = graphs.length > 0 ? graphs[ 0 ].getClip().getBounds()
                                        : new Rectangle();
        for ( int i = 0; i < graphs.length; i++ ) {
            plotRegion_.add( graphs[ i ].getClip().getBounds() );
        }

        /* Notify listeners that the plot has changed.  We haven't counted
         * statistics, so use dummy values. */
        firePlotChangedLater( new PlotEvent( this, state, -1, -1, -1 ) );
    }

    /**
     * Calculates data ranges along the X and Y axes for a given
     * point selection and data object.  The result has 1 + nplot ranges.
     * 
     * @param  data  plot data
     * @param  state  plot state
     * @param  xLimits  (lower,upper) bounds array giving region for range
     *         determination; may be null for the whole X region
     */
    public DataBounds calculateBounds( PlotData data, PlotState state,
                                       double[] xLimits ) {
        LinesPlotState lState = (LinesPlotState) state;
        int ngraph = lState.getGraphCount();
        int[] graphIndices = lState.getGraphIndices();

        /* Set X bounds. */
        double xlo = xLimits == null ? -Double.MAX_VALUE : xLimits[ 0 ];
        double xhi = xLimits == null ? +Double.MAX_VALUE : xLimits[ 1 ];

        /* Set up initial values for extrema. */
        Range xRange = new Range();
        Range[] yRanges = new Range[ ngraph ];
        for ( int ig = 0; ig < ngraph; ig++ ) {
            yRanges[ ig ] = new Range();
        }

        /* Go through all the data finding extrema. */
        int nset = data.getSetCount();
        int ip = 0;
        int[] npoints = new int[ nset ];
        PointSequence pseq = data.getPointSequence();
        while ( pseq.next() ) {
            double[] coords = pseq.getPoint();
            double x = coords[ 0 ];
            double y = coords[ 1 ];
            if ( x >= xlo && x <= xhi ) {
                boolean isUsed = false;
                for ( int iset = 0; iset < nset; iset++ ) {
                    if ( pseq.isIncluded( iset ) ) {
                        npoints[ iset ]++;
                        isUsed = true;
                        int igraph = graphIndices[ iset ];
                        yRanges[ igraph ].submit( y );
                    }
                }
                if ( isUsed ) {
                    xRange.submit( x );
                }
            }
            ip++;
        }
        pseq.close();

        /* Package and return calculated bounds. */
        Range[] ranges = new Range[ ngraph + 1 ];
        ranges[ 0 ] = xRange;
        System.arraycopy( yRanges, 0, ranges, 1, ngraph );
        return new DataBounds( ranges, ip, npoints );
    }

    /**
     * Returns a bit vector indicating which points are in the X range
     * currently visible within this plot.
     *
     * @return   bit vector with true marking point indices in visible X range
     */
    public BitSet getPointsInRange() {
        LinesPlotState state = (LinesPlotState) getState();
        double xlo = state.getRanges()[ 0 ][ 0 ];
        double xhi = state.getRanges()[ 0 ][ 1 ];
        PlotData data = rawData_;
        int nset = data.getSetCount();
        BitSet inRange = new BitSet();
        PointSequence pseq = data.getPointSequence();
        for ( int ip = 0; pseq.next(); ip++ ) {
            boolean use = false;
            for ( int is = 0; is < nset && ! use; is++ ) {
                use = use || pseq.isIncluded( is );
            }
            if ( use ) {
                double[] coords = pseq.getPoint();
                double x = coords[ 0 ];
                if ( x >= xlo && x <= xhi ) {
                    inRange.set( ip );
                }
            }
        }
        return inRange;
    }

    /**
     * Returns an iterator over the points which are visible in this plot.
     *
     * @return  point iterator
     */
    public PointIterator getPlottedPointIterator() {
        LinesPlotState state = (LinesPlotState) getState();
        if ( ! state.getValid() ) {
            return PointIterator.EMPTY;
        }
        final PlotData data = rawData_;
        final int nset = data.getSetCount();
        final PlotSurface[] setSurfaces = new PlotSurface[ nset ];
        int[] graphIndices = state.getGraphIndices();
        for ( int iset = 0; iset < nset; iset++ ) {
            setSurfaces[ iset ] = surfaces_[ graphIndices[ iset ] ];
        }
        return new PointIterator() {
            private int ip_ = -1;
            private int is_ = -1;
            private double[] coords_;
            private PointSequence pseq_ = data.getPointSequence();
            private final int[] point_ = new int[ 3 ];
            protected int[] nextPoint() {
                while ( pseq_ != null ) {
                    if ( coords_ == null ) {
                        if ( pseq_.next() ) {
                            coords_ = pseq_.getPoint();
                            ip_++;
                            is_ = -1;
                        }
                        else {
                            pseq_.close();
                            pseq_ = null;
                        }
                    }
                    else if ( ++is_ < nset ) {
                        if ( pseq_.isIncluded( is_ ) ) {
                            Point xy = setSurfaces[ is_ ]
                                      .dataToGraphics( coords_[ 0 ],
                                                       coords_[ 1 ], true );
                            if ( xy != null ) {
                                point_[ 0 ] = ip_;
                                point_[ 1 ] = xy.x;
                                point_[ 2 ] = xy.y;
                                return point_;
                            }
                        }
                    }
                    else {
                        coords_ = null;
                    }
                }
                return null;
            }
        };
    }

    /**
     * Returns an array of point placers, one for each constituent graph
     * in this plot.
     *
     * @return  point placer array
     */
    public PointPlacer[] getPointPlacers() {
        int ngraph = surfaces_.length;
        PointPlacer[] placers = new PointPlacer[ ngraph ];
        for ( int igraph = 0; igraph < ngraph; igraph++ ) {
            final PlotSurface surface = surfaces_[ igraph ];
            placers[ igraph ] = new PointPlacer() {
                public Point getXY( double[] coords ) {
                    return surface.dataToGraphics( coords[ 0 ], coords[ 1 ],
                                                   true );

                }
            };
        }
        return placers;
    }

    /**
     * Returns the rectangle which is the union of all the graph regions,
     * that is the region between the axes, for the last time a plot was
     * drawn.
     *
     * @return   plot region rectangle
     */
    public Rectangle getPlotBounds() {
        return plotRegion_ == null ? getBounds()
                                   : plotRegion_;
    }

    protected void paintComponent( Graphics g ) {
        super.paintComponent( g );
        if ( isOpaque() ) {
            Color color = g.getColor();
            g.setColor( getBackground() );
            g.fillRect( 0, 0, getWidth(), getHeight() );
            g.setColor( color );
        }
        drawData( g, this );
    }

    /**
     * Returns a version of a PlotData object which is sorted by X value.
     * As a special case, null is returned if the input data has no 
     * points in it.
     *
     * @param   rawData  unsorted plot data
     * @return   sorted version of <code>rawData</code>; may be the same
     *           object
     */
    private static PlotData sortX( PlotData rawData ) {

        /* See if the list is already sorted. */
        boolean sorted = true;
        PointSequence pseq = rawData.getPointSequence();
        int np = 0;
        for ( double lastX = Double.NEGATIVE_INFINITY; pseq.next(); np++ ) {
            if ( sorted ) {
                double x = pseq.getPoint()[ 0 ];
                if ( x >= lastX ) {
                    lastX = x;
                }
                else {
                    sorted = false;
                }
            }
        }
        pseq.close();

        /* If there is no data, return null. */
        if ( np == 0 ) {
            return null;
        }

        /* If the list is already sorted, return it unchanged. */
        if ( sorted ) {
            return rawData;
        }

        /* Otherwise return a modified set which is sorted according to the
         * sequence of the X values. */
        ArrayPlotData sortData = ArrayPlotData.copyPlotData( rawData );
        Arrays.sort( sortData.getPoints(), new Comparator() {
            public int compare( Object o1, Object o2 ) {
                return compareByX( (PointData) o1, (PointData) o2 );
            }
        } );
        return sortData;
    }

    /**
     * Compares two point data objects by X coordinate.
     *
     * @param  p1  point 1
     * @param  p2  point 2
     */
    private static int compareByX( PointData p1, PointData p2 ) {
        double x1 = p1.getPoint()[ 0 ];
        double x2 = p2.getPoint()[ 0 ];
        if ( x1 < x2 ) {
            return -1;
        }
        else if ( x1 > x2 ) {
            return +1;
        }
        else {
            return p2.hashCode() - p1.hashCode();
        }
    }

    /**
     * Object which can accept points to plot and draw them onto the
     * graphics context in a suitable way.
     */
    private static class PointPlotter {
        private final MarkStyle style_;
        private final Graphics pointGraphics_;
        private final Graphics lineGraphics_;
        private final Rectangle pointBounds_;
        private final int huge_;
        private final boolean hasLines_;
        private final boolean hasMarks_;
        private final boolean hasErrors_;
        private int xLo_;
        private int xHi_;
        private Point lastPoint_;
        private boolean lastInclude_;
        private int[] xWork_;
        private int[] yWork_;
        private int nWork_;
        private int iLine_;

        /**
         * Constructor.
         *
         * @param  g  base graphics context - the clip is used to optimise
         *            what gets plotted
         * @param  style  the mark/line plotting style for points
         * @param  antialiasLines  true iff you want lines antialiased
         * @param  hasErrors   true if you might want errors drawn
         * @param  work1  workspace array
         * @param  work2  workspace array
         */
        PointPlotter( Graphics g, MarkStyle style, boolean antialiasLines,
                      boolean hasErrors, int[] work1, int[] work2 ) {
            style_ = style;
            xWork_ = work1;
            yWork_ = work2;
            nWork_ = Math.min( xWork_.length, yWork_.length );

            /* Work out which components we will need to be drawing. */
            hasLines_ = style.getLine() == MarkStyle.DOT_TO_DOT;
            hasMarks_ = ! style.getHidePoints();
            hasErrors_ = hasErrors;

            /* Set up a graphics context for plotting points.
             * Null if no points are to be plotted. */
            pointGraphics_ = hasMarks_ || hasErrors_ ? g.create() : null;

            /* Set up a graphics context for drawing lines.
             * Null if no lines are to be drawn. */
            if ( hasLines_ ) {
                lineGraphics_ = g.create();
                lineGraphics_.setColor( style.getColor() );
                if ( lineGraphics_ instanceof Graphics2D ) {
                    Graphics2D lg2 = (Graphics2D) lineGraphics_;
                    lg2.setStroke( style.getStroke( BasicStroke.CAP_BUTT,
                                                    BasicStroke.JOIN_ROUND ) );
                    lg2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        antialiasLines ? RenderingHints.VALUE_ANTIALIAS_ON
                                       : RenderingHints.VALUE_ANTIALIAS_OFF );
                }
            }
            else {
                lineGraphics_ = null;
            }

            /* Prepare clipping information. */
            Rectangle gClip = g.getClipBounds();
            int maxr = style.getMaximumRadius();
            pointBounds_ = new Rectangle( gClip.x - maxr, gClip.y - maxr,
                                          gClip.width + maxr * 2,
                                          gClip.height + maxr * 2 );
            xLo_ = gClip.x;
            xHi_ = gClip.x + gClip.width;
            huge_ = Math.max( gClip.height, gClip.width ) * 100;
        }

        /**
         * Adds a point to the sequence to be plotted.
         *
         * @param  point  point to plot
         */
        void point( Point point ) {
            if ( ! point.equals( lastPoint_ ) ) {
                if ( hasMarks_ && pointBounds_.contains( point ) ) {
                    style_.drawMarker( pointGraphics_, point.x, point.y );
                }
                if ( hasLines_ ) {
                    boolean include = point.x >= xLo_ && point.x <= xHi_;
                    if ( include && ! lastInclude_ && lastPoint_ != null ) {
                        addLineVertex( lastPoint_.x, lastPoint_.y );
                    }
                    if ( include || lastInclude_ ) {
                        addLineVertex( point.x, point.y );
                    }
                    lastInclude_ = include;
                }
                assert lastPoint_ != point
                    : "PlotSurface keeps returning same mutable Point object?";
                lastPoint_ = point;
            }
        }

        /**
         * Adds a set of errors to the sequence to be plotted.
         *
         * @param  point  central point
         * @param  xoffs  error point offsets in X dimension
         * @param  yoffs  error point offsets in Y dimension
         */
        void errors( Point point, int[] xoffs, int[] yoffs ) {
            if ( hasErrors_ ) {
                style_.drawErrors( pointGraphics_, point.x, point.y,
                                   xoffs, yoffs );
            }
        }

        /**
         * Adds a vertex to the list which will have lines drawn between
         * them.
         *
         * @param   x  X graphics coordinate
         * @param   y  Y graphics coordinate
         */
        private void addLineVertex( int x, int y ) {

            /* If we've filled up the points buffer, flush it. */
            if ( iLine_ >= nWork_ ) {
                flush();
            }

            /* If an attempt is made to draw to a line which is monstrously
             * far off the graphics clip, it can lead to problems (e.g. 
             * the display system attempting to locate so much memory that
             * the kernel kills the JVM).  In this case, approximate the
             * point to somewhere far away in roughly the right direction.
             * This isn't likely to happen very often in any case. */
            x = Math.max( -huge_, Math.min( huge_, x ) );
            y = Math.max( -huge_, Math.min( huge_, y ) );

            /* Store the point for later plotting. */
            xWork_[ iLine_ ] = x;
            yWork_[ iLine_ ] = y;
            iLine_++;
        }

        /**
         * Ensures that all points have been drawn.  
         * Since points are plotted in bursts for reasons of aesthetics and
         * efficiency, this must be called after all {@link #point} calls
         * to ensure that the drawing has actuall been done.
         */
        void flush() {
            if ( iLine_ > 1 ) {
                lineGraphics_.drawPolyline( xWork_, yWork_, iLine_ );
                xWork_[ 0 ] = xWork_[ iLine_ - 1 ];
                yWork_[ 0 ] = yWork_[ iLine_ - 1 ];
                iLine_ = 1;
            }
        }

        /**
         * Should be called to release resources when this plotter is no
         * longer required.  Calls {@link #flush}.
         */
        void dispose() {
            flush();
            if ( pointGraphics_ != null ) {
                pointGraphics_.dispose();
            }
            if ( lineGraphics_ != null ) {
                lineGraphics_.dispose();
            }
        }
    }
}
