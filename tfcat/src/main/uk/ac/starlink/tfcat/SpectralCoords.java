package uk.ac.starlink.tfcat;

/**
 * Represents a TFCat SpectralCoords object.
 *
 * @author   Mark Taylor
 * @since    9 Feb 2022
 */
public interface SpectralCoords {

    /**
     * Returns the spectral coordinate system name.
     *
     * @return  system name
     */
    public String getName();

    /**
     * Returns the unit string for this coordinate system.
     *
     * @return  unit
     */
    public String getUnit();
}
