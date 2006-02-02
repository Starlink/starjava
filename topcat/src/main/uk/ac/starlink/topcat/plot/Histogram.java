package uk.ac.starlink.topcat.plot;

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

    public double[] getFullDataRange() {
        HistogramPlotState state = (HistogramPlotState) getState();
        boolean xlog = state.getLogFlags()[ 0 ];
        boolean cumulative = state.getCumulative();
        double xlo = Double.POSITIVE_INFINITY;
        double xhi = xlog ? Double.MIN_VALUE : Double.NEGATIVE_INFINITY;
        int nset = getPointSelection().getSubsets().length;
        int[] yhis = new int[ nset ];

        boolean someData = false;
        for ( Iterator it = getBinnedData().getBinIterator( false );
              it.hasNext(); ) {
            BinnedData.Bin bin = (BinnedData.Bin) it.next();
            boolean included = false;
            for ( int iset = 0; iset < nset; iset++ ) {
                int count = bin.getCount( iset );
                if ( count > 0 ) {
                    included = true;
                    yhis[ iset ] = cumulative ? yhis[ iset ] + count
                                              : Math.max( yhis[ iset ], count );
                }
            }
            if ( included ) {
                someData = true;
                xlo = Math.min( xlo, bin.getLowBound() );
                xhi = Math.max( xhi, bin.getHighBound() );
            }
        }

        int yhi = 0;
        for ( int iset = 0; iset < nset; iset++ ) {
            yhi = Math.max( yhi, yhis[ iset ] );
        }

        return someData ? new double[] { xlo, 0.0, xhi, (double) yhi }
                        : null;
    }

    /**
     * Lazily populate a BinnedData object describing the bins.
     *
     * @return  binned data
     */
    private BinnedData getBinnedData() {
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
                int count = bin.getCount( iset );
                double dcount = cumulative ? (double) ( total + count )
                                           : (double) count;
                total += count;
                if ( dcount <= 0 ) {
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
                int iyhi = surface.dataToGraphics( dxmid, dcount, false ).y;

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
        double[] coords = new double[ 1 ];
        BitSet mask = new BitSet();
        for ( int ip = 0; ip < np; ip++ ) {
            points.getCoords( ip, coords );
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

    private double[] getVisibleYRange() {
        HistogramPlotState state = (HistogramPlotState) getState();
        boolean xflip = state.getFlipFlags()[ 0 ];
        boolean cumulative = state.getCumulative();
        double[] bounds = getSurfaceBounds();
        double xbot = bounds[ 0 ];
        double xtop = bounds[ 2 ];
        int nset = getPointSelection().getSubsets().length;
        boolean someData = false;
        double[] ybots = new double[ nset ];
        double[] ytops = new double[ nset ];
        long[] counts = new long[ nset ];
        for ( int iset = 0; iset < nset; iset++ ) {
            ybots[ iset ] = Double.MAX_VALUE;
            ytops[ iset ] = 0.0;
        }
        for ( Iterator it = getBinnedData().getBinIterator( false );
              it.hasNext(); ) {
            BinnedData.Bin bin = (BinnedData.Bin) it.next();
            double xlo = bin.getLowBound();
            double xhi = bin.getHighBound();
            if ( xlo >= xbot && xhi <= xtop ) {
                for ( int iset = 0; iset < nset; iset++ ) {
                    int count = bin.getCount( iset );
                    counts[ iset ] = cumulative ? counts[ iset ] + count
                                                : count;
                    if ( counts[ iset ] > 0 ) {
                        someData = true;
                        ytops[ iset ] = Math.max( ytops[ iset ],
                                                  counts[ iset ] );
                        ybots[ iset ] = Math.min( ybots[ iset ],
                                                  counts[ iset ] );
                    }
                }
            }
        }
        if ( someData ) {
            double ybot = Double.MAX_VALUE;
            double ytop = 0.0;
            for ( int iset = 0; iset < nset; iset++ ) {
                ybot = Math.min( ybot, ybots[ iset ] );
                ytop = Math.max( ytop, ytops[ iset ] );
            }
            return new double[] { ybot, ytop };
        }
        else {
            return new double[] { Double.NaN, Double.NaN };
        }
    }

    /**
     * Requests a rescale in the X and/or Y direction to fit the current data.
     *
     * @param   scaleX   whether to scale in X direction
     * @param   scaleY   whether to scale in Y direction
     */
    public void rescale( boolean scaleX, boolean scaleY ) {
        if ( scaleX && scaleY ) {
            rescale();
        }
        else if ( scaleX ) {
            double[] xrange =
                HistogramWindow.getXRange( getState(), getPoints() );
            getSurface().setDataRange( xrange[ 0 ], Double.NaN,
                                       xrange[ 1 ], Double.NaN );
        }
        else if ( scaleY ) {
            double[] yrange = getVisibleYRange();
            getSurface().setDataRange( Double.NaN, 0.0,
                                       Double.NaN, yrange[ 1 ] );
        }
        else {
            assert false : "Not much of a rescale";
        }
    }

    public void scaleYFactor( double factor ) {
        double[] bounds = getSurfaceBounds();
        if ( ! getState().getLogFlags()[ 1 ] ) {
            getSurface().setDataRange( Double.NaN, bounds[ 1 ] * factor,
                                       Double.NaN, bounds[ 3 ] * factor );
        }
        else {
            getSurface().setDataRange( Double.NaN, bounds[ 1 ],
                                       Double.NaN, bounds[ 3 ] + factor );
        }
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
        double[] coords = new double[ 1 ];
        boolean[] setFlags = new boolean[ nset ];
        for ( int ip = 0; ip < np; ip++ ) {
            long lp = (long) ip;
            points.getCoords( ip, coords );
            double x = coords[ 0 ];
            if ( ! Double.isNaN( x ) && ! Double.isInfinite( x ) ) {
                for ( int is = 0; is < nset; is++ ) {
                    setFlags[ is ] = rsets[ is ].isIncluded( lp );
                }
                binned.submitDatum( x, setFlags );
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
            drawData( g );
        }
    }

}
