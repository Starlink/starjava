// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

import java.util.LinkedHashMap;
import java.util.Map;
import uk.ac.starlink.util.CgiQuery;

/**
 * Functions that construct URLs for external services.
 * Most of the functions here just do string manipulation to build up
 * URL strings, using knowledge of the parameters required for
 * various services.
 *
 * @author   Mark Taylor
 * @since    18 Oct 2019
 */
public class URLs {

    /**
     * Private constructor prevents instantiation.
     */
    private URLs() {
    }

    /**
     * Builds a query-type URL string given a base URL and a list of
     * name, value pairs.
     *
     * <p>The parameters are encoded on the command line according to the
     * "<code>application/x-www-form-urlencoded</code>" convention,
     * which appends a "?" to the base URL, and then adds name=value pairs
     * separated by "&amp;" characters, with percent-encoding of
     * non-URL-friendly characters.
     * This format is used by many services that require a list of parameters
     * to be conveyed on the URL.
     *
     * @example
     *  <code>paramsUrl("http://x.org/", "a", "1", "b", "two", "c", "3&amp;4")
     *      = "http://x.org/?a=1&amp;b=two&amp;c=3%264"</code>
     *
     * @param  baseUrl  basic URL (may or may not already contain a "?")
     * @param  nameValuePairs   an even number of arguments
     *         (or an even-length string array) giving
     *         parameter name1,value1,name2,value2,...nameN,valueN
     * @return  form-encoded URL
     */
    public static String paramsUrl( String baseUrl, String... nameValuePairs ) {
        return paramsUrl( baseUrl, toStringMap( nameValuePairs ) );
    }

    /**
     * Builds a query-type URL string given a base URL and a map giving
     * name-value pairs.
     * The output is in the <code>application/x-www-form-urlencoded</code>
     * format.
     * 
     * @param  baseUrl  basic URL (may or may not already contain a "?")
     * @return  form-encoded URL
     */
    private static String paramsUrl( String baseUrl, Map<String,String> pmap ) {
        CgiQuery query = new CgiQuery( baseUrl );
        for ( Map.Entry<String,String> entry : pmap.entrySet() ) {
            query.addArgument( entry.getKey(), entry.getValue() );
        }
        return query.toURL().toString();
    }

    /**
     * Turns an even-length string array into a Map, by interpreting
     * each pair of elements as a name-value pair.
     *
     * @param  nameValuePairs   an even number of arguments
     *         (or an even-length string array) giving
     *         parameter name1,value1,name2,value2,...nameN,valueN
     * @return  map
     */
    private static Map<String,String> toStringMap( String... nameValuePairs ) {
        int n2 = nameValuePairs.length;
        if ( n2 % 2 == 0 ) {
            Map<String,String> pmap = new LinkedHashMap<String,String>();
            for ( int i2 = 0; i2 < n2; i2 += 2 ) {
                pmap.put( nameValuePairs[ i2 ], nameValuePairs[ i2 + 1 ] );
            }
            return pmap;
        }
        else {
            return null;
        }
    }
}
