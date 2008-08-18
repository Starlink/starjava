package uk.ac.starlink.ttools.plot;

/**
 * Interface for objects which wish to be informed when a plot has been
 * completed.
 *
 * @author   Mark Taylor
 * @since    3 Apr 2008
 */
public interface PlotListener {

    /**
     * Called when a plot has been completed.
     *
     * @param  evt  plot event
     */
    void plotChanged( PlotEvent evt );
}
