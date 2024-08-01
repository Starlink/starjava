package uk.ac.starlink.topcat.plot2;

import java.awt.Component;
import javax.swing.ListModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.geom.MatrixPlotType;
import uk.ac.starlink.ttools.plot2.geom.PlaneAspect;
import uk.ac.starlink.ttools.plot2.geom.PlaneSurfaceFactory;

/**
 * Plot window for matrix plots.
 *
 * @author   Mark Taylor
 * @since    20 Sep 2023
 */
public class MatrixPlotWindow
        extends StackPlotWindow<PlaneSurfaceFactory.Profile,PlaneAspect> {

    private static final MatrixPlotType PLOT_TYPE =
        MatrixPlotType.getInstance();
    private static final MatrixPlotTypeGui PLOT_GUI = new MatrixPlotTypeGui();

    /**
     * Constructor.
     *
     * @param  parent  parent component
     * @param  tablesModel  list of available tables
     */
    @SuppressWarnings("this-escape")
    public MatrixPlotWindow( Component parent,
                             ListModel<TopcatModel> tablesModel ) {
        super( "Corner Plot", parent, PLOT_TYPE, PLOT_GUI, tablesModel );
        getToolBar().addSeparator();
        addHelp( "MatrixPlotWindow" );

        /* Since the top right corner of the plot is by default blank,
         * configure the legend to appear there rather than taking space
         * outside of the plot bounds. */
        getLegendControl().getInsideModel().setSelected( true );
    }

    /**
     * Defines GUI features specific to matrix plot.
     */
    private static class MatrixPlotTypeGui
            implements PlotTypeGui<PlaneSurfaceFactory.Profile,PlaneAspect> {
        public AxesController<PlaneSurfaceFactory.Profile,PlaneAspect>
                createAxesController() {
            return new MatrixAxesController();
        }
        public PositionCoordPanel createPositionCoordPanel( int npos ) {
            if ( npos != 1 ) {
                throw new UnsupportedOperationException();
            }
            return new MatrixPositionCoordPanel();
        }
        public PositionCoordPanel createAreaCoordPanel() {
            throw new UnsupportedOperationException();
        }
        public ZoneLayerManager createLayerManager( FormLayerControl flc ) {
            return new MatrixLayerManager( flc );
        }
        public boolean hasExtraHistogram() {
            return true;
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
            // It would be possible to do something here,
            // but the CartesianRanger interface would need to be changed.
            // Returning null means that the Algebraic Subset From Visible
            // action is not available.
            return null;
        }
        public String getNavigatorHelpId() {
            return "matrixNavigation";
        }
    }
}
