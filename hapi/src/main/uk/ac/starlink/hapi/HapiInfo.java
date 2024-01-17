package uk.ac.starlink.hapi;

import java.io.IOException;
import java.io.InputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.util.ByteList;

/**
 * Models the HAPI Info response object,
 * as obtained from the info endpoint.
 * Except where noted, the return values may be null.
 * 
 * @author   Mark Taylor
 * @see  <a href="https://github.com/hapi-server/data-specification/blob/master/hapi-3.1.0/HAPI-data-access-spec-3.1.0.md#362-info-response-object"
 *          >HAPI 3.1.0 Sec 3.6.2</a>
 * @since    11 Jan 2024
 */
public interface HapiInfo {

    /**
     * Returns the HAPI version.
     *
     * @return   version object, not null
     */
    HapiVersion getHapiVersion();

    /**
     * Returns the parameters (table columns) associated with the dataset.
     * 
     * @return   parameter array, not null
     */
    HapiParam[] getParameters();

    /**
     * Returns the start date for this dataset.
     *
     * @return  start date string, expected to be in Restricted ISO-8601 format
     */
    String getStartDate();

    /**
     * Returns the end date for this dataset.
     *
     * @return  stop date string, expected to be in Restricted ISO-8601 format
     */
    String getStopDate();

    /**
     * Returns the data format.
     *
     * @return  data format
     */
    String getFormat();

    /**
     * Returns the resource URL with additional information about the dataset.
     *
     * @return  resource URL
     */
    String getResourceUrl();

    /**
     * Returns the approximate cadence of records for this dataset.
     *
     * @return  cadence string, expected to be as an ISO 8601 duration
     */
    String getCadence();

    /**
     * Returns the maximum allowed duration for a request.
     *
     * @return  maximum request, expected to be as an ISO 8601 duration
     */
    String getMaxRequestDuration();

    /**
     * Returns the sample start date if available.
     *
     * @return  sample start date, expected to be restricted ISO 8601
     */
    String getSampleStartDate();

    /**
     * Returns the sample stop date if available.
     *
     * @return  sample stop date, expected to be restricted ISO 8601
     */
    String getSampleStopDate();

    /**
     * Returns any other string-typed metadata item provided by the
     * info object.
     *
     * @param  key  name of entry in Info structure JSON object
     * @return   value of entry if present and string-like
     */
    String getMetadata( String key );

    /**
     * Reads HapiInfo from a HAPI Info response JSON object.
     *
     * @param  json  JSON object
     * @return  info object, not null
     */
    public static HapiInfo fromJson( JSONObject json ) {
        HapiVersion version =
            HapiVersion.fromText( json.optString( "HAPI", null ) );
        String startDate = json.optString( "startDate", null );
        String stopDate = json.optString( "stopDate", null );
        String format = json.optString( "format", null );
        String resourceUrl = json.optString( "resourceURL", null );
        String cadence = json.optString( "cadence", null );
        String maxRequestDuration = json.optString( "maxRequestDuration", null);
        String sampleStartDate = json.optString( "sampleStartDate", null );
        String sampleStopDate = json.optString( "sampleStopDate", null );
        JSONArray paramArray = json.optJSONArray( "parameters" );
        final HapiParam[] params;
        if ( paramArray != null ) {
            int np = paramArray.length();
            params = new HapiParam[ np ];
            for ( int ip = 0; ip < np; ip++ ) {
                params[ ip ] =
                    HapiParam.fromJson( paramArray.optJSONObject( ip ) );
            }
        }
        else {
            params = new HapiParam[ 0 ];
        }
        return new HapiInfo() {
            public HapiVersion getHapiVersion() {
                return version;
            }
            public String getStartDate() {
                return startDate;
            }
            public String getStopDate() {
                return stopDate;
            }
            public String getFormat() {
                return format;
            }
            public String getResourceUrl() {
                return resourceUrl;
            }
            public String getCadence() {
                return cadence;
            }
            public String getMaxRequestDuration() {
                return maxRequestDuration;
            }
            public String getSampleStartDate() {
                return sampleStartDate;
            }
            public String getSampleStopDate() {
                return sampleStopDate;
            }
            public HapiParam[] getParameters() {
                return params.clone();
            }
            public String getMetadata( String key ) {
                return json.optString( key, null );
            }
        };
    }

    /**
     * Reads a HapiInfo object from an input stream containing a JSON
     * object with "#" signs at the start of lines,
     * as per the HAPI data response when it includes a header.
     * When the first non-comment line is encountered,
     * reading stops, so that the rest of the InputStream's content
     * can be read following exit.
     * However, to determine that the comments have ended,
     * the implementation has to read at least one byte from the
     * post-comment part.  This byte, if present, is stored in the
     * first element of the supplied <code>overread1</code> buffer
     * if present.  If the end of the file is encounted before any
     * non-comment content, -1 is written to that buffer.
     *
     * @param   in  stream containing commented JSON
     * @param   overread1  buffer for receiving the first non-comment byte,
     *                     may be null
     * @return   info object, not null
     */
    public static HapiInfo fromCommentedStream( InputStream in,
                                                int[] overread1 )
            throws IOException {
        String jsonTxt = readCommentedText( in, overread1 );
        JSONObject json;
        try {
            json = new JSONObject( jsonTxt );
        }
        catch ( JSONException e ) {
            throw new TableFormatException( "Bad JSON in header", e );
        }
        try {
            return fromJson( json );
        }
        catch ( JSONException e ) {
            throw new TableFormatException( "Trouble parsing HAPI JSON", e );
        }
    }

    /**
     * Reads all the commented lines at the start of a stream,
     * storing the first byte of the uncommented part in a supplied buffer.
     * Commented lines are those starting with a "#".
     *
     * <p>This method should be private, but Java 8, unlike Java 9+,
     * does not permit private static methods on interfaces.
     *
     * @param   in  stream containing commented JSON
     * @param   overread1  buffer for receiving the first non-comment byte,
     *                     may be null
     * @return  string containing comment text
     *          without the prefixed "#" characters
     */
    static String readCommentedText( InputStream in, int[] overread1 )
            throws IOException {
        ByteList buf = new ByteList();
        boolean isStart = true;
        while ( true ) {
            int b = in.read();
            switch ( b ) {
                case -1:
                    if ( overread1 != null ) {
                        overread1[ 0 ] = -1;
                    }
                    return buf.decodeUtf8();
                case '\r':
                case '\n':
                    if ( ! isStart ) {
                        isStart = true;
                        buf.add( (byte) '\n' );
                    }
                    break;
                case '#':
                    isStart = false;
                    break;
                default:
                    if ( isStart ) {
                        if ( overread1 != null ) {
                            overread1[ 0 ] = b;
                        }
                        return buf.decodeUtf8();
                    }
                    else {
                        buf.add( (byte) b );
                    }
            }
        }
    }
}
