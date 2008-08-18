package uk.ac.starlink.topcat.plot;

import uk.ac.starlink.ttools.plot.PlotEvent;
import uk.ac.starlink.ttools.plot.PlotListener;

/**
 * Component which reports on the number of points plotted and not plotted
 * by listening for plot events.
 *
 * @author   Mark Taylor
 * @since    3 Apr 2008
 */
public class PlotStatsLabel extends CountsLabel implements PlotListener {

    /**
     * Constructor.
     */
    public PlotStatsLabel() {
        super( new String[] { "Potential", "Included", "Visible", } );
    }

    public void plotChanged( PlotEvent evt ) {
        setValues( new int[] { evt.getPotentialPointCount(),
                               evt.getIncludedPointCount(),
                               evt.getVisiblePointCount(), } );
    }
}
