package uk.ac.starlink.pds4;

/**
 * Common interface for PDS4 Table_* elements.
 *
 * @author   Mark Taylor
 * @since    24 Nov 2021
 * @see <a href="https://pds.nasa.gov/datastandards/documents/dd/current/PDS4_PDS_DD_1G00.html"
 *         >PDS4 Common Data Dictionary</a>
 */
public interface Table {

    /**
     * Returns the file_name for the file containing this table's data
     * (not the label file).
     * This location is interpreted as relative to the label file.
     *
     * @return  name of data file
     */
    String getFileName();

    /**
     * Indicates what type of PDS4 table this is.
     *
     * @return  table type
     */
    TableType getTableType();

    /**
     * Gives byte offset into data file of data for this table.
     *
     * @return  data byte offset
     */
    long getOffset();

    /**
     * Returns the number of records in this table.
     *
     * @return  record count
     */
    long getRecordCount();

    /**
     * Returns the name of this table.
     *
     * @return  table name, may be null
     */
    String getName();

    /**
     * Returns the local_identifier for this table.
     *
     * @return  local identifier, may be null
     */
    String getLocalIdentifier();

    /**
     * Returns the description for this table.
     *
     * @return  description, may be null
     */
    String getDescription();

    /**
     * Returns the fields and groups defining the record structure
     * of this table.
     *
     * @return   record item array
     */
    RecordItem[] getContents();
}
