package uk.ac.starlink.vo;

/**
 * Exception thrown when an object cannot be resolved by a name resolution
 * service for some reason.
 * 
 * @author   Mark Taylor (Starlink)
 * @since    4 Feb 2005
 */
public class ResolverException extends Exception {

    public ResolverException( String msg ) {
        super( msg );
    }

    public ResolverException( String msg, Throwable e ) {
        super( msg, e );
    }

    public ResolverException( Throwable e ) {
        super( e );
    }
}
