package uk.ac.starlink.tfcat;

/**
 * Represents a TFCat Local CRS (Coordinate Reference System) structure.
 *
 * @author   Mark Taylor
 * @since    28 Jun 2022
 */
public interface LocalCrs extends Crs {

    /**
     * Returns the standard time coordinate system identifier for this CRS.
     * May be null if the <code>time_coords</code> property was supplied.
     *
     * @return  time coords ID
     */
    String getTimeCoordsId();

    /**
     * Returns the time coordinate system description for this CRS.
     *
     * <p>For a legal LocalCrs instance this should always be non-null;
     * if the <code>time_coords_id</code> property has been supplied
     * instead of the <code>time_coords</code> property, the return value
     * will be one of the pre-defined instances.
     *
     * @return  time coords object
     */
    TimeCoords getTimeCoords();

    /**
     * Returns the spectral coordinate system description for this CRS.
     *
     * @return  spectral coords object
     */
    SpectralCoords getSpectralCoords();

    /**
     * Returns the reference position ID for this CRS.
     *
     * @return  refernce position ID
     */
    String getRefPositionId();
}
