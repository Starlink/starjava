package uk.ac.starlink.splat.iface;

import java.util.EventListener;

/**
 * PlotListener defines an interface used when listening for the
 * creation, removal and changes of Plots.
 *
 * @since $Date$
 * @since 04-OCT-2000
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 */
public interface PlotListener extends EventListener {

    /**
     *  Sent when a plot is created.
     */
    public void plotCreated( PlotChangedEvent e );

    /**
     *  Sent when a plot is removed.
     */
    public void plotRemoved( PlotChangedEvent e );

    /**
     *  Sent when a plot property is changed (i.e. spectrum added/removed?).
     */
    public void plotChanged( PlotChangedEvent e );
}
