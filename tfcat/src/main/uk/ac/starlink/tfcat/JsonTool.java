package uk.ac.starlink.tfcat;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Utility class for manipulating object resulting from an org.json parse.
 * A reporter is supplied; the various methods interpret supplied parsed
 * object as documented, and return either a suitable typed object or
 * null if the supplied object is not suitable.  Any problems in the
 * interpretation are reported through the reporter.
 *
 * @author   Mark Taylor
 * @since    9 Feb 2022
 */
class JsonTool {

    private final Reporter reporter_;

    /**
     * Constructor.
     *
     * @param  destination for validity messages
     */
    public JsonTool( Reporter reporter ) {
        reporter_ = reporter;
    }

    /**
     * Returns a JSON object as a string.
     * Problems are reported via this object's reporter.
     *
     * @param  json  JSON object
     * @param  isRequired  whether a blank value should be reported as an error
     * @return   string value if available, null otherwise
     */
    public String asString( Object json, boolean isRequired ) {
        if ( json instanceof String ) {
            return (String) json;
        }
        else if ( isNull( json ) ) {
            if ( isRequired ) {
                reporter_.report( "missing value" );
            }
            return null;
        }
        else {
            reporter_.report( "non-string value" );
            return null;
        }
    }

    /**
     * Returns a JSON object, which may be a string or number in its
     * JSON representation, as a string.
     * Problems are reported via this object's reporter.
     *
     * @param  json  JSON object
     * @param  isRequired  whether a blank value should be reported as an error
     * @return  string representation if available, null otherwise
     */
    public String asStringOrNumber( Object json, boolean isRequired ) {
        if ( json instanceof String || json instanceof Number ) {
            return json.toString();
        }
        else if ( isNull( json ) ) {
            if ( isRequired ) {
                reporter_.report( "missing value" );
            }
            return null;
        }
        else {
            reporter_.report( "non-string/numeric value" );
            return null;
        }
    }

    /**
     * Returns the content of a JSON object as an array of doubles.
     * No NaN or infinite values will be present in the output.
     * Problems are reported via this object's reporter.
     *
     * @param  json  JSON object
     * @param  nreq  required size of output array, or -1 for no contraint
     * @return  double array of required size, or null
     */
    public double[] asNumericArray( Object json, int nreq ) {
        boolean isNanPermitted = false;
        if ( json instanceof JSONArray ) {
            JSONArray jarray = (JSONArray) json;
            int nd = jarray.length();
            if ( nreq > 0 && nreq != nd ) {
                reporter_.report( "wrong array length"
                                + " (" + nd + " != " + nreq + ")" );
                return null;
            }
            double[] darray = new double[ nd ];
            for ( int id = 0; id < nd; id++ ) {
                Object item = jarray.get( id );
                final double dval;
                if ( isNull( item ) && isNanPermitted ) {
                    dval = Double.NaN;
                }
                else if ( item instanceof Number ) {
                    dval = ((Number) item).doubleValue();
                }
                else {
                    reporter_.createReporter( id )
                             .report( "non-numeric value (" + item + ")" );
                    return null;
                }
                darray[ id ] = dval;
            }
            return darray;
        }
        else if ( isNull( json ) ) {
            reporter_.report( "missing array" );
            return null;
        }
        else {
            reporter_.report( "non-array value" );
            return null;
        }
    }

    /**
     * Returns a supplied object as a JSONObject.
     * Problems are reported via this object's reporter.
     *
     * @param  json  JSON object
     * @return  JSONObject if suitable, null otherwise
     */
    public JSONObject asJSONObject( Object json ) {
        if ( json instanceof JSONObject ) {
            return (JSONObject) json;
        }
        else if ( isNull( json ) ) {
            reporter_.report( "missing object" );
            return null;
        }
        else {
            reporter_.report( "non-object value" );
            return null;
        }
    }

    /**
     * Returns a supplied object as a JSONArray.
     * Problems are reported via this object's reporter.
     *
     * @param  json  JSON object
     * @return  JSONArray if suitable, null otherwise
     */
    public JSONArray asJSONArray( Object json ) {
        if ( json instanceof JSONArray ) {
            return (JSONArray) json;
        }
        else if ( isNull( json ) ) {
            reporter_.report( "missing array" );
            return null;
        }
        else {
            reporter_.report( "non-array value" );
            return null;
        }
    }

    /**
     * Checks that a named member is not present in a given JSON object.
     * If it is, the fact will be reported through this object's reporter.
     *
     * @param  json  JSON object
     * @param  member  name of proscribed member
     */
    public void requireAbsent( JSONObject json, String member ) {
        if ( json.has( member ) ) {
            reporter_.report( "has illegal member \"" + member + "\"" );
        }
    }

    /**
     * Tests whether a given object represents the null value.
     * This is sometimes funny in org.JSON output.
     *
     * @param  json  JSON object
     * @return  true iff supplied object represents a blank value
     */
    public static boolean isNull( Object json ) {
        return json == null || JSONObject.NULL.equals( json );
    }
}
