package uk.ac.starlink.ttools.plot2.layer;

import java.util.HashMap;
import java.util.Map;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.Scale;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;

/**
 * Partial PlotLayer implementation.
 * This implementation just supplies straightforward implementations of
 * the interface accessor methods.
 *
 * @author   Mark Taylor
 * @since    15 Feb 2013
 */
public abstract class AbstractPlotLayer implements PlotLayer {

    private final Plotter<?> plotter_;
    private final DataGeom geom_;
    private final DataSpec dataSpec_;
    private final Style style_;
    private final LayerOpt opt_;

    /**
     * Constructor.
     *
     * @param  plotter  plotter that created this layer
     * @param  geom   defines data space
     * @param  dataSpec  required data values
     * @param  style  plotting style
     * @param  opt   layer optimisation option
     */
    protected AbstractPlotLayer( Plotter<?> plotter, DataGeom geom,
                                 DataSpec dataSpec, Style style,
                                 LayerOpt opt ) {
        plotter_ = plotter;
        geom_ = geom;
        dataSpec_ = dataSpec;
        style_ = style;
        opt_ = opt;
    }

    public Plotter<?> getPlotter() {
        return plotter_;
    }

    public DataGeom getDataGeom() {
        return geom_;
    }

    /**
     * This implementation does nothing.
     */
    public void extendCoordinateRanges( Range[] ranges, Scale[] scales,
                                        DataStore dataStore ) {
    }

    public DataSpec getDataSpec() {
        return dataSpec_;
    }

    public Style getStyle() {
        return style_;
    }

    public LayerOpt getOpt() {
        return opt_;
    }

    /**
     * This implementation returns a new empty map.
     * Subclasses overriding this implementation may call the superclass
     * method, modify the resulting map, and pass it on.
     */
    public Map<AuxScale,AuxReader> getAuxRangers() {
        return new HashMap<AuxScale,AuxReader>();
    }
}
