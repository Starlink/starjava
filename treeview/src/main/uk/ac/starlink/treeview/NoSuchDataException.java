package uk.ac.starlink.treeview;

public class NoSuchDataException extends Exception {
    public NoSuchDataException( String message ) {
        super( message );
    }

    public NoSuchDataException( Throwable th ) {
        super( th ); 
    }

    public NoSuchDataException( String message, Throwable th ) {
        super( message, th );
    }
}
