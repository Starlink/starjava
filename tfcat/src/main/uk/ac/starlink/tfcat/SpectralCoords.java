package uk.ac.starlink.tfcat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Represents a TFCat SpectralCoords object.
 *
 * @author   Mark Taylor
 * @since    9 Feb 2022
 */
public interface SpectralCoords {

    /** Permitted values for SpectralCoords type attribute. */
    public static final Collection<String> TYPE_VALUES =
            Collections.unmodifiableList( Arrays.asList(
        "frequency", "wavelength", "energy", "wavenumber"
    ) );

    /** Permitted values for SpectralCoords scale attribute. */
    public static final Collection<String> SCALE_VALUES =
        Collections.unmodifiableList( Arrays.asList( "linear", "log" ) );

    /**
     * Returns the spectral coordinate system type.
     * This should be one of the members of {@link #TYPE_VALUES},
     * currently "frequency", "wavelength", "energy" or "wavenumber".
     *
     * @return  system type
     */
    public String getType();

    /**
     * Returns the unit string for this coordinate system.
     *
     * @return  unit
     */
    public String getUnit();

    /**
     * Returns a token giving the scale type for the spectral coordinate
     * system.
     * This should be one of the members of {@link #SCALE_VALUES},
     * currently "linear" or "log".
     *
     * @return  scaling type
     */
    public String getScale();
}
