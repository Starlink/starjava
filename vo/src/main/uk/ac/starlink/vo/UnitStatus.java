package uk.ac.starlink.vo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import uk.me.nxg.unity.OneUnit;
import uk.me.nxg.unity.Syntax;
import uk.me.nxg.unity.UnitDefinition;
import uk.me.nxg.unity.UnitExpr;
import uk.me.nxg.unity.UnitParser;
import uk.me.nxg.unity.UnitParserException;
import uk.me.nxg.unity.UnitRepresentation;

/**
 * Categorises VOUnit validity.
 * The {@link #getStatus} method tests a unit string to determine whether
 * it conforms to the VOUnits standard.
 *
 * <p>Norman Gray's Unity library is used for VOUnits parsing.
 *
 * @author   Mark Taylor
 * @since    9 Jul 2021
 * @see  <a href="https://www.ivoa.net/documents/VOUnits/">VOUnits</a>
 */
public class UnitStatus {

    private final Code code_;
    private final String message_;
    private static final Map<String,UnitStatus> statusMap_ = createCache( 200 );

    /**
     * Constructor.
     *
     * @param  code   status code
     * @param  message  human-readable message supplying additional information
     */
    protected UnitStatus( Code code, String message ) {
        code_ = code;
        message_ = message;
    }

    /**
     * Returns a status element indicating conformance.
     *
     * @return  code
     */
    public Code getCode() {
        return code_;
    }

    /**
     * Returns a human-readable message supplying additional information.
     * The returned text does not in general report the original unit string.
     *
     * @return  message, or null if nothing to say
     */
    public String getMessage() {
        return message_;
    }

    /**
     * Returns the status for a given unit string.
     *
     * @param  unit  unit text
     * @return  status, or null for blank input
     */
    public static UnitStatus getStatus( String unit ) {
        return statusMap_.computeIfAbsent( unit, UnitStatus::createStatus );
    }

    /**
     * Does the work of parsing a Unit.
     *
     * @param  unit  unit text
     * @return  new status, or null for blank input
     */
    private static UnitStatus createStatus( String unit ) {
        Syntax syntax = Syntax.VOUNITS;
        if ( unit == null || unit.trim().length() == 0 ) {
            return null;
        }
        String cunit = unit.replaceAll( "\\s", "" );
        boolean hasWhitespace = ! cunit.equals( unit );
        UnitExpr punit;
        try {
            UnitParser parser = new UnitParser( syntax );
            parser.setGuessing( true );
            punit = parser.parse( cunit );
        }
        catch ( UnitParserException e ) {
            return new UnitStatus( Code.BAD_SYNTAX, e.getMessage() );
        }
        catch ( Throwable e ) {
            return new UnitStatus( Code.PARSE_ERROR, e.toString() );
        }
        if ( hasWhitespace ) {
            return new UnitStatus( Code.WHITESPACE,
                                   "Whitespace illegal at VOUnits 1.0" );
        }
        Map<String,String> unknown = new LinkedHashMap<>();
        List<String> deprecated = new ArrayList<>();
        for ( OneUnit word : punit ) {
            if ( ! word.isRecognisedUnit( syntax ) || word.wasGuessed() ) {
                String utxt = word.getOriginalUnitString();
                UnitDefinition udef = word.getBaseUnitDefinition();
                UnitRepresentation urep = udef == null
                                        ? null
                                        : udef.getRepresentation( syntax );
                String guess = urep == null ? null : urep.toString();
                unknown.put( utxt, guess );
            }
            if ( ! word.isRecommendedUnit( syntax ) ) {
                deprecated.add( word.getBaseUnitName() );
            }
        }
        if ( unknown.size() > 0 ) {
            List<String> items = new ArrayList<>();
            boolean allGuessed = true;
            for ( Map.Entry<String,String> unkEntry : unknown.entrySet() ) {
                String utxt = unkEntry.getKey();
                String guess = unkEntry.getValue();
                StringBuffer sbuf = new StringBuffer()
                   .append( '"' )
                   .append( utxt )
                   .append( '"' );
                if ( guess != null ) {
                    sbuf.append( " (" )
                        .append( "-> \"" )
                        .append( guess )
                        .append( "\"?)" );
                }
                else {
                    allGuessed = false;
                }
                items.add( sbuf.toString() );
            }
            String txt = ( unknown.size() == 1 ? "Unknown unit "
                                               : "Unknown units " )
                       + String.join( ", ", items );
            return new UnitStatus( allGuessed ? Code.GUESSED_UNIT
                                              : Code.UNKNOWN_UNIT,
                                   txt );
        }
        if ( ! punit.allUsageConstraintsSatisfied( syntax ) ) {
            return new UnitStatus( Code.USAGE, "Usage constraints violated" );
        }
        if ( deprecated.size() > 0 ) {
            String txt = deprecated.size() == 1
                       ? "Deprecated unit \"" + deprecated.get( 0 ) + "\""
                       : "Deprecated units " + deprecated;
            return new UnitStatus( Code.DEPRECATED, txt );
        }
        assert punit.isFullyConformant( syntax );
        return new UnitStatus( Code.OK, null );
    }

    /**
     * Returns an LRU cache suitable for storing UcdStatus values.
     *
     * @param  limit  maximum cache size
     * @return  new thread-safe map
     */
    private static Map<String,UnitStatus> createCache( final int limit ) {
        return Collections
              .synchronizedMap( new LinkedHashMap<String,UnitStatus>() {
            @Override
            public boolean removeEldestEntry( Map.Entry<String,UnitStatus>
                                              entry ) {
                return size() > limit;
            }
        } );
    }

    /**
     * Characterises VOUnits standard conformance.
     */
    public enum Code {

        /** Conforms to VOUnits standard. */
        OK( ' ' ),
 
        /** Contains some units deprecated in VOUnits standard. */
        // In most cases this means deprecated by the IAU.
        // Don't even class this as a warning, since it contains some
        // items like Angstrom and erg that are common and reasonable.
        DEPRECATED( ' ' ),

        /** Parsed as VOUnit but contains unknown base units. */
        UNKNOWN_UNIT( 'W' ),

        /** Parsed as VOUnit but contains unknown though guessable units. */
        GUESSED_UNIT( 'W' ),

        /** Cannot be parsed as VOUnit. */
        BAD_SYNTAX( 'E' ),

        /** Cannot be parsed as VOUnit (shouldn't happen). */
        PARSE_ERROR( 'E' ),

        /** Violates VOUnit usage constraints. */
        USAGE( 'E' ),

        /** Legal VOUnit except that it contains illegal whitespace. */
        WHITESPACE( 'E' );

        private final char stat_;

        /**
         * Constructor.
         *
         * @param  stat  status code: [E]rror, [W]arning or ' ' (ok)
         */
        private Code( char stat ) {
            stat_ = stat;
            switch ( stat ) {
                case ' ':
                case 'W':
                case 'E':
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }

        /**
         * Indicates whether this code represents an invalid VOUnit
         * specification.
         *
         * @return  true for error status
         */
        public boolean isError() {
            return stat_ == 'E';
        }

        /**
         * Indicates whether this status represents a UCD value which
         * may deserve attention, but is not actually a standards violation.
         * Note this includes use of unknown and non-standard,
         * as well as deprecated, units that are still syntactically
         * permissible.
         *
         * @return  true for warning status
         */
        public boolean isWarning() {
            return stat_ == 'W';
        }
    }
}
