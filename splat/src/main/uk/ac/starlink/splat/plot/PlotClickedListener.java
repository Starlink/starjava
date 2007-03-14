/*
 * Copyright (C) 2007 Particle Physics and Astronomy Research Council
 *
 *  History:
 *     13-MAR-2007 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.plot;

import java.util.EventListener;
import java.awt.event.MouseEvent;

/**
 * PlotClickedListener defines a interface for listening for clicks on a
 * {@link DivaPlot}.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public interface PlotClickedListener 
    extends EventListener 
{
    /**
     * Handle a instance of a {@link DivaPlot} mouse click.
     */
    public void plotClicked( MouseEvent e );
}
