package uk.ac.starlink.tfcat;

/**
 * Coordinate Reference System.
 *
 * @author   Mark Taylor
 * @since    1 Jul 2022
 */
public interface Crs {

    /**
     * Returns the type member of this CRS object.
     * Types "local", "link" and "name" are defined.
     *
     * @return  type
     */
    String getCrsType();
}
