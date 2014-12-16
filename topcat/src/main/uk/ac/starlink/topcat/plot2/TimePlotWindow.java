package uk.ac.starlink.topcat.plot2;

import java.awt.Component;
import uk.ac.starlink.ttools.plot2.PlotType;
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
     */
    public TimePlotWindow( Component parent ) {
        super( "Time Plot", parent, PLOT_TYPE, PLOT_GUI );
        getToolBar().addSeparator();
        addHelp( "TimePlotWindow" );
    }

    /**
     * Defines GUI features specific to time plot.
     */
    private static class TimePlotTypeGui
            implements PlotTypeGui<TimeSurfaceFactory.Profile,TimeAspect> {
        public AxisController<TimeSurfaceFactory.Profile,TimeAspect>
                createAxisController( ControlStack stack ) {
            return new TimeAxisController( stack );
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
