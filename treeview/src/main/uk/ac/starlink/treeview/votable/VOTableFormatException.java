package uk.ac.starlink.treeview.votable;

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
