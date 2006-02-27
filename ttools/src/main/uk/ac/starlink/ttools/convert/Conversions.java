package uk.ac.starlink.ttools.convert;

import java.util.regex.Pattern;
import uk.ac.starlink.table.ValueInfo;

/**
 * Utility functions for converting between value types.
 *
 * @author   Mark Taylor
 * @since    24 Feb 2006
 */
public class Conversions {

    private static final Pattern ISO8601_UNIT_PATTERN =
        Pattern.compile( "iso.?8601", Pattern.CASE_INSENSITIVE );
    private static final Pattern HMS_UNIT_PATTERN =
        Pattern.compile( "h+[: ]?m+[: ]?s+(\\..*)?", Pattern.CASE_INSENSITIVE );
    private static final Pattern DMS_UNIT_PATTERN =
        Pattern.compile( "d+[: ]?m+[: ]?s+(\\..*)?", Pattern.CASE_INSENSITIVE );

    private static final Pattern ISO8601_UCD_PATTERN =
        Pattern.compile( "time|time\\.epoch(\\..*)?|TIME_DATE(_.*)?",
                         Pattern.CASE_INSENSITIVE );
    private static final Pattern HMS_UCD_PATTERN =
        Pattern.compile( "POS_EQ_RA.*|pos\\.eq\\.ra.*",
                         Pattern.CASE_INSENSITIVE );
    private static final Pattern DMS_UCD_PATTERN =
        Pattern.compile( "POS_EQ_DEC.*|pos\\.eq\\.dec.*",
                         Pattern.CASE_INSENSITIVE );

    /**
     * Returns a converter from the given ValueInfo to a numeric quantity.
     *
     * @param   info  input quantity metadata
     * @return  converter which can convert <code>info</code> quantities to
     *          a numeric form, or <code>null</code> if we don't know how
     *          to do it
     */
    public static ValueConverter getNumericConverter( final ValueInfo info ) {
        String units = info.getUnitString();
        String ucd = info.getUCD();
        Class clazz = info.getContentClass();

        /* If it's numeric, no problem. */
        if ( Number.class.isAssignableFrom( clazz ) ) {
            return new ValueConverter() {
                public ValueInfo getInputInfo() {
                    return info;
                }
                public ValueInfo getOutputInfo() {
                    return info;
                }
                public Object convert( Object value ) {
                    return value;
                }
            };
        }

        /* Try to handle strings. */
        if ( String.class.isAssignableFrom( clazz ) ) {

            /* See if we recognise the units. */
            if ( units != null && units.trim().length() > 0 ) {
                if ( ISO8601_UNIT_PATTERN.matcher( units ).matches() ) {
                    return new Iso8601ToJulian( info );
                }
                else if ( DMS_UNIT_PATTERN.matcher( units ).matches() ) {
                    return new SexagesimalToDegrees( info, false );
                }
                else if ( HMS_UNIT_PATTERN.matcher( units ).matches() ) {
                    return new SexagesimalToDegrees( info, true );
                }
            }

            /* See if there's a clue in the UCD. */
            if ( ucd != null && ucd.trim().length() > 0 ) {
                if ( ISO8601_UCD_PATTERN.matcher( ucd ).matches() ) {
                    return new Iso8601ToJulian( info );
                }
                else if ( DMS_UCD_PATTERN.matcher( ucd ).matches() ) {
                    return new SexagesimalToDegrees( info, false );
                }
                else if ( HMS_UCD_PATTERN.matcher( ucd ).matches() ) {
                    return new SexagesimalToDegrees( info, true );
                }
            }
        }

        /* Nope. */
        return null;
    }
}
