package uk.ac.starlink.vo;

import java.net.URL;

/**
 * Defines behaviour when a URL is clicked.
 * Typically it might launch a browser or something.
 *
 * @author   Mark Taylor
 * @since    7 May 2015
 */
public interface UrlHandler {

    /**
     * Accept a URL that a user has clicked on.
     *
     * @param  url  URL
     */
    void clickUrl( URL url );
}
