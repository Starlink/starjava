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

    /** Instance with value trimming, bad char replacement, no HIERARCH. */
    public static final CardFactory CLASSIC = new CardFactory( new Config() {
        public boolean allowHierarch() {
            return false;
        }
        public boolean allowTrim() {
            return true;
        }
        public String sanitiseText( String txt ) {
            return sanitiseByReplacement( txt, '?' );
        }
    } );

    /** Instance with value trimming, bad char replacement, and HIERARCH. */
    public static final CardFactory HIERARCH = new CardFactory( new Config() {
        public boolean allowHierarch() {
            return true;
        }
        public boolean allowTrim() {
            return true;
        }
        public String sanitiseText( String txt ) {
            return sanitiseByReplacement( txt, '?' );
        }
    } );

    /** Instance with no value trimming, bad char rejection, no HIERARCH. */
    public static final CardFactory STRICT = new CardFactory( new Config() {
        public boolean allowHierarch() {
            return false;
        }
        public boolean allowTrim() {
            return false;
        }
        public String sanitiseText( String txt ) {
            return sanitiseAsError( txt );
        }
    } );

    /** Default instance (currently {@link #CLASSIC}). */
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
     * No sanitisation or trimming is performed.
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
     * The text may be sanitised and trimmed if too long according to policy.
     *
     * @param  txt  comment text
     * @return   new card
     */
    public CardImage createCommentCard( String txt ) {
        String ctxt = "COMMENT " + sanitiseText( txt );
        if ( ctxt.length() > 80 && config_.allowTrim() ) {
            ctxt = ctxt.substring( 0, 80 );
        }
        return createPlainCard( ctxt );
    }

    /**
     * Constructs a key-value card with string content.
     * The value and comment may be sanitised and trimmed if too long
     * according to policy.
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
           .append( sanitiseText( value ).replace( "'", "''" ) );
        if ( comment == null || comment.length() < 50 ) {
            while ( content.length() < 9 ) {
                content.append( ' ' );
            }
        }
        content.append( '\'' );
        return createLiteralCard( key, content.toString(),
                                  sanitiseText( comment ) );
    }

    /**
     * Constructs a key-value card with integer content.
     * The comment may be sanitised and trimmed if too long according to policy.
     *
     * @param  key  header keyword
     * @param  value  header value
     * @param  comment  comment text, or null
     * @return  new card
     */
    public CardImage createIntegerCard( String key, long value,
                                        String comment ) {
        return createLiteralCard( key, Long.toString( value ),
                                  sanitiseText( comment ) );
    }

    /**
     * Constructs a key-value card with floating point content.
     * The comment may be sanitised and trimmed if too long according to policy.
     *
     * @param  key  header keyword
     * @param  value  header value
     * @param  comment  comment text, or null
     * @return  new card
     */
    public CardImage createRealCard( String key, double value,
                                     String comment ) {
        return createLiteralCard( key, Double.toString( value ),
                                  sanitiseText( comment ) );
    }

    /**
     * Constructs a key-value card with logical content.
     * The comment may be sanitised and trimmed if too long according to policy.
     *
     * @param  key  header keyword
     * @param  value  header value
     * @param  comment  comment text, or null
     * @return  new card
     */
    public CardImage createLogicalCard( String key, boolean value,
                                        String comment ) {
        return createLiteralCard( key, value ? "T" : "F",
                                  sanitiseText( comment ) );
    }

    /**
     * Constructs a key-value card with given literal text for the value part.
     * No additional quoting or escaping is performed on the provided string,
     * but both string and comment may be trimmed in a string-sensitive way
     * according to policy.
     *
     * <p>The key must contain only legal key characters,
     * and the literal and comment must contain only legal header characters.
     *
     * @param  key  header keyword
     * @param  literal  formatted value text
     * @param  comment  comment text, or null
     * @return  new card
     * @throws  IllegalArgumentException  if inputs are not suitable for FITS
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
     * <p>The key must contain only legal key characters,
     * and the literal and comment must contain only legal header characters.
     *
     * @param  key  header keyword starting with "HIERARCH"
     * @param  literal  formatted value text
     * @param  comment  comment text, or null
     * @return  new card
     * @throws  IllegalArgumentException  if inputs are not suitable for FITS
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
             sbuf.length() + comment.length() + 3 < 80 ) {
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
     * Ensures that a given string contains only legal FITS header characters
     * in accordance with configuration.
     *
     * @param  txt  free text string
     * @return  string like input but containing only legal FITS characters
     * @throws  IllegalArgumentException  if this factory's config is unable
     *                                    or unwilling to sanitise
     */
    private String sanitiseText( String txt ) {
        return txt == null ? null : config_.sanitiseText( txt );
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
     * Utility string sanitisation function that replaces illegal characters
     * by a given char.
     *
     * @param  txt  input text
     * @param  replaceChar  character to replace non-FITS characters
     * @return  same as <code>txt</code>, but with non-FITS characters
     *          replaced by <code>replaceChar</code>
     */
    public static String sanitiseByReplacement( String txt, char replaceChar ) {
        int nc = txt.length();
        int nr = 0;
        StringBuffer sbuf = new StringBuffer( txt );
        for ( int i = 0; i < nc; i++ ) {
            if ( ! FitsUtil.isFitsCharacter( sbuf.charAt( i ) ) ) {
                sbuf.setCharAt( i, replaceChar );
                nr++;
            }
        }
        if ( nr > 0 ) {
            logger_.info( "Replace " + nr + " non-FITS "
                        + ( nr == 1 ? "char" : "chars" ) + " with '"
                        + replaceChar + "' in \"" + txt + "\"" );
        }
        return nr == 0 ? txt : sbuf.toString();
    }

    /**
     * Utility string sanitisation function that throws an
     * IllegalArgumentException if any illegal characters are present.
     *
     * @param  txt  input text
     * @return  input text
     * @throws  IllegalArgumentException  if non-FITS characters are present
     */
    public static String sanitiseAsError( String txt ) {
        int nc = txt.length();
        for ( int ic = 0; ic < nc; ic++ ) {
            char ch = txt.charAt( ic );
            if ( ! FitsUtil.isFitsCharacter( ch ) ) {
                throw new IllegalArgumentException( "Illegal FITS character 0x"
                                                  + Integer.toHexString( ch )
                                                  + " in \"" + txt + "\"" );
            }
        }
        return txt;
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

        /**
         * This method is called on uncontrolled text inputs that will
         * end up as text or comment content in output cards.
         * The output value must contain only legal FITS header characters,
         * that is in the range 0x20-0x7e inclusive
         * (see {@link FitsUtil#isFitsCharacter}).
         * If the input text contains no illegal characters, it should be
         * returned unchanged.  If it does contain illegal characters,
         * it should be adjusted in some way to remove them,
         * or a RuntimeException may be thrown.
         *
         * <p>Example implementations are provided in
         * {@link #sanitiseByReplacement sanitiseAsReplacement} and
         * {@link #sanitiseAsError sanitiseAsError}.
         *
         * @param  txt  non-null free text string
         * @return  string like input but containing only legal FITS characters
         * @throws  IllegalArgumentException  if this config is unable
         *                                    or unwilling to sanitise
         */
        String sanitiseText( String txt );
    }
}
