package uk.ac.starlink.util.gui;

import java.util.EventObject;

/**
 * SelectCharactersEvent defines an event that passes on new text that
 * has been created.
 *
 * @since $Date$
 * @since 03-NOV-2000
 * @author Peter W. Draper
 * @version $Id$
 */
public class SelectCharactersEvent 
    extends EventObject
{
    /**
     * Constructs a SelectCharactersEvent object.
     *
     * @param source the source Object (typically this).
     * @param text the new text.
     */
    public SelectCharactersEvent( Object source, String text )
    {
        super( source );
        this.text = text;
    }

    /**
     * The text.
     */
    protected String text = null;

    /**
     *  Return the event text.
     */
    public String getText()
    {
        return text;
    }
}
