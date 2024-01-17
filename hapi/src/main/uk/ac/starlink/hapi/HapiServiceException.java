package uk.ac.starlink.hapi;

import java.io.IOException;

/**
 * IOException subclass that is known to arise from a HAPI service
 * error with a HAPI-specific status code.
 *
 * @author   Mark Taylor
 * @since    26 Jan 2024
 */
public class HapiServiceException extends IOException {

    private final int hapiCode_;

    /**
     * HAPI status code for "Bad request - too much time or data requested".
     * @see  <a href="https://github.com/hapi-server/data-specification/blob/master/hapi-3.1.0/HAPI-data-access-spec-3.1.0.md#4-status-codes"
     *          >HAPI 3.1.0 sec 4.2</a>
     */
    public static final int CODE_TOOMUCH = 1408;

    /**
     * Constructor.
     *
     * @param  errMsg  message for this exception
     * @param  hapiCode  HAPI protocol error status code
     */
    public HapiServiceException( String errMsg, int hapiCode ) {
        super( errMsg );
        hapiCode_ = hapiCode;
    }

    /**
     * Returns the HAPI error status code for this exception.
     * Generally of the form 1xxx.
     *
     * @return  HAPI status code
     */
    public int getHapiCode() {
        return hapiCode_;
    }
}
