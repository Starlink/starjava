package uk.ac.starlink.fits;

/**
 * Aggregates configuration options for FITS table serialization.
 *
 * @author   Mark Taylor
 * @since    13 May 2021
 */
public interface FitsTableSerializerConfig {

    /**
     * Indicates how byte values will be written.
     * If true, byte values are written as FITS unsigned bytes with an
     * offset as discussed in the FITS standard
     * (<code>TFORM=B</code>, <code>TZERO=-128</code>);
     * if false, they are written as signed shorts
     * (<code>TFORM=I</code>).
     *
     * @return   true to write as bytes, false to write as shorts
     */
    boolean allowSignedByte();

    /**
     * Indicates whether columns with TFORMn=0A may be output.
     * Such columns are explicitly permitted by the FITS standard,
     * but they cause failures in some versions of CFITSIO.
     *
     * @return  false to block TFORMn=0A column output, true to allow it
     */
    boolean allowZeroLengthString();

    /**
     * Returns the convention for representing over-wide tables.
     *
     * @return   wide table convention, or null to avoid writing wide tables
     */
    WideFits getWide();
}
