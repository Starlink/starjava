package uk.ac.starlink.table;

import java.io.IOException;

/**
 * Exception thrown if no table handler exists to make a StarTable
 * out of a given item.
 */
public class UnknownTableFormatException extends IOException {

    public UnknownTableFormatException( String message ) {
        super( message );
    }

    public UnknownTableFormatException() {
        super();
    }
}
