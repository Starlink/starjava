package uk.ac.starlink.topcat.plot;

import java.awt.Rectangle;
import uk.ac.starlink.ttools.plot.AuxLegend;

/**
 * ZoomRegion for use with an {@link uk.ac.starlink.ttools.plot.AuxLegend}.
 *
 * @author   Mark Taylor
 * @since    2 Apr 2008
 */
public abstract class AuxLegendZoomRegion extends AxisZoomRegion {

    private final AuxLegend legend_;

    /**
     * Constructor.
     *
     * @param  legend  lagend to zoom over
     */
    public AuxLegendZoomRegion( AuxLegend legend ) {
        super( legend.isHorizontal() );
        legend_ = legend;
    }

    /**
     * Called when a zoom has taken place.
     *
     * @param  lo  new requested lower data bound
     * @param  hi  new requested upper data bound
     */
    protected abstract void dataZoomed( double lo, double hi );

    public Rectangle getDisplay() {
        return legend_.getDataBounds();
    }

    public Rectangle getTarget() {
        return legend_.getDataBounds();
    }

    /**
     * @throws  UnsupportedOperationException
     */
    public void setDisplay( Rectangle display ) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws  UnsupportedOperationException
     */
    public void setTarget( Rectangle target ) {
        throw new UnsupportedOperationException();
    }

    public void zoomed( double[][] bounds ) {
        double b0 = legend_.fractionToData( bounds[ 0 ][ 0 ] );
        double b1 = legend_.fractionToData( bounds[ 0 ][ 1 ] );
        if ( b0 <= b1 ) {
            dataZoomed( b0, b1 );
        }
        else {
            dataZoomed( b1, b0 );
        }
    }
}
