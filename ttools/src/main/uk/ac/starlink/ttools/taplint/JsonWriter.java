package uk.ac.starlink.ttools.taplint;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Outputs an object as JSON.
 * Classes List, Map, Number and Boolean are recognised.
 * Arrays are not.
 * If there is a non-string where a string is required, it's just forced
 * to a string by calling its toString method.
 *
 * @author   Mark Taylor
 * @since    23 Oct 2016
 */
public class JsonWriter {

    private final int indent_;
    private final String spc_;
    private static final String[] ctrlStrs = new String[] {
        "\\u0000", "\\u0001", "\\u0002", "\\u0003",
        "\\u0004", "\\u0005", "\\u0006", "\\u0007",
        "\\b",     "\\t",     "\\n",     "\\u000b",
        "\\f",     "\\r",     "\\u000e", "\\u000f",
        "\\u0010", "\\u0011", "\\u0012", "\\u0013",
        "\\u0014", "\\u0015", "\\u0016", "\\u0017",
        "\\u0018", "\\u0019", "\\u001a", "\\u001b",
        "\\u001c", "\\u001d", "\\u001e", "\\u001f",
    };

    /**
     * Constructor with default properties.
     */
    public JsonWriter() {
        this( 2, true );
    }

    /**
     * Custom constructor.
     *
     * @param   indent  number of characters indent per level
     * @param   spacer  whether to put spaces inside brackets
     */
    public JsonWriter( int indent, boolean spacer ) {
        indent_ = indent;
        spc_ = spacer ? " " : "";
    }

    /**
     * Converts an item to JSON.
     *
     * @param  item  suitable object
     * @return  JSON representation
     */
    public String toJson( Object item ) {
        StringBuffer sbuf = new StringBuffer();
        toJson( sbuf, item, 0, false );
        if ( indent_ >= 0 ) {
            assert sbuf.charAt( 0 ) == '\n';
            return sbuf.substring( 1, sbuf.length() );
        }
        else {
            return sbuf.toString();
        }
    }

    /**
     * Recursive method which does the work for conversion.
     * If possible, call this method with <code>isPositioned=false</code>.
     *
     * @param   sbuf  string buffer to append result to
     * @param   item  object to convert
     * @param   level  current indentation level
     * @param  isPositioned  true if output should be direct to sbuf,
     *         false if it needs a newline plus indentation first
     */
    public void toJson( StringBuffer sbuf, Object item, int level,
                        boolean isPositioned ) {
        if ( item instanceof List ) {
            List list = (List) item;
            if ( list.isEmpty() ) {
                if ( ! isPositioned ) {
                    sbuf.append( getIndent( level ) );
                }
                sbuf.append( "[]" );
            }
            else {
                sbuf.append( getIntroIndent( level, '[', isPositioned ) );
                boolean isPos = ! isPositioned;
                for ( Iterator it = list.iterator(); it.hasNext(); ) {
                    toJson( sbuf, it.next(), level + 1, isPos );
                    if ( it.hasNext() ) {
                        sbuf.append( "," );
                    }
                    isPos = false;
                }
                sbuf.append( spc_ + "]" );
            }
        }
        else if ( item instanceof Map ) {
            Map map = (Map) item;
            if ( map.isEmpty() ) {
                if ( ! isPositioned ) {
                    sbuf.append( getIndent( level ) );
                }
                sbuf.append( "{}" );
            }
            else {
                sbuf.append( getIntroIndent( level, '{', isPositioned ) );
                boolean isPos = ! isPositioned;
                for ( Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) it.next();
                    sbuf.append( jsonPair( entry.getKey().toString(),
                                           entry.getValue(),
                                           level + 1, isPos ) );
                    if ( it.hasNext() ) {
                        sbuf.append( "," );
                    }
                    isPos = false;
                }
                sbuf.append( spc_ + "}" );
            }
        }
        else {
            if ( ! isPositioned ) {
                sbuf.append( getIndent( level ) );
            }
            sbuf.append( jsonScalar( item ) );
        }
    }

    /**
     * Returns prepended whitespace.
     *
     * @param  level  indentation level
     * @return  string to prepend
     */
    public String getIndent( int level ) {
        if ( indent_ >= 0 ) {
            int nc = level * indent_;
            StringBuffer sbuf = new StringBuffer( nc + 1 );
            sbuf.append( '\n' );
            for ( int ic = 0; ic < nc; ic++ ) {
                sbuf.append( ' ' );
            }
            return sbuf.toString();
        }
        else {
            return "";
        }
    }

    /**
     * Represents an object as a JSON string.
     * The supplied value is stringified, its characters are escaped
     * as required, and it is wrapped in quotes.
     *
     * @param  item   scalar object
     * @return   JSON scalar representation
     */
    private String jsonString( Object item ) {
        String str = item == null ? "" : item.toString();
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( '"' );
        for ( int i = 0; i < str.length(); i++ ) {
            sbuf.append( jsonChar( str.charAt( i ) ) );
        }
        sbuf.append( '"' );
        return sbuf.toString();
    }

    /**
     * Serialises a key-value pair to JSON.
     *
     * @param   key  key string
     * @param   value   value object
     * @param   level  indentation level
     * @param  isPositioned  true if output should be direct to sbuf,
     *         false if it needs a newline plus indentation first
     * @return  pair representation
     */
    public String jsonPair( String key, Object value, int level,
                            boolean isPositioned ) {
        StringBuffer sbuf = new StringBuffer();
        toJson( sbuf, key, level, isPositioned );
        sbuf.append( ":" + spc_ );
        toJson( sbuf, value, level, true );
        return sbuf.toString();
    }

    /**
     * Represents a scalar value as a JSON token.
     *
     * @param  item  object, assumed of scalar type, to represent
     * @return  JSON representation (literal, number or string)
     */
    private String jsonScalar( Object item ) {
        if ( item == null ) {
            return "null";
        }
        else if ( item instanceof Boolean ||
                  item instanceof Number ) {
            return item.toString();
        }
        else {
            return jsonString( item );
        }
    }

    /**
     * Escapes a Unicode character as required for insertion
     * into a JSON string.
     *
     * @param  c  character
     * @return   representation of character in a JSON string
     */
    private String jsonChar( char c ) {
        if ( c < 0x20 ) {
            return ctrlStrs[ (int) c ];
        }
        else if ( c == '"' ) {
            return "\\\"";
        }
        else if ( c == '\\' ) {
            return "\\\\";
        }
        else {
            return Character.toString( c );
        }
    }

    /**
     * Returns prepended whitespace containing an opener character.
     *
     * @param  level  indentation level
     * @param  chr  opener character
     * @param  isPositioned  true if output should be direct to sbuf,
     *         false if it needs a newline plus indentation first
     * @return  string to prepend
     */
    private String getIntroIndent( int level, char chr, boolean isPositioned ) {
        if ( isPositioned ) {
            return new StringBuffer().append( chr ).toString();
        }
        else {
            StringBuffer sbuf = new StringBuffer();
            sbuf.append( getIndent( level ) );
            sbuf.append( chr );
            for ( int ic = 0; ic < indent_ - 1; ic++ ) {
                sbuf.append( ' ' );
            }
            return sbuf.toString();
        }
    }
}
