package uk.ac.starlink.tfcat;

/**
 * Represents a TFCat Field.
 *
 * @author   Mark Taylor
 * @since    9 Feb 2022
 */
public interface Field {

    /**
     * Returns the field name.
     *
     * @return value of field "name" member
     */
    String getName();

    /**
     * Returns field description.
     *
     * @return  value of field "info" member
     */
    String getInfo();

    /**
     * Returns field datatype.
     *
     * @return   datatype corresponding to field "datatype" member;
     *           null if not present or invalid
     */
    Datatype<?> getDatatype();

    /**
     * Returns field Uniform Content Descriptor.
     *
     * @return  value of field "ucd" member
     */
    String getUcd();

    /**
     * Returns field unit.
     *
     * @return  value of field "unit" member
     */
    String getUnit();
}
