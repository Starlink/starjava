package uk.ac.starlink.hdx;

/** 
 * General-purpose exception
 *
 * @author Norman Gray (norman@astro.gla.ac.uk)
 * @author Peter W. Draper
 * @version $Id$
 */
public class HdxException extends Exception {

    public HdxException (String s) {
        super( s );
    }

    /**
     * Construct an HdxException from another Exception. Retains the
     * original message and stack trace.
     *
     * @param e the exception to be recast as an HdxException.
     */
    public HdxException( Exception e ) 
    {
        super( e.getMessage() );
        setStackTrace( e.getStackTrace() );
    }
}
