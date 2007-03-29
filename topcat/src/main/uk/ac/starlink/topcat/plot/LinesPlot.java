package uk.ac.starlink.topcat.plot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Arrays;
import java.util.BitSet;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.convert.ValueConverter;

/**
 * Component which paints a stack of line plots.
 *
 * @author   Mark Taylor
 * @since    3 Mar 2006
 */
public abstract class LinesPlot extends JComponent {

    private LinesPlotState state_;
    private Points points_;
    private Zoomer zoomer_;
    private PlotSurface[] surfaces_;
    private int[] sequence_;
    private Rectangle plotRegion_;
    private int[][] work_;

    /**
     * Constructor.
     */
    public LinesPlot() {
        zoomer_ = new Zoomer();
        addMouseListener( zoomer_ );
        addMouseMotionListener( zoomer_ );
        surfaces_ = new PlotSurface[ 0 ];
        work_ = new int[ 2 ][ 10240 ];
    }

    /**
     * Sets the data set for this plot.  These are the points which will
     * be plotted the next time this component is painted.
     * 
     * @param   points  data points
     */
    public void setPoints( Points points ) {
        if ( points != points_ ) {
            sequence_ = sortX( points );
        }
        points_ = points;
    }

    /**
     * Sets the plot state for this plot.  This characterises how the
     * plot will be done next time this component is painted.
     *
     * @param  state  plot state
     */
    public void setState( LinesPlotState state ) {
        state_ = state;
    }

    /**
     * Returns the most recently set state for this plot.
     *
     * @return  plot state
     */
    public LinesPlotState getState() {
        return state_;
    }

    /**
     * Performs the drawing.
     *
     * @param  g  graphics conext
     * @param  c  target component
     */
    private void drawData( Graphics g, Component c ) {

        /* Acquire state. */
        Points points = points_;
        LinesPlotState state = getState();
        if ( points == null || state == null || ! state.getValid() ) {
            return;
        }
        int npoint = points.getCount();
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
                              10 )
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
                                  fm, AxisLabeller.Y, 6 );
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
                                               fm, AxisLabeller.X, 10 );

        /* Position each graph and draw the axes. */
        zoomer_.getRegions().clear();
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

            /* Set up and install a zoom region for Y zoom events. */
            Rectangle axisBox = new Rectangle( 0, yPos, xPos, yInc );
            final int ig = igraph;
            final int yp = yPos;
            ZoomRegion yZoom = new AxisZoomRegion( false,
                                                   axisBox, displayBox ) {
                public void zoomed( double[][] bounds ) {
                    double v0 = bounds[ 0 ][ 0 ];
                    double v1 = bounds[ 0 ][ 1 ];
                    int y0 = (int) Math.round( yp + v0 * yInc );
                    int y1 = (int) Math.round( yp + v1 * yInc );
                    boolean flip = yFlipFlags[ ig ];
                    requestZoomY( ig,
                        graph.graphicsToData( xPos,
                                              flip ? y0 : y1, false )[ 1 ],
                        graph.graphicsToData( xPos,
                                              flip ? y1 : y0, false )[ 1 ] );
                }
            };
            zoomer_.getRegions().add( yZoom );
            yPos -= yInc;
        }

        /* Set up and install a zoom region for X zoom events. */
        Rectangle xTarget =
            new Rectangle( xPos, height - border.bottom, xInc, border.bottom );
        Rectangle xDisplay =
            new Rectangle( xPos, border.top, xInc, yInc * ngraph );
        final GraphSurface graph0 = graphs[ ngraph - 1 ];
        ZoomRegion xZoom = new AxisZoomRegion( true, xTarget, xDisplay ) {
            public void zoomed( double[][] bounds ) {
                double v0 = bounds[ 0 ][ 0 ];
                double v1 = bounds[ 0 ][ 1 ];
                int x0 = (int) Math.round( xPos + v0 * xInc );
                int x1 = (int) Math.round( xPos + v1 * xInc );
                requestZoomX( graph0.graphicsToData( x0, 0, false )[ 0 ],
                              graph0.graphicsToData( x1, 0, false )[ 0 ] );
            }
        };
        zoomer_.getRegions().add( xZoom );

        /* Draw data points and lines. */
        RowSubset[] sets = state.getPointSelection().getSubsets();
        Style[] styles = state.getPointSelection().getStyles();
        int[] graphIndices = state.getGraphIndices();
        int nset = sets.length;
        double[] coords = new double[ 2 ];
        for ( int iset = 0; iset < nset; iset++ ) {
            MarkStyle style = (MarkStyle) styles[ iset ];
            GraphSurface graph = graphs[ graphIndices[ iset ] ];
            boolean noLines = style.getLine() != MarkStyle.DOT_TO_DOT;
            Rectangle graphClip = graph.getClip().getBounds();
            if ( g.hitClip( graphClip.x, graphClip.y,
                            graphClip.width, graphClip.height ) ) {
                Graphics g1 = g.create();
                g1.clipRect( graphClip.x, graphClip.y,
                             graphClip.width, graphClip.height );
                PointPlotter plotter =
                    new PointPlotter( g1, style, state.getAntialias(),
                                      work_[ 0 ], work_[ 1 ] );
                RowSubset rset = sets[ iset ];
                for ( int ix = 0; ix < npoint; ix++ ) {
                    int ip = sequence_ == null ? ix : sequence_[ ix ];
                    if ( rset.isIncluded( (long) ip ) ) {
                        points.getCoords( ip, coords );
                        Point point =
                            graph.dataToGraphics( coords[ 0 ], coords[ 1 ],
                                                  noLines );
                        if ( point != null ) {
                            plotter.point( point );
                        }
                    }
                }
                plotter.flush();
                plotter.dispose();
                g1.dispose();
            }
        }

        /* Mark active points. */
        MarkStyle target = MarkStyle.targetStyle();
        Graphics targetGraphics = g.create();
        targetGraphics.setColor( Color.BLACK );
        int[] activePoints = state.getActivePoints();
        for ( int i = 0; i < activePoints.length; i++ ) {
            int ip = activePoints[ i ];
            PlotSurface surface = null;
            for ( int iset = 0; iset < nset && surface == null; iset++ ) {
                if ( sets[ iset ].isIncluded( ip ) ) {
                    surface = graphs[ graphIndices[ iset ] ];
                }
            }
            if ( surface != null && points.getCount() > 0 ) {
                points.getCoords( ip, coords );
                Point point =
                    surface.dataToGraphics( coords[ 0 ], coords[ 1 ], true );
                if ( point != null ) {
                    target.drawMarker( targetGraphics, point.x, point.y );
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
    }

    /**
     * Returns a bit vector indicating which points are in the X range
     * currently visible within this plot.
     *
     * @return   bit vector with true marking point indices in visible X range
     */
    public BitSet getPointsInRange() {
        double xlo = state_.getRanges()[ 0 ][ 0 ];
        double xhi = state_.getRanges()[ 0 ][ 1 ];
        Points points = points_;
        int npoint = points.getCount();
        RowSubset[] sets = state_.getPointSelection().getSubsets();
        int nset = sets.length;
        BitSet inRange = new BitSet();
        double[] coords = new double[ 2 ];
        for ( int ip = 0; ip < npoint; ip++ ) {
            long lp = (long) ip;
            boolean use = false;
            for ( int is = 0; is < nset && ! use; is++ ) {
                use = use || sets[ is ].isIncluded( lp );
            }
            if ( use ) {
                points.getCoords( ip, coords );
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
        if ( ! state_.getValid() ) {
            return null;
        }
        final Points points = points_;
        final int npoint = points.getCount();
        final RowSubset[] sets = state_.getPointSelection().getSubsets();
        final int nset = sets.length;
        final PlotSurface[] setSurfaces = new PlotSurface[ nset ];
        int[] graphIndices = state_.getGraphIndices();
        for ( int iset = 0; iset < nset; iset++ ) {
            setSurfaces[ iset ] = surfaces_[ graphIndices[ iset ] ];
        }
        return new PointIterator() {
            int ip = -1;
            int[] point = new int[ 3 ];
            double[] coords = new double[ 2 ];
            protected int[] nextPoint() {
                while ( ++ip < npoint ) {
                    long lp = (long) ip;
                    PlotSurface surface = null;
                    for ( int is = 0; is < nset && surface == null; is++ ) {
                        if ( sets[ is ].isIncluded( lp ) ) {
                            surface = setSurfaces[ is ];
                        }
                    }
                    if ( surface != null ) {
                        points.getCoords( ip, coords );
                        double x = coords[ 0 ];
                        double y = coords[ 1 ];
                        Point p = surface.dataToGraphics( x, y, true );
                        if ( p != null ) {
                            point[ 0 ] = ip;
                            point[ 1 ] = p.x;
                            point[ 2 ] = p.y;
                            return point;
                        }
                    }
                }
                return null;
            }
        };
    }

    /**
     * Returns the rectangle which is the union of all the graph regions,
     * that is the region between the axes, for the last time a plot was
     * drawn.
     *
     * @return   plot region rectangle
     */
    public Rectangle getPlotRegion() {
        return plotRegion_;
    }

    /**
     * Indicates that the user has asked to zoom to a particular region
     * in the X direction.
     *
     * @param   x0  lower bound of new view region in data coordinates
     * @param   x1  upper bound of new view region in data coordinates
     */
    protected abstract void requestZoomX( double x0, double x1 );

    /**
     * Indicates that the user has asked to zoom to a particular region
     * in the Y direction for one of the graphs.
     *
     * @param  igraph  index of graph to zoom on
     * @param  y0    lower bound of new view region in data coordinates
     * @param  y1    upper bound of new view region in data coordinates
     */
    protected abstract void requestZoomY( int igraph,
                                          double y0, double y1 );

    /**
     * Returns a component which will display the current position of
     * the mouse pointer when it is in this component.
     *
     * @return   position display component
     */
    public JComponent createPositionLabel() {
        return new LinesPositionLabel();
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
     * Returns an array giving the order in which the elements of the
     * Points structure should be processed.  This is numerically
     * increasing order of the X coordinate.
     * A null return indicates that the natural ordering may be used.
     *
     * @param   points   points data
     * @return  array of indices of ordered elements of <code>points</code>
     */
    private int[] sortX( Points points ) {
        int npoint = points.getCount();
        double[] coords = new double[ 2 ];

        /* As an optimisation, check if the points are sorted by X already.
         * This will often be the case and we can save ourselves construction
         * and disposal of a potentially large array in this case. */
        boolean sorted = true;
        double last = Double.NaN;
        for ( int i = 0; i < npoint && sorted; i++ ) {
            points.getCoords( i, coords );
            if ( coords[ 0 ] < last ) {
                sorted = false;
            }
            last = coords[ 0 ];
        }
        if ( sorted ) {
            return null;
        }

        /* Set up a sortable item class. */
        class Item implements Comparable {
            final int index_;
            final double x_;
            Item( int index, double x ) {
                index_ = index;
                x_ = x;
            }
            public int compareTo( Object o ) {
                double x1 = this.x_;
                double x2 = ((Item) o).x_;
                if ( x1 > x2 ) {
                    return +1;
                }
                else if ( x1 < x2 ) {
                    return -1;
                }
                else if ( x1 == x2 ) {
                    return 0;
                }
                else if ( Double.isNaN( x1 ) && Double.isNaN( x2 ) ) {
                    return 0;
                }
                else if ( Double.isNaN( x1 ) ) {
                    return +1;
                }
                else if ( Double.isNaN( x2 ) ) {
                    return -1;
                }
                else {
                    throw new AssertionError( x1 + " cmp " + x2 + "??" );
                }
            }
        }

        /* Construct and populate an array representing the points data. */
        Item[] items = new Item[ npoint ];
        for ( int i = 0; i < npoint; i++ ) {
            points.getCoords( i, coords );
            items[ i ] = new Item( i, coords[ 0 ] );
        }

        /* Sort the array. */
        Arrays.sort( items );

        /* Extract the sorted indices into a usable form. */
        int[] indices = new int[ npoint ];
        for ( int i = 0; i < npoint; i++ ) {
            indices[ i ] = items[ i ].index_;
        }

        /* Return the result. */
        return indices;
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
         * @param  work1  workspace array
         * @param  work2  workspace array
         */
        PointPlotter( Graphics g, MarkStyle style, boolean antialiasLines,
                      int[] work1, int[] work2 ) {
            style_ = style;
            xWork_ = work1;
            yWork_ = work2;
            nWork_ = Math.min( xWork_.length, yWork_.length );

            /* Set up a graphics context for plotting points.
             * Null if no points are to be plotted. */
            pointGraphics_ = style.getHidePoints() ? null : g.create();

            /* Set up a graphics context for drawing lines.
             * Null if no lines are to be drawn. */
            if ( style.getLine() == MarkStyle.DOT_TO_DOT ) {
                lineGraphics_ = g.create();
                style.configureForLine( lineGraphics_, BasicStroke.CAP_BUTT,
                                        BasicStroke.JOIN_ROUND );
                if ( lineGraphics_ instanceof Graphics2D ) {
                    ((Graphics2D) lineGraphics_).setRenderingHint(
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
                if ( pointGraphics_ != null &&
                     pointBounds_.contains( point ) ) {
                    style_.drawMarker( pointGraphics_, point.x, point.y );
                }
                if ( lineGraphics_ != null ) {
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

    /**
     * Component which can display the current coordinates of the cursor
     * in this plot.
     *
     * @see  PositionLabel
     */
    private class LinesPositionLabel extends JLabel
                                     implements MouseMotionListener {
        private int isurf_;
        private PlotSurface surface_;
        private PositionReporter reporter_;

        /**
         * Constructor.
         */
        LinesPositionLabel() {
            setFont( new Font( "Monospaced", getFont().getStyle(),
                               getFont().getSize() ) );
            setBorder( BorderFactory.createEtchedBorder() );
            LinesPlot.this.addMouseMotionListener( this );
            reportPosition( null );
        }

        public void mouseMoved( MouseEvent evt ) {
            reportPosition( evt.getPoint() );
        }

        public void mouseDragged( MouseEvent evt ) {
            reportPosition( evt.getPoint() );
        }

        /**
         * Reports the position at a given point by drawing as the text
         * content of this label.
         *
         * @param   p  position
         */
        void reportPosition( Point p ) {
            PositionReporter reporter = getReporter( p );
            StringBuffer sbuf = new StringBuffer( "Position: " );
            if ( reporter != null ) {
                String[] fc = reporter.formatPosition( p.x, p.y );
                if ( fc != null ) {
                    sbuf.append( '(' )
                        .append( fc[ 0 ] )
                        .append( ", " )
                        .append( fc[ 1 ] )
                        .append( ')' );
                }
            }
            setText( sbuf.toString() );
        }

        /**
         * Returns a reporter object which corresponds to the given position.
         *
         * @param   p  point at which reporting is required
         * @return  position reporter which knows about coordinates
         *          at <code>p</code> (may be null if invalid position)
         */
        private PositionReporter getReporter( Point p ) {

            /* No point, no reporter. */
            if ( p == null || state_ == null || ! state_.getValid() ) {
                return null;
            }

            /* If the reporter required is the same as for the last call
             * to this method, and if that reporter is still valid for
             * this plot, return the same one. */
            else if ( isurf_ < surfaces_.length &&
                      surface_ == surfaces_[ isurf_ ] &&
                      surface_.getClip().contains( p ) ) {
                return reporter_;
            }

            /* Otherwise, create a new suitable reporter if necessary. */
            else {

                /* Search through the plot surfaces corresponding to the
                 * graphs plotted last time we did a paint. */
                for ( int isurf = 0; isurf < surfaces_.length; isurf++ ) {
                    PlotSurface surf = surfaces_[ isurf ];
                    if ( surf.getClip().contains( p ) ) {

                        /* If the point is within one, construct a new 
                         * suitable reporter, save it and enough information
                         * to be able to tell whether it's still valid later,
                         * and return it. */
                        ValueConverter xConv = state_.getConverters()[ 0 ];
                        ValueConverter yConv = state_.getYConverters()[ isurf ];
                        isurf_ = isurf;
                        surface_ = surf;
                        reporter_ = new PositionReporter( surf, xConv, yConv ) {
                            protected void reportPosition( String[] coords ) {
                            }
                        };
                        return reporter_;
                    }
                }

                /* Point not in any of the known graphs - return null. */
                return null;
            }
        }
    }
}
