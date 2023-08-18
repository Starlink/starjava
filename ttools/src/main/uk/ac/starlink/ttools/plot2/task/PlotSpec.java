package uk.ac.starlink.ttools.plot2.task;

import java.awt.Dimension;
import uk.ac.starlink.ttools.plot2.Padding;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;

/**
 * Specifies a plot in sufficient detail to recreate it as a STILTS
 * command.
 *
 * @author   Mark Taylor
 * @since    17 Jul 2017
 */
public class PlotSpec<P,A> {

    private final PlotType<P,A> plotType_;
    private final Dimension extSize_;
    private final Padding padding_;
    private final ConfigMap globalConfig_;
    private final ConfigMap[] zoneConfigs_;
    private final TrimmingSpec[] trimSpecs_;
    private final ShadeSpec[] shadeSpecs_;
    private final LayerSpec[] layerSpecs_;

    /**
     * Constructor.
     *
     * @param   plotType   plot type
     * @param   extSize   total size of output graphic, or null
     * @param   padding   padding within extSize, or null
     * @param   globalConfig  per-plot configuration map
     * @param   zoneConfigs   per-zone configuration maps
     * @param   trimSpecs    trimming specification array,
     *                       either nzone-element or 1-element for global
     * @param   shadeSpecs   aux shade axis specification array,
     *                       either nzone-element or 1-element for global
     * @param   layerSpecs   specifications for each plot layer
     */
    public PlotSpec( PlotType<P,A> plotType, Dimension extSize, Padding padding,
                     ConfigMap globalConfig, ConfigMap[] zoneConfigs,
                     TrimmingSpec[] trimSpecs, ShadeSpec[] shadeSpecs,
                     LayerSpec[] layerSpecs ) {
        plotType_ = plotType;
        extSize_ = extSize;
        padding_ = padding;
        globalConfig_ = globalConfig;
        zoneConfigs_ = zoneConfigs;
        trimSpecs_ = trimSpecs;
        shadeSpecs_ = shadeSpecs;
        layerSpecs_ = layerSpecs;
    }

    /**
     * Returns the plot type supplied at construction time.
     *
     * @return  plot type
     */
    public PlotType<P,A> getPlotType() {
        return plotType_;
    }
                     
    /** 
     * Returns the external plot size supplied at construction time.
     *  
     * @return  total size of export graphic, or null
     */ 
    public Dimension getExtSize() {
        return extSize_;
    }   
        
    /**     
     * Returns the external padding supplied at construction time.
     *  
     * @return  padding within extSize, or null
     */
    public Padding getPadding() {
        return padding_;
    }

    /**
     * Returns per-plot configuration settings.
     *
     * @return  global config map
     */
    public ConfigMap getGlobalConfig() {
        return globalConfig_;
    }

    /**
     * Returns the per-zone array of zone configuration settings.
     *
     * @return  nzone-element config map array
     */
    public ConfigMap[] getZoneConfigs() {
        return zoneConfigs_;
    }

    /**
     * Returns the trimming specifications.
     * This is either an nzone-element array for per-zone trimmings,
     * or a 1-element array for global trimmings.
     *
     * @return  nz- or 1-element array of trimming specifications
     */
    public TrimmingSpec[] getTrimmingSpecs() {
        return trimSpecs_;
    }

    /**
     * Returns the aux shade axis specifications.
     * This is either an nzone-element array for per-zone aux axes,
     * or a 1-element array for a global aux axis.
     *
     * @return  nz- or 1-element array of shader specifications
     */
    public ShadeSpec[] getShadeSpecs() {
        return shadeSpecs_;
    }

    /**
     * Returns the layer specification array supplied at construction time.
     *
     * @return   specifications for each plot layer
     */
    public LayerSpec[] getLayerSpecs() {
        return layerSpecs_;
    }
}
