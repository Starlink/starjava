package uk.ac.starlink.pds4;

/**
 * Labels the known PDS4 table storage formats.
 *
 * @author   Mark Taylor
 * @since    24 Nov 2021
 */
public enum TableType {

    /** Corresponds to Table_Binary. */
    BINARY( "Binary" ),

    /** Corresponds to Table_Character. */
    CHARACTER( "Character" ),

    /** Corresponds to Table_Delimited. */
    DELIMITED( "Delimited" );

    private final String suffix_;

    TableType( String suffix ) {
        suffix_ = suffix;
    }

    /**
     * Returns the PDS4 element name to which this table type corresponds.
     *
     * @return  table element name
     */
    String getTableTag() {
        return "Table_" + suffix_;
    }

    /**
     * Returns the PDS4 element name for records within this table.
     *
     * @return  record element name
     */
    String getRecordTag() {
        return "Record_" + suffix_;
    }

    /**
     * Returns the PDS4 element name for fields within this table.
     *
     * @return  field element name
     */
    String getFieldTag() {
        return "Field_" + suffix_;
    }

    /**
     * Returns the PDS4 element name for groups within this table.
     *
     * @return  group element name
     */
    String getGroupTag() {
        return "Group_Field_" + suffix_;
    } 
}
