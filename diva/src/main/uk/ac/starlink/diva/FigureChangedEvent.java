/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     08-JAN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.diva;

import java.util.EventObject;
import diva.canvas.event.LayerEvent;

/**
 * FigureChangedEvent defines an event that describes a change to a
 * figure drawn on a {@link Draw} instance.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class FigureChangedEvent 
    extends EventObject 
{
    /**
     *  Used when a figure is created.
     */
    public static final int CREATED = 0;

    /**
     *  Used when a figure is removed.
     */
    public static final int REMOVED = 1;
    
    /**
     *  Used when figure is changed (transformed).
     */
    public static final int CHANGED = 1;
    
    /**
     *  The type of event.
     */
    protected int type = CREATED;

    /**
     *  The LayerEvent associated with the change to the Figure.
     */
    protected LayerEvent layerEvent = null;

    /**
     *  Constructs a FigureChangedEvent object.
     *
     *  @param source the source Figure.
     *  @param type an int specifying CREATED, REMOVED or CHANGED.
     *  @param le the LayerEvent associated with the Figure change.
     */
    public FigureChangedEvent( Object source, int type, 
                               LayerEvent le ) 
    {
        super( source );
        this.type = type;
        this.layerEvent = le;
    }

    /**
     * Constructs a FigureChangedEvent object, suitable when no
     * LayerEvent is associated.
     *
     *  @param source the source Figure.
     *  @param type an int specifying CREATED, REMOVED or CHANGED.
     */
    public FigureChangedEvent( Object source, int type ) 
    {
        super( source );
        this.type = type;
    }

    /**
     *  Return type of event.
     */
    public int getType() 
    {
        return type;
    }

    /**
     *  Return the LayerEvent that was associated with the change.
     */
    public LayerEvent getLayerEvent()
    {
        return this.layerEvent;
    }
}
