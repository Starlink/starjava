package uk.ac.starlink.hapi;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Characterises a HAPI Parameter, that is the metadata for a column
 * in a HAPI dataset.
 *
 * @author   Mark Taylor
 * @since    11 Jan 2024
 * @see  <a href="https://github.com/hapi-server/data-specification/blob/master/hapi-3.1.0/HAPI-data-access-spec-3.1.0.md#366-parameter-object"
 *          >HAPI 3.1, sec 3.6.6</a>
 */
public interface HapiParam {

    /**
     * Returns the parameter name.
     *
     * @return  name
     */
    String getName();

    /**
     * Returns the parameter data type.
     *
     * @return  type
     */
    HapiType<?,?> getType();

    /**
     * Returns the length of this parameter.
     * This is non-negative for data types {@link HapiType#STRING}
     * and {@link HapiType#ISOTIME}, and -1 for numeric types.
     *
     * @return  string length or -1
     */
    int getLength();

    /**
     * Returns the size attribute of this parameter.
     * This is an array giving dimensions of an array parameter,
     * or null for a scalar parameter.
     * Array indices are C-like (last index varies fastest).
     *
     * @return  array shape or null
     */
    int[] getSize();

    /**
     * Returns the units for this parameter.
     * If present it should have a length of either 1
     * or the array element count.
     *
     * @return  units array or null
     */
    String[] getUnits();

    /**
     * Returns the labels for this parameter.
     * If present it should have a length of either 1
     * or the array element count.
     *
     * @return  labels array or null
     */
    String[] getLabel();

    /**
     * Returns a string representation of the fill value for this parameter.
     *
     * @return  fill string or null
     */
    String getFill();

    /**
     * Returns a description of this parameter.
     *
     * @return  description or null
     */
    String getDescription();

    /**
     * Reads HapiParam from a HAPI Parameter JSON object.
     *
     * @param  json  JSON object
     * @return  param object, not null
     */
    public static HapiParam fromJson( JSONObject json ) {
        String name = json.optString( "name", null );
        HapiType<?,?> type = HapiType.fromText( json.optString( "type" ) );
        int length = json.optInt( "length", -1 );
        JSONArray sizeArray = json.optJSONArray( "size" );
        final int[] sizes;
        if ( sizeArray != null ) {
            int nd = sizeArray.length();
            sizes = new int[ nd ];
            for ( int i = 0; i < nd; i++ ) {

                /* Note this might throw an (unchecked) JSONException.
                 * But it's hard to know how to recover if the dimension
                 * is not numeric. */
                sizes[ i ] = sizeArray.getInt( i );
            }
        }
        else {
            sizes = null;
        }
        String[] units = stringOrArray( json.opt( "units" ) );
        String[] label = stringOrArray( json.opt( "labels" ) );
        String fill = json.optString( "fill", null );
        String description = json.optString( "description", null );
        return new HapiParam() {
            public String getName() {
                return name;
            }
            public HapiType<?,?> getType() {
                return type;
            }
            public int getLength() {
                return length;
            }
            public int[] getSize() {
                return sizes;
            }
            public String[] getUnits() {
                return units;
            }
            public String[] getLabel() {
                return label;
            }
            public String getFill() {
                return fill;
            }
            public String getDescription() {
                return description;
            }
        };
    }

    /**
     * Interprets an object which may be either a String or a JSONArray
     * as an array of strings.
     * If it's a String, the result is a one-element array.
     *
     * <p>This method should be private, but Java 8, unlike Java 9+, 
     * does not permit private static methods on interfaces.
     *
     * @param  json  object assumed to be read from JSON
     * @return   array of strings or null
     */
    static String[] stringOrArray( Object json ) {
        if ( json instanceof String ) {
            return new String[] { (String) json };
        }
        else if ( json instanceof JSONArray ) {
            JSONArray jarray = (JSONArray) json;
            int nel = jarray.length();
            String[] items = new String[ nel ];
            for ( int i = 0; i < nel; i++ ) {
                items[ i ] = jarray.optString( i, null );
            }
            return items;
        }
        else {
            return null;
        }
    }
}
