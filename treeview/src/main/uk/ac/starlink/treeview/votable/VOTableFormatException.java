package uk.ac.starlink.treeview.votable;

/**
 * Exception thrown during parsing or processing of a VOTable when it
 * is found to violate the required format.
 */
public class VOTableFormatException extends RuntimeException {
    public VOTableFormatException() {
        super();
    }
    public VOTableFormatException( Throwable th ) {
        super( th );
    }
    public VOTableFormatException( String msg ) {
        super( msg );
    }
    public VOTableFormatException( String msg, Throwable th ) {
        super( msg, th );
    }
}
