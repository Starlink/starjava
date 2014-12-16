package uk.ac.starlink.topcat.plot2;

import java.awt.Component;
import uk.ac.starlink.ttools.plot2.geom.CubeAspect;
import uk.ac.starlink.ttools.plot2.geom.CubeSurfaceFactory;
import uk.ac.starlink.ttools.plot2.geom.SpherePlotType;

/**
 * Layer plot window for 3D plots with spherical polar coordinates.
 *
 * @author   Mark Taylor
 * @since    19 Mar 2013
 */
public class SpherePlotWindow
       extends StackPlotWindow<CubeSurfaceFactory.Profile,CubeAspect> {
    private static final SpherePlotType PLOT_TYPE =
        SpherePlotType.getInstance();
    private static final SpherePlotTypeGui PLOT_GUI = new SpherePlotTypeGui();

    /**
     * Constructor.
     *
     * @param  parent  parent component
     */
    public SpherePlotWindow( Component parent ) {
        super( "Sphere Plot", parent, PLOT_TYPE, PLOT_GUI );
        getToolBar().addSeparator();
        addHelp( "SpherePlotWindow" );
    }

    /**
     * Defines GUI features specific to sphere plot.
     */
    private static class SpherePlotTypeGui
            implements PlotTypeGui<CubeSurfaceFactory.Profile,CubeAspect> {
        public AxisController<CubeSurfaceFactory.Profile,CubeAspect>
                createAxisController( ControlStack stack ) {
            return new CubeAxisController( true, stack );
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
