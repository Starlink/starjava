package uk.ac.starlink.ttools.plot2.task;

import java.awt.Dimension;
import uk.ac.starlink.ttools.plot2.Padding;
import uk.ac.starlink.ttools.plot2.PlotType;

/**
 * Specifies a plot in sufficient detail to recreate it as a STILTS
 * command.
 *
 * @author   Mark Taylor
 * @since    17 Jul 2017
 */
public class PlotSpec {

    private final PlotType plotType_;
    private final Dimension extSize_;
    private final Padding padding_;
    private final ZoneSpec[] zoneSpecs_;
    private final LayerSpec[] layerSpecs_;

    /**
     * Constructor.
     *
     * @param   plotType   plot type
     * @param   extSize   total size of output graphic, or null
     * @param   padding   padding within extSize, or null
     * @param   zoneSpecs   specifications for each plot zone;
     *                      has at least one element
     * @param   layerSpecs   specifications for each plot layer
     */
    public PlotSpec( PlotType plotType, Dimension extSize, Padding padding,
                     ZoneSpec[] zoneSpecs, LayerSpec[] layerSpecs ) {
        plotType_ = plotType;
        extSize_ = extSize;
        padding_ = padding;
        zoneSpecs_ = zoneSpecs;
        layerSpecs_ = layerSpecs;
    }

    /**
     * Returns the plot type supplied at construction time.
     *
     * @return  plot type
     */
    public PlotType getPlotType() {
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
     * Returns the zone specification array supplied at construction time.
     *
     * @return  specifications for each plot zone; has at least one element
     */
    public ZoneSpec[] getZoneSpecs() {
        return zoneSpecs_;
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
