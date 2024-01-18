package uk.ac.starlink.topcat.plot2;

import java.awt.Component;
import javax.swing.ListModel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.TimeDomain;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.geom.TimeAspect;
import uk.ac.starlink.ttools.plot2.geom.TimePlotType;
import uk.ac.starlink.ttools.plot2.geom.TimeSurfaceFactory;

/**
 * Layer plot window for plots with a horizontal time axis.
 *
 * @author   Mark Taylor
 * @since    24 Jul 2013
 */
public class TimePlotWindow
             extends StackPlotWindow<TimeSurfaceFactory.Profile,TimeAspect> {
    private static final TimePlotType PLOT_TYPE = TimePlotType.getInstance();
    private static final TimePlotTypeGui PLOT_GUI = new TimePlotTypeGui();

    /**
     * Constructor.
     *
     * @param  parent  parent component
     * @param  tablesModel  list of available tables
     */
    public TimePlotWindow( Component parent,
                           ListModel<TopcatModel> tablesModel ) {
        super( "Time Plot", parent, PLOT_TYPE, PLOT_GUI, tablesModel );
        getToolBar().addSeparator();
        addHelp( "TimePlotWindow" );
    }

    /**
     * Defines GUI features specific to time plot.
     */
    private static class TimePlotTypeGui
            implements PlotTypeGui<TimeSurfaceFactory.Profile,TimeAspect> {
        public AxesController<TimeSurfaceFactory.Profile,TimeAspect>
                createAxesController() {
            return new DisjointAxesController<TimeSurfaceFactory.Profile,
                                              TimeAspect>(
                ZoneFactories.createIntegerZoneFactory( true ),
                TimeAxisController::new
            );
        }
        public PositionCoordPanel createPositionCoordPanel( final int npos ) {
            final TimeDomain domain = TimeDomain.INSTANCE;
            DataGeom geom = PLOT_TYPE.getPointDataGeoms()[ 0 ];
            Coord[] coords =
                BasicCoordPanel.multiplyCoords( geom.getPosCoords(), npos );
            return new SimplePositionCoordPanel( coords, geom ) {
                @Override
                public void autoPopulate() {
                    if ( npos > 1 ) {
                        return;
                    }

                    /* Try to put a time column in the time column selector. */
                    ColumnDataComboBoxModel timeModel =
                        getColumnSelector( 0, 0 );
                    ColumnDataComboBoxModel yModel =
                        getColumnSelector( 1, 0 );
                    ColumnData timeData = null;
                    for ( int ic = 0;
                          timeData == null && ic < timeModel.getSize(); ic++ ) {
                        ColumnData cdata = timeModel.getColumnDataAt( ic );
                        if ( cdata != null ) {
                            ColumnInfo info = cdata.getColumnInfo();
                            if ( info != null ) {
                                if ( domain.getProbableMapper( info )
                                     != null ) {
                                    timeData = cdata;
                                }
                            }
                        }
                    }

                    /* If successful, fill in a Y value as well. */
                    if ( timeData != null ) {
                        timeModel.setSelectedItem( timeData );
                        ColumnData yData = null;
                        for ( int ic = 0;
                              yData == null && ic < yModel.getSize(); ic++ ) {
                            ColumnData cdata = yModel.getColumnDataAt( ic );
                            if ( cdata != null ) {
                                ColumnInfo info = cdata.getColumnInfo();
                                if ( info != null &&
                                     domain.getProbableMapper( info ) == null &&
                                     Number.class
                                    .isAssignableFrom( info
                                                      .getContentClass() ) ) {
                                    yData = cdata;
                                }
                            }
                        }
                        if ( yData != null ) {
                            yModel.setSelectedItem( yData );
                        }
                    }
                }
            };
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
            return ZoneFactories.createIntegerZoneFactory( true );
        }
        public CartesianRanger getCartesianRanger() {
            return null;
        }
        public boolean hasExtraHistogram() {
            return false;
        }
        public String getNavigatorHelpId() {
            return "timeNavigation";
        }
    }
}
