/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     04-OCT-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.util.EventListener;

/**
 * PlotListener defines an interface used when listening for the
 * creation, removal and changes of Plots.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public interface PlotListener 
    extends EventListener 
{
    /**
     *  Sent when a plot is created.
     */
    public void plotCreated( PlotChangedEvent e );

    /**
     *  Sent when a plot is removed.
     */
    public void plotRemoved( PlotChangedEvent e );

    /**
     *  Sent when a plot property is changed (i.e.<!-- --> 
     *  spectrum added/removed?).
     */
    public void plotChanged( PlotChangedEvent e );
}
