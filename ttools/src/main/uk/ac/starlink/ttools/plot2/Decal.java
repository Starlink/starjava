package uk.ac.starlink.ttools.plot2;

import java.awt.Graphics;

/**
 * Like an Icon but less complicated.
 *
 * @author   Mark Taylor
 * @since    12 Feb 2013
 */
public interface Decal {

    /**
     * Paints some content onto a given Graphics context.
     * No position is supplied, so it's implicitly placed at the origin.
     *
     * @param   g  graphics context
     */
    void paintDecal( Graphics g );

    /**
     * Reports whether this decal is known to do only opaque drawing.
     *
     * @return  true if no transparent drawing will be done
     */
    boolean isOpaque();
}
