// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Functions for working with JSON strings.
 *
 * <p>Usage of this class to manipulate hierarchical JSON objects is
 * a bit different from the way that most of the other functions in
 * the expression language are used.
 * The main function provided by this class is <code>jsonObject</code>,
 * which can be applied to a string value containing a JSON object
 * (legal JSON object strings are enclosed in curly brackets), and
 * returns a <em>JSONObject</em> instance.
 * This JSONObject cannot be used on its own in the rest of the application,
 * but various <em>methods</em> can be applied to it to extract information
 * from its structure.
 * These methods are applied by writing
 * <code>jsonObject.method(arg,arg,...)</code>
 * rather than
 * <code>function(jsonObject,arg,arg,...)</code>.
 *
 * <p>Methods you can apply to a JSONObject include:
 * <ul>
 * <li><code>getString(key)</code></li>
 * <li><code>getInt(key)</code></li>
 * <li><code>getDouble(key)</code></li>
 * <li><code>getBoolean(key)</code></li>
 * <li><code>getJSONObject(key)</code></li>
 * <li><code>getJSONArray(key)</code></li>
 * </ul>
 * <p>where <code>key</code> is a string giving the name with which
 * the member of the JSON object is associated.
 * The first four of the above methods give you string, numeric, or
 * boolean values that you can use in the application for plotting etc.
 * <code>getJSONObject</code> returns another JSONObject that you can
 * interrogate with further methods.
 * <code>getJSONArray</code> gives you a JSONArray.
 *
 * <p>You can apply a different set of methods to a JSONArray, including:
 * <ul>
 * <li><code>getString(index)</code></li>
 * <li><code>getInt(index)</code></li>
 * <li><code>getDouble(index)</code></li>
 * <li><code>getBoolean(index)</code></li>
 * <li><code>getJSONObject(index)</code></li>
 * <li><code>getJSONArray(index)</code></li>
 * </ul>
 * <p>where <code>index</code> is an integer giving the (zero-based)
 * index of the element in the array that you want.
 *
 * <p>Using these methods you can drill down into the hierarchical structure
 * of a JSON string to retrieve the string, numeric or boolean values
 * that you need.
 * If you are not familiar with this syntax, an example is the
 * best way to illustrate it.
 * Consider the following JSON string, which may be the value in a
 * column named <code>txt</code>:
 * <pre>
 *    {
 *       "sequence": 23,
 *       "temperature": {
 *          "value": 278.5,
 *          "units": "Kelvin"
 *       },
 *       "operational": true,
 *       "readings": [12, null, 23.2, 441, 0]
 *    }
 * </pre>
 * To obtain the sequence value, you can write:
 * <pre>
 *    jsonObject(txt).getInt("sequence")
 * </pre>
 * to obtain the numeric temperature value, you can write:
 * <pre>
 *    jsonObject(txt).getJSONObject("temperature").getDouble("value")
 * </pre>
 * and to obtain the first element of the readings array, you can write:
 * <pre>
 *    jsonObject(txt).getJSONArray("readings").getDouble(0)
 * </pre>
 *
 * <p>Other methods are available on JSONObject and JSONArray;
 * you can currently find documentation on them at
 * <a href="https://stleary.github.io/JSON-java/"
 *         >https://stleary.github.io/JSON-java/</a>.
 * Note in particular that each of the JSONObject.<code>get*(key)</code>
 * and JSONArray.<code>get*(index)</code> methods is accompanied by
 * a corresponding <code>opt*</code> method; where the key/index may
 * not exist, this will probably give effectively the same behaviour
 * (generating a blank result) but may be considerably faster.
 *
 * <p>This class also contains some other utility functions for working
 * with JSONObjects and JSONArrays; see the function documentation below
 * for details.
 *
 * <p><strong>Note:</strong>
 * This class is somewhat experimental, and the functions and methods
 * may change in future.  An attempt will be made to retain the functions
 * and methods described in this section, but those described in the
 * external JSON-java javadocs may be subject to change.
 *
 * @author   Mark Taylor
 * @since    21 Nov 2023
 */
public class Json {

    /**
     * Private constructor prevents instantiation.
     */
    private Json() {
    }

    /**
     * Converts the supplied string to a JSONObject which can be
     * interrogated further.
     * If the input string doesn't have the syntax of a JSON object,
     * an empty JSONObject will be returned.
     *
     * <p>The JSON parsing is currently somewhat lenient, for instance
     * allowing a comma before a closing curly bracket.
     *
     * @param  txt   string assumed to contain a JSON object
     * @return   JSON object value on which further methods can be called
     */
    public static JSONObject jsonObject( String txt ) {
        JSONObject jsonObj = jsonObjectOpt( txt );
        return jsonObj == null ? new JSONObject() : jsonObj;
    }

    /**
     * Converts the supplied string to a JSONArray which can be
     * interrogated further.
     * If the input string doesn't have the syntax of a JSON array,
     * a zero-length JSONArray will be returned.
     *
     * <p>The JSON parsing is currently somewhat lenient, for instance
     * allowing a comma before a closing square bracket.
     *
     * @param  txt   string assumed to contain a JSON array
     * @return  JSON array value on which further methods can be called
     */
    public static JSONArray jsonArray( String txt ) {
        JSONArray jsonArray = jsonArrayOpt( txt );
        return jsonArray == null ? new JSONArray() : jsonArray;
    }

    /**
     * Converts the supplied string to a JSON object which can be
     * interrogated further.
     * If the input doesn't have the syntax of a JSON object,
     * null will be returned.
     *
     * <p>For most purposes this will behave the same as the
     * <code>jsonObject</code> function,
     * but it may be slower if there are many invalid or empty JSON strings.
     *
     * @param  txt   JSON object text
     * @return   JSON object value on which further methods can be called,
     *           or null
     */
    public static JSONObject jsonObjectOpt( String txt ) {
        txt = txt == null ? "" : txt.trim();
        if ( txt.startsWith( "{" ) && txt.endsWith( "}" ) ) {
            try {
                return new JSONObject( txt );
            }
            catch ( JSONException e ) {
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Converts the supplied string to a JSONArray which can be
     * interrogated further.
     * If the input string doesn't have the syntax of a JSON array,
     * null will be returned.
     *
     * @param  txt   JSON object text
     * @return   JSON array value on which further methods can be called,
     *           or null
     */
    public static JSONArray jsonArrayOpt( String txt ) {
        txt = txt == null ? "" : txt.trim();
        if ( txt.startsWith( "[" ) && txt.endsWith( "]" ) ) {
            try {
                return new JSONArray( txt );
            }
            catch ( JSONException e ) {
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Converts a JSONArray to an array of floating point values.
     * The result will be the same length as the supplied JSONArray,
     * and any element in the JSONArray which cannot be interpreted
     * as a floating point value is represented by a NaN.
     *
     * @example  jsonToDoubles(jsonArray("[true, \"two\", 3.0, 4, null"]))
     *           = [NaN, NaN, 3.0, 4.0, NaN]
     *
     * @param  jsonArray  JSON Array
     * @return   floating point array the same length as the input array
     */
    public static double[] jsonToDoubles( JSONArray jsonArray ) {
        if ( jsonArray == null ) {
            return null;
        }
        int n = jsonArray.length();
        double[] doubleArray = new double[ n ];
        for ( int i = 0; i < n; i++ ) {
            doubleArray[ i ] = jsonArray.optDouble( i, Double.NaN );
        }
        return doubleArray;
    }

    /**
     * Converts a JSONArray to an array of string values.
     *
     * @example  jsonToStrings(jsonArray("[true, \"two\", 3.0, 4, null]"))
     *           = ["true", "two", "3.0", "4", null]
     *
     * @param  jsonArray  JSON Array
     * @return   string array the same length as the input array
     */
    public static String[] jsonToStrings( JSONArray jsonArray ) {
        if ( jsonArray == null ) {
            return null;
        }
        int n = jsonArray.length();
        String[] stringArray = new String[ n ];
        for ( int i = 0; i < n; i++ ) {
            stringArray[ i ] = jsonArray.optString( i, null );
        }
        return stringArray;
    }

    /**
     * Returns an array giving keys for each key-value pair in a JSON object.
     * The members of a JSON object are not ordered, so no order can
     * be guaranteed in the output array.
     *
     * @example  jsonGetKeys(jsonObject("{\"one\", 1, \"two\", 2,
     *                                    \"three\", 3}"))
     *           = ["one", "two", "three"]
     * @param  jsonObject  JSON object
     * @return  string array containing keys of object
     */
    public static String[] jsonGetKeys( JSONObject jsonObject ) {
        return jsonObject == null ? null : JSONObject.getNames( jsonObject );
    }
}
