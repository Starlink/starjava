package uk.ac.starlink.ast.gui;

import java.util.EventListener;

/**
 * FontChangedListener defines an interface used when listening
 * for changes in a displayed font specification.
 *
 * @since $Date$
 * @since 08-NOV-2000
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 */
public interface FontChangedListener extends EventListener 
{
    /**
     * Invoked when the font is changed.
     */
    public void fontChanged( FontChangedEvent e );
}
