package uk.ac.starlink.topcat.plot2;

import java.awt.Component;
import java.awt.Rectangle;
import javax.swing.ListModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.geom.CubeAspect;
import uk.ac.starlink.ttools.plot2.geom.CubePlotType;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
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
    private static final CubeRanger CUBE_RANGER = new CubeRanger();
    private static final CubePlotTypeGui PLOT_GUI = new CubePlotTypeGui();
    private static final String[] XYZ = new String[] { "x", "y", "z" };
    private static final CoordSpotter[] XYZ_SPOTTERS = new CoordSpotter[] {
        CoordSpotter.createUcdSpotter( "pos.cartesian", XYZ, false ),
        CoordSpotter.createUcdSpotter( "pos.cartesian", XYZ, true ),
        CoordSpotter.createNamePrefixSpotter( XYZ, true ),
        CoordSpotter.createNamePrefixSpotter( XYZ, false ),
    };

    /**
     * Constructor.
     *
     * @param  parent   parent component
     * @param  tablesModel  list of available tables
     */
    public CubePlotWindow( Component parent,
                           ListModel<TopcatModel> tablesModel ) {
        super( "Cube Plot", parent, PLOT_TYPE, PLOT_GUI, tablesModel );
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
                  .createPanel( PLOT_TYPE.getPointDataGeoms()[ 0 ], npos,
                                XYZ_SPOTTERS );
        }
        public PositionCoordPanel createAreaCoordPanel() {
            throw new UnsupportedOperationException();
        }
        public boolean hasPositions() {
            return true;
        }
        public FigureMode[] getFigureModes() {
            return new FigureMode[ 0 ];
        }
        public ZoneFactory createZoneFactory() {
            return ZoneFactories.FIXED;
        }
        public CartesianRanger getCartesianRanger() {
            return CUBE_RANGER;
        }
        public String getNavigatorHelpId() {
            return "cubeNavigation";
        }
    }

    /**
     * CartesianRanger implementation for cube plot.
     */
    private static class CubeRanger implements CartesianRanger {
        public int getDimCount() {
            return 3;
        }
        public double[][] getDataLimits( Surface surf ) {
            CubeSurface csurf = (CubeSurface) surf;
            return new double[][] {
                csurf.getDataLimits( 0 ),
                csurf.getDataLimits( 1 ),
                csurf.getDataLimits( 2 ),
            };
        }
        public boolean[] getLogFlags( Surface surf ) {
            return ((CubeSurface) surf).getLogFlags();
        }
        public int[] getPixelDims( Surface surf ) {
            Rectangle bounds = ((CubeSurface) surf).getPlotBounds();
            int npix = Math.max( bounds.width, bounds.height );
            return new int[] { npix, npix, npix };
        }
    }
}
