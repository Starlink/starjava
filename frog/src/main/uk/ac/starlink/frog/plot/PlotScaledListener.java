package uk.ac.starlink.frog.plot;

import java.util.EventListener;

/**
 * PlotScaledListener defines an interface used when listening for 
 * when a Plot changes its drawing scale.
 *
 * @since $Date$
 * @since 08-JAN-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 */
public interface PlotScaledListener extends EventListener 
{
    public void plotScaleChanged( PlotScaleChangedEvent e );
}
