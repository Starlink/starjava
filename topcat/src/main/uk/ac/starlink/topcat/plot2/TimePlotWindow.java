package uk.ac.starlink.topcat.plot2;

import java.awt.Component;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.TimeMapper;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.GangerFactory;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.geom.TimeAspect;
import uk.ac.starlink.ttools.plot2.geom.TimePlotType;
import uk.ac.starlink.ttools.plot2.geom.TimeStackGanger;
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
     */
    public TimePlotWindow( Component parent ) {
        super( "Time Plot", parent, PLOT_TYPE, PLOT_GUI );
        getToolBar().addSeparator();
        addHelp( "TimePlotWindow" );
    }

    /**
     * Returns the index of the column within a given colum model at which
     * a value in the time domain can be found.
     *
     * @param  colModel   list of columns from a table
     * @return    index in list of the first/best time (epoch) column,
     *            or -1 if nothing suitable can be found
     */
    private static int getTimeIndex( ColumnDataComboBoxModel colModel ) {
        if ( colModel != null ) {
            for ( int ic = 0; ic < colModel.getSize(); ic++ ) {
                ColumnData cdata = colModel.getColumnDataAt( ic );
                if ( cdata != null ) {
                    ColumnInfo info = cdata.getColumnInfo();
                    for ( DomainMapper mapper : info.getDomainMappers() ) {
                        if ( mapper instanceof TimeMapper ) {
                            return ic;
                        }
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Defines GUI features specific to time plot.
     */
    private static class TimePlotTypeGui
            implements PlotTypeGui<TimeSurfaceFactory.Profile,TimeAspect> {
        public AxisController<TimeSurfaceFactory.Profile,TimeAspect>
                createAxisController() {
            return new TimeAxisController();
        }
        public PositionCoordPanel createPositionCoordPanel( int npos ) {
            DataGeom geom = PLOT_TYPE.getPointDataGeoms()[ 0 ];
            Coord[] coords =
                PositionCoordPanel.multiplyCoords( geom.getPosCoords(), npos );
            return new SimplePositionCoordPanel( coords, geom ) {
                @Override
                public void autoPopulate() {

                    /* Try to put a time column in the time column selector. */
                    ColumnDataComboBoxModel timeModel =
                        getColumnSelector( 0, 0 );
                    ColumnDataComboBoxModel yModel =
                        getColumnSelector( 1, 0 );
                    int ict = getTimeIndex( timeModel );
                    if ( timeModel != null && yModel != null && ict >= 0 ) {
                        timeModel.setSelectedItem( timeModel
                                                  .getElementAt( ict ) );
                        int icy = -1;
                        for ( int ic = 0; ic < yModel.getSize() && icy < 0;
                              ic++ ) {
                            ColumnData cdata = yModel.getColumnDataAt( ic );
                            if ( ic != ict && cdata != null ) {
                                ColumnInfo info = cdata.getColumnInfo();
                                if ( Number.class
                                    .isAssignableFrom( info
                                                      .getContentClass() ) ) {
                                    icy = ic;
                                }
                            }
                        }
                        if ( icy >= 0 ) {
                            yModel.setSelectedItem( yModel
                                                   .getElementAt( icy ) );
                        }
                    }
                }
            };
        }
        public boolean hasPositions() {
            return true;
        }
        public GangerFactory getGangerFactory() {
            return TimeStackGanger.FACTORY;
        }
        public ZoneFactory createZoneFactory() {
            return ZoneFactories.createIntegerZoneFactory( true );
        }
        public String getNavigatorHelpId() {
            return "timeNavigation";
        }
    }
}
