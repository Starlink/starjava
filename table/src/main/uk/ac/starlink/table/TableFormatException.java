package uk.ac.starlink.table;

import java.io.IOException;

/**
 * Exception thrown if a table handler fails to parse a table because it
 * does not match the format it can decode.
 */
public class TableFormatException extends IOException {

    public TableFormatException( String message ) {
        super( message );
    }

    public TableFormatException() {
        super();
    }

    @SuppressWarnings("this-escape")
    public TableFormatException( String message, Throwable cause ) {
        super( message );
        initCause( cause );
    }

    @SuppressWarnings("this-escape")
    public TableFormatException( Throwable cause ) {
        super();
        initCause( cause );
    }
}
