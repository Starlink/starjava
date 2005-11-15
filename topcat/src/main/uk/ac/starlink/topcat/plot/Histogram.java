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

    private static final double LOG10 = Math.log( 1e1 );

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
        boolean xlog = getState().getLogFlags()[ 0 ];
        double xlo = Double.POSITIVE_INFINITY;
        double xhi = xlog ? Double.MIN_VALUE : Double.NEGATIVE_INFINITY;
        int yhi = 0;

        int nset = getPointSelection().getSubsets().length;
        
        boolean someData = false;
        for ( Iterator it = getBinnedData().getBinIterator(); it.hasNext(); ) {
            BinnedData.Bin bin = (BinnedData.Bin) it.next();
            boolean included = false;
            for ( int iset = 0; iset < nset; iset++ ) {
                int count = bin.getCount( iset );
                if ( count > 0 ) {
                    included = true;
                    yhi = Math.max( yhi, count );
                }
            }
            if ( included ) {
                someData = true;
                xlo = Math.min( xlo, bin.getLowBound() );
                xhi = Math.max( xhi, bin.getHighBound() );
            }
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
        PlotState state = getState();
        PlotSurface surface = getSurface();
        if ( points == null || state == null || surface == null ||
             ! state.getValid() ) {
            return;
        }

        /* Clone the context. */
        g = g.create();

        /* Clip it to the bounds of the drawable part of the surface. */
        g.setClip( getSurface().getClip() );

        /* Get the plotting styles to use. */
        boolean xflip = state.getFlipFlags()[ 0 ];
        int nset = getPointSelection().getSubsets().length;
        MarkStyle[] styles = getPointSelection().getStyles();

        /* Draw bars. */
        for ( Iterator it = getBinnedData().getBinIterator(); it.hasNext(); ) {

            /* Work out the X bounds of the rectangle. */
            BinnedData.Bin bin = (BinnedData.Bin) it.next();
            double dxlo = bin.getLowBound();
            double dxhi = bin.getHighBound();
            double dxmid = dxlo + 0.5 * dxhi;
            int ixlo =
                surface.dataToGraphics( xflip ? dxhi : dxlo, 0.0, false ).x;
            int ixhi =
                surface.dataToGraphics( xflip ? dxlo : dxhi, 0.0, false ).x;
            int iylo = surface.dataToGraphics( dxmid, 0.0, false ).y;

            /* For each subset work out the height of the bar. */
            for ( int iset = 0; iset < nset; iset++ ) {
                int count = bin.getCount( iset );
                int iyhi =
                    surface.dataToGraphics( dxmid, (double) count, false ).y;

                /* Plot in the correct style. */
                g.setColor( styles[ iset ].getColor() );
                g.drawRect( ixlo, iyhi, ixhi - ixlo - 1, iylo - iyhi );
            }
        }
    }

    private double[] getIncludedXRange() {
        boolean xlog = getState().getLogFlags()[ 0 ];
        double xlo = Double.POSITIVE_INFINITY;
        double xhi = xlog ? Double.MIN_VALUE : Double.NEGATIVE_INFINITY;

        int nok = 0;
        Points points = getPoints();
        int np = points.getCount();
        RowSubset[] rsets = getPointSelection().getSubsets();
        int nset = rsets.length;
        double[] coords = new double[ 1 ];
        for ( int ip = 0; ip < np; ip++ ) {
            long lp = (long) ip;
            boolean use = false;
            for ( int is = 0; is < nset; is++ ) {
                if ( rsets[ is ].isIncluded( lp ) ) {
                    use = true;
                    break;
                }
            }
            if ( use ) {
                points.getCoords( ip, coords );
                double xp = coords[ 0 ];
                if ( ! Double.isNaN( xp ) && ! Double.isInfinite( xp ) &&
                     ( ! xlog || xp > 0.0 ) ) {
                    nok++;
                    if ( xp < xlo ) {
                        xlo = xp;
                    }
                    if ( xp > xhi ) {
                        xhi = xp;
                    }
                }
            }
        }
        return nok > 0 ? new double[] { xlo, xhi }
                       : new double[] { 0.0, 1.0 };
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
        for ( Iterator it = getBinnedData().getBinIterator(); it.hasNext(); ) {
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
        return mask;
    }

    private double[] getVisibleYRange() {
        double[] bounds = getSurfaceBounds();
        boolean xflip = getState().getFlipFlags()[ 0 ];
        double xbot = bounds[ 0 ];
        double xtop = bounds[ 2 ];
        int nset = getPointSelection().getSubsets().length;
        boolean someData = false;
        double ybot = Double.MAX_VALUE;
        double ytop = Double.MIN_VALUE;
        for ( Iterator it = getBinnedData().getBinIterator(); it.hasNext(); ) {
            BinnedData.Bin bin = (BinnedData.Bin) it.next();
            double xlo = bin.getLowBound();
            double xhi = bin.getHighBound();
            if ( xlo >= xbot && xhi <= xtop ) {
                for ( int iset = 0; iset < nset; iset++ ) {
                    int count = bin.getCount( iset );
                    if ( count > 0 ) {
                        someData = true;
                        ytop = Math.max( ytop, count );
                        ybot = Math.min( ybot, count );
                    }
                }
            }
        }
        return someData ? new double[] { ybot, ytop }
                        : new double[] { Double.NaN, Double.NaN };
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
            double[] xrange = getIncludedXRange();
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
            for ( int is = 0; is < nset; is++ ) {
                setFlags[ is ] = rsets[ is ].isIncluded( lp );
            }
            binned.submitDatum( x, setFlags );
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
        double[] xrange = getIncludedXRange();
        boolean log = getState().getLogFlags()[ 0 ];
        if ( log ) {
            double factor =
                Math.exp( Math.log( xrange[ 1 ] / xrange[ 0 ] ) / 20 );
            return MapBinnedData.createLogBinnedData( nset, factor );
        }
        else {
            double width = roundNumber( ( xrange[ 1 ] - xrange[ 0 ] ) / 20 );
            return MapBinnedData.createLinearBinnedData( nset, width );
        }
    }

    /**
     * Returns a round number near to a given value.
     *
     * @param   num   input
     * @return  output
     */
    private static double roundNumber( double num ) {
        double exponent = Math.floor( Math.log( num ) / LOG10 );
        double multiplier = Math.pow( 10, exponent );
        double mantissa = num / multiplier;
        assert mantissa >= 0.999 && mantissa <= 10.001
             : mantissa + " * " + multiplier + " = " + num;
        double roundedMantissa;
        if ( mantissa < 1.5 ) {
            roundedMantissa = 1;
        }
        else if ( mantissa < 2.2 ) {
            roundedMantissa = 2;
        }
        else if ( mantissa < 3.5 ) {
            roundedMantissa = 2.5;
        }
        else if ( mantissa < 7.5 ) {
            roundedMantissa = 5;
        }
        else {
            roundedMantissa = 10;
        }
        return roundedMantissa * multiplier;
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
