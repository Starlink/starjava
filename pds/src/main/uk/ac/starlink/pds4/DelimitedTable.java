package uk.ac.starlink.pds4;

/**
 * Table subinterface for PDS4 Table_Delimited objects.
 *
 * @author   Mark Taylor
 * @since    24 Nov 2021
 */
public interface DelimitedTable extends Table {

    /**
     * Returns the field delimiter character.
     *
     * @return  field delimiter character
     */
    char getFieldDelimiter();
}
