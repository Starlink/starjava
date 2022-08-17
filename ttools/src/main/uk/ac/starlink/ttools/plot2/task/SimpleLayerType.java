package uk.ac.starlink.ttools.plot2.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.data.Coord;

/**
 * LayerType representing a single Plotter.
 *
 * @author   Mark Taylor
 * @since    15 Sep 2014
 */
public class SimpleLayerType implements LayerType {

    private final Plotter<?> plotter_;

    /**
     * Constructor.
     *
     * @param  plotter  plotter
     */
    public SimpleLayerType( Plotter<?> plotter ) {
        plotter_ = plotter;
    }

    public String getName() {
        return plotter_.getPlotterName();
    }

    public String getXmlDescription() {
        return plotter_.getPlotterDescription();
    }

    public Parameter<?>[] getAssociatedParameters( String suffix ) {
        return new Parameter<?>[ 0 ];
    }

    public Plotter<?> getPlotter( Environment env, String suffix ) {
        return plotter_;
    }

    /**
     * Returns this layer type's single plotter.
     *
     * @return  plotter
     */
    public Plotter<?> getPlotter() {
        return plotter_;
    }

    public int getPositionCount() {
        return plotter_.getCoordGroup().getBasicPositionCount();
    }

    public Coord[] getExtraCoords() {
        return plotter_.getCoordGroup().getExtraCoords();
    }

    public ConfigKey<?>[] getStyleKeys() {
        return plotter_.getStyleKeys();
    }
}

