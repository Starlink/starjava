package uk.ac.starlink.ttools.plot2;

import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;

/**
 * GangerFactory implementation for single-zone gangs.
 *
 * @author   Mark Taylor
 * @since    25 Jan 2016
 */
public class SingleGangerFactory<P,A> implements GangerFactory<P,A> {

    /** Sole instance.  @see {@link #instance}. */
    @SuppressWarnings("rawtypes")
    private static final SingleGangerFactory INSTANCE =
        new SingleGangerFactory<>();

    /**
     * Private sole constructor prevents external instantiation of
     * singleton class.
     */
    private SingleGangerFactory() {
    }

    public boolean hasIndependentZones() {
        return false;
    }

    public ConfigKey<?>[] getGangerKeys() {
        return new ConfigKey<?>[ 0 ];
    }

    public Ganger<P,A> createGanger( Padding padding, ConfigMap config,
                                     GangContext context ) {
        return new SingleGanger<P,A>( padding );
    }

    /**
     * Returns the sole instance of this class.
     *
     * @return  factory instance
     */
    @SuppressWarnings("unchecked")
    public static <P,A> SingleGangerFactory<P,A> instance() {
        return (SingleGangerFactory<P,A>) INSTANCE;
    }

    /**
     * Returns a single-zone ganger with specified padding.
     *
     * @param  padding   padding, may be null
     * @return  new ganger
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public static <P,A> Ganger<P,A> createGanger( Padding padding ) {
        return (Ganger<P,A>) new SingleGanger( padding );
    }

    /**
     * Returns a single-zone ganger with default padding.
     *
     * @return  new ganger
     */
    public static <P,A> Ganger<P,A> createGanger() {
        return createGanger( null );
    }

    /**
     * Ganger implementation for use with this factory.
     */
    private static class SingleGanger<P,A> implements Ganger<P,A> {

        private final Padding padding_;

        /**
         * Constructor.
         *
         * @param  padding  padding, may be null
         */
        public SingleGanger( Padding padding ) {
            padding_ = padding;
        }

        public int getZoneCount() {
            return 1;
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
                                SurfaceFactory<P,A> surfFact,
                                ZoneContent<P,A>[] contents,
                                Trimming[] trimmings,
                                ShadeAxis[] shadeAxes, boolean withScroll ) {
            ZoneContent<P,A> content = contents[ 0 ];
            Rectangle plotBounds =
                PlotPlacement
               .calculateDataBounds( extBounds, padding_, surfFact,
                                     content.getProfile(), content.getAspect(),
                                     withScroll, trimmings[ 0 ],
                                     shadeAxes[ 0 ] );
            return new SingleGang( plotBounds );
        }

        public Gang createApproxGang( Rectangle extBounds ) {
            Insets insets = padding_.overrideInsets( new Insets( 0, 0, 0, 0 ) );
            Rectangle plotBounds = PlotUtil.subtractInsets( extBounds, insets );
            return new SingleGang( plotBounds );
        }

        public A[] adjustAspects( A[] oldAspects, int iz ) {
            return oldAspects;
        }

        public P[] adjustProfiles( P[] oldProfiles ) {
            return oldProfiles;
        }
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
