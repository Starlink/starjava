package uk.ac.starlink.ast.gui;

import java.util.EventObject;
import java.awt.Font;

/**
 * FontChangedEvent defines an event that passes on a new Font.
 *
 * @since $Date$
 * @since 08-NOV-2000
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 */
public class FontChangedEvent extends EventObject 
{
    /**
     * Constructs a FontChangedEvent object.
     *
     * @param source the source Object (typically this).
     * @param font the new font..
     */
    public FontChangedEvent( Object source, Font font ) 
    {
        super( source );
        this.font = font;
    }

    /**
     * The font
     */
    protected Font font = null;

    /**
     *  Return the font.
     */
    public Font getFont() 
    {
        return font;
    }
}
