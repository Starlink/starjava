package uk.ac.starlink.votable;

/**
 * Encapsulates configuration information for the details of setting up
 * a VOSerializer object.
 *
 * @author   Mark Taylor
 * @since    19 Jun 2025
 */
public class VOSerializerConfig {

    private final DataFormat dataFormat_;
    private final VOTableVersion version_;

    /**
     * Constructor.
     *
     * @param  dataFormat   serialization format
     * @param  version  VOTable output version
     */
    public VOSerializerConfig( DataFormat dataFormat, VOTableVersion version ) {
        dataFormat_ = dataFormat;
        version_ = version;
    }

    /**
     * Returns the VOTable serialization format.
     *
     * @return  serialization format
     */
    public DataFormat getDataFormat() {
        return dataFormat_;
    }

    /**
     * Returns the output VOTable version.
     *
     * @return  VOTable version
     */
    public VOTableVersion getVersion() {
        return version_;
    }
}
