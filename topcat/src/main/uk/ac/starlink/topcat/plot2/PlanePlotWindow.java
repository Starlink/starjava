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
        super( "Plane2", parent, PLOT_TYPE, PLOT_GUI );
        addHelp( "PlanePlotWindow" );
    }

    /**
     * Defines GUI features specific to plane plot.
     */
    private static class PlanePlotTypeGui
            implements PlotTypeGui<PlaneSurfaceFactory.Profile,PlaneAspect> {
        public AxisControl<PlaneSurfaceFactory.Profile,PlaneAspect>
                createAxisControl( ControlStack stack ) {
            return new PlaneAxisControl( stack );
        }
        public PositionCoordPanel createPositionCoordPanel( int npos ) {
            return SimplePositionCoordPanel
                  .createPanel( PLOT_TYPE.getPointDataGeoms()[ 0 ], npos );
        }
    }
}
