package uk.ac.starlink.ttools.plot2;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;

/**
 * Ganger implementation for single-zone gangs.
 *
 * @author   Mark Taylor
 * @since    25 Jan 2016
 */
public class SingleGanger<P,A> implements Ganger<P,A> {

    public Gang createGang( Rectangle[] zonePlotBounds ) {
        if ( zonePlotBounds.length == 1 ) {
            return new SingleGang( zonePlotBounds[ 0 ] );
        }
        else {
            throw new IllegalArgumentException( "Zone count not 1" );
        }
    }

    public Gang createGang( Rectangle extBounds,
                            SurfaceFactory<P,A> surfFact, int nz,
                            ZoneContent[] contents,
                            P[] profiles, A[] aspects,
                            ShadeAxis[] shadeAxes, boolean withScroll ) {
        if ( nz != 1 ) {
            throw new IllegalArgumentException( "Not single zone" );
        }
        ZoneContent content = contents[ 0 ];
        Rectangle plotBounds =
            PlotPlacement
           .calculateDataBounds( extBounds, surfFact,
                                 profiles[ 0 ], aspects[ 0 ], withScroll,
                                 content.getLegend(),
                                 content.getLegendPosition(),
                                 content.getTitle(), shadeAxes[ 0 ] );
        return new SingleGang( plotBounds );
    }

    public Gang createApproxGang( Rectangle extBounds, int nz ) {
        if ( nz == 1 ) {
            return new SingleGang( extBounds );
        }
        else {
            throw new IllegalArgumentException( "Not single zone" );
        }
    }

    public A[] adjustAspects( A[] oldAspects, int iz ) {
        return oldAspects;
    }

    public P[] adjustProfiles( P[] oldProfiles ) {
        return oldProfiles;
    }

    /**
     * Single-zone Gang implementation.
     */
    private static class SingleGang implements Gang {

        private final Rectangle plotBounds_;

        /**
         * Constructor.
         *
         * @param  plotBounds  data bounds for sole zone
         */
        SingleGang( Rectangle plotBounds ) {
            plotBounds_ = new Rectangle( plotBounds );
        }

        public int getZoneCount() {
            return 1;
        }

        public Rectangle getGangPlotBounds() {
            return plotBounds_;
        }

        public Rectangle getZonePlotBounds( int iz ) {
            if ( iz == 0 ) {
                return plotBounds_;
            }
            else {
                throw new IllegalArgumentException();
            }
        }

        public int getNavigationZoneIndex( Point pos ) {
            return 0;
        }
    }
}
