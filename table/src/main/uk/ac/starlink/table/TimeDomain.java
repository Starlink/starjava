package uk.ac.starlink.table;

import java.util.regex.Pattern;

/**
 * Domain representing epochs in a common time scale.
 *
 * <p>The sole instance of this singleton class is available as the
 * {@link #INSTANCE} static member.
 *
 * @author   Mark Taylor
 * @since    14 Apr 2020
 */
public class TimeDomain implements Domain<TimeMapper> {

    /** Regex in unit string that might indicate ISO-8601 values. */
    private static final Pattern ISO8601_UNIT_PATTERN =
        Pattern.compile( "iso.?8601", Pattern.CASE_INSENSITIVE );

    /** Regex in UCD that might indicate ISO-8601 values. */
    private static final Pattern ISO8601_UCD_PATTERN =
        Pattern.compile( "time(\\.(epoch|start|end))?(;.*)?|TIME_DATE(_.*)?",
                         Pattern.CASE_INSENSITIVE );

    /** Regex in Utype that looks like an ObsCore MJD-mandated value. */
    private static final Pattern OBSCORE_T_UTYPE_PATTERN =
        Pattern.compile( ".*Char\\.TimeAxis\\.Coverage\\.Bounds\\.Limits.*",
                         Pattern.CASE_INSENSITIVE );

    /** Singleton instance. */
    public static TimeDomain INSTANCE = new TimeDomain();

    /**
     * Private constructor prevents external instantiation.
     */
    private TimeDomain() {
    }

    public String getDomainName() {
        return "Time";
    }

    public TimeMapper[] getMappers() {
        return TimeMapper.getTimeMappers();
    }

    public TimeMapper getProbableMapper( ValueInfo info ) {
        if ( info == null ) {
            return null;
        }
        for ( DomainMapper mapper : info.getDomainMappers() ) {
            if ( mapper instanceof TimeMapper ) {
                return (TimeMapper) mapper;
            }
        }
        Class<?> clazz = info.getContentClass();
        String unit = info.getUnitString();
        String ucd = info.getUCD();
        String utype = info.getUtype();
        String xtype = info.getXtype();
        if ( xtype == null ) {
            xtype = "";
        }
        if ( unit == null ) {
            unit = "";
        }
        if ( ucd == null ) {
            ucd = "";
        }
        if ( utype == null ) {
            utype = "";
        }

        /* Unquote the string.  This tackles a convention in use
         * at CDS which uses values of the unit attribute in VOTables
         * to represent non-standard values, e.g. unit='"hms"'.
         * Also trim it for good measure. */
        unit = unquote( unit.trim() ).trim();
        unit = unquote( unit.trim() ).trim();

        /* If it's a string, see if it looks like an ISO-8601 date. */
        if ( String.class.isAssignableFrom( clazz ) ) {
            if ( "iso8601".equals( xtype ) ||
                 ISO8601_UNIT_PATTERN.matcher( xtype ).matches() ||
                 "adql:TIMESTAMP".equalsIgnoreCase( xtype ) ||
                 "timestamp".equalsIgnoreCase( xtype ) ||
                 ISO8601_UNIT_PATTERN.matcher( unit ).matches() ||
                 ISO8601_UCD_PATTERN.matcher( ucd ).matches() ) {
                return TimeMapper.ISO_8601;
            }
        }

        /* Otherwise try some ad hoc numeric domains. */
        if ( Number.class.isAssignableFrom( clazz ) ) {
            if ( "mjd".equalsIgnoreCase( xtype ) ||
                 ( OBSCORE_T_UTYPE_PATTERN.matcher( utype ).matches() &&
                   "d".equals( unit ) ) ) {
                return TimeMapper.MJD;
            }
            else if ( "jd".equalsIgnoreCase( xtype ) ) {
                return TimeMapper.JD;
            }
            else if ( "yr".equals( unit ) ||
                      "a".equals( unit ) ||
                      "year".equals( unit ) ) {
                return TimeMapper.DECIMAL_YEAR;
            }
        }
        return null;
    }


    public TimeMapper getPossibleMapper( ValueInfo info ) {
        if ( info == null ) {
            return null;
        }
        for ( DomainMapper mapper : info.getDomainMappers() ) {
            if ( mapper instanceof TimeMapper ) {
                return (TimeMapper) mapper;
            }
        }
        Class<?> clazz = info.getContentClass();
        String unit = info.getUnitString();
        if ( String.class.equals( clazz ) ) {
            return TimeMapper.ISO_8601;
        }
        else if ( Float.class.equals( clazz ) ||
                  Double.class.equals( clazz ) ||
                  Integer.class.equals( clazz ) ||
                  Long.class.equals( clazz ) ) {
            if ( "a".equals( unit ) ||
                 "yr".equals( unit ) ||
                 "year".equals( unit ) ) {
                return TimeMapper.DECIMAL_YEAR;
            }
            else if ( "d".equals( unit ) ||
                      "day".equals( unit ) ) {
                return TimeMapper.MJD;
            }
            else if ( "s".equals( unit ) ||
                      "sec".equals( unit ) ||
                      "second".equals( unit ) ) {
                return TimeMapper.UNIX_SECONDS;
            }
            else {
                return TimeMapper.MJD;
            }
        }
        else {
            return null;
        }
    }

    @Override
    public String toString() {
        return getDomainName();
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
