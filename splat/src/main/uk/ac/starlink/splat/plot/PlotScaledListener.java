/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     08-JAN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.plot;

import java.util.EventListener;

/**
 * PlotScaledListener defines an interface used when listening for 
 * when a Plot changes its drawing scale.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public interface PlotScaledListener 
    extends EventListener 
{
    public void plotScaleChanged( PlotScaleChangedEvent e );
}
