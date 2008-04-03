package uk.ac.starlink.tplot;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.BitSet;
import java.util.Iterator;
import javax.swing.JComponent;
import uk.ac.starlink.topcat.RowSubset;

/**
 * Component which draws a histogram.
 *
 * @author   Mark Taylor
 * @since    11 Nov 2005
 */
public class Histogram extends SurfacePlot {

    private BinnedData binned_;
    private Points lastPoints_;
    private PointSelection lastPointSelection_;

    /**
     * Constructs a new Histogram.
     *
     * @param  surface  the surface on which plotting will take place
     */
    public Histogram( PlotSurface surface ) {
        super();
        add( new HistogramDataPanel() );
        setSurface( surface );
    }

    public void setPoints( Points points ) {
        super.setPoints( points );
        if ( points != lastPoints_ ) {
            binned_ = null;
            lastPoints_ = points;
        }
    }

    public void setState( PlotState state ) {
        super.setState( state );
        if ( state.getPointSelection() != lastPointSelection_ ) {
            binned_ = null;
            lastPointSelection_ = state.getPointSelection();
        }
    }

    /**
     * Lazily populate a BinnedData object describing the bins.
     *
     * @return  binned data
     */
    public BinnedData getBinnedData() {
        if ( binned_ == null ) {
            binned_ = binData( getPoints() );
        }
        return binned_;
    }

    /**
     * Does the actual drawing of bars.
     *
     * @param   g  graphics context
     */
    private void drawData( Graphics g ) {

        /* Get and check relevant state. */
        Points points = getPoints();
        HistogramPlotState state = (HistogramPlotState) getState();
        PlotSurface surface = getSurface();
        if ( points == null || state == null || surface == null ||
             ! state.getValid() ) {
            return;
        }

        /* Clone the context. */
        g = g.create();

        /* Clip it to the bounds of the drawable part of the surface. */
        g.setClip( g.getClip().getBounds()
                  .createIntersection( getSurface().getClip().getBounds() ) );

        /* Get the plotting styles to use. */
        boolean xflip = state.getFlipFlags()[ 0 ];
        boolean cumulative = state.getCumulative();
        double dylo = state.getLogFlags()[ 1 ] ? Double.MIN_VALUE : 0.0;
        int nset = getPointSelection().getSubsets().length;
        Style[] styles = getPointSelection().getStyles();

        /* Work out the y position in graphics space of the plot base line. */
        int iylo = surface.dataToGraphics( 1.0, dylo, false ).y;

        /* Draw bars.  The loops are this way round so that later subsets
         * get plotted in their entirety after earlier ones, which 
         * (for some plotting styles) is necessary for the correct visual
         * effect.  Although this way round is probably less efficient
         * than the other way round, the actual plot is unlikely to be
         * a computational bottleneck. */
        for ( int iset = 0; iset < nset; iset++ ) {
            BarStyle style = (BarStyle) styles[ iset ];
            int lastIxLead = xflip ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            int lastIyhi = 0;
            long total = 0;
            for ( Iterator it = getBinnedData().getBinIterator( cumulative );
                  it.hasNext(); ) {

                /* Get the bin and its value. */
                BinnedData.Bin bin = (BinnedData.Bin) it.next();
                double sum = bin.getWeightedCount( iset );
                double tsum = cumulative ? total + sum
                                         : sum;
                total += sum;
                if ( tsum <= 0 ) {
                    continue;
                }

                /* Work out the bar geometry. */
                double dxlo = bin.getLowBound();
                double dxhi = bin.getHighBound();
                double dxmid = dxlo + 0.5 * dxhi;
                int ixlo = surface.dataToGraphics( xflip ? dxhi : dxlo,
                                                   dylo, false ).x;
                int ixhi = surface.dataToGraphics( xflip ? dxlo : dxhi,
                                                   dylo, false ).x;
                int iyhi = surface.dataToGraphics( dxmid, tsum, false ).y;

                /* Draw the trailing edge of the last bar if necessary. */
                if ( lastIxLead != ( xflip ? ixhi : ixlo ) ) {
                    style.drawEdge( g, lastIxLead, lastIyhi, iylo, iset, nset );
                    lastIyhi = iylo;
                }

                /* Draw the leading edge of the current bar. */
                style.drawEdge( g, xflip ? ixhi : ixlo, lastIyhi, iyhi,
                                iset, nset );
                lastIxLead = xflip ? ixlo : ixhi;
                lastIyhi = iyhi;

                /* Plot the bar. */
                style.drawBar( g, ixlo, ixhi, iyhi, iylo, iset, nset );
            }

            /* Draw trailing edge of the final bar. */
            style.drawEdge( g, lastIxLead, lastIyhi, iylo, iset, nset );
        }

        /* Notify listeners that the plot has changed.  We haven't counted
         * statistics, so use dummy values. */
        firePlotChangedLater( new PlotEvent( this, state, -1, -1, -1 ) );
    }

    /**
     * Returns the bounds of the plotting window in data space.
     * The first two elements of the results are the numerically lower bounds
     * and the second two are the numerically upper bounds.
     * This is true even if axes are flipped.
     *
     * @return  4-element array (xlo,ylo,xhi,yhi)
     */
    private double[] getSurfaceBounds() {
        PlotSurface surface = getSurface();
        Rectangle container = surface.getClip().getBounds();
        double[] lopt =
            surface.graphicsToData( container.x, container.y + container.height,
                                    false );
        double[] hipt =
            surface.graphicsToData( container.x + container.width, container.y,
                                    false );
        double[] bounds = 
            new double[] { lopt[ 0 ], lopt[ 1 ], hipt[ 0 ], hipt[ 1 ] };
        if ( getState().getFlipFlags()[ 0 ] ) {
            assert bounds[ 2 ] <= bounds[ 0 ];
            bounds = new double[] { bounds[ 2 ], bounds[ 1 ],
                                    bounds[ 0 ], bounds[ 3 ] };
        }
        return bounds;
    }

    /**
     * Returns a bit vector describing which of the points in the Points object
     * most recently plotted by this histogram are covered by the currently
     * visible data range.
     *
     * @return   bit set indexing Points
     */
    public BitSet getVisiblePoints() {
        double[] bounds = getSurfaceBounds();
        double xbot = bounds[ 0 ];
        double xtop = bounds[ 2 ];
        for ( Iterator it = getBinnedData().getBinIterator( false );
              it.hasNext(); ) {
            BinnedData.Bin bin = (BinnedData.Bin) it.next();
            if ( bin.getLowBound() < xbot ) {
                xbot = Math.max( xbot, bin.getHighBound() );
            }
            if ( bin.getHighBound() > xtop ) {
                xtop = Math.min( xtop, bin.getLowBound() );
            }
        }

        RowSubset[] rsets = getPointSelection().getSubsets();
        int nset = rsets.length;
        Points points = getPoints();
        int np = points.getCount();
        BitSet mask = new BitSet();
        for ( int ip = 0; ip < np; ip++ ) {
            double[] coords = points.getPoint( ip );
            double x = coords[ 0 ];
            if ( ! Double.isNaN( x ) && ! Double.isInfinite( x ) ) {
                if ( x >= xbot && x <= xtop ) {
                    long lp = (long) ip;
                    for ( int is = 0; is < nset; is++ ) {
                        if ( rsets[ is ].isIncluded( lp ) ) {
                            mask.set( ip );
                            break;
                        }
                    }
                }
            }
        }
        return mask;
    }

    /**
     * Builds a BinnedData object based on a given set of data points.
     *
     * @param   points  data points
     * @return   binned data object
     */
    private BinnedData binData( Points points ) {

        /* Acquire an object to hold the data. */
        RowSubset[] rsets = getPointSelection().getSubsets();
        int nset = rsets.length;
        BinnedData binned = newBinnedData( nset );

        /* Populate it. */
        int np = points.getCount();
        boolean[] setFlags = new boolean[ nset ];
        for ( int ip = 0; ip < np; ip++ ) {
            long lp = (long) ip;
            double[] coords = points.getPoint( ip );
            double x = coords[ 0 ];
            double w = coords[ 1 ];
            if ( ! Double.isNaN( x ) && ! Double.isInfinite( x ) ) {
                for ( int is = 0; is < nset; is++ ) {
                    setFlags[ is ] = rsets[ is ].isIncluded( lp );
                }
                binned.submitDatum( x, w, setFlags );
            }
        }
        return binned;
    }

    /**
     * Factory method to provide an empty BinnedData object ready for 
     * population.
     *
     * @param   nset  number of subsets to be stored in it
     * @return   empty binned data object
     */
    private BinnedData newBinnedData( int nset ) {
        HistogramPlotState state = (HistogramPlotState) getState();
        boolean log = state.getLogFlags()[ 0 ];
        double binWidth = state.getBinWidth();
        return state.getLogFlags()[ 0 ]
             ? MapBinnedData.createLogBinnedData( nset, binWidth )
             : MapBinnedData.createLinearBinnedData( nset, binWidth,
                                                     state.getZeroMid() );
    }

    /**
     * Component class which holds the plotted bars (but not the axes
     * etc) themselves.
     */
    private class HistogramDataPanel extends JComponent {
        HistogramDataPanel() {
            setOpaque( false );
        }
        protected void paintComponent( Graphics g ) {
            if ( isOpaque() ) {
                Color color = g.getColor();
                g.setColor( getBackground() );
                g.fillRect( 0, 0, getWidth(), getHeight() );
                g.setColor( color );
            }
            drawData( g );
        }
    }

}
