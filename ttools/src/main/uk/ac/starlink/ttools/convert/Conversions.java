package uk.ac.starlink.ttools.convert;

import java.util.regex.Pattern;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.votable.VOStarTable;

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
        String xtype = info instanceof ColumnInfo
                     ? (String) ((ColumnInfo) info)
                               .getAuxDatumValue( VOStarTable.XTYPE_INFO,
                                                  String.class )
                     : null;
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
                public Object unconvert( Object value ) {
                    return value;
                }
            };
        }

        /* Try to handle strings. */
        if ( String.class.isAssignableFrom( clazz ) ) {

            /* Try the VOTable 1.2 xtype attribute. */
            if ( xtype != null && xtype.trim().length() > 0 ) {
                if ( "iso8601".equals( xtype ) ||
                     ISO8601_UNIT_PATTERN.matcher( xtype ).matches() ||
                     "adql:TIMESTAMP".equalsIgnoreCase( xtype ) ) {
                    return new Iso8601ToDecimalYear( info );
                }
            }

            /* See if we recognise the units. */
            if ( units != null && units.trim().length() > 0 ) {

                /* Unquote the string.  This tackles a convention in use
                 * at CDS which uses values of the unit attribute in VOTables
                 * to represent non-standard values, e.g. unit='"hms"'.
                 * Also trim it for good measure. */
                units = unquote( units.trim() ).trim();
                if ( ISO8601_UNIT_PATTERN.matcher( units ).matches() ) {
                    return new Iso8601ToDecimalYear( info );
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
                ucd = ucd.trim();
                if ( ISO8601_UCD_PATTERN.matcher( ucd ).matches() ) {
                    return new Iso8601ToDecimalYear( info );
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

    /**
     * Removes matched single or double quotes from the ends of a string,
     * if they are present.  If they aren't, returns the original string.
     *
     * @param  str   quoted or unquoted string
     * @return  unquoted string
     */
    private static String unquote( String str ) {
        int leng = str.length();
        if ( ( leng > 1 ) &&
             ( ( str.charAt( 0 ) == '\'' && str.charAt( leng - 1 ) == '\'' ) ||
               ( str.charAt( 0 ) == '"' && str.charAt( leng - 1 ) == '"' ) ) ) {
            return str.substring( 1, leng - 1 );
        }
        else {
            return str;
        }
    }
}
