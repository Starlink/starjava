package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.ttools.plot2.Trimming;
import uk.ac.starlink.ttools.plot2.ShadeAxisKit;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;

/**
 * Encapsulates all the information gathered by the TOPCAT GUI
 * about a plot that needs to be painted.
 * An instance of this class is gathered by {@link StackPlotWindow}
 * and passed to {@link PlotPanel}.
 *
 * @author   Mark Taylor
 * @since    18 Aug 2023
 */
public interface PlotContent<P,A> {

    /**
     * Returns the ganger used for this plot.
     *
     * @return  ganger
     */
    Ganger<P,A> getGanger();

    /**
     * Returns the plot positioning object.
     *
     * @return  plot position
     */
    PlotPosition getPlotPosition();

    /**
     * Returns global configuration for the plot.
     * Per-zone information may be acquired from the
     * {@link ZoneController#getConfig getConfig} method
     * of the relevant ZoneController.
     *
     * <p>Note that much of this information will be redundant with the
     * other items specified here, but it may be required for reconstructing
     * the instructions that led to this zone definition.
     *
     * @return  global plot configuration items
     */
    ConfigMap getGlobalConfig();

    /**
     * Returns trimming configuration items.
     *
     * @return  nzone- or 1-element array of configuration maps
     */
    ConfigMap[] getTrimmingConfigs();

    /**
     * Returns aux shade axis configuration items.
     *
     * @return  nzone- or 1-element array of configuration maps
     */
    ConfigMap[] getShadeConfigs();

    /**
     * Returns an array of zone control GUI components, one per plotted zone.
     * The length of the array must be
     * {@link uk.ac.starlink.ttools.plot2.Ganger#getZoneCount}.
     *
     * @return  nzone-element array of zone controllers
     */
    ZoneController<P,A>[] getZoneControllers();

    /**
     * Returns an array of TopcatLayers that specify what will be plotted
     * on each zone.
     * The {@link TopcatLayer#getPlotLayers} method of the returned objects
     * must all return nzone-element arrays.
     *
     * @return   array of layer content objects
     */
    TopcatLayer[] getLayers();

    /**
     * Returns an array of plot decoration objects.
     * This can be either an nzone-element array giving per-zone decorations,
     * or a 1-element array giving global decorations, according to the result
     * of {@link uk.ac.starlink.ttools.plot2.Ganger#isTrimmingGlobal}.
     *
     * @return  nzone- or 1-element array of trimmings
     */
    Trimming[] getTrimmings();

    /**
     * Returns an array of aux axis kits.
     * This can be either an nzone-element array giving per-zone shade kits,
     * or a 1-element array giving a global shade kit, according to the result
     * of {@link uk.ac.starlink.ttools.plot2.Ganger#isShadingGlobal}.
     * Elements may be null if no aux axis is present.
     *
     * @return  nzone- or 1-element array of aux axis specifications
     */
    ShadeAxisKit[] getShadeAxisKits();
}
