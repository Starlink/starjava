package uk.ac.starlink.topcat.plot;

import java.awt.Graphics;
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
            xlo = Math.min( xlo, bin.getLowBound() );
            xhi = Math.max( xhi, bin.getHighBound() );
            for ( int iset = 0; iset < nset; iset++ ) {
                int count = bin.getCount( iset );
                if ( count > 0 ) {
                    someData = true;
                    yhi = Math.max( yhi, count );
                }
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
        boolean xflip = state.getFlipFlags()[ 0 ];
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

    public double[] getIncludedXRange() {
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
