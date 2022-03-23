package uk.ac.starlink.fits;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Defines different types of FITS header card.
 * Each instance knows how to parse an 80-byte card.
 *
 * @author   Mark Taylor
 * @since    4 Mar 2022
 */
public class CardType<T> {

    private final String name_;
    private final Class<T> clazz_;
    private final Pattern kvcPattern_;
    private final Function<String,T> decodeValue_;

    // Regular expressions used for header parsing.
    private static final String U_KEY8 = "[ A-Z0-9_-]{8}";
    private static final String B_KEYEQ = "(" + U_KEY8 + ")= ";
    private static final String B_HIEREQ =
        "(HIERARCH [-_A-Z0-9]+(?: [-_A-Z0-9]+)*) += ";
    private static final String B_OPTCOMMENT = " *(?:/ *(.*))?";
    private static final String B_QUOTED = " *'((?:[^']|'')*)'";
    private static final String B_LOGICAL = " *([TF])";
    private static final String B_INT = " *([-+]?[0-9]+)";
    private static final String U_FLOAT = FitsUtil.FLOAT_REGEX;
    private static final String B_FLOAT = " *(" + U_FLOAT + ")";
    private static final String B_COMPLEX =
        " *\\( *(" + U_FLOAT + " *, *" + U_FLOAT + ") *\\)";

    /** HISTORY card type. */
    public static final CardType<Void> HISTORY =
        createType( "HISTORY", Void.class, "(HISTORY) ()(.*)", t -> null );

    /** COMMENT card type. */
    public static final CardType<Void> COMMENT =
        createType( "COMMENT", Void.class, "(COMMENT) ()(.*)", t -> null );

    /** Card type with nothing in the keyword field. */
    public static final CardType<Void> COMMENT_BLANK =
        createType( "COMMENT_BLANK", Void.class, "()        ()(.*)", t -> null);

    /** CONTINUE card type. */
    public static final CardType<String> CONTINUE =
        createType( "CONTINUE", String.class,
                    "(CONTINUE) " + B_QUOTED + B_OPTCOMMENT,
                    t -> parseQuoted( t ) );

    /** END card type. */
    public static final CardType<Void> END =
        createType( "END", Void.class, "(END)()() +", t -> null );

    /** Standard key/value card with string content. */
    public static final CardType<String> STRING =
        createType( "STRING", String.class,
                    B_KEYEQ + B_QUOTED + B_OPTCOMMENT,
                    t -> parseQuoted( t ) );

    /** HIERARCH key/value card with string content. */
    public static final CardType<String> STRING_HIER =
        createType( "STRING_HIER", String.class,
                    B_HIEREQ + B_QUOTED + B_OPTCOMMENT,
                    t -> parseQuoted( t ) );

    /** Standard key/value card with logical content. */
    public static final CardType<Boolean> LOGICAL =
        createType( "LOGICAL", Boolean.class,
                    B_KEYEQ + B_LOGICAL + B_OPTCOMMENT,
                    t -> parseLogical( t ) );

    /** HIERARCH key/value card with logical content. */
    public static final CardType<Boolean> LOGICAL_HIER =
        createType( "LOGICAL_HIER", Boolean.class,
                    B_HIEREQ + B_LOGICAL + B_OPTCOMMENT,
                    t -> parseLogical( t ) );

    /** Standard key/value card with integer content. */
    public static final CardType<BigInteger> INTEGER =
        createType( "INTEGER", BigInteger.class,
                    B_KEYEQ + B_INT + B_OPTCOMMENT, BigInteger::new );

    /** HIERARCH key/value card with integer content. */
    public static final CardType<BigInteger> INTEGER_HIER =
        createType( "INTEGER_HIER", BigInteger.class,
                    B_HIEREQ + B_INT + B_OPTCOMMENT, BigInteger::new );

    /** Standard key/value card with floating point content. */
    public static final CardType<Double> REAL =
        createType( "REAL", Double.class,
                    B_KEYEQ + B_FLOAT + B_OPTCOMMENT,
                    t -> Double.valueOf( parseDouble( t ) ) );

    /** HIERARCH key/value card with floating point content. */
    public static final CardType<Double> REAL_HIER =
        createType( "REAL_HIER", Double.class,
                    B_HIEREQ + B_FLOAT + B_OPTCOMMENT,
                    t -> Double.valueOf( parseDouble( t ) ) );

    /** Standard key/value card with complex content. */
    public static final CardType<double[]> COMPLEX =
        createType( "COMPLEX", double[].class,
                    B_KEYEQ + B_COMPLEX + B_OPTCOMMENT,
                    t -> parseComplex( t ) );

    /** HIERARCH key/value card with complex content. */
    public static final CardType<double[]> COMPLEX_HIER =
        createType( "COMPLEX_HIER", double[].class,
                    B_HIEREQ + B_COMPLEX + B_OPTCOMMENT,
                    t -> parseComplex( t ) );

    /** Non-standard comment card (no value indicator). */
    public static final CardType<Void> COMMENT_OTHER =
        createType( "COMMENT_OTHER", Void.class,
                    "()()(" + U_KEY8 + "[^=].*)", t -> null );

    /** Catch-all card type - apparently not legal FITS. */
    public static final CardType<Void> UNKNOWN =
        createType( "UNKNOWN", Void.class, "()()().*", t -> null );

    /** Unmofifiable list of all known card types. */
    public static final List<CardType<?>> CARD_TYPES =
            Collections.unmodifiableList( Arrays.asList(
        HISTORY, COMMENT, COMMENT_BLANK, CONTINUE, END,
        STRING, LOGICAL, INTEGER, REAL, COMPLEX,
        STRING_HIER, LOGICAL_HIER, INTEGER_HIER, REAL_HIER, COMPLEX_HIER,
        COMMENT_OTHER
    ) );

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.fits" );

    /**
     * Constructor.
     *
     * @param  name  card type name
     * @param  clazz   class of value
     * @param  kvcRegex  regular expression with three groups:
     *                   key, value, comment
     * @param  decodeValue  turns value group string into typed card value
     */
    private CardType( String name, Class<T> clazz, String kvcRegex,
                      Function<String,T> decodeValue ) {
        name_ = name;
        clazz_ = clazz;
        kvcPattern_ = Pattern.compile( kvcRegex );
        decodeValue_ = decodeValue;
    }

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns the type of values yielded by this CardType.
     *
     * @return  value  class
     */
    public Class<T> getValueClass() {
        return clazz_;
    }

    /**
     * Parses an 80-character string as a FITS header card.
     *
     * @param  txt80  80-character string, should be ASCII-clean
     * @return   parsed header card of this type,
     *           or null if card cannot be parsed as this type
     */
    public ParsedCard<T> toCard( String txt80 ) {
        Matcher matcher = kvcPattern_.matcher( txt80 );
        if ( matcher.matches() ) {
            String key = matcher.group( 1 );
            String rawValue = matcher.group( 2 );
            String comment = matcher.group( 3 );
            T value;
            try {
                value = decodeValue_.apply( rawValue );
            }
            catch ( RuntimeException e ) {
                logger_.warning( "Bad " + name_ + " card value \"" + rawValue
                               + "\": " + e );
                return null;
            }
            if ( key != null ) {
                key = key.trim();
            }
            if ( comment != null ) {
                comment = comment.trim();
            }
            return new ParsedCard<T>( key, this, value, comment ) {
                @Override
                public String toString() {
                    return txt80;
                }
            };
        }
        else {
            return null;
        }
    }

    /**
     * Constructs a card type from required information.
     *
     * @param  name  card type name
     * @param  clazz   class of value
     * @param  kvcRegex  regular expression with three groups:
     *                   key, value, comment
     * @param  decodeValue  turns value group string into typed card value
     */
    private static <T> CardType<T> createType( String name, Class<T> clazz,
                                               String kvcRegex,
                                               Function<String,T> decode ) {
        return new CardType<T>( name, clazz, kvcRegex, decode );
    }

    /**
     * Returns the string value of a quoted header string.
     * This strips the surrounding quotes, unescapes any embedded quotes,
     * and strips trailing spaces, as per the FITS specification.
     *
     * @param  txt  stripped quoted value string in FITS header format
     * @return  unquoted text content
     */
    private static String parseQuoted( String txt ) {
        if ( txt.indexOf( '\'' ) >= 0 ) {
            txt = txt.replace( "''", "'" );
        }
        if ( txt.endsWith( " " ) ) {
            txt = txt.replaceAll( " +$", "" );
        }
        return txt;
    }

    /**
     * Returns the boolean value of a logical header string.
     *
     * @param   value string in FITS header format
     * @return  true for "T" false for "F"
     */
    private static Boolean parseLogical( String txt ) {
        return Boolean.valueOf( "T".equals( txt ) );
    }

    /**
     * Returns the double value of a header floating point value.
     *
     * @param  txt   value string in FITS header format
     * @return  numeric value
     */
    private static double parseDouble( String txt ) {
        return Double.valueOf( txt.replace( 'D', 'E' ) );
    }

    /**
     * Returns the complex value of a header complex integer or floating point
     * value.
     *
     * @param  txt   value string in FITS header format
     * @return  complex value as a 2-element (real,imaginary) array
     */
    private static double[] parseComplex( String txt ) {
        String[] pair = txt.split( "," );
        return new double[] { parseDouble( pair[ 0 ] ),
                              parseDouble( pair[ 1 ] ) };
    }
}
