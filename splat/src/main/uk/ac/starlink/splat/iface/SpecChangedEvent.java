/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     29-SEP-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.util.EventObject;

/**
 * SpecChangedEvent defines an event that describes a change to the
 * list of all known spectra.
 *
 * @author Peter W. Draper
 * @version $Id$
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
     *  Used when spectrum is changed.
     */
    public static final int CHANGED = 2;
    
    /**
     *  Used when spectrum is modified. Total redraw required.
     */
    public static final int MODIFIED = 3;

    /**
     *  Used when spectrum becomes current.
     */
    public static final int CURRENT = 4;
    
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
     *  @param type one of the defined ints.
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
