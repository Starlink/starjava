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
    private final boolean preserveDatatypes_;

    /**
     * Constructor.
     *
     * @param  dataFormat   serialization format
     * @param  version  VOTable output version
     * @param  stringSizer  object that can determine lengths of string array
     *                      elements
     * @param  preserveDatatypes  if true, do not attempt to improve matters
     *                            by choosing more appropriate datatypes
     */
    public VOSerializerConfig( DataFormat dataFormat, VOTableVersion version,
                               StringElementSizer stringSizer,
                               boolean preserveDatatypes ) {
        dataFormat_ = dataFormat;
        version_ = version;
        stringSizer_ = stringSizer;
        preserveDatatypes_ = preserveDatatypes;
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

    /**
     * Indicates whether the serializer should try to keep datatypes as
     * per input data, or whether it may adjust them to produce cleaner output.
     *
     * @return   true if serialization should avoid changing datatypes
     */
    public boolean isPreserveDatatypes() {
        return preserveDatatypes_;
    }
}
