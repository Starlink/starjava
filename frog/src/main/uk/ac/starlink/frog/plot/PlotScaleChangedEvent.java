package uk.ac.starlink.frog.plot;

import java.util.EventObject;

/**
 * PlotScaleChangedEvent defines an event that is sent when a Plot
 * changes it drawing scale.
 *
 * @since $Date$
 * @since 08-JAN-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 */
public class PlotScaleChangedEvent extends EventObject 
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
