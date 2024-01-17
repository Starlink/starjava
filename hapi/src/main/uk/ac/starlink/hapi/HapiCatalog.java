package uk.ac.starlink.hapi;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Models the HAPI Catalog response object,
 * as obtained from the catalog endpoint.
 *
 * @author   Mark Taylor
 * @since    12 Jan 2024
 * @see <a href="https://github.com/hapi-server/data-specification/blob/master/hapi-3.1.0/HAPI-data-access-spec-3.1.0.md#35-catalog"
 *         >HAPI 3.1 sec 3.5</a>
 */
public interface HapiCatalog {

    /**
     * Returns the protocol version.
     *
     * @return  version object, not null
     */
    HapiVersion getHapiVersion();

    /**
     * Returns an array of dataset ID values in this catalog.
     *
     * @return  array of dataset IDs, not null
     */
    String[] getDatasetIds();

    /**
     * Reads HapiCatalog from a JSON object,
     * with structure defined by the HAPI catalog endpoint.
     *
     * @param  json  JSON object
     * @return  catalog object, not null
     */
    public static HapiCatalog fromJson( JSONObject json ) {
        HapiVersion version =
            HapiVersion.fromText( json.optString( "HAPI", null ) );
        JSONArray catalog = json.optJSONArray( "catalog" );
        int nel = catalog.length();
        List<String> datasets = new ArrayList<>();
        for ( int i = 0; i < nel; i++ ) {
            JSONObject dsObj = catalog.optJSONObject( i );
            if ( dsObj != null ) {
                String id = dsObj.optString( "id" );
                if ( id != null ) {
                    datasets.add( id );
                }
            }
        }
        String[] dsids = datasets.toArray( new String[ 0 ] );
        return new HapiCatalog() {
            public HapiVersion getHapiVersion() {
                return version;
            }
            public String[] getDatasetIds() {
                return dsids;
            }
        };
    }
}
