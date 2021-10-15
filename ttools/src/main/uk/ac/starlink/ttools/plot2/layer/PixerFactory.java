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
     * Returns a pixer.  Every call to this method returns a
     * functionally identical object.
     *
     * @return  pixer
     */
    Pixer createPixer();

    /**
     * Returns the number of pixels over which each created pixer iterates.
     *
     * @return  pixel count
     */
    int getPixelCount();

    /**
     * Returns the lowest X value from each created pixer.
     *
     * @return  minimum X coordinate
     */
    int getMinX();

    /**
     * Returns the highest X value from each created pixer.
     *
     * @return  maximum X coordinate
     */
    int getMaxX();

    /**
     * Returns the lowest Y value from each created pixer.
     *
     * @return  minimum Y coordinate
     */
    int getMinY();

    /**
     * Returns the highest Y value from each created pixer.
     *
     * @return  maximum Y coordinate
     */
    int getMaxY();
}
