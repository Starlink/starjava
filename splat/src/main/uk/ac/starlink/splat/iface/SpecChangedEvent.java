package uk.ac.starlink.splat.iface;

import java.util.EventObject;

/**
 * SpecChangedEvent defines an event that describes a change to the
 * list of all known spectra.
 *
 * @since $Date$
 * @since 29-SEP-2000
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 */
public class SpecChangedEvent extends EventObject 
{
    /**
     *  Used when spectrum is added.
     */
    public static final int ADDED = 0;

    /**
     *  Used when spectrum is removed.
     */
    public static final int REMOVED = 1;
    
    /**
     *  Used when spectrum is changed (i.e. needs redrawing).
     */
    public static final int CHANGED = 1;
    
    /**
     *  Used when spectrum becomes current.
     */
    public static final int CURRENT = 1;
    
    /**
     *  The type of event.
     */
    protected int type = ADDED;

    /**
     *  The index of spectrum changed.
     */
    protected int index = 0;

    /**
     *  Constructs a SpecChangedEvent object.
     *
     *  @param source - the source Object (typically this).
     *  @param type an int specifying ADDED, REMOVED or CHANGED.
     *  @param index - an int specifying the position of the changed
     *                 spectrum.
     */
    public SpecChangedEvent( Object source, int type, int index ) 
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
     *  Return index of spectrum changed.
     */
    public int getIndex() 
    {
        return index;
    }
}
