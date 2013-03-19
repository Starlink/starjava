package uk.ac.starlink.topcat.plot2;

import java.awt.Component;
import uk.ac.starlink.ttools.plot2.geom.CubeAspect;
import uk.ac.starlink.ttools.plot2.geom.CubePlotType;
import uk.ac.starlink.ttools.plot2.geom.CubeSurfaceFactory;

/**
 * Layer plot window for 3D Cartesian plots.
 *
 * @author   Mark Taylor
 * @since    19 Mar 2013
 */
public class CubePlotWindow
       extends StackPlotWindow<CubeSurfaceFactory.Profile,CubeAspect> {
    private static final CubePlotType PLOT_TYPE = CubePlotType.getInstance();
    private static final CubePlotTypeGui PLOT_GUI = new CubePlotTypeGui();

    /**
     * Constructor.
     *
     * @param  parent   parent component
     */
    public CubePlotWindow( Component parent ) {
        super( "Cube2", parent, PLOT_TYPE, PLOT_GUI );
    }

    /**
     * Defines GUI features specific to cube plot.
     */
    private static class CubePlotTypeGui
            implements PlotTypeGui<CubeSurfaceFactory.Profile,CubeAspect> {
        public AxisControl<CubeSurfaceFactory.Profile,CubeAspect>
                createAxisControl( ControlStack stack ) {
            return new CubeAxisControl( false, stack );
        }
        public PositionCoordPanel createPositionCoordPanel() {
            return new SimplePositionCoordPanel( PLOT_TYPE.getDataGeoms()[ 0 ],
                                                 true );
        }
    }
}
