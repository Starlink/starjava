package uk.ac.starlink.ttools.votlint;

import ari.ucidy.UCD;
import ari.ucidy.UCDParser;
import ari.ucidy.UCDWord;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Categorises UCD validity.
 * The {@link #getStatus} method tests a UCD string to determine whether
 * it conforms to standards, and returns an object that reports this.
 * UCD1+ and UCD1 are recognised.
 *
 * <p>Gregory Mantelet's Ucidy library is used for UCD1+ parsing.
 * 
 * @author   Mark Taylor
 * @since    9 Jul 2021
 */
public class UcdStatus {

    private final Code code_;
    private final String message_;

    private static final Map<String,UcdStatus> statusMap_ = createCache( 200 );
    private static final UCDParser ucdParser_ = createParser();
    private static final Collection<String> ucd1s_ =
        Collections.unmodifiableSet( readUcd1s() );
    private static final Pattern VOX_REGEX =
        Pattern.compile( "VOX:[A-Za-z_]+" );

    /**
     * Constructor.
     *
     * @param  code   status code
     * @param  message  human-readable message supplying additional information
     */
    protected UcdStatus( Code code, String message ) {
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
     * The returned text does not in general report the original UCD string.
     *
     * @return  message, or null if nothing to say (OK status)
     */
    public String getMessage() {
        return message_;
    }

    /**
     * Returns the status for a given UCD string.
     *
     * @param  ucd  UCD text
     * @return  status, or null for blank input
     */
    public static UcdStatus getStatus( String ucd ) {
        return statusMap_.computeIfAbsent( ucd, UcdStatus::createStatus );
    }

    /**
     * Returns the UCD parser used by this class.
     *
     * @return  parser
     */
    public static UCDParser getParser() {
        return ucdParser_;
    }

    /**
     * Returns a UCDParser instance for use by this class.
     *
     * @return  parser
     */
    private static UCDParser createParser() {

        /* At time of writing there is an issue with the list of deprecated
         * words that means a message is issued.  We will ignore this.
         * Future ucidy updates (based perhaps on UCD1+ errata or versions
         * later than 1.4) may mean this is no longer necessary. */
        Logger.getLogger( "ari.ucidy" ).setLevel( Level.OFF );

        /* Currently use the default instance, but could construct one
         * based on a customised word list. */
        return UCDParser.defaultParser;
    }

    /**
     * Does the work of parsing the UCD.
     *
     * @param  ucd  UCD text
     * @return new status, or null for blank input
     */
    private static UcdStatus createStatus( String ucd ) {
        if ( ucd == null || ucd.trim().length() == 0 ) {
            return null;
        }
        if ( ucd1s_.contains( ucd ) ) {
            return new UcdStatus( Code.UCD1, "UCD1, not UCD1+" );
        }
        if ( VOX_REGEX.matcher( ucd ).matches() ) {
            return new UcdStatus( Code.VOX, "SIAv1-style VOX namespace" );
        }
        UCD pucd = ucdParser_.parse( ucd );
        StringBuffer sbuf = new StringBuffer();
        for ( Iterator<String> it = pucd.getErrors(); it.hasNext(); ) {
            String line = it.next().replaceFirst( " *!$", "" );
            if ( sbuf.length() > 0 && line.length() > 0 ) {
                sbuf.append( "; " );
            }
            sbuf.append( line );
        }
        String message = sbuf.length() > 0 ? sbuf.toString() : null;
        boolean hasNamespace = false;
        boolean isValid = true;
        boolean isRecognised = true;
        boolean isRecommended = true;
        boolean isDeprecated = false;
        for ( UCDWord word : pucd ) {
            if ( word != null ) {
                hasNamespace = hasNamespace || word.namespace != null;
                isValid = isValid && word.valid;
                isRecognised = isRecognised && word.recognised;
                isRecommended = isRecommended && word.recommended;
                isDeprecated = isDeprecated || word.isDeprecated();
            }
        }
        if ( ! isValid ) {
            return new UcdStatus( Code.BAD_SYNTAX, message );
        }
        if ( hasNamespace ) {
            return new UcdStatus( Code.NAMESPACE, message );
        }
        if ( isDeprecated ) {
            return new UcdStatus( Code.DEPRECATED, message );
        }
        if ( ! isRecognised ) {
            return new UcdStatus( Code.UNKNOWN_WORD, message );
        }
        if ( ! isRecommended ) {
            return new UcdStatus( Code.DEPRECATED, message );
        }
        return new UcdStatus( pucd.isFullyValid() ? Code.OK : Code.BAD_SEQUENCE,
                              message );
    }

    /**
     * Returns a collection of known UCD1 strings.
     *
     * @return  UCD1s
     */
    private static Set<String> readUcd1s() {
        Set<String> set = new LinkedHashSet<>();
        for ( Iterator<uk.ac.starlink.table.UCD> it =
                  uk.ac.starlink.table.UCD.getUCDs(); it.hasNext(); ) {
            set.add( it.next().getID() );
        }
        return set;
    }

    /**
     * Returns an LRU cache suitable for storing UcdStatus values.
     *
     * @param  limit  maximum cache size
     * @return  new thread-safe map
     */
    private static Map<String,UcdStatus> createCache( final int limit ) {
        return Collections
              .synchronizedMap( new LinkedHashMap<String,UcdStatus>() {
            @Override
            public boolean removeEldestEntry( Map.Entry<String,UcdStatus>
                                              entry ) {
                return size() > limit;
            }
        } );
    }

    /**
     * Characterises UCD standards conformance.
     */
    public enum Code {

        /** Conforms to UCD1+ standard. */
        OK( ' ' ),

        /** Conforms to UCD1 standard. */
        UCD1( ' ' ),

        /** Is in VOX: namespace introduced by SIAv1. */
        VOX( ' ' ),

        /** Not a UCD1 and cannot be parsed according to UCD1+. */
        BAD_SYNTAX( 'E' ),

        /** UCD words violate UCD1+ sequence rules. */
        BAD_SEQUENCE( 'E' ),

        /** UCD1+ syntax but contains non-UCD1+ word. */
        UNKNOWN_WORD( 'E' ),

        /** Contains apparently namespaced UCD words. */
        NAMESPACE( 'W' ),

        /** Contains deprecated UCD1+ words. */
        DEPRECATED( 'W' );

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
         * Indicates whether this status represents a UCD value which
         * violates known standards.
         *
         * @return  true for error status
         */
        public boolean isError() {
            return stat_ == 'E';
        }

        /**
         * Indicates whether this status represents a UCD value which
         * may deserve attention, but is not actually a standards violation.
         *
         * @return  true for warning status
         */
        public boolean isWarning() {
            return stat_ == 'W';
        }
    }
}
