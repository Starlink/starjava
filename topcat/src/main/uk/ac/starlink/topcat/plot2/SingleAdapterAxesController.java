package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;
import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;

/**
 * AxesController implementation for use with one-zone plots.
 * This is a wrapper around the legacy {@link AxisController} class.
 *
 * @author   Mark Taylor
 * @since    18 Aug 2023
 */
public class SingleAdapterAxesController<P,A> implements AxesController<P,A> {

    private final AxisController<P,A> axisController_;
    private final ZoneController<P,A> zoneController_;

    /**
     * Constructor.
     * The supplied axisController provides the behaviour for the methods
     * of both this instance and of the sole ZoneController which it dispenses.
     *
     * @param  axisController   object to which behaviour is delegated
     * @see   #create
     */
    public SingleAdapterAxesController( AxisController<P,A> axisController ) {
        axisController_ = axisController;
        zoneController_ =
            new SingleAdapterZoneController<P,A>( axisController );
    }

    public void configureForLayers( LayerControl[] layerControls ) {
        axisController_.configureForLayers( layerControls );
    }

    public List<ZoneController<P,A>> getZoneControllers( Ganger<P,A> ganger ) {
        if ( ganger.getZoneCount() == 1 ) {
            return Collections.singletonList( zoneController_ );
        }
        else {
            throw new IllegalArgumentException( "Can't do multi-zone" );
        }
    }

    public Control[] getStackControls() { 
        return axisController_.getControls();
    }

    public ConfigMap getConfig() {
        return axisController_.getConfig();
    }

    public void addActionListener( ActionListener listener ) {
        axisController_.addActionListener( listener );
    }

    public void removeActionListener( ActionListener listener ) {
        axisController_.removeActionListener( listener );
    }

    /**
     * Utiilty method for instance construction.
     * Convenient for use with generics.
     *
     * @param axisController  object to which behaviour is delegated
     * @return  new instance
     */
    public static <P,A> SingleAdapterAxesController<P,A>
            create( AxisController<P,A> axisController ) {
        return new SingleAdapterAxesController<P,A>( axisController );
    }
}
