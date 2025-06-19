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
    private final StringElementSizer stringSizer_;

    /**
     * Constructor.
     *
     * @param  dataFormat   serialization format
     * @param  version  VOTable output version
     * @param  stringSizer  object that can determine lengths of string array
     *                      elements
     */
    public VOSerializerConfig( DataFormat dataFormat, VOTableVersion version,
                               StringElementSizer stringSizer ) {
        dataFormat_ = dataFormat;
        version_ = version;
        stringSizer_ = stringSizer;
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

    /**
     * Returns an object that can determine the lengths of
     * string array elements.
     *
     * @return  string array element sizer
     */
    public StringElementSizer getStringSizer() {
        return stringSizer_;
    }
}
