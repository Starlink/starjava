package uk.ac.starlink.hdx;

/**
 * Unchecked exception to indicate that a plugin has violated its contract.
 *
 * <p>This is generally thrown by code in the Hdx package, to indicate
 * that one of the classes extending the system has violated its
 * contract.  However, it may also be thrown by client code,
 * implementing such extensions, if it wishes to indicate a
 * `this-can't-happen' error during its initialisation.
 *
 * @author Norman Gray <norman@astro.gla.ac.uk>
 * @version $Id$
 */
public class PluginException
        extends RuntimeException {

    public PluginException (String s) {
        super(s);
    }
    
    /**
     * Construct an HdxException from another Exception. Retains the
     * original message and stack trace.
     *
     * @param e the exception to be recast as an HdxException.
     */
    public PluginException (Exception e) {
        super(e.getMessage());
        setStackTrace(e.getStackTrace());
    }
}
