package uk.ac.starlink.pds4;

import gov.nasa.pds.label.object.FieldType;

/**
 * Characterises PDS4 Field_* objects.
 * These correspond to table columns.
 *
 * @author  Mark Taylor
 * @since   24 Nov 2021
 * @see <a href="https://pds.nasa.gov/datastandards/documents/dd/current/PDS4_PDS_DD_1G00.html"
 *         >PDS4 Common Data Dictionary</a>
 */
public interface Field {

    /**
     * Returns the field name.
     *
     * @return  field name
     */
    String getName();

    /**
     * Returns the field data type object.
     * This can be used to decode the data.
     *
     * @return  field type
     */
    FieldType getFieldType();

    /**
     * Returns the 1-based byte offset into the record at which
     * this field is found.
     * This is the PDS4 <code>field_location</code> item.
     *
     * @return  field location byte offset; note this is 1-based
     */
    int getFieldLocation();

    /**
     * Returns the number of bytes this field occupies in a record.
     * This is the PDS4 <code>field_length</code> item.
     *
     * @return  field byte count
     */
    int getFieldLength();

    /**
     * Returns the unit string associated with this field.
     *
     * @return  field unit, or null
     */
    String getUnit();

    /**
     * Returns the description string associated with this field.
     *
     * @return  description text, or null
     */
    String getDescription();

    /**
     * Returns a set of string values representing data values in this field
     * which are to be mapped to null values when reading data.
     *
     * @return  array of blank value representations
     */
    String[] getBlankConstants();
}
