package uk.ac.starlink.ttools.plot;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.BitSet;
import java.util.Iterator;
import javax.swing.JComponent;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.ValueInfo;

/**
 * Component which draws a histogram.
 *
 * @author   Mark Taylor
 * @since    11 Nov 2005
 */
public class Histogram extends SurfacePlot {

    private BinnedData binned_;
    private PlotData lastData_;

    /**
     * Constructs a new Histogram.
     *
     * @param  surface  the surface on which plotting will take place
     */
    @SuppressWarnings("this-escape")
    public Histogram( PlotSurface surface ) {
        super();
        add( new HistogramDataPanel() );
        setSurface( surface );
    }

    public void setState( PlotState state ) {
        super.setState( state );
        PlotData data = state.getPlotData();

        /* It would be possible to improve efficiency for subsequent plots
         * here by arranging that the binning didn't get done every time.
         * This is not currently done however. */
        if ( data != lastData_ ) {
            binned_ = null;
            lastData_ = data;
        }
    }

    /**
     * Lazily populate a BinnedData object describing the bins.
     *
     * @return  binned data
     */
    public BinnedData getBinnedData() {
        if ( binned_ == null ) {
            binned_ = binData( (HistogramPlotState) getState() );
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
        HistogramPlotState state = (HistogramPlotState) getState();
        if ( state == null || ! state.getValid() ) {
            return;
        }
        PlotData data = state.getPlotData();
        BinnedData binnedData = getBinnedData();
        PlotSurface surface = getSurface();
        if ( data == null || binnedData == null || surface == null ) {
            return;
        }

        /* Clone the context. */
        g = g.create();

        /* Clip it to the bounds of the drawable part of the surface. */
        Rectangle clip =
            g.getClipBounds()
             .createIntersection( getSurface().getClip().getBounds() )
             .getBounds();
        g.setClip( clip );

        /* Get the plotting styles to use. */
        boolean xflip = state.getFlipFlags()[ 0 ];
        boolean cumulative = state.getCumulative();
        double dylo = state.getLogFlags()[ 1 ] ? Double.MIN_VALUE : 0.0;
        int nset = data.getSetCount();

        /* Work out the y position in graphics space of the plot base line. */
        int iylo = surface.dataToGraphics( 1.0, dylo, false ).y;

        /* Draw bars.  The loops are this way round so that later subsets
         * get plotted in their entirety after earlier ones, which 
         * (for some plotting styles) is necessary for the correct visual
         * effect.  Although this way round is probably less efficient
         * than the other way round, the actual plot is unlikely to be
         * a computational bottleneck. */
        for ( int iset = 0; iset < nset; iset++ ) {
            BarStyle style = (BarStyle) data.getSetStyle( iset );
            int lastIxLead = xflip ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            int lastIyhi = 0;
            double total = 0;
            for ( Iterator<BinnedData.Bin> it =
                      binnedData.getBinIterator( cumulative );
                  it.hasNext(); ) {

                /* Get the bin and its value. */
                BinnedData.Bin bin = it.next();
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

                /* Clip these explicitly.  This shouldn't be necessary since
                 * the clip has been applied to the graphics context already,
                 * but it seems it can sometimes cause problems (AWT lockup)
                 * if drawing is attempted with very large coordinate values.
                 * Perhaps some graphics context implementations are not
                 * observing the clip. */
                ixlo = Math.max( ixlo, clip.x - 64 );
                ixhi = Math.min( ixhi, clip.x + clip.width + 64 );
                iylo = Math.max( iylo, clip.y - 64 );
                iyhi = Math.min( iyhi, clip.y + clip.height + 64 );

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

    public DataBounds calculateBounds( PlotData data, PlotState state ) {
        DataBounds plotBounds = super.calculateBounds( data, state );
        int np = plotBounds.getPointCount();
        int[] npoints = plotBounds.getPointCounts();
        Range xRange = plotBounds.getRanges()[ 0 ];
        double[] xBounds = xRange.getFiniteBounds( state.getLogFlags()[ 0 ] );
        return new DataBounds( new Range[] { xRange }, np, npoints );
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
        BinnedData binnedData = getBinnedData();
        if ( binnedData == null ) {
            return new BitSet();
        }
        double[] bounds = getSurfaceBounds();
        double xbot = bounds[ 0 ];
        double xtop = bounds[ 2 ];
        for ( Iterator<BinnedData.Bin> it = binnedData.getBinIterator( false );
              it.hasNext(); ) {
            BinnedData.Bin bin = it.next();
            if ( bin.getLowBound() < xbot ) {
                xbot = Math.max( xbot, bin.getHighBound() );
            }
            if ( bin.getHighBound() > xtop ) {
                xtop = Math.min( xtop, bin.getLowBound() );
            }
        }

        PlotData data = getState().getPlotData();
        int nset = data.getSetCount();
        BitSet mask = new BitSet();
        PointSequence pseq = data.getPointSequence();
        for ( int ip = 0; pseq.next(); ip++ ) {
            double[] coords = pseq.getPoint();
            double x = coords[ 0 ];
            if ( ! Double.isNaN( x ) && ! Double.isInfinite( x ) ) {
                if ( x >= xbot && x <= xtop ) {
                    for ( int is = 0; is < nset; is++ ) {
                        if ( pseq.isIncluded( is ) ) {
                            mask.set( ip );
                            break;
                        }
                    }
                }
            }
        }
        pseq.close();
        return mask;
    }

    /**
     * Builds a BinnedData object based on a given set of data points.
     * As a special case, null is returned if the supplied data set contains
     * no points.
     *
     * @param   data   plot state object
     * @return   binned data object
     */
    private BinnedData binData( HistogramPlotState state ) {
        PlotData data = state.getPlotData();

        /* Acquire an object to hold the data. */
        int nset = data.getSetCount();
        BinnedData binned = createBinnedData( nset );
        if ( state.getNormalised() ) {
            binned = new NormalisedBinnedData( binned );
        }

        /* Populate it. */
        boolean[] setFlags = new boolean[ nset ];
        PointSequence pseq = data.getPointSequence();
        int ip = 0;
        for ( ; pseq.next(); ip++ ) {
            double[] coords = pseq.getPoint();
            double x = coords[ 0 ];
            double w = coords[ 1 ];
            if ( ! Double.isNaN( x ) && ! Double.isInfinite( x ) &&
                 ! Double.isNaN( w ) && ! Double.isInfinite( w ) ) {
                for ( int is = 0; is < nset; is++ ) {
                    setFlags[ is ] = pseq.isIncluded( is );
                }
                binned.submitDatum( x, w, setFlags );
            }
        }
        return ip > 0 ? binned : null;
    }

    /**
     * Factory method to provide an empty BinnedData object ready for 
     * population.
     *
     * @param   nset  number of subsets to be stored in it
     * @return   empty binned data object
     */
    private BinnedData createBinnedData( int nset ) {
        HistogramPlotState state = (HistogramPlotState) getState();
        return MapBinnedData.createBinMapper( state.getLogFlags()[ 0 ],
                                              state.getBinWidth(),
                                              state.getBinBase() )
                            .createBinnedData( nset );
    }

    /**
     * Returns a metadata object describing the values on the vertical axis.
     * 
     * @param  isWeighted  whether the histogram uses weighted counts
     * @param  isNormalised  whether the histogram values are normalised to 1
     * @return  metadata object
     */
    public static ValueInfo getYInfo( boolean isWeighted,
                                      boolean isNormalised ) {
        final String name;
        final String descrip;
        if ( isWeighted ) {
            if ( isNormalised ) {
                name = "Normalised weighted count";
                descrip = "Normalised weighted sum of values";
            }
            else {
                name = "Weighted count";
                descrip = "Weighted sum of values";
            }
        }
        else {
            if ( isNormalised ) {
                name = "Normalised count";
                descrip = "Normalised sum of values";
            }
            else {
                name = "Count";
                descrip = "Number of values";
            }
        }
        final boolean isInt = ( ! isWeighted ) && ( ! isNormalised );
        return new DefaultValueInfo( name, isInt ? Integer.class : Double.class,
                                     descrip + " in bin" );
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
