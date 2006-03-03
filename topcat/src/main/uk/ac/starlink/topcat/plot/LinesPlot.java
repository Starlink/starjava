package uk.ac.starlink.topcat.plot;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
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
        double[][] yRanges = new double[ ngraph ][ 2 ];
        for ( int i = 0; i < ngraph; i++ ) {
            yRanges[ i ] = 
                new double[] { Double.MAX_VALUE, - Double.MAX_VALUE };
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
                     ! Double.isNaN( y ) && ! Double.isInfinite( y ) ) {
                    if ( x < xlo ) {
                        xlo = x;
                    }
                    if ( x > xhi ) {
                        xhi = x;
                    }
                    for ( int iset = 0; iset < nset; iset++ ) {
                        if ( sets[ iset ].isIncluded( ip ) ) {
                            int igraph = graphIndices[ iset ];
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
            }
        }

        /* Set defaults in the case of no data. */
        xRange_ = xlo < xhi ? new double[] { xlo, xhi }
                            : new double[] { 0.0, 1.0 };
        for ( int i = 0; i < yRanges.length; i++ ) {
            double[] yRange = yRanges[ i ];
            if ( ! ( yRange[ 0 ] < yRange[ 1 ] ) ) {
                yRange[ 0 ] = 0.0;
                yRange[ 1 ] = 1.0;
            }

            /* Add padding on Y axis. */
            double pad = ( yRange[ 1 ] - yRange[ 0 ] ) * padRatio_;
            yRange[ 0 ] -= pad;
            yRange[ 1 ] += pad;
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

        /* Assemble an array of graphs to draw data on, and calculate 
         * the borders we need to leave for axis annotation. */
        GraphSurface[] graphs = new GraphSurface[ ngraph ];
        Insets border = new Insets( 0, 0, 0, 0 );
        for ( int igraph = 0; igraph < ngraph; igraph++ ) {
            GraphSurface graph = new GraphSurface( this, igraph );
            graphs[ igraph ] = graph;
            graph.setDataRange( xRange[ 0 ], yRanges[ igraph ][ 0 ],
                                xRange[ 1 ], yRanges[ igraph ][ 1 ] );
            graph.setLabelAxes( igraph == 0, true );
            Insets ai = graph.getAnnotationInsets();
            border.left = Math.max( border.left, ai.left );
            border.right = Math.max( border.right, ai.right );
        }
        border.bottom = graphs[ 0 ].getAnnotationInsets().bottom;
        border.top = graphs[ ngraph - 1 ].getAnnotationInsets().top;

        /* Position each graph and draw the axes. */
        int width = getSize().width;
        int height = getSize().height;
        Insets insets = getInsets();
        int xInc = width - insets.left - border.left
                         - border.right - insets.right;
        int xPos = insets.left + border.left;
        int yInc = ( height - insets.bottom - border.bottom 
                            - border.top - insets.top ) / ngraph;
        int yPos = height - insets.bottom - border.bottom - yInc;
        for ( int igraph = 0; igraph < ngraph; igraph++ ) {
            GraphSurface graph = graphs[ igraph ];
            graph.setBounds( new Rectangle( xPos, yPos, xInc, yInc ) );
            graph.setState( state );
            graph.paintSurface( g );
            yPos -= yInc;
        }

        /* Draw data points and lines. */
        RowSubset[] sets = state.getPointSelection().getSubsets();
        Style[] styles = state.getPointSelection().getStyles();
        int[] graphIndices = state.getGraphIndices();
        int nset = sets.length;
        double[] coords = new double[ 2 ];
        for ( int iset = 0; iset < nset; iset++ ) {
            Style style = styles[ iset ];
            Graphics lineGraphics = g.create();
            ((MarkStyle) style).configureForLine( lineGraphics,
                                                  BasicStroke.CAP_BUTT,
                                                  BasicStroke.JOIN_MITER );
            GraphSurface graph = graphs[ graphIndices[ iset ] ];
            lineGraphics.setClip( graph.getClip() );
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
                        if ( notFirst ) {
                            lineGraphics.drawLine( lastxp, lastyp, xp, yp );
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
