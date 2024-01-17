package uk.ac.starlink.hapi;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Models the HAPI Capabilities response object,
 * as obtained from the capabilities endpoint.
 *
 * @author   Mark Taylor
 * @since    11 Jan 2024
 * @see  <a href="https://github.com/hapi-server/data-specification/blob/master/hapi-3.1.0/HAPI-data-access-spec-3.1.0.md#34-capabilities"
 *          >HAPI 3.1 sec 3.4</a>
 */
public interface HapiCapabilities {

    /**
     * Returns the protocol version.
     *
     * @return  version object, not null
     */
    HapiVersion getHapiVersion();

    /**
     * Returns the supported output formats.
     *
     * @return  output format list, not null and will contain at least
     *          one entry
     */
    String[] getOutputFormats();

    /**
     * Reads HapiCapabilities from a JSON object,
     * with structure defined by the HAPI capabilities endpoint.
     *
     * @param  json  JSON object
     * @return  capabilities object, not null
     */
    public static HapiCapabilities fromJson( JSONObject json ) {
        HapiVersion version =
            HapiVersion.fromText( json.optString( "HAPI", null ) );
        JSONArray formatsArray = json.optJSONArray( "outputFormats" );
        List<String> formatList = new ArrayList<>();
        if ( formatsArray != null ) {
            for ( int i = 0; i < formatsArray.length(); i++ ) {
                String fmt = formatsArray.optString( i, null );
                if ( fmt != null ) {
                    formatList.add( fmt );
                }
            }
        }
        if ( formatList.size() == 0 ) {
            formatList.add( "csv" );
        }
        String[] formats = formatList.toArray( new String[ 0 ] );
        return new HapiCapabilities() {
            public HapiVersion getHapiVersion() {
                return version;
            }
            public String[] getOutputFormats() {
                return formats.clone();
            }
        };
    }
}
