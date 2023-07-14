package uk.ac.starlink.ttools.plot2;

import java.awt.Insets;
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

    private final Padding padding_;

    /**
     * Constructs a ganger with no specified padding.
     */
    public SingleGanger() {
        this( null );
    }

    /**
     * Constructs a ganger with specified padding.
     *
     * @param  padding   defines user preferences, if any, for space
     *                   reserved for annotations
     */
    public SingleGanger( Padding padding ) {
        padding_ = padding == null ? new Padding() : padding;
    }

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
                            ZoneContent<P,A>[] contents, Trimming[] trimmings,
                            ShadeAxis[] shadeAxes, boolean withScroll ) {
        if ( nz != 1 ) {
            throw new IllegalArgumentException( "Not single zone" );
        }
        ZoneContent<P,A> content = contents[ 0 ];
        Rectangle plotBounds =
            PlotPlacement
           .calculateDataBounds( extBounds, padding_, surfFact,
                                 content.getProfile(), content.getAspect(),
                                 withScroll, trimmings[ 0 ], shadeAxes[ 0 ] );
        return new SingleGang( plotBounds );
    }

    public Gang createApproxGang( Rectangle extBounds, int nz ) {
        if ( nz == 1 ) {
            Insets insets = padding_.overrideInsets( new Insets( 0, 0, 0, 0 ) );
            Rectangle plotBounds = PlotUtil.subtractInsets( extBounds, insets );
            return new SingleGang( plotBounds );
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
     * Returns a factory that creates SingleGangers.
     *
     * @param  plotType  plot type, only used for generic typing
     * @return  ganger factory
     */
    public static <P,A> GangerFactory<P,A>
            createFactory( PlotType<P,A> plotType ) {
        return new GangerFactory<P,A>() {
            public boolean isMultiZone() {
                return false;
            }
            public Ganger<P,A> createGanger( Padding padding ) {
                return new SingleGanger<P,A>( padding );
            }
        };
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
