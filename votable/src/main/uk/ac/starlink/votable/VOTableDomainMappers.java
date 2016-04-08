package uk.ac.starlink.votable;

import java.util.ArrayList;
import java.util.List;
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

    /**
     * Identifies suitable DomainMapper objects to associate with a column.
     *
     * @param  info  column basic metadata
     * @param  xtype  column xtype value
     */
    public static DomainMapper[] getMappers( ValueInfo info, String xtype ) {
        List<DomainMapper> mappers = new ArrayList<DomainMapper>();
        Class clazz = info.getContentClass();
        String units = info.getUnitString();
        String ucd = info.getUCD();
        if ( xtype == null ) {
            xtype = "";
        }
        if ( units == null ) {
            units = "";
        }

        /* Unquote the string.  This tackles a convention in use
         * at CDS which uses values of the unit attribute in VOTables
         * to represent non-standard values, e.g. unit='"hms"'.
         * Also trim it for good measure. */
        units = unquote( units.trim() ).trim();
        if ( ucd == null ) {
            ucd = "";
        }

        /* If it's a string, see if it looks like an ISO-8601 date. */
        if ( String.class.isAssignableFrom( clazz ) ) {
            if ( "iso8601".equals( xtype ) ||
                 ISO8601_UNIT_PATTERN.matcher( xtype ).matches() ||
                 "adql:TIMESTAMP".equalsIgnoreCase( xtype ) ||
                 "timestamp".equalsIgnoreCase( xtype ) ||
                 ISO8601_UNIT_PATTERN.matcher( units ).matches() ||
                 ISO8601_UCD_PATTERN.matcher( ucd ).matches() ) {
                mappers.add( TimeMapper.ISO_8601 );
            }
        }

        /* Try some numeric time domains. */
        if ( Number.class.isAssignableFrom( clazz ) ) {
            if ( "mjd".equalsIgnoreCase( xtype ) ) {
                mappers.add( TimeMapper.MJD );
            }
            else if ( "jd".equalsIgnoreCase( xtype ) ) {
                mappers.add( TimeMapper.JD );
            }
            else if ( "yr".equals( units ) ||
                      "a".equals( units ) ||
                      "year".equals( units ) ) {
                mappers.add( TimeMapper.DECIMAL_YEAR );
            }
        }

        /* There may be some other conventions that I could spot here -
         * ask CDS for known indicators of epoch-like columns? */

        return mappers.toArray( new DomainMapper[ 0 ] );
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
