package uk.ac.starlink.topcat.plot2;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.ListModel;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Scale;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.data.AreaCoord;
import uk.ac.starlink.ttools.plot2.data.Coord;
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
    @SuppressWarnings("this-escape")
    public PlanePlotWindow( Component parent,
                            ListModel<TopcatModel> tablesModel ) {
        super( "Plane Plot", parent, PLOT_TYPE, PLOT_GUI, tablesModel );
        getToolBar().addSeparator();
        addHelp( "PlanePlotWindow" );
    }

    /**
     * Defines GUI features specific to plane plot.
     */
    private static class PlanePlotTypeGui
            implements PlotTypeGui<PlaneSurfaceFactory.Profile,PlaneAspect> {
        public AxesController<PlaneSurfaceFactory.Profile,PlaneAspect>
                createAxesController() {
            return SingleAdapterAxesController
                  .create( new PlaneAxisController() );
        }
        public PositionCoordPanel createPositionCoordPanel( int npos ) {
            final SimplePositionCoordPanel panel =
                SimplePositionCoordPanel
               .createPanel( PLOT_TYPE.getPointDataGeoms()[ 0 ], npos,
                             XY_SPOTTERS );
            assert panel.getCoords().length == 2 * npos;
            panel.addButtons( new Action[] {
                new BasicAction( "X \u2194 Y", null, "Switch X and Y values" ) {
                    public void actionPerformed( ActionEvent evt ) {
                        for ( int ipos = 0; ipos < npos; ipos++ ) {
                            ColumnDataComboBoxModel xModel =
                                panel.getColumnSelector( 2 * ipos + 0, 0 );
                            ColumnDataComboBoxModel yModel =
                                panel.getColumnSelector( 2 * ipos + 1, 0 );
                            if ( xModel != null && yModel != null ) {
                                Object xItem = xModel.getSelectedItem();
                                Object yItem = yModel.getSelectedItem();
                                xModel.setSelectedItem( yItem );
                                yModel.setSelectedItem( xItem );
                            }
                        }
                    }
                },
            } );
            return panel;
        }
        public PositionCoordPanel createAreaCoordPanel() {
            return new AreaCoordPanel( AreaCoord.PLANE_COORD, new Coord[ 0 ],
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
            return PlaneFigureMode.MODES;
        }
        public ZoneFactory createZoneFactory() {
            return ZoneFactories.FIXED;
        } 
        public CartesianRanger getCartesianRanger() {
            return PLANE_RANGER;
        }
        public boolean hasExtraHistogram() {
            return false;
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
        public Scale[] getScales( Surface surf ) {
            return ((PlanarSurface) surf).getScales();
        }
        public int[] getPixelDims( Surface surf ) {
            Rectangle bounds = surf.getPlotBounds();
            return new int[] { bounds.width, bounds.height };
        }
    }
}
