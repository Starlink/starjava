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
     * Feature type key for ADQL(-like) User-Defined Functions. {@value}
     */
    public static final String UDF_FEATURE_TYPE =
        TAPREGEXT_STD_URI + "#features-udf";

    /**
     * Feature type key for ADQL geometrical functions. {@value}
     */
    public static final String ADQLGEO_FEATURE_TYPE =
        TAPREGEXT_STD_URI + "#features-adqlgeo";

    /**
     * Returns an array of upload methods known by this capability.
     *
     * @return  uploadMethod element ivo-id attribute values
     */
    String[] getUploadMethods();

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
    String[] getDataModels();

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
}
