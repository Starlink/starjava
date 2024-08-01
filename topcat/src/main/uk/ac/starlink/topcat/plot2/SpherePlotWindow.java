package uk.ac.starlink.topcat.plot2;

import java.awt.Component;
import javax.swing.ListModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.data.AreaCoord;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.layer.AreaForm;
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
     * @param  tablesModel  list of available tables
     */
    @SuppressWarnings("this-escape")
    public SpherePlotWindow( Component parent,
                             ListModel<TopcatModel> tablesModel ) {
        super( "Sphere Plot", parent, PLOT_TYPE, PLOT_GUI, tablesModel );
        getToolBar().addSeparator();
        addHelp( "SpherePlotWindow" );
    }

    /**
     * Defines GUI features specific to sphere plot.
     */
    private static class SpherePlotTypeGui
            implements PlotTypeGui<CubeSurfaceFactory.Profile,CubeAspect> {
        public AxesController<CubeSurfaceFactory.Profile,CubeAspect>
                createAxesController() {
            return SingleAdapterAxesController
                  .create( new CubeAxisController( true ) );
        }
        public PositionCoordPanel createPositionCoordPanel( int npos ) {
            return SimplePositionCoordPanel
                  .createPanel( PLOT_TYPE.getPointDataGeoms()[ 0 ], npos,
                                null );
        }
        public PositionCoordPanel createAreaCoordPanel() {
            return new AreaCoordPanel( AreaCoord.SPHERE_COORD,
                                       new Coord[] { AreaForm.RADIAL_COORD },
                                       new ConfigKey<?>[ 0 ] ) {
                public DataGeom getDataGeom() {
                    return PLOT_TYPE.getPointDataGeoms()[ 0 ];
                }
            };
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
            return null;
        }
        public boolean hasExtraHistogram() {
            return false;
        }
        public String getNavigatorHelpId() {
            return "sphereNavigation";
        }
    }
}
