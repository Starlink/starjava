package uk.ac.starlink.topcat.plot2;

import java.awt.Component;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.geom.PlaneAspect;
import uk.ac.starlink.ttools.plot2.geom.PlanePlotType;
import uk.ac.starlink.ttools.plot2.geom.PlaneSurfaceFactory;

/**
 * Layer plot window for 2D Cartesian plots.
 *
 * @author   Mark Taylor
 * @since    19 Mar 2013
 */
public class PlanePlotWindow
             extends StackPlotWindow<PlaneSurfaceFactory.Profile,PlaneAspect> {
    private static final PlanePlotType PLOT_TYPE = PlanePlotType.getInstance();
    private static final PlanePlotTypeGui PLOT_GUI = new PlanePlotTypeGui();

    /**
     * Constructor.
     *
     * @param  parent  parent component
     */
    public PlanePlotWindow( Component parent ) {
        super( "Plane Plot", parent, PLOT_TYPE, PLOT_GUI );
        getToolBar().addSeparator();
        addHelp( "PlanePlotWindow" );
    }

    /**
     * Defines GUI features specific to plane plot.
     */
    private static class PlanePlotTypeGui
            implements PlotTypeGui<PlaneSurfaceFactory.Profile,PlaneAspect> {
        public AxisController<PlaneSurfaceFactory.Profile,PlaneAspect>
                createAxisController( ControlStack stack ) {
            return new PlaneAxisController( stack );
        }
        public PositionCoordPanel createPositionCoordPanel( int npos ) {
            return SimplePositionCoordPanel
                  .createPanel( PLOT_TYPE.getPointDataGeoms()[ 0 ], npos );
        }
        public boolean hasPositions() {
            return true;
        }
    }
}
