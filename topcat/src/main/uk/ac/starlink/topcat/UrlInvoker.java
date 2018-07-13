package uk.ac.starlink.topcat;

import java.net.URL;

/**
 * Defines an action that consumes a URL.
 *
 * @author   Mark Taylor
 * @since    6 Feb 2018
 */
public interface UrlInvoker {

    /**
     * Returns a short name for this type of invocation.
     *
     * @return  title
     */
    public abstract String getTitle();

    /**
     * Consumes the URL corresponding to the row
     * to perform the activation action.
     *
     * <p>This method is executed on a non-EDT thread.
     *
     * @param  url  URL
     * @return   outcome
     */
    public abstract Outcome invokeUrl( URL url );

    /**
     * Returns the safety status of invoking an unknown URL in this way.
     *
     * @return  safety
     */
    public Safety getSafety();
}
