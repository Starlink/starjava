package uk.ac.starlink.ttools.plot2.task;

import java.util.EventListener;

/**
 * Listens for events representing the selection of a data point in a plot.
 * This usually happens when a user clicks on a plot.
 *
 * @author   Mark Taylor
 * @since    21 Nov 2014
 */
public interface PointSelectionListener extends EventListener {

    /**
     * Called when an attempt has been made to select a point,
     * usually by clicking on a plot.  The selection event is triggered
     * by the attempt; the content of the event indicates whether
     * any point(s) were actually selected.
     *
     * <p>Note that because identification of a selected point is a
     * potentially expensive operation (it requires a scan of all plotted
     * data points), this method is called asynchronously, rather than
     * from the associated mouse listener.
     * Invocation will however be on the Event Dispatch Thread as usual.
     * 
     * @param  evt  selection event
     */
    public void pointSelected( PointSelectionEvent evt );
}
