package uk.ac.starlink.topcat.plot2;

import java.awt.Component;
import java.awt.Rectangle;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.TypedListModel;
import uk.ac.starlink.ttools.plot2.GangerFactory;
import uk.ac.starlink.ttools.plot2.SingleGanger;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.geom.PlanarSurface;
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
    private static final CartesianRanger PLANE_RANGER = new PlaneRanger();
    private static final PlanePlotTypeGui PLOT_GUI = new PlanePlotTypeGui();
    private static final String[] XY = new String[] { "x", "y" };
    private static final CoordSpotter[] XY_SPOTTERS = new CoordSpotter[] {
        CoordSpotter.createUcdSpotter( "pos.cartesian", XY, false ),
        CoordSpotter.createUcdSpotter( "pos.cartesian", XY, true ),
        CoordSpotter.createNamePrefixSpotter( XY, true ),
        CoordSpotter.createNamePrefixSpotter( XY, false ),
    };

    /**
     * Constructor.
     *
     * @param  parent  parent component
     * @param  tablesModel  list of available tables
     */
    public PlanePlotWindow( Component parent,
                            TypedListModel<TopcatModel> tablesModel ) {
        super( "Plane Plot", parent, PLOT_TYPE, PLOT_GUI, tablesModel );
        getToolBar().addSeparator();
        addHelp( "PlanePlotWindow" );
    }

    /**
     * Defines GUI features specific to plane plot.
     */
    private static class PlanePlotTypeGui
            implements PlotTypeGui<PlaneSurfaceFactory.Profile,PlaneAspect> {
        public AxisController<PlaneSurfaceFactory.Profile,PlaneAspect>
                createAxisController() {
            return new PlaneAxisController();
        }
        public PositionCoordPanel createPositionCoordPanel( int npos ) {
            return SimplePositionCoordPanel
                  .createPanel( PLOT_TYPE.getPointDataGeoms()[ 0 ], npos,
                                XY_SPOTTERS );
        }
        public boolean hasPositions() {
            return true;
        }
        public boolean isPlanar() {
            return true;
        }
        public FigureMode[] getFigureModes() {
            return PlaneFigureMode.MODES;
        }
        public GangerFactory getGangerFactory() {
            return SingleGanger.FACTORY;
        }
        public ZoneFactory createZoneFactory() {
            return ZoneFactories.FIXED;
        } 
        public CartesianRanger getCartesianRanger() {
            return PLANE_RANGER;
        }
        public String getNavigatorHelpId() {
            return "planeNavigation";
        }
    }

    /**
     * CartesianRanger implementation for plane plot.
     */
    private static class PlaneRanger implements CartesianRanger {
        public int getDimCount() {
            return 2;
        }
        public double[][] getDataLimits( Surface surf ) {
            return ((PlanarSurface) surf).getDataLimits();
        }
        public boolean[] getLogFlags( Surface surf ) {
            return ((PlanarSurface) surf).getLogFlags();
        }
        public int[] getPixelDims( Surface surf ) {
            Rectangle bounds = surf.getPlotBounds();
            return new int[] { bounds.width, bounds.height };
        }
    }
}
