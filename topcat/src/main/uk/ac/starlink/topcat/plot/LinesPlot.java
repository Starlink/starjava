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
public class LinesPlot extends javax.swing.JPanel {

    private LinesPlotState state_;
    private Points points_;
    private double[] xRange_;
    private double[][] yRanges_;

    /** Gives the fractional gap between data extrema and edge of plot. */
    private double padRatio_ = 0.02;

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
     * Updates the X and Y ranges of the plotting surface so that all the
     * data points which are currently selected for plotting will fit in
     * nicely.
     */
    public void rescale() {

        /* Acquire data. */
        Points points = getPoints();
        LinesPlotState state = (LinesPlotState) getState();
        int ngraph = state.getGraphCount();

        /* Set up initial values for extrema. */
        double xlo = Double.MAX_VALUE;
        double xhi = - Double.MAX_VALUE;
        boolean xLog = state.getValid() && state.getLogFlags()[ 0 ];
        double[][] yRanges = new double[ ngraph ][ 2 ];
        boolean[] yLogs = state.getYLogFlags();
        for ( int i = 0; i < ngraph; i++ ) {
            yRanges[ i ] = new double[] {
                Double.MAX_VALUE,
                ( state.getValid() && yLogs[ i ] ) ? Double.MIN_VALUE
                                                   : - Double.MAX_VALUE
            };
        }

        /* Go through all the data finding extrema. */
        if ( state != null && state.getValid() && points != null ) {
            RowSubset[] sets = state.getPointSelection().getSubsets();
            int[] graphIndices = state.getGraphIndices();
            int nset = sets.length;
            int npoint = points.getCount();

            double[] coords = new double[ 2 ];
            for ( int ip = 0; ip < npoint; ip++ ) {
                points.getCoords( ip, coords );
                double x = coords[ 0 ];
                double y = coords[ 1 ];
                if ( ! Double.isNaN( x ) && ! Double.isInfinite( x ) &&
                     ! Double.isNaN( y ) && ! Double.isInfinite( y ) &&
                     ( ! xLog || x > 0.0 ) ) {
                    boolean isUsed = false;
                    for ( int iset = 0; iset < nset; iset++ ) {
                        if ( sets[ iset ].isIncluded( ip ) ) {
                            int igraph = graphIndices[ iset ];
                            boolean yLog = yLogs[ igraph ];
                            if ( ! yLog || y > 0.0 ) {
                                isUsed = true;
                                double[] yRange = yRanges[ igraph ];
                                if ( y < yRange[ 0 ] ) {
                                    yRange[ 0 ] = y;
                                }
                                if ( y > yRange[ 1 ] ) {
                                    yRange[ 1 ] = y;
                                }
                            }
                        }
                    }
                    if ( isUsed ) {
                        if ( x < xlo ) {
                            xlo = x;
                        }
                        if ( x > xhi ) {
                            xhi = x;
                        }
                    }
                }
            }
        }

        /* Set defaults in the case of no data. */
        double[] xRange = new double[] { xlo, xhi };
        if ( ! ( xRange[ 0 ] < xRange[ 1 ] ) ) {
            xRange[ 0 ] = xLog ? 0.1 : 0.0;
            xRange[ 1 ] = 1.0;
        }
        xRange_ = xRange;
        for ( int i = 0; i < yRanges.length; i++ ) {
            double[] yRange = yRanges[ i ];
            boolean yLog = state.getValid() && yLogs[ i ];
            if ( ! ( yRange[ 0 ] < yRange[ 1 ] ) ) {
                yRange[ 0 ] = yLog ? 0.1 : 0.0;
                yRange[ 1 ] = 1.0;
            }

            /* Add padding on Y axis. */
            if ( yLog ) {
                double pad = Math.exp( Math.log( yRange[ 1 ] / yRange[ 0 ] )
                                       * padRatio_ );
                yRange[ 0 ] /= pad;
                yRange[ 1 ] *= pad;
            }
            else {
                double pad = ( yRange[ 1 ] - yRange[ 0 ] ) * padRatio_;
                yRange[ 0 ] -= pad;
                yRange[ 1 ] += pad;
            }
        }
        yRanges_ = yRanges;
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
        boolean[] yFlipFlags = state.getYFlipFlags();

        /* Adjust data ranges as requested. */
        ValueInfo xInfo = state.getAxes()[ 0 ];
        ValueInfo[] yInfos = state.getYAxes();
        int ngraph = yInfos.length;
        double[] xRange = (double[]) xRange_.clone();
        double[][] yRanges = (double[][]) yRanges_.clone();
        double[] xAxisRange = state.getRanges()[ 0 ];
        double[][] yAxisRanges = state.getYRanges();
        for ( int i = 0; i < 2; i++ ) {
            if ( ! Double.isNaN( xAxisRange[ i ] ) ) {
                xRange[ i ] = xAxisRange[ i ];
            }
            for ( int igraph = 0; igraph < ngraph; igraph++ ) {
                if ( ! Double.isNaN( yAxisRanges[ igraph ][ i ] ) ) {
                    yRanges[ igraph ][ i ] = yAxisRanges[ igraph ][ i ];
                }
            }
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
        int yInc = ( height - border.bottom - border.top ) / ngraph;

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
        int xInc = width - border.left - border.right;
        AxisLabeller xAxis = new AxisLabeller( state.getAxisLabels()[ 0 ],
                                               xRange[ 0 ], xRange[ 1 ], xInc,
                                               state.getLogFlags()[ 0 ],
                                               state.getFlipFlags()[ 0 ],
                                               fm, AxisLabeller.X, 10 );

        /* Position each graph and draw the axes. */
        int xPos = border.left;
        int yPos = height - border.bottom - yInc;
        for ( int igraph = 0; igraph < ngraph; igraph++ ) {
            Graphics g1 = g.create();
            GraphSurface graph = graphs[ igraph ];
            graph.setBounds( new Rectangle( xPos, yPos, xInc, yInc ) );
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
            yPos -= yInc;
        }

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
