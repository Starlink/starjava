package uk.ac.starlink.topcat.plot;

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
import javax.swing.JComponent;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.RowSubset;

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

    /** Gives the fractional gap between data extrema and edge of plot. */
    private double padRatio_ = 0.02;

    /**
     * Constructor.
     */
    public LinesPlot() {
        zoomer_ = new Zoomer();
        addMouseListener( zoomer_ );
        addMouseMotionListener( zoomer_ );
    }

    /**
     * Sets the data set for this plot.  These are the points which will
     * be plotted the next time this component is painted.
     * 
     * @param   points  data points
     */
    public void setPoints( Points points ) {
        points_ = points;
    }

    /**
     * Returns the data set for this point.
     *
     * @return  data points
     */
    public Points getPoints() {
        return points_;
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
        Points points = getPoints();
        LinesPlotState state = getState();
        if ( points == null || state == null || ! state.getValid() ) {
            return;
        }
        int npoint = points.getCount();

        boolean xLogFlag = state.getLogFlags()[ 0 ];
        boolean xFlipFlag = state.getFlipFlags()[ 0 ];
        boolean[] yLogFlags = state.getYLogFlags();
        final boolean[] yFlipFlags = state.getYFlipFlags();

        /* Adjust data ranges as requested. */
        ValueInfo xInfo = state.getAxes()[ 0 ];
        ValueInfo[] yInfos = state.getYAxes();
        int ngraph = yInfos.length;
        double[] xRange = (double[]) state.getRanges()[ 0 ].clone();
        double[][] yRanges = new double[ ngraph ][];
        for ( int igraph = 0; igraph < ngraph; igraph++ ) {
            yRanges[ igraph ] = (double[]) state.getYRanges()[ igraph ].clone();
        }

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
            Graphics g1 = g.create();
            final GraphSurface graph = graphs[ igraph ];
            Rectangle displayBox = new Rectangle( xPos, yPos, xInc, yInc );
            graph.setBounds( displayBox );
            graph.paintSurface( g1 );
            g1.setColor( Color.WHITE );
            g1.fillRect( xPos, yPos, xInc, yInc );
            g1.setColor( Color.BLACK );
            g1.drawRect( xPos, yPos, xInc, yInc );
            Graphics gx = g1.create();
            gx.translate( xPos, yPos + yInc );
            xAxis.setDrawText( igraph == 0 );
            xAxis.annotateAxis( gx );
            Graphics2D gy = (Graphics2D) g1.create();
            gy.translate( xPos, yPos + yInc );
            gy.rotate( - Math.PI * 0.5 );
            yAxes[ igraph ].annotateAxis( gy );
            Rectangle axisBox = new Rectangle( 0, yPos, xPos, yInc );
            final int ig = igraph;
            final int yp = yPos;

            /* Listen for zoom events in the Y axis region. */
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

        /* Listen for zoom events in the X axis region. */
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
            Graphics lineGraphics = null;
            if ( style.getLine() == MarkStyle.DOT_TO_DOT ) {
                lineGraphics = g.create();
                style.configureForLine( lineGraphics,
                                        BasicStroke.CAP_BUTT,
                                        BasicStroke.JOIN_MITER );
                lineGraphics.setClip( graph.getClip() );
                if ( lineGraphics instanceof Graphics2D ) {
                    ((Graphics2D) lineGraphics)
                   .setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                                      state.getAntialias()
                                         ? RenderingHints.VALUE_ANTIALIAS_ON
                                         : RenderingHints.VALUE_ANTIALIAS_OFF );
                }
            }
            Graphics pointGraphics = null;
            if ( ! style.getHidePoints() ) {
                pointGraphics = g.create();
                pointGraphics.setClip( graph.getClip() );
            }
            RowSubset rset = sets[ iset ];
            boolean notFirst = false;
            int lastxp = 0;
            int lastyp = 0;
            for ( int ip = 0; ip < npoint; ip++ ) {
                if ( rset.isIncluded( (long) ip ) ) {
                    points.getCoords( ip, coords );
                    double x = coords[ 0 ];
                    double y = coords[ 1 ];
                    Point point = graph.dataToGraphics( x, y, false );
                    if ( point != null ) {
                        int xp = point.x;
                        int yp = point.y;
                        if ( pointGraphics != null ) {
                            style.drawMarker( pointGraphics, xp, yp );
                        }
                        if ( notFirst ) {
                            if ( lineGraphics != null ) {
                                lineGraphics.drawLine( lastxp, lastyp, xp, yp );
                            }
                        }
                        else {
                            notFirst = true;
                        }
                        lastxp = xp;
                        lastyp = yp;
                    }
                }
            }
        }
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

    protected void paintComponent( Graphics g ) {
        super.paintComponent( g );
        if ( isOpaque() ) {
            Graphics g1 = g.create();
            g1.setColor( getBackground() );
            g1.fillRect( 0, 0, getWidth(), getHeight() );
        }
        drawData( g, this );
    }
}
