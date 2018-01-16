package uk.ac.starlink.ttools.plot2.layer;

/**
 * Unit for solid angle quantities.
 * Base unit is a square degree.
 *
 * @author   Mark Taylor
 * @since    16 Jan 2018
 */
public class SolidAngleUnit extends Unit {

    /** Steradian. */
    public static final SolidAngleUnit STERADIAN;

    /** Square degree. */
    public static final SolidAngleUnit DEGREE2;

    /** Square arcminute. */
    public static final SolidAngleUnit ARCMIN2;

    /** Square arcsecond. */
    public static final SolidAngleUnit ARCSEC2;

    /** Square milliarcsecond. */
    public static final SolidAngleUnit MAS2;

    /** Square microarcsecond. */
    public static final SolidAngleUnit UAS2;

    private static final SolidAngleUnit[] VALUES = {
        STERADIAN =
            new SolidAngleUnit( "steradian", "steradian", "sr",
                                AngleUnit.RADIAN,
                                "(180/Pi)^2 deg^2" ),
        DEGREE2 =
            new SolidAngleUnit( "degree2", "square degree", "deg**2",
                                AngleUnit.DEGREE,
                                "square degrees" ),
        ARCMIN2 =
            new SolidAngleUnit( "arcmin2", "square arcminute", "arcmin**2",
                                AngleUnit.MINUTE,
                                "square arcminute, (1/60)^2 deg^2" ),
        ARCSEC2 =
            new SolidAngleUnit( "arcsec2", "square arcsecond", "arcsec**2",
                                AngleUnit.ARCSEC,
                                "square arcsecond, (1/3600)^2 deg^2" ),
        MAS2 =
            new SolidAngleUnit( "mas2", "square milliarcsec", "mas**2",
                                AngleUnit.MAS,
                                "square milliarcsecond, (.001/3600)^2 deg^2" ),
        UAS2 =
            new SolidAngleUnit( "uas2", "square microarcsec", "uas**2",
                                AngleUnit.UAS,
                                "square microarcsecond, (1e-6/3600)^2 deg^2" ),
    };

    /**
     * Constructor.
     *
     * @param  label     text to appear in a selection interface
     * @param  textName  text to appear in user-directed descriptive text
     * @param  symbol    text to appear as unit metadata,
     *                   preferably compatible with the VOUnit standard
     * @param  linearAngleUnit   unit corresponding to the square root of this
     * @param  description  textual description
     */
    public SolidAngleUnit( String label, String textName, String symbol,
                           AngleUnit linearAngleUnit, String description ) {
        super( label, textName, symbol,
               Math.pow( linearAngleUnit.getValueInDegrees(), 2.0 ),
               description );
    }

    /**
     * Returns the extent of this unit in square degrees.
     *
     * @return  same as extent
     */
    public double getExtentInSquareDegrees() {
        return getExtent();
    }

    /**
     * Returns an array of the defined solid angle units.
     *
     * @return   list of known values
     */
    public static SolidAngleUnit[] getKnownUnits() {
        return VALUES.clone();
    }
}
