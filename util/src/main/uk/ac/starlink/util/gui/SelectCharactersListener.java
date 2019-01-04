package uk.ac.starlink.util.gui;

import java.util.EventListener;

/**
 * SelectCharactersListener defines an interface used when listening
 * for a SelectCharacters window to send updated text.
 *
 * @since $Date$
 * @since 04-OCT-2000
 * @author Peter W. Draper
 * @version $Id$
 */
public interface SelectCharactersListener 
    extends EventListener 
{
    /**
     *  Invoked when new text in available.
     */
    public void newCharacters( SelectCharactersEvent e );
}
