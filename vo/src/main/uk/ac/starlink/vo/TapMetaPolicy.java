package uk.ac.starlink.vo;

import java.net.URL;

/**
 * Defines the policy for acquiring TAP metadata from a remote service.
 * This is a factory for TapMetaReader objects.
 *
 * @author   Mark Taylor
 * @since    25 Mar 2015
 */
public abstract class TapMetaPolicy {

    private final String name_;
    private final String description_;

    /** Uses the /tables endpoint. */
    public static final TapMetaPolicy TABLESET;

    /** Uses the TAP_SCHEMA tables. */
    public static final TapMetaPolicy TAPSCHEMA;
 
    private static final TapMetaPolicy[] KNOWN_VALUES = {
        TABLESET = new TapMetaPolicy( "TableSet",
                                      "Reads all metadata in one go from the "
                                    + "vs:TableSet document at the /tables "
                                    + "endpoint of the TAP service" ) {
            public TapMetaReader createMetaReader( URL serviceUrl ) {
                return new TableSetTapMetaReader( serviceUrl + "/tables" );
            }
        },
        TAPSCHEMA = new TapMetaPolicy( "TAP_SCHEMA",
                                       "Reads metadata as required by making "
                                     + "synchronous queries on the TAP_SCHEMA "
                                     + "tables in the TAP service" ) {
            public TapMetaReader createMetaReader( URL serviceUrl ) {
                return new TapSchemaTapMetaReader( serviceUrl.toString(), 99999,
                                                   true, false );
            }
        },
    };

    /**
     * Constructor.
     *
     * @param  name  short name for this instance
     * @param  description   plain text description of this instance
     */
    protected TapMetaPolicy( String name, String description ) {
        name_ = name;
        description_ = description;
    }

    /**
     * Returns the name of this object.
     *
     * @return  short name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns a plain text description of this object.
     *
     * @return  description
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Creates an object capable of acquiring TAP metadata for a TAP service
     * at a given URL.
     *
     * @param    serviceUrl   base URL of the TAP service
     * @return   new metadata reader
     */
    public abstract TapMetaReader createMetaReader( URL serviceUrl );

    /**
     * Returns a list of some general-purpose concrete implementations
     * of this class.
     *
     * @return   list of instances
     */
    public static TapMetaPolicy[] getStandardInstances() {
        return KNOWN_VALUES.clone();
    }

    /**
     * Returns an instance of this class suitable for general use.
     *
     * @return   default instance
     */
    public static TapMetaPolicy getDefaultInstance() {
        return TAPSCHEMA;
    }
}
