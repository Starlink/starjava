/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     04-OCT-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.util.EventObject;

/**
 * PlotChangedEvent defines an event that describes a change to the
 * status of a Plot.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class PlotChangedEvent 
    extends EventObject 
{    
    /**
     *  Used when a plot is created.
     */
    public static final int CREATED = 0;

    /**
     *  Used when a plot is removed.
     */
    public static final int REMOVED = 1;
    
    /**
     *  Used when plot is changed (i.e.&nbsp;a new spectrum is added).
     */
    public static final int CHANGED = 1;
    
    /**
     *  The type of event.
     */
    protected int type = CREATED;

    /**
     *  The index of plot changed.
     */
    protected int index = 0;

    /**
     *  Constructs a PlotChangedEvent object.
     *
     *  @param source - the source Object (typically this).
     *  @param type an int specifying CREATED, REMOVED or CHANGED.
     *  @param index - an int specifying the position of the changed
     *                 plot.
     */
    public PlotChangedEvent( Object source, int type, int index ) 
    {
        super( source );
        this.type = type;
        this.index = index;
    }

    /**
     *  Return type of event.
     */
    public int getType() 
    {
        return type;
    }

    /**
     *  Return index of plot changed.
     */
    public int getIndex() 
    {
        return index;
    }
}
