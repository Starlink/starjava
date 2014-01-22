package uk.ac.starlink.topcat.plot2;

import java.awt.Component;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.geom.PlaneAspect;
import uk.ac.starlink.ttools.plot2.geom.PlaneDataGeom;
import uk.ac.starlink.ttools.plot2.geom.PlanePlotType;
import uk.ac.starlink.ttools.plot2.geom.PlaneSurfaceFactory;
import uk.ac.starlink.ttools.plot2.layer.FunctionPlotter;
import uk.ac.starlink.ttools.plot2.layer.HistogramPlotter;
import uk.ac.starlink.ttools.plot2.paper.PaperTypeSelector;

/**
 * Layer plot window for histograms.
 * This is a slight variant of PlanePlotWindow, with a restricted set
 * of plotters and modified axis controls.  It's here for convenience
 * and easy start of a histogram - it is also possible to draw histograms
 * in a normal plane plot.
 *
 * @author   Mark Taylor
 * @since    21 Jan 2014
 */
public class HistogramPlotWindow
             extends StackPlotWindow<PlaneSurfaceFactory.Profile,PlaneAspect> {

    private static final PlotType PLOT_TYPE = new HistogramPlotType();
    private static final PlotTypeGui PLOT_GUI = new HistogramPlotTypeGui();

    /**
     * Constructor.
     *
     * @param  parent  parent component
     */
    public HistogramPlotWindow( Component parent ) {
        super( "Histogram2", parent, PLOT_TYPE, PLOT_GUI );
        addHelp( "HistogramPlotWindow" );
    }

    /**
     * Variant of PlanePlotType for histograms; has a different set of plotters.
     */
    private static class HistogramPlotType implements PlotType {
        private final PaperTypeSelector ptSelector_ =
            PlanePlotType.getInstance().getPaperTypeSelector();
        private final SurfaceFactory surfFact_ = new PlaneSurfaceFactory();
        public PaperTypeSelector getPaperTypeSelector() {
            return ptSelector_;
        }
        public DataGeom[] getPointDataGeoms() {
            return new DataGeom[] { PlaneDataGeom.INSTANCE };
        }
        public SurfaceFactory getSurfaceFactory() {
            return surfFact_;
        }
        public Plotter[] getPlotters() {
            return new Plotter[] {
                new HistogramPlotter( PlaneDataGeom.X_COORD, true ),
                FunctionPlotter.PLANE,
            };
        }
    }

    /**
     * Defines GUI features specific to histogram plot.
     */
    private static class HistogramPlotTypeGui
            implements PlotTypeGui<PlaneSurfaceFactory.Profile,PlaneAspect> {
        public AxisController<PlaneSurfaceFactory.Profile,PlaneAspect>
                createAxisController( ControlStack stack ) {
            return new HistogramAxisController( stack );
        }
        public PositionCoordPanel createPositionCoordPanel( int npos ) {
            return SimplePositionCoordPanel
                  .createPanel( PLOT_TYPE.getPointDataGeoms()[ 0 ], npos );
        }
    }
}
