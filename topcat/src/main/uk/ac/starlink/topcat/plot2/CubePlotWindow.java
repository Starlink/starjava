package uk.ac.starlink.topcat.plot2;

import java.awt.Component;
import uk.ac.starlink.ttools.plot2.GangerFactory;
import uk.ac.starlink.ttools.plot2.SingleGanger;
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
        super( "Cube Plot", parent, PLOT_TYPE, PLOT_GUI );
        getToolBar().addSeparator();
        addHelp( "CubePlotWindow" );
    }

    /**
     * Defines GUI features specific to cube plot.
     */
    private static class CubePlotTypeGui
            implements PlotTypeGui<CubeSurfaceFactory.Profile,CubeAspect> {
        public AxisController<CubeSurfaceFactory.Profile,CubeAspect>
                createAxisController() {
            return new CubeAxisController( false );
        }
        public PositionCoordPanel createPositionCoordPanel( int npos ) {
            return SimplePositionCoordPanel
                  .createPanel( PLOT_TYPE.getPointDataGeoms()[ 0 ], npos );
        }
        public boolean hasPositions() {
            return true;
        }
        public GangerFactory getGangerFactory() {
            return SingleGanger.FACTORY;
        }
        public ZoneFactory createZoneFactory() {
            return ZoneFactories.FIXED;
        }
        public String getNavigatorHelpId() {
            return "cubeNavigation";
        }
    }
}
