package uk.ac.starlink.hds;

/**
 * Thrown to indicate that there has been an HDS error of some description.
 *
 * @author  Mark Taylor (STARLINK)
 * @version $Id$
 */
public class HDSException extends Exception {
    public HDSException( String message ) {
        super( message );
    }
}
