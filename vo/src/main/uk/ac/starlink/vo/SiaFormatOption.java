package uk.ac.starlink.vo;

/**
 * Encapsulates options for record format requirements used in an SIA query.
 *
 * <p>This is a bit of a historical relic: in SIAv1 FORMAT was one of the
 * most important options, but in SIAv2 there are many options and
 * FORMAT doesn't necessarily warrant special treatment.
 * This is here to enable pluggability between SIAv1 and v2
 * without disruption to the UI, but it may be replaced with a more
 * comprehensive upgrade to SIAv2 handling in future.
 *
 * @since   12 Mar 2020
 * @author  Mark Taylor
 */
public class SiaFormatOption {

    private final String v1Value_;
    private final String[] v2Values_;

    public static final SiaFormatOption ALL;
    public static final SiaFormatOption GRAPHIC;
    public static final SiaFormatOption FITS;
    public static final SiaFormatOption DATALINK;

    private static final SiaFormatOption[] OPTIONS = new SiaFormatOption[] {
        ALL = new SiaFormatOption( "ALL", new String[ 0 ] ),
        GRAPHIC = new SiaFormatOption( "GRAPHIC", new String[] {
            "image/png", "image/jpeg", "image/gif",
        } ),
        FITS = new SiaFormatOption( "image/fits" ),
        DATALINK = new SiaFormatOption( "application/x-votable+xml"
                                      + ";content=datalink" ),
    };

    /**
     * Constructs an option instance with a single value,
     * suitable for both SIAv1 and SIAv2.
     *
     * @param  value  required FORMAT parameter value
     */
    public SiaFormatOption( String value ) {
        this( value, new String[] { value } );
    }

    /**
     * Constructs a general format option.
     *
     * @param  v1Value  value for use in SIAv1 queries
     * @param  v2Values  list of values to be ORed together for SIAv2 queries
     */
    public SiaFormatOption( String v1Value, String[] v2Values ) {
        v1Value_ = v1Value;
        v2Values_ = v2Values;
    }

    /**
     * Returns value for use in SIAv1 queries.
     *
     * @return  SIAv1 FORMAT parameter value
     */
    public String getSiav1Value() {
        return v1Value_;
    }

    /**
     * Returns list of values for use in SIAv2 queries.
     *
     * @return  list of values for repeated SIAv2 FORMAT parameters
     */
    public String[] getSiav2Values() {
        return v2Values_.clone();
    }

    @Override
    public String toString() {
        return v1Value_;
    }

    /**
     * Returns a list of standard options suitable for use in an SIA UI.
     *
     * @return  standard format options
     */
    public static SiaFormatOption[] getStandardOptions() {
        return OPTIONS.clone();
    }

    /**
     * Tries to turn an object into an SiaFormatObject.
     * The input object may already be an SiaFormatObject or may be a String.
     * This method is intended for use with an editable JComboBox
     * containing SiaFormatOption objects.
     *
     * @param   formatOrString  object that may represent an SiaFormatObject,
     *                          either an SiaFormatObject or a FORMAT value
     * @return  corresponding SiaFormatObject, or null
     */
    public static SiaFormatOption fromObject( Object formatOrString ) {
        if ( formatOrString instanceof SiaFormatOption ) {
            return (SiaFormatOption) formatOrString;
        }
        else if ( formatOrString instanceof String &&
                  ((String) formatOrString).trim().length() > 0 ) {
            String formatTxt = ((String) formatOrString).trim();
            for ( SiaFormatOption opt : OPTIONS ) {
                if ( opt.toString().equalsIgnoreCase( formatTxt ) ) {
                    return opt;
                }
            }
            return new SiaFormatOption( ((String) formatOrString).trim() );
        }
        else {
            return null;
        }
    }
}
