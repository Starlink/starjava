package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionListener;
import java.util.List;
import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;

/**
 * Object which coordinates user control of multi-zone plot axes.
 *
 * @author   Mark Taylor
 * @since    18 Aug 2023
 */
public interface AxesController<P,A> extends Configger {

    /**
     * Returns a list of zone contollers, one for each zone defined
     * by a supplied ganger.
     *
     * @param   ganger  ganger, assumed to be compatible with this controller
     * @return   list of zone controllers,
     *           one for each zone defined by the ganger
     */
    List<ZoneController<P,A>> getZoneControllers( Ganger<P,A> ganger );

    /**
     * Returns all the controls for user configuration of this controller.
     * This includes the main control and possibly others.
     *
     * @return  user controls
     */
    Control[] getStackControls();

    /**
     * Provides a hook for implementations to adjust
     * their GUI state based on the layer controls which will be supplying
     * layers for them to plot.
     *
     * @param   layerControls   layer controls expected to provide layers
     *                          to be plotted on these axes
     */
    void configureForLayers( LayerControl[] layerControls );

    /**
     * Adds a listener notified when any of the controls changes.
     *
     * @param  listener  listener to add
     */
    void addActionListener( ActionListener listener );

    /**
     * Removes a listener previously added by addActionListener.
     *
     * @param   listener   listener to remove
     */
    void removeActionListener( ActionListener listener );
}
