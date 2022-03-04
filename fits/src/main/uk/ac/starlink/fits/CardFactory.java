package uk.ac.starlink.fits;

import java.util.logging.Logger;

/**
 * Creates FITS CardImages suitable for writing to a FITS header.
 * Different factory instances are available with variant options
 * for header construction.
 *
 * <p>Attempts to construct illegal FITS headers will generally provoke
 * RuntimeExceptions.
 *
 * @author   Mark Taylor
 * @since    4 Mar 2022
 */
public class CardFactory {

    private final Config config_;

    /** Instance with silent value trimming and no HIERARCH. */
    public static final CardFactory CLASSIC = new CardFactory( new Config() {
        public boolean allowHierarch() {
            return false;
        }
        public boolean allowTrim() {
            return true;
        }
    } );

    /** Instance with value trimming and HIERARCH support. */
    public static final CardFactory HIERARCH = new CardFactory( new Config() {
        public boolean allowHierarch() {
            return true;
        }
        public boolean allowTrim() {
            return true;
        }
    } );

    /** Instance with no value trimming and no HIERARCH. */
    public static final CardFactory STRICT = new CardFactory( new Config() {
        public boolean allowHierarch() {
            return false;
        }
        public boolean allowTrim() {
            return false;
        }
    } );

    /** Default instance. */
    public static final CardFactory DEFAULT = CLASSIC;

    /** CardImage for terminating header ("END"). */
    public static final CardImage END_CARD =
        createImmutableCard( DEFAULT.createPlainCard( "END" ) );

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.fits" );

    /**
     * Constructor.
     *
     * @param  config  factory configuration
     */
    public CardFactory( Config config ) {
        config_ = config;
    }

    /**
     * Returns the configuration state of this factory.
     *
     * @return  config object
     */
    public Config getConfig() {
        return config_;
    }

    /**
     * Constructs a card containing the given exact text.
     * It is padded with spaces if necessary.
     *
     * @param  txt  literal card content, &lt;=80 characters
     * @return   new card
     */
    public CardImage createPlainCard( String txt ) {
        StringBuffer sbuf = new StringBuffer( 80 );
        if ( txt.length() <= 80 ) {
            sbuf.append( txt );
        }
        else {
            throw new IllegalArgumentException( "Text too long" );
        }
        while ( sbuf.length() < 80 ) {
            sbuf.append( ' ' );
        }
        return new CardImage( sbuf );
    }

    /**
     * Constructs a COMMENT card with the given comment.
     * May be trimmed if too long according to policy.
     *
     * @param  txt  comment text
     * @return   new card
     */
    public CardImage createCommentCard( String txt ) {
        String ctxt = "COMMENT " + txt;
        if ( ctxt.length() > 80 && config_.allowTrim() ) {
            ctxt = ctxt.substring( 0, 80 );
        }
        return createPlainCard( ctxt );
    }

    /**
     * Constructs a key-value card with string content.
     * The value may be trimmed if too long according to policy.
     *
     * @param  key  header keyword
     * @param  value  header value
     * @param  comment  comment text, or null
     * @return  new card
     */
    public CardImage createStringCard( String key, String value,
                                       String comment ) {
        StringBuffer content =
            new StringBuffer()
           .append( '\'' )
           .append( value.replace( "'", "''" ) );
        if ( comment == null || comment.length() < 50 ) {
            while ( content.length() < 9 ) {
                content.append( ' ' );
            }
        }
        content.append( '\'' );
        return createLiteralCard( key, content.toString(), comment );
    }

    /**
     * Constructs a key-value card with integer content.
     *
     * @param  key  header keyword
     * @param  value  header value
     * @param  comment  comment text, or null
     * @return  new card
     */
    public CardImage createIntegerCard( String key, long value,
                                        String comment ) {
        return createLiteralCard( key, Long.toString( value ), comment );
    }

    /**
     * Constructs a key-value card with floating point content.
     *
     * @param  key  header keyword
     * @param  value  header value
     * @param  comment  comment text, or null
     * @return  new card
     */
    public CardImage createRealCard( String key, double value,
                                     String comment ) {
        return createLiteralCard( key, Double.toString( value ), comment );
    }

    /**
     * Constructs a key-value card with logical content
     *
     * @param  key  header keyword
     * @param  value  header value
     * @param  comment  comment text, or null
     * @return  new card
     */
    public CardImage createLogicalCard( String key, boolean value,
                                        String comment ) {
        return createLiteralCard( key, value ? "T" : "F", comment );
    }

    /**
     * Constructs a key-value card with given literal text for the value part.
     * No additional quoting or escaping is performed on the provided string,
     * but it may be trimmed in a string-sensitive way according to policy.
     *
     * @param  key  header keyword
     * @param  literal  formatted value text
     * @param  comment  comment text, or null
     * @return  new card
     */
    public CardImage createLiteralCard( String key, String literal,
                                        String comment ) {
        if ( config_.allowHierarch() && key.startsWith( "HIERARCH " ) ) {
            return createLiteralHierarchCard( key, literal, comment );
        }
        if ( key.length() > 8 ) {
            throw new IllegalArgumentException( "Key too long: " + key );
        }
        if ( ! key.matches( "[A-Z0-9_-]+" ) ) {
            throw new IllegalArgumentException( "Bad characters in key: "
                                              + key );
        }
        StringBuffer sbuf = new StringBuffer( 80 );
        sbuf.append( key );
        while ( sbuf.length() < 8 ) {
            sbuf.append( ' ' );
        }
        sbuf.append( "= " );

        if ( literal.charAt( 0 ) != '\'' &&
             literal.length() <= 21 &&
             ( comment == null || comment.length() <= 46 ) ) {
            while ( sbuf.length() + literal.length() < 30 ) {
                sbuf.append( ' ' );
            }
        }
        if ( sbuf.length() + literal.length() <= 80 ) {
            sbuf.append( literal );
        }
        else {
            if ( config_.allowTrim() ) {
                logger_.info( "Trim overlength value for " + key );
                sbuf.append( trimLiteralString( literal,
                                                80 - sbuf.length() ) );
            }
            else {
                throw new IllegalArgumentException( "Value too long: "
                                                  + literal );
            }
        }
        if ( comment != null && comment.length() > 0 ) {
            if ( comment.length() + 33 < 80 ) {
                while ( sbuf.length() < 30 ) {
                    sbuf.append( ' ' );
                }
            }
            if ( sbuf.length() + 3 < 80 ) {
                sbuf.append( " / " );
                for ( int i = 0; i < comment.length() && sbuf.length() < 80;
                      i++ ) {
                    sbuf.append( comment.charAt( i ) );
                }
            }
        }
        while ( sbuf.length() < 80 ) {
            sbuf.append( ' ' );
        }
        assert sbuf.length() == 80;
        return new CardImage( sbuf );
    }

    /**
     * Constructs a key-value card with given literal text for the value part
     * and a key that starts "HIERARCH".
     * No additional quoting or escaping is performed on the provided string,
     * but it may be trimmed in a string-sensitive way according to policy.
     *
     * @param  key  header keyword starting with "HIERARCH"
     * @param  literal  formatted value text
     * @param  comment  comment text, or null
     * @return  new card
     */
    private CardImage createLiteralHierarchCard( String key, String literal,
                                                 String comment ) {
        if ( ! key.matches( "[ A-Z0-9_-]+" ) ) {
            throw new IllegalArgumentException( "Bad characters in key: "
                                              + key );
        }
        StringBuffer sbuf = new StringBuffer( 80 );
        sbuf.append( key );
        sbuf.append( " = " );
        if ( sbuf.length() + literal.length() <= 80 ) {
            sbuf.append( literal );
        }
        else if ( config_.allowTrim() && sbuf.length() < 75 ) {
            logger_.info( "Trim overlength value for key " + key );
            sbuf.append( trimLiteralString( literal, 80 - sbuf.length() ) );
        }
        else {
            throw new IllegalArgumentException( "key + value too long: "
                                              + key + " = " + literal );
        }
        assert sbuf.length() <= 80;
        if ( comment != null && comment.trim().length() > 0 &&
             comment.length() + 3 < 80 ) {
            sbuf.append( " / " );
            for ( int i = 0; i < comment.length() && sbuf.length() < 80;
                  i++ ) {
                sbuf.append( comment.charAt( i ) );
            }
        }
        while ( sbuf.length() < 80 ) {
            sbuf.append( ' ' );
        }
        assert sbuf.length() == 80;
        return new CardImage( sbuf );
    }

    /**
     * Attempts to trim a formatted header value to a given maximum length.
     * Only string values can be trimmed.
     *
     * @param  literal  formatted value
     * @param  maxleng  maximum lenght of result
     * @return   trimmed value
     * @throws  IllegalValueException  if trimming can't be done
     */
    private String trimLiteralString( String literal, int maxleng ) {
        int leng0 = literal.length();
        if ( leng0 <= maxleng ) {
            return literal;
        }
        if ( leng0 == 0 ||
             literal.charAt( 0 ) != '\'' ||
             literal.charAt( literal.length() - 1 ) != '\'' ) {
            throw new IllegalArgumentException( "Can't trim non-string literal "
                                              + "\"" + literal + "\"" );
        }
        if ( leng0 < 5 ) {
            throw new IllegalArgumentException( "Too short to trim" );
        }
        boolean useEllipsis = false;
        String content = useEllipsis ? literal.substring( 1, maxleng - 4 )
                                     : literal.substring( 1, maxleng - 1 );
        if ( content.charAt( content.length() - 1 ) == '\'' &&
             content.charAt( content.length() - 2 ) != '\'' ) {
            content = content.substring( 0, content.length() - 1 );
        }
        return "'" + content + ( useEllipsis ? "..." : "" ) + "'";
    }

    /**
     * Creates a copy of a CardImage that cannot be tampered with.
     *
     * @param  template  card to copy
     * @return   immutable copy
     */
    private static CardImage createImmutableCard( CardImage template ) {
        return new CardImage( template.getBytes() ) {
            @Override
            public byte[] getBytes() {
                return super.getBytes().clone();
            }
        };
    }

    /**
     * CardFactory configuration.
     */
    public interface Config {

        /**
         * Determines whether the HIERARCH keyword convention is permitted.
         * If true, overlength keywords starting "HIERARCH " are allowed.
         *
         * @return  true iff HIERARCH is allowed
         */
        boolean allowHierarch();

        /**
         * Determines whether overlength string values will be trimmed
         * to fits card constraints.  If true, strings will be silently
         * truncated, if false overlength values will provoke a
         * RuntimeException.
         *
         * @return  true  iff overlength string values may be truncated
         */
        boolean allowTrim();
    }
}
