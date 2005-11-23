package uk.ac.starlink.topcat.plot;

import java.awt.Graphics;

/**
 * Defines a hook for modifying a graphics context.
 *
 * @author   Mark Taylor
 * @since    23 Nov 2005
 */
public interface GraphicsTweaker {

    /**
     * Returns a graphics context based on an existing one but perhaps with
     * some modifications.
     * The submitted context <code>g</code> must not be modified;
     * if the return value is to be different from the input value,
     * then a cloned context should be returned.
     *
     * @param   g  graphics context
     * @see     java.awt.Graphics#create()
     */
    Graphics tweak( Graphics g );
}
