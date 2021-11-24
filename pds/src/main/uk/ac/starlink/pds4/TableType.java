package uk.ac.starlink.pds4;

/**
 * Labels the known PDS4 table storage formats.
 *
 * @author   Mark Taylor
 * @since    24 Nov 2021
 */
public enum TableType {

    /** Corresponds to Table_Binary. */
    BINARY( "Table_Binary" ),

    /** Corresponds to Table_Character. */
    CHARACTER( "Table_Character" ),

    /** Corresponds to Table_Delimited. */
    DELIMITED( "Table_Delimited" );

    private final String elName_;

    TableType( String elName ) {
        elName_ = elName;
    }

    /**
     * Returns the PDS4 element name to which this table type corresponds.
     *
     * @return  element name
     */
    String getElementName() {
        return elName_;
    }
}
