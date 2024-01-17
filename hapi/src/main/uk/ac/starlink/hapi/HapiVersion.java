package uk.ac.starlink.hapi;

/**
 * Characterises the version of the HAPI protocol in use.
 *
 * @author   Mark Taylor
 * @since    11 Jan 2024
 */
public class HapiVersion {

    private final String name_;
    private final String datasetName_;
    private final String startName_;
    private final String stopName_;

    /** Version suitable for use if no version information is available. */
    public static final HapiVersion ASSUMED = createVersion2( "2.x?" );

    /** Version 2.0. */
    public static final HapiVersion V20;

    /** Version 3.0. */
    public static final HapiVersion V30;

    private static final HapiVersion[] STANDARD_VERSIONS = {
        V20 = createVersion2( "2" ),
        V30 = createVersion3( "3" ),
    };

    /**
     * Constructor.
     *
     * @param  name  version name
     * @param  datasetName  name for dataset specification parameter
     * @param  startName   name for start time parameter
     * @param  stopName   name for stop time parameter
     */
    private HapiVersion( String name, String datasetName,
                         String startName, String stopName ) {
        name_ = name;
        datasetName_ = datasetName;
        startName_ = startName;
        stopName_ = stopName;
    }

    /**
     * Return the version string for this version.
     *
     * @return   version string
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns the name for the dataset specification request parameter.
     *
     * @return  dataset request parameter name
     */
    public String getDatasetRequestParam() {
        return datasetName_;
    }

    /**
     * Returns the name for the start time request parameter.
     *
     * @return  start time parameter name
     */
    public String getStartRequestParam() {
        return startName_;
    }

    /**
     * Returns the name for the stop time request parameter.
     *
     * @return  stop time parameter name
     */
    public String getStopRequestParam() {
        return stopName_;
    }

    /**
     * Returns an effective version instance for a version specification string.
     * The supplied string is expected to be of the form 2.x or 3.x,
     * but anything including null is accepted.
     *
     * @param  name  version identifier
     * @return   version instance, not null
     */
    public static HapiVersion fromText( String name ) {
        if ( name == null ) {
            return ASSUMED;
        }
        else {
            return name.startsWith( "3" )   
                 ? createVersion3( name )
                 : createVersion2( name );
        }
    }

    /**
     * Returns standard supported versions.
     *
     * @return  new array of standard versions
     */
    public static HapiVersion[] getStandardVersions() {
        return STANDARD_VERSIONS.clone();
    }

    /**
     * Returns a v2-like version instance.
     *
     * @return  version
     */
    private static HapiVersion createVersion2( String name ) {
        return new HapiVersion( name, "id", "time.min", "time.max" );
    }

    /**
     * Returns a v3-like version instance.
     *
     * @return version
     */
    private static HapiVersion createVersion3( String name ) {
        return new HapiVersion( name, "dataset", "start", "stop" );
    }
}
