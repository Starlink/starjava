package uk.ac.starlink.ttools.plot2.layer;

import java.util.Map;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.Scale;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.paper.PaperType;

/**
 * PlotLayer implementation that delegates all its behaviour to a
 * base instance.
 *
 * @author   Mark Taylor
 * @since    22 Jan 2021
 */
public class WrapperPlotLayer implements PlotLayer {

    private final PlotLayer base_;

    /**
     * Constructor.
     *
     * @param   base  base instance
     */
    public WrapperPlotLayer( PlotLayer base ) {
        base_ = base;
    }

    public Plotter<?> getPlotter() {
        return base_.getPlotter();
    }

    public Style getStyle() {
        return base_.getStyle();
    }

    public DataGeom getDataGeom() {
        return base_.getDataGeom();
    }

    public DataSpec getDataSpec() {
        return base_.getDataSpec();
    }

    public void extendCoordinateRanges( Range[] ranges, Scale[] scales,
                                        DataStore dataStore ) {
        base_.extendCoordinateRanges( ranges, scales, dataStore );
    }

    public Map<AuxScale,AuxReader> getAuxRangers() {
        return base_.getAuxRangers();
    }

    public LayerOpt getOpt() {
        return base_.getOpt();
    }

    public Drawing createDrawing( Surface surface, Map<AuxScale,Span> auxSpans,
                                  PaperType paperType ) {
        return base_.createDrawing( surface, auxSpans, paperType );
    }
}
