package uk.ac.starlink.vo;

/**
 * Describes the capabilities of a TAP service as serialized by the
 * TAPRegExt schema.
 *
 * @author   Mark Taylor
 * @since    7 Mar 2011
 */
public interface TapCapability {

    /**
     * IVO ID for the TAPRegExt standard registry record {@value}.
     * This forms the base URI onto which fragment parts are appended to
     * generate StandardsRegExt StandardKey-style keys to describe some
     * concepts used by TAPRegExt standard.
     */
    public static final String TAPREGEXT_STD_URI =
        "ivo://ivoa.net/std/TAPRegExt";

    /**
     * Feature type key for ADQL(-like) User-Defined Functions.
     */
    public static final Ivoid UDF_FEATURE_TYPE =
        createTapRegExtIvoid( "#features-udf" );

    /**
     * Feature type key for ADQL geometrical functions.
     */
    public static final Ivoid ADQLGEO_FEATURE_TYPE =
        createTapRegExtIvoid( "#features-adqlgeo" );

    /**
     * Returns an array of upload methods known by this capability.
     *
     * @return  uploadMethod element ivo-id attribute values
     */
    Ivoid[] getUploadMethods();

    /**
     * Returns an array of query language specifiers known by this capability.
     *
     * @return  array of language objects
     */
    TapLanguage[] getLanguages();

    /**
     * Returns an array of output format options declared by this capability.
     *
     * @return  array of output formats
     */
    OutputFormat[] getOutputFormats();

    /**
     * Returns an array of data models known by this capability.
     *
     * @return   dataModel element ivo-id attribute values
     */
    Ivoid[] getDataModels();

    /**
     * Returns an array of limit values representing the data limits for
     * result tables.
     * Legal values for limit units are "row" or "byte".
     *
     * @return   output table limits
     */
    TapLimit[] getOutputLimits();

    /**
     * Returns an array of limit values representing the data limits for
     * uploaded tables.
     * Legal values for limit units are "row" or "byte".
     *
     * @return   upload table limits
     */
    TapLimit[] getUploadLimits();

    /**
     * Returns an array of limit values representing the time limits for
     * query execution.
     * The limit units will be "seconds".
     *
     * @return   execution time limits
     */
    TapLimit[] getExecutionLimits();

    /**
     * Returns an array of limit values representing the time limits for
     * query retention.
     * The limit units will be "seconds".
     *
     * @return   retention time limits
     */
    TapLimit[] getRetentionLimits();

    /**
     * Returns an Ivoid with the registry part {@value #TAPREGEXT_STD_URI}
     * and the local part given by a supplied fragment.
     * A "#" character will be prepended to the fragment value if it is
     * non-empty and it does not already have one.
     *
     * @param  fragment  local part of IVOID with or without initial "#"
     * @return  TAPRegExt Ivoid
     */
    public static Ivoid createTapRegExtIvoid( String fragment ) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( TAPREGEXT_STD_URI );
        if ( fragment != null && fragment.trim().length() > 0 ) {
            if ( ! fragment.startsWith( "#" ) ) {
                sbuf.append( '#' );
            }
            sbuf.append( fragment );
        }
        return new Ivoid( sbuf.toString() );
    }
}
