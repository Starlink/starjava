package uk.ac.starlink.tfcat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

/**
 * Represents a TFCat CRS (Coordinate Reference System) structure.
 *
 * @author   Mark Taylor
 * @since    9 Feb 2022
 */
public class Crs {

    private final String type_;
    private final TimeCoords timeCoords_;
    private final SpectralCoords spectralCoords_;
    private final RefPosition refPosition_;

    /** Permitted values for CRS type attribute. */
    public static Collection<String> CRS_TYPES =
            Collections
           .unmodifiableSet( new LinkedHashSet<String>( Arrays.asList( 
        "Time-Frequency", "Time-Wavelength", "Time-Energy", "Time-Wavenumber"
    ) ) );

    /**
     * Constructor.
     *
     * @param  type  crs type name
     * @param  timeCoords   time coordinate system description
     * @param  spectralCoords  spectral coordinate system description
     * @param  refPosition   reference position description
     */
    public Crs( String type, TimeCoords timeCoords,
                SpectralCoords spectralCoords, RefPosition refPosition ) {
        type_ = type;
        timeCoords_ = timeCoords;
        spectralCoords_ = spectralCoords;
        refPosition_ = refPosition;
    }

    /**
     * Returns the type attribute for this CRS.
     * This should be, but is not guaranteed to be,
     * one of the {@link #CRS_TYPES}.
     *
     * @return  type name
     */
    public String getType() {
        return type_;
    }

    /**
     * Returns the time coordinate system description for this CRS.
     *
     * @return  time coords
     */
    public TimeCoords getTimeCoords() {
        return timeCoords_;
    }

    /**
     * Returns the spectral coordinate system description for this CRS.
     *
     * @return  spectral coords
     */
    public SpectralCoords getSpectralCoords() {
        return spectralCoords_;
    }

    /**
     * Returns the reference position for this CRS.
     *
     * @return  reference position
     */
    public RefPosition getRefPosition() {
        return refPosition_;
    }
}
