package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.Pixer;

/**
 * Issues copies of a Pixer.
 *
 * @author   Mark Taylor
 * @since    23 Jul 2018
 */
public interface PixerFactory {

    /**
     * Returns a pixer.
     *
     * @return  pixer
     */
    public Pixer createPixer();
}
