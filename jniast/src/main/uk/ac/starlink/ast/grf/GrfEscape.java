package uk.ac.starlink.ast.grf;

import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.ast.AstObject;

/**
 * Represents a Grf escape sequence.
 * Each <code>GrfEscape</code> 
 * instance has a type code and an associated integer value.
 * The meaning of the sequence is determined by the type code, and
 * sometimes the <code>value</code> integer.  The known escape codes
 * are defined as static final integer members of this class - the
 * definitions below document the textual meaning of these and what,
 * if anything, the value means.
 * <p>
 * This class also contains static methods for locating escape sequences
 * and global control of how AST handles them in text strings.
 *
 * @author   Mark Taylor (Starlink)
 * @see      uk.ac.starlink.ast.Grf#cap
 */
public class GrfEscape {

    /**
     * Code for a literal percent sign.
     * <p><code>value</code> is ignored.
     * <p>Textual representation is "%%".
     */
    public final static int GRF__ESPER =
        AstObject.getAstConstantI( "GRF__ESPER" );

    /**
     * Code for superscripted text. 
     * <p><code>value</code> gives the distance from the base-line of "normal"
     * text to the base-line of the superscripted text, scaled so a
     * value of 100 corresponds to the height of normal text.
     * A value of -1 means draw subsequent characters with the normal baseline.
     * <p>Textual representation is "%^<i>value</i>+", or "%^+" for value=-1.
     */
    public final static int GRF__ESSUP =
        AstObject.getAstConstantI( "GRF__ESSUP" );

    /**
     * Code for subscripted text.
     * <p><code>value</code> gives the distance from the base-line of "normal"
     * text to the base-line of the subscripted text, scaled so that a
     * value of 100 corresponds to the height of normal text.
     * A value of -1 means draw subsequent characters with the normal baseline.
     * <p>Textual representation is "%v<i>value</i>+", or "%v+" for value=-1.
     */
    public final static int GRF__ESSUB =
        AstObject.getAstConstantI( "GRF__ESSUB" );

    /**
     * Code for a horizontal gap before drawing the next characters.
     * <p><code>value</code> gives the size of the movement, scaled 
     * so that a value of 100 corresponds to the height(?) of normal text.
     * <p>Textual representation is "%&gt;<i>value</i>+".
     */
    public final static int GRF__ESGAP =
        AstObject.getAstConstantI( "GRF__ESGAP" );

    /**
     * Code for a backwards space before drawing the next characters.
     * <p><code>value</code> gives the size of the movement, scaled
     * so that a value of 100 corresponds to the height(?) of normal text.
     * <p>Textual representation is "%&lt;<i>value</i>+".
     */
    public final static int GRF__ESBAC =
        AstObject.getAstConstantI( "GRF__ESBAC" );

    /**
     * Code for a change in character size.
     * <p><code>value</code> gives the new size as a fraction of "normal" size,
     * scaled so a value of 100 corresponds to 1.0.
     * A value of -1 means draw subsequent characters at the normal size.
     * <p>Textual representation is "%s<i>value</i>+", or "%s+" for value=-1.
     */
    public final static int GRF__ESSIZ =
        AstObject.getAstConstantI( "GRF__ESSIZ" );

    /**
     * Code for a change in character width.
     * <p><code>value</code> gives the new width as a fraction of "normal" 
     * width,
     * scaled so a value of 100 corresponds to 1.0.
     * A value of -1 means draw subsequent characters at the normal width.
     * <p>Textual representation is "%w<i>value</i>+", or "%w+" for value=-1.
     */
    public final static int GRF__ESWID =
        AstObject.getAstConstantI( "GRF__ESWID" );

    /**
     * Code for a change in font.
     * <p><code>value</code> gives the new font value.
     * A value of -1 means reset the font attribute to its normal value.
     * <p>Textual representation is "%f<i>value</i>+", or "%f+" for value=-1.
     */
    public final static int GRF__ESFON =
        AstObject.getAstConstantI( "GRF__ESFON" );

    /**
     * Code for a change in colour.
     * <p><code>value</code> gives the new colour value.
     * A value of -1 means reset the font attribute to its normal value.
     * <p>Textual representation is "%c<i>value</i>+", or "%c+" for value=-1.
     */
    public final static int GRF__ESCOL =
        AstObject.getAstConstantI( "GRF__ESCOL" );

    /**
     * Code for a change in style.
     * <p><code>value</code> gives the new style value.
     * A value of -1 means reset the styel attribute to its normal value.
     * <p>Textual representation is "%t<i>value</i>+", or "%t+" for value=-1.
     */
    public final static int GRF__ESSTY =
        AstObject.getAstConstantI( "GRF__ESSTY" );

    /**
     * Code for popping the current graphics attribute values from the stack.
     * <p><code>value</code> is ignored.
     * <p>Textual representation is "%-".
     */
    public final static int GRF__ESPOP =
        AstObject.getAstConstantI( "GRF__ESPOP" );

    /**
     * Code for pushing the current graphics attribute values onto the stack.
     * <p><code>value</code> is ignored.
     * <p>Textual representation is "%+".
     */
    public final static int GRF__ESPSH =
        AstObject.getAstConstantI( "GRF__ESPSH" );

    private final int code;
    private final int value;

    /**
     * Constructs a new escape object.
     *
     * @param  code   symbolic graphics code (one of the GRF__ES* constants)
     * @param  value  associated numeric value (meaning is dependent on the
     *                value of <code>code</code>)
     */
    public GrfEscape( int code, int value ) {
        this.code = code;
        this.value = value;
    }

    /**
     * Returns the symbolic graphics code representing the type of this
     * escape object.
     *
     * @return   one of the GRF__* values
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns the integer value associated with this escape object.
     * 
     * @return  value (meaning is dependent on code).
     */
    public int getValue() {
        return value;
    }

    /**
     * Returns the textual representation of this escape sequence.
     * This starts with a "%" sign.
     *
     * @return  text string for this escape sequence
     */
    public String getText() {
        boolean start = value >= 0;
        String valSeq = ( value >= 0 ) ? ( value + "+" ) : "+";
             if ( code == GRF__ESPER ) return "%%";
        else if ( code == GRF__ESSUP ) return "%^" + valSeq;
        else if ( code == GRF__ESSUB ) return "%v" + valSeq;
        else if ( code == GRF__ESGAP ) return "%>" + value + "+";
        else if ( code == GRF__ESBAC ) return "%<" + value + "+";
        else if ( code == GRF__ESSIZ ) return "%s" + valSeq;
        else if ( code == GRF__ESWID ) return "%w" + valSeq;
        else if ( code == GRF__ESFON ) return "%f" + valSeq;
        else if ( code == GRF__ESCOL ) return "%c" + valSeq;
        else if ( code == GRF__ESSTY ) return "%t" + valSeq;
        else if ( code == GRF__ESPSH ) return "%+";
        else if ( code == GRF__ESPOP ) return "%-";
        else {
                return "%?+";
        }
    }

    public String toString() {
        return getText();
    }

    public boolean equals( Object o ) {
        if ( o instanceof GrfEscape ) {
            GrfEscape other = (GrfEscape) o;
            return other.getCode() == getCode() 
                && other.getValue() == getValue();
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        int h = 5;
        h = h * 23 + getCode();
        h = h * 23 + getValue();
        return h;
    }

    /**
     * Determines whether escape sequences should be stripped out of
     * strings returned by AstObject methods.
     * <code>Plot</code> defines a set of escape sequences which can
     * be included within a text string to control the appearance of
     * substrings in the text.
     * It is usually inappropriate for object methods to return strings
     * containing such escape sequences when called by application code.
     * For instance, an application which displays the value of
     * the Title attribute of a Frame usually does not want the displayed
     * string to include escape sequences which a human would have
     * difficulty interpreting.  Therefore the default behaviour is
     * to strip out such escape sequences when called by application code.
     *
     * @param  escapes  true iff you want <code>get*</code> methods to
     *                  include escape sequences
     * @see   uk.ac.starlink.ast.Plot#setEscape
     */
    public static void setEscapes( boolean escapes ) {
        escapes( escapes ? 1 : 0 );
    }

    /**
     * Indicates whether escape sequences will be stripped out of
     * strings returned by AstObject methods.
     * <code>Plot</code> defines a set of escape sequences which can
     * be included within a text string to constrol the appearance of
     * substrings in the text.
     * It is usually inappropriate for object methods to return strings
     * containing such escape sequences when called by application code.
     * For instance, an application which displays the value of
     * the Title attribute of a Frame usually does not want the displayed
     * string to include escape sequences which a human would have
     * difficulty interpreting.  Therefore the default behaviour is
     * to strip out such escape sequences when called by application code.
     *
     * @return   true iff <code>get*</code> methods will include
     *           escape sequences
     * @see  uk.ac.starlink.ast.Plot#setEscape
     */
    public static boolean getEscapes() {
        return escapes( -1 );
    }

    private static native boolean escapes( int escapes );

    /**
     * Locates escape sequences within a text string.
     * The findings are returned in a user-supplied <code>int[]</code>
     * array, with the following elements:
     * <ul>
     * <li>results[0]: type of escape sequence (one of the GRF__ES*
     *     static final ints defined by the {@link Grf} interface).
     *     This value is undefined if <code>text</code> does not begin
     *     with an escape sequence.
     * <li>results[1]: integer value associated with the escape sequence.
     *     All usable values will be positive.  Zero is returned if the
     *     escape sequence has no associated integer.  A value of -1
     *     indicates that the attribute indicated by <code>results[0]</code>
     *     should be reset to its "normal" value (as established by
     *     {@link Grf#attr} etc).
     *     This value is undefined if <code>text</code> does not begin
     *     with an escape sequence.
     * <li>results[2]: number of characters returned by this call.
     *     If the text starts with an escape sequence, the value
     *     will be the number of characters in the escape sequence.
     *     Otherwise, the value will be the number of character prior
     *     to the first escape sequence, or the length of the supplied
     *     text if no escape sequence is found.
     * </ul>
     *
     * @param  text  the string within which to locate escapes
     * @param  results  array of at least three elements in which the
     *         results are returned
     * @return true iff <code>text</code> starts with an escape sequence
     */
    private static native boolean findEscape( String text, int[] results );

    /**
     * Parses a string into a set of blocks each representing text
     * or an escape code.  The returned value is an array of objects
     * each element of which is either a {@link GrfEscape} or a String.
     *
     * @param  text  string to analyse
     * @return  array of <code>GrfEscape</code> and <code>String</code>
     *          objects representing the parsed value of <code>text</code>
     */
    public static Object[] findEscapes( String text ) {
        List answer = new ArrayList();
        for ( int pos = 0; pos < text.length(); ) {
            int[] results = new int[ 3 ];
            boolean found = findEscape( text.substring( pos ), results );
            if ( found ) {
                answer.add( new GrfEscape( results[ 0 ], results[ 1 ] ) );
            }
            else {
                answer.add( text.substring( pos, pos + results[ 2 ] ) );
            }
            pos += results[ 2 ];
        }
        return answer.toArray();
    }

}
