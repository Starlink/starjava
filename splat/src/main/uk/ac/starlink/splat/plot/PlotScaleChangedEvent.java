/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     08-JAN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.plot;

import java.util.EventObject;

/**
 * PlotScaleChangedEvent defines an event that is sent when a Plot
 * changes it drawing scale.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class PlotScaleChangedEvent 
    extends EventObject 
{
    /**
     *  Constructs a PlotScaleChangedEvent object.
     *
     *  @param source the source Plot.
     */
    public PlotScaleChangedEvent( Object source ) 
    {
        super( source );
    }
}
