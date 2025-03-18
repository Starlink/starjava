package uk.ac.starlink.topcat.plot2;

import java.awt.Component;
import java.awt.Rectangle;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.ListModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Scale;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.geom.CubeAspect;
import uk.ac.starlink.ttools.plot2.geom.CubeDataGeom;
import uk.ac.starlink.ttools.plot2.geom.CubePlotType;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.geom.CubeSurfaceFactory;
import uk.ac.starlink.ttools.plot2.geom.CubeVectorDataGeom;
import uk.ac.starlink.ttools.plot2.geom.SphereDataGeom;

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
    private static final Map<DataGeom,CoordSpotter[]> GEOM_SPOTTERS =
        createGeomSpotterMap();
    private static final CubePlotTypeGui PLOT_GUI = new CubePlotTypeGui();

    /**
     * Constructor.
     *
     * @param  parent   parent component
     * @param  tablesModel  list of available tables
     */
    @SuppressWarnings("this-escape")
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
        public AxesController<CubeSurfaceFactory.Profile,CubeAspect>
                createAxesController() {
            return SingleAdapterAxesController
                  .create( new CubeAxisController( false ) );
        }
        public PositionCoordPanel createPositionCoordPanel( int npos ) {
            return new MultiGeomPositionCoordPanel( npos, GEOM_SPOTTERS );
        }
        public PositionCoordPanel createAreaCoordPanel() {
            throw new UnsupportedOperationException();
        }
        public ZoneLayerManager createLayerManager( FormLayerControl flc ) {
            return new SingleZoneLayerManager( flc );
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
        public boolean hasExtraHistogram() {
            return false;
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
            Scale[] scales = ((CubeSurface) surf).getScales();
            return new boolean[] {
                scales[ 0 ].isPositiveDefinite(),
                scales[ 1 ].isPositiveDefinite(),
                scales[ 2 ].isPositiveDefinite(),
            };
        }
        public int[] getPixelDims( Surface surf ) {
            Rectangle bounds = ((CubeSurface) surf).getPlotBounds();
            int npix = Math.max( bounds.width, bounds.height );
            return new int[] { npix, npix, npix };
        }
    }

    /**
     * Sets up a map from DataGeom to a list of CoordSpotters that can
     * guess suitable values for the coordinate sets used.
     *
     * <p>UCDs in the implementation are taken from v 1.5 of the
     * <a href="https://www.ivoa.net/documents/UCD1+/"
     *         >UCD1+ Controlled Vocabulary</a> document.
     *
     * @return  map from all DataGeoms suitable for the CubePlotWindow
     *          to lists of appropriate CoordSpotters
     */
    private static Map<DataGeom,CoordSpotter[]> createGeomSpotterMap() {
        Map<DataGeom,CoordSpotter[]> map = new LinkedHashMap<>();

        /* Cartesian component coordinates. */
        String[] xyz = new String[] { "x", "y", "z" };
        map.put( CubeDataGeom.INSTANCE, new CoordSpotter[] {
            CoordSpotter.createUcdSpotter( "pos.cartesian", xyz, false ),
            CoordSpotter.createUcdSpotter( "pos.cartesian", xyz, true ),
            CoordSpotter.createNamePrefixSpotter( xyz, true ),
            CoordSpotter.createNamePrefixSpotter( xyz, false ),
        } );

        /* Cartesian 3-vector coordinates. */
        map.put( CubeVectorDataGeom.INSTANCE, new CoordSpotter[] {
            CoordSpotter.createVectorSpotter( 3 ),
            CoordSpotter.createVectorSpotter( -1 ),
        } );

        /* Spherical polar coordinates. */
        map.put( SphereDataGeom.INSTANCE, new CoordSpotter[] {
            CoordSpotter
           .createUcdSpotter( "pos.bodyrc",
                              new String[] { "lon", "lat", "alt" }, true ),
            CoordSpotter
           .createUcdSpotter( "pos.earth",
                              new String[] { "lon", "lat", "altitude" }, true ),

            // Note these ones don't match the required coordinates,
            // but they are similar, so filling them in may provide a useful
            // clue to the user in absence of better guesses.
            CoordSpotter
           .createUcdSpotter( "pos.spherical",
                              new String[] { "azi", "colat", "r" }, true ),
        } );

        /* Check we have set up coordinate value defaults for all the
         * geoms in use. */
        for ( DataGeom geom : PLOT_TYPE.getPointDataGeoms() ) {
            assert map.containsKey( geom );
        }
        return map;
    }
}
