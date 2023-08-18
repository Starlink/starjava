package uk.ac.starlink.topcat.plot2;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import uk.ac.starlink.ttools.plot2.Gang;
import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.geom.StackGanger;

/**
 * AxesController implementation for multi-zone plots in which the
 * zones are not related to each other.
 * It is currently implemented on top of a list of
 * {@link AxisController} instances.
 *
 * @author   Mark Taylor
 * @since    18 Aug 2023
 */
public class DisjointAxesController<P,A> extends AbstractAxesController<P,A> {

    private final ZoneFactory zoneFact_;
    private final MultiConfigger multiConfigger_;
    private final MultiController<ZoneController<P,A>> multiController_;

    /**
     * Constructor.
     *
     * @param  zoneFact  zone factory
     * @param  acSupplier  supplier for per-zone AxisController instances
     */
    public DisjointAxesController( ZoneFactory zoneFact,
                                   Supplier<AxisController<P,A>> acSupplier ) {
        zoneFact_ = zoneFact;
        AxesControllerFactory<P,A> controllerFact =
            new AxesControllerFactory<>( acSupplier );
        multiConfigger_ = new MultiConfigger();
        multiController_ =
            new MultiController<ZoneController<P,A>>
                               ( controllerFact, zoneFact, multiConfigger_ );
        for ( Control control : multiController_.getStackControls() ) {
            addControl( control );
        }
    }

    public void configureForLayers( LayerControl[] layerControls ) {
    }

    public ConfigMap getConfig() {
        // There is no common config information here, it's all at the
        // per-zone level.
        return new ConfigMap();
    }

    public List<ZoneController<P,A>> getZoneControllers( Ganger<P,A> ganger ) {
         String[] zoneNames = ((StackGanger<P,A>) ganger).getZoneNames();
         ZoneId[] zids = Arrays.stream( zoneNames )
                               .map( zoneFact_::nameToId )
                               .toArray( n -> new ZoneId[ n ] );
         Gang gang = ganger.createApproxGang( new Rectangle( 0, 0, 100, 100 ) );
         multiController_.setZones( zids, gang );
         return Arrays.stream( zids )
                      .map( zid -> multiController_.getController( zid ) )
                      .collect( Collectors.toList() );
    }

    /**
     * Utility class adapting a supplier of AxisControllers to a
     * MultiController.ControllerFactory.
     */
    private static class AxesControllerFactory<P,A>
            implements MultiController
                      .ControllerFactory<ZoneController<P,A>> {
        private final Supplier<AxisController<P,A>> acSupplier_;
        AxesControllerFactory( Supplier<AxisController<P,A>> acSupplier ) {
            acSupplier_ = acSupplier;
        }
        public ZoneController<P,A> createController() {
            return new SingleAdapterZoneController<P,A>( acSupplier_.get() );
        }
        public Configger getConfigger( ZoneController<P,A> zoneController ){
            return zoneController;
        }
        public int getControlCount() {
            return 1;
        }
        public Control[] getControls( ZoneController<P,A> zoneController ) {
            return ((SingleAdapterZoneController<P,A>) zoneController)
                  .getAxisController().getControls();
        }
    }
}
