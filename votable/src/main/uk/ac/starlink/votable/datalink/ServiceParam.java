package uk.ac.starlink.votable.datalink;

/**
 * Characterises one of the input parameters for a DataLink service descriptor.
 * This is usually generated from a VOTable PARAM element within
 * the service descriptor GROUP element with @name="inputParams".
 *
 * @author   Mark Taylor
 * @since    22 Nov 2017
 */
public interface ServiceParam {

    /**
     * Returns this parameter's name.
     *
     * @return  value of PARAM's @name attribute
     */
    String getName();

    /**
     * Returns this parameter's value as a string;
     * if the empty string is specified for the PARAM's @value attribute,
     * null should be returned.
     *
     * @return  value of PARAM's @value attribute or null
     */
    String getValue();

    /**
     * Returns the XML ID value for this parameter.
     *
     * @return value of PARAM's @ID attribute
     */
    String getId();

    /**
     * Returns the ref (XML REFID) value for this parameter.
     * If present, this points to a FIELD supplying per-row parameter values.
     *
     * @return  value of PARAM's @ref attribute
     */
    String getRef();

    /**
     * Returns the datatype value for this parameter.
     *
     * @return  value of PARAM's @datatype attribute
     */
    String getDatatype();

    /**
     * Returns the unit string for this parameter.
     *
     * @return  value of PARAM's @unit attribute
     */
    String getUnit();

    /**
     * Returns the UCD string for this parameter.
     *
     * @return  value of PARAM's @ucd attribute
     */
    String getUcd();

    /**
     * Returns the Utype string for this parameter.
     *
     * @return  value of PARAM's @utype attribute
     */
    String getUtype();

    /**
     * Returns the Xtype string for this parameter.
     *
     * @return  value of PARAM's @xtype attribute
     */
    String getXtype();

    /**
     * Returns the description for this parameter.
     *
     * @return content of PARAM's DESCRIPTION child
     */
    String getDescription();

    /**
     * Returns the arraysize for this parameter as a numeric array.
     * The final element of the array may be -1, indicating that it
     * is of unknown extent.
     *
     * @return   parsed content of PARAM's @arraysize attribute
     */
    int[] getArraysize();

    /**
     * Returns the minimum and maximum values specified for this param.
     * The result or either element may be null if not supplied.
     *
     * @return  2-element array giving VALUES/MIN and VALUES/MAX contents,
     *          or null if no VALUES child
     */
    String[] getMinMax();

    /**
     * Returns a list of options specified for this param.
     * May be null if not supplied.
     *
     * @return   array giving VALUES/OPTION @value attributes,
     *           or null if no VALUES child
     */
    String[] getOptions();
}
