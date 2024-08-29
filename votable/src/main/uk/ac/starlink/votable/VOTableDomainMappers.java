package uk.ac.starlink.votable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.TimeMapper;
import uk.ac.starlink.table.ValueInfo;

/**
 * Utility class for identifying domain mappers for VOTable columns.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2013
 */
class VOTableDomainMappers {

    /** Regex in unit string that might indicate ISO-8601 values. */
    static final Pattern ISO8601_UNIT_PATTERN =
        Pattern.compile( "iso.?8601", Pattern.CASE_INSENSITIVE );

    /** Regex in UCD that might indicate ISO-8601 values. */
    static final Pattern ISO8601_UCD_PATTERN =
        Pattern.compile( "time(\\.(epoch|start|end))?(;.*)?|TIME_DATE(_.*)?",
                         Pattern.CASE_INSENSITIVE );

    /** Regex in Utype that looks like an ObsCore MJD-mandated value. */
    static final Pattern OBSCORE_T_UTYPE_PATTERN =
        Pattern.compile( ".*Char\\.TimeAxis\\.Coverage\\.Bounds\\.Limits.*",
                         Pattern.CASE_INSENSITIVE );

    /**
     * Identifies suitable DomainMapper objects to associate with a column.
     *
     * @param  info  column basic metadata
     * @return  array of mappers; may be empty
     */
    public static DomainMapper[] getMappers( ValueInfo info ) {
        TimeMapper tmapper = getTimeMapper( info );
        return tmapper == null ? new DomainMapper[ 0 ]
                               : new DomainMapper[] { tmapper };
    }

    /**
     * Tries to identify a TimeMapper to associated with a column.
     *
     * @param  info  column basic metadata
     * @return  domain mapper for time data, or null if not successful
     */
    private static TimeMapper getTimeMapper( ValueInfo info ) {
        Class<?> clazz = info.getContentClass();
        String units = info.getUnitString();
        String ucd = info.getUCD();
        String utype = info.getUtype();
        String xtype = info.getXtype();
        if ( xtype == null ) {
            xtype = "";
        }
        if ( units == null ) {
            units = "";
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
        units = unquote( units.trim() ).trim();

        /* If it's a string, see if it looks like an ISO-8601 date. */
        if ( String.class.isAssignableFrom( clazz ) ) {
            if ( "iso8601".equals( xtype ) ||
                 ISO8601_UNIT_PATTERN.matcher( xtype ).matches() ||
                 "adql:TIMESTAMP".equalsIgnoreCase( xtype ) ||
                 "timestamp".equalsIgnoreCase( xtype ) ||
                 ISO8601_UNIT_PATTERN.matcher( units ).matches() ||
                 ISO8601_UCD_PATTERN.matcher( ucd ).matches() ) {
                return TimeMapper.ISO_8601;
            }
        }

        /* Look for metadata imported from TIMESYS elements, which is how
         * it's supposed to be done post VOTable 1.4.  Just use the timeorigin,
         * don't attempt anything clever with the refposition or timescale.
         * Note that this ignores TIMESYS references where timeorigin is
         * not set; that corresponds to an absolute date in years from 0AD.
         * If timescale and refposition are to be extracted in future,
         * handling for those cases will need to be added. */
        Timesys tsys = Timesys.getTimesys( info );
        if ( tsys != null && Number.class.isAssignableFrom( clazz ) ) {
            double jdOrigin = tsys.getTimeorigin();
            final double unitSec = getUnitInSeconds( units );
            if ( ! Double.isNaN( unitSec ) &&
                 ! Double.isNaN( jdOrigin ) ) {
                double unixDayOrigin = jdOrigin - 2440587.5;
                final double unixSecOrigin = unixDayOrigin * 60 * 60 * 24;
                return new TimeMapper( clazz, "TIMESYS", tsys.toString() ) {
                    public double toUnixSeconds( Object sourceValue ) {
                        double val = sourceValue instanceof Number
                                   ? ((Number) sourceValue).doubleValue()
                                   : Double.NaN;
                        return Double.isNaN( val )
                             ? Double.NaN
                             : ( val * unitSec ) + unixSecOrigin;
                    }
                };
            }
        }

        /* Otherwise try some ad hoc numeric domains. */
        if ( Number.class.isAssignableFrom( clazz ) ) {
            if ( "mjd".equalsIgnoreCase( xtype ) ||
                 ( OBSCORE_T_UTYPE_PATTERN.matcher( utype ).matches() && 
                   "d".equals( units ) ) ) {
                return TimeMapper.MJD;
            }
            else if ( "jd".equalsIgnoreCase( xtype ) ) {
                return TimeMapper.JD;
            }
            else if ( "yr".equals( units ) ||
                      "a".equals( units ) ||
                      "year".equals( units ) ) {
                return TimeMapper.DECIMAL_YEAR;
            }
        }

        /* There may be some other conventions that I could spot here -
         * ask CDS for known indicators of epoch-like columns? */
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

    /**
     * Returns the numeric size of a unit in seconds given the VOTable
     * units attribute value.
     *
     * <p><strong>Note:</strong> this does not currently implement
     * all of VOUnits; for instance SI prefixes (kilodays etc)
     * are not understood except for a few special cases.
     *
     * @param  units  unit string
     * @return   extent of unit in seconds, or NaN if not understood
     */
    private static double getUnitInSeconds( String units ) {
        if ( "ps".equals( units ) ) {
            return 1e-12;
        }
        else if ( "ns".equals( units ) ) {
            return 1e-9;
        }
        else if ( "us".equals( units ) ) {
            return 1e-6;
        }
        else if ( "ms".equals( units ) ) {
            return 1e-3;
        }
        else if ( "s".equals( units ) ) {
            return 1.0;
        }
        else if ( "ks".equals( units ) ) {
            return 1e3;
        }
        else if ( "d".equals( units ) ) {
            return 60 * 60 * 24;
        }
        else if ( "a".equals( units ) ||
                  "yr".equals( units ) ||
                  "year".equals( units ) ) {
            return 60 * 60 * 24 * 365.25;
        }
        else {
            return Double.NaN;
        }
    }
}
