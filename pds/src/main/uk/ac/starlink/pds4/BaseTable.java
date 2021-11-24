package uk.ac.starlink.pds4;

/**
 * Table subinterface for PDS4 Table_Base objects, representing
 * fixed-width-record-based tables.
 * This corresponds to Table_Binary and Table_Character tables.
 *
 * @author   Mark Taylor
 * @since    24 Nov 2021
 */
public interface BaseTable extends Table {

    /**
     * Returns the record_length value.
     *
     * @return  fixed record length in bytes
     */
    int getRecordLength();
}
