/*
 * ESO Archive
 *
 * $Id: URLQueryResult.java,v 1.3 2002/07/09 13:30:36 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/10  Created
 */

package jsky.catalog;

import java.net.*;

/**
 * Represents a query result of some type pointed to by a URL.
 */
public class URLQueryResult implements QueryResult {

    /** A URL pointing to the query result */
    protected URL url;

    /** For FITS tables, the index of the HDU in the FITS file, otherwise -1. */
    protected int hdu = -1;


    /**
     * Create a URLQueryResult from the given URL.
     */
    public URLQueryResult(URL url) {
        this.url = url;
    }

    /**
     * Create a URLQueryResult from the given URL.
     */
    public URLQueryResult(URL url, int hdu) {
        this.url = url;
        this.hdu = hdu;
    }


    /** Return the URL  */
    public URL getURL() {
        return url;
    }

    /** Set the URL  */
    public void setURL(URL url) {
        this.url = url;
    }

    /** Return the HDU  */
    public int getHDU() {
        return hdu;
    }

    /** Set the HDU  */
    public void setHDU(int hdu) {
        this.hdu = hdu;
    }
}
